package brightspot.reviewcycle;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import brightspot.reviewcycle.notification.ReviewCycleDueWarningDuration;
import com.google.common.collect.ImmutableMap;
import com.psddev.cms.db.Site;
import com.psddev.cms.db.ToolUi;
import com.psddev.cms.db.ToolUi.Cluster;
import com.psddev.cms.db.ToolUi.Tab;
import com.psddev.cms.db.ToolUser;
import com.psddev.cms.ui.LocalizationContext;
import com.psddev.cms.ui.ToolLocalization;
import com.psddev.cms.ui.ToolRequest;
import com.psddev.cms.ui.form.DynamicPlaceholderMethod;
import com.psddev.cms.ui.form.Note;
import com.psddev.dari.db.Grouping;
import com.psddev.dari.db.Modification;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.State;
import com.psddev.dari.db.StringException;
import com.psddev.dari.util.UuidUtils;
import com.psddev.dari.web.WebRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ReviewCycleSiteSettings is a modification of site that is shown in the CMS tab under Review Cycle. This will set the
 * default cycle duration along with the default due warning duration.This is where the list of content type maps are
 * chosen. If you save an empty content type map then the values from the default cycle duration and default due warning
 * duration will be chosen. This can be overriden by the @see {@link ReviewCycleContentModification}.
 */
@ToolUi.FieldInternalNamePrefix(ReviewCycleSiteSettings.FIELD_PREFIX)
public class ReviewCycleSiteSettings extends Modification<Site> {

    public static final String FIELD_PREFIX = "reviewCycleSettings.";
    public static final String CONTENT_TYPE_MAPS_FIELD = "contentTypeMaps";
    public static final String CONTENT_TYPE_MAPS_FIELD_INTERNAL_NAME = FIELD_PREFIX + CONTENT_TYPE_MAPS_FIELD;
    private static final String REVIEW_CYCLE_CLUSTER = "Review Cycle";
    private static final String REVIEW_CYCLE_TAB = "CMS";

    private transient boolean shouldReindex;

    private static final Logger LOGGER = LoggerFactory.getLogger(ReviewCycleSiteSettings.class);

    @Required
    @Tab(REVIEW_CYCLE_TAB)
    @Cluster(REVIEW_CYCLE_CLUSTER)
    @DisplayName("Default Cycle Duration")
    private ReviewCycleDurationForContent reviewCycleDurationForContent;

    @Required
    @Tab(REVIEW_CYCLE_TAB)
    @Cluster(REVIEW_CYCLE_CLUSTER)
    @DisplayName("Due Warning Duration")
    @Note("Durations should be from longest to shortest")
    @DynamicPlaceholderMethod("getDefaultDueWarningDuration")
    private List<ReviewCycleDueWarningDuration> reviewCycleDueWarningDurations;

    @Tab(REVIEW_CYCLE_TAB)
    @Cluster(REVIEW_CYCLE_CLUSTER)
    @InternalName(ReviewCycleSiteSettings.CONTENT_TYPE_MAPS_FIELD)
    private List<ReviewCycleContentTypeMap> contentTypeMaps;

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

    @Override
    protected void beforeCommit() {
        super.beforeCommit();
        if (getState().isNew()) {
            return;
        }
        // Fetch original from database to check for changes and recalculate
        Site databaseSite = ((Site) Query.fromAll()
            .where("_id = ?", getOriginalObject().getId())
            .first());

        if (databaseSite == null) {
            return;
        }

        ReviewCycleSiteSettings databaseSettings = databaseSite.as(ReviewCycleSiteSettings.class);
        if (databaseSettings == null) {
            return;
        }
        State databaseState = databaseSettings.getState();

        // check if changed
        if (databaseState != null
            && Utils.computeDelta(databaseSettings, this).get(CONTENT_TYPE_MAPS_FIELD_INTERNAL_NAME)
            != null) {
            String info = "CONTENT TYPE MAPS FIELD HAS CHANGED. TRIGGERING REINDEX";

            ToolUser user = WebRequest.getCurrent().as(ToolRequest.class).getCurrentUser();

            if (user != null) {
                info += "Change made by: " + user.getName() + " {" + user.getId() + "} ";
            }
            LOGGER.info(info);
            shouldReindex = true;
        }
    }

    @Override
    protected void onValidate() {
        Set<ObjectType> recordType = new HashSet<>();

        for (ReviewCycleContentTypeMap map : this.getContentTypeMaps()) {
            if (recordType.contains(map.getContentType())) {
                // If the content type is already selected, throw an error
                getState().addError(
                    getState().getField(
                        ReviewCycleSiteSettings.FIELD_PREFIX + ReviewCycleSiteSettings.CONTENT_TYPE_MAPS_FIELD),
                    new StringException("Content Type Maps must not contain duplicate Content Types!"));
            } else {
                recordType.add(map.getContentType());
            }
        }

        super.onValidate();
    }

    public String getOneOffTypeConfigurations() {
        Set<ObjectType> siteSettingsConfiguredTypes = this.getContentTypeMaps()
            .stream()
            .map(ReviewCycleContentTypeMap::getContentType)
            .collect(Collectors.toSet());

        String overrideTypes = Query.from(HasReviewCycle.class)
            .where("* matches *")
            .and(ReviewCycleContentModification.NEXT_REVIEW_DATE_INDEX_FIELD_INTERNAL_NAME + " != missing")
            .and(getOriginalObject().itemsPredicate())
            .groupBy("_type")
            .stream()
            .map(Grouping::getKeys)
            .filter(keys -> !keys.isEmpty())
            .map(key -> key.get(0))
            .filter(String.class::isInstance)
            .map(String.class::cast)
            .map(UuidUtils::fromString)
            .map(ObjectType::getInstance)
            .filter(objType -> !siteSettingsConfiguredTypes.contains(objType))
            .map(ObjectType::getDisplayName)
            .collect(Collectors.joining(", "));

        return ToolLocalization.text(new LocalizationContext(
            ReviewCycleSiteSettings.class,
            ImmutableMap.of("types", overrideTypes)), "note.oneOffTypeConfigurations");
    }
}
