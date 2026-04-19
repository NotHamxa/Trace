package com.logiclab.ui;

import com.logiclab.io.SubCircuitIO;
import com.logiclab.io.SubCircuitLibrary;
import com.logiclab.model.Circuit;
import com.logiclab.model.subcircuit.SubCircuitDefinition;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import com.logiclab.util.DemoLibrary;
import com.logiclab.util.DemoLibrary.Demo;
import com.logiclab.util.RecentProjects;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * JetBrains-style launcher: sidebar with Projects / Demos, main area swaps
 * between a recent-projects list and a pre-built demo list.
 */
public class StartMenu {
    // Palette pulled from the shared Theme
    private static final String BG_SIDEBAR   = Theme.BG_CHROME;
    private static final String BG_MAIN      = Theme.BG_EDITOR;
    private static final String BORDER       = Theme.BORDER_STRONG;
    private static final String NAV_HOVER    = Theme.BG_HOVER;
    private static final String NAV_SELECT   = "#43454a";
    private static final String ROW_HOVER    = Theme.BG_HOVER;
    private static final String ROW_SELECT   = Theme.BG_SELECT;
    private static final String TEXT_1       = Theme.TEXT_PRIMARY;
    private static final String TEXT_2       = Theme.TEXT_SECONDARY;
    private static final String ACCENT       = Theme.ACCENT_BLUE;
    private static final String BTN_BG       = Theme.BTN_BG;
    private static final String BTN_HOVER    = Theme.BTN_HOVER;
    private static final String FIELD_BG     = Theme.BG_INPUT;
    private static final String FIELD_BORDER = "#43454a";

    private final BorderPane root;

    // Nav items
    private Label projectsNav;
    private Label demosNav;
    private Label subcircuitsNav;

    // Main panels
    private Pane projectsPanel;
    private Pane demosPanel;
    private Pane subcircuitsPanel;

    // Projects panel state
    private ListView<File> recentList;
    private TextField searchField;

    // Subcircuits panel state
    private ListView<SubCircuitDefinition> subcircuitList;

    // Callbacks
    private Runnable onNewProject;
    private Consumer<File> onOpenProject;
    private Consumer<Circuit> onOpenDemo;
    private Runnable onNewSubCircuit;
    private Consumer<SubCircuitDefinition> onOpenSubCircuit;
    private Stage stage;

    public StartMenu() {
        root = new BorderPane();
        root.setStyle("-fx-background-color: " + BG_MAIN + ";");

        projectsPanel = buildProjectsPanel();
        demosPanel = buildDemosPanel();
        subcircuitsPanel = buildSubcircuitsPanel();

        root.setLeft(buildSidebar());
        showProjects();
    }

    // ---------- sidebar ----------

    private VBox buildSidebar() {
        Label appName = new Label("LogicLab");
        appName.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        appName.setStyle("-fx-text-fill: " + TEXT_1 + ";");

        Label version = new Label("1.0");
        version.setFont(Font.font("Segoe UI", 11));
        version.setStyle("-fx-text-fill: " + TEXT_2 + ";");

        VBox head = new VBox(2, appName, version);
        head.setPadding(new Insets(18, 18, 18, 18));

        projectsNav = buildNavItem("Projects", this::showProjects);
        demosNav = buildNavItem("Demos", this::showDemos);
        subcircuitsNav = buildNavItem("Subcircuits", this::showSubcircuits);

        VBox nav = new VBox(2, projectsNav, demosNav, subcircuitsNav);
        nav.setPadding(new Insets(4, 8, 8, 8));

        VBox sidebar = new VBox(head, nav);
        sidebar.setPrefWidth(220);
        sidebar.setMinWidth(220);
        sidebar.setStyle(
                "-fx-background-color: " + BG_SIDEBAR + ";" +
                "-fx-border-color: " + BORDER + ";" +
                "-fx-border-width: 0 1 0 0;"
        );
        return sidebar;
    }

