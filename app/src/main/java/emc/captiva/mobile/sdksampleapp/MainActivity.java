/** -------------------------------------------------------------------------
 * Copyright 2013-2015 EMC Corporation.  All rights reserved.
 ---------------------------------------------------------------------------- */

package emc.captiva.mobile.sdksampleapp;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;

import emc.captiva.mobile.sdk.CaptureException;
import emc.captiva.mobile.sdk.CaptureImage;
import emc.captiva.mobile.sdk.CaptureWindow;
import emc.captiva.mobile.sdk.PictureCallback;
import emc.captiva.mobile.sdk.ContinuousCaptureCallback;
import emc.captiva.mobile.sdksampleapp.JsonPojo.ImageUploadObj;
import emc.captiva.mobile.sdksampleapp.JsonPojo.LoginResponseObj;
import emc.captiva.mobile.sdksampleapp.Model.Cookie;
import emc.captiva.mobile.sdksampleapp.Network.CaptivaImageUploadService;
import emc.captiva.mobile.sdksampleapp.Network.FilestackImageUploadService;
import emc.captiva.mobile.sdksampleapp.RestClient.CaptivaImageUploaderClient;
import emc.captiva.mobile.sdksampleapp.RestClient.FilestackClient;
import emc.captiva.mobile.sdksampleapp.RestClient.SessionClient;
import emc.captiva.mobile.sdksampleapp.Util.StringUtil;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * This class represents the main window offering the ability to take a picture,
 * enhance an image from the gallery, or adjust the settings.
 */
public class MainActivity extends Activity implements PictureCallback, ContinuousCaptureCallback {
	static boolean _newLoad = true;
    private static String TAG = MainActivity.class.getSimpleName();
    private final int CHOOSE_IMAGE = 1;
    private int _captureCount = 0;
    static String USE_QUADVIEW = "UseQuadView";
    static String USE_MOTION = "UseMotion";
    CustomWindow _customWindow;
    private boolean loggedIn = false;
    private ProgressDialog dialog;
    /* (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // License the application
        CoreHelper.license(this);

    }
    
    /**
     * Launches the camera window to take a picture, which saves to the image gallery.
     * @param view      The view for the control event.
     */
    public void onTakePicture(View view) {
        // Use a separate HashMap to hold non-TakePicture parameter values from preferences.
        HashMap<String, Object> appParams = new HashMap<>();
        // Obtain our picture parameters from the preferences. Only supported SDK keys should go into parameters.
        HashMap<String, Object> parameters = CoreHelper.getTakePictureParametersFromPrefs(this, appParams);

        // Get the preference for CaptureWindow
        SharedPreferences gprefs = PreferenceManager.getDefaultSharedPreferences(this);
        String capWndNone = CoreHelper.getStringResource(this, R.string.GPREF_CAPTURE_CUSTOM_OPTIONS_NONE);
        String capWndPref = gprefs.getString(CoreHelper.getStringResource(this, R.string.GPREF_CAPTURE_CUSTOM_OPTIONS), capWndNone);

        // Explicitly test for null to avoid linter warning.
        if (capWndPref == null) {
            capWndPref = capWndNone;
        }

        if (capWndPref.compareToIgnoreCase(capWndNone) != 0) {
            // Assign a custom CaptureWindow if specified by the prefs.
            CaptureWindow wnd = new CustomWindow(this, capWndPref, appParams);
            parameters.put(CaptureImage.PICTURE_CAPTUREWINDOW, wnd);
        }
        else if ((boolean) appParams.get(USE_QUADVIEW))
        {
            CaptureWindow wnd = new CustomWindow(this, capWndPref, appParams);
            parameters.put(CaptureImage.PICTURE_CAPTUREWINDOW, wnd);
        }

        // Launch the camera to take a picture.
    CaptureImage.takePicture(this, parameters);
    }

    /* (non-Javadoc)
     * @see emc.captiva.mobile.sdk.PictureCallback#onPictureTaken(byte[])
     */
    @Override
    public void onPictureTaken(byte[] imageData) {

        // Use our utility functions to obtain a unique filename to store into the image gallery.
        File fullpath = new File(CoreHelper.getImageGalleryPath(), CoreHelper.getUniqueFilename("Img", ".JPG"));
        try {
            // Use our utility function to save this JPG encoded byte array to storage.
            ByteArrayInputStream inputStream = new ByteArrayInputStream(imageData);       
            CoreHelper.saveFile(inputStream, fullpath);
            
            // Get a URI to broadcast and let Android know there is a new image in the gallery.
            Uri uri = Uri.fromFile(fullpath);
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));
            
