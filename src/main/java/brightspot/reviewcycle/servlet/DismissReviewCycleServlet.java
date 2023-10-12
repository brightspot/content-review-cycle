package brightspot.reviewcycle.servlet;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import javax.servlet.http.HttpServletResponse;

import brightspot.reviewcycle.ReviewCycleContentModification;
import com.psddev.cms.db.Content;
import com.psddev.cms.db.History;
import com.psddev.cms.db.ToolUser;
import com.psddev.cms.ui.ToolRequest;
import com.psddev.dari.db.Query;
import com.psddev.dari.web.AbstractWebPage;
import com.psddev.dari.web.WebRequest;
import com.psddev.dari.web.annotation.WebPath;
import com.psddev.dari.web.annotation.WebPathGroup;
import org.apache.http.HttpHeaders;

/**
 * This class handles when a user chooses the option to dismiss a review of a content type.
 */
@WebPathGroup("cms")
@WebPath(DismissReviewCycleServlet.PATH)
public class DismissReviewCycleServlet extends AbstractWebPage {

    public static final String PATH = "/dismiss-review-servlet";
    public static final String RECORD_ID_PARAMETER = "recordid";

    @Override
    protected void onGet() throws Exception {

        if (!WebRequest.isAvailable()) {
            throw new RuntimeException("Web Request is unavailable.");
        }
        WebRequest currentWebRequest = WebRequest.getCurrent();

        UUID recordId = Optional.ofNullable(currentWebRequest.getParameter(UUID.class, RECORD_ID_PARAMETER))
            .orElse(null);

        if (recordId == null) {
            throw new IllegalArgumentException("Malformed Request: Missing or invalid record UUID");
        }

        Content dismissedContent = Query.from(Content.class)
            .where("_id = ?", recordId)
            .first();

        if (dismissedContent != null) {
            // Update review cycle date on review
            dismissedContent.as(ReviewCycleContentModification.class).setReviewDate(new Date());

            // Create revision history element
            ToolUser currentUser = WebRequest.getCurrent().as(ToolRequest.class).getCurrentUser();
            History dismissedHistory = new History(currentUser, dismissedContent);
            dismissedHistory.setName("Review dismissed");

            dismissedHistory.saveImmediately();
            dismissedContent.saveImmediately();

            String redirectUrl = currentWebRequest.as(ToolRequest.class).getPathBuilder("/content/edit.jsp")
                .setParameter("id", recordId)
                .build();

            // Redirect to edit page
            response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
            response.setHeader(HttpHeaders.LOCATION, redirectUrl);
        }
    }
}
