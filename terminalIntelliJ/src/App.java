import javax.swing.*;

/**
 * Main application entry point for the Java Terminal Emulator.
 * Bootstraps the terminal buffer (data model) and the Swing UI (view).
 */
public class App {
    public static void main(String[] args) {
        // Ensure UI initialization is executed on the Event Dispatch Thread (EDT) for thread safety
        SwingUtilities.invokeLater(() -> {

            // Initialize the main application window
            JFrame frame = new JFrame("Java Terminal Emulator");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);

            // Initialize the core data structure (Terminal Buffer)
            // Configuration: 80 columns width, 15 rows max height, 1000 lines of scrollback history
            TerminalBuffer buffer = new TerminalBuffer(80, 25, 1000);

            // Populate the initial screen with the welcome banner and hotkey instructions
            buffer.writeText("=== JAVA TERMINAL EMULATOR ===\n\n");
            buffer.writeText("Hotkeys to test project requirements:\n");
            buffer.writeText(" [F4] Insert text (pushes existing characters to the right)\n");
            buffer.writeText(" [F5] Clear the active screen (preserves scrollback history)\n");
            buffer.writeText(" [F6] Fill the current line with '=' characters\n");
            buffer.writeText(" [F7] Clear everything (active screen AND scrollback history)\n\n");
            buffer.writeText("Type a wide character (e.g., CJK: 日本語) to test the Bonus requirement.\n");
            buffer.writeText("> ");

            // Apply the read-only boundary
            // Everything written above this point is locked and cannot be modified or deleted by the user
            buffer.lockCurrentPosition();

            // Initialize the UI component and bind it to the data buffer
            TerminalPanel terminalPanel = new TerminalPanel(buffer);

            // Configure window properties, pack components, and render
            frame.add(terminalPanel);
            frame.pack(); // Size the frame to precisely fit the TerminalPanel's preferred size
            frame.setLocationRelativeTo(null); // Center the window on the screen
            frame.setVisible(true);
        });
    }
}