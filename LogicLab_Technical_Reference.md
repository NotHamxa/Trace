# LogicLab — Technical Reference

---

## 1. Class Hierarchy

```
Serializable (interface)
├── Circuit
├── Breadboard
├── Wire
└── Component (abstract)

Component (abstract)
├── ICChip (abstract)
│   ├── ANDGate7408
│   ├── ORGate7432
│   ├── NOTGate7404
│   ├── NANDGate7400
│   ├── NORGate7402
│   └── XORGate7486
├── InputComponent (abstract)
│   ├── ToggleSwitch
│   ├── PushButton
│   └── DIPSwitch
├── OutputComponent (abstract)
│   ├── LED
│   └── SevenSegmentDisplay (stretch goal)
├── PassiveComponent (abstract)
│   └── Resistor
└── PowerComponent (abstract)
    ├── PowerSupply5V
    └── Ground

Renderable (interface)
  — implemented by Component, Wire, Breadboard, Pin

Interactable (interface)
  — implemented by ToggleSwitch, PushButton, DIPSwitch

Simulatable (interface)
  — implemented by all Components
```

---

## 2. Core Classes — Fields & Methods

### 2.1 Pin

Every component connects to the circuit through pins.

```java
public class Pin implements Serializable, Renderable {
    private String label;            // "1", "2", "VCC", "GND", etc.
    private PinType type;            // INPUT, OUTPUT, POWER, GROUND
    private LogicState state;        // HIGH, LOW, FLOATING
    private Component owner;         // the component this pin belongs to
    private double x, y;             // position on canvas (absolute)

    // Getters/setters
    public LogicState getState();
    public void setState(LogicState state);
    public Component getOwner();
    public PinType getType();
    public void render(GraphicsContext gc);
}

public enum LogicState {
    HIGH,    // 5V / logic 1
    LOW,     // 0V / logic 0
    FLOATING // not connected — undefined
}

public enum PinType {
    INPUT, OUTPUT, POWER, GROUND
}
```

### 2.2 Component (abstract)

```java
public abstract class Component implements Serializable, Renderable, Simulatable {
    private String id;               // unique ID, e.g. "IC_001"
    private String name;             // display name, e.g. "7408 AND"
    private double x, y;             // position on canvas
    private double width, height;    // bounding box
    private List<Pin> pins;          // all pins on this component
    private boolean placed;          // is it on the breadboard

    public abstract void simulate();          // propagate logic
    public abstract void render(GraphicsContext gc);  // draw on canvas
    public abstract Component clone();        // for drag-from-toolbox
    public List<Pin> getPins();
    public Pin getPin(String label);
    public void setPosition(double x, double y);
    public boolean containsPoint(double px, double py); // hit testing
}
```

### 2.3 ICChip (abstract)

All 7400-series ICs are 14-pin DIP packages. Pin 7 = GND, Pin 14 = VCC. Each chip has 4 gates (except NOT which has 6).

```java
public abstract class ICChip extends Component {
    protected boolean powered;  // true if pin 14 = HIGH and pin 7 = LOW

    @Override
    public void simulate() {
        // Check power first
        powered = (getPin("14").getState() == LogicState.HIGH
                && getPin("7").getState() == LogicState.LOW);

        if (!powered) {
            // All outputs go FLOATING when unpowered
            for (Pin p : getOutputPins()) {
                p.setState(LogicState.FLOATING);
            }
            return;
        }

        // Delegate to subclass gate logic
        computeGates();
    }

    protected abstract void computeGates();
    protected abstract List<Pin> getOutputPins();

    @Override
    public void render(GraphicsContext gc) {
        // Draw the DIP package rectangle
        // Draw pin numbers on each side (1-7 left, 8-14 right)
        // Draw the IC label in the center
        // Draw the notch at the top
    }
}
```

### 2.4 Concrete IC Chips

**ANDGate7408** — Quad 2-input AND

```java
public class ANDGate7408 extends ICChip {

    public ANDGate7408() {
        // 14 pins, standard DIP layout
        // Gate 1: inputs 1,2  → output 3
        // Gate 2: inputs 4,5  → output 6
        // Gate 3: inputs 9,10 → output 8
        // Gate 4: inputs 12,13 → output 11
        // Pin 7 = GND, Pin 14 = VCC
    }

    @Override
    protected void computeGates() {
        applyGate("1", "2", "3");   // gate 1
        applyGate("4", "5", "6");   // gate 2
        applyGate("9", "10", "8");  // gate 3
        applyGate("12", "13", "11"); // gate 4
    }

    private void applyGate(String inA, String inB, String out) {
        LogicState a = getPin(inA).getState();
        LogicState b = getPin(inB).getState();

        if (a == LogicState.FLOATING || b == LogicState.FLOATING) {
            getPin(out).setState(LogicState.FLOATING);
        } else {
            boolean result = (a == LogicState.HIGH) && (b == LogicState.HIGH);
            getPin(out).setState(result ? LogicState.HIGH : LogicState.LOW);
        }
    }

    @Override
    protected List<Pin> getOutputPins() {
        return List.of(getPin("3"), getPin("6"), getPin("8"), getPin("11"));
    }
}
```

