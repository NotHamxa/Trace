package com.logiclab.ui;

import com.logiclab.model.Component;
import com.logiclab.model.chips.*;
import com.logiclab.model.input.*;
import com.logiclab.model.output.*;
import com.logiclab.model.passive.*;
import com.logiclab.model.power.*;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class ToolboxPanel extends ScrollPane {
    // Base styles for tool buttons — one is the idle state, the other is the
    // highlight applied to whichever button represents the currently active tool.
    private static final String STYLE_IDLE =
            "-fx-background-color: " + Theme.BTN_BG + "; -fx-text-fill: " + Theme.TEXT_PRIMARY + "; -fx-font-size: 11; " +
            "-fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 6 10; " +
            "-fx-border-color: transparent; -fx-border-width: 1; -fx-border-radius: 6;";
    private static final String STYLE_HOVER =
            "-fx-background-color: " + Theme.BTN_HOVER + "; -fx-text-fill: #ffffff; -fx-font-size: 11; " +
            "-fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 6 10; " +
            "-fx-border-color: transparent; -fx-border-width: 1; -fx-border-radius: 6;";
    private static final String STYLE_ACTIVE =
            "-fx-background-color: " + Theme.BG_SELECT + "; -fx-text-fill: #ffffff; -fx-font-size: 11; " +
            "-fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 6 10; " +
            "-fx-border-color: " + Theme.ACCENT_BLUE + "; -fx-border-width: 1; -fx-border-radius: 6; " +
            "-fx-font-weight: bold;";

    private Consumer<Component> onComponentSelected;
    private Consumer<Color> onWireToolSelected;
    private Runnable onSelectMode;
    private Runnable onAddBreadboard;
    private VBox content;

    // Active-tool tracking — so we can visually mark the last-picked tool and
    // revert its styling when a different one takes over.
    private Button activeButton;
    private Button selectButton;
    private Button redWireButton;
    private Button blackWireButton;
    private Button whiteWireButton;

    // The VBox that currently receives new items (the body of the latest category).
    private VBox currentCategoryBody;

    public ToolboxPanel(Consumer<Component> onComponentSelected,
                        Consumer<Color> onWireToolSelected,
                        Runnable onSelectMode,
                        Runnable onAddBreadboard) {
        this.onComponentSelected = onComponentSelected;
        this.onWireToolSelected = onWireToolSelected;
        this.onSelectMode = onSelectMode;
        this.onAddBreadboard = onAddBreadboard;

        content = new VBox();
        content.setPrefWidth(155);
        content.setSpacing(4);
        content.setPadding(new Insets(10));
        content.setStyle("-fx-background-color: " + Theme.BG_CHROME + ";");

        Label title = new Label("Components");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14; -fx-text-fill: " + Theme.TEXT_PRIMARY + ";");
        content.getChildren().add(title);
        content.getChildren().add(new Separator());

        addCategory("Tools");
        selectButton = addToolButton("Select  [S]", () -> onSelectMode.run());

        addCategory("Wires");
        redWireButton   = addToolButton("Red Wire  [R]",   () -> onWireToolSelected.accept(Color.rgb(220, 50, 50)));
        blackWireButton = addToolButton("Black Wire  [B]", () -> onWireToolSelected.accept(Color.rgb(20, 20, 20)));
        whiteWireButton = addToolButton("White Wire  [W]", () -> onWireToolSelected.accept(Color.rgb(240, 240, 240)));

        addCategory("Logic Gates");
        addDraggableItem("AND (7408)", ANDGate7408::new);
        addDraggableItem("OR (7432)", ORGate7432::new);
        addDraggableItem("NOT (7404)", NOTGate7404::new);
        addDraggableItem("NAND (7400)", NANDGate7400::new);
        addDraggableItem("NOR (7402)", NORGate7402::new);
        addDraggableItem("XOR (7486)", XORGate7486::new);
        addDraggableItem("AND3 (7411)", ANDGate7411::new);
        addDraggableItem("NAND3 (7410)", NANDGate7410::new);
        addDraggableItem("NOR3 (7427)", NORGate7427::new);

        addCategory("Input");
        addDraggableItem("Toggle Switch", ToggleSwitch::new);
        addDraggableItem("Push Button", PushButton::new);
        addDraggableItem("DIP Switch", DIPSwitch::new);

        addCategory("Output");
        addDraggableItem("LED (Red)", () -> new LED(Color.RED));
        addDraggableItem("LED (Green)", () -> new LED(Color.GREEN));
        addDraggableItem("7-Seg Display", SevenSegmentDisplay::new);
        addDraggableItem("Light Bar", LightBar::new);
        addDraggableItem("BCD Display", BinaryToBCDDisplay::new);

        addCategory("Power");
        addDraggableItem("5V Supply", PowerSupply5V::new);
        addDraggableItem("Ground", Ground::new);

        addCategory("Passive");
        addDraggableItem("Resistor", Resistor::new);

        addCategory("Board");
        addToolButton("Breadboard", () -> { if (onAddBreadboard != null) onAddBreadboard.run(); });

        // Start with Select highlighted so the user can see the default tool.
        setActive(selectButton);

        setContent(content);
        setFitToWidth(true);
        setHbarPolicy(ScrollBarPolicy.NEVER);
        setStyle("-fx-background-color: " + Theme.BG_CHROME +
                "; -fx-border-color: transparent " + Theme.BORDER_SOFT + " transparent transparent;");
    }

    private Button addToolButton(String name, Runnable action) {
        Button btn = new Button(name);
        btn.setPrefWidth(140);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setFocusTraversable(false);
        applyIdleStyle(btn);
        btn.setOnMouseEntered(e -> { if (btn != activeButton) btn.setStyle(STYLE_HOVER); });
        btn.setOnMouseExited(e -> { if (btn != activeButton) btn.setStyle(STYLE_IDLE); });
        btn.setOnAction(e -> {
            setActive(btn);
            action.run();
        });
        currentCategoryBody.getChildren().add(btn);
        return btn;
    }

    private void addCategory(String name) {
        VBox body = new VBox();
        body.setSpacing(4);

        Label header = new Label("\u25BC " + name);
        header.setStyle("-fx-font-weight: bold; -fx-text-fill: " + Theme.TEXT_SECONDARY +
                "; -fx-padding: 8 0 4 0; -fx-font-size: 11; -fx-cursor: hand;");
        header.setMaxWidth(Double.MAX_VALUE);
        header.setOnMouseClicked(e -> {
            boolean visible = body.isVisible();
            body.setVisible(!visible);
            body.setManaged(!visible);
            header.setText((!visible ? "\u25BC " : "\u25B6 ") + name);
        });

        content.getChildren().add(header);
        content.getChildren().add(body);
        currentCategoryBody = body;
    }

    private Button addDraggableItem(String name, Supplier<Component> factory) {
        Button btn = new Button(name);
        btn.setPrefWidth(140);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setFocusTraversable(false);
        applyIdleStyle(btn);
        btn.setOnMouseEntered(e -> { if (btn != activeButton) btn.setStyle(STYLE_HOVER); });
        btn.setOnMouseExited(e -> { if (btn != activeButton) btn.setStyle(STYLE_IDLE); });
        btn.setOnAction(e -> {
            setActive(btn);
            onComponentSelected.accept(factory.get());
        });
        currentCategoryBody.getChildren().add(btn);
        return btn;
    }

    private void applyIdleStyle(Button btn) {
        btn.setStyle(STYLE_IDLE);
    }

    /** Marks a button as the active tool — restyles the old and new buttons. */
    private void setActive(Button btn) {
        if (activeButton != null && activeButton != btn) {
            activeButton.setStyle(STYLE_IDLE);
        }
        activeButton = btn;
        if (activeButton != null) {
            activeButton.setStyle(STYLE_ACTIVE);
        }
    }

    // --- Public API used by keyboard shortcuts + canvas state changes --------

    /** Activates the Select tool (clears any active placement/wire tool). */
    public void activateSelect() {
        setActive(selectButton);
        onSelectMode.run();
    }

    /** Activates the red wire tool. */
    public void activateRedWire() {
        setActive(redWireButton);
        onWireToolSelected.accept(Color.rgb(220, 50, 50));
    }

    /** Activates the black wire tool. */
    public void activateBlackWire() {
        setActive(blackWireButton);
        onWireToolSelected.accept(Color.rgb(20, 20, 20));
    }

    /** Activates the white wire tool. */
    public void activateWhiteWire() {
        setActive(whiteWireButton);
        onWireToolSelected.accept(Color.rgb(240, 240, 240));
    }

    /** Called by the canvas after a one-shot placement finishes/cancels. */
    public void revertToSelect() {
        setActive(selectButton);
    }
}
