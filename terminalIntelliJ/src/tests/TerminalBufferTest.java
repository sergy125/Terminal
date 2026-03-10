package tests;

import main.TerminalBuffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TerminalBuffer.
 * Validates expected behavior, boundary conditions, edge cases, and scrollback memory management.
 */
class TerminalBufferTest {

    private TerminalBuffer buffer;

    // Executed BEFORE each @Test to ensure a clean, isolated state
    @BeforeEach
    void setUp() {
        // Initialize a small terminal (10 columns, 5 rows, 50 lines of scrollback)
        // to make coordinates easy to calculate during testing
        buffer = new TerminalBuffer(10, 5, 50);
    }

    // ==========================================
    // 1. BEHAVIORAL TESTS
    // ==========================================

    @Test
    void writingTextShouldAdvanceCursorAndWrapLines() {
        // Write 12 characters into a 10-column width grid
        buffer.writeText("HelloWorld12");

        // The cursor should have wrapped to the second row (y = 1) at column 2 (x = 2)
        assertEquals(2, buffer.getCursorColumn(), "Cursor X should wrap to the next line");
        assertEquals(1, buffer.getCursorRow(), "Cursor Y should move down one row");

        // Verify the characters were stored in the correct coordinates
        assertEquals('H', buffer.getCharacterAt(0, 0));
        assertEquals('2', buffer.getCharacterAt(1, 1));
    }

    @Test
    void clearScreenShouldEraseEverythingAndResetCursor() {
        buffer.writeText("Test");
        buffer.clearScreen();

        assertEquals(0, buffer.getCursorColumn(), "Cursor should reset to X=0");
        assertEquals(0, buffer.getCursorRow(), "Cursor should reset to Y=0");
        assertEquals(' ', buffer.getCharacterAt(0, 0), "The cell should be empty after clearing");
    }

    // ==========================================
    // 2. BOUNDARY CONDITIONS
    // ==========================================

    @Test
    void cursorShouldNotMoveBeyondTopLeftCorner() {
        // Attempt to move left and up when already at the origin (0,0)
        buffer.moveCursorLeft(5);
        buffer.moveCursorUp(5);

        assertEquals(0, buffer.getCursorColumn(), "Cursor must not pass the left boundary (0)");
        assertEquals(0, buffer.getCursorRow(), "Cursor must not pass the top boundary (0)");
    }

    @Test
    void cursorShouldNotMoveBeyondBottomRightCorner() {
        // Attempt an exaggerated move down and right
        buffer.moveCursorRight(50);
        buffer.moveCursorDown(50);

        // The limits are width-1 (9) and height-1 (4)
        assertEquals(9, buffer.getCursorColumn(), "Cursor must not exceed maximum width");
        assertEquals(4, buffer.getCursorRow(), "Cursor must not exceed maximum height");
    }

    // ==========================================
    // 3. EDGE CASES & SECURITY
    // ==========================================

    @Test
    void readOnlyBoundaryShouldPreventCursorMovementAndEdits() {
        buffer.writeText("Lock");
        buffer.lockCurrentPosition(); // Lock placed at x=4, y=0

        // Attempt to move back or up past the lock boundary
        buffer.moveCursorLeft(10);
        buffer.moveCursorUp(10);

        // Cursor should hit the invisible read-only wall
        assertEquals(4, buffer.getCursorColumn(), "Lock must prevent moving left");
        assertEquals(0, buffer.getCursorRow(), "Lock must prevent moving up");

        // Attempt to overwrite the 'L' in "Lock" by forcing a write operation
        buffer.writeText("X");

        // The original 'L' must remain (the cursor is safely ejected from the protected zone)
        assertEquals('L', buffer.getCharacterAt(0, 0), "Locked text cannot be overwritten");
    }

    @Test
    void scrollbackHistoryShouldKeepDroppedLines() {
        // Write 6 lines in a terminal with a 5-row maximum height
        buffer.writeText("L0\nL1\nL2\nL3\nL4\nL5");

        // Line "L0" should be pushed off the active screen.
        // In our design, y = -1 represents the most recently dropped history line.
        assertEquals('L', buffer.getCharacterAt(0, -1), "Must retrieve 'L' from the scrollback history");
        assertEquals('0', buffer.getCharacterAt(1, -1), "Must retrieve '0' from the scrollback history");

        // The active screen now starts with "L1" at row 0
        assertEquals('1', buffer.getCharacterAt(1, 0));
    }