All other chips follow the same pattern, changing only the boolean logic:

| IC       | Type | Gate Logic               | Pin Mapping (per gate)                     |
|----------|------|--------------------------|--------------------------------------------|
| 7408     | AND  | `a && b`                 | (1,2→3) (4,5→6) (9,10→8) (12,13→11)      |
| 7432     | OR   | `a \|\| b`              | (1,2→3) (4,5→6) (9,10→8) (12,13→11)      |
| 7404     | NOT  | `!a` (single input)      | (1→2) (3→4) (5→6) (9→8) (11→10) (13→12)  |
| 7400     | NAND | `!(a && b)`              | (1,2→3) (4,5→6) (9,10→8) (12,13→11)      |
| 7402     | NOR  | `!(a \|\| b)`           | (1,2→3) (4,5→6) (9,10→8) (12,13→11)      |
| 7486     | XOR  | `a ^ b`                  | (1,2→3) (4,5→6) (9,10→8) (12,13→11)      |

**Note on 7404 (NOT):** It has 6 inverters instead of 4 gates, each with a single input and single output. Pin layout is different from the 2-input gates.

### 2.5 InputComponent

```java
public abstract class InputComponent extends Component implements Interactable {
    protected LogicState outputState = LogicState.LOW;

    @Override
    public void simulate() {
        // Push current outputState to the output pin
        getOutputPin().setState(outputState);
    }

    public abstract void onInteract();  // called on mouse click
    public abstract Pin getOutputPin();
}
```

**ToggleSwitch** — click to toggle between HIGH and LOW, stays in position.

```java
public class ToggleSwitch extends InputComponent {
    private boolean on = false;

    @Override
    public void onInteract() {
        on = !on;
        outputState = on ? LogicState.HIGH : LogicState.LOW;
    }

    @Override
    public void render(GraphicsContext gc) {
        // Draw a switch graphic
        // Color changes based on on/off state
    }
}
```

**PushButton** — HIGH only while held (mouse pressed), LOW when released.

```java
public class PushButton extends InputComponent {
    @Override
    public void onInteract() {
        // Called on press — set HIGH
        outputState = LogicState.HIGH;
    }

    public void onRelease() {
        // Called on mouse release — set LOW
        outputState = LogicState.LOW;
    }
}
```

**DIPSwitch** — a row of 4 or 8 independent toggle switches in one package. Each switch is a separate output pin.

### 2.6 OutputComponent

```java
public abstract class OutputComponent extends Component {
    public abstract boolean isActive(); // is the output "on"
}
```

**LED**

```java
public class LED extends OutputComponent {
    private Color color;  // RED, GREEN, YELLOW etc.

    public LED(Color color) {
        this.color = color;
        // 2 pins: anode (input), cathode (connects to ground)
    }

    @Override
    public void simulate() {
        // LED lights up if anode = HIGH and cathode path reaches GND
    }

    @Override
    public boolean isActive() {
        return getPin("anode").getState() == LogicState.HIGH;
    }

    @Override
    public void render(GraphicsContext gc) {
        if (isActive()) {
            // Draw filled bright circle with glow effect
        } else {
            // Draw dim/outline circle
        }
    }
}
```

### 2.7 PowerComponent

```java
public class PowerSupply5V extends Component {
    @Override
    public void simulate() {
        getPin("VCC").setState(LogicState.HIGH);
    }
}

public class Ground extends Component {
    @Override
    public void simulate() {
        getPin("GND").setState(LogicState.LOW);
    }
}
```

### 2.8 Resistor

```java
public class Resistor extends PassiveComponent {
    private int resistance; // ohms (for display purposes)

    // 2 pins — in a logic simulator, the resistor just passes signal through
    // Its main role is visual correctness (LED circuits need a resistor)
    @Override
    public void simulate() {
        // Pass signal from pin 1 to pin 2 (or vice versa, whichever is driven)
        if (getPin("1").getState() != LogicState.FLOATING) {
            getPin("2").setState(getPin("1").getState());
        } else if (getPin("2").getState() != LogicState.FLOATING) {
            getPin("1").setState(getPin("2").getState());
        }
    }
}
```

---

## 3. Wire

