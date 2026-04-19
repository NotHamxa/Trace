package com.trace;

import com.trace.controller.FileController;
import com.trace.model.Circuit;
import com.trace.ui.MainWindow;
import com.trace.ui.StartMenu;
import com.trace.ui.StatusBar;
import com.trace.ui.Theme;
import com.trace.ui.TitleBar;
import com.trace.util.RecentProjects;
import com.trace.util.UpdateChecker;
import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;

import java.io.File;

public class App extends Application {
    private Stage primaryStage;
    private Scene scene;
    private StartMenu startMenu;
    private MainWindow mainWindow;
    private TitleBar titleBar;
    private StatusBar statusBar;
    private StackPane contentHolder;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.initStyle(StageStyle.UNDECORATED);

        contentHolder = new StackPane();
        contentHolder.setStyle("-fx-background-color: " + Theme.BG_EDITOR + ";");
        VBox.setVgrow(contentHolder, Priority.ALWAYS);

        titleBar = new TitleBar(primaryStage, this::onCloseRequested);
        statusBar = new StatusBar();

        VBox outer = new VBox(titleBar, contentHolder, statusBar);
        outer.setStyle("-fx-background-color: " + Theme.BG_EDITOR +
                "; -fx-border-color: " + Theme.BORDER_STRONG + "; -fx-border-width: 1;");

        scene = new Scene(outer, 1280, 800);

        try {
            String css = getClass().getResource("/styles/trace.css").toExternalForm();
            scene.getStylesheets().add(css);
        } catch (Exception e) {
            // CSS not found, continue with inline styles
        }

