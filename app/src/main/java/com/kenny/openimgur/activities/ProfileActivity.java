package com.kenny.openimgur.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.kenny.openimgur.R;
import com.kenny.openimgur.api.ApiClient;
import com.kenny.openimgur.api.OAuthInterceptor;
import com.kenny.openimgur.api.responses.UserResponse;
import com.kenny.openimgur.classes.ImgurUser;
import com.kenny.openimgur.fragments.ProfileAlbumsFragment;
import com.kenny.openimgur.fragments.ProfileCommentsFragment;
import com.kenny.openimgur.fragments.ProfileFavoritesFragment;
import com.kenny.openimgur.fragments.ProfileInfoFragment;
import com.kenny.openimgur.fragments.ProfileSubmissionsFragment;
import com.kenny.openimgur.fragments.ProfileUploadsFragment;
import com.kenny.openimgur.services.AlarmReceiver;
import com.kenny.openimgur.ui.ViewPager;
import com.kenny.openimgur.util.LogUtil;
import com.kenny.openimgur.util.SqlHelper;
import com.kenny.openimgur.util.ViewUtils;
import com.kennyc.view.MultiStateView;

import butterknife.BindView;
import butterknife.OnClick;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by kcampagna on 12/14/14.
 */
public class ProfileActivity extends BaseActivity {
    private static final String IMGUR_LOGIN_URL = "https://imgur.com/signin";

    private static final String IMGUR_ROOT_URL = "https://imgur.com";

    private static final String IMGUR_API_URL = "https://api.imgur.com";

    private static final String IMGUR_MOBILE_URL = "https://m.imgur.com";

    private static final String IMGUR_COOKIES_PREF = "imgur_cookies";

    public static final String KEY_LOGGED_IN = "logged_in";

    public static final String KEY_LOGGED_OUT = "logged_out";

    private static final String KEY_USERNAME = "username";

    private static final String KEY_USER = "user";

    private static final String KEY_IS_LOGGING_IN = "is_logging_in";

    private static final String KEY_WAITING_FOR_TOKEN = "waiting_for_token";

    @BindView(R.id.slidingTabs)
    TabLayout mSlidingTabs;

    @BindView(R.id.pager)
    ViewPager mPager;

    @BindView(R.id.multiView)
    MultiStateView mMultiView;

    @BindView(R.id.toolBar)
    Toolbar mToolBar;

    ImgurUser mSelectedUser;

    ProfilePager mAdapter;

    private boolean mWaitingForToken;

    public static Intent createIntent(Context context, @Nullable String userName) {
        return createIntent(context, userName, false);
    }

    public static Intent createIntent(@NonNull Context context, @Nullable String userName, boolean isLoggingIn) {
        Intent intent = new Intent(context, ProfileActivity.class);
        if (!TextUtils.isEmpty(userName)) intent.putExtra(KEY_USERNAME, userName);
        return intent.putExtra(KEY_IS_LOGGING_IN, isLoggingIn);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        setStatusBarColorResource(theme.darkColor);
        setupToolBar();

        if (savedInstanceState != null) {
            mWaitingForToken = savedInstanceState.getBoolean(KEY_WAITING_FOR_TOKEN, false);
        }

        handleData(savedInstanceState, getIntent());
    }