```java
public class Wire implements Serializable, Renderable {
    private Pin startPin;
    private Pin endPin;
    private Color color;              // user-selectable wire color
    private List<Point2D> waypoints;  // intermediate bend points for routing
    private LogicState currentState;  // for visual rendering

    public Wire(Pin start, Pin end) {
        this.startPin = start;
        this.endPin = end;
        this.waypoints = new ArrayList<>();
        this.color = Color.BLACK;
    }

    public void propagate() {
        // Determine which pin is driving and propagate to the other
        if (startPin.getType() == PinType.OUTPUT || startPin.getState() != LogicState.FLOATING) {
            endPin.setState(startPin.getState());
            currentState = startPin.getState();
        } else {
            startPin.setState(endPin.getState());
            currentState = endPin.getState();
        }
    }

    @Override
    public void render(GraphicsContext gc) {
        // In simulate mode: color by state
        //   HIGH  → bright green or red
        //   LOW   → dark blue or black
        //   FLOATING → gray dashed
        // In draw mode: use the user-selected wire color
        // Draw lines through waypoints
    }

    public boolean isNear(double px, double py, double threshold); // for selection
}
```

---

## 4. Breadboard

A real breadboard has:
- **2 power rails** (top and bottom) — each rail is one continuous horizontal strip, typically split in the middle
- **Terminal strips** — rows of 5 holes on each side of the center gap. Each row of 5 is internally connected.

```java
public class Breadboard implements Serializable, Renderable {
    private int rows;          // typically 30 or 63
    private int cols;          // 10 (5 left + gap + 5 right)
    private ContactPoint[][] grid;  // the hole grid
    private PowerRail topPositive, topNegative;
    private PowerRail bottomPositive, bottomNegative;

    public Breadboard(int rows) {
        this.rows = rows;
        this.cols = 10;
        this.grid = new ContactPoint[rows][cols];
        initializeConnections();
    }

    private void initializeConnections() {
        for (int row = 0; row < rows; row++) {
            // Left side: columns 0-4 are internally connected (one net)
            Net leftNet = new Net();
            for (int col = 0; col < 5; col++) {
                grid[row][col] = new ContactPoint(row, col, leftNet);
            }

            // Right side: columns 5-9 are internally connected (separate net)
            Net rightNet = new Net();
            for (int col = 5; col < 10; col++) {
                grid[row][col] = new ContactPoint(row, col, rightNet);
            }

            // LEFT and RIGHT sides are NOT connected across the center gap
        }
    }

    public ContactPoint getHole(int row, int col);
    public Net getNet(int row, int col); // which net does this hole belong to

    @Override
    public void render(GraphicsContext gc) {
        // Draw the board background (cream/white)
        // Draw the center gap
        // Draw hole grid as small circles
        // Draw power rail markings (red +, blue -)
        // Highlight occupied holes
    }
}
```

### ContactPoint

```java
public class ContactPoint implements Serializable {
    private int row, col;
    private Net net;           // which electrical net this hole belongs to
    private Pin occupant;      // null if empty, or the pin inserted here
    private double canvasX, canvasY;  // screen position

    public boolean isOccupied();
    public void insertPin(Pin pin);
    public void removePin();
    public Net getNet();
}
```

### Net

A Net represents a set of electrically connected points. All holes in the same breadboard row (same side) share a Net. Wires create connections between Nets.

```java
public class Net implements Serializable {
    private List<ContactPoint> points;
    private LogicState state;

    public void addPoint(ContactPoint cp);
    public LogicState getState();
    public void setState(LogicState state);
    public List<Pin> getConnectedPins(); // all pins inserted into this net's holes
}
```

### PowerRail

```java
public class PowerRail implements Serializable {
    private RailType type;  // POSITIVE or NEGATIVE
    private List<ContactPoint> points;
    private Net net;

    // All holes on a power rail share one net
    // When PowerSupply5V is connected to the positive rail, the entire rail is HIGH
    // When Ground is connected to the negative rail, the entire rail is LOW
}
```

---

## 5. Circuit (Top-Level Model)

