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
 * ReviewCycleGlobalDynamicType hides review cycle controls on global content
 */
public class ReviewCycleGlobalDynamicType implements DynamicType {

    @Override
    public void update(ObjectType type, Recordable recordable) {

        if (!(recordable instanceof HasReviewCycle)) {
            return;
        }

        if (recordable.as(Site.ObjectModification.class).getOwner() == null) {
            type.getFields().stream()
                .filter(objectField -> objectField.getInternalName()
                    .startsWith(ReviewCycleContentModification.FIELD_PREFIX))
                .forEach(field -> field.as(ToolUi.class).setHidden(true));
        }

        Site site = WebRequest.getCurrent().as(ToolRequest.class).getCurrentSite();

        // Get content type maps lists from sites & settings
        List<ReviewCycleContentTypeMap> mapsList = SiteSettings.get(
                site,
                s -> s.as(ReviewCycleSiteSettings.class).getSettings().getContentTypeMaps());

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
