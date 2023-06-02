package skadistats.clarity.analyzer.main;

import com.tobiasdiez.easybind.EasyBind;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
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
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.analyzer.Analyzer;
import skadistats.clarity.analyzer.map.MapControl;
import skadistats.clarity.analyzer.replay.ObservableEntity;
import skadistats.clarity.analyzer.replay.ObservableEntityList;
import skadistats.clarity.analyzer.replay.ObservableEntityProperty;
import skadistats.clarity.analyzer.replay.ReplayController;
import skadistats.clarity.analyzer.util.TickHelper;

import java.io.File;
import java.util.function.Consumer;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import static javafx.beans.binding.Bindings.createBooleanBinding;
import static javafx.beans.binding.Bindings.createObjectBinding;
import static javafx.beans.binding.Bindings.createStringBinding;
import static javafx.beans.binding.Bindings.valueAt;

public class MainView implements Initializable {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    @FXML
    public Button buttonPlay;

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
    public CheckBox hideEmptySlots;

    @FXML
    public TextField propertyNameFilter;

    @FXML
    public CheckBox onlyRecentlyUpdated;

    @FXML
    private MapControl mapControl;

    private Preferences preferences;

    private ReplayController replayController;
    private final ObjectProperty<FilteredList<ObservableEntity>> filteredEntityList = new SimpleObjectProperty<>();
    private final ObjectProperty<FilteredList<ObservableEntityProperty>> filteredPropertyList = new SimpleObjectProperty<>();

    public void initialize(java.net.URL location, java.util.ResourceBundle resources) {
        preferences = Preferences.userNodeForPackage(this.getClass());
        replayController = new ReplayController(slider);

        BooleanBinding runnerIsNull = createBooleanBinding(() -> replayController.getRunner() == null, replayController.runnerProperty());
        buttonPlay.disableProperty().bind(runnerIsNull);
        buttonPlay.textProperty().bind(EasyBind.map(replayController.playingProperty(), playing -> playing ? "⏸" : "⏵"));
        slider.disableProperty().bind(runnerIsNull);

        labelTick.textProperty().bind(replayController.tickProperty().asString());
        labelLastTick.textProperty().bind(replayController.lastTickProperty().asString());

        // filtered entity list
        filteredEntityList.bind(createObjectBinding(() -> {
                ObservableEntityList src = replayController.getEntityList();
                if (src == null) return null;
                FilteredList<ObservableEntity> filteredList = new FilteredList<>(src);
                filteredList.predicateProperty().bind(createObjectBinding(() -> {
                        String filter = entityNameFilter.getText();
                        return e ->
                            (!hideEmptySlots.isSelected() || e.getDtClass() != null)
                                && (filter.isEmpty() || e.getName().toLowerCase().contains(filter.toLowerCase()));
                    },
                    entityNameFilter.textProperty(), hideEmptySlots.selectedProperty()
                ));
                return filteredList;
            },
            replayController.entityListProperty()
        ));

        // filtered property list
        filteredPropertyList.bind(createObjectBinding(() -> {
                ObservableEntity src = entityTable.getSelectionModel().selectedItemProperty().get();
                if (src == null) return null;
                FilteredList<ObservableEntityProperty> filteredList = new FilteredList<>(src);
                filteredList.predicateProperty().bind(createObjectBinding(() -> {
                        String filter = propertyNameFilter.getText();
                        return oe ->
                            (!onlyRecentlyUpdated.isSelected() || TickHelper.isRecent(oe.getLastChangedAtTick()))
                                && (filter.isEmpty() || oe.getName().toLowerCase().contains(filter.toLowerCase()));
                    },
                    propertyNameFilter.textProperty(), onlyRecentlyUpdated.selectedProperty(), src.recentChangesHashProperty()
                ));
                return filteredList;
            },
            entityTable.getSelectionModel().selectedItemProperty()
        ));

        entityTable.itemsProperty().bind(filteredEntityList);
        detailTable.itemsProperty().bind(filteredPropertyList);

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
                    col.setCellValueFactory(f -> f.getValue().valueAsStringProperty());
                    col.setCellFactory(v -> new EntityValueTableCell());
                }
        );
        detailTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        detailTable.setOnKeyPressed(this::handleDetailTableKeyPressed);

        // map control
        mapControl.entityListProperty().bind(replayController.entityListProperty());
    }

    private <S, V> void createTableCell(TableView<S> tableView, String header, Class<V> valueClass, Consumer<TableColumn<S, V>> columnInitializer) {
        TableColumn<S, V> column = new TableColumn<>(header);
        tableView.getColumns().add(column);
        columnInitializer.accept(column);
    }

    public void actionQuit(ActionEvent actionEvent) {
        replayController.haltIfRunning();
        Analyzer.primaryStage.close();
    }

    public void actionOpen(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load a replay");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Replay files", "*.dem"),
            new FileChooser.ExtensionFilter("All files", "*")
        );
        File dir = new File(preferences.get("fileChooserPath", "."));
        if (!dir.isDirectory()) {
            dir = new File(".");
        }
        fileChooser.setInitialDirectory(dir);
        File replayFile = fileChooser.showOpenDialog(Analyzer.primaryStage);
        if (replayFile == null) {
            return;
        }
        preferences.put("fileChooserPath", replayFile.getParent());
        replayController.load(replayFile);
    }

    public void clickPlay(ActionEvent actionEvent) {
        replayController.setPlaying(!replayController.isPlaying());
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
