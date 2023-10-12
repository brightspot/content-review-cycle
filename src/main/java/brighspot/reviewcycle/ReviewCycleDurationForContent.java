package brightspot.reviewcycle;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import com.psddev.dari.db.Record;

public class ReviewCycleDurationForContent extends Record implements ReviewCycleDuration {

    private CalendarField calendarField;
    private int calendarFieldCount;

    public ReviewCycleDurationForContent() {
    }

    ReviewCycleDurationForContent(CalendarField calendarField, int calendarFieldCount) {
        this.calendarField = calendarField;
        this.calendarFieldCount = calendarFieldCount;
    }

    public CalendarField getCalendarField() {
        return calendarField;
    }

    @Override
    public int getCalendarFieldCount() {
        return calendarFieldCount;
    }

    public Date subtractCycleDuration(Date now) {
        if (now == null) {
            now = new Date();
        }
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(now);
        calendar.add(this.getCalendarField().getCalendarField(), -1 * this.getCalendarFieldCount());
        return calendar.getTime();
    }

    public Date addCycleDuration(Date lastDue) {
        if (lastDue == null) {
            lastDue = new Date();
        }
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(lastDue);
        calendar.add(this.getCalendarField().getCalendarField(), this.getCalendarFieldCount());
        return calendar.getTime();
    }

    @Override
    public String getLabel() {
       return "Every " + getCalendarFieldCount() + " " + getCalendarField().toString();
    }
}
