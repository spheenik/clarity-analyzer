package skadistats.clarity.analyzer.main;

import com.tobiasdiez.easybind.EasyBind;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.analyzer.Analyzer;
import skadistats.clarity.analyzer.map.MapControl;
import skadistats.clarity.analyzer.replay.NavigationController;
import skadistats.clarity.analyzer.replay.ObservableEntity;
import skadistats.clarity.analyzer.replay.ObservableEntityCellType;
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
    public GridPane rootPane;

    @FXML
    public Button buttonPlay;

    @FXML
    public Button buttonNavigateBackward;

    @FXML
    public Button buttonNavigateForward;

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
    private NavigationController navigationController;

    private int keptEntityIndex = -1;
    private boolean navigating;

    private final ObjectProperty<ObservableList<ObservableEntity>> filteredEntityList = new SimpleObjectProperty<>(FXCollections.emptyObservableList());
    private final ObjectProperty<ObservableList<ObservableEntityProperty>> filteredPropertyList = new SimpleObjectProperty<>(FXCollections.emptyObservableList());

    public void initialize(java.net.URL location, java.util.ResourceBundle resources) {
        preferences = Preferences.userNodeForPackage(this.getClass());
        replayController = new ReplayController(slider);
        navigationController = new NavigationController();
        navigationController.setOnNavigate(entity -> {
            navigating = true;
            try {
                var savedFilter = entityNameFilter.getText();
                entityNameFilter.setText("");
                entityTable.getSelectionModel().select(entity);
                entityNameFilter.setText(savedFilter);
                entityTable.scrollTo(entity);
            } finally {
                navigating = false;
            }
        });

        EasyBind.subscribe(
                entityTable.getSelectionModel().selectedItemProperty(),
                e -> {
                    keptEntityIndex = (e != null) ? e.getIndex() : -1;
                    if (!navigating && e != null && e.getDtClass() != null) {
                        navigationController.recordSelection(e);
                    }
                }
        );

        var runnerIsNull = createBooleanBinding(() -> replayController.getRunner() == null, replayController.runnerProperty());
        buttonPlay.disableProperty().bind(runnerIsNull);
        buttonPlay.textProperty().bind(EasyBind.map(replayController.playingProperty(), playing -> playing ? "⏸" : "⏵"));
        slider.disableProperty().bind(runnerIsNull);

        buttonNavigateBackward.disableProperty().bind(navigationController.canNavigateBackwardProperty().not().or(runnerIsNull));
        buttonNavigateForward.disableProperty().bind(navigationController.canNavigateForwardProperty().not().or(runnerIsNull));

        EasyBind.subscribe(replayController.entityListProperty(), navigationController::setEntityList);

        labelTick.textProperty().bind(replayController.tickProperty().asString());
        labelLastTick.textProperty().bind(replayController.lastTickProperty().asString());

        // filtered entity list
        filteredEntityList.bind(createObjectBinding(() -> {
                var src = replayController.getEntityList();
                if (src == null) return FXCollections.emptyObservableList();
                var filteredList = new FilteredList<>(src);
                filteredList.predicateProperty().bind(createObjectBinding(() -> {
                        var filter = entityNameFilter.getText();
                        var kept = keptEntityIndex;
                        return e -> {
                            if (hideEmptySlots.isSelected() && e.getDtClass() == null) {
                                return false;
                            }
                            if (e.getIndex() == kept) {
                                return true;
                            }
                            if (filter.isEmpty()) {
                                return true;
                            }
                            if (!e.getName().toLowerCase().contains(filter.toLowerCase())) {
                                return false;
                            }
                            return true;
                        };
                    },
                    entityNameFilter.textProperty(), hideEmptySlots.selectedProperty()
                ));
                return filteredList;
            },
            replayController.entityListProperty()
        ));

        // filtered property list
        filteredPropertyList.bind(createObjectBinding(() -> {
                var src = entityTable.getSelectionModel().selectedItemProperty().get();
                if (src == null) return FXCollections.emptyObservableList();
                var filteredList = new FilteredList<>(src);
                filteredList.predicateProperty().bind(createObjectBinding(() -> {
                        var filter = propertyNameFilter.getText();
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
        createTableCell(entityTable, "class", String.class, col -> {
                col.setCellValueFactory(f -> {
                    ObjectBinding<? extends ObservableEntity> src = valueAt(replayController.getEntityList(), f.getValue().getIndex());
                    return createStringBinding(() -> src.get().getName(), src);
                });
                col.setCellFactory(c -> new TableCell<>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                            setStyle("");
                        } else {
                            setText(item);
                            var filter = entityNameFilter.getText();
                            if (!filter.isEmpty() && !item.toLowerCase().contains(filter.toLowerCase())) {
                                setStyle("-fx-font-style: italic;");
                            } else {
                                setStyle("");
                            }
                        }
                    }
                });
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
        createTableCell(detailTable, "value", ObservableEntityProperty.class, col -> {
                    col.setCellValueFactory(f -> new ReadOnlyObjectWrapper<>(f.getValue()));
                    col.setCellFactory(v -> {
                        var cell = new TableCell<ObservableEntityProperty, ObservableEntityProperty>();

                        cell.graphicProperty().bind(cell.itemProperty()
                                .map(ObservableEntityProperty::getCellType)
                                .map(cellType -> {
                                    Labeled graphic;
                                    if (cellType == ObservableEntityCellType.HANDLE) {
                                        var engineType = replayController.getEntityList().getEngineType();
                                        var handleProperty = cell.itemProperty()
                                                .flatMap(ObservableEntityProperty::valueProperty)
                                                .map(value -> {
                                                    try {
                                                        return (Integer) value;
                                                    } catch (Exception e) {
                                                        return engineType.emptyHandle();
                                                    }
                                                });
                                        var isEmptyHandle = handleProperty.map(h -> engineType.emptyHandle() == h);

                                        var link = new Hyperlink();
                                        link.disableProperty().bind(isEmptyHandle);
                                        link.setOnAction(event -> {
                                            var handle = handleProperty.getValue();
                                            if (handle != null && handle != engineType.emptyHandle()) {
                                                navigationController.navigateTo(handle);

                                            }
                                        });
                                        graphic = link;
                                    } else {
                                        graphic = new Label();
                                    }
                                    graphic.setPadding(new Insets(0, 1, 0, 1));
                                    graphic.textProperty().bind(cell.itemProperty().flatMap(ObservableEntityProperty::valueAsStringProperty));
                                    return graphic;
                                })
                        );

                        Animation animation = new Transition() {
                            {
                                setCycleDuration(Duration.millis(500));
                                setInterpolator(Interpolator.EASE_OUT);
                            }

                            @Override
                            protected void interpolate(double frac) {
                                var col = Color.YELLOW.interpolate(Color.WHITE, frac);
                                cell.getTableRow().setStyle(String.format(
                                        "-fx-control-inner-background: #%02X%02X%02X;",
                                        (int) (col.getRed() * 255),
                                        (int) (col.getGreen() * 255),
                                        (int) (col.getBlue() * 255)
                                ));
                            }
                        };

                        cell.itemProperty()
                                .flatMap(ObservableEntityProperty::valueProperty)
                                .addListener((obs, oldVal, newVal) -> {
                                    animation.stop();
                                    var item = cell.getItem();
                                    if (item != null) {
                                        animation.playFrom(Duration.millis(System.currentTimeMillis() - item.getLastChangedAtMillis()));
                                    } else if (cell.getTableRow() != null) {
                                        cell.getTableRow().setStyle("");
                                    }
                                });

                        return cell;
                    });
                }
        );
        detailTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        detailTable.setOnKeyPressed(this::handleDetailTableKeyPressed);

        // map control
        mapControl.entityListProperty().bind(replayController.entityListProperty());

        // keyboard shortcuts (LEFT/RIGHT for entity history navigation, SPACE for play/pause)
        rootPane.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getTarget() instanceof TextField) return;
            switch (e.getCode()) {
                case LEFT -> {
                    if (!buttonNavigateBackward.isDisabled()) navigationController.navigateBackward();
                    e.consume();
                }
                case RIGHT -> {
                    if (!buttonNavigateForward.isDisabled()) navigationController.navigateForward();
                    e.consume();
                }
                case SPACE -> {
                    if (!buttonPlay.isDisabled()) replayController.setPlaying(!replayController.isPlaying());
                    e.consume();
                }
            }
        });
    }

    private <S, V> void createTableCell(TableView<S> tableView, String header, Class<V> valueClass, Consumer<TableColumn<S, V>> columnInitializer) {
        var column = new TableColumn<S, V>(header);
        tableView.getColumns().add(column);
        columnInitializer.accept(column);
    }

    public void actionQuit(ActionEvent actionEvent) {
        replayController.haltIfRunning();
        Analyzer.primaryStage.close();
    }

    public void actionOpen(ActionEvent actionEvent) {
        var fileChooser = new FileChooser();
        fileChooser.setTitle("Load a replay");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Replay files", "*.dem"),
            new FileChooser.ExtensionFilter("All files", "*")
        );
        var dir = new File(preferences.get("fileChooserPath", "."));
        if (!dir.isDirectory()) {
            dir = new File(".");
        }
        fileChooser.setInitialDirectory(dir);
        var replayFile = fileChooser.showOpenDialog(Analyzer.primaryStage);
        if (replayFile == null) {
            return;
        }
        load(replayFile);
    }

    public void load(File replayFile) {
        preferences.put("fileChooserPath", replayFile.getParent());
        replayController.load(replayFile);
    }

    public void clickPlay(ActionEvent actionEvent) {
        replayController.setPlaying(!replayController.isPlaying());
    }

    public void navigateBackward(ActionEvent actionEvent) {
        navigationController.navigateBackward();
    }

    public void navigateForward(ActionEvent actionEvent) {
        navigationController.navigateForward();
    }

    private void handleDetailTableKeyPressed(KeyEvent e) {
        KeyCombination ctrlC = new KeyCodeCombination(KeyCode.C, KeyCodeCombination.CONTROL_DOWN);
        if (ctrlC.match(e)) {
            var cbc = new ClipboardContent();
            cbc.putString(detailTable.getSelectionModel().getSelectedIndices().stream()
                    .map(i -> detailTable.getItems().get(i))
                    .map(p -> String.format("%s %s %s", p.getFieldPath(), p.getName(), p.getValue()))
                    .collect(Collectors.joining("\n"))
            );
            Clipboard.getSystemClipboard().setContent(cbc);
        }
    }

}
