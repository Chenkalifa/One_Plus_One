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
    private CallbackManager callbackManager;
    private AccessTokenTracker accessTokenTracker;
    private ProfileTracker profileTracker;
    private Profile profile;
    private ImageView background_img, user_img;
    private String friends;
    final private String MY_LOG = "myLog";

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
//        loginButton.setReadPermissions("user_friends");
        loginButton.setPublishPermissions("publish_actions");
        callbackManager = CallbackManager.Factory.create();
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.i(MY_LOG, "success");
                AccessToken accessToken = loginResult.getAccessToken();
                profile = Profile.getCurrentProfile();
                new UserFriendsRequestTask().execute(accessToken);
//                LoginManager.getInstance().logInWithPublishPermissions(
//                        LoginActivity.this,
//                        Arrays.asList("publish_actions"));
//                new GraphRequest(
//                        accessToken,
//                        "/me/friends",
//                        null,
//                        HttpMethod.GET,
//                        new GraphRequest.Callback() {
//                            public void onCompleted(GraphResponse response) {
//
//                                try {
//                                    JSONArray friendsList = response.getJSONObject().getJSONArray("data");
//                                    friends=friendsList.toString();
//                                    Log.i(MY_LOG, friends);
//
//                                } catch (JSONException e) {
//                                    e.printStackTrace();
//                                }
//                            }
//                        }
//                ).executeAsync();
//                Intent intent = new Intent(LoginActivity.this, ShareActivity.class);
//                intent.putExtra("friendsList", friends);
//                startActivity(intent);
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

    @Override
    protected void onResume() {
        super.onResume();
        profile=Profile.getCurrentProfile();
        if (profile!=null){
            Log.i(MY_LOG, "on resume profile="+profile.toString());
            openAlreadyLoggedDialog();
        }

    }

    private boolean isLoggedIn() {
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        return accessToken != null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
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
        if(profile!=null){
            dialogtxt.setText(getString(R.string.dialog_msg) + profile.getFirstName() + "\n" + getString(R.string.cuntinue_msg));
            Uri profilePictureUri = profile.getProfilePictureUri(80, 80);
            Glide.with(LoginActivity.this).load(profilePictureUri).into(user_img);
        } else {
            Log.i(MY_LOG, "profile null");
        }

//        new PictureLoaderTask().execute(profilePictureUri);
        Button ok = (Button) dialog.findViewById(R.id.ok_button);
        Button cancel = (Button) dialog.findViewById(R.id.cancel_button);
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                new UserFriendsRequestTask().execute(AccessToken.getCurrentAccessToken());
                Intent intent = new Intent(LoginActivity.this, ShareActivity.class);
                startActivity(intent);
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

    private class UserFriendsRequestTask extends AsyncTask<AccessToken, Void, JSONObject>{
       private JSONObject jsonResponse;
        @Override
        protected JSONObject doInBackground(AccessToken... params) {
            Log.i(MY_LOG, "get user friends");
            GraphRequest request = GraphRequest.newMeRequest(
                    AccessToken.getCurrentAccessToken(),
                    new GraphRequest.GraphJSONObjectCallback() {
                        @Override
                        public void onCompleted(
                                JSONObject object,
                                GraphResponse response) {
                            if(object!=null){
                                Log.i(MY_LOG, "jsonObject="+object.toString());
                                jsonResponse=object;
                            }

                        }
                    });
            Bundle parameters = new Bundle();
            parameters.putString("fields", "friends");
            request.setParameters(parameters);
            request.executeAndWait();


//            new GraphRequest(
//                    AccessToken.getCurrentAccessToken(),
//                    "/me/friends",
//                    null,
//                    HttpMethod.GET,
//                    new GraphRequest.Callback() {
//                        public void onCompleted(GraphResponse response) {
//                            graphResponse=response;
//                        }
//                    }
//            ).executeAndWait();
            return jsonResponse;
        }


        @Override
        protected void onPostExecute(JSONObject jsonObject) {
            super.onPostExecute(jsonObject);
            if (jsonObject!=null){
//                try {
//
//                    friends=jsonObject.getJSONObject("friends").toString();
//                } catch (JSONException e) {
//                    Log.e(MY_LOG, "JSON error", e);
//                }
//                Log.i(MY_LOG, "friends="+friends);
                    Intent intent = new Intent(LoginActivity.this, ShareActivity.class);
                    intent.putExtra("friendsList",  jsonObject.toString());
                    startActivity(intent);
            }
        }
    }

    private class PictureLoaderTask extends AsyncTask<Uri, Void, Bitmap> {
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            if (bitmap != null)
                user_img.setImageBitmap(bitmap);
            else
                Log.i(MY_LOG, "bitmap null");
        }

        @Override
        protected Bitmap doInBackground(Uri... params) {
            Bitmap bitmap = null;
            try {
                URI path = new URI(params[0].toString());
                URL picUrl = path.toURL();
                InputStream inputStream = (InputStream) picUrl.getContent();
                bitmap = BitmapFactory.decodeStream(inputStream);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (URISyntaxException e) {
                e.printStackTrace();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return bitmap;
        }
    }
}
