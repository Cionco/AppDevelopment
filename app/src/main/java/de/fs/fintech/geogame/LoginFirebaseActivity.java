package de.fs.fintech.geogame;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fs.fintech.geogame.data.User;

public class LoginFirebaseActivity extends AppCompatBaseActivity implements GoogleApiClient.OnConnectionFailedListener {

    private static final Logger log = LoggerFactory.getLogger(LoginFirebaseActivity.class);
    private static final int RC_SIGN_IN = 43542;
    private static final String EXTRA_INT_ID = "signed_in_id";

    private GoogleApiClient mGoogleApiClient;

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
                .requestEmail()
                .build();

        // Build a GintoogleApiClient with access to GoogleSignIn.API and the options above.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

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

        SharedPreferences prefs= PreferenceManager.getDefaultSharedPreferences(this);
        String displayName = prefs.getString("google.plus.displayName",null);
        if(displayName!=null) {
            displaySignedIn(displayName);
        } else  {
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
        log.info("handleSignInResult:" + result.isSuccess());
        if (result.isSuccess()) {
            // Signed in successfully, show authenticated UI.
            GoogleSignInAccount acct = result.getSignInAccount();
            String displayName=acct.getDisplayName();
            String email=acct.getEmail();

            displaySignedIn(displayName);

            SharedPreferences prefs= PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("google.plus.displayName",displayName);
            editor.putString("google.plus.email",email);
            editor.putString("google.plus.id",acct.getId());
            editor.commit();

            loadOrCreateRtDbUser(email,displayName);

            Intent resultIntent=new Intent();
            resultIntent.putExtra(LoginActivity.EXTRA_GOOGLE_ID,email);
            resultIntent.putExtra(EXTRA_INT_ID,acct.getId());
            setResult(RESULT_OK,resultIntent);
            finish();
            //updateUI(true);
        } else {
            // Signed out, show unauthenticated UI.
            //updateUI(false);
        }
    }

    private void loadOrCreateRtDbUser(final String email, final String displayName) {

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        final String pseudonym=email.replace('.','°');
        final DatabaseReference myRef = database.getReference("geogame/users/"+pseudonym);

        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                log.info("user exists "+email+" => "+user.displayName);

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                log.info("new user  "+email+" => "+displayName);
                User user=new User();
                user.email=email;
                user.displayName=displayName;
                user.pseudonym=displayName.replaceAll(" ","_");

                myRef.setValue(user);
            }
        });



    }


    private void displaySignedIn(String displayName) {
        String msg=getString(R.string.signed_in_fmt, displayName);
        Toast.makeText(this,msg,Toast.LENGTH_LONG).show();
        TextView mStatusTextView= (TextView) findViewById(R.id.loggedInAs);
        mStatusTextView.setText(msg);
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
}
