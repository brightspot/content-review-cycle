package brightspot.reviewcycle;

/**
 * Enum used in {@link brightspot.reviewcycle.notification.ReviewCycleDueWarningDuration}
 * and {@link ReviewCycleDurationForContent} for distinction between days, months, and years.
 */
public enum NotificationInterval {

    HOUR("Every hour"),
    DAY("Every day"),
    MINUTE("Every minute");

    private final String interval;

    NotificationInterval(String interval) {
        this.interval = interval;
    }

    public String getInterval() {
        return interval;
    }

    @Override
    public String toString() {
        return getInterval();
    }
}
