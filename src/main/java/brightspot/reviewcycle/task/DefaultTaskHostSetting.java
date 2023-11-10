package brightspot.reviewcycle.task;

import com.psddev.cms.db.ToolUi;
import com.psddev.cms.tool.CmsTool;
import com.psddev.dari.db.Modification;

public class DefaultTaskHostSetting extends Modification<CmsTool> {

    @ToolUi.Cluster("Advanced")
    @ToolUi.Tab("CMS")
    private String defaultTaskHost;

    public String getDefaultTaskHost() {
        return defaultTaskHost;
    }

    public void setDefaultTaskHost(String defaultTaskHost) {
        this.defaultTaskHost = defaultTaskHost;
    }
}