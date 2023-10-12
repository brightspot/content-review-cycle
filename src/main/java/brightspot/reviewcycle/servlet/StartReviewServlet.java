package brightspot.reviewcycle.servlet;

import java.util.Optional;
import java.util.UUID;
import javax.servlet.http.HttpServletResponse;

import com.psddev.cms.db.Content;
import com.psddev.cms.ui.ToolRequest;
import com.psddev.dari.db.Query;
import com.psddev.dari.web.AbstractWebPage;
import com.psddev.dari.web.WebRequest;
import com.psddev.dari.web.annotation.WebPath;
import com.psddev.dari.web.annotation.WebPathGroup;
import org.apache.http.HttpHeaders;

/**
 * This class handles when a user chooses the option to start a review of a content type.
 */
@WebPathGroup("cms")
@WebPath(StartReviewServlet.PATH)
public class StartReviewServlet extends AbstractWebPage {

    public static final String PATH = "/start-review-servlet";
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
            String redirectUrl = currentWebRequest.as(ToolRequest.class).getPathBuilder("/content/edit/new-draft")
                .setParameter("id", recordId)
                .build();

            // Redirect to edit page
            response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
            response.setHeader(HttpHeaders.LOCATION, redirectUrl);
        }
    }
}