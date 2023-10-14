package red.jackf.lenientdeath.config;

import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;

import java.io.File;

public class ConfigChangeListener {
    public static final ConfigChangeListener INSTANCE = new ConfigChangeListener();
    private static final long INTERVAL_MILLIS = 1000L;

    private ConfigChangeListener() {}

    protected void setup() {
        var filter = FileFilterUtils.and(
                FileFilterUtils.fileFileFilter(),
                FileFilterUtils.nameFileFilter(ConfigHandler.PATH.getFileName().toString()));
        var observer = new FileAlterationObserver(ConfigHandler.PATH.getParent().toFile(), filter);
        observer.addListener(new FileAlterationListenerAdaptor() {
            @Override
            public void onFileChange(File file) {
                ConfigHandler.LOGGER.info("Config changed, reloading");
                LenientDeathConfig.INSTANCE.load();
            }
        });

        var monitor = new FileAlterationMonitor(INTERVAL_MILLIS);
        monitor.addObserver(observer);
        try {
            monitor.start();
            ConfigHandler.LOGGER.debug("Setup config watcher");
        } catch (Exception e) {
            ConfigHandler.LOGGER.error("Couldn't setup config file watcher", e);
        }
    }
}
