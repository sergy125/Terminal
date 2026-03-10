import javax.swing.*;

public class App {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {

            JFrame frame = new JFrame("Mi Emulador de Terminal Java");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);

            // 80 columnas de ancho, 15 líneas de alto máximo, 1000 líneas de historial
            TerminalBuffer buffer = new TerminalBuffer(80, 15, 1000);

            // 1. Escribimos el mensaje inicial con la guía de teclas
            buffer.writeText("=== BIENVENIDO A TU TERMINAL ===\n\n");
            buffer.writeText("Atajos para probar los requisitos del proyecto:\n");
            buffer.writeText(" [F4] Insertar texto (empuja las letras a la derecha)\n");
            buffer.writeText(" [F5] Limpiar solo la pantalla activa\n");
            buffer.writeText(" [F6] Rellenar la linea actual con '='\n");
            buffer.writeText(" [F7] Limpiar TODO (pantalla y el historial/scrollback)\n\n");
            buffer.writeText("Escribe un caracter japones (ej: 日本語) para ver el Bonus.\n");
            buffer.writeText("> ");

            // 2. Blindamos todo este texto de arriba para que sea intocable
            buffer.lockCurrentPosition();

            // 3. Cargamos la interfaz gráfica
            TerminalPanel terminalPanel = new TerminalPanel(buffer);

            frame.add(terminalPanel);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}