    private Label buildNavItem(String text, Runnable onClick) {
        Label l = new Label(text);
        l.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 13));
        l.setPrefWidth(Double.MAX_VALUE);
        l.setPadding(new Insets(8, 12, 8, 12));
        l.setStyle("-fx-text-fill: " + TEXT_1 + "; -fx-background-radius: 6; " +
                "-fx-background-color: transparent; -fx-cursor: hand;");
        l.setOnMouseClicked(e -> onClick.run());
        l.setOnMouseEntered(e -> {
            if (!"selected".equals(l.getUserData())) {
                l.setStyle("-fx-text-fill: " + TEXT_1 + "; -fx-background-radius: 6; " +
                        "-fx-background-color: " + NAV_HOVER + "; -fx-cursor: hand;");
            }
        });
        l.setOnMouseExited(e -> {
            if (!"selected".equals(l.getUserData())) {
                l.setStyle("-fx-text-fill: " + TEXT_1 + "; -fx-background-radius: 6; " +
                        "-fx-background-color: transparent; -fx-cursor: hand;");
            }
        });
        return l;
    }

    private void selectNav(Label selected) {
        for (Label l : new Label[]{projectsNav, demosNav, subcircuitsNav}) {
            if (l == selected) {
                l.setUserData("selected");
                l.setStyle("-fx-text-fill: " + TEXT_1 + "; -fx-background-radius: 6; " +
                        "-fx-background-color: " + NAV_SELECT + "; -fx-cursor: hand;");
            } else {
                l.setUserData(null);
                l.setStyle("-fx-text-fill: " + TEXT_1 + "; -fx-background-radius: 6; " +
                        "-fx-background-color: transparent; -fx-cursor: hand;");
            }
        }
    }

    private void showProjects() {
        selectNav(projectsNav);
        root.setCenter(projectsPanel);
    }

    private void showDemos() {
        selectNav(demosNav);
        root.setCenter(demosPanel);
    }

    private void showSubcircuits() {
        selectNav(subcircuitsNav);
        refreshSubcircuits();
        root.setCenter(subcircuitsPanel);
    }

    // ---------- projects panel ----------

    private VBox buildProjectsPanel() {
        VBox panel = new VBox();
        panel.setStyle("-fx-background-color: " + BG_MAIN + ";");
        panel.setPadding(new Insets(28, 40, 28, 40));
        panel.setSpacing(18);

        Label title = new Label("Projects");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        title.setStyle("-fx-text-fill: " + TEXT_1 + ";");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button newBtn = buildPrimaryButton("New Project");
        newBtn.setOnAction(e -> { if (onNewProject != null) onNewProject.run(); });

        Button openBtn = buildSecondaryButton("Open");
        openBtn.setOnAction(e -> {
            File f = chooseFile();
            if (f != null && onOpenProject != null) onOpenProject.accept(f);
        });

        HBox headerRow = new HBox(10, title, spacer, newBtn, openBtn);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        searchField = new TextField();
        searchField.setPromptText("Search projects");
        searchField.setStyle(
                "-fx-background-color: " + FIELD_BG + ";" +
                "-fx-text-fill: " + TEXT_1 + ";" +
                "-fx-prompt-text-fill: " + TEXT_2 + ";" +
                "-fx-background-radius: 6;" +
                "-fx-border-color: " + FIELD_BORDER + ";" +
                "-fx-border-radius: 6;" +
                "-fx-padding: 6 10 6 10;" +
                "-fx-font-size: 13;"
        );
        searchField.textProperty().addListener((obs, o, n) -> applyFilter(n));

        recentList = new ListView<>();
        recentList.setStyle(
                "-fx-background-color: " + BG_MAIN + ";" +
                "-fx-control-inner-background: " + BG_MAIN + ";" +
                "-fx-background-insets: 0;" +
                "-fx-padding: 0;"
        );
        Label empty = new Label("No recent projects");
        empty.setFont(Font.font("Segoe UI", 13));
        empty.setStyle("-fx-text-fill: " + TEXT_2 + ";");
        recentList.setPlaceholder(empty);
        recentList.setCellFactory(lv -> buildProjectCell());
        recentList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                File f = recentList.getSelectionModel().getSelectedItem();
                if (f != null && onOpenProject != null) onOpenProject.accept(f);
            }
        });
        VBox.setVgrow(recentList, Priority.ALWAYS);

        panel.getChildren().addAll(headerRow, searchField, recentList);
        return panel;
    }

    private ListCell<File> buildProjectCell() {
        return new ListCell<File>() {
            private final Label thumb = new Label();
            private final Label name = new Label();
            private final Label path = new Label();
            private final VBox textBox = new VBox(2, name, path);
            private final HBox row = new HBox(12, thumb, textBox);

            {
                thumb.setPrefSize(36, 36);
                thumb.setMinSize(36, 36);
                thumb.setAlignment(Pos.CENTER);
                thumb.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
                thumb.setStyle("-fx-background-color: #3c3f41; -fx-text-fill: " + TEXT_1 +
                        "; -fx-background-radius: 6;");

                name.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 13));
                name.setStyle("-fx-text-fill: " + TEXT_1 + ";");
                path.setFont(Font.font("Segoe UI", 11));
                path.setStyle("-fx-text-fill: " + TEXT_2 + ";");

                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(8, 10, 8, 10));
            }

            @Override
            protected void updateItem(File f, boolean empty) {
                super.updateItem(f, empty);
                if (empty || f == null) {
                    setGraphic(null);
                    setText(null);
                    setStyle("-fx-background-color: transparent; -fx-padding: 0;");
                    setOnMouseEntered(null);
                    setOnMouseExited(null);
                } else {
                    String fname = f.getName();
                    if (fname.endsWith(".llb")) fname = fname.substring(0, fname.length() - 4);
                    name.setText(fname);
                    path.setText(f.getParent() == null ? "" : f.getParent());
                    thumb.setText(fname.isEmpty() ? "?" : fname.substring(0, 1).toUpperCase());

                    setGraphic(row);
                    setText(null);
                    applyRowStyle(this, isSelected(), false);
                    setOnMouseEntered(e -> applyRowStyle(this, isSelected(), true));
                    setOnMouseExited(e -> applyRowStyle(this, isSelected(), false));
                }
            }
        };
    }

    // ---------- demos panel ----------

    private VBox buildDemosPanel() {
        VBox panel = new VBox();
        panel.setStyle("-fx-background-color: " + BG_MAIN + ";");
        panel.setPadding(new Insets(28, 40, 28, 40));
        panel.setSpacing(18);

        Label title = new Label("Demos");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        title.setStyle("-fx-text-fill: " + TEXT_1 + ";");

        Label hint = new Label("Pre-built sample circuits. Double-click to open.");
        hint.setFont(Font.font("Segoe UI", 12));
        hint.setStyle("-fx-text-fill: " + TEXT_2 + ";");

        VBox headerBox = new VBox(4, title, hint);

        ListView<Demo> demoList = new ListView<>();
        demoList.setStyle(
                "-fx-background-color: " + BG_MAIN + ";" +
                "-fx-control-inner-background: " + BG_MAIN + ";" +
                "-fx-background-insets: 0;" +
                "-fx-padding: 0;"
        );
        demoList.getItems().addAll(DemoLibrary.all());
        demoList.setCellFactory(lv -> buildDemoCell());
        demoList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                Demo d = demoList.getSelectionModel().getSelectedItem();
                if (d != null && onOpenDemo != null) onOpenDemo.accept(d.builder.get());
            }
        });
        VBox.setVgrow(demoList, Priority.ALWAYS);

        panel.getChildren().addAll(headerBox, demoList);
        return panel;
    }

    private ListCell<Demo> buildDemoCell() {
        return new ListCell<Demo>() {
            private final Label thumb = new Label();
            private final Label name = new Label();
            private final Label desc = new Label();
            private final VBox textBox = new VBox(2, name, desc);
            private final HBox row = new HBox(12, thumb, textBox);

            {
                thumb.setPrefSize(36, 36);
                thumb.setMinSize(36, 36);
                thumb.setAlignment(Pos.CENTER);
                thumb.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
                thumb.setText("IC");
                thumb.setStyle("-fx-background-color: #2e436e; -fx-text-fill: " + TEXT_1 +
                        "; -fx-background-radius: 6;");

                name.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 13));
                name.setStyle("-fx-text-fill: " + TEXT_1 + ";");
                desc.setFont(Font.font("Segoe UI", 11));
                desc.setStyle("-fx-text-fill: " + TEXT_2 + ";");

                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(8, 10, 8, 10));
            }

            @Override
            protected void updateItem(Demo d, boolean empty) {
                super.updateItem(d, empty);
                if (empty || d == null) {
                    setGraphic(null);
                    setText(null);
                    setStyle("-fx-background-color: transparent; -fx-padding: 0;");
                    setOnMouseEntered(null);
                    setOnMouseExited(null);
                } else {
                    name.setText(d.name);
                    desc.setText(d.description);
                    setGraphic(row);
                    setText(null);
                    applyRowStyle(this, isSelected(), false);
                    setOnMouseEntered(e -> applyRowStyle(this, isSelected(), true));
                    setOnMouseExited(e -> applyRowStyle(this, isSelected(), false));
                }
            }
        };
    }

    // ---------- subcircuits panel ----------

    private VBox buildSubcircuitsPanel() {
        VBox panel = new VBox();
        panel.setStyle("-fx-background-color: " + BG_MAIN + ";");
        panel.setPadding(new Insets(28, 40, 28, 40));
        panel.setSpacing(18);

        Label title = new Label("Subcircuits");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        title.setStyle("-fx-text-fill: " + TEXT_1 + ";");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button newBtn = buildPrimaryButton("New Subcircuit");
        newBtn.setOnAction(e -> { if (onNewSubCircuit != null) onNewSubCircuit.run(); });

        Button loadBtn = buildSecondaryButton("Load Subcircuit");
        loadBtn.setOnAction(e -> importSubcircuit());

        HBox headerRow = new HBox(10, title, spacer, loadBtn, newBtn);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        Label hint = new Label("Reusable building blocks with Input/Output ports. Double-click to edit.");
        hint.setFont(Font.font("Segoe UI", 12));
        hint.setStyle("-fx-text-fill: " + TEXT_2 + ";");

        subcircuitList = new ListView<>();
        subcircuitList.setStyle(
                "-fx-background-color: " + BG_MAIN + ";" +
                "-fx-control-inner-background: " + BG_MAIN + ";" +
                "-fx-background-insets: 0;" +
                "-fx-padding: 0;"
        );
        Label empty = new Label("No subcircuits yet — click \u201CNew Subcircuit\u201D to create one.");
        empty.setFont(Font.font("Segoe UI", 13));
        empty.setStyle("-fx-text-fill: " + TEXT_2 + ";");
        subcircuitList.setPlaceholder(empty);
        subcircuitList.setCellFactory(lv -> buildSubcircuitCell());
        subcircuitList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                SubCircuitDefinition d = subcircuitList.getSelectionModel().getSelectedItem();
                if (d != null && onOpenSubCircuit != null) onOpenSubCircuit.accept(d);
            }
        });
        VBox.setVgrow(subcircuitList, Priority.ALWAYS);

        panel.getChildren().addAll(headerRow, hint, subcircuitList);
        return panel;
    }

    private ListCell<SubCircuitDefinition> buildSubcircuitCell() {
        return new ListCell<SubCircuitDefinition>() {
            private final Label thumb = new Label();
            private final Label name = new Label();
            private final Label id = new Label();
            private final VBox textBox = new VBox(2, name, id);
            private final Region rowSpacer = new Region();
            private final Button exportBtn = buildSecondaryButton("Export");
            private final HBox row = new HBox(12, thumb, textBox, rowSpacer, exportBtn);

            {
                thumb.setPrefSize(36, 36);
                thumb.setMinSize(36, 36);
                thumb.setAlignment(Pos.CENTER);
                thumb.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
                thumb.setText("SC");
                thumb.setStyle("-fx-background-color: #4a3a6e; -fx-text-fill: " + TEXT_1 +
                        "; -fx-background-radius: 6;");

                name.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 13));
                name.setStyle("-fx-text-fill: " + TEXT_1 + ";");
                id.setFont(Font.font("Segoe UI", 11));
                id.setStyle("-fx-text-fill: " + TEXT_2 + ";");

                HBox.setHgrow(rowSpacer, Priority.ALWAYS);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(8, 10, 8, 10));
            }

            @Override
            protected void updateItem(SubCircuitDefinition d, boolean empty) {
                super.updateItem(d, empty);
                if (empty || d == null) {
                    setGraphic(null);
                    setText(null);
                    setStyle("-fx-background-color: transparent; -fx-padding: 0;");
                    setOnMouseEntered(null);
                    setOnMouseExited(null);
                    exportBtn.setOnAction(null);
                } else {
                    name.setText(d.getName());
                    id.setText(d.getId());
                    exportBtn.setOnAction(e -> exportSubcircuit(d));
                    setGraphic(row);
                    setText(null);
                    applyRowStyle(this, isSelected(), false);
                    setOnMouseEntered(e -> applyRowStyle(this, isSelected(), true));
                    setOnMouseExited(e -> applyRowStyle(this, isSelected(), false));
                }
            }
        };
    }

    private void exportSubcircuit(SubCircuitDefinition def) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Subcircuit");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("LogicLab Sub-Circuit", "*.lls"));
        chooser.setInitialFileName(def.getId() + ".lls");
        File out = chooser.showSaveDialog(stage);
        if (out == null) return;
        try {
            SubCircuitIO.write(def, System.getProperty("user.name"), out);
        } catch (java.io.IOException ex) {
            new Alert(Alert.AlertType.ERROR, "Export failed: " + ex.getMessage(), ButtonType.OK).showAndWait();
        }
    }

    private void importSubcircuit() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Load Subcircuit");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("LogicLab Sub-Circuit", "*.lls"));
        File in = chooser.showOpenDialog(stage);
        if (in == null) return;
        SubCircuitDefinition def;
        try {
            def = SubCircuitIO.read(in);
        } catch (java.io.IOException ex) {
            new Alert(Alert.AlertType.ERROR, "Load failed: " + ex.getMessage(), ButtonType.OK).showAndWait();
            return;
        }
        File existing = SubCircuitLibrary.fileFor(def.getId());
        if (existing.exists()) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "A subcircuit with id \"" + def.getId() + "\" already exists. Overwrite?",
                    ButtonType.YES, ButtonType.NO);
            confirm.setHeaderText("Subcircuit already exists");
            java.util.Optional<ButtonType> r = confirm.showAndWait();
            if (r.isEmpty() || r.get() != ButtonType.YES) return;
        }
        try {
            SubCircuitLibrary.save(def, System.getProperty("user.name"));
            refreshSubcircuits();
        } catch (java.io.IOException ex) {
            new Alert(Alert.AlertType.ERROR, "Save failed: " + ex.getMessage(), ButtonType.OK).showAndWait();
        }
    }

    public void refreshSubcircuits() {
        if (subcircuitList == null) return;
        SubCircuitLibrary.reload();
        subcircuitList.getItems().setAll(SubCircuitLibrary.all());
    }

    // ---------- shared helpers ----------

    private static void applyRowStyle(ListCell<?> cell, boolean selected, boolean hover) {
        String bg;
        if (selected) bg = ROW_SELECT;
        else if (hover) bg = ROW_HOVER;
        else bg = "transparent";
        cell.setStyle(
                "-fx-background-color: " + bg + ";" +
                "-fx-background-radius: 6;" +
                "-fx-padding: 0;"
        );
    }

    private Button buildPrimaryButton(String text) {
        Button b = new Button(text);
        b.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 13));
        b.setPrefHeight(32);
        String base = "-fx-background-color: " + ACCENT + "; -fx-text-fill: #ffffff;" +
                "-fx-background-radius: 6; -fx-padding: 4 14 4 14; -fx-cursor: hand;";
        String hover = "-fx-background-color: #4481f5; -fx-text-fill: #ffffff;" +
                "-fx-background-radius: 6; -fx-padding: 4 14 4 14; -fx-cursor: hand;";
        b.setStyle(base);
        b.setOnMouseEntered(e -> b.setStyle(hover));
        b.setOnMouseExited(e -> b.setStyle(base));
        return b;
    }

    private Button buildSecondaryButton(String text) {
        Button b = new Button(text);
        b.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 13));
        b.setPrefHeight(32);
        String base = "-fx-background-color: " + BTN_BG + "; -fx-text-fill: " + TEXT_1 + ";" +
                "-fx-background-radius: 6; -fx-padding: 4 14 4 14; -fx-cursor: hand;";
        String hover = "-fx-background-color: " + BTN_HOVER + "; -fx-text-fill: " + TEXT_1 + ";" +
                "-fx-background-radius: 6; -fx-padding: 4 14 4 14; -fx-cursor: hand;";
        b.setStyle(base);
        b.setOnMouseEntered(e -> b.setStyle(hover));
        b.setOnMouseExited(e -> b.setStyle(base));
        return b;
    }

    // ---------- filtering + API ----------

    private void applyFilter(String query) {
        List<File> all = RecentProjects.load();
        if (query == null || query.isEmpty()) {
            recentList.getItems().setAll(all);
            return;
        }
        String q = query.toLowerCase();
        List<File> filtered = new ArrayList<>();
        for (File f : all) {
            if (f.getName().toLowerCase().contains(q)
                    || (f.getParent() != null && f.getParent().toLowerCase().contains(q))) {
                filtered.add(f);
            }
        }
        recentList.getItems().setAll(filtered);
    }

    public void refreshRecents() {
        applyFilter(searchField == null ? "" : searchField.getText());
    }

    private File chooseFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open Circuit");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("LogicLab Files", "*.llb")
        );
        return chooser.showOpenDialog(stage);
    }

    public Pane getRoot() { return root; }
    public void setStage(Stage stage) { this.stage = stage; }
    public void setOnNewProject(Runnable r) { this.onNewProject = r; }
    public void setOnOpenProject(Consumer<File> c) { this.onOpenProject = c; }
    public void setOnOpenDemo(Consumer<Circuit> c) { this.onOpenDemo = c; }
    public void setOnNewSubCircuit(Runnable r) { this.onNewSubCircuit = r; }
    public void setOnOpenSubCircuit(Consumer<SubCircuitDefinition> c) { this.onOpenSubCircuit = c; }
}
