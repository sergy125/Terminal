import java.util.EnumSet;

public class TerminalCell {
    public enum Style { BOLD, ITALIC, UNDERLINE }

    private char character;
    private int foregroundColor;
    private int backgroundColor;
    private EnumSet<Style> styles;

    public TerminalCell() {
        reset();
    }

    // Constructor de copia (muy útil para guardar el estado en el scrollback)
    public TerminalCell(TerminalCell other) {
        this.character = other.character;
        this.foregroundColor = other.foregroundColor;
        this.backgroundColor = other.backgroundColor;
        this.styles = EnumSet.copyOf(other.styles);
    }

    public void set(char c, int fg, int bg, EnumSet<Style> styles) {
        this.character = c;
        this.foregroundColor = fg;
        this.backgroundColor = bg;
        this.styles = EnumSet.copyOf(styles);
    }

    public void reset() {
        this.character = ' ';
        this.foregroundColor = -1; // -1 representará el color por defecto
        this.backgroundColor = -1;
        this.styles = EnumSet.noneOf(Style.class);
    }

    public char getCharacter() { return character; }
    public int getForegroundColor() { return foregroundColor; }
    public int getBackgroundColor() { return backgroundColor; }
    public EnumSet<Style> getStyles() { return EnumSet.copyOf(styles); }
}