```java
public class Circuit implements Serializable {
    private Breadboard breadboard;
    private List<Component> components;
    private List<Wire> wires;
    private String name;
    private boolean modified;  // unsaved changes flag

    // === Component Management ===
    public void addComponent(Component c);
    public void removeComponent(Component c);

    // === Wire Management ===
    public void addWire(Pin start, Pin end);
    public void removeWire(Wire w);

    // === Simulation ===
    public void simulate() {
        // 1. Reset all nets to FLOATING
        resetAllNets();

        // 2. Simulate power components first (set VCC and GND)
        for (Component c : components) {
            if (c instanceof PowerComponent) {
                c.simulate();
            }
        }

        // 3. Propagate through wires and nets
        propagateNets();

        // 4. Simulate input components (switches, buttons)
        for (Component c : components) {
            if (c instanceof InputComponent) {
                c.simulate();
            }
        }

        // 5. Propagate again after inputs
        propagateNets();

        // 6. Simulate ICs (may need multiple passes for cascaded gates)
        boolean changed = true;
        int maxIterations = 20;  // prevent infinite loops from feedback
        int iteration = 0;

        while (changed && iteration < maxIterations) {
            changed = false;
            for (Component c : components) {
                if (c instanceof ICChip) {
                    Map<Pin, LogicState> before = captureOutputStates(c);
                    c.simulate();
                    if (outputsChanged(c, before)) {
                        changed = true;
                    }
                }
            }
            propagateNets();
            iteration++;
        }

        if (iteration >= maxIterations) {
            throw new OscillationException("Circuit may have unstable feedback loop");
        }

        // 7. Simulate output components (LEDs update their visual state)
        for (Component c : components) {
            if (c instanceof OutputComponent) {
                c.simulate();
            }
        }
    }

    private void resetAllNets() {
        // Set all contact point nets and pin states to FLOATING
    }

    private void propagateNets() {
        // For each wire: propagate signal from one pin to the other
        for (Wire w : wires) {
            w.propagate();
        }

        // For each net on the breadboard: if any pin in the net is driven,
        // all other pins in the same net receive that state
        breadboard.propagateAllNets();
    }

    private Map<Pin, LogicState> captureOutputStates(Component c) { ... }
    private boolean outputsChanged(Component c, Map<Pin, LogicState> before) { ... }

    // === Save/Load ===
    public void saveToFile(File file) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(this);
        }
    }

    public static Circuit loadFromFile(File file) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            return (Circuit) ois.readObject();
        }
    }
}
```

### Simulation Algorithm (detailed)

The simulation runs every time the user interacts in Simulate Mode (flips a switch, presses a button). It uses an iterative propagation approach:

```
1. Reset all signals to FLOATING
2. Power sources set their output pins (VCC → HIGH, GND → LOW)
3. Propagate power through wires and breadboard nets
4. Input components set their output pins based on user state
5. Propagate input signals through wires and nets
6. IC chips compute their gate logic from input pin states → set output pins
7. Propagate IC outputs through wires and nets
8. Repeat steps 6-7 until no output changes (stable state) or max iterations hit
9. Output components read their input pins and update visual state
```

The multi-pass loop in step 8 handles cascaded gates — where IC1's output feeds IC2's input. A single pass wouldn't propagate through a chain of 3+ ICs. 20 iterations is more than enough for any acyclic circuit; if it doesn't stabilize, there's a feedback oscillation.

---

## 6. Exception Handling

```java
// Circuit errors
public class CircuitShortException extends RuntimeException {
    // Power directly connected to ground
    public CircuitShortException(String message) { super(message); }
}

public class OscillationException extends RuntimeException {
    // Feedback loop causing unstable state
    public OscillationException(String message) { super(message); }
}

public class FloatingInputException extends RuntimeException {
    // IC input pin not connected to anything
    // Used as a warning, not a hard error
    public FloatingInputException(String pinLabel, String componentId) { ... }
}

// User action errors
public class InvalidPlacementException extends RuntimeException {
    // Component placed off-board or overlapping another
    public InvalidPlacementException(String message) { super(message); }
}

public class InvalidConnectionException extends RuntimeException {
    // Connecting two outputs together, or wire to empty space
    public InvalidConnectionException(String message) { super(message); }
}

// File errors
public class CircuitFileCorruptedException extends IOException {
    // Save file can't be deserialized
    public CircuitFileCorruptedException(String message) { super(message); }
}
```

---

## 7. JavaFX Application Structure

### 7.1 Project Layout

