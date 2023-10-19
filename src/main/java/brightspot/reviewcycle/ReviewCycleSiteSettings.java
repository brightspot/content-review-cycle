package brightspot.reviewcycle;

import com.psddev.cms.db.Site;
import com.psddev.cms.db.ToolUi;
import com.psddev.cms.db.ToolUser;
import com.psddev.cms.ui.ToolRequest;
import com.psddev.dari.db.Modification;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.State;
import com.psddev.dari.web.WebRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ReviewCycleSiteSettings is a modification of site that is shown in the CMS tab under Review Cycle.
 * See {@link ReviewCycleSettings wrapper class } so that the site settings configurations are not directly
 * on the site.
 * This can be overriden by the @see {@link ReviewCycleContentModification}.
 */
@ToolUi.FieldInternalNamePrefix(ReviewCycleSiteSettings.FIELD_PREFIX)
public class ReviewCycleSiteSettings extends Modification<Site> {

    public static final String FIELD_PREFIX = "reviewCycleSettings.";

    public static final String CONTENT_TYPE_MAPS_FIELD = "contentTypeMaps";
    public static final String CONTENT_TYPE_MAPS_FIELD_INTERNAL_NAME = FIELD_PREFIX + CONTENT_TYPE_MAPS_FIELD;

    private static final Logger LOGGER = LoggerFactory.getLogger(ReviewCycleSiteSettings.class);

    private transient boolean shouldReindex;

    private ReviewCycleSettings settings;

    public ReviewCycleSettings getSettings() {
        return settings;
    }

    public void setSettings(ReviewCycleSettings settings) {
        this.settings = settings;
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
    protected void beforeSave() {
        super.beforeSave();

        ReviewCycleSettings settings = getSettings();

        if (settings != null && settings.getOwner() == null) {
            settings.setOwner(getOriginalObject());
        }
    }
}
