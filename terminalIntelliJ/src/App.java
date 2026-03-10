import javax.swing.*;

public class App {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {

            JFrame frame = new JFrame("Mi Emulador de Terminal Java");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);

            TerminalBuffer buffer = new TerminalBuffer(80, 20, 1000);

            // 1. Escribimos el mensaje inicial que queremos proteger
            buffer.writeText("=== BIENVENIDO A TU TERMINAL ===\n");
            buffer.writeText("Escribe algo para probar el buffer visual.\n\n");
            buffer.writeText("> ");

            // 2. ¡LA LÍNEA MÁGICA!
            // Todo lo que se haya escrito ANTES de esta línea queda blindado.
            // El cursor nunca podrá subir más arriba de donde está ahora mismo.
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