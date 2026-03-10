import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class TerminalPanel extends JPanel {
    private final TerminalBuffer buffer;
    private final Font font;
    private int charWidth = 10;
    private int charHeight = 20;
    private int fontAscent = 15;

    // ¡NUEVO! Nuestra paleta de colores pasteles para el texto
    private final Color[] pastelColors = {
            new Color(119, 158, 203), // Azul pastel
            new Color(119, 221, 119), // Verde pastel
            new Color(222, 165, 164), // Rojo/Coral pastel
            new Color(179, 158, 181), // Morado pastel
            new Color(255, 179, 71),  // Naranja pastel
            new Color(130, 200, 190)  // Menta pastel
    };

    public TerminalPanel(TerminalBuffer buffer) {
        this.buffer = buffer;
        // Hacemos la fuente un poco más gruesa (BOLD) para que se lea mejor sobre fondo claro
        this.font = new Font("Monospaced", Font.BOLD, 18);

        // ¡NUEVO! Fondo Rosa Pastel
        setBackground(new Color(255, 218, 224));
        setFocusable(true);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();

                if (c == '\n' || c == '\r') {
                    buffer.writeText("\n> ");
                    // ¡HEMOS BORRADO EL LOCK AQUÍ!
                    // Ahora al pulsar Enter no se bloquea la línea de arriba.
                } else if (c == '\b') {
                    int currentX = buffer.getCursorColumn();
                    int currentY = buffer.getCursorRow();

                    int targetX = currentX - 1;
                    int targetY = currentY;
                    if (targetX < 0) {
                        targetX = buffer.getWidth() - 1;
                        targetY--;
                    }

                    if (targetY >= 0 && buffer.isPositionEditable(targetX, targetY)) {
                        buffer.moveCursorLeft(1);
                        buffer.writeText(" ");
                        buffer.moveCursorLeft(1);
                    }
                } else if (c >= 32 && c != 127) {
                    buffer.writeText(String.valueOf(c));
                }
                repaint();
            }

            @Override
            public void keyPressed(KeyEvent e) {
                int keyCode = e.getKeyCode();

                // --- MOVIMIENTO (Requisito: Move cursor up, down, left, right) ---
                if (keyCode == KeyEvent.VK_UP) buffer.moveCursorUp(1);
                else if (keyCode == KeyEvent.VK_DOWN) buffer.moveCursorDown(1);
                else if (keyCode == KeyEvent.VK_LEFT) buffer.moveCursorLeft(1);
                else if (keyCode == KeyEvent.VK_RIGHT) buffer.moveCursorRight(1);

                    // --- PRUEBAS DE LOS REQUISITOS DEL PROYECTO ---

                    // Requisito: "Insert a text on a line" -> Pulsa F4
                    // A diferencia de escribir normal, esto inserta una "X" y EMPUJA el texto a la derecha
                else if (keyCode == KeyEvent.VK_F4) {
                    buffer.insertText("X");
                }

                // Requisito: "Clear the entire screen" -> Pulsa F5
                // Borra todo lo que ves, pero si luego haces scroll (cuando lo programemos), el historial seguiría ahí.
                else if (keyCode == KeyEvent.VK_F5) {
                    buffer.clearScreen();
                    buffer.writeText("> "); // Volvemos a poner el candado para no romper el programa
                    buffer.lockCurrentPosition();
                }

                // Requisito: "Fill a line with a character" -> Pulsa F6
                // Rellena la fila donde esté el cursor con un símbolo.
                else if (keyCode == KeyEvent.VK_F6) {
                    buffer.fillLine('=');
                }

                // Requisito: "Clear the screen and scrollback" -> Pulsa F7
                // Borra absolutamente todo, pantalla y memoria (historial).
                else if (keyCode == KeyEvent.VK_F7) {
                    buffer.clearScreenAndScrollback();
                    buffer.writeText("> ");
                    buffer.lockCurrentPosition();
                }

                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setFont(font);

        FontMetrics fm = g2.getFontMetrics();
        charWidth = fm.charWidth('W');
        charHeight = fm.getHeight();
        fontAscent = fm.getAscent();

        int cols = buffer.getWidth();
        int rows = buffer.getHeight();

        // Dibujar el texto línea por línea
        for (int y = 0; y < rows; y++) {

            // ¡NUEVO! Elegimos el color de la paleta basándonos en la fila actual.
            // El operador % (módulo) hace que si llegamos al final de la paleta, vuelva a empezar.
            Color lineColor = pastelColors[y % pastelColors.length];
            g2.setColor(lineColor);

            for (int x = 0; x < cols; x++) {
                char c = buffer.getCharacterAt(x, y);
                if (c != ' ') {
                    g2.drawString(String.valueOf(c), x * charWidth, (y * charHeight) + fontAscent);
                }
            }
        }

        // ¡NUEVO! Cursor de color blanco semitransparente para que pegue con los pasteles
        g2.setColor(new Color(255, 255, 255, 180));
        g2.fillRect(buffer.getCursorColumn() * charWidth, buffer.getCursorRow() * charHeight, charWidth, charHeight);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(buffer.getWidth() * charWidth, buffer.getHeight() * charHeight);
    }

}