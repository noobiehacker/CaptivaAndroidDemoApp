package emc.captiva.mobile.sdksampleapp.Presenter;
import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

import emc.captiva.mobile.sdksampleapp.BuildConfig;
import emc.captiva.mobile.sdksampleapp.Constant;
import emc.captiva.mobile.sdksampleapp.MainActivity;
import emc.captiva.mobile.sdksampleapp.Model.Cookie;
import emc.captiva.mobile.sdksampleapp.Model.FilterProfile;
import emc.captiva.mobile.sdksampleapp.Repository.CookieRepo;
import emc.captiva.mobile.sdksampleapp.Repository.FilterProfileRepo;
import io.realm.Realm;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

/**
 * Created by david on 9/9/16.
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
public class MainActivityPresenterTest{

    @Mock
    FilterProfileRepo filterRepo;
    MainActivityPresenter mainActivityPresenter;
    MainActivity activity;

    @Before
    public void setUp(){

        //1)Mock Up realm
        //2)Create Repo
        this.filterRepo = Mockito.mock(FilterProfileRepo.class);
        this.activity = Robolectric.buildActivity(MainActivity.class).get();
        this.mainActivityPresenter = new MainActivityPresenter(this.filterRepo, this.activity);

    }

    @Test
    public void testCheckLogInStatus() throws Exception {

        Cookie cookie = new Cookie();
        cookie.setCookie("A COOKIE");
        CookieRepo.sessionCookie = cookie;
        boolean result = mainActivityPresenter.userIsLoggedIn();
        Assert.assertTrue(result);

        CookieRepo.sessionCookie = null;
        result = mainActivityPresenter.userIsLoggedIn();
        Assert.assertFalse(result);

    }

    @Test
    public void testLoadProfile(){

        //1)Call Function in Presenter
        doNothing().when(filterRepo).readProfiles(Matchers.any(Realm.Transaction.OnSuccess.class),
                Matchers.any(Realm.Transaction.OnError.class));
        this.mainActivityPresenter.readProfileList(null,null);
        //2)Validate Repo function got called
        verify(this.filterRepo).readProfiles(Matchers.any(Realm.Transaction.OnSuccess.class),
                Matchers.any(Realm.Transaction.OnError.class));

    }

    @Test
    public void testGetStringFromProfileList(){

        List<FilterProfile> list = new ArrayList<FilterProfile>();
        int count = 10;
        for(int i =0 ; i< count ; i++){
            FilterProfile profile = new FilterProfile("ABCD");
            list.add(profile);
        }
        List<String> result = this.mainActivityPresenter.getStringFromProfileList(list);
        Assert.assertNotNull(result);
        Assert.assertEquals(result.size() , count);
    }

    @Test
    public void TestGetIdFromAdapterObjectSuccess(){

        //1)Create Object
        FilterProfile profile = new FilterProfile();
        int profileID = 123;
        profile.setProfileName("ABCD");
        profile.setId(profileID);
        Object object = profile;
        //2)Call Method
        int result = this.mainActivityPresenter.getIdFromAdapterObject(object);
        //3)Assert Result
        Assert.assertTrue(result != Constant.invalidId);
        Assert.assertEquals(profileID, result);

    }

    @Test
    public void TestGetIdFromAdapterObjectFail(){

        //1)Create Object
        FilterProfile profile = new FilterProfile();
        profile.setProfileName("Please select a profile");
        profile.setId(-124);
        Object object = profile;
        //2)Call Method
        int result = this.mainActivityPresenter.getIdFromAdapterObject(object);
        //3)Assert Result
        Assert.assertEquals(result, Constant.invalidId);

    }

    @Test
    public void TestGetListWithValidName(){

        //1)Create Object
        List<FilterProfile> profiles = new ArrayList<>();
        for(int i =0 ; i< 10 ; i++){
            profiles.add(new FilterProfile());
        }
        profiles.add(new FilterProfile("ValidName"));
        //2)Call Method
        List<FilterProfile> resultList = this.mainActivityPresenter.getListWithValidName(profiles);
        //3)Validate Result
        Assert.assertEquals(1 , resultList.size());
    }
}
