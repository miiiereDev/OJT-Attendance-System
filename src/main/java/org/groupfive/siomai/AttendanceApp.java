package org.groupfive.siomai;

import org.groupfive.siomai.ui.LauncherFrame;
import javax.swing.SwingUtilities;

/**
 * Main Application launcher.
 * Starts the landing LauncherFrame portal.
 */
public class AttendanceApp {
    public static void main(String[] args) {
        // Run on the Event Dispatch Thread (EDT) for Swing thread-safety
        SwingUtilities.invokeLater(() -> {
            LauncherFrame portal = new LauncherFrame();
            portal.setVisible(true);
        });
    }
}
