package de.fs.fintech.geogame;

import android.*;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.crash.FirebaseCrash;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.text.BreakIterator;

import de.fs.fintech.geogame.data.User;

public class LoginActivity extends AppCompatBaseActivity implements GoogleApiClient.OnConnectionFailedListener {

    private static final Logger log = LoggerFactory.getLogger(LoginActivity.class);
    private static final int RC_SIGN_IN = 43542;
    public static final String EXTRA_GOOGLE_ID = "google.id";
    public static final String EXTRA_INT_ID = "signed_in_id";
    public static final String PSEUDONYM = "pseudonym";

    private GoogleApiClient mGoogleApiClient;
    private FirebaseAuth mAuth;
    private String email;
    private String pseudonym;

    enum TermsSource {
        WEBSITE,
        INTL,
        ASSETS
    }

    private TermsSource termsSource = TermsSource.WEBSITE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }

        WebView webViewTerms = (WebView) findViewById(R.id.webViewTerms);
        switch (termsSource) {
            default:
            case WEBSITE:
                webViewTerms.loadUrl("http://www.frankfurt-school.de/home/legal.html");
                webViewTerms.setWebViewClient(new WebViewClient());
                WebSettings webSettings = webViewTerms.getSettings();
                webSettings.setJavaScriptEnabled(true);
                break;

            case INTL:
                String termsHtmlString = "<html><body>ERROR</body></html>";
                termsHtmlString = getString(R.string.terms_intl);
                webViewTerms.loadData(termsHtmlString, "text/html", null);
                break;

            case ASSETS:
                webViewTerms.loadUrl("file:///android_asset/terms.html");
                break;
        }

        final SignInButton buttonGoogleLogin = (SignInButton) findViewById(R.id.buttonGoogleLogin);
        buttonGoogleLogin.setEnabled(false);

        CheckBox acceptTerms = (CheckBox) findViewById(R.id.checkBoxTermsAccepted);
        acceptTerms.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                buttonGoogleLogin.setEnabled(isChecked);
                buttonGoogleLogin.setBackgroundColor(isChecked ? Color.RED : Color.GRAY);
            }
        });


        // Configure sign-in to request the user's ID, email address, and basic profile. ID and
        // basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // Firebase
                .requestEmail()
                .build();

        // Build a GintoogleApiClient with access to GoogleSignIn.API and the options above.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();



        // [START initialize_auth]
        mAuth = FirebaseAuth.getInstance();
        // [END initialize_auth]

        // Set the dimensions of the sign-in button.
        SignInButton signInButton = (SignInButton) findViewById(R.id.buttonGoogleLogin);
        signInButton.setSize(SignInButton.SIZE_STANDARD);
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
                startActivityForResult(signInIntent, RC_SIGN_IN);
            }
        });

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String displayName = prefs.getString("google.plus.displayName", null);
        if (displayName != null) {
            displaySignedIn(displayName);
        } else {
            // Show Logout Button...
        }

    }

    @Override
    /** for GoogleApiClient
     *
     */
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        log.info("Connection to G+/GoogleApiClient failed");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }
    }

    private void handleSignInResult(GoogleSignInResult result) {
        log.debug("handleSignInResult:" + result.isSuccess());
        if (result.isSuccess()) {
            // Signed in successfully, show authenticated UI.
            GoogleSignInAccount acct = result.getSignInAccount();
            String displayName=acct.getDisplayName();
            email=acct.getEmail();
            pseudonym=email.replace('.','°');

            displaySignedIn(displayName);

            SharedPreferences prefs= PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("google.plus.displayName",displayName);
            editor.putString("google.plus.email",email);
            editor.putString("google.plus.id",acct.getId());
            editor.putString(PSEUDONYM,pseudonym);
            editor.commit();

            /*
            FirebaseCrash.log("Sign In G+ "+email+" : "+displayName);
            FirebaseCrash.report(new Exception("My first Android non-fatal error"));
            */
            //updateUI(true);

            saveUserToDisk(email, displayName);

            firebaseAuthWithGoogle(acct);

        } else {
            // Signed out, show unauthenticated UI.
            //updateUI(false);
        }
    }

    private void displaySignedIn(String displayName) {
        String msg=getString(R.string.signed_in_fmt, displayName);
        Toast.makeText(this,msg,Toast.LENGTH_LONG).show();
        TextView mStatusTextView= (TextView) findViewById(R.id.loggedInAs);
        mStatusTextView.setText(msg);
    }

    private void saveUserToDisk(String email, String displayName) {

        User user=new User(email,displayName);
        File dump;
        boolean needsSpecialPermissions=ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        boolean publicData=true;
        if(needsSpecialPermissions) {
            /* WRITE_EXTERNAL_STORAGE & READ_EXTERNAL_STORAGE permissions
             * See ActivityCompat.requestPermissions
             * == /storage/emulated/0/dump.json
             */
            dump = new File(Environment.getExternalStorageDirectory(), "dump.json");
        } else if(publicData){
            /* ohne WRITE_EXTERNAL_STORAGE permissions
             * für große Files, die jeder sehen darf, ggf. auch auf seinen PC kopieren kann
             * == file dumped to /storage/emulated/0/Android/data/de.fs.fintech.geogame/files/ext_data/dump.json
             */
            dump = new File(((Context)this).getExternalFilesDir("ext_data"), "dump.json");
        } else {
            /* privater "sicherer" Telefonspeicher
             * ohne WRITE_EXTERNAL_STORAGE permissions
             * == file dumped to /data/user/0/de.fs.fintech.geogame/files/dump.json
             */
            dump = new File(((Context)this).getApplicationContext().getFilesDir(), "dump.json");

        }
        try {
            ObjectMapper mapper=new ObjectMapper();
            FileWriter out=new FileWriter(dump);
            mapper.writeValue(out,user);
            out.close();
            log.info("file dumped to "+dump.getAbsolutePath());
        } catch (Exception e) {
            log.error( "Error opening Log.", e);
        }
    }

    // Use when the user clicks a link from a web page in your WebView
    private class WebViewClient extends android.webkit.WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (Uri.parse(url).getHost().equals("www.frankfurt-school.de")) {
                return false;
            }

            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
            return true;
        }
    }

    // [START auth_with_google]
    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        log.debug("firebaseAuthWithGoogle:" + acct.getId());
        // [START_EXCLUDE silent]
        //showProgressDialog();
        // [END_EXCLUDE]

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        log.debug("signInWithCredential:onComplete:" + task.isSuccessful());

                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful()) {
                            log.warn("signInWithCredential", task.getException());
                            Toast.makeText(LoginActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                        // [START_EXCLUDE]
                        //hideProgressDialog();

                        Intent resultIntent=new Intent();
                        resultIntent.putExtra(EXTRA_GOOGLE_ID,email);
                        resultIntent.putExtra(EXTRA_INT_ID,pseudonym);

                        setResult(RESULT_OK,resultIntent);
                        finish();
                        // [END_EXCLUDE]
                    }
                });
    }
    // [END auth_with_google]

    // [START signin]
    private void signIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }
    // [END signin]
}