    /**
     * Sets up the tool bar to take the place of the action bar
     */
    private void setupToolBar() {
        setSupportActionBar(mToolBar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
    }

    private void handleData(Bundle savedInstanceState, Intent args) {
        if (savedInstanceState == null) {
            if (args.hasExtra(KEY_USERNAME)) {
                LogUtil.v(TAG, "User present in Bundle extras");
                String username = args.getStringExtra(KEY_USERNAME);
                mSelectedUser = SqlHelper.getInstance(getApplicationContext()).getUser(username);
                configUser(username);
            } else if (user != null) {
                LogUtil.v(TAG, "User already logged in");
                mSelectedUser = user;
                configUser(null);
            } else {
                LogUtil.v(TAG, "No user present. Showing Login screen");
                configWebView();
            }
        } else if (savedInstanceState.containsKey(KEY_USER)) {
            mSelectedUser = savedInstanceState.getParcelable(KEY_USER);
            configUser(null);
        }
    }

    private void configUser(String username) {
        mMultiView.setViewState(MultiStateView.VIEW_STATE_LOADING);
        // Load the new user data if we haven't viewed the user within 24 hours
        if (mSelectedUser == null || System.currentTimeMillis() - mSelectedUser.getLastSeen() >= DateUtils.DAY_IN_MILLIS) {
            LogUtil.v(TAG, "Selected user is null or data is too old, fetching new data");
            String userName = mSelectedUser == null ? username : mSelectedUser.getUsername();
            getSupportActionBar().setTitle(userName);
            fetchProfile(userName);
        } else {
            LogUtil.v(TAG, "Selected user present in database and has valid data");
            mAdapter = new ProfilePager(getApplicationContext(), getFragmentManager(), mSelectedUser);
            mPager.setAdapter(mAdapter);
            mSlidingTabs.setupWithViewPager(mPager);
            mMultiView.setViewState(MultiStateView.VIEW_STATE_CONTENT);
            getSupportActionBar().setTitle(mSelectedUser.getUsername());
            supportInvalidateOptionsMenu();
        }
    }

    public void onUserLogout() {
        mSelectedUser = null;
        app.getPreferences().edit().remove(IMGUR_COOKIES_PREF).apply();
        CookieManager.getInstance().removeAllCookies(null);
        CookieManager.getInstance().flush();
        app.onLogout();
        configWebView();
        setResult(Activity.RESULT_OK, new Intent().putExtra(KEY_LOGGED_OUT, true));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.profile, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.logout).setVisible(mSelectedUser != null && mSelectedUser.isSelf(app));
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.logout:
                new AlertDialog.Builder(ProfileActivity.this, theme.getAlertDialogTheme())
                        .setTitle(R.string.logout)
                        .setMessage(R.string.logout_confirm)
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                onUserLogout();
                            }
                        }).show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Configures the webview to handle a user logging in
     */
    private void configWebView() {
        getSupportActionBar().hide();
        mMultiView.setViewState(MultiStateView.VIEW_STATE_LOADING);
        mMultiView.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isFinishing() && !isDestroyed()) {
                    showWebViewOAuth();
                }
            }
        }, 100);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void showWebViewOAuth() {
        final WebView webView = new WebView(this);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.setAcceptThirdPartyCookies(webView, true);
        }

        mWaitingForToken = true;
        
        final AlertDialog dialog = new AlertDialog.Builder(this, theme.getAlertDialogTheme())
                .setTitle(R.string.login_msg)
                .setView(webView)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mWaitingForToken = false;
                    }
                })
                .create();

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                maybeHandleWebLoginComplete(url, dialog);
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                maybeHandleWebLoginComplete(url, dialog);
            }
        });
        
        webView.loadUrl(IMGUR_LOGIN_URL);
        dialog.show();
    }

    private void maybeHandleWebLoginComplete(@Nullable String url, @NonNull AlertDialog dialog) {
        if (!mWaitingForToken || TextUtils.isEmpty(url) || !url.contains("imgur.com")) {
            return;
        }

        String cookies = getImgurCookies();
        if (TextUtils.isEmpty(cookies) || !hasLoggedInCookie(cookies)) {
            return;
        }

        if (dialog.isShowing()) {
            dialog.dismiss();
        }

        saveCookiesAndLogin(cookies, extractUsernameFromUrl(url));
    }

    private String getImgurCookies() {
        CookieManager cookieManager = CookieManager.getInstance();
        String rootCookies = cookieManager.getCookie(IMGUR_ROOT_URL);
        String apiCookies = cookieManager.getCookie(IMGUR_API_URL);
        String mobileCookies = cookieManager.getCookie(IMGUR_MOBILE_URL);

        StringBuilder mergedCookies = new StringBuilder();
        appendCookies(mergedCookies, rootCookies);
        appendCookies(mergedCookies, apiCookies);
        appendCookies(mergedCookies, mobileCookies);
        return mergedCookies.toString();
    }

    private void appendCookies(StringBuilder builder, @Nullable String cookies) {
        if (TextUtils.isEmpty(cookies)) {
            return;
        }

        if (builder.length() > 0) {
            builder.append("; ");
        }
        builder.append(cookies);
    }

    private boolean hasLoggedInCookie(@NonNull String cookies) {
        String[] parts = cookies.split(";");
        for (String part : parts) {
            int separator = part.indexOf('=');
            if (separator <= 0) {
                continue;
            }

            String cookieName = part.substring(0, separator).trim().toLowerCase();
            if (cookieName.contains("session")
                    || cookieName.contains("auth")
                    || cookieName.contains("token")
                    || cookieName.equals("sid")) {
                return true;
            }
        }

        return false;
    }

    @Nullable
    private String extractUsernameFromUrl(@NonNull String url) {
        try {
            Uri uri = Uri.parse(url);
            String path = uri.getPath();
            if (TextUtils.isEmpty(path)) {
                return null;
            }

            String[] split = path.split("/");
            for (int i = 0; i < split.length - 1; i++) {
                if (("user".equalsIgnoreCase(split[i]) || "account".equalsIgnoreCase(split[i])) && !TextUtils.isEmpty(split[i + 1])) {
                    return split[i + 1];
                }
            }
        } catch (Throwable ignored) {
        }

        return null;
    }

    private void saveCookiesAndLogin(String cookies, @Nullable String usernameHint) {
        mWaitingForToken = false;
        mMultiView.setViewState(MultiStateView.VIEW_STATE_LOADING);

        app.getPreferences().edit().putString(IMGUR_COOKIES_PREF, cookies).apply();

        fetchCurrentUserFromCookies(cookies, usernameHint);
    }

    private void fetchCurrentUserFromCookies(final String cookies, @Nullable final String usernameHint) {
        ApiClient.getService().getProfile("me").enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                if (isDestroyed() || isFinishing()) return;

                if (response != null && response.body() != null && response.body().data != null) {
                    completeCookieLogin(response.body().data.getUsername());
                } else if (!TextUtils.isEmpty(usernameHint)) {
                    completeCookieLogin(usernameHint);
                } else {
                    ViewUtils.setErrorText(mMultiView, R.id.errorMessage, R.string.login_pin_failed);
                    mMultiView.setViewState(MultiStateView.VIEW_STATE_ERROR);
                }
            }

            @Override
            public void onFailure(Call<UserResponse> call, Throwable t) {
                if (isDestroyed() || isFinishing()) return;
                if (!TextUtils.isEmpty(usernameHint)) {
                    completeCookieLogin(usernameHint);
                } else {
                    ViewUtils.setErrorText(mMultiView, R.id.errorMessage, ApiClient.getErrorCode(t));
                    mMultiView.setViewState(MultiStateView.VIEW_STATE_ERROR);
                }
            }
        });
    }

    private void completeCookieLogin(@NonNull String username) {
        String fakeAccessToken = "cookie_session_" + System.currentTimeMillis();
        String fakeRefreshToken = "cookie_refresh_" + System.currentTimeMillis();
        long expiration = System.currentTimeMillis() + (365L * DateUtils.DAY_IN_MILLIS);

        ImgurUser newUser = new ImgurUser(username, fakeAccessToken, fakeRefreshToken, expiration);
        app.setUser(newUser);
        user = newUser;
        mSelectedUser = newUser;
        OAuthInterceptor.setAccessToken(fakeAccessToken);
        LogUtil.v(TAG, "User " + newUser.getUsername() + " logged in via cookies");
        getSupportActionBar().show();
        getSupportActionBar().setTitle(newUser.getUsername());
        setResult(Activity.RESULT_OK, new Intent().putExtra(KEY_LOGGED_IN, true));
        AlarmReceiver.createNotificationAlarm(getApplicationContext());
        fetchProfile(newUser.getUsername());
    }

    @OnClick(R.id.errorButton)
    public void retryClick() {
        if (mSelectedUser != null) {
            fetchProfile(mSelectedUser.getUsername());
        } else {
            configWebView();
        }
    }

    void fetchProfile(final String username) {
        ApiClient.getService().getProfile(username).enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                if (isDestroyed() || isFinishing()) return;

                if (response != null && response.body() != null) {
                    UserResponse userResponse = response.body();

                    if (mSelectedUser != null) {
                        mSelectedUser.copy(userResponse.data);
                    } else {
                        mSelectedUser = userResponse.data;
                    }

                    mSelectedUser.setLastSeen(System.currentTimeMillis());

                    if (mSelectedUser.isSelf(app) && !TextUtils.isEmpty(mSelectedUser.getAccessToken())) {
                        SqlHelper.getInstance(getApplicationContext()).updateUserInfo(mSelectedUser);
                        Intent intent = getIntent();

                        // If we are logging in for an upload, return
                        if (intent != null && intent.getBooleanExtra(KEY_IS_LOGGING_IN, false)) {
                            finish();
                            return;
                        }
                    } else {
                        SqlHelper.getInstance(getApplicationContext()).insertProfile(mSelectedUser);
                    }


                    mAdapter = new ProfilePager(getApplicationContext(), getFragmentManager(), mSelectedUser);
                    mPager.setAdapter(mAdapter);
                    mSlidingTabs.setupWithViewPager(mPager);
                    mMultiView.setViewState(MultiStateView.VIEW_STATE_CONTENT);
                    supportInvalidateOptionsMenu();
                } else {
                    ViewUtils.setErrorText(mMultiView, R.id.errorMessage, getString(R.string.profile_not_found, username));
                    mMultiView.setViewState(MultiStateView.VIEW_STATE_ERROR);
                }
            }

            @Override
            public void onFailure(Call<UserResponse> call, Throwable t) {
                ViewUtils.setErrorText(mMultiView, R.id.errorMessage, ApiClient.getErrorCode(t));
                mMultiView.setViewState(MultiStateView.VIEW_STATE_ERROR);
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mSelectedUser != null) {
            outState.putParcelable(KEY_USER, mSelectedUser);
        }

        outState.putBoolean(KEY_WAITING_FOR_TOKEN, mWaitingForToken);
    }





    @Override
    protected int getStyleRes() {
        return theme.isDarkTheme ? R.style.Theme_Opengur_Dark_Main_Dark : R.style.Theme_Opengur_Light_Main_Light;
    }

    private static class ProfilePager extends FragmentStatePagerAdapter {
        private static final int NUM_PAGES_SELF = 6;

        private static final int NUM_PAGES_FAR = 5;

        private String[] mTitles;

        private ImgurUser mUser;

        private int mNumPages;

        public ProfilePager(Context context, FragmentManager fm, ImgurUser user) {
            super(fm);
            mTitles = context.getResources().getStringArray(R.array.profile_tabs);
            mUser = user;
            mNumPages = mUser.isSelf() ? NUM_PAGES_SELF : NUM_PAGES_FAR;
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                // Info
                case 0:
                    return ProfileInfoFragment.createInstance(mUser);

                // Submissions
                case 1:
                    return ProfileSubmissionsFragment.newInstance(mUser);

                // Favorites
                case 2:
                    return ProfileFavoritesFragment.newInstance(mUser);

                // Albums
                case 3:
                    return ProfileAlbumsFragment.createInstance(mUser);

                // Comments
                case 4:
                    return ProfileCommentsFragment.createInstance(mUser);

                // Uploads
                case 5:
                    return new ProfileUploadsFragment();

                default:
                    throw new IndexOutOfBoundsException("How did this happen?");
            }
        }

        @Override
        public int getCount() {
            return mNumPages;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mTitles[position];
        }
    }
}