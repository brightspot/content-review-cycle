package brightspot.reviewcycle;

import java.util.List;
import java.util.stream.Collectors;

import com.psddev.cms.db.Site;
import com.psddev.cms.db.SiteSettings;
import com.psddev.cms.db.ToolUi;
import com.psddev.cms.ui.ToolRequest;
import com.psddev.cms.ui.form.DynamicType;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Recordable;
import com.psddev.dari.web.WebRequest;

/**
 * ReviewCycleGlobalDynamicType hides review cycle controls on content not configured in review cycle settings content type map
 */
public class ReviewCycleOverrideType implements DynamicType {

    @Override
    public void update(ObjectType type, Recordable recordable) {

        if (!(recordable instanceof HasReviewCycle)) {
            return;
        }

        Site site = WebRequest.getCurrent().as(ToolRequest.class).getCurrentSite();

        // Get content type maps lists from sites & settings
        ReviewCycleSettings settings = SiteSettings.get(site, s -> s.as(ReviewCycleSiteSettings.class).getSettings());

        // If settings is disabled, we hide review cycle overrides fields from ALL content types
        if (settings == null) {
            type.getFields().stream()
                    .filter(objectField -> objectField.getInternalName()
                            .startsWith(ReviewCycleContentModification.FIELD_PREFIX))
                    .forEach(field -> field.as(ToolUi.class).setHidden(true));

        // Else, we hide SPECIFIC overrides fields
        } else {

            List<ReviewCycleContentTypeMap> mapsList = SiteSettings.get(
                    site,
                    s -> settings.getContentTypeMaps());

            // Get the content type maps into a list of object types
            List<ObjectType> reviewCycleContentTypeMapObjectList
                    = mapsList
                    .stream()
                    .map(ReviewCycleContentTypeMap::getContentType)
                    .collect(Collectors.toList());

            // If true, then we know that sites & settings content type map list does not have the object configured
            if (!reviewCycleContentTypeMapObjectList.contains(type)) {
                type.getFields().stream()
                        .filter(objectField -> objectField.getInternalName()
                                .startsWith(ReviewCycleContentModification.FIELD_PREFIX))
                        .forEach(field -> field.as(ToolUi.class).setHidden(true));
            }
        }
    }
}
