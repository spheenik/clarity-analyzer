package skadistats.clarity.analyzer.cdi;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import java.util.prefs.Preferences;

public class PreferencesProducer {

    @Produces
    public Preferences getPreferences(InjectionPoint ip) {
        return Preferences.userNodeForPackage(ip.getBean().getBeanClass());
    }

}
