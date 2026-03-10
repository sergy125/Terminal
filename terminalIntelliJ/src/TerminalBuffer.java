import java.util.Deque;
import java.util.LinkedList;
import java.util.EnumSet;

public class TerminalBuffer {
    private final int width;
    private final int height;
    private final int maxScrollback;

    private final Deque<TerminalCell[]> scrollback;
    private final TerminalCell[][] screen;

    private int cursorX = 0;
    private int cursorY = 0;

    // Límites de protección
    private int readOnlyX = 0;
    private int readOnlyY = 0;

    private int currentFg = -1;
    private int currentBg = -1;
    private EnumSet<TerminalCell.Style> currentStyles = EnumSet.noneOf(TerminalCell.Style.class);

    public TerminalBuffer(int width, int height, int maxScrollback) {
        this.width = width;
        this.height = height;
        this.maxScrollback = maxScrollback;
        this.scrollback = new LinkedList<>();
        this.screen = new TerminalCell[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) screen[y][x] = new TerminalCell();
        }
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }

    // --- MAGIA DEL BLOQUEO ---

    public void lockCurrentPosition() {
        this.readOnlyX = cursorX;
        this.readOnlyY = cursorY;
    }

    // Novedad: Comprueba si una coordenada específica está protegida
    public boolean isPositionEditable(int x, int y) {
        if (y < readOnlyY) return false;
        if (y == readOnlyY && x < readOnlyX) return false;
        return true;
    }

    // --- CURSOR OPERATIONS ---

    public int getCursorColumn() { return cursorX; }
    public int getCursorRow() { return cursorY; }

    public void setCursorPosition(int column, int row) {
        int targetX = Math.max(0, Math.min(column, width - 1));
        int targetY = Math.max(0, Math.min(row, height - 1));

        // El muro de contención absoluto
        if (targetY < readOnlyY) {
            targetY = readOnlyY;
        }
        if (targetY == readOnlyY && targetX < readOnlyX) {
            targetX = readOnlyX;
        }

        this.cursorX = targetX;
        this.cursorY = targetY;
    }

    public void moveCursorUp(int n) { setCursorPosition(cursorX, cursorY - n); }
    public void moveCursorDown(int n) { setCursorPosition(cursorX, cursorY + n); }

    public void moveCursorLeft(int n) {
        for (int i = 0; i < n; i++) {
            if (cursorX > 0) setCursorPosition(cursorX - 1, cursorY);
            else if (cursorY > 0) setCursorPosition(width - 1, cursorY - 1);
        }
    }

    public void moveCursorRight(int n) {
        for (int i = 0; i < n; i++) {
            if (cursorX < width - 1) setCursorPosition(cursorX + 1, cursorY);
            else if (cursorY < height - 1) setCursorPosition(0, cursorY + 1);
        }
    }

    // --- NON-CURSOR EDITING OPERATIONS ---

    public void insertEmptyLineAtBottom() {
        if (maxScrollback > 0) {
            TerminalCell[] topRowCopy = new TerminalCell[width];
            for (int x = 0; x < width; x++) topRowCopy[x] = new TerminalCell(screen[0][x]);
            scrollback.addLast(topRowCopy);
            if (scrollback.size() > maxScrollback) scrollback.removeFirst();
        }

        for (int y = 0; y < height - 1; y++) {
            for (int x = 0; x < width; x++) {
                screen[y][x].set(
                        screen[y+1][x].getCharacter(), screen[y+1][x].getForegroundColor(),
                        screen[y+1][x].getBackgroundColor(), screen[y+1][x].getStyles()
                );
            }
        }

        for (int x = 0; x < width; x++) screen[height - 1][x].reset();

        // Ajustar el muro cuando la pantalla hace scroll
        if (readOnlyY > 0) {
            readOnlyY--;
        } else if (readOnlyY == 0) {
            readOnlyX = 0;
        }
    }

    public void clearScreen() {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) screen[y][x].reset();
        }
        readOnlyX = 0;
        readOnlyY = 0;
        setCursorPosition(0, 0);
    }

    public void clearScreenAndScrollback() {
        clearScreen();
        scrollback.clear();
    }

    // --- CURSOR-DEPENDENT EDITING OPERATIONS ---

    // --- MAGIA DEL BONUS: DETECTOR DE CARACTERES ANCHOS ---
    public int getCharWidth(char c) {
        // Detecta los bloques Unicode principales de Chino, Japonés y Coreano (CJK)
        if (c >= 0x4E00 && c <= 0x9FFF) return 2; // Ideogramas CJK
        if (c >= 0x3040 && c <= 0x309F) return 2; // Hiragana (Japonés)
        if (c >= 0x30A0 && c <= 0x30FF) return 2; // Katakana (Japonés)
        if (c >= 0xFF01 && c <= 0xFF60) return 2; // Caracteres Fullwidth
        return 1; // Para el abecedario normal, números, etc.
    }

    // --- CURSOR-DEPENDENT EDITING OPERATIONS ---
    public void writeText(String text) {
        for (char c : text.toCharArray()) {
            if (!isPositionEditable(cursorX, cursorY)) {
                setCursorPosition(readOnlyX, readOnlyY);
            }

            if (c == '\n') {
                cursorX = 0;
                cursorY++;
            } else {
                int charW = getCharWidth(c); // Preguntamos cuánto ocupa esta letra

                // REGLA DEL BONUS: Si la letra ocupa 2 celdas, pero estamos justo en la
                // última columna de la pantalla... ¡no cabe! Hay que saltar de línea primero.
                if (charW == 2 && cursorX == width - 1) {
                    cursorX = 0;
                    cursorY++;
                    if (cursorY >= height) {
                        insertEmptyLineAtBottom();
                        cursorY = height - 1;
                    }
                }

                // 1. Escribimos el carácter en la celda actual
                screen[cursorY][cursorX].set(c, currentFg, currentBg, currentStyles);

                // 2. Si ocupa 2 celdas, marcamos la siguiente como "fantasma" y movemos 2 veces
                if (charW == 2) {
                    screen[cursorY][cursorX + 1].set(' ', currentFg, currentBg, currentStyles);
                    screen[cursorY][cursorX + 1].isWidePlaceholder = true; // Es la sombra del CJK
                    cursorX += 2;
                } else {
                    cursorX++; // Si es normal, movemos 1 vez
                }
            }

            // Comprobación de límites (Wrap)
            if (cursorX >= width) {
                cursorX = 0;
                cursorY++;
            }
            if (cursorY >= height) {
                insertEmptyLineAtBottom();
                cursorY = height - 1;
            }
        }
    }

    // --- MÉTODOS QUE FALTABAN ---

    public void insertText(String text) {
        for (char c : text.toCharArray()) {
            // Si intentamos insertar en zona bloqueada, forzamos el cursor a la zona libre
            if (!isPositionEditable(cursorX, cursorY)) {
                setCursorPosition(readOnlyX, readOnlyY);
            }

            // Desplazar los caracteres actuales a la derecha para hacer hueco
            for (int x = width - 1; x > cursorX; x--) {
                // Solo movemos las letras si esa parte de la pantalla no está bloqueada
                if (isPositionEditable(x, cursorY) && isPositionEditable(x-1, cursorY)) {
                    screen[cursorY][x].set(
                            screen[cursorY][x-1].getCharacter(), screen[cursorY][x-1].getForegroundColor(),
                            screen[cursorY][x-1].getBackgroundColor(), screen[cursorY][x-1].getStyles()
                    );
                }
            }

            // Insertar el nuevo carácter
            screen[cursorY][cursorX].set(c, currentFg, currentBg, currentStyles);

            // Mover el cursor y manejar el salto de línea
            cursorX++;
            if (cursorX >= width) {
                cursorX = 0;
                cursorY++;
                if (cursorY >= height) {
                    insertEmptyLineAtBottom();
                    cursorY = height - 1;
                }
            }
        }
    }

    public void fillLine(char c) {
        for (int x = 0; x < width; x++) {
            // Solo rellenamos la celda si NO está protegida por el candado
            if (isPositionEditable(x, cursorY)) {
                screen[cursorY][x].set(c, currentFg, currentBg, currentStyles);
            }
        }
    }
    // =======================================================
    // --- CONTENT ACCESS (Acceso a la información) ---
    // =======================================================

    // Método auxiliar privado: El núcleo que unifica Screen y Scrollback.
    private TerminalCell getCell(int x, int y) {
        if (x < 0 || x >= width) throw new IllegalArgumentException("X fuera de límites");

        if (y < 0) {
            // Si 'y' es negativo, calculamos su posición en el historial (Deque)
            int scrollbackIndex = scrollback.size() + y;
            if (scrollbackIndex < 0 || scrollbackIndex >= scrollback.size()) {
                throw new IllegalArgumentException("Y fuera de límites del scrollback");
            }
            return ((TerminalCell[]) scrollback.toArray()[scrollbackIndex])[x];
        } else if (y < height) {
            // Si 'y' es 0 o positivo, leemos de la matriz normal de la pantalla
            return screen[y][x];
        } else {
            throw new IllegalArgumentException("Y fuera de límites");
        }
    }

    // 1. Get character at position (from screen and scrollback)
    public char getCharacterAt(int x, int y) {
        return getCell(x, y).getCharacter();
    }

    // 2. Get attributes at position (from screen and scrollback)
    public TerminalCell getAttributesAt(int x, int y) {
        // Devolvemos una COPIA de la celda (new TerminalCell)
        // para que nadie pueda modificar los colores originales por accidente.
        return new TerminalCell(getCell(x, y));
    }

    // 3. Get line as string (from screen and scrollback)
    public String getLineAsString(int y) {
        StringBuilder sb = new StringBuilder(width);
        for (int x = 0; x < width; x++) {
            sb.append(getCell(x, y).getCharacter());
        }
        return sb.toString();
    }

    // 4. Get entire screen content as string
    public String getEntireScreenAsString() {
        StringBuilder sb = new StringBuilder(width * height + height);
        // Bucle solo por la altura de la pantalla visible (y >= 0)
        for (int y = 0; y < height; y++) {
            sb.append(getLineAsString(y)).append("\n");
        }
        return sb.toString();
    }

    // 5. Get entire screen+scrollback content as string
    public String getEntireScreenAndScrollbackAsString() {
        StringBuilder sb = new StringBuilder();
        // Primero: Bucle por los números negativos para sacar el historial
        for (int i = -scrollback.size(); i < 0; i++) {
            sb.append(getLineAsString(i)).append("\n");
        }
        // Segundo: Añadimos la pantalla visible normal justo debajo
        sb.append(getEntireScreenAsString());
        return sb.toString();

    }

}