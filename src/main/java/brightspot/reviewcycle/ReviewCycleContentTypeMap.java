package brightspot.reviewcycle;

import java.util.Date;

import brightspot.reviewcycle.widget.ReviewActivityWidget;
import com.psddev.cms.ui.ToolLocalization;
import com.psddev.cms.ui.ToolRequest;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Predicate;
import com.psddev.dari.db.PredicateParser;
import com.psddev.dari.db.Record;
import com.psddev.dari.db.Recordable;
import com.psddev.dari.web.WebRequest;

/**
 * The ReviewCycleContentMap is associated with a specific content type and its cycle duration. Each
 * ReviewCycleContentTypeMap will contain a @see {@link #getCycleDuration()} that is either filled in or inherited
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

    private ReviewCycleDurationForContent cycleDuration;

    @Override
    public void beforeSave() {
        if (getCycleDuration() == null) {
            setCycleDuration(getDefaultCycleDurationFallback());
        }
    }

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

    private ReviewCycleDurationForContent getDefaultCycleDurationFallback() {
        return WebRequest.getCurrent().as(ToolRequest.class).getCurrentSite().as(ReviewCycleSiteSettings.class).getReviewCycleDurationForContent();
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

        String defaultText = this.getContentType().getDisplayName() + " - " + "Every " + calenderFieldCount + " "
                + timePeriod;

        return ToolLocalization.text(ReviewActivityWidget.class, "label.duration", defaultText);
    }

    // Using "next date"
    public Predicate getExpiredPredicate(Date now) {
        return PredicateParser.Static.parse(
            ReviewCycleContentModification.NEXT_REVIEW_DATE_INDEX_FIELD_INTERNAL_NAME
                + " != missing and "
                + ReviewCycleContentModification.NEXT_REVIEW_DATE_INDEX_FIELD_INTERNAL_NAME
                + " < ?", now.getTime());
    }

    public Predicate getTypePredicate() {
        return PredicateParser.Static.parse("_type = ?", this.getContentType());
    }
}