```
logiclab/
├── src/main/java/com/logiclab/
│   ├── App.java                      — JavaFX Application entry point
│   ├── model/
│   │   ├── Circuit.java
│   │   ├── Breadboard.java
│   │   ├── ContactPoint.java
│   │   ├── Net.java
│   │   ├── PowerRail.java
│   │   ├── Wire.java
│   │   ├── Pin.java
│   │   ├── LogicState.java           — enum
│   │   ├── PinType.java              — enum
│   │   ├── Component.java            — abstract
│   │   ├── ICChip.java               — abstract
│   │   ├── InputComponent.java       — abstract
│   │   ├── OutputComponent.java      — abstract
│   │   ├── PassiveComponent.java     — abstract
│   │   ├── PowerComponent.java       — abstract
│   │   ├── chips/
│   │   │   ├── ANDGate7408.java
│   │   │   ├── ORGate7432.java
│   │   │   ├── NOTGate7404.java
│   │   │   ├── NANDGate7400.java
│   │   │   ├── NORGate7402.java
│   │   │   └── XORGate7486.java
│   │   ├── input/
│   │   │   ├── ToggleSwitch.java
│   │   │   ├── PushButton.java
│   │   │   └── DIPSwitch.java
│   │   ├── output/
│   │   │   ├── LED.java
│   │   │   └── SevenSegmentDisplay.java
│   │   ├── passive/
│   │   │   └── Resistor.java
│   │   └── power/
│   │       ├── PowerSupply5V.java
│   │       └── Ground.java
│   ├── ui/
│   │   ├── MainWindow.java           — root BorderPane layout
│   │   ├── CanvasView.java           — center: the breadboard + circuit canvas
│   │   ├── ToolboxPanel.java         — left: draggable component palette
│   │   ├── PropertiesPanel.java      — right: selected component info
│   │   ├── Toolbar.java              — top: mode toggle, save/load, clear
│   │   └── StatusBar.java            — bottom: messages, warnings
│   ├── controller/
│   │   ├── DrawController.java       — handles draw mode interactions
│   │   ├── SimulateController.java   — handles simulate mode interactions
│   │   └── FileController.java       — save/load/new
│   ├── interfaces/
│   │   ├── Renderable.java
│   │   ├── Simulatable.java
│   │   └── Interactable.java
│   ├── exceptions/
│   │   ├── CircuitShortException.java
│   │   ├── OscillationException.java
│   │   ├── FloatingInputException.java
│   │   ├── InvalidPlacementException.java
│   │   ├── InvalidConnectionException.java
│   │   └── CircuitFileCorruptedException.java
│   └── util/
│       ├── ComponentFactory.java     — creates components by type string
│       └── CircuitValidator.java     — checks for shorts, floating pins
├── src/main/resources/
│   ├── styles/
│   │   └── logiclab.css              — JavaFX stylesheet
│   └── icons/
│       ├── and_gate.png
│       ├── or_gate.png
│       ├── led.png
│       └── ...                       — toolbox icons
├── build.gradle
└── README.md
```

### 7.2 Main Window Layout (BorderPane)

```
┌─────────────────────────────────────────────────────┐
│  Toolbar: [Draw Mode | Simulate Mode]  [Save] [Load]│
├──────────┬──────────────────────────┬───────────────┤
│          │                          │               │
│ Toolbox  │      Canvas              │  Properties   │
│          │                          │               │
│ [AND]    │   ┌────────────────┐     │  Component:   │
│ [OR]     │   │                │     │  AND 7408     │
│ [NOT]    │   │   Breadboard   │     │               │
│ [NAND]   │   │   + Components │     │  Pin 1: HIGH  │
│ [NOR]    │   │   + Wires      │     │  Pin 2: LOW   │
│ [XOR]    │   │                │     │  Pin 3: LOW   │
│ [LED]    │   └────────────────┘     │               │
│ [Switch] │                          │               │
│ [Button] │                          │               │
│ [5V]     │                          │               │
│ [GND]    │                          │               │
│ [Resistor│                          │               │
├──────────┴──────────────────────────┴───────────────┤
│  Status: Ready | Warnings: 2 floating inputs        │
└─────────────────────────────────────────────────────┘
```

### 7.3 App.java (Entry Point)

```java
public class App extends Application {
    @Override
    public void start(Stage primaryStage) {
        MainWindow mainWindow = new MainWindow();

        Scene scene = new Scene(mainWindow.getRoot(), 1280, 800);
        scene.getStylesheets().add(getClass()
            .getResource("/styles/logiclab.css").toExternalForm());

        primaryStage.setTitle("LogicLab — Circuit Simulator");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
```

### 7.4 CanvasView.java (Core Rendering)

```java
public class CanvasView extends Pane {
    private Canvas canvas;
    private GraphicsContext gc;
    private Circuit circuit;
    private AppMode mode;          // DRAW or SIMULATE

    // Drawing state
    private Component selectedComponent;
    private Pin wireStartPin;       // when drawing a wire
    private boolean drawingWire;
    private double mouseX, mouseY;

    // Zoom and pan
    private double zoom = 1.0;
    private double panX = 0, panY = 0;

    public CanvasView(Circuit circuit) {
        this.circuit = circuit;
        this.canvas = new Canvas(2000, 1500);
        this.gc = canvas.getGraphicsContext2D();
        getChildren().add(canvas);

        setupMouseHandlers();
        setupKeyHandlers();
    }

    private void setupMouseHandlers() {
        canvas.setOnMousePressed(this::handleMousePressed);
        canvas.setOnMouseDragged(this::handleMouseDragged);
        canvas.setOnMouseReleased(this::handleMouseReleased);
        canvas.setOnMouseMoved(this::handleMouseMoved);
        canvas.setOnScroll(this::handleScroll);  // zoom
    }

    private void handleMousePressed(MouseEvent e) {
        if (mode == AppMode.DRAW) {
            // Check if clicking a pin → start wire drawing
            // Check if clicking a component → select it for dragging
            // Check if clicking empty space → deselect
        } else if (mode == AppMode.SIMULATE) {
            // Check if clicking an Interactable component
            Component hit = findComponentAt(e.getX(), e.getY());
            if (hit instanceof Interactable) {
                ((Interactable) hit).onInteract();
                circuit.simulate();  // re-simulate after interaction
                redraw();
            }
        }
    }

    public void redraw() {
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        gc.save();
        gc.translate(panX, panY);
        gc.scale(zoom, zoom);

        // 1. Draw breadboard
        circuit.getBreadboard().render(gc);

        // 2. Draw wires
        for (Wire w : circuit.getWires()) {
            w.render(gc);
        }

        // 3. Draw components
        for (Component c : circuit.getComponents()) {
            c.render(gc);
        }

        // 4. Draw in-progress wire (if user is currently drawing one)
        if (drawingWire && wireStartPin != null) {
            gc.setStroke(Color.GRAY);
            gc.setLineDash(5, 5);
            gc.strokeLine(wireStartPin.getX(), wireStartPin.getY(), mouseX, mouseY);
            gc.setLineDash();
        }

        gc.restore();
    }

    private Component findComponentAt(double x, double y) {
        // Iterate components in reverse (top-most first)
        // Return first one where containsPoint(x, y) is true
    }

    private Pin findPinAt(double x, double y, double radius) {
        // Search all component pins for one within radius of (x, y)
    }
}

public enum AppMode {
    DRAW, SIMULATE
}
```

