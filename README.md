# Trace

A JavaFX-based digital logic circuit simulator. Build circuits on a virtual breadboard using switches, gates, ICs, LEDs, and more — then simulate or step through them in trace mode.

## About

Trace models a physical breadboard rather than a schematic. Components are placed on holes, wires run between rails and columns, and the simulator respects real-world constraints — floating inputs, shorts, and oscillation are all detected and reported as errors.

**What's included:**

- **Breadboard + power rails** with 5V supply and ground components
- **7400-series ICs**: 7400 NAND, 7402 NOR, 7404 NOT, 7408 AND, 7410/7411 variants, 7427 NOR, 7432 OR, 7483 4-bit adder, 7486 XOR
- **Inputs**: toggle switches, push buttons, DIP switches
- **Outputs**: LEDs, light bars, 7-segment and binary-to-BCD displays
- **Flip-flops** (SR) and passive components (resistors)
- **Sub-circuits** — package a circuit as a reusable block, save/load from a library
- **Three modes**: draw, simulate, and test (table-driven truth-table verification)
- **Undo/redo**, recent projects, demo circuit library, and JSON-based `.trc` / `.trs` file formats

## Requirements

- **JDK 21** (the Gradle toolchain will try to provision it automatically if missing)
- Windows, macOS, or Linux
- Git

JavaFX 21 is pulled in by the Gradle build — you do **not** need a separate JavaFX SDK install.

## Download

```bash
git clone https://github.com/NotHamxa/Trace.git
cd LogicLab
```

## Run

Use the Gradle wrapper to launch the app:

```bash
./gradlew run
```

On Windows (cmd / PowerShell):

```bat
gradlew.bat run
```
