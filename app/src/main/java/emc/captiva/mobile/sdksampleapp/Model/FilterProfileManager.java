package emc.captiva.mobile.sdksampleapp.Model;

import android.util.Log;
import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmResults;

/**
 * Created by david on 9/1/16.
 */
public class FilterProfileManager {

    public static int defaultId = 0;
    public static  RealmResults<FilterProfile> profilesCache;
    public static Number maxIdCache = 0;

    //Write Profile To Disk
    public void createAndPersistProfile(final FilterProfile profile, final Realm realm,
                                        Realm.Transaction.OnSuccess onSuccess,
                                        Realm.Transaction.OnError onError){

        realm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm bgrealm) {
                FilterProfileManager.this.createFilterProfileSync(realm, profile);
            }
        },onSuccess,onError);

    }

    public void createFilterProfileSync(Realm bgrealm,final FilterProfile profile){

        //Persist Profile
        FilterProfile dbObj  = bgrealm.createObject(FilterProfile.class);
        dbObj.setAutoMaticallyApplyFilter(profile.isAutoMaticallyApplyFilter());
        dbObj.setId(profile.getId());
        dbObj.setFilters(profile.getFilters());

    }

    public void readListOfProfileSync(FilterProfile profile, final Realm realm,
                                  Realm.Transaction.OnSuccess onSuccess,
                                  Realm.Transaction.OnError onError){

        realm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm bgrealm) {
                FilterProfileManager.this.readListOfFilterProfileSync(realm);
            }
        },onSuccess,onError);

    }

    public void readListOfFilterProfileSync(Realm bgrealm){

        //Read list Of profile
        this.profilesCache = bgrealm.where(FilterProfile.class).findAll();

    }

    public void readMaxId(final Realm realm ,Realm.Transaction.OnSuccess onSuccess,
                          Realm.Transaction.OnError onError){

        realm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm bgrealm) {
                FilterProfileManager.this.readMaxIdSync(realm);
            }
        },onSuccess,onError);

    }

    public void readMaxIdSync(final Realm realm){

        //Read list Of profile
        this.maxIdCache = realm.where(FilterProfile.class).max("id");

    }

}
