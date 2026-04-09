package skadistats.clarity.analyzer.replay;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class NavigationController {

    private final ObservableList<Integer> indexes = FXCollections.observableArrayList();

    private final IntegerProperty idx = new SimpleIntegerProperty();

    private final BooleanBinding canNavigateBackward;
    private final BooleanBinding canNavigateForward;

    public NavigationController() {
        this.canNavigateBackward = Bindings.createBooleanBinding(() -> {
            var result = idx.get() > 0;
            System.out.println("canNavigateBackward " + result);
            return result;
        }, idx);
        this.canNavigateForward = Bindings.createBooleanBinding(() -> {
            var result = idx.get() + 1 < indexes.size();
            System.out.println("canNavigateForward " + result);
            return result;
        }, indexes, idx);
    }

    public Boolean getCanNavigateBackward() {
        return canNavigateBackward.get();
    }

    public BooleanBinding canNavigateBackwardProperty() {
        return canNavigateBackward;
    }

    public Boolean getCanNavigateForward() {
        return canNavigateForward.get();
    }

    public BooleanBinding canNavigateForwardProperty() {
        return canNavigateForward;
    }

}
