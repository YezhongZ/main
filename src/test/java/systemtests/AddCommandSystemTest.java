package systemtests;

import static seedu.address.commons.core.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.address.logic.commands.CommandTestUtil.DESCRIPTION_DESC_MA3220;
import static seedu.address.logic.commands.CommandTestUtil.END_DATETIME_DESC_MA3220;
import static seedu.address.logic.commands.CommandTestUtil.EVENT_NAME_DESC_MA3220;
import static seedu.address.logic.commands.CommandTestUtil.INVALID_EVENT_NAME_DESC;
import static seedu.address.logic.commands.CommandTestUtil.REPEAT_TYPE_DESC_MA3220;
import static seedu.address.logic.commands.CommandTestUtil.REPEAT_UNTIL_DATETIME_DESC_MA3220;
import static seedu.address.logic.commands.CommandTestUtil.START_DATETIME_DESC_MA3220;
import static seedu.address.logic.commands.CommandTestUtil.TAG_DESC_PLAY;
import static seedu.address.logic.commands.CommandTestUtil.VENUE_DESC_MA3220;
import static seedu.address.testutil.TypicalEvents.KEYWORD_MATCHING_JANUARY;
import static seedu.address.testutil.TypicalEvents.MA3220_JANUARY_1_2019_SINGLE;
import static seedu.address.testutil.TypicalEvents.TRAVEL_JUNE_1_2018_SINGLE;
import static seedu.address.testutil.TypicalEvents.WORK_DECEMBER_12_2018_SINGLE;

import java.util.List;

import org.junit.Test;

import seedu.address.commons.core.Messages;
import seedu.address.commons.core.index.Index;
import seedu.address.logic.commands.AddCommand;
import seedu.address.logic.commands.RedoCommand;
import seedu.address.logic.commands.UndoCommand;
import seedu.address.model.Model;
import seedu.address.model.event.Event;
import seedu.address.model.event.EventName;
import seedu.address.testutil.EventBuilder;
import seedu.address.testutil.EventUtil;

public class AddCommandSystemTest extends SchedulerSystemTest {

    @Test
    public void add() {
        Model model = getModel();
        /* ------------------------ Perform add operations on the shown unfiltered list ----------------------------- */

        /* Case: add an event with tags to a non-empty scheduler, command with leading spaces and trailing spaces
         * -> added
         */
        Event toAdd = MA3220_JANUARY_1_2019_SINGLE;
        String command = "   " + AddCommand.COMMAND_WORD + "  " + EVENT_NAME_DESC_MA3220 + "  "
                + START_DATETIME_DESC_MA3220 + "   " + END_DATETIME_DESC_MA3220 + "   " + DESCRIPTION_DESC_MA3220
                + "   " + VENUE_DESC_MA3220 + "   " + REPEAT_TYPE_DESC_MA3220 + "   "
                + REPEAT_UNTIL_DATETIME_DESC_MA3220 + " " + TAG_DESC_PLAY;
        assertCommandSuccess(command, toAdd);

        /* Case: undo adding MA2101 to the list -> MA2101 deleted */
        command = UndoCommand.COMMAND_WORD;
        String expectedResultMessage = UndoCommand.MESSAGE_SUCCESS;
        assertCommandSuccess(command, model, expectedResultMessage);

        /* Case: redo adding MA2101 to the list -> MA2101 added again */
        command = RedoCommand.COMMAND_WORD;
        model.addEvents(List.of(toAdd));
        expectedResultMessage = RedoCommand.MESSAGE_SUCCESS;
        assertCommandSuccess(command, model, expectedResultMessage);

        /* Case: add an event with all fields same as another event in the scheduler -> added */
        toAdd = new EventBuilder(MA3220_JANUARY_1_2019_SINGLE).build();
        command = AddCommand.COMMAND_WORD + EVENT_NAME_DESC_MA3220 + START_DATETIME_DESC_MA3220
                + END_DATETIME_DESC_MA3220 + DESCRIPTION_DESC_MA3220 + VENUE_DESC_MA3220
                + REPEAT_TYPE_DESC_MA3220 + REPEAT_UNTIL_DATETIME_DESC_MA3220 + TAG_DESC_PLAY;
        assertCommandSuccess(command, toAdd);

        /* Case: add to empty scheduler -> added */
        deleteAllEvents();
        assertCommandSuccess(MA3220_JANUARY_1_2019_SINGLE);

        /* Case: add an event with tags, command with parameters in random order -> added */
        toAdd = MA3220_JANUARY_1_2019_SINGLE;
        command = AddCommand.COMMAND_WORD + REPEAT_UNTIL_DATETIME_DESC_MA3220 + START_DATETIME_DESC_MA3220
                + VENUE_DESC_MA3220 + REPEAT_TYPE_DESC_MA3220 + END_DATETIME_DESC_MA3220
                + DESCRIPTION_DESC_MA3220 + EVENT_NAME_DESC_MA3220 + TAG_DESC_PLAY;
        assertCommandSuccess(command, toAdd);

        /* Case: add an event, missing tags -> added */
        toAdd = new EventBuilder(MA3220_JANUARY_1_2019_SINGLE).withTags().build();
        assertCommandSuccess(toAdd);

        /* -------------------------- Perform add operation on the shown filtered list ------------------------------ */

        /* Case: filters the event list before adding -> added */
        showEventsWithEventName(KEYWORD_MATCHING_JANUARY);
        assertCommandSuccess(TRAVEL_JUNE_1_2018_SINGLE);

        /* ------------------------ Perform add operation while an event card is selected --------------------------- */

        /* Case: selects first card in the event list, add an event -> added, card selection remains unchanged */
        selectEvent(Index.fromOneBased(1));
        assertCommandSuccess(WORK_DECEMBER_12_2018_SINGLE);

        /* ----------------------------------- Perform invalid add operations --------------------------------------- */

        /* Case: missing name -> rejected */
        command = AddCommand.COMMAND_WORD + START_DATETIME_DESC_MA3220 + END_DATETIME_DESC_MA3220
                + DESCRIPTION_DESC_MA3220 + VENUE_DESC_MA3220 + REPEAT_TYPE_DESC_MA3220
                + REPEAT_UNTIL_DATETIME_DESC_MA3220;
        assertCommandFailure(command, String.format(MESSAGE_INVALID_COMMAND_FORMAT, AddCommand.MESSAGE_USAGE));

        /* Case: invalid keyword -> rejected */
        command = "adds " + EventUtil.getEventDetails(toAdd);
        assertCommandFailure(command, Messages.MESSAGE_UNKNOWN_COMMAND);

        /* Case: invalid event name -> rejected */
        command = AddCommand.COMMAND_WORD + INVALID_EVENT_NAME_DESC + START_DATETIME_DESC_MA3220
                + END_DATETIME_DESC_MA3220 + DESCRIPTION_DESC_MA3220
                + VENUE_DESC_MA3220 + REPEAT_TYPE_DESC_MA3220 + REPEAT_UNTIL_DATETIME_DESC_MA3220;
        assertCommandFailure(command, EventName.MESSAGE_EVENT_NAME_CONSTRAINTS);
    }

