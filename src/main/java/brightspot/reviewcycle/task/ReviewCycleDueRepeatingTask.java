package brightspot.reviewcycle.task;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import brightspot.reviewcycle.ReviewCycleContentModification;
import brightspot.reviewcycle.ReviewCycleContentTypeMap;
import brightspot.reviewcycle.ReviewCycleDurationForContent;
import brightspot.reviewcycle.ReviewCycleSiteSettings;
import brightspot.reviewcycle.notification.ReviewCycleDueNotification;
import brightspot.reviewcycle.notification.ReviewCycleDueWarningDuration;
import brightspot.reviewcycle.notification.ReviewCycleNotificationBundle;
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
 * and non-overriden content.
 * The method {@link #calculateRunTime(DateTime)} sets how often this task should be repeated.
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

    /**
     * Repeating task to send notifications
     */
    @Override
    public void doRepeatingTask(DateTime runTime) {

        List<Site> sites = Query.from(Site.class).selectAll();

        for (Site value : sites) {

            List<ReviewCycleContentTypeMap> contentMaps = SiteSettings.get(
                    value,
                    siteSettings -> siteSettings.as(ReviewCycleSiteSettings.class).getSettings().getContentTypeMaps());

            List<ReviewCycleDueWarningDuration> notificationWarningTimes = SiteSettings.get(
                    value,
                    siteSettings -> siteSettings.as(ReviewCycleSiteSettings.class).getSettings().getNotificationWarningTimes());

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
                        ReviewCycleDueWarningDuration.getDueWarningPredicate(now, notificationWarningTimes));

                // Check for content (that is due or has a warning today) that does not have overrides
                List<Content> contents = Query.from(Content.class)
                        .where(map.getTypePredicate())
                        .and(dueNowOrWarningPredicate)
                        .and(ReviewCycleContentModification.REVIEW_CYCLE_DURATION_FIELD_INTERNAL_NAME + " = missing")
                        .and(getSitePredicate())
                        .selectAll();

                LOGGER.info("Contents size: " + contents.size());

                // Prevents duplication of notification records
                dedupeNotificationRecords(contents);
            }

            List<Content> overridesList = new ArrayList<>();

            LOGGER.info("Searching cycle overrides configured content...");

            // Handle cycle overrides

            List<ReviewCycleDurationForContent> durations = Query.from(ReviewCycleDurationForContent.class).selectAll();

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
                        ReviewCycleDueWarningDuration.getDueWarningPredicate(now, notificationWarningTimes));

                // Search for all content where the override is not missing and the date for this duration is due
                overridesList.addAll(Query.from(Content.class)
                        .where(ReviewCycleContentModification.REVIEW_CYCLE_DURATION_FIELD_INTERNAL_NAME + " != missing")
                        .and(ReviewCycleContentModification.REVIEW_CYCLE_DURATION_FIELD_INTERNAL_NAME + " = ?", duration)
                        .and(dueNowOrWarningPredicate)
                        .and(getSitePredicate())
                        .selectAll());
            }

            LOGGER.info("Content overrides size: " + overridesList.size());

            // Prevents duplication of notification records
            dedupeNotificationRecords(overridesList);
        }

    }

    /**
     * Prevents duplicate notification records of content by checking if it has already been published initially
     *
     * @param contentList list of content needed to be published or updated.
     */
    public void dedupeNotificationRecords(List<Content> contentList) {

        // Get contentList ids
        List<UUID> overridesListIds = contentList.stream().map(Content::getId).collect(Collectors.toList());

        List<ReviewCycleDueNotification> reviewCycleDueNotifications = new ArrayList<>();

        // Get all notifications from the content ids
        for (UUID overridesListId : overridesListIds) {
            ReviewCycleDueNotification notification = Query.from(ReviewCycleDueNotification.class)
                    .where("getContentId = ?", overridesListId)
                    .first();

            if (notification != null) {
                reviewCycleDueNotifications.add(notification);
            }
        }

        // We want to send out notifications ONCE daily
        long now = Instant.now().toEpochMilli();
        long interval = 86400000;

        /* If there are more than 0 notifications that have been sent out today, we only update notification records
            not creating new ones (until the next day), because that means publishedNotifications has been called.
         */
        boolean notificationsSentOutToday = Query.from(ReviewCycleDueNotification.class)
                .where("publishedAt = ?", now - interval)
                .hasMoreThan(0);

        if (notificationsSentOutToday) {
            updateNotificationRecordsPerDay(reviewCycleDueNotifications);
        } else {
            // Else if there are 0 that have been
            publishNotifications(contentList);
        }
    }

    /**
     * Takes in a list of notifications and modifies them by setting their last notified date to current date time.
     *
     * @param notifications list of notifications.
     */
    private void updateNotificationRecordsPerDay(List<ReviewCycleDueNotification> notifications) {
        for (ReviewCycleDueNotification notification : notifications) {
            Date nextDue;
            ReviewCycleNotificationBundle reviewCycleNotificationBundle = notification.getBundle();
            if (reviewCycleNotificationBundle != null) {
                LOGGER.info("Updating Notification for " + reviewCycleNotificationBundle.getContentLabel());
                // Calculate date
                if (reviewCycleNotificationBundle.getContent() != null) {
                    nextDue = reviewCycleNotificationBundle.getContent()
                            .as(ReviewCycleContentModification.class)
                            .getNextReviewDateIndex();
                    notification.getBundle().setDueDate(nextDue);

                }
                notification.getBundle().setLastNotified(new Date());
                notification.getBundle().saveImmediately();
                notification.publish();
            }
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
                    content,
                    content.getLabel(),
                    content.getId(),
                    new Date(),
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