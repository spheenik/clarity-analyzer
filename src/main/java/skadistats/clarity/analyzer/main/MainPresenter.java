package skadistats.clarity.analyzer.main;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Slider;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.analyzer.Main;
import skadistats.clarity.analyzer.replay.ObservableEntity;
import skadistats.clarity.analyzer.replay.ObservableEntityProperty;
import skadistats.clarity.analyzer.replay.ReplayController;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import static javafx.beans.binding.Bindings.createStringBinding;
import static javafx.beans.binding.Bindings.valueAt;

public class MainPresenter implements Initializable {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    @FXML
    public Button buttonPlay;

    @FXML
    public Button buttonPause;

    @FXML
    public Slider slider;

    @FXML
    public Label labelTick;

    @FXML
    public Label labelLastTick;

    @FXML
    public TableView<ObservableEntity> entityTable;

    @FXML
    public TableView<ObservableEntityProperty> detailTable;

    @FXML
    public TextField entityNameFilter;

    @FXML
    public AnchorPane mapCanvasPane;

    private Preferences preferences;
    private ReplayController replayController;
    private MapControl mapControl;
    private FilteredList<ObservableEntity> filteredEntityList;

    private Predicate<ObservableEntity> allFilterFunc = new Predicate<ObservableEntity>() {
        @Override
        public boolean test(ObservableEntity e) {
            return true;
        }
    };

    private Predicate<ObservableEntity> filterFunc = new Predicate<ObservableEntity>() {
        @Override
        public boolean test(ObservableEntity e) {
            String filter = entityNameFilter.getText();
            if (filter.length() > 0) {
                return e != null && e.getName().toLowerCase().contains(filter.toLowerCase());
            }
            return true;
        }
    };

    public void initialize(java.net.URL location, java.util.ResourceBundle resources) {
        preferences = Preferences.userNodeForPackage(this.getClass());
        replayController = new ReplayController(slider);

        BooleanBinding runnerIsNull = Bindings.createBooleanBinding(() -> replayController.getRunner() == null, replayController.runnerProperty());
        buttonPlay.disableProperty().bind(runnerIsNull.or(replayController.playingProperty()));
        buttonPause.disableProperty().bind(runnerIsNull.or(replayController.playingProperty().not()));
        slider.disableProperty().bind(runnerIsNull);

        labelTick.textProperty().bind(replayController.tickProperty().asString());
        labelLastTick.textProperty().bind(replayController.lastTickProperty().asString());

        // entity table
        createTableCell(entityTable, "#", String.class, col ->
                col.setCellValueFactory(f -> f.getValue().indexProperty().asString())
        );
        createTableCell(entityTable, "class", String.class, col ->
                col.setCellValueFactory(f -> {
                    ObjectBinding<? extends ObservableEntity> src = valueAt(replayController.getEntityList(), f.getValue().getIndex());
                    return createStringBinding(() -> src.get().getName(), src);
                })
        );
        entityTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            log.info("entity table selection from {} to {}", oldValue, newValue);
            detailTable.setItems(newValue);
        });

        // detail table
        createTableCell(detailTable, "#", String.class, col ->
                col.setCellValueFactory(f -> f.getValue().fieldPathProperty().asString())
        );
        createTableCell(detailTable, "type", String.class, col ->
                col.setCellValueFactory(f -> f.getValue().typeProperty())
        );
        createTableCell(detailTable, "name", String.class, col ->
                col.setCellValueFactory(f -> f.getValue().nameProperty())
        );
        createTableCell(detailTable, "value", String.class, col -> {
                    col.setCellValueFactory(f -> f.getValue().valueProperty().asString());
                    col.setCellFactory(v -> new EntityValueTableCell());
                }
        );
        detailTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        detailTable.setOnKeyPressed(this::handleDetailTableKeyPressed);


        entityNameFilter.textProperty().addListener(observable -> {
            if (filteredEntityList != null) {
                filteredEntityList.setPredicate(allFilterFunc);
                filteredEntityList.setPredicate(filterFunc);
            }
        });

        mapControl = new MapControl();
        mapCanvasPane.getChildren().add(mapControl);

        mapCanvasPane.setTopAnchor(mapControl, 0.0);
        mapCanvasPane.setBottomAnchor(mapControl, 0.0);
        mapCanvasPane.setLeftAnchor(mapControl, 0.0);
        mapCanvasPane.setRightAnchor(mapControl, 0.0);
        mapCanvasPane.widthProperty().addListener(evt -> resizeMapControl());
        mapCanvasPane.heightProperty().addListener(evt -> resizeMapControl());

    }

    private <S, V> void createTableCell(TableView<S> tableView, String header, Class<V> valueClass, Consumer<TableColumn<S, V>> columnInitializer) {
        TableColumn<S, V> column = new TableColumn<>(header);
        tableView.getColumns().add(column);
        columnInitializer.accept(column);
    }

    private void resizeMapControl() {
        double scale = Math.min(mapCanvasPane.getWidth() / mapControl.getSize(), mapCanvasPane.getHeight() / mapControl.getSize());

        double sw = mapControl.getSize() * scale;
        double dx = mapCanvasPane.getWidth() - sw;
        double dy = mapCanvasPane.getHeight() - sw;

        mapCanvasPane.getTransforms().clear();
        mapCanvasPane.getTransforms().add(new Scale(scale, scale));
        mapCanvasPane.getTransforms().add(new Translate(0.5 * dx / scale, 0.5 * dy / scale));
    }

    public void actionQuit(ActionEvent actionEvent) {
        replayController.haltIfRunning();
        Main.primaryStage.close();
    }

    public void actionOpen(ActionEvent actionEvent) throws IOException {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load a replay");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Dota 2 replay files", "*.dem"),
            new FileChooser.ExtensionFilter("All files", "*")
        );
        File dir = new File(preferences.get("fileChooserPath", "."));
        if (!dir.isDirectory()) {
            dir = new File(".");
        }
        fileChooser.setInitialDirectory(dir);
        File f = fileChooser.showOpenDialog(Main.primaryStage);
        if (f == null) {
            return;
        }
        preferences.put("fileChooserPath", f.getParent());
        try {
            replayController.load(f);
            mapControl.setEntities(replayController.getEntityList());
            filteredEntityList = replayController.getEntityList().filtered(filterFunc);
            entityTable.setItems(filteredEntityList);
        } catch (Exception e) {
            e.printStackTrace();
            new ExceptionDialog(e).show();
        }
    }

    public void clickPlay(ActionEvent actionEvent) {
        replayController.setPlaying(true);
    }

    public void clickPause(ActionEvent actionEvent) {
        replayController.setPlaying(false);
    }

    private void handleDetailTableKeyPressed(KeyEvent e) {
        KeyCombination ctrlC = new KeyCodeCombination(KeyCode.C, KeyCodeCombination.CONTROL_DOWN);
        if (ctrlC.match(e)) {
            ClipboardContent cbc = new ClipboardContent();
            cbc.putString(detailTable.getSelectionModel().getSelectedIndices().stream()
                    .map(i -> detailTable.getItems().get(i))
                    .map(p -> String.format("%s %s %s", p.getFieldPath(), p.getName(), p.getValue()))
                    .collect(Collectors.joining("\n"))
            );
            Clipboard.getSystemClipboard().setContent(cbc);
        }
    }

}
