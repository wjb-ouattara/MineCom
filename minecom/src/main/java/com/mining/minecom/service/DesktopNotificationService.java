package com.mining.minecom.service;

import com.mining.minecom.common.enums.MessageType;
import javafx.application.Platform;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.AWTException;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.RenderingHints;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Notifications « système » façon Telegram / WhatsApp Desktop :
 * <ul>
 *   <li>badge (nombre + couleur du message le plus grave) sur l'icône de la barre des tâches ;</li>
 *   <li>icône dans la zone de notification (en bas à droite) qui reste active quand on ferme la fenêtre ;</li>
 *   <li>notification Windows quand un message arrive alors que la fenêtre n'est pas au premier plan.</li>
 * </ul>
 * Les appels publics se font depuis le thread JavaFX ; les appels AWT sont délégués à l'EDT.
 */
public class DesktopNotificationService {

    /** Tailles fournies à Windows pour l'icône de la fenêtre (il choisit selon le DPI). */
    private static final int[] TASKBAR_ICON_SIZES = {16, 24, 32, 48, 64};

    private final Stage stage;
    private final Runnable onQuit;
    private final BufferedImage baseIcon;
    private TrayIcon trayIcon;
    private boolean backgroundHintShown = false;

    public DesktopNotificationService(Stage stage, Runnable onQuit) {
        this.stage = stage;
        this.onQuit = onQuit;
        this.baseIcon = loadBaseIcon();
        installTrayIcon();
    }

    public boolean isTraySupported() {
        return trayIcon != null;
    }

    /** Vrai si l'utilisateur a la fenêtre sous les yeux. */
    public boolean isWindowActive() {
        return stage.isShowing() && !stage.isIconified() && stage.isFocused();
    }

    // ================================================================
    // BADGE (barre des tâches + zone de notification)
    // ================================================================

    /**
     * @param count   nombre de messages non lus (0 = icône normale)
     * @param fxColor couleur du message le plus grave (ignorée si count = 0)
     * @param tooltip texte au survol de l'icône de la zone de notification
     */
    public void updateBadge(int count, javafx.scene.paint.Color fxColor, String tooltip) {
        Color color = toAwtColor(fxColor);

        List<javafx.scene.image.Image> icons = new ArrayList<>();
        for (int size : TASKBAR_ICON_SIZES) {
            javafx.scene.image.Image fx = toFxImage(renderIcon(size, count, color));
            if (fx != null) icons.add(fx);
        }
        if (!icons.isEmpty()) {
            stage.getIcons().setAll(icons);
        }

        if (trayIcon != null) {
            BufferedImage trayImage = renderIcon(trayRenderSize(), count, color);
            EventQueue.invokeLater(() -> {
                trayIcon.setImage(trayImage);
                trayIcon.setToolTip(tooltip);
            });
        }
    }

    // ================================================================
    // NOTIFICATION WINDOWS
    // ================================================================

    public void notify(String title, String text, MessageType type) {
        if (trayIcon == null) return;
        TrayIcon.MessageType level = switch (type != null ? type : MessageType.TEXT) {
            case ALERTE, INCIDENT -> TrayIcon.MessageType.ERROR;
            case URGENT, SECURITE -> TrayIcon.MessageType.WARNING;
            default -> TrayIcon.MessageType.INFO;
        };
        EventQueue.invokeLater(() -> trayIcon.displayMessage(title, text, level));
    }

    // ================================================================
    // FENÊTRE
    // ================================================================

    /** Ferme la fenêtre sans quitter : MineCom continue de recevoir les messages. */
    public void hideToTray() {
        if (trayIcon == null) {
            stage.setIconified(true);
            return;
        }
        stage.hide();
        if (!backgroundHintShown) {
            backgroundHintShown = true;
            notify("MineCom reste actif",
                    "Vous serez notifié des nouveaux messages. Clic droit sur l'icône → Quitter pour fermer.",
                    MessageType.TEXT);
        }
    }

    public void showWindow() {
        Platform.runLater(() -> {
            stage.show();
            stage.setIconified(false);
            stage.toFront();
            stage.requestFocus();
        });
    }

