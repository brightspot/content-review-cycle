package brightspot.reviewcycle;

import com.psddev.cms.db.Site;
import com.psddev.cms.db.ToolUi;
import com.psddev.cms.ui.form.DynamicType;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Recordable;

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
    }
}
