package brightspot.reviewcycle;

import java.util.Calendar;

/**
 * Enum used in @see {@link brightspot.reviewcycle.notification.ReviewCycleDueWarningDuration} and
 * @see {@link brightspot.reviewcycle.ReviewCycleDurationForContent} for distinction between days, months, and years.
 */
public enum CalendarField {

    DAYS("day(s)", Calendar.DATE),
    WEEKS("week(s)", Calendar.WEEK_OF_MONTH),
    MONTHS("month(s)", Calendar.MONTH);

    private final String stringType;
    private final int type;

    CalendarField(String stringType, int type) {
        this.stringType = stringType;
        this.type = type;
    }

    public String getStringType() {
        return stringType;
    }

    public int getType() {
        return type;
    }

    @Override
    public String toString() {
        return getStringType();
    }
}
