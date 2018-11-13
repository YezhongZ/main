package seedu.scheduler.ui;

import java.net.URL;
import java.util.logging.Logger;

import com.google.common.eventbus.Subscribe;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.layout.Region;
import javafx.scene.web.WebView;
import seedu.scheduler.MainApp;
import seedu.scheduler.commons.core.LogsCenter;
import seedu.scheduler.commons.events.ui.EventPanelSelectionChangedEvent;

/**
 * The Browser Panel of the App.
 */
public class BrowserPanel extends UiPart<Region> {

    public static final String DEFAULT_PAGE = "default.html";
    public static final String CALENDER_PAGE_URL =
            "https://calendar.google.com/calendar/b/2/r/month";

    private static final String FXML = "BrowserPanel.fxml";

    private final Logger logger = LogsCenter.getLogger(getClass());

    @FXML
    private WebView browser;

    public BrowserPanel() {
        super(FXML);

        // To prevent triggering events for typing inside the loaded Web page.
        getRoot().setOnKeyPressed(Event::consume);
        loadDefaultPage();
        runDelayAction(this::loadCalendarPage);
        registerAsAnEventHandler(this);
    }

    /**
     * Run an action later.
     */
    private void runDelayAction(Runnable runnable) {
        new java.util.Timer().schedule(
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        runnable.run();
                    }
                },
                45000
        );
    }

    public void loadCalendarPage() {
        loadPage(CALENDER_PAGE_URL);
    }

    private void loadEventPage() {
        loadPage(CALENDER_PAGE_URL);
    }

    public void loadPage(String url) {
        Platform.runLater(() -> browser.getEngine().load(url));
    }

    public void reloadPage() {
        Platform.runLater(() -> browser.getEngine().reload());
    }

    /**
     * Loads a default HTML file with a background that matches the general theme.
     */
    private void loadDefaultPage() {
        URL defaultPage = MainApp.class.getResource(FXML_FILE_FOLDER + DEFAULT_PAGE);
        loadPage(defaultPage.toExternalForm());
    }

    /**
     * Frees resources allocated to the browser.
     */
    public void freeResources() {
        browser = null;
    }

    @Subscribe
    private void handleEventPanelSelectionChangedEvent(EventPanelSelectionChangedEvent event) {
        logger.info(LogsCenter.getEventHandlingLogMessage(event));
        loadEventPage();
        reloadPage();
    }
}