### 7.5 ToolboxPanel.java

```java
public class ToolboxPanel extends VBox {
    private Consumer<Component> onComponentSelected;  // callback

    public ToolboxPanel(Consumer<Component> onComponentSelected) {
        this.onComponentSelected = onComponentSelected;
        setPrefWidth(160);
        setSpacing(8);
        setPadding(new Insets(10));

        addCategory("Logic Gates");
        addDraggableItem("AND (7408)", () -> new ANDGate7408());
        addDraggableItem("OR (7432)",  () -> new ORGate7432());
        addDraggableItem("NOT (7404)", () -> new NOTGate7404());
        addDraggableItem("NAND (7400)", () -> new NANDGate7400());
        addDraggableItem("NOR (7402)", () -> new NORGate7402());
        addDraggableItem("XOR (7486)", () -> new XORGate7486());

        addCategory("Input");
        addDraggableItem("Toggle Switch", () -> new ToggleSwitch());
        addDraggableItem("Push Button",   () -> new PushButton());
        addDraggableItem("DIP Switch",    () -> new DIPSwitch());

        addCategory("Output");
        addDraggableItem("LED (Red)",   () -> new LED(Color.RED));
        addDraggableItem("LED (Green)", () -> new LED(Color.GREEN));

        addCategory("Power");
        addDraggableItem("5V Supply",  () -> new PowerSupply5V());
        addDraggableItem("Ground",     () -> new Ground());

        addCategory("Passive");
        addDraggableItem("Resistor", () -> new Resistor());
    }

    private void addCategory(String name) {
        Label label = new Label(name);
        label.setStyle("-fx-font-weight: bold; -fx-padding: 8 0 4 0;");
        getChildren().add(label);
    }

    private void addDraggableItem(String name, Supplier<Component> factory) {
        Button btn = new Button(name);
        btn.setPrefWidth(140);
        btn.setOnAction(e -> onComponentSelected.accept(factory.get()));
        getChildren().add(btn);
    }
}
```

### 7.6 PropertiesPanel.java

```java
public class PropertiesPanel extends VBox {
    private Label componentName;
    private VBox pinList;

    public void showProperties(Component c) {
        getChildren().clear();

        if (c == null) {
            getChildren().add(new Label("No selection"));
            return;
        }

        componentName = new Label(c.getName());
        componentName.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");
        getChildren().add(componentName);

        // List all pins with their current state
        for (Pin p : c.getPins()) {
            HBox row = new HBox(8);
            row.getChildren().addAll(
                new Label("Pin " + p.getLabel() + ":"),
                new Label(p.getState().toString())
            );
            getChildren().add(row);
        }

        // Component-specific properties
        if (c instanceof Resistor) {
            // Show resistance value, editable
        }
        if (c instanceof LED) {
            // Show color, on/off state
        }
    }
}
```

---

## 8. Draw Mode vs Simulate Mode

### Draw Mode

| Action | Behavior |
|--------|----------|
| Click toolbox item | Component attaches to cursor, click canvas to place |
| Click component on canvas | Select it (highlight, show in properties panel) |
| Drag component | Move it on the canvas |
| Click a pin | Start drawing a wire from that pin |
| Click another pin | Complete the wire (connect the two pins) |
| Right-click component | Delete it (and all connected wires) |
| Delete key | Remove selected component |
| Ctrl+Z | Undo last action |
| Ctrl+S | Save circuit |
| Ctrl+O | Load circuit |

### Simulate Mode

