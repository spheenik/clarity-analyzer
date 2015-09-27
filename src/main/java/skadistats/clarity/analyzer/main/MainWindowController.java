package skadistats.clarity.analyzer.main;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.util.converter.NumberStringConverter;
import org.controlsfx.dialog.Dialogs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.analyzer.PrimaryStage;
import skadistats.clarity.analyzer.replay.ObservableEntity;
import skadistats.clarity.analyzer.replay.ObservableEntityList;
import skadistats.clarity.analyzer.replay.ObservableEntityProperty;
import skadistats.clarity.analyzer.replay.ReplayController;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.function.Predicate;
import java.util.prefs.Preferences;

public class MainWindowController implements Initializable {

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

    @Inject
    private PrimaryStage primaryStage;

    @Inject
    private Preferences preferences;

    @Inject
    private ReplayController replayController;

    private FilteredList<ObservableEntity> filteredEntityList = null;
    private Predicate<ObservableEntity> filterFunc = new Predicate<ObservableEntity>() {
        @Override
        public boolean test(ObservableEntity e) {
            String filter = entityNameFilter.getText();
            if (filter.length() > 0) {
                return e != null && e.getName().contains(filter);
            }
            return true;
        }
    };


    public void initialize(java.net.URL location, java.util.ResourceBundle resources) {
        BooleanBinding runnerIsNull = Bindings.createBooleanBinding(() -> replayController.getRunner() == null, replayController.runnerProperty());
        buttonPlay.disableProperty().bind(runnerIsNull.or(replayController.playingProperty()));
        buttonPause.disableProperty().bind(runnerIsNull.or(replayController.playingProperty().not()));
        slider.disableProperty().bind(runnerIsNull);
        replayController.changingProperty().bind(slider.valueChangingProperty());

        labelTick.textProperty().bindBidirectional(replayController.tickProperty(), new NumberStringConverter());
        labelLastTick.textProperty().bindBidirectional(replayController.lastTickProperty(), new NumberStringConverter());

        slider.maxProperty().bind(replayController.lastTickProperty());
        replayController.tickProperty().addListener((observable, oldValue, newValue) -> {
            if (!slider.isValueChanging()) {
                slider.setValue(newValue.intValue());
            }
        });
        slider.valueProperty().addListener((observable, oldValue, newValue) -> {
            replayController.getRunner().setDemandedTick(newValue.intValue());
        });

        TableColumn<ObservableEntity, String> entityTableIdColumn = (TableColumn<ObservableEntity, String>) entityTable.getColumns().get(0);
        entityTableIdColumn.setCellValueFactory(param -> param.getValue() != null ? param.getValue().indexProperty() : new ReadOnlyStringWrapper(""));
        TableColumn<ObservableEntity, String> entityTableNameColumn = (TableColumn<ObservableEntity, String>) entityTable.getColumns().get(1);
        entityTableNameColumn.setCellValueFactory(param -> param.getValue() != null ? param.getValue().nameProperty() : new ReadOnlyStringWrapper(""));
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
        valueColumn.setCellValueFactory(param -> param.getValue().valueProperty());

        entityNameFilter.textProperty().addListener(observable -> {
            if (filteredEntityList != null) {
                filteredEntityList.setPredicate(null);
                filteredEntityList.setPredicate(filterFunc);
            }
        });
    }
    public void actionQuit(ActionEvent actionEvent) {
        primaryStage.getStage().close();
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
        File f = fileChooser.showOpenDialog(primaryStage.getStage());
        if (f == null) {
            return;
        }
        preferences.put("fileChooserPath", f.getParent());
        try {
            ObservableEntityList entityList = replayController.load(f);
            filteredEntityList = entityList.filtered(filterFunc);
            entityTable.setItems(filteredEntityList);
        } catch (Exception e) {
            Dialogs.create().title("Error loading replay").showException(e);
        }
    }

    public void clickPlay(ActionEvent actionEvent) {
        replayController.setPlaying(true);
    }

    public void clickPause(ActionEvent actionEvent) {
        replayController.setPlaying(false);

    }
}
