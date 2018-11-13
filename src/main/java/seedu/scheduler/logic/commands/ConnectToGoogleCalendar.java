package seedu.scheduler.logic.commands;

import static com.google.api.client.util.DateTime.parseRfc3339;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.URL;
import java.net.URLConnection;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event.Reminders;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.EventReminder;
import com.google.api.services.calendar.model.Events;

import seedu.scheduler.commons.core.LogsCenter;
import seedu.scheduler.commons.util.EventFormatUtil;
import seedu.scheduler.logic.commands.exceptions.CommandException;
import seedu.scheduler.model.event.Event;
import seedu.scheduler.model.event.ReminderDurationList;
import seedu.scheduler.ui.UiManager;

/**
 * Methods related to the connection with Google Calendar.
 */
public class ConnectToGoogleCalendar {
    /**
     * Google Calender related setup
     */
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList(CalendarScopes.CALENDAR);
    private static final String CREDENTIALS_FILE_PATH = "/credentials/credentials.json";
    private static final String CALENDAR_NAME = "primary";
    /**
     * Messages
     */
    private static final String MESSAGE_NOT_FOUND = "Event not found on Google Calendar."
            + "Operation effect is only local.";
    private static final String MESSAGE_IO_ERROR = "Unable to retrieve event from Google Calendar."
            + "Please check your network and check its existence on Google Calendar.";
    private static final String MESSAGE_READ_WRITE_ERROR =
            "Unable to write/read the status to the local status indicator file";
    private static final String MESSAGE_NOT_ENABLED = "Google Service not enabled.";
    private static final Logger logger = LogsCenter.getLogger(UiManager.class);

    /**
     * Static method to check/set status
     */
    public static boolean isGoogleCalendarEnabled() throws CommandException {
        return checkStatus("Enabled");
    }

    public static boolean isGoogleCalendarDisabled() throws CommandException {
        return checkStatus("Disabled");
    }

    public static void setGoogleCalendarEnabled() throws CommandException {
        setGoogleCalendarStatus("Enabled");
    }

    public static void setGoogleCalendarDisabled() throws CommandException {
        setGoogleCalendarStatus("Disabled");
    }

    public static void setGoogleCalendarStatus(String wantedStatus) throws CommandException {
        logger.info("Setting Google Calendar Status to " + wantedStatus);
        File file = new File("./tokens/mode.txt");
        try (Writer writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(wantedStatus);
            logger.info("Google Calendar status changed to " + wantedStatus
                    + " recoded in ./tokens/enabled.txt");
        } catch (IOException e) {
            logger.warning(MESSAGE_READ_WRITE_ERROR);
            throw new CommandException(MESSAGE_READ_WRITE_ERROR);
        }
    }

