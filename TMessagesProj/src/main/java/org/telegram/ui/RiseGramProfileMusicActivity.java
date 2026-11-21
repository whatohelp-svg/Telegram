package org.telegram.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BackDrawable;
import org.telegram.ui.ActionBar.BaseFragment;

/** In-app RiseGram profile music: uses the same authenticated account API as desktop. */
public class RiseGramProfileMusicActivity extends BaseFragment {
    private static final String URL = "https://risegram-risegram.up.railway.app/desktop#music";
    private static final int PICK_AUDIO_REQUEST = 5091;
    private WebView browser;
    private ValueCallback<Uri[]> fileCallback;

    @Override protected boolean useSimpleActionBar() { return true; }

    @Override public View createView(Context context) {
        setHasOwnBackground(true);
        simpleActionBar.setBackButtonDrawable(new BackDrawable(false));
        simpleActionBar.setTitle("Музыка профиля");
        simpleActionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override public void onItemClick(int id) {
                if (id == -1) finishFragment();
            }
        });
        browser = new WebView(context);
        browser.setBackgroundColor(0xFF0C1521);
        browser.getSettings().setJavaScriptEnabled(true);
        browser.getSettings().setDomStorageEnabled(true);
        browser.getSettings().setAllowFileAccess(false);
        browser.getSettings().setAllowContentAccess(true);
        browser.getSettings().setJavaScriptCanOpenWindowsAutomatically(false);
        browser.getSettings().setMediaPlaybackRequiresUserGesture(true);
        browser.getSettings().setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        browser.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if ("https".equals(uri.getScheme())
                    && "risegram-risegram.up.railway.app".equals(uri.getHost())
                    && "/desktop".equals(uri.getPath())) return false;
                if ("https".equals(uri.getScheme())) {
                    try { context.startActivity(new Intent(Intent.ACTION_VIEW, uri)); }
                    catch (Exception ignored) { }
                }
                return true;
            }
        });
        browser.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView webView,
                ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                try {
                    Intent intent = params.createIntent();
                    intent.setType("audio/*");
                    startActivityForResult(intent, PICK_AUDIO_REQUEST);
                    return true;
                } catch (Exception e) {
                    fileCallback = null;
                    Toast.makeText(context, "Выбор файла недоступен", Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
        });
        browser.loadUrl(URL);
        fragmentView = browser;
        return fragmentView;
    }

    @Override public void onActivityResultFragment(int requestCode, int resultCode, Intent data) {
        super.onActivityResultFragment(requestCode, resultCode, data);
        if (requestCode != PICK_AUDIO_REQUEST || fileCallback == null) return;
        Uri[] result = resultCode == Activity.RESULT_OK && data != null && data.getData() != null
            ? new Uri[]{data.getData()} : null;
        fileCallback.onReceiveValue(result);
        fileCallback = null;
    }

    @Override public void onFragmentDestroy() {
        if (fileCallback != null) {
            fileCallback.onReceiveValue(null);
            fileCallback = null;
        }
        if (browser != null) {
            browser.stopLoading();
            browser.destroy();
            browser = null;
        }
        super.onFragmentDestroy();
    }
}
