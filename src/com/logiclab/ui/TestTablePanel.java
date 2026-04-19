package com.logiclab.ui;

import com.logiclab.model.*;
import com.logiclab.model.input.DIPSwitch;
import com.logiclab.model.input.ToggleSwitch;
import com.logiclab.model.output.LED;
import com.logiclab.model.output.LightBar;
import com.logiclab.model.ports.InputPort;
import com.logiclab.model.ports.OutputPort;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Truth-table test panel. Scans the circuit for labelled input/output
 * signals, lets the user define test-case rows (H / L per signal), then
 * runs every row against the simulator and reports pass/fail.
 */
public class TestTablePanel extends VBox {

    // ---- data model --------------------------------------------------------

    /** One column in the truth table — maps to a single bit on a component. */
    static class SignalInfo {
        final Component component;
        final int bitIndex;      // -1 for single-pin components (ToggleSwitch, LED)
        final String label;
        final boolean isInput;

        SignalInfo(Component c, int bit, String label, boolean isInput) {
            this.component = c;
            this.bitIndex = bit;
            this.label = label;
            this.isInput = isInput;
        }
    }

    /** One row in the truth table. */
    static class TestRow {
        LogicState[] inputs;
        LogicState[] expected;   // null entry = don't-care
        LogicState[] actual;     // filled after a run
        Boolean passed;          // null = not yet run
    }

    // ---- fields ------------------------------------------------------------

    private Circuit circuit;
    private final Runnable onRedraw;

    private final List<SignalInfo> inputs  = new ArrayList<>();
    private final List<SignalInfo> outputs = new ArrayList<>();
    private final List<TestRow>   rows    = new ArrayList<>();

    private GridPane grid;
    private Label summaryLabel;

    // ---- styling constants -------------------------------------------------

    private static final String CELL_BASE =
            "-fx-padding: 4 10; -fx-border-color: #393b40; -fx-border-width: 0 0 1 1; " +
            "-fx-alignment: center; -fx-font-size: 12; -fx-font-family: 'Monospaced';";
    private static final String HEADER_STYLE =
            CELL_BASE + " -fx-background-color: #23242a; -fx-text-fill: #dfe1e5; -fx-font-weight: bold;";
    private static final String ROW_NUM_STYLE =
            CELL_BASE + " -fx-background-color: #1e1f22; -fx-text-fill: #868a91;";

    // ---- constructor -------------------------------------------------------

