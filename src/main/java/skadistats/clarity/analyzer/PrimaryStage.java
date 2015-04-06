package skadistats.clarity.analyzer;

import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import skadistats.clarity.analyzer.cdi.FXMLScene;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class PrimaryStage {

    private Stage stage;

    @Inject @FXMLScene("/main.fxml")
    private Scene mainScene;

    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        primaryStage.setTitle("Clarity Analyzer");
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/images/dota_2_icon.png")));
        primaryStage.setScene(mainScene);
        primaryStage.show();
    }

    public Stage getStage() {
        return stage;
    }


}
