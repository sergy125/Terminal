package main;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * Custom UI component responsible for rendering the terminal grid and handling user input.
 * Acts as both the View and Controller in the application's architecture.
 */
public class TerminalPanel extends JPanel {
    private final TerminalBuffer buffer;
    private final Font font;

    // Dynamic font metrics calculated during rendering
    private int charWidth = 10;
    private int charHeight = 20;
    private int fontAscent = 15;

    /**
     * Pastel color palette used for row-based text highlighting.
     */
    private final Color[] pastelColors = {
            new Color(119, 158, 203), // Pastel Blue
            new Color(119, 221, 119), // Pastel Green
            new Color(222, 165, 164), // Pastel Coral/Red
            new Color(179, 158, 181), // Pastel Purple
            new Color(255, 179, 71),  // Pastel Orange
            new Color(130, 200, 190)  // Pastel Mint
    };

    /**
     * Initializes the terminal panel, sets up the graphical properties,
     * and binds the keyboard event listeners.
     *
     * @param buffer The underlying data model to render and mutate.
     */
    public TerminalPanel(TerminalBuffer buffer) {
        this.buffer = buffer;
        // Using Monospaced to maintain grid integrity
        this.font = new Font("Monospaced", Font.BOLD, 18);

        // Set the default background color (Pastel Pink)
        setBackground(new Color(255, 218, 224));
        setFocusable(true); // Required to capture keyboard events

        // Initialize input event handling
        addKeyListener(new KeyAdapter() {

            /**
             * Handles printable characters and basic editing controls (Enter, Backspace).
             */
            @Override
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();

                if (c == '\n' || c == '\r') {
                    // Carriage return: Move to next line and draw a new prompt
                    buffer.writeText("\n> ");
                    // Note: We deliberately do NOT lock the position here to allow free movement upwards
                } else if (c == '\b') {
                    // Backspace handling: Calculate target coordinates before mutating
                    int currentX = buffer.getCursorColumn();
                    int currentY = buffer.getCursorRow();

                    int targetX = currentX - 1;
                    int targetY = currentY;

                    // Handle line wrapping during deletion
                    if (targetX < 0) {
                        targetX = buffer.getWidth() - 1;
                        targetY--;
                    }

                    // Enforce read-only boundaries: Only delete if the target cell is mutable
                    if (targetY >= 0 && buffer.isPositionEditable(targetX, targetY)) {
                        buffer.moveCursorLeft(1);
                        buffer.writeText(" ");
                        buffer.moveCursorLeft(1);
                    }
                } else if (c >= 32 && c != 127) {
                    // Standard printable ASCII and Extended Unicode characters
                    buffer.writeText(String.valueOf(c));
                }

                // Trigger a UI refresh after mutating the data model
                repaint();
            }

            /**
             * Handles cursor navigation and special function keys mapped to project requirements.
             */
            @Override
            public void keyPressed(KeyEvent e) {
                int keyCode = e.getKeyCode();

                // --- CURSOR NAVIGATION ---
                if (keyCode == KeyEvent.VK_UP) buffer.moveCursorUp(1);
                else if (keyCode == KeyEvent.VK_DOWN) buffer.moveCursorDown(1);
                else if (keyCode == KeyEvent.VK_LEFT) buffer.moveCursorLeft(1);
                else if (keyCode == KeyEvent.VK_RIGHT) buffer.moveCursorRight(1);

                    // --- PROJECT REQUIREMENTS DEMONSTRATION HOTKEYS ---

                    // F4: Insert text (pushes existing characters to the right)
                else if (keyCode == KeyEvent.VK_F4) {
                    buffer.insertText("X");
                }
                // F5: Clear the active screen grid
                else if (keyCode == KeyEvent.VK_F5) {
                    buffer.clearScreen();
                    buffer.writeText("> ");
                    buffer.lockCurrentPosition();
                }
                // F6: Fill the current mutable line with '='
                else if (keyCode == KeyEvent.VK_F6) {
                    buffer.fillLine('=');
                }
                // F7: Clear screen and wipe the scrollback history
                else if (keyCode == KeyEvent.VK_F7) {
                    buffer.clearScreenAndScrollback();
                    buffer.writeText("> ");
                    buffer.lockCurrentPosition();
                }

                // Trigger a UI refresh after navigation or state changes
                repaint();
            }
        });
    }

    /**
     * Custom rendering pipeline. Draws the grid, text characters, and the cursor.
     *
     * @param g The Graphics context provided by Swing.
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        // Enable text anti-aliasing for smoother font rendering
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setFont(font);

        // Dynamically calculate font metrics based on the current graphics context
        FontMetrics fm = g2.getFontMetrics();
        charWidth = fm.charWidth('W'); // Assume 'W' is the widest standard character
        charHeight = fm.getHeight();
        fontAscent = fm.getAscent();

        int cols = buffer.getWidth();
        int rows = buffer.getHeight();

        // Render the buffer content row by row, column by column
        for (int y = 0; y < rows; y++) {
            // Cycle through the pastel color palette based on the row index
            Color lineColor = pastelColors[y % pastelColors.length];
            g2.setColor(lineColor);

            for (int x = 0; x < cols; x++) {
                // Fetch the complete cell attributes to handle CJK bonus logic
                TerminalCell cell = buffer.getAttributesAt(x, y);
                char c = cell.getCharacter();

                // BONUS LOGIC: Skip empty cells and ghost placeholders for wide CJK characters
                if (c != ' ' && !cell.isWidePlaceholder) {
                    g2.drawString(String.valueOf(c), x * charWidth, (y * charHeight) + fontAscent);
                }
            }
        }

        // Render the cursor as a semi-transparent overlay block
        g2.setColor(new Color(255, 255, 255, 180));
        g2.fillRect(buffer.getCursorColumn() * charWidth, buffer.getCursorRow() * charHeight, charWidth, charHeight);
    }

    /**
     * Calculates the exact pixel dimensions required to display the entire terminal grid.
     * Used by the parent JFrame to size the window perfectly during frame.pack().
     */
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(buffer.getWidth() * charWidth, buffer.getHeight() * charHeight);
    }
}