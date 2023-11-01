package brightspot.reviewcycle.notification;

import java.util.Date;
import java.util.UUID;

import com.psddev.dari.db.Record;
import com.psddev.dari.db.Recordable;

@Recordable.Embedded
public class ReviewCycleNotificationBundle extends Record {

    public ReviewCycleNotificationBundle() {
    }

    public ReviewCycleNotificationBundle(String contentLabel, UUID contentId, Date lastRunDate, Date dueDate, String ownerName) {
        this.contentLabel = contentLabel;
        this.contentId = contentId;
        this.lastRunDate = lastRunDate;
        this.dueDate = dueDate;
        this.ownerName = ownerName;
    }

    private String contentLabel;

    private UUID contentId;

    private Date lastRunDate;

    private Date dueDate;

    private String ownerName;

    public String getContentLabel() {
        return contentLabel;
    }

    public void setContentLabel(String contentLabel) {
        this.contentLabel = contentLabel;
    }

    public UUID getContentId() {
        return contentId;
    }

    public void setContentId(UUID contentId) {
        this.contentId = contentId;
    }

    public Date getLastRunDate() {
        return lastRunDate;
    }

    public void setLastRunDate(Date lastRunDate) {
        this.lastRunDate = lastRunDate;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }
}