    // ==========================================
    // 4. BONUS REQUIREMENT TEST (Wide Characters)
    // ==========================================

    @Test
    void wideCharactersShouldConsumeTwoCells() {
        // '日' is a CJK character that requires 2 terminal cells
        buffer.writeText("日");

        assertEquals(2, buffer.getCursorColumn(), "Cursor must advance 2 positions for a CJK character");

        // Verify the first cell contains the symbol and the second acts as an empty placeholder
        assertEquals('日', buffer.getCharacterAt(0, 0));
        assertEquals(' ', buffer.getCharacterAt(1, 0));
        assertTrue(buffer.getAttributesAt(1, 0).isWidePlaceholder, "The second cell must be marked as a wide placeholder");
    }

    // ==========================================
    // 5. SCROLLBACK / HISTORY TESTS
    // ==========================================

    @Test
    void scrollbackShouldStorePushedOutLines() {
        // Create a small buffer: 10 width, 3 height, and up to 5 history lines
        TerminalBuffer testBuffer = new TerminalBuffer(10, 3, 5);

        // Write 4 lines. Since max height is 3, "L1" must be pushed to scrollback.
        testBuffer.writeText("L1\nL2\nL3\nL4");

        // 1. Verify active screen (y=0 is now L2)
        assertEquals('L', testBuffer.getCharacterAt(0, 0));
        assertEquals('2', testBuffer.getCharacterAt(1, 0));

        // 2. Verify scrollback (y=-1 is the latest dropped line)
        assertEquals('L', testBuffer.getCharacterAt(0, -1), "Scrollback must contain the 'L' of L1");
        assertEquals('1', testBuffer.getCharacterAt(1, -1), "Scrollback must contain the '1' of L1");
    }

    @Test
    void scrollbackShouldRespectMaxCapacityAndDropOldestLines() {
        // Buffer: width 10, height 2, max scrollback 2
        TerminalBuffer testBuffer = new TerminalBuffer(10, 2, 2);

        // Write 6 lines.
        // - 2 remain on the active screen (L5, L6)
        // - 2 go to scrollback history (L3, L4)
        // - 2 are permanently lost due to capacity limits (L1, L2)
        testBuffer.writeText("L1\nL2\nL3\nL4\nL5\nL6");

        // 1. Check active screen (L5 and L6)
        assertEquals('5', testBuffer.getCharacterAt(1, 0), "Row 0 of the screen must be L5");
        assertEquals('6', testBuffer.getCharacterAt(1, 1), "Row 1 of the screen must be L6");

        // 2. Check scrollback (y=-1 is the newest dropped, y=-2 is the oldest dropped)
        assertEquals('4', testBuffer.getCharacterAt(1, -1), "Recent scrollback (y=-1) must be L4");
        assertEquals('3', testBuffer.getCharacterAt(1, -2), "Oldest scrollback (y=-2) must be L3");

        // 3. Verify capacity limit by trying to access L2 (y=-3)
        assertThrows(IllegalArgumentException.class, () -> {
            testBuffer.getCharacterAt(0, -3); // Attempt to access a purged line
        }, "Should throw an exception because L2 was purged after exceeding maxScrollback capacity");
    }

    @Test
    void getEntireScreenAndScrollbackAsStringShouldCombineBothCorrectly() {
        TerminalBuffer testBuffer = new TerminalBuffer(5, 2, 5);

        testBuffer.writeText("A\nB\nC");
        // 'A' goes to history (y=-1). 'B' and 'C' remain on the active screen (y=0, y=1).

        String totalContent = testBuffer.getEntireScreenAndScrollbackAsString();

        // The method should stitch the scrollback and screen in the correct chronological order
        assertTrue(totalContent.contains("A    \n"), "Total output must include line A from scrollback");
        assertTrue(totalContent.contains("B    \n"), "Total output must include line B from screen");
        assertTrue(totalContent.contains("C    \n"), "Total output must include line C from screen");
    }
}