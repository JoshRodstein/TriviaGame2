package edu.pitt.cs1699.jor94_triviagame2;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.*;
import com.google.firebase.database.*;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;


public class LoginActivity extends AppCompatActivity {

    private GoogleApiClient mGoogleApiClient;
    private final int RC_SIGN_IN = 202;
    private final int RC_CAMERA = 204;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseStorage storage;
    private DatabaseReference db, photoResult;
    private Uri downloadUrl;
    private SignInButton mSignInButton;
    private boolean authTrack = false;
    private static final String GoogleTAG= "GOOGLE_SIGN_IN:";
    private static final String UserPhotoTAG= "UPLOAD_USER_PHOTO:";
    DatabaseHelper dbh;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        configureSignIn();
        new DatabaseHelper(this).getAllTermsAndDefs();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel, but only on API 26+ because
            // the NotificationChannel class is new and not in the support library
            {
                CharSequence name = getString(R.string.channel_name);
                NotificationChannel channel = new NotificationChannel("channel_1", name,
                        NotificationManager.IMPORTANCE_DEFAULT);
                channel.setDescription("Top Scores Changed");
                // Register the channel with the system
                NotificationManager notificationManager =
                        (NotificationManager) getSystemService(this.NOTIFICATION_SERVICE);
                notificationManager.createNotificationChannel(channel);

                name = "channel_2";
                NotificationChannel channel2 = new NotificationChannel("channel_2", name,
                        NotificationManager.IMPORTANCE_DEFAULT);
                channel.setDescription("A user has added a term!");
                notificationManager.createNotificationChannel(channel2);
            }


        }

        mSignInButton = (SignInButton) findViewById(R.id.sign_in_button);
        mSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(view.getId() == R.id.sign_in_button) {
                    signIn();
                }
            }
        });



    }

    public void configureSignIn(){
        // Configure sign-in to request the user’s basic profile like name and email
        GoogleSignInOptions options = new GoogleSignInOptions
                .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .requestProfile()
                .build();
        // Build a GoogleApiClient with access to GoogleSignIn.API and the options above.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */ ,
                        new GoogleApiClient.OnConnectionFailedListener() {
                            @Override
                            public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

                            }
                        })
                .addApi(Auth.GOOGLE_SIGN_IN_API, options)
                .build();
        mAuth = com.google.firebase.auth.FirebaseAuth.getInstance();


    }

    private void signIn(){
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(Arrays.asList(
                        new AuthUI.IdpConfig.GoogleBuilder().build()))
                .build(), RC_SIGN_IN);
    }

    public void uploadUserPhoto(FirebaseUser u){
        db = FirebaseDatabase.getInstance().getReference();
        photoResult = db.child("users").child(u.getUid())
                .child("photo");
        photoResult.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String photoURL = dataSnapshot.getValue(String.class);
                if(photoURL == null || photoURL.equals("null")){
                    Log.w(UserPhotoTAG, "Take User Photo: ");
                    Intent photoCaptureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(photoCaptureIntent, RC_CAMERA);
                } else {
                    Log.w(UserPhotoTAG, "User Has Photo: ");
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                return;
            }
        });
        authTrack = true;

    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == RESULT_OK) {
                // Successfully signed in
                Log.w(GoogleTAG, "Success");
                FirebaseUser user = mAuth.getCurrentUser();
                uploadUserPhoto(user);
            } else {
                // Sign in failed, check response for error code
                Log.w(GoogleTAG, "Failed:\n" + response);
            }
        } else if (requestCode == RC_CAMERA && resultCode == RESULT_OK) {

            String photo = mAuth.getCurrentUser().getUid() + ".jpg";

            storage = FirebaseStorage.getInstance();
            StorageReference storageRef = storage.getReference();
            StorageReference userImagesRef = storageRef.child("user_photos/" + photo);

            Bitmap bitmap = (Bitmap)data.getExtras().get("data");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            byte[] dataAry = baos.toByteArray();

            UploadTask uploadTask = userImagesRef.putBytes(dataAry);
            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    // Handle unsuccessful uploads
                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download URL.
                    downloadUrl = taskSnapshot.getDownloadUrl();
                    photoResult.setValue(downloadUrl.toString());

                }
            });

        }

    }

    public void play_button(View view) {

        if (authTrack == true || mAuth.getCurrentUser() != null) {
            Intent intent = new Intent(this, StartActivity.class);
            startService(new Intent(this, ChildEventListener.class));
            startActivity(intent);

        }
    }

    public void onStart() {
        super.onStart();
    }

    public void onDestroy() {
        super.onDestroy();
        AuthUI.getInstance()
                .signOut(this)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    public void onComplete(@NonNull Task<Void> task) {
                        Log.w("ON_DESTROY", "CALLED:\n");
                    }
                });
        authTrack = false;

    }



}
