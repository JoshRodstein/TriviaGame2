package edu.pitt.cs1699.jor94_triviagame2;

import android.app.usage.ConfigurationStats;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.*;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;


public class LoginActivity extends AppCompatActivity {

    private GoogleApiClient mGoogleApiClient;
    private final int RC_SIGN_IN = 202;
    private final int RC_CAMERA = 204;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private static final String GoogleTAG= "GOOGLE_SIGN_IN:";
    private static final String FAuthWG= "FIREBASE_AUTH_WITH_GOOGLE";
    private String idToken;
    //public SharedPrefManager sharedPrefManager;
    private final Context mContext = this;

    private SignInButton mSignInButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        configureSignIn();

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

    public void userPhoto(FirebaseUser u){
        DatabaseReference db = FirebaseDatabase.getInstance().getReference();
        DatabaseReference photoResult = db.child("users").child(u.getUid())
                .child("photo");

        photoResult.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String photoURL = dataSnapshot.getValue(String.class);
                //do what you want with the email
                if(photoURL.equals("null")){
                    Log.w("USER_PHOTO", "Take User Photo: ");
                    takePhoto();
                } else {
                    Log.w("USER_PHOTO", "User Has Photo: ");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    protected void takePhoto(){
        Intent photoCaptureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(photoCaptureIntent, RC_CAMERA);

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

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
/*
        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(…);
        if (requestCode == RC_SIGN_IN) {

            //GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                //Google SIgn in was Successful, save token/state, then auth with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
                Log.w(GoogleTAG, "Success");
            } catch (ApiException e) {
                Log.w(GoogleTAG, "Failed:\n" + e);
            }
        }*/

        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == RESULT_OK) {
                // Successfully signed in
                Log.w(GoogleTAG, "Success");
                FirebaseUser user = mAuth.getCurrentUser();
                userPhoto(user);
                // ...
            } else {
                // Sign in failed, check response for error code
                Log.w(GoogleTAG, "Failed:\n" + response);
            }
        } else if (requestCode == RC_CAMERA && resultCode == RESULT_OK) {
            Bitmap bitmap = (Bitmap)data.getExtras().get("data");


            storeCameraPhotoInSDCard(bitmap);

            // display the image from SD Card to ImageView Control
            String storeFilename = "user_photo.jpg";
            //Bitmap mBitmap = getImageFileFromSDCard(storeFilename);
            //imageHolder.setImageBitmap(mBitmap);

        }

    }

    private void storeCameraPhotoInSDCard(Bitmap bitmap){
        File outputFile = new File(Environment.getExternalStorageDirectory(), "user_photo.jpg");
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Bitmap getImageFileFromSDCard(String filename){
        Bitmap bitmap = null;
        File imageFile = new File(Environment.getExternalStorageDirectory() + filename);
        try {
            FileInputStream fis = new FileInputStream(imageFile);
            bitmap = BitmapFactory.decodeStream(fis);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return bitmap;
    }
/*
    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.w(FAuthWG, "firebaseAuthWithGoogle:" + acct.getId());

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.w(FAuthWG, "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            //updateUI(user);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(FAuthWG, "signInWithCredential:failure", task.getException());
                            //Snackbar.make(findViewById(R.id.ad_image_view), "Authentication Failed.", Snackbar.LENGTH_SHORT).show();
                            //updateUI(null);
                        }

                        // ...
                    }
                });
    }*/

    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        //updateUI(currentUser);
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

    }



}
