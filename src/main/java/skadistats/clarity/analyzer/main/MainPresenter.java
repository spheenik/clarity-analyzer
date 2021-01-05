package skadistats.clarity.analyzer.main;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.Transition;
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
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.analyzer.Main;
import skadistats.clarity.analyzer.replay.ObservableEntity;
import skadistats.clarity.analyzer.replay.ObservableEntityProperty;
import skadistats.clarity.analyzer.replay.ReplayController;

import java.io.File;
import java.io.IOException;
import java.util.function.Predicate;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

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

    private MapControl mapControl;

    private Preferences preferences;
    private ReplayController replayController;


    private FilteredList<ObservableEntity> filteredEntityList = null;

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

        TableColumn<ObservableEntity, String> entityTableIdColumn = (TableColumn<ObservableEntity, String>) entityTable.getColumns().get(0);
        entityTableIdColumn.setCellValueFactory(e -> e.getValue().indexProperty().asString());
        TableColumn<ObservableEntity, String> entityTableNameColumn = (TableColumn<ObservableEntity, String>) entityTable.getColumns().get(1);
        entityTableNameColumn.setCellValueFactory(
                e -> {
                    ObjectBinding<? extends ObservableEntity> src = Bindings.valueAt(replayController.getEntityList(), e.getValue().getIndex());
                    return Bindings.createStringBinding(() -> src.get().getName(), src);
                }
        );

        entityTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            log.info("entity table selection from {} to {}", oldValue, newValue);
            detailTable.setItems(newValue);
        });

        TableColumn<ObservableEntityProperty, String> idColumn =
            (TableColumn<ObservableEntityProperty, String>) detailTable.getColumns().get(0);
        idColumn.setCellValueFactory(param -> param.getValue().indexProperty());
        TableColumn<ObservableEntityProperty, String> nameColumn =
            (TableColumn<ObservableEntityProperty, String>) detailTable.getColumns().get(1);
        nameColumn.setCellValueFactory(param -> param.getValue().nameProperty());
        TableColumn<ObservableEntityProperty, String> valueColumn =
            (TableColumn<ObservableEntityProperty, String>) detailTable.getColumns().get(2);
        valueColumn.setCellValueFactory(param -> param.getValue().valueProperty().asString());

        valueColumn.setCellFactory(v -> new TableCell<ObservableEntityProperty, String>() {
            final Animation animation = new Transition() {
                {
                    setCycleDuration(Duration.millis(500));
                    setInterpolator(Interpolator.EASE_OUT);
                }
                @Override
                protected void interpolate(double frac) {
                    Color col = Color.YELLOW.interpolate(Color.WHITE, frac);
                    getTableRow().setStyle(String.format(
                            "-fx-control-inner-background: #%02X%02X%02X;",
                            (int)(col.getRed() * 255),
                            (int)(col.getGreen() * 255),
                            (int)(col.getBlue() * 255)
                    ));
                }
            };
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(item);
                ObservableEntityProperty oep = (ObservableEntityProperty) getTableRow().getItem();
                if (oep != null) {
                    animation.stop();
                    animation.playFrom(Duration.millis(System.currentTimeMillis() - oep.getLastChangedAt()));
                }
            }
        });

        detailTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        detailTable.setOnKeyPressed(e -> {
            KeyCombination ctrlC = new KeyCodeCombination(KeyCode.C, KeyCodeCombination.CONTROL_DOWN);
            if (ctrlC.match(e)) {
                ClipboardContent cbc = new ClipboardContent();
                cbc.putString(detailTable.getSelectionModel().getSelectedIndices().stream()
                        .map(i -> detailTable.getItems().get(i))
                        .map(p -> String.format("%s %s %s", p.indexProperty().get(), p.nameProperty().get(), p.valueProperty().get()))
                        .collect(Collectors.joining("\n"))
                );
                Clipboard.getSystemClipboard().setContent(cbc);
            }
        });


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

}
