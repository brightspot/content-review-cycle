package brightspot.reviewcycle.task;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import brightspot.reviewcycle.ReviewCycleContentModification;
import brightspot.reviewcycle.ReviewCycleContentTypeMap;
import brightspot.reviewcycle.ReviewCycleDurationForContent;
import brightspot.reviewcycle.ReviewCycleSiteSettings;
import brightspot.reviewcycle.notification.ReviewCycleDueNotification;
import brightspot.reviewcycle.notification.ReviewCycleDueWarningDuration;
import com.psddev.cms.db.Content;
import com.psddev.cms.db.Site;
import com.psddev.cms.db.SiteSettings;
import com.psddev.dari.db.CompoundPredicate;
import com.psddev.dari.db.Predicate;
import com.psddev.dari.db.PredicateParser;
import com.psddev.dari.db.Query;
import com.psddev.dari.util.RepeatingTask;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This ReviewCycleDueRepeatingTask publishes notifications of content that is due or has a warning, both overriden
 * and non-overriden content. This task is set to repeat once per day.
 */
public class ReviewCycleDueRepeatingTask extends RepeatingTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReviewCycleDueRepeatingTask.class);

    private Site site;

    public Site getSite() {
        return site;
    }

    public void setSite(Site site) {
        this.site = site;
    }

    @Override
    public void doRepeatingTask(DateTime runTime) {

        List<Site> sites = Query.from(Site.class).selectAll();

        for (Site value : sites) {

            List<ReviewCycleContentTypeMap> contentMaps = SiteSettings.get(
                    value,
                    siteSettings -> siteSettings.as(ReviewCycleSiteSettings.class).getContentTypeMaps());

            List<ReviewCycleDurationForContent> durations = new ArrayList<>();
            for (ReviewCycleContentTypeMap contentMap : contentMaps) {
                durations.add(contentMap.getCycleDuration());
            }

            List<ReviewCycleDueWarningDuration> dueWarnings = new ArrayList<>();
            for (ReviewCycleContentTypeMap contentMap : contentMaps) {
                dueWarnings.add(contentMap.getDueWarningDuration());
            }

            LOGGER.info("ContentMaps size: " + contentMaps.size());

            String group = "";
            ReviewCycleDurationForContent cycleDuration;

            Predicate dueNowOrWarningPredicate;

            // Time of now, truncated to days
            Date now = Date.from(new Date().toInstant().truncatedTo(ChronoUnit.DAYS));

            // Handle each Type mapped in Sites & Settings
            for (ReviewCycleContentTypeMap map : contentMaps) {
                group = map.getContentType().getInternalName();
                cycleDuration = map.getCycleDuration();

                LOGGER.info(group + " " + cycleDuration.toString());

                dueNowOrWarningPredicate = CompoundPredicate.combine(
                        PredicateParser.OR_OPERATOR,
                        map.getExpiredPredicate(now),
                        ReviewCycleDueWarningDuration.getDueWarningPredicate(now, dueWarnings));

                // Check for content (that is due or has a warning today) that does not have overrides
                List<Content> contents = Query.from(Content.class)
                        .where(map.getTypePredicate())
                        .and(dueNowOrWarningPredicate)
                        .and(ReviewCycleContentModification.REVIEW_CYCLE_DURATION_FIELD_INTERNAL_NAME + " = missing")
                        .and(getSitePredicate())
                        .selectAll();

                LOGGER.info("Contents size: " + contents.size());

                this.publishNotifications(contents);
            }

            List<Content> overridesList = new ArrayList<>();

            LOGGER.info("Searching cycle overrides configured content...");
            // Handle cycle overrides

            for (ReviewCycleDurationForContent duration : durations) {

                // Generate predicate
                Predicate datePredicate = PredicateParser.Static.parse(
                        ReviewCycleContentModification.NEXT_REVIEW_DATE_INDEX_FIELD_INTERNAL_NAME
                                + " != missing and "
                                + ReviewCycleContentModification.NEXT_REVIEW_DATE_INDEX_FIELD_INTERNAL_NAME
                                + " < ?", now.getTime());

                dueNowOrWarningPredicate = CompoundPredicate.combine(
                        PredicateParser.OR_OPERATOR,
                        datePredicate,
                        ReviewCycleDueWarningDuration.getDueWarningPredicate(now, dueWarnings));

                // Search for all content where the override is not missing and the date for this duration is due
                overridesList.addAll(Query.from(Content.class)
                        .where(ReviewCycleContentModification.REVIEW_CYCLE_DURATION_FIELD_INTERNAL_NAME + " != missing")
                        .and(ReviewCycleContentModification.REVIEW_CYCLE_DURATION_FIELD_INTERNAL_NAME + " = ?", duration)
                        .and(dueNowOrWarningPredicate)
                        .and(getSitePredicate())
                        .selectAll());
            }

            LOGGER.info("Content overrides size: " + overridesList.size());

            this.publishNotifications(overridesList);
        }

    }

    /**
     * Takes in a list of content that has been queried as being due. Calculate next due and publish notification for
     * content.
     *
     * @param contentList list of content queried as being due.
     */
    private void publishNotifications(List<Content> contentList) {
        Date nextDue;
        for (Content content : contentList) {
            LOGGER.info("Sending Notification for " + content.getLabel());
            // Calculate date
            nextDue = content
                .as(ReviewCycleContentModification.class)
                .getNextReviewDateIndex();
            new ReviewCycleDueNotification(
                content.getLabel(),
                content.getId(),
                nextDue,
                content.as(Site.ObjectModification.class).getOwner().getName()).publish();
        }
    }

    /**
     * Returns a predicate to ensure that global content is not checked for ReviewCycle Controls and to only check
     * content owned by the site for which the task is running.
     *
     * @return predicate to check proper site ownership
     */
    private Predicate getSitePredicate() {

        Predicate validSites = PredicateParser.Static.parse("cms.site.owner != missing");

        if (getSite() != null) {
            return CompoundPredicate.combine(
                PredicateParser.AND_OPERATOR,
                validSites,
                PredicateParser.Static.parse("cms.site.owner = ?", getSite()));

        } else {
            return validSites;
        }
    }

    @Override
    protected DateTime calculateRunTime(DateTime currentTime) {
        return everyMinute(currentTime);
    }

}