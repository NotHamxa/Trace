package com.trace.ui;

import com.trace.model.*;
import com.trace.model.input.DIPSwitch;
import com.trace.model.output.LED;
import com.trace.model.output.LightBar;
import com.trace.model.passive.Resistor;
import com.trace.model.ports.InputPort;
import com.trace.model.ports.OutputPort;
import com.trace.model.subcircuit.SubCircuitInstance;
import java.util.List;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

public class PropertiesPanel extends VBox {

    /** Called when the user flips the Locked checkbox — forwarded to CanvasView. */
    private Consumer<Boolean> onLockToggled;

    /** Called whenever a DIP switch tag is edited — forwarded to CanvasView for redraw. */
    private Runnable onComponentChanged;

    /** Called when an in-progress edit is committed (focus lost / Enter) — snapshots undo. */
    private Runnable onTagCommitted;

    /** Called when the user picks a new pin side for an input component. */
    private Consumer<PinSide> onPinSideChanged;

    /** Called when the user clicks "View Internals" on a sub-circuit instance. */
    private Consumer<SubCircuitInstance> onViewSubCircuit;

    public PropertiesPanel() {
        setPrefWidth(180);
        setMinWidth(160);
        setPadding(new Insets(10));
        setSpacing(6);
        setStyle("-fx-background-color: " + Theme.BG_CHROME +
                "; -fx-border-color: " + Theme.BORDER_SOFT + " transparent transparent " + Theme.BORDER_SOFT + ";");

        Label title = new Label("Properties");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14; -fx-text-fill: " + Theme.TEXT_PRIMARY + ";");
        getChildren().add(title);
        getChildren().add(new Separator());

        Label placeholder = new Label("No selection");
        placeholder.setStyle("-fx-text-fill: " + Theme.TEXT_SECONDARY + ";");
        getChildren().add(placeholder);
    }

    public void setOnLockToggled(Consumer<Boolean> handler) {
        this.onLockToggled = handler;
    }

    public void setOnComponentChanged(Runnable handler) {
        this.onComponentChanged = handler;
    }

    public void setOnTagCommitted(Runnable handler) {
        this.onTagCommitted = handler;
    }

    public void setOnPinSideChanged(Consumer<PinSide> handler) {
        this.onPinSideChanged = handler;
    }

    public void setOnViewSubCircuit(Consumer<SubCircuitInstance> handler) {
        this.onViewSubCircuit = handler;
    }

    /** Rebuilds the panel for a selected wire. */
    public void showWireProperties(Wire w) {
        getChildren().clear();
        Label title = new Label("Properties");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14; -fx-text-fill: " + Theme.TEXT_PRIMARY + ";");
        getChildren().add(title);
        getChildren().add(new Separator());

        if (w == null) {
            Label noSel = new Label("No selection");
            noSel.setStyle("-fx-text-fill: " + Theme.TEXT_SECONDARY + ";");
            getChildren().add(noSel);
            return;
        }

        Label nameLabel = new Label("Wire");
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13; -fx-text-fill: " + Theme.TEXT_PRIMARY + ";");
        getChildren().add(nameLabel);

        Label stateLabel = new Label("State: " + w.getCurrentState());
        String stateColor;
        switch (w.getCurrentState()) {
            case HIGH: stateColor = Theme.ACCENT_GREEN; break;
            case LOW:  stateColor = Theme.ACCENT_BLUE; break;
            default:   stateColor = Theme.TEXT_SECONDARY; break;
        }
        stateLabel.setStyle("-fx-text-fill: " + stateColor + "; -fx-font-size: 11;");
        getChildren().add(stateLabel);

        getChildren().add(new Separator());
        getChildren().add(buildLockedCheckbox(w.isLocked()));
    }

    private CheckBox buildLockedCheckbox(boolean locked) {
        CheckBox cb = new CheckBox("Locked");
        cb.setSelected(locked);
        cb.setStyle("-fx-text-fill: " + Theme.TEXT_PRIMARY + "; -fx-font-size: 11;");
        cb.setOnAction(e -> {
            if (onLockToggled != null) onLockToggled.accept(cb.isSelected());
        });
        return cb;
    }

