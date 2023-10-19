package brightspot.reviewcycle.widget;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import brightspot.reviewcycle.HasReviewCycle;
import brightspot.reviewcycle.ReviewCycleContentModification;
import com.google.common.collect.ImmutableMap;
import com.psddev.cms.db.Content;
import com.psddev.cms.db.Directory;
import com.psddev.cms.db.Site;
import com.psddev.cms.db.ToolUi;
import com.psddev.cms.db.ToolUser;
import com.psddev.cms.tool.Dashboard;
import com.psddev.cms.tool.DefaultDashboardWidget;
import com.psddev.cms.tool.QueryRestriction;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.cms.ui.LocalizationContext;
import com.psddev.cms.ui.ToolLocalization;
import com.psddev.cms.ui.ToolRequest;
import com.psddev.cms.ui.page.ContentSummary;
import com.psddev.dari.db.Grouping;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Predicate;
import com.psddev.dari.db.PredicateParser;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.Recordable;
import com.psddev.dari.db.State;
import com.psddev.dari.util.JspUtils;
import com.psddev.dari.util.PaginatedResult;
import com.psddev.dari.util.UuidUtils;
import com.psddev.dari.web.UrlBuilder;
import com.psddev.dari.web.WebRequest;
import org.joda.time.DateTime;

/** The ReviewActivityWidget shows the content types due soon or past due. It can be filtered by any content type. */
public class ReviewActivityWidget extends DefaultDashboardWidget {

    private static final int[] LIMITS = { 10, 20, 50 };

    @Override
    public int getColumnIndex() {
        return 0;
    }

    @Override
    public int getWidgetIndex() {
        return 0;
    }

