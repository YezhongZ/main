package seedu.scheduler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.logging.Logger;

import org.apache.log4j.BasicConfigurator;

import com.google.common.eventbus.Subscribe;
import com.google.common.io.Files;


import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import seedu.scheduler.commons.core.Config;
import seedu.scheduler.commons.core.EventsCenter;
import seedu.scheduler.commons.core.LogsCenter;
import seedu.scheduler.commons.core.Version;
import seedu.scheduler.commons.events.ui.ExitAppRequestEvent;
import seedu.scheduler.commons.exceptions.DataConversionException;
import seedu.scheduler.commons.util.ConfigUtil;
import seedu.scheduler.commons.util.StringUtil;
import seedu.scheduler.logic.Logic;
import seedu.scheduler.logic.LogicManager;
import seedu.scheduler.model.Model;
import seedu.scheduler.model.ModelManager;
import seedu.scheduler.model.PopUpManager;
import seedu.scheduler.model.ReadOnlyScheduler;
import seedu.scheduler.model.Scheduler;
import seedu.scheduler.model.UserPrefs;
import seedu.scheduler.model.util.SampleSchedulerDataUtil;
import seedu.scheduler.storage.JsonUserPrefsStorage;
import seedu.scheduler.storage.SchedulerStorage;
import seedu.scheduler.storage.Storage;
import seedu.scheduler.storage.StorageManager;
import seedu.scheduler.storage.UserPrefsStorage;
import seedu.scheduler.storage.XmlSchedulerStorage;
import seedu.scheduler.ui.Ui;
import seedu.scheduler.ui.UiManager;

/**
 * The main entry point to the application.
 */
public class MainApp extends Application {

    public static final Version VERSION = new Version(1, 4, 0, true);

    private static final Logger logger = LogsCenter.getLogger(MainApp.class);

    protected Ui ui;
    protected Logic logic;
    protected Storage storage;
    protected Model model;
    protected Config config;
    protected UserPrefs userPrefs;
    protected PopUpManager popUp;


    @Override
    public void init() throws Exception {
        logger.info("=============================[ Initializing Scheduler ]====================+=======");
        super.init();
        Path currentRelativePath = Paths.get("");
        String s = currentRelativePath.toAbsolutePath().toString();
        File path = new File(s+"/tokens");
        File file = new File(s+"/tokens/mode.txt");
        if (!file.exists() && !file.isDirectory()) {
            path.mkdirs();
            file.createNewFile();
            Writer writer = new BufferedWriter(new FileWriter(file));
            writer.write("Disabled");
            writer.close();
            logger.info("Status file initiated.");
        }

        AppParameters appParameters = AppParameters.parse(getParameters());
        config = initConfig(appParameters.getConfigPath());

        UserPrefsStorage userPrefsStorage = new JsonUserPrefsStorage(config.getUserPrefsFilePath());
        userPrefs = initPrefs(userPrefsStorage);
        SchedulerStorage schedulerStorage = new XmlSchedulerStorage(userPrefs.getSchedulerFilePath());
        storage = new StorageManager(schedulerStorage, userPrefsStorage);

        initLogging(config);
        BasicConfigurator.configure();

        model = initModelManager(storage, userPrefs);

        logic = new LogicManager(model);

        ui = new UiManager(logic, config, userPrefs);

        popUp = initPopUpManager();

        initEventsCenter();
    }

    /**
     * Returns a {@code ModelManager} with the data from {@code storage}'s scheduler and {@code userPrefs}. <br> The
     * data from the sample scheduler will be used instead if {@code storage}'s scheduler is not found, or an empty
     * scheduler will be used instead if errors occur when reading {@code storage}'s scheduler.
     */
    private Model initModelManager(Storage storage, UserPrefs userPrefs) {
        ReadOnlyScheduler initialSchedulerData = initSchedulerData(storage);

        return new ModelManager(initialSchedulerData, userPrefs);
    }

