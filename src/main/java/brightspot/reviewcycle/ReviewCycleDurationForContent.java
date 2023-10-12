package brightspot.reviewcycle;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;

import com.psddev.dari.db.Record;

/**
 * This file is associated with the cycle duration for a content type. It utillizes CalendarField enum which can be a
 * specific amount of days, months, or years.
 */
public class ReviewCycleDurationForContent extends Record implements ReviewCycleDuration {

    private CalendarField calendarField;
    private int calendarFieldCount;

    public ReviewCycleDurationForContent() {
    }

    ReviewCycleDurationForContent(CalendarField calendarField, int calendarFieldCount) {
        this.calendarField = calendarField;
        this.calendarFieldCount = calendarFieldCount;
    }

    public CalendarField getCalendarField() {
        return calendarField;
    }

    @Override
    public int getCalendarFieldCount() {
        return calendarFieldCount;
    }

    public Date subtractCycleDuration(Date now) {
        ZonedDateTime lastDueZoned;

        if (now == null) {
            lastDueZoned = ZonedDateTime.now(ZoneId.of("UTC"));
        } else {
            lastDueZoned = ZonedDateTime.ofInstant(now.toInstant(),
                    ZoneId.of("UTC"));
        }

        if (this.getCalendarField().getCalendarField() == Calendar.DAY_OF_MONTH) {
            lastDueZoned.minusDays(calendarFieldCount);
        } else if (this.getCalendarField().getCalendarField() == Calendar.WEEK_OF_MONTH) {
            lastDueZoned.minusWeeks(calendarFieldCount);
        } else {
            lastDueZoned.minusMonths(calendarFieldCount);
        }

        return Date.from(lastDueZoned.toInstant());
    }

    public Date addCycleDuration(Date lastDue) {
        ZonedDateTime lastDueZoned;

        if (lastDue == null) {
            lastDueZoned = ZonedDateTime.now(ZoneId.of("UTC"));
        } else {
            lastDueZoned = ZonedDateTime.ofInstant(lastDue.toInstant(),
                    ZoneId.of("UTC"));
        }

        if (this.getCalendarField().getCalendarField() == Calendar.DAY_OF_MONTH) {
            lastDueZoned.plusDays(calendarFieldCount);
        } else if (this.getCalendarField().getCalendarField() == Calendar.WEEK_OF_MONTH) {
            lastDueZoned.plusWeeks(calendarFieldCount);
        } else {
            lastDueZoned.plusMonths(calendarFieldCount);
        }

        return Date.from(lastDueZoned.toInstant());
    }

    @Override
    public String getLabel() {
        return "Every " + getCalendarFieldCount() + " " + getCalendarField().toString();
    }
}
