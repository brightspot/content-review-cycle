package brightspot.reviewcycle.notification;

import java.util.Date;
import java.util.UUID;

import com.psddev.dari.db.Record;
import com.psddev.dari.db.Recordable;

@Recordable.Embedded
public class ReviewCycleNotificationBundle extends Record {

    public ReviewCycleNotificationBundle() {
    }

    public ReviewCycleNotificationBundle(String contentLabel, UUID contentId, Date dueDate, String ownerName) {
        this.contentLabel = contentLabel;
        this.contentId = contentId;
        this.dueDate = dueDate;
        this.ownerName = ownerName;
    }

    private String contentLabel;

    private UUID contentId;

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
