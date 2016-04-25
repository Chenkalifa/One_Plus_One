package hyperactive.co.il.myfacebookapp;

import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.facebook.Profile;
import com.facebook.ProfileTracker;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;


public class LoginActivity extends AppCompatActivity {
    private LoginButton loginButton;
    private LoginManager loginManager;
    private CallbackManager callbackManager, loginManagerCallback;
    private AccessToken accessToken;
    private AccessTokenTracker accessTokenTracker;
    private ProfileTracker profileTracker;
    private Profile profile;
    private ImageView background_img, user_img;
    private String friends;
    private boolean isJustLoggedIn = false;
    private int counter = 0;
    final private String MY_LOG = "myLog";
//    public static Facebook facebook = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        AppEventsLogger.activateApp(this);
//        background_img= (ImageView) findViewById(R.id.background);
//        Glide.with(this).load(R.drawable.banner).into(background_img);
        profileTracker = new ProfileTracker() {
            @Override
            protected void onCurrentProfileChanged(Profile oldProfile, Profile currentProfile) {

            }
        };
        accessTokenTracker = new AccessTokenTracker() {
            @Override
            protected void onCurrentAccessTokenChanged(AccessToken oldAccessToken, AccessToken currentAccessToken) {

            }
        };
        profileTracker.startTracking();
        accessTokenTracker.startTracking();
        loginManager = LoginManager.getInstance();

        loginButton = (LoginButton) findViewById(R.id.login_button);
        loginButton.setReadPermissions("user_friends");
        callbackManager = CallbackManager.Factory.create();
        loginManagerCallback = CallbackManager.Factory.create();

        loginManager.registerCallback(loginManagerCallback, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                isJustLoggedIn = true;
                Log.i(MY_LOG, "on login manager success");
            }

            @Override
            public void onCancel() {
                Log.i(MY_LOG, "cancel");
            }

            @Override
            public void onError(FacebookException error) {
                Log.e(MY_LOG, "error", error);
                Toast.makeText(LoginActivity.this, error.toString(), Toast.LENGTH_LONG).show();
            }
        });
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.i(MY_LOG, "success");
                isJustLoggedIn = true;
                if (counter == 0) {
                    loginManager.logInWithPublishPermissions(
                            LoginActivity.this,
                            Arrays.asList("publish_actions"));
                    counter++;
                } else {
                    accessToken = loginResult.getAccessToken();
                    profile = Profile.getCurrentProfile();
                    getFriends(accessToken);
                }
            }

            @Override
            public void onCancel() {
                Log.i(MY_LOG, "cancel");
            }

            @Override
            public void onError(FacebookException error) {
                Log.e(MY_LOG, "error", error);
                Toast.makeText(LoginActivity.this, error.toString(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void getFriends(AccessToken accessToken) {
        if (accessToken==null)
            accessToken=AccessToken.getCurrentAccessToken();
        isJustLoggedIn = false;
        GraphRequest.newMyFriendsRequest(accessToken, new GraphRequest.GraphJSONArrayCallback() {
            @Override
            public void onCompleted(JSONArray objects, GraphResponse response) {

                Log.i(MY_LOG, "getFriends response=" + response.toString());
                Log.i(MY_LOG, "getFriends JSONArray objects=" + objects.toString());
                Intent intent = new Intent(LoginActivity.this, ShareActivity.class);
                intent.putExtra("friendsJsonList", objects.toString());
                startActivity(intent);
            }
        }).executeAsync();
    }

    @Override
    protected void onResume() {
        super.onResume();
        profile = Profile.getCurrentProfile();
        if (profile != null && !isJustLoggedIn) {
            Log.i(MY_LOG, "on resume profile=" + profile.toString());
            openAlreadyLoggedDialog();
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
        loginManagerCallback.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        profileTracker.stopTracking();
        accessTokenTracker.stopTracking();
    }

    private void openAlreadyLoggedDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.login_dialog);
        user_img = (ImageView) dialog.findViewById(R.id.user_img);
        TextView dialogtxt = (TextView) dialog.findViewById(R.id.dialog_txt);
        if (profile != null) {
            dialogtxt.setText(getString(R.string.dialog_msg) + profile.getFirstName() + "\n" + getString(R.string.cuntinue_msg));
            Uri profilePictureUri = profile.getProfilePictureUri(80, 80);
            Glide.with(LoginActivity.this).load(profilePictureUri).into(user_img);
        } else {
            Log.i(MY_LOG, "profile null");
        }

        Button ok = (Button) dialog.findViewById(R.id.ok_button);
        Button cancel = (Button) dialog.findViewById(R.id.cancel_button);
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isJustLoggedIn = false;
                getFriends(accessToken);
            }
        });
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginManager.logOut();
                dialog.dismiss();
            }
        });
        dialog.show();
    }
}
