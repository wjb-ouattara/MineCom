package com.mining.minecom_server.controller;

import com.mining.minecom_server.model.UserEntity;
import com.mining.minecom_server.service.UserService;
import com.mining.minecom_server.common.dto.SignUpRequest;
import com.mining.minecom_server.common.dto.LoginRequest;
import com.mining.minecom_server.service.PresenceService;
import com.mining.minecom_server.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import com.mining.minecom_server.util.JwtUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;
    private final PresenceService presenceService;

    public AuthController(UserService userService, UserRepository userRepository,
                          JwtUtils jwtUtils, PresenceService presenceService) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.jwtUtils = jwtUtils;
        this.presenceService = presenceService;
    }

    /**
     * 📝 INSCRIPTION (signup)
     */
    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@RequestBody SignUpRequest signUpRequest) {

        // Validation
        if (signUpRequest.getUsername() == null || signUpRequest.getUsername().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Username requis");
        }
        if (signUpRequest.getPassword() == null || signUpRequest.getPassword().length() < 4) {
            return ResponseEntity.badRequest().body("Mot de passe trop court (min 4 caractères)");
        }

        // Vérifier si existe déjà
        if (userService.findByUsername(signUpRequest.getUsername()).isPresent()) {
            System.out.println("❌ Username déjà pris: " + signUpRequest.getUsername());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Nom d'utilisateur déjà pris");
        }

        // Créer le user
        UserEntity userToRegister = new UserEntity();
        userToRegister.setUsername(signUpRequest.getUsername());
        userToRegister.setPassword(signUpRequest.getPassword());
        userToRegister.setPhone(signUpRequest.getPhone());
        userToRegister.setProfilePictureUrl(signUpRequest.getProfilePictureUrl());

        try {
            // ✅ SAUVEGARDER en base
            UserEntity registeredUser = userService.registerUser(userToRegister);

            System.out.println("✅ Nouveau user créé: " + registeredUser.getUsername() +
                    " (ID: " + registeredUser.getId() + ")");

            // Vérifier que le user est bien en base
            boolean exists = userRepository.findById(registeredUser.getId()).isPresent();
            System.out.println("✅ Vérification: User existe en base = " + exists);

            // Générer un token pour connexion automatique
            String jwt = jwtUtils.generateJwtToken(registeredUser.getUsername());

            // ✅ Format de réponse cohérent avec le login
            Map<String, Object> response = new HashMap<>();
            response.put("token", jwt);
            response.put("userId", registeredUser.getId());
            response.put("username", registeredUser.getUsername());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la création du user: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la création du compte");
        }
    }

    /**
     * 🔐 CONNEXION (login)
     */
    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody LoginRequest loginRequest) {

        String username = loginRequest.getUsername();
        String password = loginRequest.getPassword();

        System.out.println("🔑 Tentative de connexion: " + username);

        // ✅ Utiliser Optional directement
        Optional<UserEntity> userOptional = userService.authenticateUser(username, password);

        if (userOptional.isPresent()) {
            UserEntity authenticatedUser = userOptional.get();

            System.out.println("✅ Authentification réussie: " + authenticatedUser.getUsername() +
                    " (ID: " + authenticatedUser.getId() + ")");

            // Générer le Token JWT
            String jwt = jwtUtils.generateJwtToken(authenticatedUser.getUsername());

            // ✅ Format cohérent avec signup
            Map<String, Object> response = new HashMap<>();
            response.put("token", jwt);
            response.put("userId", authenticatedUser.getId());
            response.put("username", authenticatedUser.getUsername());

            return ResponseEntity.ok(response);
        } else {
            System.out.println("❌ Authentification échouée pour: " + username);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Identifiants incorrects");
        }
    }

    /**
     * 🚪 DÉCONNEXION
     */
    @PostMapping("/logout")
    public ResponseEntity<String> logout(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();

            // Mettre à jour le statut
            presenceService.setUserOnline(username, false);

            SecurityContextHolder.clearContext();

            System.out.println("👋 Déconnexion: " + username);
            return ResponseEntity.ok("Déconnexion réussie");
        }

        return ResponseEntity.ok("Déconnexion effectuée");
    }
}