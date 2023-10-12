package brightspot.reviewcycle.notification;

import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import brightspot.reviewcycle.CalendarField;
import brightspot.reviewcycle.ReviewCycleContentModification;
import brightspot.reviewcycle.ReviewCycleDuration;
import com.psddev.dari.db.CompoundPredicate;
import com.psddev.dari.db.Predicate;
import com.psddev.dari.db.PredicateParser;
import com.psddev.dari.db.Record;

public class ReviewCycleDueWarningDuration extends Record implements ReviewCycleDuration {

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

    @Override
    public int getCalendarFieldCount() {
        return calendarFieldCount;
    }

    public Date subtractCycleDuration(Date now, ReviewCycleDueWarningDuration durationValues) {
        if (now == null) {
            now = new Date();
        }
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(now);
        calendar.add(durationValues.getCalendarField().getCalendarField(), -1 * durationValues.getCalendarFieldCount());
        return calendar.getTime();
    }

    public static Date addCycleDuration(Date now, ReviewCycleDueWarningDuration durationValues) {
        if (now == null) {
            now = new Date();
        }
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(now);
        calendar.add(durationValues.getCalendarField().getCalendarField(), durationValues.getCalendarFieldCount());
        return calendar.getTime();
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
