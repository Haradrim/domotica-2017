package howest.nmct.be.securitysystem.View;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import howest.nmct.be.securitysystem.Adapter.LoginCodeAdapter;
import howest.nmct.be.securitysystem.Model.LoginCode;
import howest.nmct.be.securitysystem.Model.User;
import howest.nmct.be.securitysystem.Model.UserCode;
import howest.nmct.be.securitysystem.R;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.http.NextServiceFilterCallback;
import com.microsoft.windowsazure.mobileservices.http.OkHttpClientFactory;
import com.microsoft.windowsazure.mobileservices.http.ServiceFilter;
import com.microsoft.windowsazure.mobileservices.http.ServiceFilterRequest;
import com.microsoft.windowsazure.mobileservices.http.ServiceFilterResponse;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;
import com.microsoft.windowsazure.mobileservices.table.sync.MobileServiceSyncContext;
import com.microsoft.windowsazure.mobileservices.table.sync.localstore.ColumnDataType;
import com.microsoft.windowsazure.mobileservices.table.sync.localstore.MobileServiceLocalStoreException;
import com.microsoft.windowsazure.mobileservices.table.sync.localstore.SQLiteLocalStore;
import com.microsoft.windowsazure.mobileservices.table.sync.synchandler.SimpleSyncHandler;
import com.squareup.okhttp.OkHttpClient;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

