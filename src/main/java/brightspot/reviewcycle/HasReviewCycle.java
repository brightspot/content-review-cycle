package brightspot.reviewcycle;

import com.psddev.dari.db.Recordable;

/**
 * Marker interface. Define which contents are selectable for review cycle in Sites &amp; Settings and have the modification
 */
public interface HasReviewCycle extends Recordable {

    String INTERNAL_NAME = "brightspot.reviewcycle.HasReviewCycle";
}
