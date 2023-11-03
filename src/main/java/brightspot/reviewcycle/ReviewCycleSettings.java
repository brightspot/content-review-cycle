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

/**
 * This class will set the default cycle duration. This class is also where the list of content type maps are
 * created, where each contains the content type to be configured along with its cycle duration. Additionally, a list of
 * due warning durations can be populated, where the review cycle due banner will appear in the content type after the
 * first notification is sent.
 */
@Recordable.Embedded
public class ReviewCycleSettings extends Record {

    public static final String FIELD_PREFIX = "reviewCycleSettings.";
    public static final String CONTENT_TYPE_MAPS_FIELD = "contentTypeMaps";
    public static final String CONTENT_TYPE_MAPS_FIELD_INTERNAL_NAME = FIELD_PREFIX + CONTENT_TYPE_MAPS_FIELD;

    @Required
    @DisplayName("Banner Warning Duration")
    @Note("The freshness banner will appear on content when the review cycle due date is within this amount of time.")
    private ReviewCycleDueWarningDuration reviewCycleDueWarningDuration;

    @Required
    @InternalName(ReviewCycleSettings.CONTENT_TYPE_MAPS_FIELD)
    @DisplayName("Content Types")
    private List<ReviewCycleContentTypeMap> contentTypeMaps;

    @Required
    @ToolUi.Cluster("Notification Settings")
    @DisplayName("Notification Warning Times")
    @Note("Notifications will be sent out daily to content past due and due in specific days, weeks, or months.")
    private List<ReviewCycleDueWarningDuration> notificationWarningTimes;

    @ToolUi.Hidden
    private Site owner;

    public ReviewCycleDueWarningDuration getReviewCycleDueWarningDuration() {
        return reviewCycleDueWarningDuration;
    }

    public void setReviewCycleDueWarningDurations(ReviewCycleDueWarningDuration reviewCycleDueWarningDuration) {
        this.reviewCycleDueWarningDuration = reviewCycleDueWarningDuration;
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

    public List<ReviewCycleDueWarningDuration> getNotificationWarningTimes() {
        if (notificationWarningTimes == null) {
            notificationWarningTimes = new ArrayList<>();
        }
        return notificationWarningTimes;
    }

    public void setNotificationWarningTimes(List<ReviewCycleDueWarningDuration> notificationWarningTimes) {
        this.notificationWarningTimes = notificationWarningTimes;
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
