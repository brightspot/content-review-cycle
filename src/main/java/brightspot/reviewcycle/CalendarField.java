package brightspot.reviewcycle;

import java.util.Calendar;

/**
 * Enum used in Due Warning Duration and Duration For Content for distinction between days monthhs and years
 */
public enum CalendarField {

    DAYS("day(s)", Calendar.DATE),
    WEEKS("week(s)", Calendar.WEEK_OF_MONTH),
    MONTHS("month(s)", Calendar.MONTH);

    private final String weeks;
    private final int calendarField;

    CalendarField(String weeks, int calendarField) {
        this.weeks = weeks;
        this.calendarField = calendarField;
    }

    public String getWeeks() {
        return weeks;
    }

    public int getCalendarField() {
        return calendarField;
    }

    @Override
    public String toString() {
        return getWeeks();
    }
}
