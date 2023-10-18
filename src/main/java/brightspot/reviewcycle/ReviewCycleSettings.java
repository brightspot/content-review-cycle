package brightspot.reviewcycle;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import brightspot.reviewcycle.notification.ReviewCycleDueWarningDuration;
import com.psddev.cms.db.Site;
import com.psddev.cms.db.ToolUi;
import com.psddev.cms.ui.form.Note;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Record;
import com.psddev.dari.db.Recordable;
import com.psddev.dari.db.StringException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Recordable.Embedded
public class ReviewCycleSettings extends Record {

    public static final String FIELD_PREFIX = "reviewCycleSettings.";
    public static final String CONTENT_TYPE_MAPS_FIELD = "contentTypeMaps";
    public static final String CONTENT_TYPE_MAPS_FIELD_INTERNAL_NAME = FIELD_PREFIX + CONTENT_TYPE_MAPS_FIELD;
    private static final String REVIEW_CYCLE_CLUSTER = "Review Cycle";
    private static final String REVIEW_CYCLE_TAB = "CMS";

    private static final Logger LOGGER = LoggerFactory.getLogger(ReviewCycleSiteSettings.class);

    @Required
    @ToolUi.Tab(REVIEW_CYCLE_TAB)
    @ToolUi.Cluster(REVIEW_CYCLE_CLUSTER)
    @DisplayName("Default Cycle Duration")
    private ReviewCycleDurationForContent reviewCycleDurationForContent;

    @Required
    @ToolUi.Tab(REVIEW_CYCLE_TAB)
    @ToolUi.Cluster(REVIEW_CYCLE_CLUSTER)
    @DisplayName("Due Warning Duration")
    @Note("The freshness banner will appear on content after the first notification is sent out")
    private List<ReviewCycleDueWarningDuration> reviewCycleDueWarningDurations;

    @Required
    @ToolUi.Tab(REVIEW_CYCLE_TAB)
    @ToolUi.Cluster(REVIEW_CYCLE_CLUSTER)
    @InternalName(ReviewCycleSettings.CONTENT_TYPE_MAPS_FIELD)
    private List<ReviewCycleContentTypeMap> contentTypeMaps;

    @ToolUi.Hidden
    private Site owner;

    public ReviewCycleDurationForContent getReviewCycleDurationForContent() {
        return reviewCycleDurationForContent;
    }

    public void setReviewCycleDurationForContent(ReviewCycleDurationForContent reviewCycleDurationForContent) {
        this.reviewCycleDurationForContent = reviewCycleDurationForContent;
    }

    public List<ReviewCycleDueWarningDuration> getReviewCycleDueWarningDurations() {
        if (reviewCycleDueWarningDurations == null) {
            reviewCycleDueWarningDurations = new ArrayList<>();
        }
        return reviewCycleDueWarningDurations;
    }

    public void setReviewCycleDueWarningDurations(List<ReviewCycleDueWarningDuration> reviewCycleDueWarningDurations) {
        this.reviewCycleDueWarningDurations = reviewCycleDueWarningDurations;
    }

    public List<ReviewCycleContentTypeMap> getContentTypeMaps() {
        if (contentTypeMaps == null) {
            contentTypeMaps = new ArrayList<>();
        }
        return contentTypeMaps;
    }

    public void setContentTypeMaps(List<ReviewCycleContentTypeMap> contentTypeMaps) {
        this.contentTypeMaps = contentTypeMaps;
    }

    public Site getOwner() {
        return owner;
    }

    public void setOwner(Site owner) {
        this.owner = owner;
    }

    @Override
    protected void onValidate() {
        Set<ObjectType> recordType = new HashSet<>();

        for (ReviewCycleContentTypeMap map : this.getContentTypeMaps()) {
            if (recordType.contains(map.getContentType())) {
                // If the content type is already selected, throw an error
                getState().addError(
                        getState().getField(
                                ReviewCycleSettings.FIELD_PREFIX + ReviewCycleSettings.CONTENT_TYPE_MAPS_FIELD),
                        new StringException("Content Type Maps must not contain duplicate Content Types!"));
            } else {
                recordType.add(map.getContentType());
            }
        }

        super.onValidate();
    }
}
