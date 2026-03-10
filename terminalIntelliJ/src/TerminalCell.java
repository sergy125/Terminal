import java.util.EnumSet;

public class TerminalCell {
    public enum Style { BOLD, ITALIC, UNDERLINE }

    private char character;
    private int foregroundColor;
    private int backgroundColor;
    private EnumSet<Style> styles;

    // ¡NUEVO PARA EL BONUS!
    public boolean isWidePlaceholder = false;

    public TerminalCell() {
        reset();
    }

    public TerminalCell(TerminalCell other) {
        this.character = other.character;
        this.foregroundColor = other.foregroundColor;
        this.backgroundColor = other.backgroundColor;
        this.styles = EnumSet.copyOf(other.styles);
        this.isWidePlaceholder = other.isWidePlaceholder; // Copiar el estado
    }

    public void set(char c, int fg, int bg, EnumSet<Style> styles) {
        this.character = c;
        this.foregroundColor = fg;
        this.backgroundColor = bg;
        this.styles = EnumSet.copyOf(styles);
        this.isWidePlaceholder = false; // Al escribir algo nuevo, ya no es un fantasma
    }

    public void reset() {
        this.character = ' ';
        this.foregroundColor = -1;
        this.backgroundColor = -1;
        this.styles = EnumSet.noneOf(Style.class);
        this.isWidePlaceholder = false;
    }

    public char getCharacter() { return character; }
    public int getForegroundColor() { return foregroundColor; }
    public int getBackgroundColor() { return backgroundColor; }
    public EnumSet<Style> getStyles() { return EnumSet.copyOf(styles); }
}