    /**
     * Executes the {@code AddCommand} that adds {@code toAdd} to the model and asserts that the,<br>
     * 1. Command box displays an empty string.<br>
     * 2. Command box has the default style class.<br>
     * 3. Result display box displays the success message of executing {@code AddCommand} with the details of
     * {@code toAdd}.<br>
     * 4. {@code Storage} and {@code EventListPanel} equal to the corresponding components in
     * the current model added with {@code toAdd}.<br>
     * 5. Browser url and selected card remain unchanged.<br>
     * 6. Status bar's sync status changes.<br>
     * Verifications 1, 3 and 4 are performed by
     * {@code SchedulerSystemTest#assertApplicationDisplaysExpected(String, String, Model)}.<br>
     * @see SchedulerSystemTest#assertApplicationDisplaysExpected(String, String, Model)
     */
    private void assertCommandSuccess(Event toAdd) {
        assertCommandSuccess(EventUtil.getAddCommand(toAdd), toAdd);
    }

    /**
     * Performs the same verification as {@code assertCommandSuccess(Event)}. Executes {@code command}
     * instead.
     * @see AddCommandSystemTest#assertCommandSuccess(Event)
     */
    private void assertCommandSuccess(String command, Event toAdd) {
        Model expectedModel = getModel();
        expectedModel.addEvents(List.of(toAdd));
        String expectedResultMessage = String.format(AddCommand.MESSAGE_SUCCESS, toAdd);

        assertCommandSuccess(command, expectedModel, expectedResultMessage);
    }

    /**
     * Performs the same verification as {@code assertCommandSuccess(String, Event)} except asserts that
     * the,<br>
     * 1. Result display box displays {@code expectedResultMessage}.<br>
     * 2. {@code Storage} and {@code EventListPanel} equal to the corresponding components in
     * {@code expectedModel}.<br>
     * @see AddCommandSystemTest#assertCommandSuccess(String, Event)
     */
    private void assertCommandSuccess(String command, Model expectedModel, String expectedResultMessage) {
        executeCommand(command);
        assertApplicationDisplaysExpected("", expectedResultMessage, expectedModel);
        assertSelectedCardUnchanged();
        assertCommandBoxShowsDefaultStyle();
        assertStatusBarUnchangedExceptSyncStatus();
    }

    /**
     * Executes {@code command} and asserts that the,<br>
     * 1. Command box displays {@code command}.<br>
     * 2. Command box has the error style class.<br>
     * 3. Result display box displays {@code expectedResultMessage}.<br>
     * 4. {@code Storage} and {@code EventListPanel} remain unchanged.<br>
     * 5. Browser url, selected card and status bar remain unchanged.<br>
     * Verifications 1, 3 and 4 are performed by
     * {@code SchedulerSystemTest#assertApplicationDisplaysExpected(String, String, Model)}.<br>
     * @see SchedulerSystemTest#assertApplicationDisplaysExpected(String, String, Model)
     */
    private void assertCommandFailure(String command, String expectedResultMessage) {
        Model expectedModel = getModel();

        executeCommand(command);
        assertApplicationDisplaysExpected(command, expectedResultMessage, expectedModel);
        assertSelectedCardUnchanged();
        assertCommandBoxShowsErrorStyle();
        assertStatusBarUnchanged();
    }
}
