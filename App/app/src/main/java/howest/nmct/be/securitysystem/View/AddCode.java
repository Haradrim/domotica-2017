package howest.nmct.be.securitysystem.View;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;

import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.http.OkHttpClientFactory;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;
import com.squareup.okhttp.OkHttpClient;

import java.net.MalformedURLException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import howest.nmct.be.securitysystem.Model.LoginCode;
import howest.nmct.be.securitysystem.Model.User;
import howest.nmct.be.securitysystem.Model.UserCode;
import howest.nmct.be.securitysystem.R;

public class AddCode extends AppCompatActivity {
    private MobileServiceClient mClient;
    private MobileServiceTable<LoginCode> mLoginCodeTable;


    private Button mButton;
    private EditText mEditText;
    private String mEmail;
    private RelativeLayout coordinatorLayout;
    public static final int RC_ADD_CODE = 80;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_code);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Send code to guest");


        try {
            // Create the Mobile Service Client instance, using the provided

            // Mobile Service URL and key
            mClient = new MobileServiceClient(
                    "http://securityapplicationservice20161229035920.azurewebsites.net/",
                    this);

                    // Extend timeout from default of 10s to 20s
            mClient.setAndroidHttpClientFactory(new OkHttpClientFactory() {
                @Override
                public OkHttpClient createOkHttpClient() {
                    OkHttpClient client = new OkHttpClient();
                    client.setReadTimeout(20, TimeUnit.SECONDS);
                    client.setWriteTimeout(20, TimeUnit.SECONDS);
                    return client;
                }
            });


            mLoginCodeTable = mClient.getTable(LoginCode.class);


        } catch (MalformedURLException e) {
        } catch (Exception e) {
        }

        mButton = (Button) findViewById(R.id.buttonAddCode);
        mEditText = (EditText) findViewById(R.id.editTextCode);

            mButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(v.getContext(), AllCodesActivity.class);
                    mEmail = mEditText.getText().toString();
                    if (mEmail.matches("") == false) {
                        Random random = new Random();
                        LoginCode loginCode = new LoginCode();
                        loginCode.setEmail(mEmail);
                        loginCode.setCode(random.nextInt(9999 - 1000) + 1000);
                        mLoginCodeTable.insert(loginCode);
                        startActivity(intent);
                    } else {
                        coordinatorLayout = (RelativeLayout) findViewById(R.id.relative);
                        Snackbar snackbar = Snackbar
                                .make(coordinatorLayout,"Email is empty", Snackbar.LENGTH_LONG);

                        snackbar.show();
                    }
                }
            });
        }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if(id == android.R.id.home){
            onBackPressed(); return true;
        }
        return  super.onOptionsItemSelected(item);
    }

    }