    @Override
    public void writeHtml(ToolPageContext page, Dashboard dashboard) throws IOException {

        /** Check types configured in @see {@link brightspot.reviewcycle.ReviewCycleSiteSettings} and at the content level */
        Site site = WebRequest.getCurrent().as(ToolRequest.class).getCurrentSite();

        List<ObjectType> configuredObjectTypes;

        if (site == null) {
            configuredObjectTypes = Query.from(HasReviewCycle.class)
                    .where("* matches *")
                    .and(ReviewCycleContentModification.NEXT_REVIEW_DATE_INDEX_FIELD_INTERNAL_NAME + " != missing")
                    .groupBy("_type")
                    .stream()
                    .map(Grouping::getKeys)
                    .filter(keys -> !keys.isEmpty())
                    .map(key -> key.get(0))
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .map(UuidUtils::fromString)
                    .map(ObjectType::getInstance)
                    .sorted()
                    .collect(Collectors.toList());
        } else {
            configuredObjectTypes = Query.from(HasReviewCycle.class)
                    .where("* matches *")
                    .and(ReviewCycleContentModification.NEXT_REVIEW_DATE_INDEX_FIELD_INTERNAL_NAME + " != missing")
                    .and(site.itemsPredicate())
                    .groupBy("_type")
                    .stream()
                    .map(Grouping::getKeys)
                    .filter(keys -> !keys.isEmpty())
                    .map(key -> key.get(0))
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .map(UuidUtils::fromString)
                    .map(ObjectType::getInstance)
                    .sorted()
                    .collect(Collectors.toList());
        }

        // Original type param selector
        ObjectType itemType = ObjectType.getInstance(page.pageParam(UUID.class, "itemType", null));
        long offset = WebRequest.getCurrent().getParameter(long.class, "offset");
        int limit = page.pageParam(Integer.class, "limit", 20);

        PaginatedResult<?> result;

        // --- Find Results ---
        boolean useItemType = itemType != null;

        Query<?> contentQuery = (useItemType
            ? Query.fromType(itemType)
            : Query.fromGroup(Content.SEARCHABLE_GROUP))
            .where(page.userTypesPredicate(useItemType ? itemType : null))
            .and(page.siteItemsSearchPredicate())
            .and(Content.UPDATE_DATE_FIELD + " != missing")
            .sortDescending(Content.UPDATE_DATE_FIELD);

        QueryRestriction.updateQueryUsingAll(contentQuery, page);

        // Due target
        String paramTarget = page.pageParam(String.class, "dueTarget", null);

        DueTarget dueTarget = Optional.ofNullable(DueTarget.TARGET_KEY_MAPPING.get(paramTarget))
            .orElse(DueTarget.DUE_SOON);

        contentQuery.and(dueTarget.getPredicate());

        result = contentQuery.and("_any matches *").select(offset, limit);

        // --- Draw widget ---

        page.writeStart("div", "class", "widget");
        page.writeStart("h1", "class", "icon icon-list");
        page.writeHtml(ToolLocalization.text(ReviewActivityWidget.class, "title", "Review Cycle Activity"));
        page.writeEnd();

        page.writeStart("div", "class", "widget-filters");
        for (Class<? extends QueryRestriction> c : QueryRestriction.classIterable()) {
            page.writeQueryRestrictionForm(c);
        }

        // Type select
        page.writeStart("form",
            "method", "get",
            "action", page.url(null));

        page.writeTypeSelect(
            configuredObjectTypes,
            itemType,
            ToolLocalization.text(ReviewActivityWidget.class, "label.allTypes", "Any Content Type"),
            "data-bsp-autosubmit", "",
            "name", "itemType",
            "data-searchable", "true");

        page.writeStart("select", "data-bsp-autosubmit", "", "name", "dueTarget");
        for (DueTarget target : DueTarget.values()) {
            page.writeStart("option",
                "selected", target.getTargetKey().equals(paramTarget) ? "selected" : null,
                "value", target.getTargetKey());
            page.writeHtml(ToolLocalization.text(null, target.getTargetLabel()));
            page.writeEnd();

        }
        page.writeEnd();

        page.writeEnd();
        page.writeEnd();

        if (result == null) {
            page.writeStart("div", "class", "message message-warning");
            page.writeStart("p");
            page.writeEnd();
            page.writeEnd();

        } else if (!result.hasPages()) {
            page.writeStart("div", "class", "message message-info");
            page.writeStart("p");
            page.writeHtml(ToolLocalization.text(ReviewActivityWidget.class, "message.noActivity"));
            page.writeEnd();
            page.writeEnd();

        } else {
            page.writeStart("ul", "class", "pagination");

            if (result.hasPrevious()) {
                page.writeStart("li", "class", "first");
                page.writeStart("a",
                    "title", ToolLocalization.text(ReviewActivityWidget.class, "pagination.firstPage"),
                    "href", page.url("", "offset", result.getFirstOffset()));
                page.writeHtml(ToolLocalization.text(ReviewActivityWidget.class, "pagination.firstPage"));
                page.writeEnd();
                page.writeEnd();

                page.writeStart("li", "class", "previous");
                page.writeStart("a",
                    "title", ToolLocalization.text(new LocalizationContext(
                        ReviewActivityWidget.class,
                        ImmutableMap.of("count", limit)), "pagination.previousCount"),
                    "href", page.url("", "offset", result.getPreviousOffset()));
                page.writeHtml(ToolLocalization.text(new LocalizationContext(
                    ReviewActivityWidget.class,
                    ImmutableMap.of("count", limit)), "pagination.previousCount"));
                page.writeEnd();
                page.writeEnd();
            }

            if (result.getOffset() > 0
                || result.hasNext()
                || result.getItems().size() > LIMITS[0]) {
                page.writeStart("li");
                page.writeStart("form",
                    "data-bsp-autosubmit", "",
                    "method", "get",
                    "action", page.url(null));
                page.writeStart("select", "name", "limit");
                for (int l : LIMITS) {
                    page.writeStart("option",
                        "value", l,
                        "selected", limit == l ? "selected" : null);
                    page.writeHtml(ToolLocalization.text(new LocalizationContext(
                        ReviewActivityWidget.class,
                        ImmutableMap.of("count", l)), "option.showCount"));
                    page.writeEnd();
                }
                page.writeEnd();
                page.writeEnd();
                page.writeEnd();
            }

            if (result.hasNext()) {
                page.writeStart("li", "class", "next");
                page.writeStart("a",
                    "title", ToolLocalization.text(new LocalizationContext(
                        ReviewActivityWidget.class,
                        ImmutableMap.of("count", limit)), "pagination.nextCount"),
                    "href", page.url("", "offset", result.getNextOffset()));
                page.writeHtml(ToolLocalization.text(new LocalizationContext(
                    ReviewActivityWidget.class,
                    ImmutableMap.of("count", limit)), "pagination.nextCount"));
                page.writeEnd();
                page.writeEnd();
            }

            page.writeEnd();

            page.writeStart("table", "class", "LinkList links table-striped pageThumbnails").writeStart("tbody");

            for (Object content : result.getItems()) {
                State contentState = State.getInstance(content);
                String permalink = contentState.as(Directory.ObjectModification.class).getPermalink();
                Content.ObjectModification contentData = contentState.as(Content.ObjectModification.class);
                DateTime updateDateTime = page.toUserDateTime(contentData.getUpdateDate());
                ToolUser updateUser = contentData.getUpdateUser();
                HasReviewCycle hasReviewCycle = contentState.as(HasReviewCycle.class);

                if (!contentState.isVisible()) {
                    permalink = JspUtils.getAbsolutePath(
                        page.getRequest(),
                        "/_preview",
                        "_cms.db.previewId",
                        contentState.getId());
                }

                page.writeStart("tr", "data-preview-url", permalink);
                page.writeStart("td", "class", "LinkList-image", "style", "display: none;");
                if (updateUser != null) {
                    page.writeRaw(updateUser.createAvatarHtml());
                }
                page.writeEnd();

                page.writeStart("td", "class", "date");
                page.writeHtml(page.formatUserDate(updateDateTime));
                page.writeEnd();

                page.writeStart("td", "class", "due");
                page.writeHtml(getDueOffsetLabel(hasReviewCycle));
                page.writeEnd();

                page.writeStart("td", "class", "time");
                page.writeHtml(page.formatUserTime(updateDateTime));
                page.writeEnd();

                page.writeStart("td", "class", "LinkList-type");
                page.writeTypeLabel(content);
                page.writeEnd();

                boolean typeHasFieldDisplayPreview = Optional.ofNullable(contentState.getType())
                    .filter(t -> !t.as(ToolUi.class).getFieldDisplayPreview().isEmpty())
                    .isPresent();

                page.writeStart("td", "class", "LinkList-main");
                page.writeStart("a",
                    "href", page.objectUrl("/content/edit.jsp", content),
                    "class", typeHasFieldDisplayPreview
                        ? "ContentSummary-link"
                        : null,
                    "target", "_top");
                page.writeObjectLabel(content);
                if (typeHasFieldDisplayPreview) {
                    page.writeStart(
                            "span",
                            "title",
                            ToolLocalization.text(ReviewActivityWidget.class, "title.name"),
                            "class",
                            "ContentSummary-info",
                            "data-summary-url",
                            new UrlBuilder(ContentSummary.class, p ->
                                p.setContent((Recordable) content))
                                .build())
                        .writeEnd();
                }
                page.writeEnd();
                page.writeEnd();

                page.writeStart("td");
                page.writeObjectLabel(updateUser);
                page.writeEnd();

                if (page.getCmsTool().isEnableViewers()) {
                    page.writeStart("td",
                        "class", "LinkList-viewers",
                        "data-rtc-content-id", contentState.getId().toString(),
                        "data-content-locking", page.getCmsTool().isContentLocking() ? "true" : null);
                    page.writeStart("div", "data-rtc-edit-field-update-viewers", "");
                    page.writeEnd();
                    page.writeEnd();
                }

                page.writeEnd();
            }

            page.writeEnd().writeEnd();
        }

        page.writeEnd();
    }

