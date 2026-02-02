package brightspot.reviewcycle.task;

import brightspot.reviewcycle.ReviewCycleSiteSettings;
import com.psddev.cms.db.ToolUi;
import com.psddev.cms.tool.CmsTool;
import com.psddev.cms.ui.form.DynamicPlaceholderMethod;
import com.psddev.dari.db.Modification;
import com.psddev.dari.db.Recordable;
import com.psddev.dari.util.ObjectUtils;

@Recordable.FieldInternalNamePrefix("reviewcycle.")
public class DefaultTaskHostSetting extends Modification<CmsTool> {

    @ToolUi.Cluster(ReviewCycleSiteSettings.CLUSTER)
    @ToolUi.Tab(ReviewCycleSiteSettings.TAB)
    @DynamicPlaceholderMethod("getFallbackTaskHost")
    private String defaultTaskHost;

    public String getDefaultTaskHost() {
        if (defaultTaskHost == null) {
            return getFallbackTaskHost();
        }
        return defaultTaskHost;
    }

    public void setDefaultTaskHost(String defaultTaskHost) {
        this.defaultTaskHost = defaultTaskHost;
    }

    private String getFallbackTaskHost() {
        // This is mainly for backwards compatibility, early versions
        // forgot a prefix so this is falling back to that old value
        return ObjectUtils.to(String.class, getOriginalObject().getState().get("defaultTaskHost"));
    }
}
