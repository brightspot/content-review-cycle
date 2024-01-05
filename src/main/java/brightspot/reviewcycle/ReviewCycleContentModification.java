package brightspot.reviewcycle;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import com.psddev.cms.db.Content;
import com.psddev.cms.db.Draft;
import com.psddev.cms.db.Site;
import com.psddev.cms.db.SiteSettings;
import com.psddev.cms.db.ToolUi;
import com.psddev.cms.db.ToolUi.Cluster;
import com.psddev.cms.db.ToolUi.Tab;
import com.psddev.cms.ui.form.DynamicTypeClass;
import com.psddev.cms.ui.form.Note;
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
public class ReviewCycleContentModification extends Modification<HasReviewCycle> {

    private static final String REVIEW_CYCLE_CLUSTER = "Review Cycle Overrides";
    private static final String REVIEW_CYCLE_TAB = "Overrides";

    public static final String FIELD_PATH = "brightspot.reviewcycle.ReviewCycleContentModification/";
    public static final String FIELD_PREFIX = "reviewCycleOverrides.";
    public static final String EXTRA_OVERLAID_DRAFT = "cms.tool.overlaidDraft";

    public static final String REVIEW_DATE_FIELD = "reviewDate";
    public static final String REVIEW_DATE_FIELD_INTERNAL_NAME = FIELD_PREFIX + REVIEW_DATE_FIELD;

    public static final String NEXT_REVIEW_DATE_FIELD = "nextReviewDate";
    public static final String NEXT_REVIEW_DATE_FIELD_INTERNAL_NAME = FIELD_PREFIX + NEXT_REVIEW_DATE_FIELD;

    public static final String REVIEW_CYCLE_DURATION_FIELD = "reviewCycleDuration";
    public static final String REVIEW_CYCLE_DURATION_FIELD_INTERNAL_NAME =
        FIELD_PATH + FIELD_PREFIX + REVIEW_CYCLE_DURATION_FIELD;

    public static final String NEXT_REVIEW_DATE_INDEX_FIELD = "getNextReviewDateIndex";
    public static final String NEXT_REVIEW_DATE_INDEX_FIELD_INTERNAL_NAME =
        FIELD_PATH + FIELD_PREFIX + NEXT_REVIEW_DATE_INDEX_FIELD;

    private static final DateFormat FORMAT = new SimpleDateFormat("EEE, MMM dd, yyyy");

    @Tab(REVIEW_CYCLE_TAB)
    @Cluster(REVIEW_CYCLE_CLUSTER)
    @InternalName(REVIEW_DATE_FIELD)
    @DisplayName("Last Cycle Duration Date")
    @ToolUi.ReadOnly
    private Date reviewDate;

    @Tab(REVIEW_CYCLE_TAB)
    @Cluster(REVIEW_CYCLE_CLUSTER)
    @InternalName(NEXT_REVIEW_DATE_FIELD)
    @ToolUi.ReadOnly
    private Date nextReviewDate;

    @Tab(REVIEW_CYCLE_TAB)
    @Cluster(REVIEW_CYCLE_CLUSTER)
    @DisplayName("Review Cycle Duration for This Content Only")
    @Note("Setting this override will calculate the next review date from the last cycle duration date, therefore it is recommended you start or cancel the review before modifying it. ")
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
        return nextReviewDate;
    }

    public void setNextReviewDate(Date nextReviewDate) {
        this.nextReviewDate = nextReviewDate;
    }

    @Indexed
    @ToolUi.Filterable
    @ToolUi.Sortable
    @ToolUi.Hidden
    public Date getNextReviewDateIndex() {
        // Or else should do the look-up for the date. Truncate whatever is returned
        Date nextReview = Optional.ofNullable(getNextReviewDate()).orElseGet(this::calculateNextReviewDate);
        return Optional.ofNullable(nextReview)
            .map(Date::toInstant)
            .map(instant -> instant.truncatedTo(ChronoUnit.DAYS))
            .map(Date::from)
            .orElse(null);
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

        // If next review is manually set, calculate last review and clear setting
        if (getNextReviewDate() != null) {
            if (this.getReviewCycleMap() != null && this.getReviewCycleMap().getCycleDuration() != null) {
                this.setReviewDate(this.getReviewCycleMap()
                    .getCycleDuration()
                    .subtractCycleDuration(getNextReviewDate()));
            }
            this.setNextReviewDate(null);
        }

        this.setNextReviewDate(calculateNextReviewDate());

        if (originalObject.as(Site.ObjectModification.class).getOwner() == null) {
            this.setReviewCycleDuration(null);
        }

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
                List<ReviewCycleContentTypeMap> mapsList = SiteSettings.get(
                    site,
                    s -> s.as(ReviewCycleSiteSettings.class).getSettings().getContentTypeMaps());
                return mapsList.stream()
                    .filter(map -> map.getContentType().equals(originalObjectType))
                    .findFirst().orElse(null);
            }
        }

        return null;
    }

}
