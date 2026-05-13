package com.trace.ui;

import com.trace.util.AppSettings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Popup;
import javafx.stage.Window;

public class SettingsPopover {
    private final Popup popup = new Popup();

    public SettingsPopover() {
        popup.setAutoHide(true);
        popup.setHideOnEscape(true);

        VBox content = new VBox(14);
        content.setPadding(new Insets(14, 16, 14, 16));
        content.setStyle(
                "-fx-background-color: " + Theme.BG_ELEVATED + ";" +
                "-fx-border-color: " + Theme.BORDER_SOFT + ";" +
                "-fx-border-width: 1;" +
                "-fx-background-radius: 8;" +
                "-fx-border-radius: 8;");
        content.setPrefWidth(380);

        Label title = new Label("Settings");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        title.setStyle("-fx-text-fill: " + Theme.TEXT_PRIMARY + ";");

        content.getChildren().addAll(
                title,
                section("Editor", buildEditorOptions()),
                section("Mouse & Wire Editing", buildMouseGuide()),
                section("Keyboard Shortcuts", buildShortcutsGrid()));

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setMaxHeight(540);
        scroll.setStyle(
                "-fx-background: transparent;" +
                "-fx-background-color: transparent;" +
                "-fx-border-color: transparent;");
        popup.getContent().add(scroll);
    }

    private VBox section(String heading, Node body) {
        Label h = new Label(heading);
        h.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 11));
        h.setStyle("-fx-text-fill: " + Theme.TEXT_SECONDARY + "; -fx-padding: 0 0 4 0;");
        VBox box = new VBox(6, h, body);
        return box;
    }

    private Node buildEditorOptions() {
        CheckBox autoBend = new CheckBox("Auto-bend wires");
        autoBend.setSelected(AppSettings.isAutoBendWires());
        autoBend.setStyle("-fx-text-fill: " + Theme.TEXT_PRIMARY + "; -fx-font-size: 12;");
        autoBend.selectedProperty().addListener((o, was, is) -> AppSettings.setAutoBendWires(is));

        Label hint = new Label("New wires take an L-shaped orthogonal path\ninstead of a single diagonal line.");
        hint.setStyle("-fx-text-fill: " + Theme.TEXT_MUTED + "; -fx-font-size: 11;");

        VBox box = new VBox(4, autoBend, hint);
        return box;
    }

    private Node buildMouseGuide() {
        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(6);

        String[][] rows = {
                {"Navigation", null,                                null},
                {null,         "Middle-drag",                       "Pan the canvas"},
                {null,         "Ctrl + Left-drag",                  "Pan the canvas"},
                {null,         "Scroll wheel",                      "Zoom in / out at cursor"},
                {"Wire — placement",  null,                         null},
                {null,         "Click pin → click pin",             "Place a wire (with the wire tool)"},
                {null,         "Right-click",                       "Cancel an in-progress wire or placement"},
                {"Wire — pivots",     null,                         null},
                {null,         "Right-click on wire",               "Add a pivot at the cursor"},
                {null,         "Right-click on pivot",              "Remove that pivot"},
                {null,         "Drag a pivot",                      "Move the bend point"},
                {null,         "Drag wire endpoint",                "Reroute to another pin / hole"},
                {"Selection",         null,                         null},
                {null,         "Click component / wire",            "Select it"},
                {null,         "Drag selected component",           "Move it on the canvas"},
                {null,         "L (with selection)",                "Lock / unlock — locked items can't move, reroute, or change pivots"},
        };

        int r = 0;
        for (String[] row : rows) {
            if (row[0] != null) {
                Label group = new Label(row[0]);
                group.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 11));
                group.setStyle("-fx-text-fill: " + Theme.TEXT_MUTED + ";" +
                        "-fx-padding: " + (r == 0 ? "0" : "6") + " 0 2 0;");
                GridPane.setColumnSpan(group, 2);
                grid.add(group, 0, r);
            } else {
                grid.add(keyChip(row[1]), 0, r);
                Label desc = new Label(row[2]);
                desc.setWrapText(true);
                desc.setMaxWidth(200);
                desc.setStyle("-fx-text-fill: " + Theme.TEXT_PRIMARY + "; -fx-font-size: 12;");
                grid.add(desc, 1, r);
            }
            r++;
        }
        return grid;
    }

    private GridPane buildShortcutsGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(6);

        String[][] rows = {
                {"File",   null,                  null},
                {null,     "Ctrl + N",            "New circuit"},
                {null,     "Ctrl + O",            "Open circuit"},
                {null,     "Ctrl + S",            "Save circuit"},
                {"Edit",   null,                  null},
                {null,     "Ctrl + Z",            "Undo"},
                {null,     "Ctrl + Y",            "Redo"},
                {null,     "Delete / Backspace",  "Delete selection"},
                {null,     "L",                   "Lock / unlock selection"},
                {"Draw Mode Tools", null,         null},
                {null,     "S",                   "Select tool"},
                {null,     "R",                   "Red wire"},
                {null,     "B",                   "Black wire"},
                {null,     "W",                   "White wire"},
        };

        int r = 0;
        for (String[] row : rows) {
            if (row[0] != null) {
                Label group = new Label(row[0]);
                group.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 11));
                group.setStyle("-fx-text-fill: " + Theme.TEXT_MUTED + ";" +
                        "-fx-padding: " + (r == 0 ? "0" : "6") + " 0 2 0;");
                GridPane.setColumnSpan(group, 2);
                grid.add(group, 0, r);
            } else {
                grid.add(keyChip(row[1]), 0, r);
                Label desc = new Label(row[2]);
                desc.setStyle("-fx-text-fill: " + Theme.TEXT_PRIMARY + "; -fx-font-size: 12;");
                grid.add(desc, 1, r);
            }
            r++;
        }
        return grid;
    }

    private Node keyChip(String text) {
        Label lbl = new Label(text);
        lbl.setFont(Font.font("Consolas", FontWeight.NORMAL, 11));
        lbl.setStyle(
                "-fx-text-fill: " + Theme.TEXT_PRIMARY + ";" +
                "-fx-background-color: " + Theme.BG_INPUT + ";" +
                "-fx-border-color: " + Theme.BORDER_SOFT + ";" +
                "-fx-border-width: 1;" +
                "-fx-background-radius: 4;" +
                "-fx-border-radius: 4;" +
                "-fx-padding: 2 8;");
        StackPane wrap = new StackPane(lbl);
        wrap.setAlignment(Pos.CENTER_LEFT);
        HBox box = new HBox(wrap);
        box.setMinWidth(160);
        return box;
    }

    public void showUnder(Node anchor) {
        if (popup.isShowing()) {
            popup.hide();
            return;
        }
        Window owner = anchor.getScene().getWindow();
        var bounds = anchor.localToScreen(anchor.getBoundsInLocal());
        popup.show(owner, bounds.getMinX(), bounds.getMaxY() + 4);
    }

    public void hide() { popup.hide(); }
}
