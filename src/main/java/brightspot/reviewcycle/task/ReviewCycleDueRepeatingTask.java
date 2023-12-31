package brightspot.reviewcycle.task;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import brightspot.reviewcycle.ReviewCycleContentModification;
import brightspot.reviewcycle.ReviewCycleContentTypeMap;
import brightspot.reviewcycle.ReviewCycleDurationForContent;
import brightspot.reviewcycle.ReviewCycleSettings;
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

            ReviewCycleSettings settings = SiteSettings.get(
                    value,
                    siteSettings -> siteSettings.as(ReviewCycleSiteSettings.class).getSettings());

            // If settings is disabled here, check the next site
            if (settings == null) {
                continue;
            }

            List<ReviewCycleContentTypeMap> contentMaps = SiteSettings.get(
                    value,
                    siteSettings -> settings.getContentTypeMaps());

            List<ReviewCycleDueWarningDuration> notificationWarningTimes = SiteSettings.get(
                    value,
                    siteSettings -> settings.getNotificationWarningTimes());

            Predicate dueNowOrWarningPredicate;

            // Time of now, truncated to days
            Date now = Date.from(new Date().toInstant().truncatedTo(ChronoUnit.DAYS));

            // Handle each Type mapped in Sites & Settings
            for (ReviewCycleContentTypeMap map : contentMaps) {
                dueNowOrWarningPredicate = CompoundPredicate.combine(
                        PredicateParser.OR_OPERATOR,
                        map.getExpiredPredicate(now),
                        ReviewCycleDueWarningDuration.getDueWarningPredicate(now, notificationWarningTimes));

                // Check for content (that is due or has a warning today) that does not have overrides
                List<Content> contents = new ArrayList<>();

                Query.from(Content.class)
                        .where(map.getTypePredicate())
                        .and(dueNowOrWarningPredicate)
                        .and(ReviewCycleContentModification.REVIEW_CYCLE_DURATION_FIELD_INTERNAL_NAME + " = missing")
                        .and(getSitePredicate())
                        .iterable(10).forEach(contents::add);

                // Prevents duplication of notification records
                dedupeNotificationRecords(contents);
            }

            List<Content> overridesList = new ArrayList<>();

            // Handle cycle overrides

            List<ReviewCycleDurationForContent> durations = Query.from(ReviewCycleDurationForContent.class).selectAll();

            if (durations.size() > 0) {
                for (ReviewCycleDurationForContent duration : durations) {
                    // Generate predicate
                    Predicate datePredicate = PredicateParser.Static.parse(
                            ReviewCycleContentModification.NEXT_REVIEW_DATE_FIELD_INTERNAL_NAME
                                    + " != missing and "
                                    + ReviewCycleContentModification.NEXT_REVIEW_DATE_FIELD_INTERNAL_NAME
                                    + " < ?", now.getTime());

                    dueNowOrWarningPredicate = CompoundPredicate.combine(
                            PredicateParser.OR_OPERATOR,
                            datePredicate,
                            ReviewCycleDueWarningDuration.getDueWarningPredicate(now, notificationWarningTimes));

                    // Search for all content where the override is not missing and the date for this duration is due
                    Query.from(Content.class)
                            .where(ReviewCycleContentModification.REVIEW_CYCLE_DURATION_FIELD_INTERNAL_NAME + " != missing")
                            .and(ReviewCycleContentModification.REVIEW_CYCLE_DURATION_FIELD_INTERNAL_NAME + " = ?", duration)
                            .and(dueNowOrWarningPredicate)
                            .and(getSitePredicate())
                            .iterable(10)
                            .forEach(overridesList::add);
                }

            } else {
                /* If there are no notification durations configured in sites & settings, conditional will arrive here
                 * to catch anything that is past due
                 */
                for (ReviewCycleContentTypeMap contentMap : contentMaps) {
                    Predicate datePredicate = PredicateParser.Static.parse(
                            ReviewCycleContentModification.NEXT_REVIEW_DATE_FIELD_INTERNAL_NAME
                                    + " != missing and "
                                    + ReviewCycleContentModification.NEXT_REVIEW_DATE_FIELD_INTERNAL_NAME
                                    + " < ?", now.getTime());

                    Query.from(Content.class)
                            .where(contentMap.getTypePredicate())
                            .and(ReviewCycleContentModification.REVIEW_CYCLE_DURATION_FIELD_INTERNAL_NAME + " != missing")
                            .and(datePredicate)
                            .and(getSitePredicate())
                            .iterable(10)
                            .forEach(overridesList::add);
                }
            }

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

        // We want to send out notifications ONCE daily AND to the latest notifications
        Calendar c = new GregorianCalendar();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        Long d1 = c.getTime().toInstant().toEpochMilli();

        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        c.set(Calendar.MILLISECOND, 999);

        Long d2 = c.getTime().toInstant().toEpochMilli();

        // Get all notifications from the content ids
        for (int i = 0; i < overridesListIds.size(); i++) {

            ReviewCycleDueNotification notification = Query.from(ReviewCycleDueNotification.class)
                    .where("getContentId = ?", overridesListIds.get(i))
                    .and("publishedAt >= ?", d1)
                    .first();

            if (notification != null) {
                reviewCycleDueNotifications.add(notification);
            } else {
                // If notification is null, meaning it has never been updated today, publish it.
                publishNotifications(Collections.singletonList(contentList.get(i)));
            }
        }

        /* If there are more than 0 notifications that have been sent out today, we only update notification records
            not creating new ones (until the next day), because that means publishedNotifications has been called.
         */
        boolean notificationsSentOutToday = Query.from(ReviewCycleDueNotification.class)
                .where("publishedAt >= ?", d1)
                .and("publishedAt <= ?", d2)
                .hasMoreThan(0);

        if (notificationsSentOutToday) {
            updateNotificationRecordsPerDay(reviewCycleDueNotifications);
        } else {
            // Else if there are 0 that have been sent out
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
                // Calculate date
                if (reviewCycleNotificationBundle.getContent() != null) {
                    nextDue = reviewCycleNotificationBundle.getContent()
                            .as(ReviewCycleContentModification.class)
                            .getNextReviewDate();
                    notification.getBundle().setDueDate(nextDue);

                }
                notification.getBundle().setLastNotified(new Date());
                notification.saveImmediately();
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
            // Calculate date
            nextDue = content
                    .as(ReviewCycleContentModification.class)
                    .getNextReviewDate();

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

        if (!DefaultTaskHost.isRunningOnTaskHost()) {
            // This will never run since this is in the future
            return currentTime.plusMinutes(2);
        }

        return everyMinute(currentTime);
    }

}