import java.util.EnumSet;

/**
 * Represents a single atomic character cell within the terminal grid.
 * Encapsulates the character data, foreground/background colors, text styles,
 * and spatial metadata for wide characters (e.g., CJK ideographs).
 */
public class TerminalCell {

    /** Supported text rendering styles. */
    public enum Style { BOLD, ITALIC, UNDERLINE }

    private char character;
    private int foregroundColor;
    private int backgroundColor;
    private EnumSet<Style> styles;

    /**
     * Flag indicating if this cell acts as a spatial placeholder (ghost cell)
     * for the right half of a double-width character.
     */
    public boolean isWidePlaceholder = false;

    /**
     * Default constructor. Initializes an empty cell with default attributes.
     */
    public TerminalCell() {
        reset();
    }

    /**
     * Copy constructor. Performs a deep copy of the provided cell.
     * Crucial for maintaining immutable states when pushing rows to the scrollback history.
     *
     * @param other The TerminalCell instance to duplicate.
     */
    public TerminalCell(TerminalCell other) {
        this.character = other.character;
        this.foregroundColor = other.foregroundColor;
        this.backgroundColor = other.backgroundColor;
        // Deep copy of the EnumSet to prevent reference leaking
        this.styles = EnumSet.copyOf(other.styles);
        // Copy the spatial state
        this.isWidePlaceholder = other.isWidePlaceholder;
    }

    /**
     * Updates the cell's content and styling attributes.
     * Writing a new character implicitly removes the placeholder status.
     *
     * @param c      The character to display.
     * @param fg     The foreground color index (-1 for default).
     * @param bg     The background color index (-1 for default).
     * @param styles The set of text styles to apply.
     */
    public void set(char c, int fg, int bg, EnumSet<Style> styles) {
        this.character = c;
        this.foregroundColor = fg;
        this.backgroundColor = bg;
        this.styles = EnumSet.copyOf(styles);

        // Writing new data overwrites any existing wide-character ghost state
        this.isWidePlaceholder = false;
    }

    /**
     * Clears the cell, resetting all data and styling to their default (empty) state.
     */
    public void reset() {
        this.character = ' ';
        this.foregroundColor = -1;
        this.backgroundColor = -1;
        this.styles = EnumSet.noneOf(Style.class);
        this.isWidePlaceholder = false;
    }

    // =======================================================
    // --- GETTERS ---
    // =======================================================

    public char getCharacter() { return character; }
    public int getForegroundColor() { return foregroundColor; }
    public int getBackgroundColor() { return backgroundColor; }

    /**
     * Returns a copy of the active styles to preserve encapsulation.
     * @return A cloned EnumSet of the current styles.
     */
    public EnumSet<Style> getStyles() { return EnumSet.copyOf(styles); }
}