    public void dispose() {
        if (trayIcon == null) return;
        TrayIcon icon = trayIcon;
        trayIcon = null;
        EventQueue.invokeLater(() -> SystemTray.getSystemTray().remove(icon));
    }

    // ================================================================
    // INTERNE
    // ================================================================

    private void installTrayIcon() {
        if (!SystemTray.isSupported()) {
            System.err.println("⚠️ Zone de notification non supportée : pas d'icône en arrière-plan.");
            return;
        }
        try {
            PopupMenu menu = new PopupMenu();
            MenuItem open = new MenuItem("Ouvrir MineCom");
            open.addActionListener(e -> showWindow());
            MenuItem quit = new MenuItem("Quitter");
            quit.addActionListener(e -> Platform.runLater(onQuit));
            menu.add(open);
            menu.addSeparator();
            menu.add(quit);

            TrayIcon icon = new TrayIcon(renderIcon(trayRenderSize(), 0, null), "MineCom", menu);
            icon.setImageAutoSize(true);
            // Double-clic sur l'icône, ou clic sur une notification → ouvrir la fenêtre
            icon.addActionListener(e -> showWindow());

            SystemTray.getSystemTray().add(icon);
            trayIcon = icon;
            System.out.println("✅ Icône MineCom ajoutée à la zone de notification");
        } catch (AWTException | RuntimeException e) {
            System.err.println("⚠️ Impossible d'ajouter l'icône de notification : " + e.getMessage());
        }
    }

    private int trayRenderSize() {
        // Rendu en 2x puis réduit par AWT (setImageAutoSize) : plus net sur écrans HiDPI
        Dimension d = SystemTray.isSupported() ? SystemTray.getSystemTray().getTrayIconSize() : new Dimension(16, 16);
        return Math.max(32, d.width * 2);
    }

    private BufferedImage loadBaseIcon() {
        try (InputStream in = DesktopNotificationService.class.getResourceAsStream("/images/EDV.png")) {
            return in != null ? ImageIO.read(in) : null;
        } catch (IOException e) {
            System.err.println("⚠️ Icône /images/EDV.png illisible : " + e.getMessage());
            return null;
        }
    }

    /** Icône de l'application avec, si count > 0, une pastille colorée en haut à droite. */
    private BufferedImage renderIcon(int size, int count, Color color) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            if (baseIcon != null) {
                g.drawImage(baseIcon, 0, 0, size, size, null);
            } else {
                g.setColor(new Color(255, 140, 0));
                g.fillRoundRect(0, 0, size, size, size / 4, size / 4);
            }

            if (count <= 0) return img;

            // Pastille : plus grande en proportion sur les petites icônes pour rester visible
            int d = (int) Math.round(size * (size < 24 ? 0.55 : 0.62));
            int x = size - d;
            int y = 0;

            g.setColor(color != null ? color : new Color(231, 76, 60));
            g.fillOval(x, y, d, d);
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(Math.max(1f, size / 24f)));
            g.drawOval(x, y, d - 1, d - 1);

            // En dessous de 24px, un chiffre serait illisible : pastille seule
            if (size >= 24) {
                String txt = count > 99 ? "99+" : String.valueOf(count);
                float ratio = txt.length() == 1 ? 0.70f : txt.length() == 2 ? 0.55f : 0.42f;
                g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(6, Math.round(d * ratio))));
                FontMetrics fm = g.getFontMetrics();
                int tx = x + (d - fm.stringWidth(txt)) / 2;
                int ty = y + (d - fm.getHeight()) / 2 + fm.getAscent();
                g.drawString(txt, tx, ty);
            }
            return img;
        } finally {
            g.dispose();
        }
    }

    /** BufferedImage → Image JavaFX, via PNG (évite une dépendance à javafx.swing). */
    private static javafx.scene.image.Image toFxImage(BufferedImage img) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(img, "png", out);
            return new javafx.scene.image.Image(new ByteArrayInputStream(out.toByteArray()));
        } catch (IOException e) {
            return null;
        }
    }

    private static Color toAwtColor(javafx.scene.paint.Color c) {
        if (c == null) return null;
        return new Color((float) c.getRed(), (float) c.getGreen(), (float) c.getBlue(), (float) c.getOpacity());
    }
}
