package skadistats.clarity.analyzer;

import com.airhacks.afterburner.injection.Injector;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import skadistats.clarity.analyzer.main.MainView;

public class Main extends Application {

    public static Stage primaryStage;

    @Override
    public void start(Stage primaryStage) throws Exception{
        this.primaryStage = primaryStage;
        MainView appView = new MainView();
        Scene mainScene = new Scene(appView.getView());
        primaryStage.setTitle("Clarity Analyzer");
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/images/dota_2_icon.png")));
        primaryStage.setScene(mainScene);
        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        Injector.forgetAll();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
