package brightspot.reviewcycle;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.psddev.cms.db.Content;
import com.psddev.cms.db.History;
import com.psddev.cms.db.Workflow;
import com.psddev.cms.ui.ToolRequest;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.State;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.RoutingFilter;
import com.psddev.dari.web.UrlBuilder;
import com.psddev.dari.web.WebRequest;
import org.apache.commons.lang3.StringUtils;

public final class Utils {
    private Utils() {

    }

    // Taken from brightspot.contentpublishutils.ContentPublishUtils
    static boolean isFirstPublish(Object object) {

        Preconditions.checkNotNull(object);

        State state = State.getInstance(object);

        Object existingObject = Query.fromAll().where("id = ?", state.getId()).noCache().first();

        State existingState = State.getInstance(existingObject);

        // this Object is already published if:
        //  - it already exists in the database
        //  - AND the database object is not in draft
        //  - AND the database object is not trashed
        //  - AND the database object is not in workflow
        boolean isAlreadyPublished = existingObject != null
            && !existingState.as(Content.ObjectModification.class).isDraft()
            && !existingState.as(Content.ObjectModification.class).isTrash()
            && existingState.as(Workflow.Data.class).getCurrentState() == null;

        // this Object is about to be published for the first time if:
        //  - this Object is not already published (above)
        //  - AND it is not in draft
        //  - AND it is not trashed
        //  - AND it is not in workflow
        return !isAlreadyPublished
            && !state.as(Content.ObjectModification.class).isDraft()
            && !state.as(Content.ObjectModification.class).isTrash()
            && state.as(Workflow.Data.class).getCurrentState() == null;
    }

    // Taken from com.psddev.dari.util.UnresolvedState.resolve
    static <T> T resolve(T object) {

        if (object == null) {
            return null;
        }

        State state = State.getInstance(object);

        if (state.isReferenceOnly()) {
            return (T) Query.fromAll().where("_id = ?", state.getId()).noCache().first();
        } else {
            return object;
        }
    }

    // Taken from RecordableUtils.computeDelta
    public static Map<String, Object> computeDelta(Object source, Object target) {
        return computeDelta(new History(null, source), new History(null, target));
    }

    // Taken from RecordableUtils.computeDelta
    public static Map<String, Object> computeDelta(History source, History target) {

        Map<String, Object> currentOriginals = source.getObjectOriginals();
        Map<String, Object> nextOriginals = target.getObjectOriginals();

        Set<String> currentKeys = new HashSet<>(currentOriginals.keySet());

        Map<String, Object> diffs = nextOriginals.keySet()
            .stream()
            .filter(key -> {
                currentKeys.remove(key);
                Object currentValue = currentOriginals.get(key);
                Object nextValue = nextOriginals.get(key);
                return !(ObjectUtils.isBlank(currentValue) && ObjectUtils.isBlank(nextValue))
                    && !ObjectUtils.equals(currentValue, nextValue);
            })
            .collect(Collectors.toMap(
                Function.identity(),
                nextOriginals::get
            ));

        currentKeys.forEach(emptyField ->
            diffs.put(emptyField, null)
        );

        return diffs;
    }

    public static UrlBuilder fullyQualifiedCmsUrlBuilder(String path) {

        UrlBuilder urlBuilder = null;

        if (WebRequest.isAvailable()) {
            urlBuilder = WebRequest.getCurrent().as(ToolRequest.class).getPathBuilder(path);
        }

        if (urlBuilder == null) {
            urlBuilder = new UrlBuilder(
                    RoutingFilter.Static.getApplicationPath("cms") + StringUtils.prependIfMissing(path, "/")
            );
        }

        return urlBuilder;
    }
}
