package brightspot.reviewcycle.notification;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import brightspot.reviewcycle.CalendarField;
import brightspot.reviewcycle.ReviewCycleContentModification;
import com.psddev.dari.db.CompoundPredicate;
import com.psddev.dari.db.Predicate;
import com.psddev.dari.db.PredicateParser;
import com.psddev.dari.db.Record;

/**
 * This file is associated with the due warning duration for a content type. It utillizes CalendarField enum which can
 * be a specific amount of days, months, or years.
 */
public class ReviewCycleDueWarningDuration extends Record {

    private CalendarField calendarField;
    private int calendarFieldCount;

    public ReviewCycleDueWarningDuration() {
    }

    public ReviewCycleDueWarningDuration(CalendarField calendarField, int calendarFieldCount) {
        this.calendarField = calendarField;
        this.calendarFieldCount = calendarFieldCount;
    }

    public CalendarField getCalendarField() {
        return calendarField;
    }

    public int getCalendarFieldCount() {
        return calendarFieldCount;
    }

    public Date subtractCycleDuration(Date now, ReviewCycleDueWarningDuration durationValues) {
        ZonedDateTime lastDueZoned;

        int calendarField = durationValues.getCalendarField().getCalendarField();
        int calendarFieldCount = durationValues.getCalendarFieldCount();

        if (now == null) {
            lastDueZoned = ZonedDateTime.now(ZoneId.of("UTC"));
        } else {
            lastDueZoned = ZonedDateTime.ofInstant(now.toInstant(),
                    ZoneId.of("UTC"));
        }

        if (calendarField == Calendar.DAY_OF_MONTH) {
            lastDueZoned.minusDays(calendarFieldCount);
        } else if (calendarField == Calendar.WEEK_OF_MONTH) {
            lastDueZoned.minusWeeks(calendarFieldCount);
        } else {
            lastDueZoned.minusMonths(calendarFieldCount);
        }

        return Date.from(lastDueZoned.toInstant());
    }

    public static Date addCycleDuration(Date now, ReviewCycleDueWarningDuration durationValues) {
        ZonedDateTime lastDueZoned;

        int calendarField = durationValues.getCalendarField().getCalendarField();
        int calendarFieldCount = durationValues.getCalendarFieldCount();

        if (now == null) {
            lastDueZoned = ZonedDateTime.now(ZoneId.of("UTC"));
        } else {
            lastDueZoned = ZonedDateTime.ofInstant(now.toInstant(),
                    ZoneId.of("UTC"));
        }

        if (calendarField == Calendar.DAY_OF_MONTH) {
            lastDueZoned.plusDays(calendarFieldCount);
        } else if (calendarField == Calendar.WEEK_OF_MONTH) {
            lastDueZoned.plusWeeks(calendarFieldCount);
        } else {
            lastDueZoned.plusMonths(calendarFieldCount);
        }

        return Date.from(lastDueZoned.toInstant());
    }

    public static Predicate getDueWarningPredicate(Date now, List<ReviewCycleDueWarningDuration> dueWarningDurations) {

        Predicate compound = null;
        Instant currentInstant;
        Predicate currentPredicate;

        for (ReviewCycleDueWarningDuration dueWarningDuration : dueWarningDurations) {
            currentInstant = addCycleDuration(now, dueWarningDuration).toInstant();
            currentPredicate = PredicateParser.Static.parse(
                    ReviewCycleContentModification.NEXT_REVIEW_DATE_INDEX_FIELD_INTERNAL_NAME
                            + " != missing && "
                            + ReviewCycleContentModification.NEXT_REVIEW_DATE_INDEX_FIELD_INTERNAL_NAME
                            + " = ?",
                    Date.from(currentInstant));

            if (compound == null) {
                compound = currentPredicate;
            } else {
                compound = CompoundPredicate.combine(
                        PredicateParser.OR_OPERATOR,
                        compound,
                        currentPredicate);
            }
        }
        return compound;
    }

    // Used for calculating banner warning date range
    public static Predicate getBannerDueWarningPredicate(Date now, ReviewCycleDueWarningDuration dueWarningDuration) {
        Date dueSoon = ReviewCycleDueWarningDuration.addCycleDuration(now, dueWarningDuration);
        return PredicateParser.Static.parse(
                ReviewCycleContentModification.NEXT_REVIEW_DATE_INDEX_FIELD_INTERNAL_NAME + " != missing && "
                        + ReviewCycleContentModification.NEXT_REVIEW_DATE_INDEX_FIELD_INTERNAL_NAME + " <= ? ",
                dueSoon.getTime());
    }

    @Override
    public String getLabel() {
        return "Every " + getCalendarFieldCount() + " " + getCalendarField().toString();
    }
}
