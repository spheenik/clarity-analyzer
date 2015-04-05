package skadistats.clarity.analyzer.cdi;


import javafx.fxml.FXMLLoader;
import javafx.util.Callback;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import java.nio.charset.StandardCharsets;

public class FXMLLoaderProducer implements Callback<Class<?>, Object> {

    @Inject
    Instance<Object> instance;

    @Override
    public Object call(Class<?> param) {
        return instance.select(param).get();
    }

    @Produces
    public FXMLLoader createLoader() {
        FXMLLoader loader = new FXMLLoader(StandardCharsets.UTF_8);
        loader.setControllerFactory(this);
        return loader;
   }
}