# LogicLab — Project Guidance

## Demo-circuit generation rules

When writing or editing demo circuits (`src/com/logiclab/util/DemoLibrary.java`,
`src/com/logiclab/DemoBuilder.java`, `src/com/logiclab/DemoIcBuilder.java`, or
anywhere a `Circuit` is built programmatically), follow these rules. They exist
to keep the rendered board readable — prior demos had heavy wire overlap and
were "a mess to look at."

### 1. Power rail convention — never mix halves

- **Top + rail carries 5V. Bottom − rail carries GND.** Top − and bottom +
  rails stay unused.
- `PowerSupply5V` sits above the board (e.g. `(holeX(N) - 17, boardY - 40)`)
  so its pin reaches the top + rail with a short vertical wire.
- `Ground` sits below the board (e.g. `(holeX(N) - 17, boardY + 315)`) so its
  pin reaches the bottom − rail with a short vertical wire.
- IC VCC (pin 14) joins the top + rail via a short stalk on the chip's own
  column using row `a` (letter 0). IC GND (pin 7) joins the bottom − rail via
  a short stalk on column `icCol+6` using row `j` (letter 9). Never run 5V on
  the bottom rail then jump it to the top, or vice versa.

### 2. Bit ordering — leftmost is MSB

For any array-style input or output component (`DIPSwitch`, `LightBar`,
`BinaryToBCDDisplay`, etc.), the **leftmost physical pin is the most
significant bit**, the rightmost is the least significant. So
`DIPSwitch.OUT1 = MSB` and `LightBar.IN0 = MSB`. A 4-bit bus wired as
`OUT1..OUT4 → B3..B0` and read back as `IN0..IN3 → B3..B0`.

### 3. Route around ICs, never through them — use waypoints & lanes

Never leave a bus wire as a single straight line that clips through a chip
footprint. Two tools make that easy:

- **`route(wire, laneY)` helper** — adds two orthogonal waypoints so the wire
  goes `start → (startX, laneY) → (endX, laneY) → end`. Use `belowBoard(bb, idx)`
  / `aboveBoard(bb, idx)` to pick a unique Y lane for each wire, so their
  horizontal runs don't stack on top of each other.
- **Dedicate lanes.** When several wires share a region below or above the
  board, each should get its own `idx` so they sit on distinct Y levels.

### 4. Prefer short on-board hops over big U-shapes — `hop()`

This is the single biggest win for readability. If two holes share the same
net half (both on the f-j net, or both on the a-e net), you don't need to
loop above or below the board. Drop a single horizontal jumper on a middle
row and be done:

- **`hop(bb, colA, colB, letter)`** — returns a direct wire between
  `(colA, letter)` and `(colB, letter)`. No waypoints, no rails, just a
  short line on the board.
- **f-net hops** (pin 1-7 side): use `letter = 6` (row `g`), falling back
  to `7` (`h`) or `8` (`i`) when two hops on the same row would stack.
- **e-net hops** (pin 14-8 side): use `letter = 3` (row `d`), falling back
  to `2` (`c`) or `1` (`b`).
- Chain hops through a shared node when a bus fans out: e.g.
  `hop(bb, colA, xorCol, 6); hop(bb, xorCol, andCol, 6);` ties
  `colA → xor pin 1 → and pin 1` as one continuous row-`g` trace.

When to stick with `route(... belowBoard/aboveBoard)`:
- Wire crosses between f-net and e-net halves (source row 9, target row 0
  or vice versa) — it genuinely has to detour around the chip middle gap.
- Wire crosses between two breadboards.
- External device (switch, DIP, LED, LightBar, supply) has to reach a
  column hole or a rail.

### 5. Signal landing rows

- Signals coming **from below the board** (switches, DIP, LEDs via cathode)
  land on **row `j` (letter 9)** of the target column — they share the f-j
  net with chip pins 1-7.
- Signals coming **from above the board** (rails, crossings) land on
  **row `a` (letter 0)** — they share the a-e net with chip pins 14-8.
- This keeps the vertical stalk between the device and the column short.

### 6. Chip placement

- Place ICs with `ic.setPosition(bb.getHoleX(icCol) - 10, bb.getHoleY(4))` —
  pin 14 at column `icCol` row `e`, pin 7 at column `icCol+6` row `f`.
- Use `attachIcSlots(c, bb, ic, icCol)` to wire each IC pin to the HolePin
  beneath it (column net membership).
- Use `wireIcPower(c, bb, icCol)` for the VCC/GND stalks.
- Call `ensurePositions(bb)` once before touching any rail/hole `ContactPoint`
  (it paints the board to a throwaway canvas so coordinates get initialized).

### 7. No simple single-gate demos

Don't generate trivial "two switches → one gate → one LED" demos (AND, OR,
NAND, XOR, NOT). The demo library focuses on multi-IC circuits (Half Adder,
Binary↔Gray, Ripple Adder, …) that actually exercise the simulator and show
off cleaner wire routing. Single-gate demos belong in unit tests, not the
Demos panel.