//import iothub packages


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener{


    private MobileServiceClient mClient;

    private GoogleApiClient mGoogleApiClient;
    /**
     * Mobile Service Table used to access data
     */
    private MobileServiceTable<LoginCode> mLoginCodeTable;
    private  MobileServiceTable<User> mUserTable;
    private MobileServiceTable<UserCode> mUserCodeTable;

    //Offline Sync
    /**
     * Mobile Service Table used to access and Sync data
     */
    //private MobileServiceSyncTable<ToDoItem> mToDoTable;

    /**
     * Adapter to sync the items list with the view
     */
    private LoginCodeAdapter mAdapter;
    private ListView mListview;
    private SwipeRefreshLayout swipeRefreshLayout;
    /**
     * EditText containing the "New To Do" text
     */
    private TextView mTextTitle;
    private Button mButton;
    /**
     *
     *
     * Progress spinner to use for table operations
     */

    private  List<User> user;
    private ProgressBar mProgressBar;

    //logged in user
    private String mEmail;
    private boolean admin = false;
    private RelativeLayout coordinatorLayout;

    private ImageView mImageView;
    private TextView mTextViewEmail;
    private TextView mTextViewName;


    public static final int RC_SIGN_IN = 6602;
    public static final int RC_ADD_CODE = 80;

    public static final String TAG = "TAG";

    public MainActivity() {
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mProgressBar = (ProgressBar) findViewById(R.id.loadingProgressBar);

        mProgressBar.setVisibility(ProgressBar.GONE);

        try {
            // Create the Mobile Service Client instance, using the provided

            // Mobile Service URL and key
            mClient = new MobileServiceClient(
                    "http://securityapplicationservice20161229035920.azurewebsites.net/",
                    this).withFilter(new ProgressFilter());

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
            mUserTable = mClient.getTable(User.class);
            mUserCodeTable = mClient.getTable(UserCode.class);
            mLoginCodeTable = mClient.getTable(LoginCode.class);


            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .build();


            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                    .build();

            mGoogleApiClient.connect();



            signIn();

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            boolean Islogin = prefs.getBoolean("Islogin", true);
            if (Islogin) {
                refreshItemsFromTable(mEmail);
            } else {
                signIn();
            }

            initLocalStore().get();



            mAdapter = new LoginCodeAdapter(this, R.layout.row_list_to_do);
            mListview = (ListView) findViewById(R.id.listViewToDo);
            mListview.setAdapter(mAdapter);
            mListview.setLongClickable(true);

            mListview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    LoginCode loginCode = (LoginCode) mListview.getItemAtPosition(position);
                    int code = loginCode.GetCode();
                    Intent intent = new Intent(view.getContext(), QRActivity.class);
                    intent.putExtra("LoginCode", code);
                    startActivity(intent);
                }
            });

            swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeContainer);

            swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    fetchTimelineAsync(0);
                }
            });

            swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);


           /* mListview.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                    LoginCode loginCode = (LoginCode) mListview.getItemAtPosition(position);
                    checkItem(loginCode);
                    return false;

                }
            });*/



            DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                    this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
            drawer.setDrawerListener(toggle);
            toggle.syncState();

            NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
            navigationView.setNavigationItemSelectedListener(this);

    } catch (MalformedURLException e) {
        createAndShowDialog(new Exception("There was an error creating the Mobile Service. Verify the URL"), "Error");
    } catch (Exception e) {
        createAndShowDialog(e, "Error");
    }
    }

    public void fetchTimelineAsync(int page) {
        // Send the network request to fetch the updated data
        // `client` here is an instance of Android Async HTTP
        // getHomeTimeline is an example endpoint.

     refreshItemsFromTable(mEmail);
        swipeRefreshLayout.setRefreshing(false);
    }



    private void signIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void signOut(){
        Auth.GoogleSignInApi.signOut(mGoogleApiClient);
        mAdapter.clear();
        mListview.deferNotifyDataSetChanged();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit().putBoolean("Islogin", false).commit();
        boolean Islogin = prefs.getBoolean("Islogin", true);
        if(Islogin) {
            refreshItemsFromTable(mEmail);
        }
        else {
            signIn();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode){
            case RC_SIGN_IN:
                GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                prefs.edit().putBoolean("Islogin", true).commit();
                boolean Islogin = prefs.getBoolean("Islogin", true);
                if(Islogin) {
                    User user = new User();
                    try {
                        handleSignInResult(result);
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                else {
                    signIn();
                }
                break;
            case RC_ADD_CODE:
                break;
        }
    }


    private void handleSignInResult(GoogleSignInResult result) throws ExecutionException, InterruptedException {
        Log.d(TAG, "handleSignInResult:" + result.isSuccess());

        if (result.isSuccess()) {
            GoogleSignInAccount acct = result.getSignInAccount();
            mEmail = acct.getEmail();
            refreshItemsFromTable(mEmail);
            if(user != null) {
                if (user.isEmpty() == false) {
                    for (User us : user) {
                        if (us.getPersonEmail().equals(mEmail)) {
                            admin = true;
                        } else {
                            admin = false;
                        }
                    }
                }
            }
            //Bitmap bitmap = getImageBitmap(acct.getPhotoUrl().toString());
            //setIcon(bitmap);
            mTextViewEmail = (TextView) findViewById(R.id.textViewEmail);
            mTextViewEmail.setText(acct.getEmail());
            mTextViewName = (TextView) findViewById(R.id.textViewName);
            mTextViewName.setText(acct.getDisplayName());

        }
        else
        {
            Log.d(TAG, "NOT SUCCES");
        }
    }

    private void setIcon(Bitmap uri){
        mImageView = (ImageView) findViewById(R.id.imageView);
        mImageView.setImageURI(null);
        mImageView.setImageBitmap(uri);

    }

    private Bitmap getImageBitmap(String url) {
        Bitmap bm = null;
        try {
            URL aURL = new URL(url);
            URLConnection conn = aURL.openConnection();
            conn.connect();
            InputStream is = conn.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);
            bm = BitmapFactory.decodeStream(bis);
            bis.close();
            is.close();
        } catch (IOException e) {
            Log.e(TAG, "Error getting bitmap", e);
        }
        return bm;
    }

    public void AddCode(String email) throws ExecutionException, InterruptedException {
        Random random = new Random();
        LoginCode loginCode = new LoginCode();
        loginCode.setEmail(email);
        loginCode.setCode(random.nextInt(9999 - 1000) + 1000);
        mLoginCodeTable.insert(loginCode);
        mAdapter.add(loginCode);
        refreshItemsFromTable(mEmail);
    }

    public void checkItem(final LoginCode item) {
        if (mClient == null) {
            return;
        }

        // Set the item as completed and update it in the table

        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>(){
            @Override
            protected Void doInBackground(Void... params) {
                try {

                    checkItemInTable(item);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            mAdapter.remove(item);

                        }
                    });
                } catch (final Exception e) {
                    createAndShowDialogFromTask(e, "Error");
                }

                return null;
            }
        };

        runAsyncTask(task);

    }

    public void checkItemInTable(LoginCode item) throws ExecutionException, InterruptedException {
       /* Random random = new Random();
        LoginCode loginCode = new LoginCode();
        loginCode.setCode(random.nextInt(9999 - 1000) + 1000);
        mLoginCodeTable.insert(loginCode).get();*/
        mLoginCodeTable.delete(item);
    }

    private List<User> getUsers(){

        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    user = mUserTable.select().execute().get();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
                return null;
            }
        };
                runAsyncTask(task);
        return user;
    }

    private void refreshItemsFromTable(final String email) {

        // Get the items that weren't marked as completed and add them in the
        // adapter

        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>(){
            @Override
            protected Void doInBackground(Void... params) {

                try {
                    final List<LoginCode> results = refreshItemsFromMobileServiceTable(email);

                    //Offline Sync
                    //final List<ToDoItem> results = refreshItemsFromMobileServiceTableSyncTable();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mAdapter.clear();

                            for (LoginCode item : results) {
                                mAdapter.add(item);
                            }
                        }
                    });
                } catch (final Exception e){
                    createAndShowDialogFromTask(e, "Error");
                }

                return null;
            }
        };

        runAsyncTask(task);
    }

    /**
     * Refresh the list with the items in the Mobile Service Table
     */

    private List<LoginCode> refreshItemsFromMobileServiceTable(String email) throws ExecutionException, InterruptedException {
        user = mUserTable.select().execute().get();
        List<LoginCode> loginCodes = mLoginCodeTable.where().field("TargetEmail").eq(email).execute().get();
        return loginCodes;
    }

    private AsyncTask<Void, Void, Void> initLocalStore() throws MobileServiceLocalStoreException, ExecutionException, InterruptedException {

        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {

                    MobileServiceSyncContext syncContext = mClient.getSyncContext();

                    if (syncContext.isInitialized())
                        return null;

                    SQLiteLocalStore localStore = new SQLiteLocalStore(mClient.getContext(), "OfflineStore", null, 1);

                    Map<String, ColumnDataType> tableDefinition = new HashMap<String, ColumnDataType>();
                    tableDefinition.put("id", ColumnDataType.String);
                    tableDefinition.put("text", ColumnDataType.String);
                    tableDefinition.put("complete", ColumnDataType.Boolean);

                    localStore.defineTable("ToDoItem", tableDefinition);

                    SimpleSyncHandler handler = new SimpleSyncHandler();

                    syncContext.initialize(localStore, handler).get();

                } catch (final Exception e) {
                    createAndShowDialogFromTask(e, "Error");
                }

                return null;
            }
        };

        return runAsyncTask(task);
    }

    private void createAndShowDialogFromTask(final Exception exception, String title) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                createAndShowDialog(exception, "Error");
            }
        });
    }


    /**
     * Creates a dialog and shows it
     *
     * @param exception
     *            The exception to show in the dialog
     * @param title
     *            The dialog title
     */
    private void createAndShowDialog(Exception exception, String title) {
        Throwable ex = exception;
        if(exception.getCause() != null){
            ex = exception.getCause();
        }
        createAndShowDialog(ex.getMessage(), title);
    }

    private void createAndShowDialog(final String message, final String title) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage(message);
        builder.setTitle(title);
        builder.create().show();
    }

    private AsyncTask<Void, Void, Void> runAsyncTask(AsyncTask<Void, Void, Void> task) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            return task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            return task.execute();
        }
    }
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            signOut();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }



    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {

        } else if (id == R.id.nav_gallery)
        {
            if(admin == true) {
                Intent intent = new Intent(this, CommandActivity.class);
                intent.putExtra("IsAdmin",admin);
                startActivity(intent);
            }
            else {
                coordinatorLayout = (RelativeLayout) findViewById(R.id.adminRelative);
                Snackbar snackbar = Snackbar
                        .make(coordinatorLayout,"Only admin acces", Snackbar.LENGTH_LONG);
                snackbar.show();
            }
        }  else if (id == R.id.nav_manage) {
            if(admin == true) {
                Intent intent = new Intent(this, AllCodesActivity.class);
                intent.putExtra("IsAdmin",admin);
                startActivity(intent);
            }
            else {
                coordinatorLayout = (RelativeLayout) findViewById(R.id.adminRelative);
                Snackbar snackbar = Snackbar
                        .make(coordinatorLayout,"Only admin acces", Snackbar.LENGTH_LONG);
                snackbar.show();
            }

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {
            if(admin == true) {
                Intent intent = new Intent(this, AddCode.class);
                intent.putExtra("IsAdmin",admin);
                startActivity(intent);
            }
            else {
                coordinatorLayout = (RelativeLayout) findViewById(R.id.adminRelative);
                Snackbar snackbar = Snackbar
                        .make(coordinatorLayout,"Only admin acces", Snackbar.LENGTH_LONG);
                snackbar.show();
            }
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }



    private class ProgressFilter implements ServiceFilter {

        @Override
        public ListenableFuture<ServiceFilterResponse> handleRequest(ServiceFilterRequest request, NextServiceFilterCallback nextServiceFilterCallback) {

            final SettableFuture<ServiceFilterResponse> resultFuture = SettableFuture.create();


            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    if (mProgressBar != null) mProgressBar.setVisibility(ProgressBar.VISIBLE);
                }
            });

            ListenableFuture<ServiceFilterResponse> future = nextServiceFilterCallback.onNext(request);

            Futures.addCallback(future, new FutureCallback<ServiceFilterResponse>() {
                @Override
                public void onFailure(Throwable e) {
                    resultFuture.setException(e);
                }

                @Override
                public void onSuccess(ServiceFilterResponse response) {
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            if (mProgressBar != null) mProgressBar.setVisibility(ProgressBar.GONE);
                        }
                    });

                    resultFuture.set(response);
                }
            });

            return resultFuture;
        }
    }
}
