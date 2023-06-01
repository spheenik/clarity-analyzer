package skadistats.clarity.analyzer;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import skadistats.clarity.analyzer.util.FXResources;

public class Analyzer extends Application {

    public static Stage primaryStage;

    @Override
    public void start(Stage primaryStage) throws Exception{
        this.primaryStage = primaryStage;

        FXMLLoader fxmlLoader = new FXMLLoader(FXResources.getResource("main.fxml"));
        Parent parent = fxmlLoader.load();

        Scene mainScene = new Scene(parent);
        primaryStage.setTitle("Clarity Analyzer");
        primaryStage.getIcons().add(new Image(FXResources.getResourceAsStream("images/dota_2_icon.png")));
        primaryStage.setScene(mainScene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