        primaryStage.setTitle("Trace - Circuit Simulator");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);

        // App icon — loaded from the classpath so it shows up on the
        // taskbar, Alt-Tab switcher, etc.
        try {
            Image appIcon = new Image(getClass().getResourceAsStream("/icons/icon.png"));
            primaryStage.getIcons().add(appIcon);
        } catch (Exception e) {
            // Icon missing — fall through, app still runs without one.
        }

        // Open maximized over the primary screen's visual bounds (respects
        // the taskbar — undecorated + setMaximized would cover it).
        Rectangle2D vb = Screen.getPrimary().getVisualBounds();
        primaryStage.setX(vb.getMinX());
        primaryStage.setY(vb.getMinY());
        primaryStage.setWidth(vb.getWidth());
        primaryStage.setHeight(vb.getHeight());

        addResizeSupport(outer, primaryStage);

        showStartMenu();

        // Intercept close: if we're inside a project with unsaved changes, prompt first.
        primaryStage.setOnCloseRequest(this::handleWindowClose);

        primaryStage.show();

        startUpdateChecker();
    }

    private void startUpdateChecker() {
        final UpdateChecker[] holder = new UpdateChecker[1];
        UpdateChecker checker = new UpdateChecker(new UpdateChecker.Listener() {
            @Override public void onAvailable(String version) {
                statusBar.showUpdateAvailable(version, () -> holder[0].startDownload());
            }
            @Override public void onProgress(double fraction) {
                statusBar.showUpdateProgress(fraction);
            }
            @Override public void onReady(java.nio.file.Path installer) {
                statusBar.showUpdateReady(() -> holder[0].installAndExit());
            }
            @Override public void onError(String message) {
                statusBar.setWarning("Update failed: " + message);
                statusBar.clearUpdate();
            }
        });
        holder[0] = checker;
        checker.checkAsync();
    }

    private void showStartMenu() {
        startMenu = new StartMenu();
        startMenu.setStage(primaryStage);
        startMenu.setOnNewProject(() -> openProject(new Circuit(), null));
        startMenu.setOnOpenProject(file -> {
            FileController fc = new FileController(primaryStage);
            Circuit c = fc.loadFrom(file);
            if (c != null) {
                RecentProjects.add(file);
                openProject(c, file);
            } else {
                // Load failed — drop the stale entry from recents.
                RecentProjects.remove(file);
                startMenu.refreshRecents();
            }
        });
        startMenu.setOnOpenDemo(circuit -> openProject(circuit, null));
        startMenu.setOnNewSubCircuit(() -> openSubCircuit(new Circuit(), null, null));
        startMenu.setOnOpenSubCircuit(def -> openSubCircuit(def.getInner(), def.getId(), def.getName()));
        startMenu.refreshRecents();
        startMenu.refreshSubcircuits();
        contentHolder.getChildren().setAll(startMenu.getRoot());
        primaryStage.setTitle("Trace - Circuit Simulator");
        titleBar.setTitle("Trace");
        mainWindow = null;
    }

    private void openProject(Circuit circuit, File file) {
        mainWindow = new MainWindow(circuit, file, statusBar);
        mainWindow.setStage(primaryStage);
        mainWindow.setOnCloseProject(this::showStartMenu);
        contentHolder.getChildren().setAll(mainWindow.getRoot());
        String label = file != null ? file.getName().replace(".trc", "") : "Untitled";
        primaryStage.setTitle("Trace - " + label);
        titleBar.setTitle("Trace \u2014 " + label);
    }

    private void openSubCircuit(Circuit circuit, String id, String name) {
        mainWindow = new MainWindow(circuit, statusBar, id, name);
        mainWindow.setStage(primaryStage);
        mainWindow.setOnCloseProject(this::showStartMenu);
        contentHolder.getChildren().setAll(mainWindow.getRoot());
        String label = name != null ? name : "New Subcircuit";
        primaryStage.setTitle("Trace - " + label);
        titleBar.setTitle("Trace \u2014 Subcircuit: " + label);
    }

    /** Title-bar close-button hook: returns true if the window may actually close. */
    private boolean onCloseRequested() {
        if (mainWindow != null) {
            return mainWindow.confirmDiscardChanges();
        }
        return true;
    }

    private void handleWindowClose(WindowEvent e) {
        if (mainWindow != null) {
            if (!mainWindow.confirmDiscardChanges()) {
                e.consume(); // user cancelled — keep the window open
            }
        }
    }

    // ---------- undecorated window resize ----------

    /** Adds edge-drag resize + edge cursor hints to the outer root of an undecorated stage. */
    private void addResizeSupport(Region root, Stage stage) {
        final int border = 6;
        final double[] state = new double[6]; // startScreenX, startScreenY, origX, origY, origW, origH
        final int[] dir = {0};

        root.addEventFilter(MouseEvent.MOUSE_MOVED, e -> {
            if (stage.isMaximized()) {
                if (root.getCursor() != Cursor.DEFAULT) root.setCursor(Cursor.DEFAULT);
                return;
            }
            int d = hitDir(e.getX(), e.getY(), root.getWidth(), root.getHeight(), border);
            if (d != 0) {
                root.setCursor(cursorFor(d));
            } else {
                // Leave null so descendants can set their own cursor freely
                root.setCursor(null);
            }
        });

        root.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (stage.isMaximized()) return;
            int d = hitDir(e.getX(), e.getY(), root.getWidth(), root.getHeight(), border);
            if (d != 0) {
                dir[0] = d;
                state[0] = e.getScreenX();
                state[1] = e.getScreenY();
                state[2] = stage.getX();
                state[3] = stage.getY();
                state[4] = stage.getWidth();
                state[5] = stage.getHeight();
                e.consume();
            }
        });

        root.addEventFilter(MouseEvent.MOUSE_DRAGGED, e -> {
            if (dir[0] == 0) return;
            double dx = e.getScreenX() - state[0];
            double dy = e.getScreenY() - state[1];
            double minW = Math.max(stage.getMinWidth(), 200);
            double minH = Math.max(stage.getMinHeight(), 150);
            if ((dir[0] & 2) != 0) { // right
                stage.setWidth(Math.max(minW, state[4] + dx));
            }
            if ((dir[0] & 8) != 0) { // bottom
                stage.setHeight(Math.max(minH, state[5] + dy));
            }
            if ((dir[0] & 1) != 0) { // left
                double newW = Math.max(minW, state[4] - dx);
                stage.setX(state[2] + state[4] - newW);
                stage.setWidth(newW);
            }
            if ((dir[0] & 4) != 0) { // top
                double newH = Math.max(minH, state[5] - dy);
                stage.setY(state[3] + state[5] - newH);
                stage.setHeight(newH);
            }
            e.consume();
        });

        root.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> {
            if (dir[0] != 0) {
                dir[0] = 0;
                e.consume();
            }
        });
    }

    private static int hitDir(double x, double y, double w, double h, int b) {
        int d = 0;
        if (x < b) d |= 1;
        if (x > w - b) d |= 2;
        if (y < b) d |= 4;
        if (y > h - b) d |= 8;
        return d;
    }

    private static Cursor cursorFor(int d) {
        switch (d) {
            case 1:      return Cursor.W_RESIZE;
            case 2:      return Cursor.E_RESIZE;
            case 4:      return Cursor.N_RESIZE;
            case 8:      return Cursor.S_RESIZE;
            case 1 | 4:  return Cursor.NW_RESIZE;
            case 2 | 4:  return Cursor.NE_RESIZE;
            case 1 | 8:  return Cursor.SW_RESIZE;
            case 2 | 8:  return Cursor.SE_RESIZE;
            default:     return Cursor.DEFAULT;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
