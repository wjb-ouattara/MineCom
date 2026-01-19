package com.mining.minecom_server.service;

import com.mining.minecom_server.common.dto.UserDto;
import com.mining.minecom_server.model.UserEntity;
import com.mining.minecom_server.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserDto getUserById(Long userId) {
        Optional<UserEntity> userOptional = userRepository.findById(userId);

        if (userOptional.isEmpty()) {
            System.err.println("❌ getUserById: User non trouvé pour ID " + userId);
            return null;
        }

        UserEntity user = userOptional.get();
        System.out.println("✅ getUserById: User trouvé " + user.getUsername() + " (ID: " + userId + ")");

        return new UserDto(user.getId(), user.getUsername(),
                user.getIsOnline() != null ? user.getIsOnline() : false);
    }

    /**
     * 📝 INSCRIPTION - Avec logs détaillés
     */
    @Transactional
    public UserEntity registerUser(UserEntity user) {
        System.out.println("🔧 registerUser DÉBUT");
        System.out.println("   Username: " + user.getUsername());
        System.out.println("   Password length: " + (user.getPassword() != null ? user.getPassword().length() : 0));

        // Valider
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("Username ne peut pas être vide");
        }

        // Vérifier qu'il n'existe pas déjà
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            System.err.println("❌ registerUser: Username déjà existant!");
            throw new IllegalArgumentException("Username déjà pris");
        }

        // Hasher le mot de passe
        String rawPassword = user.getPassword();
        String hashedPassword = passwordEncoder.encode(rawPassword);
        user.setPassword(hashedPassword);

        System.out.println("✅ Mot de passe hashé (longueur: " + hashedPassword.length() + ")");

        // Initialiser les champs
        if (user.getIsOnline() == null) {
            user.setIsOnline(false);
        }


        // ✅ SAUVEGARDER
        UserEntity savedUser = userRepository.save(user);

        // ✅ FORCER LE FLUSH (s'assurer que c'est bien écrit en DB)
        userRepository.flush();

        System.out.println("✅ registerUser: User sauvegardé avec ID = " + savedUser.getId());
        System.out.println("   Username: " + savedUser.getUsername());
        System.out.println("   IsOnline: " + savedUser.getIsOnline());

        // Vérifier immédiatement
        Optional<UserEntity> verification = userRepository.findById(savedUser.getId());
        if (verification.isPresent()) {
            System.out.println("✅ VÉRIFICATION: User bien présent en base!");
        } else {
            System.err.println("❌ ERREUR CRITIQUE: User non trouvé après save!");
        }

        return savedUser;
    }

    /**
     * 🔐 AUTHENTIFICATION
     */
    public Optional<UserEntity> authenticateUser(String username, String rawPassword) {
        System.out.println("🔑 authenticateUser: Tentative pour " + username);

        Optional<UserEntity> userOptional = userRepository.findByUsername(username);

        if (userOptional.isEmpty()) {
            System.err.println("❌ authenticateUser: User non trouvé: " + username);
            return Optional.empty();
        }

        UserEntity user = userOptional.get();
        System.out.println("✅ User trouvé en base (ID: " + user.getId() + ")");
        System.out.println("   Password hash length: " + user.getPassword().length());

        // Vérifier le mot de passe
        boolean matches = passwordEncoder.matches(rawPassword, user.getPassword());
        System.out.println("   Password match: " + matches);

        if (matches) {
            System.out.println("✅ authenticateUser: Authentification réussie!");
            return Optional.of(user);
        } else {
            System.err.println("❌ authenticateUser: Mot de passe incorrect");
            return Optional.empty();
        }
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        System.out.println("🔍 loadUserByUsername: " + username);

        return userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    System.err.println("❌ loadUserByUsername: User non trouvé: " + username);
                    return new UsernameNotFoundException("Utilisateur non trouvé : " + username);
                });
    }

    public Optional<UserEntity> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}