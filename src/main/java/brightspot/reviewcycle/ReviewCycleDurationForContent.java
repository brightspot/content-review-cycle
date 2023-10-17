package brightspot.reviewcycle;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;

import com.psddev.cms.ui.ToolLocalization;
import com.psddev.dari.db.Record;

/**
 * This file is associated with the cycle duration for a content type. It utilizes the @see {@link CalendarField} enum
 * which can be a specific amount of days, months, or years.
 */
public class ReviewCycleDurationForContent extends Record {

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

        if (this.getCalendarField().getType() == Calendar.DAY_OF_MONTH) {
            lastDueZoned = lastDueZoned.minusDays(calendarFieldCount);
        } else if (this.getCalendarField().getType() == Calendar.WEEK_OF_MONTH) {
            lastDueZoned = lastDueZoned.minusWeeks(calendarFieldCount);
        } else {
            lastDueZoned = lastDueZoned.minusMonths(calendarFieldCount);
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

        if (this.getCalendarField().getType() == Calendar.DAY_OF_MONTH) {
            lastDueZoned = lastDueZoned.plusDays(calendarFieldCount);
        } else if (this.getCalendarField().getType() == Calendar.WEEK_OF_MONTH) {
            lastDueZoned = lastDueZoned.plusWeeks(calendarFieldCount);
        } else {
            lastDueZoned = lastDueZoned.plusMonths(calendarFieldCount);
        }

        return Date.from(lastDueZoned.toInstant());
    }

    @Override
    public String getLabel() {
        String defaultText = "Every " + getCalendarFieldCount() + " " + getCalendarField().toString();
        return ToolLocalization.text(ReviewCycleDurationForContent.class, "label.duration", defaultText);
    }
}
