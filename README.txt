# Java Terminal Emulator Buffer

## Architecture and Class Overview
The architecture is divided into clear components to separate the data model from the visual representation.

* **App:** The entry point of the application. It bootstraps the Swing window and initializes the core buffer. To demonstrate the project's capabilities, the App class writes a welcome banner and a command prompt, and then locks those coordinates. This ensures that the user cannot delete or overwrite the initial instructions, mimicking how a real terminal protects previous shell output.

* **TerminalCell:** At the lowest level of the data model, I designed this as the atomic unit of the terminal grid. Instead of just storing a primitive character, it acts as a rich state container. Each cell remembers its own foreground and background colors, text styles, and crucially, a boolean flag that indicates whether it is acting as an empty spatial placeholder. This placeholder logic is what allows the terminal to support double-width characters without breaking the grid alignment.

* **TerminalBuffer:** The core of the project. It manages a two-dimensional array for the active, editable screen and a separate collection for the scrollback history. I deliberately chose to use a Deque for the scrollback memory. This provides constant time performance when the screen fills up and the oldest top row needs to be pushed into the history queue, while simultaneously dropping the oldest history lines to respect memory limits. This class also handles complex logic like line wrapping, inserting text by pushing existing characters to the right, and calculating character widths to natively support CJK (Chinese, Japanese, Korean) ideographs.

* **TerminalPanel:** To visualize the data, I created this class using Java Swing. It serves as the bridge between the user and the buffer. It captures raw keyboard events, translates them into buffer operations, and then triggers a repaint. For the rendering engine, I bypassed standard text components and used Graphics2D to draw the text cell by cell on a strict monospaced grid. I applied a custom pastel color palette to the rows to give the terminal a unique aesthetic while strictly adhering to the spatial rules dictated by the buffer.

* **TerminalBufferTest:** To guarantee that the engine is completely robust, I wrote a comprehensive test suite using JUnit 5. These tests act as living documentation and rigorously check boundary conditions, cursor collisions against the read-only locks, and the integrity of the scrollback memory when capacity is reached.
