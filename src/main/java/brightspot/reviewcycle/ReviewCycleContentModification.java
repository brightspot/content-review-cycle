package brightspot.reviewcycle;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;

import com.psddev.cms.db.Content;
import com.psddev.cms.db.Draft;
import com.psddev.cms.db.Site;
import com.psddev.cms.db.SiteSettings;
import com.psddev.cms.db.ToolUi;
import com.psddev.cms.db.ToolUi.Cluster;
import com.psddev.cms.db.ToolUi.Tab;
import com.psddev.cms.ui.form.DynamicNoteMethod;
import com.psddev.cms.ui.form.DynamicTypeClass;
import com.psddev.dari.db.Modification;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Record;
import com.psddev.dari.db.State;

/**
 * When a content type inherits the {@link HasReviewCycle} marker interface, this ReviewCycleContentModification
 * will be seen in the overrides tab. You will be able to override the cycle duration (an article with a cycle duration
 * of 1 month will be overriden by whatever is chosen here)
 */
@ToolUi.FieldInternalNamePrefix(ReviewCycleContentModification.FIELD_PREFIX)
@DynamicTypeClass(ReviewCycleGlobalDynamicType.class)
@DynamicTypeClass(ReviewCycleOverrideType.class)
public class ReviewCycleContentModification extends Modification<HasReviewCycle> {

    private static final String REVIEW_CYCLE_CLUSTER = "Review Cycle Overrides";
    private static final String REVIEW_CYCLE_TAB = "Overrides";

    public static final String FIELD_PATH = "brightspot.reviewcycle.ReviewCycleContentModification/";
    public static final String FIELD_PREFIX = "reviewCycleOverrides.";
    public static final String EXTRA_OVERLAID_DRAFT = "cms.tool.overlaidDraft";

    public static final String REVIEW_DATE_FIELD = "reviewDate";
    public static final String REVIEW_DATE_FIELD_INTERNAL_NAME = FIELD_PREFIX + REVIEW_DATE_FIELD;

    public static final String NEXT_REVIEW_DATE_FIELD = "nextReviewDate";
    public static final String NEXT_REVIEW_DATE_FIELD_INTERNAL_NAME = FIELD_PATH + FIELD_PREFIX + NEXT_REVIEW_DATE_FIELD;

    public static final String REVIEW_CYCLE_DURATION_FIELD = "reviewCycleDuration";
    public static final String REVIEW_CYCLE_DURATION_FIELD_INTERNAL_NAME =
        FIELD_PATH + FIELD_PREFIX + REVIEW_CYCLE_DURATION_FIELD;

    private static final DateFormat FORMAT = new SimpleDateFormat("EEE, MMM dd, yyyy");

    @Tab(REVIEW_CYCLE_TAB)
    @Cluster(REVIEW_CYCLE_CLUSTER)
    @InternalName(REVIEW_DATE_FIELD)
    @DisplayName("Last Cycle Due Date")
    @ToolUi.ReadOnly
    private Date reviewDate;

    @Indexed
    @ToolUi.Filterable
    @ToolUi.Sortable
    @InternalName(NEXT_REVIEW_DATE_FIELD)
    @ToolUi.Hidden
    private Date nextReviewDate;

    @Tab(REVIEW_CYCLE_TAB)
    @Cluster(REVIEW_CYCLE_CLUSTER)
    @DisplayName("Review Cycle Duration for This Content Only")
    @DynamicNoteMethod("getNextReviewDateNote")
    @InternalName(REVIEW_CYCLE_DURATION_FIELD)
    @Indexed
    private ReviewCycleDurationForContent reviewCycleDurationForContentOverride;

    public Date getReviewDate() {
        return reviewDate;
    }

    public void setReviewDate(Date reviewDate) {
        this.reviewDate = reviewDate;
    }

    public ReviewCycleDurationForContent getReviewCycleDurationForContentOverride() {
        return reviewCycleDurationForContentOverride;
    }

