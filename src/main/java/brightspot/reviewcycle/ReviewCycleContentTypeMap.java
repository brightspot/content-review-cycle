package brightspot.reviewcycle;

import java.util.Date;

import com.google.common.collect.ImmutableMap;
import com.psddev.cms.ui.LocalizationContext;
import com.psddev.cms.ui.ToolLocalization;
import com.psddev.cms.ui.form.Note;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Predicate;
import com.psddev.dari.db.PredicateParser;
import com.psddev.dari.db.Record;
import com.psddev.dari.db.Recordable;

/**
 * The ReviewCycleContentMap is associated with a specific content type and its cycle duration. Each
 * ReviewCycleContentTypeMap will contain a {@link #getCycleDuration()} that is either filled in or inherited
 * from the default values.
 */
@Recordable.Embedded
@Recordable.DisplayName("Content Type Map")
public class ReviewCycleContentTypeMap extends Record {

    public ReviewCycleContentTypeMap() {
    }

    public ReviewCycleContentTypeMap(ObjectType contentType, ReviewCycleDurationForContent cycleDuration) {
        this.contentType = contentType;
        this.cycleDuration = cycleDuration;
    }

    @Required
    @Where("groups = " + HasReviewCycle.INTERNAL_NAME + " and isAbstract = false")
    private ObjectType contentType;

    @Required
    @Note("Cycle Duration should be longer than the Banner Warning Duration to prevent content always in review.")
    private ReviewCycleDurationForContent cycleDuration;

    public ObjectType getContentType() {
        return contentType;
    }

    public void setContentType(ObjectType contentType) {
        this.contentType = contentType;
    }

    public ReviewCycleDurationForContent getCycleDuration() {
        return cycleDuration;
    }

    public void setCycleDuration(ReviewCycleDurationForContent cycleDuration) {
        this.cycleDuration = cycleDuration;
    }

    @Override
    public String getLabel() {
        int calendarType = getCycleDuration().getCalendarField().getType();
        String timePeriod;

        if (calendarType == 5) {
            timePeriod = "day(s)";
        } else if (calendarType == 4) {
            timePeriod = "week(s)";
        } else {
            timePeriod = "month(s)";
        }

        int calenderFieldCount = getCycleDuration().getCalendarFieldCount();

        String defaultText = this.getContentType().getDisplayName() + " - " + calenderFieldCount + " "
                + timePeriod;

        return ToolLocalization.text(
                new LocalizationContext(
                        ReviewCycleContentTypeMap.class,
                        ImmutableMap.of("displayName", this.getContentType().getDisplayName(), "calendarFieldCount", calenderFieldCount, "timePeriod", timePeriod)),
                "label.contentTypeMap",
                defaultText);
    }

    // Using "next date"
    public Predicate getExpiredPredicate(Date now) {
        return PredicateParser.Static.parse(
            ReviewCycleContentModification.NEXT_REVIEW_DATE_FIELD_INTERNAL_NAME
                + " != missing and "
                + ReviewCycleContentModification.NEXT_REVIEW_DATE_FIELD_INTERNAL_NAME
                + " < ?", now.getTime());
    }

    public Predicate getTypePredicate() {
        return PredicateParser.Static.parse("_type = ?", this.getContentType());
    }
}
