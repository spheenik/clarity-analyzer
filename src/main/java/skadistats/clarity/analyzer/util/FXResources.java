package skadistats.clarity.analyzer.util;

import java.io.InputStream;
import java.net.URL;

public class FXResources {

    public static URL getResource(String name) {
        return FXResources.class.getResource("/fx/" + name);
    }

    public static InputStream getResourceAsStream(String name) {
        return FXResources.class.getResourceAsStream("/fx/" + name);
    }

}