            // Send the new picture taken to the enhancement screen so that users can modify it if necessary.
            gotoEnhanceImage(uri);
        } 
        catch (IOException e) {
            // Log a message and display an error using our utility function.
            Log.e(TAG, e.getMessage(), e);            
            CoreHelper.displayError(this, "Could not save the image to the gallery.");
        }
    }
    	
	/* (non-Javadoc)
     * @see emc.captiva.mobile.sdk.PictureCallback#onPictureCanceled(int)
     */
    @Override
    public void onPictureCanceled(int reason) {
        captureCanceled(reason);
    }

    // This captureCanceled() helper method is called by both onPictureCanceled() and onContinuousCaptureCanceled().
    private void captureCanceled(int reason) {
        // This callback will be called if the take picture operation was canceled.
        if (reason == PictureCallback.REASON_OPTIMAL_CONDITIONS) {
            CoreHelper.displayError(this, "The optimal conditions were not met and the picture was canceled.");
        } else if (reason == PictureCallback.REASON_CAMERA_ERROR) {
            StringBuilder errorReport = new StringBuilder();
            CaptureException ex = CaptureImage.getLastError();
            if (ex != null)
            {
                errorReport.append("\n\n************ Stack Trace ************\n\n");
                errorReport.append(Log.getStackTraceString(ex));
            }

            CoreHelper.displayError(this, "An error occurred while accessing the camera." + errorReport);
        }
    }

    /**
     * Launches the camera window to take a picture and then continually sends images to the ContinuousCaptureCallback.newImage() callback
     * until that callback returns false or the Activity is canceled by the back button.  It is the responsibility
     * of the caller to store any images (in that newImage() callback) that should be persisted.
     * @param view      The view for the control event.
     */
    public void onTakeContinuousPictures(View view) {
        // Use a separate HashMap to hold non-TakePicture parameter values from preferences.
        HashMap<String, Object> appParams = new HashMap<>();
        // Obtain our picture parameters from the preferences. Only supported SDK keys should go into parameters.
        HashMap<String, Object> parameters = CoreHelper.getTakePictureParametersFromPrefs(this, appParams);

        // Get the preference for CaptureWindow
        SharedPreferences gprefs = PreferenceManager.getDefaultSharedPreferences(this);
        String capWndNone = CoreHelper.getStringResource(this, R.string.GPREF_CAPTURE_CUSTOM_OPTIONS_NONE);
        String capWndPref = gprefs.getString(CoreHelper.getStringResource(this, R.string.GPREF_CAPTURE_CUSTOM_OPTIONS), capWndNone);

        // Explicitly test for null to avoid the linter warning.
        if (capWndPref == null) {
            capWndPref = capWndNone;
        }

        if (capWndPref.compareToIgnoreCase(capWndNone) != 0) {
            // Assign a custom CaptureWindow if specified by the prefs.
            _customWindow = new CustomWindow(this, capWndPref, appParams);
            parameters.put(CaptureImage.PICTURE_CAPTUREWINDOW, _customWindow);
        }
        else if ((boolean) appParams.get(USE_QUADVIEW))
        {
            _customWindow = new CustomWindow(this, capWndPref, appParams);
            parameters.put(CaptureImage.PICTURE_CAPTUREWINDOW, _customWindow);
        }

        // Reset the capture counter.
        _captureCount = 0;

        // Launch the camera to take a picture.
        CaptureImage.continuousCapture(this, parameters);
    }

    /* (non-Javadoc)
     * @see emc.captiva.mobile.sdk.ContinuousCaptureCallback#newImage(byte[], Map)
     */
    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    @Override
    public ContinuousCaptureOperation newImage(byte[] imageData, Map<String, Object> qualityData) {
        CaptureException captureException;

        double newImageSimilarity = (double) qualityData.get(ContinuousCaptureCallback.CAPTURE_SIMILARITY);
        // Ensure that image data is retrieved.
        if (imageData == null) {
            CoreHelper.displayError(this, "Unable to retrieve image.");
            return ContinuousCaptureOperation.ContinuousCaptureStop;
        }

        // Get the similarity and count threshold values.
        SharedPreferences gprefs = PreferenceManager.getDefaultSharedPreferences(this);
        String temp = gprefs.getString(CoreHelper.getStringResource(this, R.string.GPREF_CAPTURESIMILARITY), "50");
        float similarityThresholdFromSettings = CoreHelper.getFloat(temp, 50f);
        temp = gprefs.getString(CoreHelper.getStringResource(this, R.string.GPREF_CAPTURECOUNT), "2");
        int captureCountLimit = CoreHelper.getInteger(temp, 2);

        // Only examine images that are different enough from the previous image that they could be
        // a new image (and not the same one that just happened to be caught by the capture interval).
        if (newImageSimilarity < similarityThresholdFromSettings ) {

            // First, load the image into the SDK for processing.
            CaptureImage.load(imageData);
            captureException = CaptureImage.getLastError();
            if (captureException != null) {
                CoreHelper.displayError(this, captureException.getMessage());
                return ContinuousCaptureOperation.ContinuousCaptureStop;
            }

            // Run autocrop on the captured image.
            Map<String, Object> imageProps = CaptureImage.getImageProperties();
            int shortSideFullSize = Math.min((int)imageProps.get(CaptureImage.IMAGE_PROPERTY_WIDTH), (int)imageProps.get(CaptureImage.IMAGE_PROPERTY_HEIGHT));
            CaptureImage.applyFilters(new String[]{CaptureImage.FILTER_CROP}, null);
            imageProps = CaptureImage.getImageProperties();
            // Only keep the cropped image if it is at least 10% the size of the short side of the full image to prevent over-cropping
            if (((int)imageProps.get(CaptureImage.IMAGE_PROPERTY_WIDTH) > shortSideFullSize / 10) && ((int)imageProps.get(CaptureImage.IMAGE_PROPERTY_HEIGHT) > shortSideFullSize / 10)) {
                // Save cropped image
                // Build the parameter map for saving the image.
                HashMap<String, Object> parameters = new HashMap<>();
                temp = gprefs.getString(CoreHelper.getStringResource(this, R.string.GPREF_DPIX), "72");
                int saveDpix = CoreHelper.getInteger(temp, 72);
                parameters.put(CaptureImage.SAVE_DPIX, saveDpix);
                temp = gprefs.getString(CoreHelper.getStringResource(this, R.string.GPREF_DPIY), "72");
                int saveDpiy = CoreHelper.getInteger(temp, 72);
                parameters.put(CaptureImage.SAVE_DPIY, saveDpiy);
                temp = gprefs.getString(CoreHelper.getStringResource(this, R.string.GPREF_JPGQUALITY), "95");
                int jpgQuality = CoreHelper.getInteger(temp, 95);
                parameters.put(CaptureImage.SAVE_JPG_QUALITY, jpgQuality);

                try {
                    // JPEG encode the cropped image.
                    imageData = CaptureImage.getEncodedBytes(CaptureImage.SAVE_JPG, parameters);
                } catch (CaptureException ex) {
                    CoreHelper.displayError(this, ex.getMessage());
                    return ContinuousCaptureOperation.ContinuousCaptureStop;
                }
            }

            // Use our utility functions to obtain a unique filename to store into the image gallery.
            File path = new File(CoreHelper.getImageGalleryPath(), CoreHelper.getUniqueFilename("Img", ".JPG"));
            ByteArrayInputStream inputStream = new ByteArrayInputStream(imageData);
            try {
                // Save the cropped and encoded image.
                CoreHelper.saveFile(inputStream, path);

                if (_customWindow != null) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            _customWindow.flashScreen();
                        }
                    });
                }
                Log.d(TAG, "Picture saved");

            } catch (IOException ex) {
                CoreHelper.displayError(this, ex.getMessage());
                return ContinuousCaptureOperation.ContinuousCaptureStop;
            }

            // Get a URI to broadcast and let Android know there is a new image in the gallery.
            Uri uri = Uri.fromFile(path);
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));

            // Once the number of captured images reaches the capture count limit, stop continuous
            // capture processing.
            if (++_captureCount >= captureCountLimit) {
                return ContinuousCaptureOperation.ContinuousCaptureStop;
            }
        } else {
            Log.d(TAG, "The new image was not different enough from the previous to initiate capture.");
            return ContinuousCaptureOperation.ContinuousCaptureContinueWithPrevious;
        }

        return ContinuousCaptureOperation.ContinuousCaptureContinue;
    }

    /**
     * This is the cancellation handler for Continuous Capture mode.  Perform any cleanup of the canceled
     * continuous capture session here.
     * @param reason    The reason code for the canceled continuous capture session.
    */
    @Override
    public void onContinuousCaptureCanceled(int reason) {
        captureCanceled(reason);
    }

    /**
     * This is the click handler for the Enhance Image button.
     * @param view    The view for the control event.
     */
    public void onEnhanceImage(View view) {
        // Launch the image gallery picker to allow the user to choose which image to enhance.
        // Only Android supported formats will display. TIFF images are not supported by the
        // image gallery viewer as of 4.2.2. Therefore, any TIFF images that are saved to the folder
        // will not display in the gallery picker. However, if you save a TIFF image to the gallery 
        // storage folder, it will still save and you can verify that it is there by using the 
        // Android "My Files" application if available on your device, or the Android Debug 
        // Bridge (adb). You can get the path to the file by debugging this application's save function.
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        startActivityForResult(intent, CHOOSE_IMAGE);
    }
    
    /**
     * Displays the Settings panel.
     * @param view      The view for the control event.
     */
    public void onSettings(View view) {
        // Launch the preference settings activity.
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    public void onLogin(View view) {

        if(this.loggedIn){
            displayCustomToast("Login" , "Failed" , "Please Log out before attempting to Log in");
            return;
        }
        SessionClient client = new SessionClient();
        Call<LoginResponseObj> call = client.login();
        call.enqueue(new Callback<LoginResponseObj>() {
            @Override
            public void onResponse(Call<LoginResponseObj> call, Response<LoginResponseObj> response) {

                MainActivity.this.stopProcessDialog();

                switch(response.code()){

                    case 200:
                        Log.d("Success", "login call succeed");
                        Log.d("Status Code", String.valueOf(response.code()));
                        Log.d("Response", response.message());
                        LoginResponseObj result = response.body();
                        Cookie.saveCookie(result.ticket);
                        MainActivity.this.loggedIn = true;
                        displayCustomToast("Login", "Success" , response.message());
                        break;
                    default:
                        Log.e("Error", "login call has failed");
                        displayCustomToast("Login", "Failed" , response.message());
                        break;
                }

            }

            @Override
            public void onFailure(Call<LoginResponseObj> call, Throwable t) {
                Log.e("Error", "login call has failed");
                MainActivity.this.stopProcessDialog();
                displayCustomToast("Login", "Failed" , t.toString());
            }
        });
        this.startProcessDialog();

    }

    public void onLogout(View view) {

        if(!this.loggedIn){
            displayCustomToast("Loggout" , "Failed" , "Please Log in before attempting to Log out");
            return;
        }
        SessionClient client = new SessionClient();
        Call<ResponseBody> call = client.logout();
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

                MainActivity.this.stopProcessDialog();
                switch(response.code()){

                    case 200:
                        Log.d("Success", "Logout call succeed");
                        Log.d("Status Code", String.valueOf(response.code()));
                        Log.d("Response", response.message());
                        Cookie.deleteCookie();
                        MainActivity.this.loggedIn = false;
                        displayCustomToast("Logout", "Success" , response.message());
                        break;
                    default:
                        Log.e("Error", "Logout call has failed");
                        displayCustomToast("Logout", "Failed" , response.message());
                        break;
                }

            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("Error", "logout call has failed");
                MainActivity.this.stopProcessDialog();
                displayCustomToast("logout", "Success" , t.toString());
            }
        });
        this.startProcessDialog();

    }

    public void onFileStackUpload(View view) {

        FilestackImageUploadService service = new FilestackClient().getService();
        String key = "AaApUHHABQg2818PX5CLTz";
        String file_name = "1169155_713668108769575_1241597324_n.jpg";
        Call<ResponseBody> call = service.updateImage(key, file_name, createRequestBody(file_name));
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                Log.d("Success", "Filestack Upload succeed");
                Log.d("Status Code", String.valueOf(response.code()));
                Log.d("Response", response.message());
                MainActivity.this.stopProcessDialog();
                displayCustomToast("Filestack Upload", "Success" , response.message());
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("Error", "Filestack upload has failed");
                Log.e("Error" , t.toString());
                MainActivity.this.stopProcessDialog();
                displayCustomToast("Filestack Upload", "Success" , t.toString());
            }
        });
        this.startProcessDialog();
    }

    public void onCaptivaUpload(View view) {

        if(!this.loggedIn){
            displayCustomToast("Upload" , "Failed" , "Please Log in before attempting to Upload");
            return;
        }

        AssetManager am = getApplicationContext().getAssets();
        InputStream is = null;
        try{
            is = getAssets().open("base64Image.txt");
        }catch(Exception e){

        }
        String imageData = StringUtil.getStringFromInputStream(is);;
        ImageUploadObj obj = new ImageUploadObj(imageData);
        CaptivaImageUploadService service = new CaptivaImageUploaderClient().createImageUploadServer();
        Call<ResponseBody> call = service.uploadImage(obj);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                Log.d("Success", "Captiva Image Upload succeed");
                Log.d("Status Code", String.valueOf(response.code()));
                Log.d("Response", response.message());
                MainActivity.this.stopProcessDialog();
                displayCustomToast("Captiva Image Upload", "Success" , response.message());
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("Error", "CaptivalImage upload has failed");
                Log.e("Error" , t.toString());
                MainActivity.this.stopProcessDialog();
                displayCustomToast("CaptivaImage Upload", "Failed" , t.toString());
            }
        });
        this.startProcessDialog();
    }

    public void onCaptureAndUpload(View view){
        //TODO Implement
    }

    public void onEnhanceAndUpload(View view){
        //TODO Implement
    }

    private void displayCustomToast(String action , String result, String description) {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage(action + " " + result)
                .setPositiveButton(description, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // FIRE ZE MISSILES!
                        dialog.dismiss();
                    }
                });
        AlertDialog dialog  = builder.create();
        dialog.show();

    }

    private RequestBody createRequestBody(String fileName){

        //Create file
        String sdCardPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        String filePath = "/Download/" + fileName;
        File file = new File(sdCardPath+filePath);

        //Multipart Builder
        String content_type = getMimeType(file.getPath());
        String file_name = file.getName();
        RequestBody requestBody = RequestBody.create(MediaType.parse(content_type),file);
        return requestBody;

    }

    private String getMimeType(String path){

        String extension = MimeTypeMap.getFileExtensionFromUrl(path);
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);

    }

    /* (non-Javadoc)
     * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {  
    	super.onActivityResult(requestCode, resultCode, data);
    	
    	// Handle results for activities launched by this activity.
        try {            
            if(requestCode == CHOOSE_IMAGE && data != null && data.getData() != null) {         
                // The user picked an image from the gallery.                
                // Send the new picture taken to the enhancement screen so that users can modify it if necessary.
                Uri uri = data.getData();
                gotoEnhanceImage(uri);
            }
            // Note: The broadcast of the image change is already done in this.onPictureTaken for original picture and
            // in EnhanceImageActivity for images that get enhanced/modified.
        }
        catch (Exception e) {
            // Log a message and display the error to the user using our utility function.
            Log.e(TAG, e.getMessage(), e);
            CoreHelper.displayError(this, e);
        }
    }

    private void gotoEnhanceImage(Uri uri) {
        
        // If we have a file, then send it to enhancement.
    	if (uri != null) {    
    	    // Send the file path to enhancement so that it can load in the screen.
            String filepath = CoreHelper.getFilePathFromContentUri(this, uri);
            Intent intent = new Intent(this, EnhanceImageActivity.class);
            intent.putExtra("Filename", filepath);
            _newLoad = true;
            startActivity(intent);
        }
    }


    private boolean startProcessDialog(){

        if(this.dialog == null){
            this.dialog = new ProgressDialog(this);
        }
        this.dialog.show();
        return true;
    }

    private boolean stopProcessDialog(){

        if(this.dialog != null){
            this.dialog.dismiss();
            return true;
        }
        return false;
    }
}