    private String getDueOffsetLabel(HasReviewCycle hasReviewCycle) {
        Date nextReviewDate = hasReviewCycle.as(ReviewCycleContentModification.class).getNextReviewDateIndex();
        if (nextReviewDate == null) {
            return ToolLocalization.text(
                ReviewActivityWidget.class,
                "label.missingNextReview",
                "Missing Next Review Date");
        }
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC")).truncatedTo(ChronoUnit.DAYS);

        long days = ChronoUnit.DAYS.between(now.toInstant(), nextReviewDate.toInstant());

        if (days >= 0) {
            // Due soon
            return ToolLocalization.text(new LocalizationContext(
                ReviewActivityWidget.class,
                ImmutableMap.of("days", days)), "label.dueSoon", "Due soon");
        } else {
            // past due
            return ToolLocalization.text(new LocalizationContext(
                ReviewActivityWidget.class,
                ImmutableMap.of("days", Math.abs(days))), "label.pastDue", "Past due");
        }
    }

    private enum DueTarget {

        DUE_SOON("duesoon", "Due Soon (15 Days)", 15L),
        PAST_DUE("pastdue", "Past Due", 0L);

        private final String targetKey;
        private final String targetLabel;
        private final Long daysOffset;

        DueTarget(String targetKey, String targetLabel, Long daysOffset) {
            this.targetKey = targetKey;
            this.targetLabel = targetLabel;
            this.daysOffset = daysOffset;
        }

        static final Map<String, DueTarget> TARGET_KEY_MAPPING = Arrays
            .stream(DueTarget.values())
            .collect(Collectors.toMap(
                DueTarget::getTargetKey,
                Function.identity()
            ));

        public String getTargetKey() {
            return targetKey;
        }

        public String getTargetLabel() {
            return targetLabel;
        }

        public Long getDaysOffset() {
            return daysOffset;
        }

        public Predicate getPredicate() {
            ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC")).truncatedTo(ChronoUnit.DAYS);

            if (this.getDaysOffset() == 0) {
                // Check if it was due before right now
                return PredicateParser.Static.parse(
                    ReviewCycleContentModification.NEXT_REVIEW_DATE_INDEX_FIELD_INTERNAL_NAME
                        + " != missing and "
                        + ReviewCycleContentModification.NEXT_REVIEW_DATE_INDEX_FIELD_INTERNAL_NAME
                        + " < ?", now.toInstant().toEpochMilli());
            } else {
                ZonedDateTime dueSoon = now.plusDays(this.getDaysOffset());
                // Check if it is due soon but after now
                return PredicateParser.Static.parse(
                    ReviewCycleContentModification.NEXT_REVIEW_DATE_INDEX_FIELD_INTERNAL_NAME
                        + " != missing and "
                        + ReviewCycleContentModification.NEXT_REVIEW_DATE_INDEX_FIELD_INTERNAL_NAME
                        + " >= ? and "
                        + ReviewCycleContentModification.NEXT_REVIEW_DATE_INDEX_FIELD_INTERNAL_NAME
                        + " < ?", now.toInstant().toEpochMilli(), dueSoon.toInstant().toEpochMilli());
            }

        }
    }
}
