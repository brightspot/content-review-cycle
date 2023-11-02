package brightspot.reviewcycle.notification;

import java.util.Date;
import java.util.UUID;

import com.psddev.cms.db.Content;
import com.psddev.dari.db.Record;
import com.psddev.dari.db.Recordable;

@Recordable.Embedded
public class ReviewCycleNotificationBundle extends Record {

    public ReviewCycleNotificationBundle() {
    }

    public ReviewCycleNotificationBundle(Content content, String contentLabel, UUID contentId, Date lastNotified, Date dueDate, String ownerName) {
        this.content = content;
        this.contentLabel = contentLabel;
        this.contentId = contentId;
        this.lastNotified = lastNotified;
        this.dueDate = dueDate;
        this.ownerName = ownerName;
    }

    private Content content;

    private String contentLabel;

    private UUID contentId;

    private Date lastNotified;

    private Date dueDate;

    private String ownerName;

    public Content getContent() {
        return content;
    }

    public void setContent(Content content) {
        this.content = content;
    }

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

    public Date getLastNotified() {
        return lastNotified;
    }

    public void setLastNotified(Date lastNotified) {
        this.lastNotified = lastNotified;
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
