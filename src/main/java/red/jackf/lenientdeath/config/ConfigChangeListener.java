package red.jackf.lenientdeath.config;

import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;

import java.io.File;

class ConfigChangeListener {
    public static final ConfigChangeListener INSTANCE = new ConfigChangeListener();
    private static final long INTERVAL_MILLIS = 1000L;
    private final FileAlterationMonitor monitor;
    private boolean running = false;

    private ConfigChangeListener() {
        var filter = FileFilterUtils.and(
                FileFilterUtils.fileFileFilter(),
                FileFilterUtils.nameFileFilter(ConfigHandler.PATH.getFileName().toString()));
        var observer = new FileAlterationObserver(ConfigHandler.PATH.getParent().toFile(), filter);
        observer.addListener(new FileAlterationListenerAdaptor() {
            @Override
            public void onFileChange(File file) {
                ConfigHandler.LOGGER.info("Config file changed, reloading");
                LenientDeathConfig.INSTANCE.load();
            }
        });

        this.monitor = new FileAlterationMonitor(INTERVAL_MILLIS);
        this.monitor.setThreadFactory(r -> {
            var thread = new Thread(r);
            thread.setName("Lenient Death Config Watcher");
            thread.setDaemon(true);
            return thread;
        });
        this.monitor.addObserver(observer);
    }

    protected void start() {
        if (running) return;
        try {
            this.monitor.start();
            running = true;
            ConfigHandler.LOGGER.debug("Started config watcher");
        } catch (Exception e) {
            ConfigHandler.LOGGER.error("Couldn't start config file watcher", e);
        }
    }

    protected void stop() {
        if (!running) return;
        try {
            this.monitor.stop();
            running = false;
            ConfigHandler.LOGGER.debug("Stopped config watcher");
        } catch (Exception e) {
            ConfigHandler.LOGGER.error("Couldn't stop config file watcher", e);
        }
    }
}