    public TestTablePanel(Circuit circuit, Runnable onRedraw) {
        this.circuit = circuit;
        this.onRedraw = onRedraw;

        setStyle("-fx-background-color: " + Theme.BG_CHROME +
                "; -fx-border-color: " + Theme.BORDER_SOFT + " transparent transparent transparent;");
        setPadding(new Insets(6, 10, 6, 10));
        setSpacing(6);

        // -- button bar --
        HBox bar = new HBox(8);
        bar.setAlignment(Pos.CENTER_LEFT);

        Button refreshBtn = styledButton("Refresh Signals");
        refreshBtn.setOnAction(e -> { refreshSignals(); rebuildGrid(); saveTestsToCircuit(); });

        Button addBtn = styledButton("Add Row");
        addBtn.setOnAction(e -> { addRow(); rebuildGrid(); saveTestsToCircuit(); });

        Button removeBtn = styledButton("Remove Last Row");
        removeBtn.setOnAction(e -> { removeLastRow(); rebuildGrid(); saveTestsToCircuit(); });

        Button autoFillBtn = styledButton("Auto Fill");
        autoFillBtn.setOnAction(e -> { autoFillCombinations(); rebuildGrid(); saveTestsToCircuit(); });

        Button runBtn = styledButton("Run All Tests");
        runBtn.setStyle(runBtn.getStyle().replace(Theme.BTN_BG, Theme.ACCENT_BLUE));
        runBtn.setOnAction(e -> { runAllTests(); rebuildGrid(); saveTestsToCircuit(); });

        summaryLabel = new Label("");
        summaryLabel.setStyle("-fx-text-fill: " + Theme.TEXT_SECONDARY + "; -fx-font-size: 12;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        bar.getChildren().addAll(refreshBtn, addBtn, removeBtn, autoFillBtn, runBtn, spacer, summaryLabel);
        getChildren().add(bar);

        // -- guidance --
        Label guide = new Label(
                "Click input cells to toggle between 1 and 0.  " +
                "Click output cells to cycle: 1 \u2192 0 \u2192 D (don't care) \u2192 1.  " +
                "Auto Fill generates all 2\u207f input combinations.");
        guide.setStyle("-fx-text-fill: " + Theme.TEXT_MUTED + "; -fx-font-size: 10; -fx-padding: 0 0 2 0;");
        guide.setWrapText(true);
        getChildren().add(guide);

        // -- table area --
        grid = new GridPane();
        grid.setStyle("-fx-background-color: #1e1f22;");

        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: #1e1f22; -fx-background: #1e1f22;");
        scroll.setPrefHeight(250);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        getChildren().add(scroll);

        refreshSignals();
        loadSavedTests();
        if (rows.isEmpty()) addRow();
        rebuildGrid();
    }

    public void setCircuit(Circuit circuit) {
        this.circuit = circuit;
        refreshSignals();
        loadSavedTests();
        if (rows.isEmpty()) addRow();
        rebuildGrid();
    }

    // ---- signal discovery --------------------------------------------------

    void refreshSignals() {
        inputs.clear();
        outputs.clear();

        int swIdx = 1;
        int ledIdx = 1;
        for (Component c : circuit.getComponents()) {
            if (c instanceof ToggleSwitch) {
                String lbl = (c.getDisplayLabel() != null && !c.getDisplayLabel().isEmpty())
                        ? c.getDisplayLabel() : "SW" + swIdx;
                inputs.add(new SignalInfo(c, -1, lbl, true));
                swIdx++;
            } else if (c instanceof DIPSwitch) {
                DIPSwitch dip = (DIPSwitch) c;
                for (int i = 0; i < dip.getNumSwitches(); i++) {
                    String tag = dip.getTag(i);
                    String label = (tag != null && !tag.isEmpty()) ? tag : "D" + swIdx + "[" + (i + 1) + "]";
                    inputs.add(new SignalInfo(c, i, label, true));
                }
                swIdx++;
            } else if (c instanceof LED) {
                String lbl = (c.getDisplayLabel() != null && !c.getDisplayLabel().isEmpty())
                        ? c.getDisplayLabel() : "LED" + ledIdx;
                outputs.add(new SignalInfo(c, -1, lbl, false));
                ledIdx++;
            } else if (c instanceof LightBar) {
                LightBar lb = (LightBar) c;
                for (int i = 0; i < lb.getLightCount(); i++) {
                    String tag = lb.getTag(i);
                    String label = (tag != null && !tag.isEmpty()) ? tag : "LB" + ledIdx + "[" + i + "]";
                    outputs.add(new SignalInfo(c, i, label, false));
                }
                ledIdx++;
            } else if (c instanceof InputPort ip) {
                String lbl = (ip.getPortLabel() != null && !ip.getPortLabel().isEmpty())
                        ? ip.getPortLabel() : "IN" + swIdx;
                inputs.add(new SignalInfo(c, -1, lbl, true));
                swIdx++;
            } else if (c instanceof OutputPort op) {
                String lbl = (op.getPortLabel() != null && !op.getPortLabel().isEmpty())
                        ? op.getPortLabel() : "OUT" + ledIdx;
                outputs.add(new SignalInfo(c, -1, lbl, false));
                ledIdx++;
            }
        }

        // Trim or extend existing rows to match new signal counts
        for (TestRow r : rows) {
            r.inputs   = resizeArray(r.inputs,   inputs.size(),  LogicState.LOW);
            r.expected = resizeArray(r.expected,  outputs.size(), null);
            r.actual   = new LogicState[outputs.size()];
            r.passed   = null;
        }
    }

    // ---- persistence --------------------------------------------------------

    private void loadSavedTests() {
        List<LogicState[]> si = circuit.getSavedTestInputs();
        List<LogicState[]> se = circuit.getSavedTestExpected();
        if (si == null || se == null || si.size() != se.size()) return;
        rows.clear();
        for (int r = 0; r < si.size(); r++) {
            TestRow row = new TestRow();
            row.inputs   = resizeArray(si.get(r), inputs.size(), LogicState.LOW);
            row.expected = resizeArray(se.get(r), outputs.size(), null);
            row.actual   = new LogicState[outputs.size()];
            rows.add(row);
        }
    }

    private void saveTestsToCircuit() {
        List<LogicState[]> si = new ArrayList<>();
        List<LogicState[]> se = new ArrayList<>();
        for (TestRow r : rows) {
            si.add(r.inputs != null ? r.inputs.clone() : new LogicState[inputs.size()]);
            se.add(r.expected != null ? r.expected.clone() : new LogicState[outputs.size()]);
        }
        circuit.setSavedTests(si, se);
    }

    // ---- row management ----------------------------------------------------

    private void addRow() {
        TestRow r = new TestRow();
        r.inputs   = new LogicState[inputs.size()];
        r.expected = new LogicState[outputs.size()];
        r.actual   = new LogicState[outputs.size()];
        for (int i = 0; i < inputs.size(); i++)  r.inputs[i] = LogicState.LOW;
        // expected starts as null (don't-care / X)
        rows.add(r);
    }

    private void removeLastRow() {
        if (!rows.isEmpty()) rows.remove(rows.size() - 1);
    }

    /** Replaces the current rows with every possible input combination (2^n rows). */
    private void autoFillCombinations() {
        int n = inputs.size();
        if (n == 0) return;
        if (n > 16) {
            // Safety cap — 2^16 = 65 536 rows is already a lot
            summaryLabel.setText("Too many inputs (" + n + ") for auto fill (max 16)");
            summaryLabel.setStyle("-fx-text-fill: " + Theme.ACCENT_YELLOW + "; -fx-font-size: 12;");
            return;
        }
        int totalRows = 1 << n;
        rows.clear();
        for (int combo = 0; combo < totalRows; combo++) {
            TestRow r = new TestRow();
            r.inputs   = new LogicState[n];
            r.expected = new LogicState[outputs.size()];
            r.actual   = new LogicState[outputs.size()];
            for (int bit = 0; bit < n; bit++) {
                // MSB is the leftmost column (bit index 0)
                boolean high = ((combo >> (n - 1 - bit)) & 1) == 1;
                r.inputs[bit] = high ? LogicState.HIGH : LogicState.LOW;
            }
            // expected outputs default to don't-care
            rows.add(r);
        }
    }

    // ---- grid rebuild ------------------------------------------------------

    private void rebuildGrid() {
        grid.getChildren().clear();
        grid.getColumnConstraints().clear();

        if (inputs.isEmpty() && outputs.isEmpty()) {
            Label msg = new Label("No input/output components found. Add components to the circuit first.");
            msg.setStyle("-fx-text-fill: " + Theme.TEXT_SECONDARY + "; -fx-font-size: 12; -fx-padding: 20;");
            grid.add(msg, 0, 0);
            summaryLabel.setText("");
            return;
        }

        int colCount = 1 + inputs.size() + 1 + outputs.size() + 1; // # | inputs | sep | outputs | result

        // --- Row 0: group headers ---
        Label hashHdr = headerCell("#");
        grid.add(hashHdr, 0, 0, 1, 2); // span 2 rows

        if (!inputs.isEmpty()) {
            Label ih = headerCell("Inputs");
            ih.setStyle(HEADER_STYLE + " -fx-border-width: 0 0 0 1;");
            grid.add(ih, 1, 0, inputs.size(), 1);
        }

        // separator column
        Label sep = new Label();
        sep.setMinWidth(6);
        sep.setMaxWidth(6);
        sep.setStyle("-fx-background-color: #3574f0; -fx-border-width: 0;");
        grid.add(sep, 1 + inputs.size(), 0, 1, rows.size() + 2);

        if (!outputs.isEmpty()) {
            Label oh = headerCell("Expected Outputs");
            grid.add(oh, 2 + inputs.size(), 0, outputs.size(), 1);
        }

        Label resHdr = headerCell("Result");
        grid.add(resHdr, 2 + inputs.size() + outputs.size(), 0, 1, 2);

        // --- Row 1: individual signal headers ---
        for (int i = 0; i < inputs.size(); i++) {
            grid.add(headerCell(inputs.get(i).label), 1 + i, 1);
        }
        for (int i = 0; i < outputs.size(); i++) {
            grid.add(headerCell(outputs.get(i).label), 2 + inputs.size() + i, 1);
        }

        // --- Data rows ---
        for (int r = 0; r < rows.size(); r++) {
            TestRow row = rows.get(r);
            final int rowIdx = r;
            int gridRow = r + 2;

            // Row number
            Label rn = new Label(String.valueOf(r + 1));
            rn.setMaxWidth(Double.MAX_VALUE);
            rn.setAlignment(Pos.CENTER);
            rn.setStyle(ROW_NUM_STYLE);
            grid.add(rn, 0, gridRow);

            // Input cells
            for (int i = 0; i < inputs.size(); i++) {
                final int col = i;
                Label cell = stateCell(row.inputs[i], false, false);
                cell.setOnMouseClicked(e -> {
                    row.inputs[col] = (row.inputs[col] == LogicState.LOW) ? LogicState.HIGH : LogicState.LOW;
                    row.passed = null;
                    updateStateCell(cell, row.inputs[col], false, false);
                    saveTestsToCircuit();
                });
                grid.add(cell, 1 + i, gridRow);
            }

            // Output cells
            for (int i = 0; i < outputs.size(); i++) {
                final int col = i;
                boolean mismatch = row.passed != null && row.expected[col] != null
                        && row.actual[col] != null && row.expected[col] != row.actual[col];
                Label cell = stateCell(row.expected[i], true, mismatch);

                // Show actual value on mismatch
                if (mismatch) {
                    cell.setText(stateText(row.expected[col]) + " (" + stateText(row.actual[col]) + ")");
                }

                cell.setOnMouseClicked(e -> {
                    row.expected[col] = cycleExpected(row.expected[col]);
                    row.passed = null;
                    updateStateCell(cell, row.expected[col], true, false);
                    saveTestsToCircuit();
                });
                grid.add(cell, 2 + inputs.size() + i, gridRow);
            }

            // Result cell
            Label res = resultCell(row);
            grid.add(res, 2 + inputs.size() + outputs.size(), gridRow);
        }

        updateSummary();
    }

    // ---- test runner -------------------------------------------------------

    private void runAllTests() {
        if (inputs.isEmpty() && outputs.isEmpty()) return;

        // Save original input states
        Map<ToggleSwitch, Boolean> savedToggles = new HashMap<>();
        Map<DIPSwitch, boolean[]> savedDips = new HashMap<>();
        for (SignalInfo sig : inputs) {
            if (sig.component instanceof ToggleSwitch && !savedToggles.containsKey(sig.component)) {
                savedToggles.put((ToggleSwitch) sig.component, ((ToggleSwitch) sig.component).isOn());
            } else if (sig.component instanceof DIPSwitch && !savedDips.containsKey(sig.component)) {
                DIPSwitch d = (DIPSwitch) sig.component;
                boolean[] s = new boolean[d.getNumSwitches()];
                for (int i = 0; i < s.length; i++) s[i] = d.isSwitchOn(i);
                savedDips.put(d, s);
            }
        }

        // Run each test row
        for (TestRow row : rows) {
            // Apply inputs
            for (int i = 0; i < inputs.size(); i++) {
                SignalInfo sig = inputs.get(i);
                boolean high = row.inputs[i] == LogicState.HIGH;
                if (sig.component instanceof ToggleSwitch) {
                    ((ToggleSwitch) sig.component).setState(high);
                } else if (sig.component instanceof DIPSwitch) {
                    ((DIPSwitch) sig.component).setSwitch(sig.bitIndex, high);
                } else if (sig.component instanceof InputPort ip) {
                    ip.setExternalState(high ? LogicState.HIGH : LogicState.LOW);
                }
            }

            // Simulate
            try {
                circuit.simulate();
            } catch (Exception ex) {
                // Mark row as failed on simulation error
                row.passed = false;
                row.actual = new LogicState[outputs.size()];
                continue;
            }

            // Read outputs and compare
            row.passed = true;
            for (int i = 0; i < outputs.size(); i++) {
                SignalInfo sig = outputs.get(i);
                LogicState actual;
                if (sig.component instanceof LED) {
                    actual = ((LED) sig.component).isActive() ? LogicState.HIGH : LogicState.LOW;
                } else if (sig.component instanceof LightBar) {
                    LogicState raw = ((LightBar) sig.component).getPin("IN" + sig.bitIndex).getState();
                    actual = (raw == LogicState.HIGH) ? LogicState.HIGH : LogicState.LOW;
                } else if (sig.component instanceof OutputPort op) {
                    LogicState raw = op.readState();
                    actual = (raw == LogicState.HIGH) ? LogicState.HIGH : LogicState.LOW;
                } else {
                    actual = LogicState.LOW;
                }
                row.actual[i] = actual;

                // Compare (null expected = don't care)
                if (row.expected[i] != null && row.expected[i] != actual) {
                    row.passed = false;
                }
            }
        }

        // Restore original states
        for (Map.Entry<ToggleSwitch, Boolean> e : savedToggles.entrySet()) {
            e.getKey().setState(e.getValue());
        }
        for (Map.Entry<DIPSwitch, boolean[]> e : savedDips.entrySet()) {
            DIPSwitch d = e.getKey();
            boolean[] s = e.getValue();
            for (int i = 0; i < s.length; i++) d.setSwitch(i, s[i]);
        }
        try {
            circuit.simulate();
        } catch (Exception ignored) {}

        if (onRedraw != null) onRedraw.run();
    }

    // ---- cell factories ----------------------------------------------------

    private Label headerCell(String text) {
        Label l = new Label(text);
        l.setMaxWidth(Double.MAX_VALUE);
        l.setAlignment(Pos.CENTER);
        l.setStyle(HEADER_STYLE);
        return l;
    }

    private Label stateCell(LogicState state, boolean allowX, boolean mismatch) {
        Label l = new Label(stateText(state));
        l.setMaxWidth(Double.MAX_VALUE);
        l.setAlignment(Pos.CENTER);
        l.setCursor(javafx.scene.Cursor.HAND);
        updateStateCell(l, state, allowX, mismatch);
        return l;
    }

    private void updateStateCell(Label l, LogicState state, boolean allowX, boolean mismatch) {
        l.setText(stateText(state));
        String bg = mismatch ? "#3d1f1f" : "#2b2d30";
        String fg;
        if (state == null) {
            fg = Theme.TEXT_MUTED;
        } else if (state == LogicState.HIGH) {
            fg = Theme.ACCENT_GREEN;
        } else {
            fg = Theme.ACCENT_BLUE;
        }
        l.setStyle(CELL_BASE + " -fx-background-color: " + bg + "; -fx-text-fill: " + fg + "; -fx-cursor: hand;");
    }

    private Label resultCell(TestRow row) {
        Label l = new Label();
        l.setMaxWidth(Double.MAX_VALUE);
        l.setAlignment(Pos.CENTER);
        if (row.passed == null) {
            l.setText("-");
            l.setStyle(CELL_BASE + " -fx-background-color: #2b2d30; -fx-text-fill: " + Theme.TEXT_MUTED + ";");
        } else if (row.passed) {
            l.setText("PASS");
            l.setStyle(CELL_BASE + " -fx-background-color: #1a3322; -fx-text-fill: " + Theme.ACCENT_GREEN + "; -fx-font-weight: bold;");
        } else {
            l.setText("FAIL");
            l.setStyle(CELL_BASE + " -fx-background-color: #3d1f1f; -fx-text-fill: " + Theme.ACCENT_RED + "; -fx-font-weight: bold;");
        }
        return l;
    }

    // ---- helpers ------------------------------------------------------------

    private static String stateText(LogicState s) {
        if (s == null)                return "D";
        if (s == LogicState.HIGH)     return "1";
        return "0";
    }

    private static LogicState cycleExpected(LogicState current) {
        if (current == null)               return LogicState.HIGH;
        if (current == LogicState.HIGH)     return LogicState.LOW;
        return null; // LOW -> don't-care
    }

    private static LogicState[] resizeArray(LogicState[] old, int newLen, LogicState fill) {
        LogicState[] a = new LogicState[newLen];
        if (old != null) {
            int copy = Math.min(old.length, newLen);
            System.arraycopy(old, 0, a, 0, copy);
        }
        for (int i = (old == null ? 0 : Math.min(old.length, newLen)); i < newLen; i++) {
            a[i] = fill;
        }
        return a;
    }

    private void updateSummary() {
        int pass = 0, fail = 0, pending = 0;
        for (TestRow r : rows) {
            if (r.passed == null)     pending++;
            else if (r.passed)        pass++;
            else                      fail++;
        }
        if (pending == rows.size()) {
            summaryLabel.setText(rows.size() + " test case(s)");
            summaryLabel.setStyle("-fx-text-fill: " + Theme.TEXT_SECONDARY + "; -fx-font-size: 12;");
        } else {
            String color = fail > 0 ? Theme.ACCENT_RED : Theme.ACCENT_GREEN;
            summaryLabel.setText(pass + " passed, " + fail + " failed" + (pending > 0 ? ", " + pending + " pending" : ""));
            summaryLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12; -fx-font-weight: bold;");
        }
    }

    private Button styledButton(String text) {
        Button btn = new Button(text);
        String idle = "-fx-background-color: " + Theme.BTN_BG + "; -fx-text-fill: " + Theme.TEXT_PRIMARY +
                "; -fx-font-size: 11; -fx-background-radius: 6; -fx-padding: 5 12; -fx-cursor: hand;";
        String hover = "-fx-background-color: " + Theme.BTN_HOVER + "; -fx-text-fill: #ffffff; " +
                "-fx-font-size: 11; -fx-background-radius: 6; -fx-padding: 5 12; -fx-cursor: hand;";
        btn.setStyle(idle);
        btn.setOnMouseEntered(e -> { if (!btn.getStyle().contains(Theme.ACCENT_BLUE)) btn.setStyle(hover); });
        btn.setOnMouseExited(e -> { if (!btn.getStyle().contains(Theme.ACCENT_BLUE)) btn.setStyle(idle); });
        return btn;
    }
}