    /**
     * Checks the status of the Google Calender Feature
     *
     * @param expectedStatus the expected status
     *
     * @return true if the expected status is true
     */
    public static boolean checkStatus(String expectedStatus) throws CommandException {
        File file = new File("./tokens/mode.txt");
        StringBuilder contents = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String text;
            String lineSeparator = System.getProperty("line.separator");
            while ((text = reader.readLine()) != null) {
                contents.append(text).append(lineSeparator);
            }
        } catch (IOException e) {
            logger.info(MESSAGE_READ_WRITE_ERROR);
            throw new CommandException(MESSAGE_READ_WRITE_ERROR);
        }
        return (contents.toString().trim().equals(expectedStatus));
    }


    public Calendar getCalendar() throws CommandException {
        NetHttpTransport httpTransport = getNetHttpTransport();

        // Build Google calender calendar object.
        Calendar calendar = null;
        try {
            logger.info("Getting a calendar");
            calendar = new Calendar.Builder(httpTransport, JSON_FACTORY,
                    getCredentials(httpTransport))
                    .setApplicationName("iScheduler Xs Max")
                    .build();
        } catch (IOException e) {
            logger.info(MESSAGE_IO_ERROR);
            throw new CommandException(MESSAGE_IO_ERROR);
        }
        return calendar;
    }

    public NetHttpTransport getNetHttpTransport() throws CommandException {
        // Build a new authorized API client calendar.
        NetHttpTransport httpTransport = null;
        try {
            logger.info("Getting a NetHeepTransport object");
            httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        } catch (GeneralSecurityException | IOException e) {
            logger.info(MESSAGE_IO_ERROR);
            throw new CommandException(MESSAGE_IO_ERROR);
        }
        return httpTransport;
    }

    /**
     * Creates an authorized Credential object.
     *
     * @param httpTransport The network HTTP Transport.
     *
     * @return An authorized Credential object.
     *
     * @throws IOException If the credentials.json file cannot be found.
     */
    public static Credential getCredentials(final NetHttpTransport httpTransport) throws IOException {
        logger.info("Getting a Credential object for Google Calendar");
        GoogleClientSecrets clientSecrets = getGoogleClientSecrets();

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new File("tokens")))
                .setAccessType("offline")
                .build();
        return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
    }

    public static GoogleClientSecrets getGoogleClientSecrets() throws IOException {
        logger.info("Load client secrets");
        InputStream in = EnterGoogleCalendarModeCommand.class
                .getResourceAsStream(CREDENTIALS_FILE_PATH);
        return GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
    }

    /**
     * To check whether internet is available.
     *
     * @param address the URL provided
     *
     * @return true if available, false otherwise.
     */
    public static boolean netIsAvailable(String address) {
        try {
            final URL url = new URL(address);
            final URLConnection conn = url.openConnection();
            conn.connect();
            conn.getInputStream()
                    .close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Pushes the event(s) to Google Calendar.
     */
    public boolean pushToGoogleCal(boolean enabled, List<Event> toAddList) throws CommandException {
        if (statusIsDisabled(enabled)) {
            logger.warning(MESSAGE_NOT_ENABLED);
            return false;
        }

        logger.info("Starting to push events Google Calendar");
        Calendar calendar = getCalendar();

        com.google.api.services.calendar.model.Event gEvent = new com.google.api.services.calendar.model.Event();
        for (Event toAddEvent : toAddList) {
            //Set common attribute too the Events
            gEvent = setCommonAttributes(gEvent, toAddEvent);

            if (!toAddEvent.isRepeatEvent()) {
                //if it is not a repeat event, we set Google EventId = local EventUid
                gEvent = gEvent.setId(toAddEvent.getEventUid()
                        .toString()
                        .replaceAll("-", ""));
            } else {
                //For a repeated event, we set repeat attribute in addition to common attributes
                gEvent = setRepeatAttribute(gEvent, toAddEvent);
            }

            try {
                calendar.events().insert(CALENDAR_NAME, gEvent).execute();
            } catch (IOException e) {
                e.printStackTrace();
                logger.warning("Pushing to Google Calendar failed");
                return false;
            }
        }
        logger.info("Pushing to Google Calendar is successful");
        return true;
    }

    private com.google.api.services.calendar.model.Event setRepeatAttribute(
            com.google.api.services.calendar.model.Event googleEvent, Event toAddEvent) {
        logger.info("Setting repeat attribute");
        //Retrieve RecurringEventId from Google Event
        String googleRecurringEventId =
                EventFormatUtil.getEventSetUidInGoogleFormatFromLocalEvent(toAddEvent);
        com.google.api.services.calendar.model.Event eventAfterSettingICalUid =
                googleEvent.setICalUID(googleRecurringEventId);

        //Retrieve the event repeat Type
        String eventRepeatType = String.valueOf(toAddEvent.getRepeatType());
        //Retrieve the eventUntilDateTime
        seedu.scheduler.model.event.DateTime eventUntilDateTime = toAddEvent.getRepeatUntilDateTime();
        String eventUntilDate = adjustLocalEventUntilDateToGoogleFormat(eventUntilDateTime);
        //Construct the command message
        String commandMessage = constructCommandMessage(eventRepeatType, eventUntilDate);
        //Set recurrence property
        eventAfterSettingICalUid.setRecurrence(Collections.singletonList(commandMessage));
        return googleEvent;
    }

    /**
     * Constructs a command message for Google's usage
     *
     * @param eventRepeatType an event RepeatType specified by a String
     * @param eventUntilDate  n event UntilDate specified by a String
     *
     * @return the constructed command message
     */
    private String constructCommandMessage(String eventRepeatType, String eventUntilDate) {
        logger.info("Constructing command message");
        //Google format:20110701T170000Z
        return "RRULE:FREQ="
                + eventRepeatType
                + ";UNTIL="
                + eventUntilDate;
    }

    /**
     * Adjusts a local EventUntilDate To Google Format
     *
     * @param eventUntilDateTime
     *
     * @return
     */
    private String adjustLocalEventUntilDateToGoogleFormat(
            seedu.scheduler.model.event.DateTime eventUntilDateTime) {
        logger.info("Adjusting a local EventUntilDate To Google Format");
        return eventUntilDateTime.getPrettyString()
                //local:2019-01-01 18:51:52
                .replaceAll("-", "")
                //local:20190101 18:51:52
                .replaceFirst(" ", "T")
                //local:20190101T18:51:52
                .replaceAll(":", "")
                //local:20190101T185152
                .concat("Z");
    }

    private com.google.api.services.calendar.model.Event setCommonAttributes(
            com.google.api.services.calendar.model.Event googleEvent, Event toAddEvent) {
        //Set various Event attributes
        logger.info("Setting Event attributes");
        setBasicEventAttribute(googleEvent, toAddEvent);

        //StartDateTime
        logger.info("Setting StartDateTime");
        setStartDateTime(googleEvent, toAddEvent);

        //EndDateTime
        logger.info("Setting EndDateTime");
        setEndDateTime(googleEvent, toAddEvent);

        //Reminders
        logger.info("Setting Reminders");
        setReminders(googleEvent, toAddEvent);
        return googleEvent;
    }

    private void setReminders(com.google.api.services.calendar.model.Event googleEvent, Event toAddEvent) {
        ReminderDurationList reminderDurationList = toAddEvent.getReminderDurationList();
        List<EventReminder> reminderOverrides = new ArrayList<>();
        Set<Duration> reminderMap = reminderDurationList.get();
        Reminders reminder = new Reminders();
        reminder.setUseDefault(false);
        for (Duration key : reminderMap) {
            EventReminder eventReminder = new EventReminder();
            eventReminder.setMethod("popup");
            eventReminder.setMinutes(Math.toIntExact(key.toMinutes()));
            reminderOverrides.add(eventReminder);
        }
        reminder.setOverrides(reminderOverrides);
        googleEvent.setReminders(reminder);
    }

    private void setEndDateTime(com.google.api.services.calendar.model.Event googleEvent, Event toAddEvent) {
        String endDateTime = EventFormatUtil.convertEndDateTimeToGoogleFormat(toAddEvent);
        DateTime end = parseRfc3339(endDateTime);
        googleEvent.setEnd(new EventDateTime().setDateTime(end).setTimeZone("Singapore"));
    }

    private void setStartDateTime(com.google.api.services.calendar.model.Event googleEvent, Event toAddEvent) {
        String startDateTime = EventFormatUtil.convertStartDateTimeToGoogleFormat(toAddEvent);
        DateTime start = parseRfc3339(startDateTime);
        googleEvent.setStart(new EventDateTime().setDateTime(start).setTimeZone("Singapore"));
    }

    private void setBasicEventAttribute(com.google.api.services.calendar.model.Event googleEvent, Event toAddEvent) {
        googleEvent.setSummary(String.valueOf(toAddEvent.getEventName()));
        googleEvent.setLocation(String.valueOf(toAddEvent.getVenue()));
        googleEvent.setDescription(String.valueOf(toAddEvent.getDescription()));
    }

    /**
     * Deletes a range of repeat event instances in the Google Calendar.
     *
     * @param eventToDelete a local Event.
     * @param instanceIndex the instance index for recurring event.
     * @param deleteSingle  true if it is intended to deelete a single event
     * @param deleteAll     true if it is intended to delete all instances
     */
    public boolean deleteEventOnGoogleCal(
            boolean enabled, Event eventToDelete, int instanceIndex,
            boolean deleteSingle, boolean deleteAll)
            throws CommandException {
        //Check status
        if (statusIsDisabled(enabled)) {
            logger.warning(MESSAGE_NOT_ENABLED);
            return false;
        }
        Calendar calendar = getCalendar();
        //Case1: delete non-repeat event: find EventId and delete
        logger.info("Deleting a non-repeat Event");
        List<String> eventIds = new ArrayList<>();
        Boolean result = deleteSingleNonRepeatEvent(eventToDelete, calendar, eventIds);
        //This return structure is to facilitate the deleting of other types of event
        if (result != null) {
            return result;
        }
        //Case2: delete repeated events: find the ICalUid and delete
        result = deleteRepeatEvents(eventToDelete, instanceIndex, deleteSingle, deleteAll, calendar, eventIds);
        if (result != null) {
            return result;
        }
        return true;

    }

    /**
     * Delete the repeated events
     *
     * @param eventToDelete the event to be deleted
     * @param instanceIndex the instance index
     * @param deleteSingle  true if delete single event
     * @param deleteAll     true if delete all event instances
     * @param calendar      the calendar service object
     * @param eventIds      a list of EventIds to identify events
     *
     * @return true if successful
     */
    private Boolean deleteRepeatEvents(Event eventToDelete, int instanceIndex, boolean deleteSingle, boolean deleteAll,
                                       Calendar calendar, List<String> eventIds) {
        String recurringEventId = null;
        boolean repeatedEventsFound = false;
        if (eventToDelete.isRepeatEvent()) {
            //Find iCalUid from Google Event
            FindIcalUid findIcalUid =
                    new FindIcalUid(eventToDelete, calendar, eventIds, recurringEventId, repeatedEventsFound).invoke();
            if (!findIcalUid.getResult()) {
                return false;
            }
            recurringEventId = findIcalUid.getRecurringEventId();
            repeatedEventsFound = findIcalUid.isRepeatedEventsFound();
        }

        //No such event online
        if (eventToDelete.isRepeatEvent() && !repeatedEventsFound) {
            logger.info(MESSAGE_IO_ERROR);
            return false;
        }
        Boolean result =
                deleteInstancesByCase(instanceIndex, deleteSingle, deleteAll,
                        calendar, eventIds, recurringEventId);
        if (result != null) {
            return result;
        }
        return null;
    }

    /**
     * Delete the event instances by case
     *
     * @param instanceIndex    the instance index
     * @param deleteSingle     true if delete single event
     * @param deleteAll        true if delete all event instances
     * @param calendar         the calendar service object
     * @param eventIds         a list of EventIds to identify events
     * @param recurringEventId the recurring Event Id
     *
     * @return true if successful
     */
    private Boolean deleteInstancesByCase(int instanceIndex, boolean deleteSingle, boolean deleteAll, Calendar calendar,
                                          List<String> eventIds, String recurringEventId) {
        //EventSet Found
        try {
            assert recurringEventId != null;
            //Case 2.1: delete single instance
            if (deleteSingle) {
                logger.info("Deleting a single instance of a Repeat Event");
                return deleteSingleInstance(instanceIndex, calendar, recurringEventId);
            } else if (deleteAll | instanceIndex == 0) {
                //delete multiple instances
                //Case2.2: Delete All
                logger.info("Deleting all instances of a Repeat Event");
                deleteAllInstances(calendar, eventIds);
            } else {
                //Case2.3:delete upcoming (and this is not the first instance)
                logger.info("Deleting On and upcoming instances of a Repeat Event");
                deleteUpcomingInstances(instanceIndex, calendar, recurringEventId);
            }
        } catch (IOException e) {
            logger.info(MESSAGE_IO_ERROR);
            return false;
        }
        return null;
    }

    /**
     * Deletes 'on and after' repeat event instances in the Google Calendar
     *
     * @param instanceIndex    the instance index for recurring event
     * @param calendar         the calender calendar object
     * @param recurringEventId recurringEventId of the Google Event
     */
    private void deleteUpcomingInstances(int instanceIndex, Calendar calendar, String recurringEventId)
            throws IOException {
        Events instances = calendar.events()
                .instances(CALENDAR_NAME, recurringEventId).execute();
        assert instances != null;
        for (int i = instanceIndex; i < instances.getItems().size(); i++) {
            com.google.api.services.calendar.model.Event instance =
                    instances.getItems().get(i);
            instance.setStatus("cancelled");
            calendar.events()
                    .update(CALENDAR_NAME, instance.getId(), instance).execute();
        }
    }

    /**
     * Deletes all repeat event instances in the Google Calendar
     *
     * @param calendar the calender calendar object
     * @param eventIds the list of events to be deleted
     */
    private void deleteAllInstances(Calendar calendar, List<String> eventIds) throws IOException {
        for (String eventId : eventIds) {
            calendar.events().delete(CALENDAR_NAME, eventId).execute();
        }
    }

    /**
     * Deletes a single repeat event instance in the Google Calendar
     *
     * @param instanceIndex    the instance index for recurring event
     * @param calendar         the calender calendar object
     * @param recurringEventId recurringEventId of the Google Event
     */
    private boolean deleteSingleInstance(int instanceIndex, Calendar calendar, String recurringEventId)
            throws IOException {
        Events instances = calendar.events().instances(CALENDAR_NAME, recurringEventId).execute();
        com.google.api.services.calendar.model.Event instance = instances.getItems().get(instanceIndex);
        //"cancelled" is a status designed by Google
        instance.setStatus("cancelled");
        calendar.events().update(CALENDAR_NAME, instance.getId(), instance).execute();
        return true;
    }

    /**
     * Deletes a single non-repeat event instance in the Google Calendar
     *
     * @param eventToDelete the Event to be deleted
     * @param calendar      the calender calendar object
     * @param eventIds      the list of events to be deleted
     */
    private Boolean deleteSingleNonRepeatEvent(Event eventToDelete, Calendar calendar, List<String> eventIds) {
        //Get the identifier: the EventUid
        if (!eventToDelete.isRepeatEvent()) {
            eventIds.add(eventToDelete.getEventUid()
                    .toString()
                    .replaceAll("-", ""));
            //delete on Google
            try {
                calendar.events().delete(CALENDAR_NAME, eventIds.get(0)).execute();
            } catch (IOException e) {
                logger.info(MESSAGE_IO_ERROR);
                return false;
            }
        }

        //This return structure is to facilitate the deleting of other types of event
        if (!eventToDelete.isRepeatEvent()) {
            return true;
        }
        return null;
    }

    /**
     * Deletes all events in the Google Calendar.
     *
     * @param enabled the enable status from caller
     */
    public boolean clear(boolean enabled) throws CommandException {
        if (statusIsDisabled(enabled)) {
            logger.warning(MESSAGE_NOT_ENABLED);
            return false;
        }
        Calendar calendar = getCalendar();
        try {
            calendar.calendars().clear(CALENDAR_NAME).execute();
        } catch (IOException e) {
            logger.info(MESSAGE_IO_ERROR);
            return false;
        }
        return true;
    }

    /**
     * Edits the details of an existing event in the Google Calendar.
     *
     * @param enabled       status of Google Calendar feasture
     * @param eventToEdit   a local Event.
     * @param editedEvent   an edited local Event.
     * @param instanceIndex the instance index of the target Event
     *
     * @return command result, true if successful
     */
    public boolean updateSingleGoogleEvent(boolean enabled,
                                           Event eventToEdit, Event editedEvent, int instanceIndex)
            throws CommandException {
        if (statusIsDisabled(enabled)) {
            logger.warning(MESSAGE_NOT_ENABLED);
            return false;
        }
        assert eventToEdit != null;
        assert editedEvent != null;
        Calendar calendar = getCalendar();
        boolean operationOnGoogleCalIsSuccessful;

        //Case1: The event is an instance of repeated event
        //get the EventUid
        if (eventToEdit.isRepeatEvent()) {
            logger.info("Updating a single instance of repeated event");
            operationOnGoogleCalIsSuccessful =
                    updateSingleRepeatInstance(eventToEdit, editedEvent, instanceIndex, calendar);
        } else {
            //Case2: The event is not an instance of Repeated Event
            logger.info("Updating a single event");
            operationOnGoogleCalIsSuccessful =
                    updateSingleNonRepeatEvent(eventToEdit, editedEvent, calendar);
        }
        return operationOnGoogleCalIsSuccessful;
    }

    /**
     * Edits the details of an existing instance of a eventSet in the Google Calendar.
     *
     * @param eventToEdit   a local Event.
     * @param editedEvent   an edited local Event.
     * @param instanceIndex the index of the eventToEdit in the eventSet
     * @param calendar      the Google calendar object
     *
     * @return command result, true if successful
     */
    private boolean updateSingleRepeatInstance(Event eventToEdit, Event editedEvent,
                                               int instanceIndex, Calendar calendar) {
        String recurringEventId = getRecurringEventId(eventToEdit, calendar);
        if (recurringEventId == null) {
            logger.info(MESSAGE_NOT_FOUND);
            return false;
        }
        //Update the repeated event
        Events instances = null;
        try {
            instances = calendar.events()
                    .instances(CALENDAR_NAME, recurringEventId)
                    .execute();

            //Sort the instances according to DateTime
            List<com.google.api.services.calendar.model.Event> instanceSort = instances.getItems();
            instanceSort.sort(Comparator.comparing(a -> a.getStart().getDateTime().toString()));

            //Get the wanted instance, edit and update
            com.google.api.services.calendar.model.Event instance = instanceSort.get(instanceIndex);
            instance = setCommonAttributes(instance, editedEvent);
            instance = setRepeatAttribute(instance, editedEvent);
            calendar.events().update(CALENDAR_NAME, instance.getId(), instance).execute();
        } catch (IOException e) {
            logger.info(MESSAGE_IO_ERROR);
            return false;
        }
        return true;
    }


    /**
     * Edits the details of an existing event in the Google Calendar.
     *
     * @param eventToEdit a local Event.
     * @param editedEvent an edited local Event.
     * @param calendar    google calendar calendar
     *
     * @return command result, true if successful
     */
    private boolean updateSingleNonRepeatEvent(Event eventToEdit, Event editedEvent, Calendar calendar) {
        String gEventId;
        com.google.api.services.calendar.model.Event gEvent;
        com.google.api.services.calendar.model.Event updatedgEvent;
        gEventId = EventFormatUtil.getEventUidInGoogleFormatFromLocalEvent(eventToEdit);
        //retrieve event using event Uid
        Events events = getSingleEvents(calendar);
        if (events == null) {
            logger.warning(MESSAGE_IO_ERROR);
            return false;
        }
        boolean found = false;
        for (com.google.api.services.calendar.model.Event tempGEvent : events.getItems()) {
            if (Objects.equals(tempGEvent.getId(), gEventId)) {
                found = true;
                break;
            }
        }

        //Next action:Edit if found, return error if not found
        if (!found) {
            logger.warning(MESSAGE_NOT_FOUND);
            return false;
        } else {
            try {
                gEvent = calendar.events().get(CALENDAR_NAME, gEventId).execute();
                assert gEvent != null;
                gEvent = setCommonAttributes(gEvent, editedEvent);
                updatedgEvent = calendar.events().update(CALENDAR_NAME, gEventId, gEvent).execute();
                assert updatedgEvent != null;
                updatedgEvent.getUpdated();
            } catch (IOException e) {
                logger.info(MESSAGE_IO_ERROR);
                return false;
            }

        }
        return true;
    }

    /**
     * Edits the details of all existing repeated events in the Google Calendar.
     *
     * @param enabled         status of Google Calender Service
     * @param eventToEdit     a local Event.
     * @param editedEvents    a list of edited local Event.
     * @param instanceIndex   the instance index of target event
     * @param rangeStartIndex the effect of update will start from this index
     *
     * @return command result, true if successful
     */
    public boolean updateRangeGoogleEvent(boolean enabled, Event eventToEdit, List<Event> editedEvents,
                                          int instanceIndex, int rangeStartIndex) throws CommandException {
        if (statusIsDisabled(enabled)) {
            logger.warning(MESSAGE_NOT_ENABLED);
            return false;
        }
        assert !editedEvents.isEmpty();
        assert editedEvents.get(0).isRepeatEvent();
        assert eventToEdit != null;
        Calendar calendar = getCalendar();
        String recurringEventId = getRecurringEventId(eventToEdit, calendar);
        if (recurringEventId == null) {
            logger.info(MESSAGE_NOT_FOUND);
            return false;
        }
        Events instances;
        if (instanceIndex > 0) {
            if (deleteIndividually(editedEvents, rangeStartIndex, calendar, recurringEventId)) {
                return true;
            }
        } else if (instanceIndex == 0) {
            //If the Event is the first instance of the EventSet
            //Google recommends to change the EventSet as a whole
            if (deleteAsSet(editedEvents, calendar, recurringEventId)) {
                return true;
            }
        } else {
            return false;
        }
        return true;
    }

    /**
     * Deletes the events as a set
     *
     * @param editedEvents     a list of edited events
     * @param calendar         the calendar object
     * @param recurringEventId the recurring Event Id
     *
     * @return true if successful
     */
    private boolean deleteAsSet(List<Event> editedEvents, Calendar calendar, String recurringEventId) {
        try {
            editAllEventInstances(editedEvents, calendar, recurringEventId);
        } catch (IOException e) {
            logger.info(MESSAGE_IO_ERROR);
            return false;
        }
        return true;
    }

    /**
     * Deletes the events one by one
     *
     * @param editedEvents     a list of edited events
     * @param rangeStartIndex  a index to start deleting
     * @param calendar         the calendar object
     * @param recurringEventId the recurring Event Id
     *
     * @return true if successful
     */
    private boolean deleteIndividually(List<Event> editedEvents, int rangeStartIndex, Calendar calendar,
                                       String recurringEventId) {
        Events instances;
        try {
            instances = calendar.events()
                    .instances(CALENDAR_NAME, recurringEventId)
                    .execute();
            editNotAllEventInstances(editedEvents, rangeStartIndex, calendar, instances);
        } catch (IOException e) {
            logger.info(MESSAGE_IO_ERROR);
            return false;
        }
        return true;
    }

    /**
     * Edits all event instances in a EventSet
     *
     * @param editedEvents     a list of edited Events
     * @param calendar         calendar calendar object
     * @param recurringEventId the identifier, a recurringEvengtId
     *
     * @throws IOException if any
     */
    private void editAllEventInstances(List<Event> editedEvents, Calendar calendar, String recurringEventId)
            throws IOException {
        int firstInstance = 0;
        com.google.api.services.calendar.model.Event googleEvent = null;
        googleEvent = calendar.events()
                .get(CALENDAR_NAME,
                        Objects.requireNonNull(recurringEventId)).execute();
        googleEvent = setCommonAttributes(googleEvent, editedEvents.get(firstInstance));
        googleEvent = setRepeatAttribute(googleEvent, editedEvents.get(firstInstance));
        calendar.events()
                .update(CALENDAR_NAME, recurringEventId, googleEvent).execute();
    }

    /**
     * Edits a range of instances in a EventSet
     *
     * @param editedEvents    a list of edited Events
     * @param rangeStartIndex the starting index of target event
     * @param calendar        calendar calendar object
     * @param instances       the instances to be edited
     *
     * @throws IOException if any
     */
    private void editNotAllEventInstances(List<Event> editedEvents, int rangeStartIndex, Calendar calendar,
                                          Events instances) throws IOException {
        //sort by start time
        List<com.google.api.services.calendar.model.Event> instanceSort = instances.getItems();
        instanceSort.sort(Comparator.comparing(a -> a.getStart().getDateTime().toString()));

        //For events no need to edit, leave them as what they are
        editEventsBeforeRangeStartIndex(rangeStartIndex, calendar, instanceSort);
        //edit target events
        int editedEventIndex = 0;
        editEventOnAndAfterRangeStartIndex(editedEvents, rangeStartIndex, calendar, instanceSort,
                editedEventIndex);
    }

    /**
     * Edits the Event after the specified index
     *
     * @param editedEvents     a list of edited Events
     * @param rangeStartIndex  the starting index of target event
     * @param calendar         calendar calendar object
     * @param instanceSort     the sorted Event instances
     * @param editedEventIndex the index of edited event
     *
     * @throws IOException if any
     */
    private void editEventOnAndAfterRangeStartIndex(List<Event> editedEvents, int rangeStartIndex, Calendar calendar,
                                                    List<com.google.api.services.calendar.model.Event> instanceSort,
                                                    int editedEventIndex) throws IOException {
        for (int i = rangeStartIndex; i < instanceSort.size(); i++) {
            com.google.api.services.calendar.model.Event instance = instanceSort.get(i);
            instance = setCommonAttributes(instance, editedEvents.get(editedEventIndex));
            instance = setRepeatAttribute(instance, editedEvents.get(editedEventIndex));
            calendar.events().update(CALENDAR_NAME, instance.getId(), instance).execute();
            //Note that this is not a re-assignment, but a counter, which has to be re-signed
            editedEventIndex++;
        }
    }

    /**
     * Edits the Event before the specified index
     *
     * @param rangeEndIndex The specified nd index of target event
     * @param calendar      calendar calendar object
     * @param instanceSort  the sorted Event instances
     *
     * @throws IOException if any
     */
    private void editEventsBeforeRangeStartIndex(int rangeEndIndex, Calendar calendar,
                                                 List<com.google.api.services.calendar.model.Event> instanceSort)
            throws IOException {
        for (int i = 0; i < rangeEndIndex; i++) {
            com.google.api.services.calendar.model.Event instance = instanceSort.get(i);
            calendar.events().update(CALENDAR_NAME, instance.getId(), instance).execute();
        }
    }

    /**
     * Checks whether in offline mode
     *
     * @param enabled status carried forward from default
     *
     * @return true if it is disabled
     */
    private boolean statusIsDisabled(boolean enabled) throws CommandException {
        //Status passed to method: Not enabled -> true (statusIsDisabled)
        //Do another check on real time -> disabled -> true (statusIsDisabled)
        if (!enabled | isGoogleCalendarDisabled()) {
            return true;
        } else {
            return false;
        }
    }


    /**
     * Retrieves the recurring Event Id of a Google Event
     *
     * @param targetEvent the target Event
     * @param calendar    the Google calender calendar object
     *
     * @return the Recurring EventId
     */
    private String getRecurringEventId(Event targetEvent, Calendar calendar) {
        String recurringEventId = null;
        Events allEventsOnGoogle = null;
        String eventSetUid =
                EventFormatUtil.getEventSetUidInGoogleFormatFromLocalEvent(targetEvent);
        logger.info("Trying to download Google Events from Calendar Service.");
        allEventsOnGoogle = getSingleEvents(calendar);

        logger.info("Trying to retrieve GoogleEvents from Events.");
        if (allEventsOnGoogle == null) {
            return null;
        }
        for (com.google.api.services.calendar.model.Event event : allEventsOnGoogle.getItems()) {
            if (Objects.equals(event.getICalUID(), eventSetUid)) {
                recurringEventId = event.getRecurringEventId();
            }
        }

        if (recurringEventId == null) {
            logger.warning("RecurringEventId is not found."
                    + "No such repeated event on Google Calendar.");
            return null;
        }
        return recurringEventId;
    }


    /**
     * Retrieve Google Events The result will exclude the instances of repeat Event, only the underlying repeat event
     * itself is returned
     *
     * @param calendar the Google Calender Service
     *
     * @return true if it is disabled
     */
    public Events getEvents(Calendar calendar) {
        //max 2500 by Google
        //default value is 250 if not specified
        int numberOfEventsToBeDownloaded = 2000;

        Events events;
        try {
            events = calendar.events().list(CALENDAR_NAME)//set the source calendar on google
                    .setMaxResults(numberOfEventsToBeDownloaded) //set upper limit for number of events
                    .execute();
        } catch (IOException e3) {
            logger.info(MESSAGE_IO_ERROR);
            return null;
        }
        return events;
    }

    /**
     * Retrieve Google Events The result will include the instances of repeat Even But the underlying repeat event
     * itself is NOT returned
     *
     * @param calendar the Google Calender Service
     *
     * @return true if it is disabled
     */
    public Events getSingleEvents(Calendar calendar) {
        //max 2500 by Google
        //default value is 250 if not specified
        int numberOfEventsToBeDownloaded = 2000;

        Events events;
        try {
            events = calendar.events().list(CALENDAR_NAME)//set the source calendar on google
                    .setMaxResults(numberOfEventsToBeDownloaded) //set upper limit for number of events
                    .setSingleEvents(true)//not the underlying repeated event, but its instances
                    .execute();
        } catch (IOException e) {
            logger.warning(MESSAGE_IO_ERROR);
            return null;
        }
        return events;
    }

    /**
     * A internal class to find the IcalUid of a Google Event
     */
    private class FindIcalUid {
        private boolean myResult;
        private Event eventToDelete;
        private Calendar calendar;
        private List<String> eventIds;
        private String recurringEventId;
        private boolean repeatedEventsFound;

        public FindIcalUid(Event eventToDelete, Calendar calendar, List<String> eventIds, String recurringEventId,
                           boolean repeatedEventsFound) {
            this.eventToDelete = eventToDelete;
            this.calendar = calendar;
            this.eventIds = eventIds;
            this.recurringEventId = recurringEventId;
            this.repeatedEventsFound = repeatedEventsFound;
        }

        public boolean getResult() {
            return myResult;
        }

        public String getRecurringEventId() {
            return recurringEventId;
        }

        public boolean isRepeatedEventsFound() {
            return repeatedEventsFound;
        }

        /**
         * Retireves the iCalUid
         */
        public FindIcalUid invoke() {
            Events allEventsOnGoogle;
            String eventSetUid =
                    EventFormatUtil.getEventSetUidInGoogleFormatFromLocalEvent(eventToDelete);
            try {
                allEventsOnGoogle = getSingleEvents(calendar);
                for (com.google.api.services.calendar.model.Event event : allEventsOnGoogle.getItems()) {
                    if (Objects.equals(event.getICalUID(), eventSetUid)) {
                        eventIds.add(event.getId());
                        repeatedEventsFound = true;
                        recurringEventId = event.getRecurringEventId();
                    }
                }
            } catch (NullPointerException e) {
                logger.info(MESSAGE_IO_ERROR);
                myResult = false;
                return this;
            }
            myResult = true;
            return this;
        }
    }
}
