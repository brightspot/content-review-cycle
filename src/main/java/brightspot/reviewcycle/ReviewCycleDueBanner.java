package brightspot.reviewcycle;

import java.io.IOException;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import brightspot.reviewcycle.notification.ReviewCycleDueWarningDuration;
import brightspot.reviewcycle.servlet.DismissReviewCycleServlet;
import brightspot.reviewcycle.servlet.StartReviewServlet;
import com.psddev.cms.db.Content;
import com.psddev.cms.tool.ContentEditWidget;
import com.psddev.cms.tool.ContentEditWidgetPlacement;
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
public class ReviewCycleDueBanner extends ContentEditWidget {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReviewCycleDueBanner.class);

    @Override
    public ContentEditWidgetPlacement getPlacement(ToolPageContext page, Object content) {
        return ContentEditWidgetPlacement.TOP;
    }

    @Override
    public String getHeading(ToolPageContext page, Object content) {
        return null;
    }

    @Override
    public boolean shouldDisplay(ToolPageContext page, Object content) {

        if (!(content instanceof Content)
            || WebRequest.getCurrent().getParameterNames().contains("draftId")) {
            return false;
        }

        UUID itemId = State.getInstance(content).getId();

        HasReviewCycle reviewCycleContent = Query.fromAll()
            .where("_id = ?", itemId)
            .findFirst()
            .map(Recordable.class::cast)
            .filter(recordable -> recordable.isInstantiableTo(HasReviewCycle.class))
            .map(recordable -> recordable.as(HasReviewCycle.class))
            .orElse(null);

        if (reviewCycleContent == null) {
            return false;
        }

        ReviewCycleContentTypeMap map = Optional.of(reviewCycleContent)
            .map(c -> c.as(ReviewCycleContentModification.class))
            .map(ReviewCycleContentModification::getReviewCycleMap)
            .orElse(null);

        if (map == null) {
            return false;
        }

        final Date now = Date.from(new Date().toInstant().truncatedTo(ChronoUnit.DAYS));

        if (PredicateParser.Static.evaluate(reviewCycleContent, map.getExpiredPredicate(now))) {
            return true;
        }

        return Optional.ofNullable(WebRequest.getCurrent()
                .as(ToolRequest.class)
                .getCurrentSite())
            .map(site -> site.as(ReviewCycleSiteSettings.class))
            .map(ReviewCycleSiteSettings::getSettings)
            .map(ReviewCycleSettings::getReviewCycleDueWarningDuration)
            .map(dueWarningDuration -> PredicateParser.Static.evaluate(
                content,
                ReviewCycleDueWarningDuration.getBannerDueWarningPredicate(now, dueWarningDuration)))
            .orElse(false);
    }

    @Override
    public void display(ToolPageContext page, Object content, ContentEditWidgetPlacement placement) throws IOException {

        UUID itemId = State.getInstance(content).getId();

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
            .map(c -> c.as(ReviewCycleContentModification.class))
            .map(ReviewCycleContentModification::getReviewCycleMap)
            .orElse(null);

        if (map != null) {
            // Get due warning duration
            final Date now = Date.from(new Date().toInstant().truncatedTo(ChronoUnit.DAYS));

            if (PredicateParser.Static.evaluate(reviewCycleContent, map.getExpiredPredicate(now))) {
                this.writeBanner(reviewCycleContent, page, true);
            } else {
                boolean showReviewDueWarning = Optional.ofNullable(WebRequest.getCurrent()
                        .as(ToolRequest.class)
                        .getCurrentSite())
                    .map(site -> site.as(ReviewCycleSiteSettings.class))
                    .map(ReviewCycleSiteSettings::getSettings)
                    .map(ReviewCycleSettings::getReviewCycleDueWarningDuration)
                    .map(dueWarningDuration -> PredicateParser.Static.evaluate(
                        content,
                        ReviewCycleDueWarningDuration.getBannerDueWarningPredicate(now, dueWarningDuration)))
                    .orElse(false);
                if (showReviewDueWarning) {
                    this.writeBanner(reviewCycleContent, page, false);
                }
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
                    .with(ToolLocalization.text(
                        ReviewCycleDueBanner.class,
                        "label.cancelReview",
                        "Cancel this review")))
                .with(Nodes.SPAN.with(ToolLocalization.text(ReviewCycleDueBanner.class, "label.OR", " OR ")))
                .with(Nodes.A
                    .href(createStartReviewServletUrl(item))
                    .target("content-edit-new-draft")
                    .with(ToolLocalization.text(ReviewCycleDueBanner.class, "label.startReview", "Start a review")))
            );

        try {
            page.write(bannerMessage);
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