| Action | Behavior |
|--------|----------|
| Click ToggleSwitch | Flip it (HIGH ↔ LOW), re-simulate |
| Press PushButton | Set HIGH, re-simulate |
| Release PushButton | Set LOW, re-simulate |
| Click DIPSwitch bit | Toggle that bit, re-simulate |
| Hover pin | Tooltip shows pin label and current state |
| Components are NOT draggable in this mode | |

When switching from Draw → Simulate:
1. Run CircuitValidator to check for shorts and floating inputs
2. Show warnings in status bar if any
3. Run initial simulation
4. Redraw with signal colors

When switching from Simulate → Draw:
1. Wire colors revert to user-selected colors
2. LEDs go dim
3. Components become draggable again

---

## 9. Drag-and-Drop from Toolbox

```java
// In ToolboxPanel — when user clicks a component button:
onComponentSelected.accept(factory.get());

// In CanvasView — receives the component:
public void startPlacement(Component c) {
    this.placingComponent = c;
    canvas.setCursor(Cursor.CROSSHAIR);

    // Component follows mouse until clicked to place
    canvas.setOnMouseMoved(e -> {
        placingComponent.setPosition(e.getX(), e.getY());
        redraw();
        // Draw the component ghost at cursor position
    });

    canvas.setOnMouseClicked(e -> {
        try {
            validatePlacement(placingComponent);
            circuit.addComponent(placingComponent);
            snapToGrid(placingComponent);  // align to breadboard grid
            redraw();
        } catch (InvalidPlacementException ex) {
            showError(ex.getMessage());
        }
        // Reset to normal mouse handling
        this.placingComponent = null;
        canvas.setCursor(Cursor.DEFAULT);
        setupMouseHandlers();
    });
}

private void snapToGrid(Component c) {
    // Snap component position to nearest breadboard hole grid
    double gridSpacing = 20; // pixels between holes
    c.setPosition(
        Math.round(c.getX() / gridSpacing) * gridSpacing,
        Math.round(c.getY() / gridSpacing) * gridSpacing
    );
}
```

---

## 10. Wire Drawing

```java
// Phase 1: user clicks a pin to start
private void startWire(Pin pin) {
    wireStartPin = pin;
    drawingWire = true;
}

// While drawing: a dashed line follows cursor (rendered in redraw())

// Phase 2: user clicks another pin to finish
private void finishWire(Pin endPin) {
    try {
        // Validate
        if (wireStartPin == endPin) throw new InvalidConnectionException("Can't connect pin to itself");
        if (wireStartPin.getOwner() == endPin.getOwner())
            throw new InvalidConnectionException("Can't wire pins on the same component");

        Wire wire = new Wire(wireStartPin, endPin);
        circuit.addWire(wire);
        redraw();
    } catch (InvalidConnectionException ex) {
        showError(ex.getMessage());
    } finally {
        drawingWire = false;
        wireStartPin = null;
    }
}
```

---

## 11. Circuit Validation

Run before entering Simulate Mode.

```java
public class CircuitValidator {

    public static List<String> validate(Circuit circuit) {
        List<String> warnings = new ArrayList<>();

        // 1. Check for short circuits (VCC directly connected to GND through a net)
        for (Net net : circuit.getBreadboard().getAllNets()) {
            boolean hasHigh = false, hasLow = false;
            for (Pin p : net.getConnectedPins()) {
                if (p.getType() == PinType.POWER) hasHigh = true;
                if (p.getType() == PinType.GROUND) hasLow = true;
            }
            if (hasHigh && hasLow) {
                throw new CircuitShortException("Short circuit detected: power and ground on same net");
            }
        }

        // 2. Check for floating IC inputs
        for (Component c : circuit.getComponents()) {
            if (c instanceof ICChip) {
                for (Pin p : c.getPins()) {
                    if (p.getType() == PinType.INPUT && !isConnected(p, circuit)) {
                        warnings.add("Floating input: " + c.getName() + " pin " + p.getLabel());
                    }
                }
            }
        }

        // 3. Check for unpowered ICs
        for (Component c : circuit.getComponents()) {
            if (c instanceof ICChip) {
                Pin vcc = c.getPin("14");
                Pin gnd = c.getPin("7");
                if (!isConnected(vcc, circuit) || !isConnected(gnd, circuit)) {
                    warnings.add(c.getName() + " is not connected to power");
                }
            }
        }

        return warnings;
    }

    private static boolean isConnected(Pin pin, Circuit circuit) {
        // Check if this pin has any wire attached or is in a net with other pins
        for (Wire w : circuit.getWires()) {
            if (w.getStartPin() == pin || w.getEndPin() == pin) return true;
        }
        return false;
    }
}
```

---

## 12. Serialization (Save/Load)

All model classes implement `Serializable`. Transient fields (like JavaFX graphics objects) are excluded and rebuilt on load.

