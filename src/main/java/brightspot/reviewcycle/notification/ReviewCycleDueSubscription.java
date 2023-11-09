package brightspot.reviewcycle.notification;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import brightspot.reviewcycle.ReviewCycleUtils;
import com.google.common.collect.ImmutableMap;
import com.psddev.cms.notification.ToolSubscription;
import com.psddev.cms.notification.ToolUserOnlySubscription;
import com.psddev.cms.ui.LocalizationContext;
import com.psddev.cms.ui.ToolLocalization;
import com.psddev.cms.ui.form.Note;
import com.psddev.dari.db.Recordable;
import com.psddev.dari.html.Nodes;
import com.psddev.dari.html.text.AElement;
import com.psddev.dari.notification.Subscriber;

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
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            String formattedDueDateUTC = dateFormat.format(payload.getDueDate());

            String prefix = "REVIEW DUE: The following content in ";
            String siteName = payload.getOwnerName();
            String body;

            // Check payload due date
            if (payload.getDueDate().before(new Date())) {
                body = " was due for content review on ";
            } else {
                body = " is due for content review on ";
            }

            String dateString = "(" + dateFormat.format(formattedDueDateUTC) + ")";
            String formatting = ": ";
            String contentName = payload.getContentLabel();

            String defaultText = prefix + siteName + body + dateString + formatting;

            String localized = ToolLocalization.text(
                    new LocalizationContext(
                            ReviewCycleDueSubscription.class,
                            ImmutableMap.of(
                                    "siteName", payload.getOwnerName(),
                                    "dateString", dateFormat.format(formattedDueDateUTC),
                                    "contentLabel", payload.getContentLabel()
                            )
                    ),
                    "label.reviewDue",
                    defaultText);

            String redirectUrl = ReviewCycleUtils.fullyQualifiedCmsUrlBuilder("/content/edit.jsp")
                .addParameter("id", payload.getContentId())
                .build();

            AElement link = Nodes.A
                .href(redirectUrl)
                .target("_blank")
                .with(contentName);

            return Nodes.P
                .with(localized)
                .with(link)
                .toString();
        }

        return super.toHtmlFormat(subscriber, payload);
    }
}
