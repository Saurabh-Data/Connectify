package com.connectify.com.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.connectify.com.R;
import com.connectify.com.model.User;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.FirebaseDatabase;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private static final int RC_SIGN_IN = 11;

    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth;
    private FirebaseDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();

        String clientId = getString(R.string.default_web_client_id);
        if (clientId.isEmpty()) {
            Log.e(TAG, "Google client ID is null or empty. Check your 'default_web_client_id' in 'strings.xml'.");
            Toast.makeText(this, "Google client ID is not set", Toast.LENGTH_LONG).show();
            return;
        }

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(clientId)
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        findViewById(R.id.loginBtn).setOnClickListener(view -> signIn());
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    Log.d(TAG, "Google sign in successful, ID Token: " + account.getIdToken());
                    authWithGoogle(account.getIdToken());
                } else {
                    Log.e(TAG, "Google sign in failed: account is null");
                    Toast.makeText(this, "Google sign in failed: account is null", Toast.LENGTH_SHORT).show();
                }
            } catch (ApiException e) {
                handleSignInError(e);
            }
        }
    }

    private void handleSignInError(ApiException e) {
        Log.e(TAG, "Google sign in failed", e);
        if (e.getStatusCode() == CommonStatusCodes.CANCELED) {
            Log.w(TAG, "Google sign in canceled.");
            Toast.makeText(this, "Google Sign-In canceled.", Toast.LENGTH_SHORT).show();
        } else {
            Log.e(TAG, "signInResult:failed code=" + e.getStatusCode());
            Toast.makeText(this, "Google Sign-In failed. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private void authWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                saveUserProfile(user);
                            } else {
                                Log.e(TAG, "Firebase user is null");
                                Toast.makeText(LoginActivity.this, "Firebase user is null", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Log.e(TAG, "Authentication failed", task.getException());
                            Toast.makeText(LoginActivity.this, "Authentication failed: " + task.getException().getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void saveUserProfile(FirebaseUser user) {
        User firebaseUser = new User(user.getUid(), user.getDisplayName(), user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : null, "Unknown", 500);
        database.getReference()
                .child("profiles")
                .child(user.getUid())
                .setValue(firebaseUser)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finishAffinity();
                        } else {
                            Log.e(TAG, "Error saving user profile", task.getException());
                            Toast.makeText(LoginActivity.this, task.getException().getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}