    public void setReviewCycleDuration(ReviewCycleDurationForContent reviewCycleDurationForContentOverride) {
        this.reviewCycleDurationForContentOverride = reviewCycleDurationForContentOverride;
    }

    public Date getNextReviewDate() {
        Date nextReview = this.calculateNextReviewDate();
        return Optional.ofNullable(nextReview)
            .map(Date::toInstant)
            .map(instant -> instant.truncatedTo(ChronoUnit.DAYS))
            .map(Date::from)
            .orElse(null);
    }

    public void setNextReviewDate(Date nextReviewDate) {
        this.nextReviewDate = nextReviewDate;
    }

    public String getNextReviewDateNote() {
        Date utcDue = getNextReviewDate();
        String nextReviewDate = "N/A";
        if (utcDue != null) {
            FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
            nextReviewDate = FORMAT.format(utcDue);
        }

        return "This will set the cycle for this specific content. The cycle begins on the last cycle due date. The next cycle due date is " + nextReviewDate + ".";
    }

    private Date calculateNextReviewDate() {

        HasReviewCycle hasReviewCycle = ReviewCycleUtils.resolve(getOriginalObject());
        ReviewCycleDurationForContent duration = Optional.ofNullable(hasReviewCycle)
            .map(reviewCycle -> reviewCycle.as(ReviewCycleContentModification.class))
            .map(ReviewCycleContentModification::getReviewCycleMap)
            .map(ReviewCycleContentTypeMap::getCycleDuration)
            .orElse(null);

        if (duration == null) {
            return null;
        }

        // Calculate the next review date based off of the last review date
        Date reviewDate = hasReviewCycle.as(ReviewCycleContentModification.class).getReviewDate();

        if (reviewDate == null) {
            reviewDate = hasReviewCycle.as(Content.ObjectModification.class).getUpdateDate();
        }

        return duration.addCycleDuration(reviewDate);

    }

    @Override
    protected void beforeCommit() {

        if (!(getOriginalObject() instanceof Record)) {
            return;
        }

        Record originalObject = (Record) getOriginalObject();

        if (originalObject == null) {
            return;
        }

        Date now = new Date();
        // If firstPublish or Revision publish, set last date

        // First Publish
        if (ReviewCycleUtils.isFirstPublish(originalObject)) {
            originalObject.as(ReviewCycleContentModification.class)
                .setReviewDate(now);
        }

        // Revision/Draft
        Optional.of(originalObject)
            .map(State::getInstance)
            .map(state -> state.getExtra(EXTRA_OVERLAID_DRAFT))
            .filter(Draft.class::isInstance)
            .map(Draft.class::cast)
            .ifPresent(revision -> originalObject.as(ReviewCycleContentModification.class)
                .setReviewDate(now));

        if (originalObject.as(Site.ObjectModification.class).getOwner() == null) {
            this.setReviewCycleDuration(null);
        }

        setNextReviewDate(getNextReviewDate());

        super.beforeSave();
    }

    public ReviewCycleContentTypeMap getReviewCycleMap() {

        if (!(getOriginalObject() instanceof Record)) {
            return null;
        }

        ObjectType originalObjectType = ObjectType.getInstance(getOriginalObject().getClass());

        if (originalObjectType == null) {
            return null;
        }

        // Check for override
        if (this.getReviewCycleDurationForContentOverride() != null) {
            return new ReviewCycleContentTypeMap(
                    originalObjectType,
                    this.getReviewCycleDurationForContentOverride());
        } else {

            Site site = getOriginalObject().as(Site.ObjectModification.class).getOwner();
            if (site != null) {

                ReviewCycleSettings settings = SiteSettings.get(site, s -> s.as(ReviewCycleSiteSettings.class).getSettings());

                if (settings != null) {

                    List<ReviewCycleContentTypeMap> mapsList = SiteSettings.get(
                            site,
                            s -> settings.getContentTypeMaps());

                    return mapsList.stream()
                            .filter(map -> map.getContentType().equals(originalObjectType))
                            .findFirst().orElse(null);
                } else {

                    return null;
                }
            }
        }

        return null;
    }
}
