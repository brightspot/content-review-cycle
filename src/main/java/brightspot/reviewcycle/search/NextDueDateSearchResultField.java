package brightspot.reviewcycle.search;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Optional;

import brightspot.reviewcycle.HasReviewCycle;
import brightspot.reviewcycle.ReviewCycleContentModification;
import com.psddev.cms.tool.SearchResultField;
import com.psddev.cms.ui.ToolLocalization;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Record;

public class NextDueDateSearchResultField implements SearchResultField {

    //Thu, Nov 18, 2021, 4:29 PM
    private static final DateFormat FORMAT = new SimpleDateFormat("EEE, MMM dd, yyyy, hh:mm aa ");

    @Override
    public String getDisplayName() {
        return "Next Review Date";
    }

    @Override
    public boolean isSupported(ObjectType objectType) {
        return objectType != null && objectType.isInstantiableTo(HasReviewCycle.class);
    }

    @Override
    public boolean isDefault(ObjectType objectType) {
        return objectType != null && objectType.isInstantiableTo(HasReviewCycle.class);
    }

    @Override
    public String createDataCellText(Object item) {
        return Optional.ofNullable(item)
            .filter(Record.class::isInstance)
            .map(Record.class::cast)
            .filter(HasReviewCycle.class::isInstance)
            .map(record -> record.as(HasReviewCycle.class))
            .map(hasReviewCycle -> hasReviewCycle.as(ReviewCycleContentModification.class))
            .map(ReviewCycleContentModification::getNextReviewDate)
            .map(date -> ToolLocalization.dateTime(date.getTime()))
            .orElse("");
    }
}