    /**
     * Returns a {@code ReadOnlyScheduler}. The file {@code SampleSchedulerDataUtil#getSampleScheduler} will be used if
     * there is no scheduler present in storage. A new Scheduler will be used if error occurs.
     */
    private ReadOnlyScheduler initSchedulerData(Storage storage) {
        Optional<ReadOnlyScheduler> schedulerOptional;
        ReadOnlyScheduler initialSchedulerData;
        try {
            schedulerOptional = storage.readScheduler();
            if (!schedulerOptional.isPresent()) {
                logger.info("Scheduler data file not found. Will be starting with a sample Scheduler");
            }
            initialSchedulerData = schedulerOptional.orElseGet(SampleSchedulerDataUtil::getSampleScheduler);
        } catch (DataConversionException e) {
            logger.warning("Scheduler data file not in the correct format. Will be starting with an empty Scheduler");
            initialSchedulerData = new Scheduler();
        } catch (IOException e) {
            logger.warning("Problem while scheduler data from the file. Will be starting with an empty Scheduler");
            initialSchedulerData = new Scheduler();
        }
        return initialSchedulerData;
    }

    private void initLogging(Config config) {
        LogsCenter.init(config);
    }

    /**
     * Returns a {@code Config} using the file at {@code configFilePath}. <br> The default file path {@code
     * Config#DEFAULT_CONFIG_FILE} will be used instead if {@code configFilePath} is null.
     */
    protected Config initConfig(Path configFilePath) {
        Config initializedConfig;
        Path configFilePathUsed;

        configFilePathUsed = Config.DEFAULT_CONFIG_FILE;

        if (configFilePath != null) {
            logger.info("Custom Config file specified " + configFilePath);
            configFilePathUsed = configFilePath;
        }

        logger.info("Using config file : " + configFilePathUsed);

        try {
            Optional<Config> configOptional = ConfigUtil.readConfig(configFilePathUsed);
            initializedConfig = configOptional.orElse(new Config());
        } catch (DataConversionException e) {
            logger.warning("Config file at " + configFilePathUsed + " is not in the correct format. "
                    + "Using default config properties");
            initializedConfig = new Config();
        }

        //Update config file in case it was missing to begin with or there are new/unused fields
        try {
            ConfigUtil.saveConfig(initializedConfig, configFilePathUsed);
        } catch (IOException e) {
            logger.warning("Failed to save config file : " + StringUtil.getDetails(e));
        }
        return initializedConfig;
    }

    /**
     * Returns a {@code UserPrefs} using the file at {@code storage}'s user prefs file path, or a new {@code UserPrefs}
     * with default configuration if errors occur when reading from the file.
     */
    protected UserPrefs initPrefs(UserPrefsStorage storage) {
        Path prefsFilePath = storage.getUserPrefsFilePath();
        logger.info("Using prefs file : " + prefsFilePath);

        UserPrefs initializedPrefs;
        try {
            Optional<UserPrefs> prefsOptional = storage.readUserPrefs();
            initializedPrefs = prefsOptional.orElse(new UserPrefs());
        } catch (DataConversionException e) {
            logger.warning("UserPrefs file at " + prefsFilePath + " is not in the correct format. "
                    + "Using default user prefs");
            initializedPrefs = new UserPrefs();
        } catch (IOException e) {
            logger.warning("Problem while reading from the file. Will be starting with an empty Scheduler");
            initializedPrefs = new UserPrefs();
        }

        //Update prefs file in case it was missing to begin with or there are new/unused fields
        try {
            storage.saveUserPrefs(initializedPrefs);
        } catch (IOException e) {
            logger.warning("Failed to save config file : " + StringUtil.getDetails(e));
        }

        return initializedPrefs;
    }

    private void initEventsCenter() {
        EventsCenter.getInstance().registerHandler(this);
    }

    private PopUpManager initPopUpManager() {
        popUp = PopUpManager.getInstance();
        popUp.syncPopUpInfo(model.getScheduler());
        return popUp;
    }

    @Override
    public void start(Stage primaryStage) {
        logger.info("Starting Scheduler " + MainApp.VERSION);
        ui.start(primaryStage);
        popUp.startRunning();
    }

    @Override
    public void stop() {
        logger.info("============================ [ Stopping Scheduler ] =============================");
        ui.stop();
        try {
            storage.saveUserPrefs(userPrefs);
        } catch (IOException e) {
            logger.severe("Failed to save preferences " + StringUtil.getDetails(e));
        }
        Platform.exit();
        System.exit(0);
    }

    @Subscribe
    public void handleExitAppRequestEvent(ExitAppRequestEvent event) {
        logger.info(LogsCenter.getEventHandlingLogMessage(event));
        stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
