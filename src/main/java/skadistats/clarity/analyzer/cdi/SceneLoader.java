package skadistats.clarity.analyzer.cdi;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import java.io.IOException;

public class SceneLoader {

    @Inject
    FXMLLoader loader;

    @Produces
    @FXMLScene
    public Scene loadScene(InjectionPoint ip) throws IOException {
        String name = ip.getAnnotated().getAnnotation(FXMLScene.class).value();
        Parent root = loader.load(getClass().getResourceAsStream(name));
        return new Scene(root);
    }

}
