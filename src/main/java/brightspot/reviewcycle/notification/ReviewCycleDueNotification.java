package brightspot.reviewcycle.notification;

import java.util.Date;
import java.util.UUID;

import com.psddev.cms.db.Content;
import com.psddev.dari.db.Query;
import com.psddev.dari.notification.Notification;
import com.psddev.dari.notification.Subscriber;
import com.psddev.watch.WatcherObjectModification;

/**
 * Used in {@link brightspot.reviewcycle.task.ReviewCycleDueRepeatingTask} for publishing notifications
 */
public class ReviewCycleDueNotification extends Notification<ReviewCycleDueSubscription, ReviewCycleNotificationBundle> {

    private ReviewCycleNotificationBundle bundle;

    public ReviewCycleNotificationBundle getBundle() {
        return bundle;
    }

    public void setBundle(ReviewCycleNotificationBundle bundle) {
        this.bundle = bundle;
    }

    public ReviewCycleDueNotification() {
        super();
    }

    public ReviewCycleDueNotification(ReviewCycleNotificationBundle bundle) {
        super();
        this.bundle = bundle;
        setPayload(bundle);
    }

    @Indexed
    public UUID getContentId() {

        if (bundle != null) {
            return bundle.getContentId();
        }
        return null;
    }

    public ReviewCycleDueNotification(Content content, String contentLabel, UUID contentId, Date lastNotified, Date dueDate, String ownerName) {
        super();
        this.bundle = new ReviewCycleNotificationBundle(content, contentLabel, contentId, lastNotified, dueDate, ownerName);
        setPayload(bundle);
    }

    @Override
    protected Iterable<? extends Subscriber> getSubscribers() {
        // Return the watchers of this payload
        return Query
            .from(Content.class)
            .where("_id = ?", getPayload().getContentId())
            .first().as(WatcherObjectModification.class).getWatchers();
    }
}
