package brightspot.reviewcycle;

import java.io.IOException;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import brightspot.core.tool.EditTopHtml;
import brightspot.reviewcycle.notification.ReviewCycleDueWarningDuration;
import brightspot.reviewcycle.servlet.DismissReviewCycleServlet;
import brightspot.reviewcycle.servlet.StartReviewServlet;
import com.psddev.cms.db.Content;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.cms.ui.ToolLocalization;
import com.psddev.cms.ui.ToolRequest;
import com.psddev.dari.db.PredicateParser;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.Recordable;
import com.psddev.dari.db.State;
import com.psddev.dari.html.FlowFlowElement;
import com.psddev.dari.html.Nodes;
import com.psddev.dari.web.WebRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ReviewCycleDueBanner is associated with the banner shown at the top of the content type edit page ready for review.
 */
public class ReviewCycleDueBanner implements EditTopHtml {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReviewCycleDueBanner.class);

    @Override
    public int getPriority() {
        return 1;
    }

    @Override
    public boolean isSupported(Object object) {
        return object instanceof Content && !WebRequest.getCurrent().getParameterNames().contains("draftId");
    }

    @Override
    public void writeHtml(Object item, ToolPageContext page) {
        UUID itemId = State.getInstance(item).getId();
        HasReviewCycle reviewCycleContent = Query.fromAll()
            .where("_id = ?", itemId)
            .findFirst()
            .map(Recordable.class::cast)
            .filter(recordable -> recordable.isInstantiableTo(HasReviewCycle.class))
            .map(recordable -> recordable.as(HasReviewCycle.class))
            .orElse(null);

        if (reviewCycleContent == null) {
            return;
        }

        ReviewCycleContentTypeMap map = Optional.of(reviewCycleContent)
            .map(content -> content.as(ReviewCycleContentModification.class))
            .map(ReviewCycleContentModification::getReviewCycleMap)
            .orElse(null);

        // Get due warning duration
        ReviewCycleDueWarningDuration dueWarningDuration = WebRequest.getCurrent()
                .as(ToolRequest.class)
                .getCurrentSite().as(ReviewCycleSiteSettings.class)
                .getSettings().getReviewCycleDueWarningDuration();

        if (map != null) {
            Date now = Date.from(new Date().toInstant().truncatedTo(ChronoUnit.DAYS));

            if (PredicateParser.Static.evaluate(reviewCycleContent, map.getExpiredPredicate(now))) {
                this.writeBanner(reviewCycleContent, page, true);
            } else if (PredicateParser.Static.evaluate(
                item,
                ReviewCycleDueWarningDuration.getBannerDueWarningPredicate(now, dueWarningDuration))) {
                this.writeBanner(reviewCycleContent, page, false);
            }
        }
    }

    private void writeBanner(HasReviewCycle item, ToolPageContext page, boolean pastDue) {

        String messageLevel = "message message-info";
        String message = "The content in this item is ALMOST due for review: ";
        if (pastDue) {
            messageLevel = "message message-warning";
            message = "The content in this item is DUE for review: ";
        }

        ToolLocalization.text(ReviewCycleDueBanner.class, "label.messageLevel", messageLevel);
        ToolLocalization.text(ReviewCycleDueBanner.class, "label.message", message);

        FlowFlowElement bannerMessage = Nodes.DIV.className(messageLevel)
            .with(Nodes.P
                .with(Nodes.SPAN.with(message))
                .with(Nodes.A.href(createDismissReviewServletUrl(item))
                .with(ToolLocalization.text(ReviewCycleDueBanner.class, "label.cancelReview", "Cancel this review")))
                .with(Nodes.SPAN.with(ToolLocalization.text(ReviewCycleDueBanner.class, "label.OR", " OR ")))
                .with(Nodes.A
                    .href(createStartReviewServletUrl(item))
                    .target("content-edit-new-draft")
                    .with(ToolLocalization.text(ReviewCycleDueBanner.class, "label.startReview", "Start a review")))
            );

        try {
            page.write(bannerMessage);

            // Add this temporarily
            page.writeStart("br");
            page.writeStart("br");
        } catch (IOException e) {
            LOGGER.warn("Error while writing review cycle banner!", e);
        }
    }

    private String createDismissReviewServletUrl(HasReviewCycle item) {
        return WebRequest.getCurrent().as(ToolRequest.class).getPathBuilder(DismissReviewCycleServlet.PATH)
            .addParameter(DismissReviewCycleServlet.RECORD_ID_PARAMETER, item.getState().getId())
            .build();
    }

    private String createStartReviewServletUrl(HasReviewCycle item) {
        return WebRequest.getCurrent().as(ToolRequest.class).getPathBuilder(StartReviewServlet.PATH)
            .addParameter(StartReviewServlet.RECORD_ID_PARAMETER, item.getState().getId())
            .build();
    }
}
