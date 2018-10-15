package seedu.scheduler.model.event;

import static java.lang.Boolean.FALSE;
import static java.util.Objects.requireNonNull;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a list of duration object for reminder times
 */
public class ReminderDurationList {

    public Map<Duration, Boolean> values = new HashMap<>();


    /**
     * Default constructor for ReminderDurationList
     */
    public ReminderDurationList(){

    }

    /**
     * Constructs a {@code ReminderDurationList} with inputs
     *
     * @param reminderDurations a list of duration objects.
     */
    public ReminderDurationList(Collection<Duration> reminderDurations) {
        requireNonNull(reminderDurations);
        for (Duration duration: reminderDurations) {
            values.put(duration, FALSE);
        }
    }

    public Map<Duration, Boolean> get () {
        return values;
    }

    /**
     *
     * @return the current number of reminder times
     */
    public int size() {
        return values.size();
    }

    /**
     * add input duration object to the last of the list
     * @param duration
     */
    public void add(Duration duration) {
        values.put(duration, FALSE);
    }


    @Override
    public String toString() {
        String output = "";
        if (values.size() == 1){
            output = "NONE";
        }
        else{
            for (Duration duration: values.keySet()) {
                output += formatDuration(duration) + ": " + values.get(duration).toString() + ", " ;
            }
            output = output.substring(0, output.length() - 2);
        }
        return output;
    }

    /**
     * @param duration
     * @return formatted string output for toString method
     */
    private static String formatDuration(Duration duration) {
        long seconds = duration.getSeconds();
        long absSeconds = Math.abs(seconds);
        String positive = String.format(
                "%d:%02d:%02d",
                absSeconds / 3600,
                (absSeconds % 3600) / 60,
                absSeconds % 60);
        return seconds < 0 ? "-" + positive : positive;
    }

    @Override
    public int hashCode() {
        return values.hashCode();
    }



}