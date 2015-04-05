package skadistats.clarity.analyzer;

import javafx.application.Application;
import javafx.stage.Stage;
import org.jboss.weld.environment.se.Weld;

public class Main extends Application {

    private Weld weld;

    @Override
    public void start(Stage primaryStage) throws Exception{
        weld.initialize().instance().select(PrimaryStage.class).get().start(primaryStage);
    }

    @Override
    public void init() throws Exception {
        weld = new Weld();
    }

    @Override
    public void stop() throws Exception {
        weld.shutdown();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
