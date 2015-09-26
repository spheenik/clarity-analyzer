package skadistats.clarity.analyzer.main;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.util.converter.NumberStringConverter;
import org.controlsfx.dialog.Dialogs;
import skadistats.clarity.analyzer.PrimaryStage;
import skadistats.clarity.analyzer.replay.ObservableEntityList;
import skadistats.clarity.analyzer.replay.ReplayController;
import skadistats.clarity.analyzer.replay.WrappedEntity;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.prefs.Preferences;

public class MainWindowController implements Initializable {

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
    public TableView<WrappedEntity> entityTable;

    @FXML
    public TableView<WrappedEntity.EntityProperty> detailTable;

    @Inject
    private PrimaryStage primaryStage;

    @Inject
    private Preferences preferences;

    @Inject
    private ReplayController replayController;

    @Inject
    private ObservableEntityList entityList;

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
            slider.setValue(newValue.intValue());
        });
        slider.valueProperty().addListener((observable, oldValue, newValue) -> {
            replayController.getRunner().setDemandedTick(newValue.intValue());
        });

        entityTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {

            System.out.println(newValue);

            if (newValue == null) {
                return;
            }

            TableColumn<WrappedEntity.EntityProperty, String> idColumn =
                (TableColumn<WrappedEntity.EntityProperty, String>) detailTable.getColumns().get(0);
            idColumn.setCellValueFactory(newValue.getIndexCellFactory());

            TableColumn<WrappedEntity.EntityProperty, String> nameColumn =
                (TableColumn<WrappedEntity.EntityProperty, String>) detailTable.getColumns().get(1);
            nameColumn.setCellValueFactory(newValue.getNameCellFactory());

            TableColumn<WrappedEntity.EntityProperty, String> valueColumn =
                (TableColumn<WrappedEntity.EntityProperty, String>) detailTable.getColumns().get(2);
            valueColumn.setCellValueFactory(newValue.getValueCellFactory());

            detailTable.setItems(newValue.getProperties());
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
            replayController.load(f);
        } catch (Exception e) {
            Dialogs.create().title("Error loading replay").showException(e);
        }
        entityTable.setItems(entityList.getEntities());

        TableColumn<WrappedEntity, Integer> idColumn = (TableColumn<WrappedEntity, Integer>) entityTable.getColumns().get(0);
        idColumn.setCellValueFactory(entityList.getIndexCellFactory());

        TableColumn<WrappedEntity, String> clsColumn = (TableColumn<WrappedEntity, String>) entityTable.getColumns().get(1);
        clsColumn.setCellValueFactory(entityList.getDtClassCellFactory());
    }

    public void clickPlay(ActionEvent actionEvent) {
        replayController.setPlaying(true);
    }

    public void clickPause(ActionEvent actionEvent) {
        replayController.setPlaying(false);

    }
}
