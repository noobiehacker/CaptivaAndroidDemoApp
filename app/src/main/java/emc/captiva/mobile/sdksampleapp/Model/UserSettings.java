package emc.captiva.mobile.sdksampleapp.Model;

import io.realm.RealmList;
import io.realm.RealmObject;

/**
 * Created by david on 9/1/16.
 */
public class UserSettings extends RealmObject {

    private boolean autoMaticallyApplyFilter;
    private int id;
    private RealmList<Filter> filters;

    public boolean isAutoMaticallyApplyFilter() {
        return autoMaticallyApplyFilter;
    }

    public void setAutoMaticallyApplyFilter(boolean autoMaticallyApplyFilter) {
        this.autoMaticallyApplyFilter = autoMaticallyApplyFilter;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public RealmList<Filter> getFilters() {
        return filters;
    }

    public void setFilters(RealmList<Filter> filters) {
        this.filters = filters;
    }
}
