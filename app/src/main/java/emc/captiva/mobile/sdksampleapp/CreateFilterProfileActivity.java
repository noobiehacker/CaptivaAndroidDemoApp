package emc.captiva.mobile.sdksampleapp;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

import emc.captiva.mobile.sdksampleapp.Activity.BaseActivity;
import emc.captiva.mobile.sdksampleapp.ListAdapter.AvailableFilterListAdapter;
import emc.captiva.mobile.sdksampleapp.ListAdapter.SelectedFilterListAdapter;
import emc.captiva.mobile.sdksampleapp.ListItem.FilterListItem;
import emc.captiva.mobile.sdksampleapp.Model.Filter;
import emc.captiva.mobile.sdksampleapp.Model.FilterProfile;
import emc.captiva.mobile.sdksampleapp.Repository.FilterProfileRepo;
import emc.captiva.mobile.sdksampleapp.Presenter.CreateProfilePresenter;
import emc.captiva.mobile.sdksampleapp.Util.RealmUtil;
import emc.captiva.mobile.sdksampleapp.Util.UIUtils;
import emc.captiva.mobile.sdksampleapp.View.CreateProfileView;
import emc.captiva.mobile.sdksampleapp.View.AvailableFilterListListener;
import emc.captiva.mobile.sdksampleapp.View.SelectedListClickedListener;
import io.realm.Realm;
import io.realm.RealmList;

/**
 * Created by david on 9/2/16.
 */
public class CreateFilterProfileActivity extends BaseActivity implements CreateProfileView, AvailableFilterListListener, SelectedListClickedListener, TextWatcher {

    private String action = "Create Profile";
    private CreateProfilePresenter presenter;
    private AvailableFilterListAdapter availableFilterListAdapter;
    private SelectedFilterListAdapter selectedFilterListAdapter;
    private boolean autoApplyFilter = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_filter_profile);
        ListView availableListView = (ListView)findViewById(R.id.filterListView);
        ListView selectedListView = (ListView)findViewById(R.id.selectedFilterListView);
        this.availableFilterListAdapter = createAvailableFilterList(getResources().getStringArray(R.array.Filter_List), availableListView);
        this.selectedFilterListAdapter = createSelectedFilterList( selectedListView);
        this.presenter = new CreateProfilePresenter(this, new FilterProfileRepo(),this);
        EditText editText = (EditText)findViewById(R.id.createProfileNameInput);
        editText.addTextChangedListener(this);
    }


    @Override
    protected void onResume() {
        super.onResume();
        this.hideKeyboard();
    }

    private Realm.Transaction.OnSuccess createReadSuccessCallBack() {
        return new Realm.Transaction.OnSuccess() {
            @Override
            public void onSuccess() {
                CreateFilterProfileActivity.this
                        .displayCustomToast(CreateFilterProfileActivity.this.action,
                                "Succeed", "Profile Saved");
            }
        };
    }

    private Realm.Transaction.OnError createReadErrorCallBack() {
        return new Realm.Transaction.OnError(){
            @Override
            public void onError(Throwable error) {
                CreateFilterProfileActivity.this
                        .displayCustomToast(CreateFilterProfileActivity.this.action,
                                "Failed", error.getMessage());
            }
        };
    }

    private AvailableFilterListAdapter createAvailableFilterList(String[] array, ListView listView){

        List<FilterListItem> listItems = new UIUtils().initializeFilterListView(array);
        AvailableFilterListAdapter adapter = new AvailableFilterListAdapter(this,  listItems, this);
        listView.setAdapter(adapter);
        return adapter;

    }

    private SelectedFilterListAdapter createSelectedFilterList(ListView listView){

        List<FilterListItem> listItems = new ArrayList<FilterListItem>();
        SelectedFilterListAdapter adapter = new SelectedFilterListAdapter(this, listItems, this);
        listView.setAdapter(adapter);
        return adapter;

    }

    @Override
    public String getProfileName(TextView textView) {
        return textView.getText().toString();
    }

    @Override
    public FilterProfile createFilterProfile(String profileName, List<FilterListItem> items, boolean applyFilterAuto) {
        FilterProfile profile = new FilterProfile();
        profile.setProfileName(profileName);
        profile.setAutoMaticallyApplyFilter(applyFilterAuto);
        RealmList<Filter> filters = new RealmList<Filter>();
        for(FilterListItem item: items){
            filters.add(item.filter);
        }
        profile.setFilters(filters);
        return profile;
    }

    public void onSaveButtonClicked(View view){

        if(filterNameIsSet() == false){
            this.displayCustomToast("Create Profile" , "Failed" , "Filter Name Not Set");
            return;
        }
        if(atLeastOneFilterSelected() == false){
            this.displayCustomToast("Create Profile" , "Failed" , "No Filter Item Selected");
            return;
        }
        TextView textView = (TextView) findViewById(R.id.createProfileNameInput);
        List<FilterListItem> items = this.selectedFilterListAdapter.getItems();
        FilterProfile profile = createFilterProfile(getProfileName(textView),
                items, this.autoApplyFilter);

        this.callPresenterToCreateProfile(profile,getRealmInstance());
    }

    public void callPresenterToCreateProfile(FilterProfile profile, Realm realm){

        //Todo Refactor
        if (this.presenter != null) {
            this.presenter.onCreateProfile(profile,createReadSuccessCallBack(),createReadErrorCallBack(), realm);
        }

    }

    private void displayCustomToast(String action , String result, String description) {

        new UIUtils().createAlertDialog(this, action,result,description);

    }

    private Realm getRealmInstance(){

        return new RealmUtil().createRealm(this);

    }

    private boolean filterNameIsSet(){

        View view = findViewById(R.id.createProfileNameInput);
        String defaultString = getString(R.string.CreateProfilePage_ProfileNameInput);
        if(view !=null){
            EditText editText = (EditText) view;
            return this.presenter.filterNameIsSet(editText , defaultString);
        }
        return false;
    }

    private boolean atLeastOneFilterSelected(){

        List<FilterListItem> items = this.selectedFilterListAdapter.getItems();
        return this.presenter.atLeastOneFilterSelected(items);

    }

    public void setPresenter(CreateProfilePresenter presenter) {
        this.presenter = presenter;
    }

    public boolean isAutoApplyFilter() {
        return autoApplyFilter;
    }

    public void onToggleClicked(View view) {

        this.autoApplyFilter = !this.autoApplyFilter;

    }

    @Override
    public void selectedListOnClick(FilterListItem item) {

        this.selectedFilterListAdapter.removeItemFromListView(item);

    }

    @Override
    public void availableFilterListOnClick(FilterListItem item) {

        if(!this.selectedFilterListAdapter.listContainsItem(item))
            this.selectedFilterListAdapter.addItemToListView(item);

    }

    public void setSelectedFilterListAdapter(SelectedFilterListAdapter selectedFilterListAdapter) {
        this.selectedFilterListAdapter = selectedFilterListAdapter;
    }

    private void hideKeyboard(){
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        //Do nothing

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

        //Do nothing

    }

    @Override
    public void afterTextChanged(Editable s) {

        if(this.presenter.endsWithNewLine(s)){
            this.presenter.takeOutNewLineAtEnd(s);
            this.hideKeyboard();
        }

    }

}
