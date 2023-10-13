package brightspot.reviewcycle.notification;

import java.text.SimpleDateFormat;

import brightspot.reviewcycle.Utils;
import com.psddev.cms.notification.ToolSubscription;
import com.psddev.cms.notification.ToolUserOnlySubscription;
import com.psddev.cms.ui.ToolRequest;
import com.psddev.cms.ui.form.Note;
import com.psddev.dari.db.Recordable;
import com.psddev.dari.html.Nodes;
import com.psddev.dari.html.text.AElement;
import com.psddev.dari.notification.Subscriber;
import com.psddev.dari.util.RoutingFilter;
import com.psddev.dari.web.UrlBuilder;
import com.psddev.dari.web.WebRequest;
import org.apache.commons.lang3.StringUtils;

@Recordable.DisplayName("Review Cycle Due")
@Note("Sends notification if a content is due for review")
public class ReviewCycleDueSubscription extends ToolSubscription<ReviewCycleNotificationBundle>
    implements ToolUserOnlySubscription {

    // Example:
    // REVIEW DUE: The following content in [Site name param] is due for content review on [Review date param] (MM/DD/YYYY): [link to asset]

    @Override
    public String toHtmlFormat(Subscriber subscriber, ReviewCycleNotificationBundle payload) {

        if (payload != null && payload.getOwnerName() != null && payload.getContentId() != null
            && payload.getContentLabel() != null && payload.getDueDate() != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");

            String prefix = "REVIEW DUE: The following content in ";
            String siteName = payload.getOwnerName();
            String body = " is due for content review on ";
            String dateString = "(" + dateFormat.format(payload.getDueDate()) + ")";
            String formatting = ": ";
            String contentName = payload.getContentLabel();

            String redirectUrl = Utils.fullyQualifiedCmsUrlBuilder("/content/edit.jsp")
                .addParameter("id", payload.getContentId())
                .build();

            AElement link = Nodes.A
                .href(redirectUrl)
                .target("_blank")
                .with(contentName);

            return Nodes.P
                .with(prefix + siteName + body + dateString + formatting)
                .with(link)
                .toString();
        }

        return super.toHtmlFormat(subscriber, payload);
    }
}
