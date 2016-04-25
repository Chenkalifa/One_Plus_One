package hyperactive.co.il.myfacebookapp;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.Profile;
import com.facebook.share.ShareApi;
import com.facebook.share.Sharer;
import com.facebook.share.model.SharePhoto;
import com.facebook.share.model.SharePhotoContent;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ShareActivity extends AppCompatActivity implements View.OnFocusChangeListener {
    private Toolbar toolbar;
    private Profile profile;
    private TextView shareTv;
    private TableRow inputRow;
    private AutoCompleteTextView pic1UrlEt, pic2UrlEt, pic3UrlEt;
    private EditText  pic1TextEt, pic2TextEt, pic3TextEt;
    private ImageView img1, img2, img3, background_img;
    private Bitmap image;
    final private String MY_LOG = "myLog";
    private String friends;
    private List<String> friendsList;
    private Map<String, String> friendsMap;
    private JSONArray friendsJsonList;
    private Drawable et_original_background;
    private ArrayAdapter<String> adapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        friendsMap=new HashMap<String,String>();
        friendsList=new ArrayList<>();
        background_img= (ImageView) findViewById(R.id.share_background_img);
//        Glide.with(this).load(R.drawable.background_1_plus_1).into(background_img);

        inputRow = (TableRow) findViewById(R.id.input_row);
        shareTv = (TextView) findViewById(R.id.toolbar_share_btn);
        pic1UrlEt = (AutoCompleteTextView) findViewById(R.id.img_1_url_et);

        pic1TextEt = (EditText) findViewById(R.id.img_1_txt_et);
        et_original_background = pic1TextEt.getBackground();
        img1 = (ImageView) findViewById(R.id.img_1);
        pic2UrlEt = (AutoCompleteTextView) findViewById(R.id.img_2_url_et);
        pic2TextEt = (EditText) findViewById(R.id.img_2_txt_et);
        img2 = (ImageView) findViewById(R.id.img_2);
        pic3UrlEt = (AutoCompleteTextView) findViewById(R.id.img_3_url_et);
        pic3TextEt = (EditText) findViewById(R.id.img_3_txt_et);
        img3 = (ImageView) findViewById(R.id.img_3);
        pic1UrlEt.setOnFocusChangeListener(this);
        pic2UrlEt.setOnFocusChangeListener(this);
        pic3UrlEt.setOnFocusChangeListener(this);
        getUserFriends();
        profile = Profile.getCurrentProfile();
        shareTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("myLog", "going to post picture");
                postPicture();
            }
        });
    }

    private void getUserFriends() {
        Intent callingIntent=getIntent();
        if (callingIntent!=null){
            try {
                Log.i(MY_LOG, ""+callingIntent.hasExtra("friendsJsonList"));
                friendsJsonList =new JSONArray(callingIntent.getStringExtra("friendsJsonList"));
                Log.i(MY_LOG, "ShareActivity,getUserFriends friends=" + friendsJsonList.toString());
                JSONObject tempFriendObj=new JSONObject();
                for(int i=0;i< friendsJsonList.length(); i++){
                    tempFriendObj= (JSONObject) friendsJsonList.get(i);
                    friendsList.add(tempFriendObj.getString("name").trim().toLowerCase());
                    friendsMap.put(tempFriendObj.getString("name").trim().toLowerCase(), tempFriendObj.getString("id"));
                }
                adapter=new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1, friendsList);
                pic1UrlEt.setAdapter(adapter);
                pic2UrlEt.setAdapter(adapter);
                pic3UrlEt.setAdapter(adapter);
            } catch (JSONException e) {
                Log.e(MY_LOG, "JSON error", e);
            }
        }
    }

    private void postPicture() {
        Log.i("myLog", "in post picture");
        //save the screenshot
        removeViews();
        final View rootView = findViewById(R.id.activity_share_layout);//findViewById(android.R.id.content).getRootView()
//        rootView.setDrawingCacheEnabled(true);
        rootView.buildDrawingCache(true);
        image = rootView.getDrawingCache(true).copy(Bitmap.Config.RGB_565, false);
//        image = rootView.getDrawingCache();
        rootView.destroyDrawingCache();


        //share dialog
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.share_dialog);
        Button yesBtn= (Button) dialog.findViewById(R.id.dialog_share_yes_btn);
        Button cancelBtn= (Button) dialog.findViewById(R.id.dialog_share_cancel_btn);
        yesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //share the image to Facebook
                SharePhoto photo = new SharePhoto.Builder().setBitmap(image).build();

                SharePhotoContent content = new SharePhotoContent.Builder().addPhoto(photo).build();
                returnViews();
                ShareApi.share(content, new FacebookCallback<Sharer.Result>() {
                    @Override
                    public void onSuccess(Sharer.Result result) {
                        Log.i(MY_LOG, "share success");
                        Toast.makeText(ShareActivity.this, R.string.share_success_msg, Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onCancel() {
                        Log.i(MY_LOG, "share cancel");
                        Toast.makeText(ShareActivity.this, R.string.share_cancel_msg, Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onError(FacebookException error) {
                        Log.e(MY_LOG, "share error", error);
                        Toast.makeText(ShareActivity.this, R.string.share_error_msg, Toast.LENGTH_LONG).show();
                    }
                });
                dialog.dismiss();
            }
        });
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                returnViews();
                dialog.cancel();
            }
        });
        dialog.show();
    }

    private void removeViews() {
//        pic1UrlEt.requestFocus();
        toolbar.setVisibility(View.GONE);
        inputRow.setVisibility(View.GONE);
        checkTextTv(pic1TextEt);
        checkTextTv(pic2TextEt);
        checkTextTv(pic3TextEt);
    }

    private void checkTextTv(EditText textEt) {
        if (textEt.getText().toString().equals(""))
           textEt.setVisibility(View.INVISIBLE);
        else
            textEt.setBackgroundResource(R.drawable.no_shape);
    }

    private void returnViews() {
        toolbar.setVisibility(View.VISIBLE);
        inputRow.setVisibility(View.VISIBLE);
        pic1TextEt.setVisibility(View.VISIBLE);
        pic1TextEt.setBackgroundDrawable(et_original_background);
        pic2TextEt.setVisibility(View.VISIBLE);
        pic2TextEt.setBackgroundDrawable(et_original_background);
        pic3TextEt.setVisibility(View.VISIBLE);
        pic3TextEt.setBackgroundDrawable(et_original_background);
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        View vv=v;
        switch (vv.getId()) {
            case R.id.img_1_url_et:
                Log.i(MY_LOG, "in image1");
                getPicture(pic1UrlEt, img1);
                break;
            case R.id.img_2_url_et:
                Log.i(MY_LOG, "in image2");
                getPicture(pic2UrlEt, img2);
                break;
            case R.id.img_3_url_et:
                Log.i(MY_LOG, "in image3");
                getPicture(pic3UrlEt, img3);
                break;
        }

    }

    private void getPicture(EditText picUrlEt, ImageView image) {

        try {
            String picURL=checkIsInputAFriend(picUrlEt);
            Glide.with(this).load(new URL(picURL)).into(image);
            image.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            picUrlEt.setWidth(80);
            picUrlEt.setText("");
//            new PictureLoaderTask(picUrlEt, image).execute(new URL(picUrlEt.getText().toString()));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    private String checkIsInputAFriend(EditText picUrlEt) {
        String input=picUrlEt.getText().toString().trim().toLowerCase();
        if(friendsMap.containsKey(input))
            input="https://graph.facebook.com/"+friendsMap.get(input)+"/picture?type=normal";
        Log.i(MY_LOG, "input="+input);
        return input;
    }


    private class PictureLoaderTask extends AsyncTask<URL, Void, Bitmap> {
        private EditText et;
        private ImageView image;
        PictureLoaderTask(EditText et, ImageView image){
            this.et=et;
            this.image=image;
        }
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            if (bitmap != null) {
                image.setBackgroundColor(getResources().getColor(android.R.color.transparent));
//                image.setImageBitmap(bitmap);
                et.setWidth(80);
                et.setText("");
            } else{
                Log.i(MY_LOG, "bitmap null");
                Toast.makeText(ShareActivity.this, "Sorry, not a picture", Toast.LENGTH_LONG).show();
            }

        }

        @Override
        protected Bitmap doInBackground(URL... params) {
            Bitmap bitmap = null;
            try {
                Glide.with(ShareActivity.this).load(params[0]).into(image);
                InputStream inputStream = (InputStream) params[0].getContent();
                bitmap = null;//BitmapFactory.decodeStream(inputStream);
            } catch (FileNotFoundException e) {
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