```java
// In Component:
private transient double canvasX, canvasY;  // NO — these must be saved
private transient GraphicsContext gc;         // YES — this is transient

// Save
public class FileController {
    public void save(Circuit circuit, Stage stage) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Circuit");
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("LogicLab Files", "*.llb")
        );
        File file = chooser.showSaveDialog(stage);

        if (file != null) {
            try {
                circuit.saveToFile(file);
            } catch (IOException e) {
                showError("Failed to save: " + e.getMessage());
            }
        }
    }

    public Circuit load(Stage stage) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open Circuit");
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("LogicLab Files", "*.llb")
        );
        File file = chooser.showOpenDialog(stage);

        if (file != null) {
            try {
                return Circuit.loadFromFile(file);
            } catch (IOException | ClassNotFoundException e) {
                throw new CircuitFileCorruptedException("Could not load file: " + e.getMessage());
            }
        }
        return null;
    }
}
```

---

## 13. Component Rendering Guide

### IC Chip (14-pin DIP)

```
       ┌──── notch
       v
   ┌───U───┐
 1 │       │ 14  (VCC)
 2 │       │ 13
 3 │ 7408  │ 12
 4 │  AND  │ 11
 5 │       │ 10
 6 │       │ 9
 7 │       │ 8
   └───────┘
 (GND)

- Black rectangle, ~60x100 pixels
- Notch/dot at top-left to indicate pin 1
- Pin circles on both sides, spaced to match breadboard grid
- Label centered
- In simulate mode, output pins show a small colored dot (green=HIGH, dark=LOW)
```

### LED

```
  Draw mode:        Simulate (ON):     Simulate (OFF):
  ┌──┐              ┌──┐               ┌──┐
  │  │ outline      │██│ filled+glow   │  │ dim
  └┬┬┘              └┬┬┘               └┬┬┘
   ││ anode/cathode   ││                 ││
```

### Toggle Switch

```
  OFF:          ON:
  ┌─────┐       ┌─────┐
  │ ○  ·│       │·  ● │
  └──┬──┘       └──┬──┘
     │ output      │
```

### Wire Colors (Simulate Mode)

| State    | Color            |
|----------|------------------|
| HIGH     | Bright green     |
| LOW      | Dark blue/black  |
| FLOATING | Gray, dashed     |

---

## 14. Gradle Build File

```groovy
plugins {
    id 'application'
    id 'org.openjfx.javafxplugin' version '0.1.0'
}

group = 'com.logiclab'
version = '1.0'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

javafx {
    version = '21'
    modules = ['javafx.controls', 'javafx.graphics']
}

application {
    mainClass = 'com.logiclab.App'
}

dependencies {
    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.0'
}
```

---

## 15. OOP Concept Map

| Course Topic | Where It Appears |
|---|---|
| Inheritance | Component → ICChip → ANDGate7408 (3 levels deep) |
| Encapsulation | Pin hides state logic, Component hides simulation math |
| Polymorphism | `circuit.simulate()` calls `.simulate()` on every component — each type behaves differently |
| Abstract classes | Component, ICChip, InputComponent, OutputComponent, PassiveComponent, PowerComponent |
| Interfaces | Renderable, Simulatable, Interactable |
| Composition | Circuit has-a Breadboard, Breadboard has-a grid of ContactPoints, ICChip has-a list of Pins |
| Method overloading | Component constructors (default, parameterized, copy) |
| Method overriding | Every subclass overrides simulate() and render() |
| Static members | ComponentFactory with static creation methods |
| Access modifiers | private fields, protected in abstract classes, public API |
| UML | The class hierarchy maps directly to a class diagram with inheritance, composition, and interface arrows |
| Exception handling | 6 custom exceptions for circuit errors, file errors, and user errors |
| File I/O + Serialization | Save/load circuits using ObjectOutputStream/ObjectInputStream |
| SOLID | Adding a new IC chip = one new class, nothing else changes (OCP). Each class has one job (SRP). Simulation engine depends on Component abstraction, not concrete types (DIP). |

---

## 16. Build Order (suggested implementation sequence)

1. **Pin, LogicState, Net** — the signal foundation
2. **Component abstract class** — base with pins and position
3. **Breadboard + ContactPoint + PowerRail** — the physical grid
4. **Wire** — connects pins
5. **PowerSupply5V, Ground** — first simulatable components
6. **Circuit with basic simulate()** — propagate power through wires
7. **CanvasView with breadboard rendering** — see something on screen
8. **ToolboxPanel** — drag components onto canvas
9. **ToggleSwitch, LED** — first input + output
10. **ANDGate7408** — first IC, test full signal chain: switch → IC → LED
11. **Remaining ICs** — one at a time, each is quick since the pattern exists
12. **Simulate Mode toggle** — wire colors, interaction
13. **PushButton, DIPSwitch** — more inputs
14. **Save/Load** — serialization
15. **CircuitValidator** — shorts, floating pins
16. **PropertiesPanel** — pin state display
17. **Polish** — zoom, pan, undo, error messages