    public void showProperties(Component c) {
        getChildren().clear();

        Label title = new Label("Properties");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14; -fx-text-fill: " + Theme.TEXT_PRIMARY + ";");
        getChildren().add(title);
        getChildren().add(new Separator());

        if (c == null) {
            Label noSel = new Label("No selection");
            noSel.setStyle("-fx-text-fill: " + Theme.TEXT_SECONDARY + ";");
            getChildren().add(noSel);
            return;
        }

        // Component name
        Label nameLabel = new Label(c.getName());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13; -fx-text-fill: " + Theme.TEXT_PRIMARY + ";");
        getChildren().add(nameLabel);

        // Component ID
        Label idLabel = new Label("ID: " + c.getId());
        idLabel.setStyle("-fx-text-fill: " + Theme.TEXT_SECONDARY + "; -fx-font-size: 10;");
        getChildren().add(idLabel);

        getChildren().add(new Separator());

        // Pin states
        Label pinsHeader = new Label("Pins:");
        pinsHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: " + Theme.TEXT_PRIMARY + "; -fx-font-size: 11;");
        getChildren().add(pinsHeader);

        for (Pin p : c.getPins()) {
            HBox row = new HBox(8);
            Label pinLabel = new Label("Pin " + p.getLabel() + ":");
            pinLabel.setStyle("-fx-text-fill: " + Theme.TEXT_PRIMARY + "; -fx-font-size: 11;");
            pinLabel.setMinWidth(60);

            Label stateLabel = new Label(p.getState().toString());
            String stateColor;
            switch (p.getState()) {
                case HIGH: stateColor = Theme.ACCENT_GREEN; break;
                case LOW:  stateColor = Theme.ACCENT_BLUE; break;
                default:   stateColor = Theme.TEXT_SECONDARY; break;
            }
            stateLabel.setStyle("-fx-text-fill: " + stateColor + "; -fx-font-size: 11; -fx-font-weight: bold;");

            row.getChildren().addAll(pinLabel, stateLabel);
            getChildren().add(row);
        }

        // Gate routing map for IC chips
        if (c instanceof ICChip) {
            ICChip ic = (ICChip) c;
            List<String> gateDescs = ic.getGateDescriptions();
            if (!gateDescs.isEmpty()) {
                getChildren().add(new Separator());
                Label gateHeader = new Label("Gate Map:");
                gateHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: " + Theme.TEXT_PRIMARY + "; -fx-font-size: 11;");
                getChildren().add(gateHeader);

                for (String desc : gateDescs) {
                    Label gl = new Label(desc);
                    gl.setStyle("-fx-text-fill: " + Theme.TEXT_SECONDARY + "; -fx-font-size: 10; -fx-font-family: 'Monospaced';");
                    gl.setWrapText(true);
                    getChildren().add(gl);
                }
            }
        }

        // Pin-side chooser for any input component
        if (c instanceof InputComponent) {
            getChildren().add(new Separator());
            Label pinSideHeader = new Label("Pin Side:");
            pinSideHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: " + Theme.TEXT_PRIMARY + "; -fx-font-size: 11;");
            getChildren().add(pinSideHeader);

            ComboBox<PinSide> sideCombo = new ComboBox<>();
            sideCombo.getItems().addAll(PinSide.TOP, PinSide.BOTTOM, PinSide.LEFT, PinSide.RIGHT);
            sideCombo.setValue(((InputComponent) c).getPinSide());
            sideCombo.setPrefWidth(140);
            sideCombo.setStyle("-fx-font-size: 11;");
            sideCombo.setOnAction(e -> {
                PinSide picked = sideCombo.getValue();
                if (picked != null && onPinSideChanged != null) {
                    onPinSideChanged.accept(picked);
                }
            });
            getChildren().add(sideCombo);
        }

        // Component-specific properties
        if (c instanceof Resistor) {
            getChildren().add(new Separator());
            Label resLabel = new Label("Resistance: " + ((Resistor) c).getResistance() + "\u03A9");
            resLabel.setStyle("-fx-text-fill: " + Theme.TEXT_PRIMARY + "; -fx-font-size: 11;");
            getChildren().add(resLabel);
        }

        if (c instanceof LED) {
            getChildren().add(new Separator());
            boolean on = ((LED) c).isActive();
            Label ledLabel = new Label("State: " + (on ? "ON" : "OFF"));
            ledLabel.setStyle("-fx-text-fill: " + (on ? Theme.ACCENT_GREEN : Theme.TEXT_SECONDARY) +
                    "; -fx-font-size: 11;");
            getChildren().add(ledLabel);
        }

        if (c instanceof DIPSwitch) {
            getChildren().add(new Separator());
            Label tagsHeader = new Label("Tags:");
            tagsHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: " + Theme.TEXT_PRIMARY + "; -fx-font-size: 11;");
            getChildren().add(tagsHeader);

            DIPSwitch dip = (DIPSwitch) c;
            int n = dip.getNumSwitches();
            for (int i = 0; i < n; i++) {
                final int idx = i;
                HBox row = new HBox(6);
                Label lbl = new Label((i + 1) + ":");
                lbl.setStyle("-fx-text-fill: " + Theme.TEXT_PRIMARY + "; -fx-font-size: 11;");
                lbl.setMinWidth(20);

                TextField tf = new TextField(dip.getTag(i));
                tf.setPrefWidth(110);
                tf.setStyle("-fx-font-size: 11;");
                // Live-update the visible tag as the user types (no undo entry).
                tf.textProperty().addListener((obs, oldVal, newVal) -> {
                    dip.setTag(idx, newVal);
                    if (onComponentChanged != null) onComponentChanged.run();
                });
                // Commit the edit — push one undo snapshot — when the field
                // loses focus or the user presses Enter.
                tf.focusedProperty().addListener((obs, was, isNow) -> {
                    if (was && !isNow && onTagCommitted != null) onTagCommitted.run();
                });
                tf.setOnAction(e -> {
                    if (onTagCommitted != null) onTagCommitted.run();
                });

                row.getChildren().addAll(lbl, tf);
                getChildren().add(row);
            }
        }

        if (c instanceof LightBar) {
            LightBar lb = (LightBar) c;

            // Pin-side chooser
            getChildren().add(new Separator());
            Label pinSideHeader = new Label("Pin Side:");
            pinSideHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: " + Theme.TEXT_PRIMARY + "; -fx-font-size: 11;");
            getChildren().add(pinSideHeader);

            ComboBox<PinSide> sideCombo = new ComboBox<>();
            sideCombo.getItems().addAll(PinSide.TOP, PinSide.BOTTOM, PinSide.LEFT, PinSide.RIGHT);
            sideCombo.setValue(lb.getPinSide());
            sideCombo.setPrefWidth(140);
            sideCombo.setStyle("-fx-font-size: 11;");
            sideCombo.setOnAction(e -> {
                PinSide picked = sideCombo.getValue();
                if (picked != null && onPinSideChanged != null) {
                    onPinSideChanged.accept(picked);
                }
            });
            getChildren().add(sideCombo);

            // Tags
            getChildren().add(new Separator());
            Label tagsHeader = new Label("Labels:");
            tagsHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: " + Theme.TEXT_PRIMARY + "; -fx-font-size: 11;");
            getChildren().add(tagsHeader);

            int n = lb.getLightCount();
            for (int i = 0; i < n; i++) {
                final int idx = i;
                HBox row = new HBox(6);
                Label lbl = new Label(i + ":");
                lbl.setStyle("-fx-text-fill: " + Theme.TEXT_PRIMARY + "; -fx-font-size: 11;");
                lbl.setMinWidth(20);

                TextField tf = new TextField(lb.getTag(i));
                tf.setPrefWidth(110);
                tf.setStyle("-fx-font-size: 11;");
                tf.textProperty().addListener((obs, oldVal, newVal) -> {
                    lb.setTag(idx, newVal);
                    if (onComponentChanged != null) onComponentChanged.run();
                });
                tf.focusedProperty().addListener((obs, was, isNow) -> {
                    if (was && !isNow && onTagCommitted != null) onTagCommitted.run();
                });
                tf.setOnAction(e -> {
                    if (onTagCommitted != null) onTagCommitted.run();
                });

                row.getChildren().addAll(lbl, tf);
                getChildren().add(row);
            }
        }

        if (c instanceof InputPort || c instanceof OutputPort) {
            getChildren().add(new Separator());
            Label labelHeader = new Label("Port Label:");
            labelHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: " + Theme.TEXT_PRIMARY + "; -fx-font-size: 11;");
            getChildren().add(labelHeader);

            TextField tf = new TextField(
                    c instanceof InputPort ip ? ip.getPortLabel() : ((OutputPort) c).getPortLabel());
            tf.setPrefWidth(140);
            tf.setStyle("-fx-font-size: 11;");
            tf.textProperty().addListener((obs, oldVal, newVal) -> {
                if (c instanceof InputPort ip) ip.setPortLabel(newVal);
                else ((OutputPort) c).setPortLabel(newVal);
                if (onComponentChanged != null) onComponentChanged.run();
            });
            tf.focusedProperty().addListener((obs, was, isNow) -> {
                if (was && !isNow && onTagCommitted != null) onTagCommitted.run();
            });
            tf.setOnAction(e -> { if (onTagCommitted != null) onTagCommitted.run(); });
            getChildren().add(tf);
        }

        if (c instanceof SubCircuitInstance sci && !sci.isBroken()) {
            getChildren().add(new Separator());
            Button viewBtn = new Button("View Internals");
            viewBtn.setPrefWidth(150);
            viewBtn.setStyle("-fx-background-color: " + Theme.BTN_BG + "; -fx-text-fill: " +
                    Theme.TEXT_PRIMARY + "; -fx-font-size: 11; -fx-background-radius: 6; " +
                    "-fx-padding: 5 10; -fx-cursor: hand;");
            viewBtn.setOnAction(e -> {
                if (onViewSubCircuit != null) onViewSubCircuit.accept(sci);
            });
            getChildren().add(viewBtn);
        }

        getChildren().add(new Separator());
        getChildren().add(buildLockedCheckbox(c.isLocked()));
    }
}
