import java.util.Deque;
import java.util.LinkedList;
import java.util.EnumSet;

/**
 * Core data structure for the Terminal Emulator.
 * Manages the active screen grid, the scrollback history, cursor state,
 * and handles advanced features like read-only boundaries and wide characters (CJK).
 */
public class TerminalBuffer {
    private final int width;
    private final int height;
    private final int maxScrollback;

    // Uses a Deque for efficient O(1) insertions at the end and removals at the front
    private final Deque<TerminalCell[]> scrollback;
    private final TerminalCell[][] screen;

    // Current cursor coordinates
    private int cursorX = 0;
    private int cursorY = 0;

    // Read-only boundaries (locks the prompt and historical output)
    private int readOnlyX = 0;
    private int readOnlyY = 0;

    // Current styling attributes
    private int currentFg = -1;
    private int currentBg = -1;
    private EnumSet<TerminalCell.Style> currentStyles = EnumSet.noneOf(TerminalCell.Style.class);

    /**
     * Initializes a new Terminal Buffer.
     * * @param width         Number of columns.
     * @param height        Number of rows in the active screen.
     * @param maxScrollback Maximum number of lines to keep in history.
     */
    public TerminalBuffer(int width, int height, int maxScrollback) {
        this.width = width;
        this.height = height;
        this.maxScrollback = maxScrollback;
        this.scrollback = new LinkedList<>();
        this.screen = new TerminalCell[height][width];

        // Initialize the active screen with empty cells
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) screen[y][x] = new TerminalCell();
        }
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }

    // =======================================================
    // --- READ-ONLY LOCKING MECHANISM ---
    // =======================================================

    /**
     * Locks the buffer up to the current cursor position.
     * Prevents user edits or cursor movement in the locked area.
     */
    public void lockCurrentPosition() {
        this.readOnlyX = cursorX;
        this.readOnlyY = cursorY;
    }

    /**
     * Evaluates if a specific coordinate is mutable based on the read-only lock.
     * * @return true if the position can be edited, false if it is protected.
     */
    public boolean isPositionEditable(int x, int y) {
        if (y < readOnlyY) return false;
        if (y == readOnlyY && x < readOnlyX) return false;
        return true;
    }

    // =======================================================
    // --- CURSOR OPERATIONS ---
    // =======================================================

    public int getCursorColumn() { return cursorX; }
    public int getCursorRow() { return cursorY; }

    /**
     * Safely moves the cursor to a specific coordinate, enforcing screen
     * bounds and respecting the read-only lock.
     */
    public void setCursorPosition(int column, int row) {
        int targetX = Math.max(0, Math.min(column, width - 1));
        int targetY = Math.max(0, Math.min(row, height - 1));

        // Absolute containment boundary enforcement
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

    // =======================================================
    // --- NON-CURSOR EDITING OPERATIONS ---
    // =======================================================

    /**
     * Handles vertical scrolling. Pushes the top row into the scrollback history
     * and shifts all remaining rows upwards, leaving an empty line at the bottom.
     */
    public void insertEmptyLineAtBottom() {
        // Save the top row to scrollback history
        if (maxScrollback > 0) {
            TerminalCell[] topRowCopy = new TerminalCell[width];
            for (int x = 0; x < width; x++) topRowCopy[x] = new TerminalCell(screen[0][x]);
            scrollback.addLast(topRowCopy);

            // Maintain memory bounds
            if (scrollback.size() > maxScrollback) scrollback.removeFirst();
        }

        // Shift screen rows upwards
        for (int y = 0; y < height - 1; y++) {
            for (int x = 0; x < width; x++) {
                screen[y][x].set(
                        screen[y+1][x].getCharacter(), screen[y+1][x].getForegroundColor(),
                        screen[y+1][x].getBackgroundColor(), screen[y+1][x].getStyles()
                );
            }
        }

        // Clear the bottom row
        for (int x = 0; x < width; x++) screen[height - 1][x].reset();

        // Adjust the read-only boundary upwards as the screen scrolls
        if (readOnlyY > 0) {
            readOnlyY--;
        } else if (readOnlyY == 0) {
            readOnlyX = 0;
        }
    }

    /** Clears the active screen. */
    public void clearScreen() {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) screen[y][x].reset();
        }
        readOnlyX = 0;
        readOnlyY = 0;
        setCursorPosition(0, 0);
    }

    /** Clears the active screen and flushes the scrollback history. */
    public void clearScreenAndScrollback() {
        clearScreen();
        scrollback.clear();
    }

    // =======================================================
    // --- CURSOR-DEPENDENT EDITING OPERATIONS ---
    // =======================================================

    /**
     * Determines the display width of a character (Bonus Requirement).
     * * @param c The character to evaluate.
     * @return 2 for wide CJK characters/ideographs, 1 for standard characters.
     */
    public int getCharWidth(char c) {
        if (c >= 0x4E00 && c <= 0x9FFF) return 2; // CJK Unified Ideographs
        if (c >= 0x3040 && c <= 0x309F) return 2; // Hiragana
        if (c >= 0x30A0 && c <= 0x30FF) return 2; // Katakana
        if (c >= 0xFF01 && c <= 0xFF60) return 2; // Fullwidth Forms
        return 1; // Standard ASCII / Latin characters
    }

    /**
     * Writes text to the buffer, overwriting existing content.
     * Automatically handles line wrapping, scrolling, and wide characters.
     */
    public void writeText(String text) {
        for (char c : text.toCharArray()) {
            // Eject cursor from locked areas
            if (!isPositionEditable(cursorX, cursorY)) {
                setCursorPosition(readOnlyX, readOnlyY);
            }

            if (c == '\n') {
                cursorX = 0;
                cursorY++;
            } else {
                int charW = getCharWidth(c);

                // Edge case: Wide character at the last column triggers a wrap first
                if (charW == 2 && cursorX == width - 1) {
                    cursorX = 0;
                    cursorY++;
                    if (cursorY >= height) {
                        insertEmptyLineAtBottom();
                        cursorY = height - 1;
                    }
                }

                // 1. Write the main character
                screen[cursorY][cursorX].set(c, currentFg, currentBg, currentStyles);

                // 2. If wide, set the next cell as a placeholder (ghost cell)
                if (charW == 2) {
                    screen[cursorY][cursorX + 1].set(' ', currentFg, currentBg, currentStyles);
                    screen[cursorY][cursorX + 1].isWidePlaceholder = true;
                    cursorX += 2;
                } else {
                    cursorX++;
                }
            }

            // Check boundaries and wrap/scroll if needed
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

    /**
     * Inserts text at the cursor, pushing existing characters to the right.
     */
    public void insertText(String text) {
        for (char c : text.toCharArray()) {
            if (!isPositionEditable(cursorX, cursorY)) {
                setCursorPosition(readOnlyX, readOnlyY);
            }

            // Shift current line characters to the right
            for (int x = width - 1; x > cursorX; x--) {
                if (isPositionEditable(x, cursorY) && isPositionEditable(x-1, cursorY)) {
                    screen[cursorY][x].set(
                            screen[cursorY][x-1].getCharacter(), screen[cursorY][x-1].getForegroundColor(),
                            screen[cursorY][x-1].getBackgroundColor(), screen[cursorY][x-1].getStyles()
                    );
                }
            }

            // Insert new character
            screen[cursorY][cursorX].set(c, currentFg, currentBg, currentStyles);

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

    /** Fills the current line with a specified character, respecting locks. */
    public void fillLine(char c) {
        for (int x = 0; x < width; x++) {
            if (isPositionEditable(x, cursorY)) {
                screen[cursorY][x].set(c, currentFg, currentBg, currentStyles);
            }
        }
    }

    // =======================================================
    // --- CONTENT ACCESS API ---
    // =======================================================

    /**
     * Internal unified getter. Resolves negative Y coordinates to the scrollback
     * history and positive Y coordinates to the active screen.
     */
    private TerminalCell getCell(int x, int y) {
        if (x < 0 || x >= width) throw new IllegalArgumentException("X out of bounds");

        if (y < 0) {
            int scrollbackIndex = scrollback.size() + y;
            if (scrollbackIndex < 0 || scrollbackIndex >= scrollback.size()) {
                throw new IllegalArgumentException("Y out of scrollback bounds");
            }
            return ((TerminalCell[]) scrollback.toArray()[scrollbackIndex])[x];
        } else if (y < height) {
            return screen[y][x];
        } else {
            throw new IllegalArgumentException("Y out of screen bounds");
        }
    }

    public char getCharacterAt(int x, int y) {
        return getCell(x, y).getCharacter();
    }

    public TerminalCell getAttributesAt(int x, int y) {
        // Returns a deep copy to prevent unauthorized state mutations
        return new TerminalCell(getCell(x, y));
    }

    public String getLineAsString(int y) {
        StringBuilder sb = new StringBuilder(width);
        for (int x = 0; x < width; x++) {
            sb.append(getCell(x, y).getCharacter());
        }
        return sb.toString();
    }

    public String getEntireScreenAsString() {
        StringBuilder sb = new StringBuilder(width * height + height);
        for (int y = 0; y < height; y++) {
            sb.append(getLineAsString(y)).append("\n");
        }
        return sb.toString();
    }

    public String getEntireScreenAndScrollbackAsString() {
        StringBuilder sb = new StringBuilder();
        // Append historical data first
        for (int i = -scrollback.size(); i < 0; i++) {
            sb.append(getLineAsString(i)).append("\n");
        }
        // Append active screen data
        sb.append(getEntireScreenAsString());
        return sb.toString();
    }
}