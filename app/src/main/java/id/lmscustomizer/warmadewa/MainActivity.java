package id.lmscustomizer.warmadewa;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Locale;

public class MainActivity extends Activity {
    private WebView web;
    private ProgressBar bar;
    private final Uploads uploads = new Uploads();
    private boolean connectingCal = false;
    private boolean autoClicked = false;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(0xFF1747A6);

        FrameLayout root = new FrameLayout(this);
        web = new WebView(this);
        root.addView(web, new FrameLayout.LayoutParams(-1, -1));

        bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        root.addView(bar, new FrameLayout.LayoutParams(-1, dp(3), Gravity.TOP));

        ImageButton fab = new ImageButton(this);
        fab.setImageResource(R.drawable.fab_cat);
        fab.setBackgroundColor(Color.TRANSPARENT);
        fab.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
        fab.setPadding(0, 0, 0, 0);
        fab.setAlpha(0.92f);
        fab.setContentDescription("Pengaturan");
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(dp(52), dp(52), Gravity.BOTTOM | Gravity.START);
        lp.leftMargin = dp(12);
        lp.bottomMargin = dp(16);
        root.addView(fab, lp);
        fab.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));

        setContentView(root);
        setupWebView();

        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 11);
        }

        SyncJobService.ensure(this);
        long stale = System.currentTimeMillis() - Store.lastSync(this);
        if (!Store.load(this).optString("calUrl", "").isEmpty() && stale > 60L * 60 * 1000) {
            new Thread(() -> Sync.run(getApplicationContext())).start();
        }

        if (!route(getIntent())) web.loadUrl(Store.HOME);
    }

    private void setupWebView() {
        if ((getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(false);
        CookieManager cm = CookieManager.getInstance();
        cm.setAcceptCookie(true);
        cm.setAcceptThirdPartyCookies(web, true);

        web.setWebViewClient(new Client());
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView v, int p) {
                bar.setProgress(p);
                bar.setVisibility(p >= 100 ? android.view.View.GONE : android.view.View.VISIBLE);
                if (p >= 15) injectIfLms(v.getUrl());
            }

            @Override
            public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams p) {
                return uploads.show(MainActivity.this, cb, p);
            }
        });
        web.setDownloadListener((url, ua, cd, mime, len) -> download(url, cd, mime));
    }

    // ------------------------------------------------------------------ navigasi
    private class Client extends WebViewClient {
        @Override
        public void onPageStarted(WebView v, String url, Bitmap icon) {
            super.onPageStarted(v, url, icon);
            injectIfLms(url);
        }

        @Override
        public void onPageFinished(WebView v, String url) {
            super.onPageFinished(v, url);
            injectIfLms(url);
            sniffCalendar(url);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
            return handleUrl(r.getUrl());
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
            Uri u = r.getUrl();
            if (Store.BG_PATH.equals(u.getPath()) && Store.isLmsHost(u.getHost()) && Store.hasBgFile(MainActivity.this)) {
                try {
                    return new WebResourceResponse(Store.bgMime(MainActivity.this), null,
                            new FileInputStream(Store.bgFile(MainActivity.this)));
                } catch (Exception ignored) {
                }
            }
            return null;
        }
    }

    private boolean handleUrl(Uri u) {
        String scheme = u.getScheme() == null ? "" : u.getScheme();
        if (!scheme.equals("http") && !scheme.equals("https")) {
            openExternal(u);
            return true;
        }
        String host = u.getHost() == null ? "" : u.getHost();
        boolean lms = Store.isLmsHost(host);
        boolean auth = host.equals("accounts.google.com") || host.equals("login.microsoftonline.com")
                || host.equals("login.live.com");
        if (lms || auth) {
            if (lms && isDocPath(u.getPath())) {
                download(u.toString(), null, null);
                return true;
            }
            return false;
        }
        openExternal(u);
        return true;
    }

    private static boolean isDocPath(String path) {
        if (path == null) return false;
        String p = path.toLowerCase(Locale.ROOT);
        String[] ext = {".pdf", ".doc", ".docx", ".ppt", ".pptx", ".xls", ".xlsx", ".zip", ".rar", ".7z", ".csv"};
        for (String e : ext) if (p.endsWith(e)) return true;
        return false;
    }

    private void openExternal(Uri u) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, u));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Tidak ada aplikasi untuk membuka tautan ini", Toast.LENGTH_SHORT).show();
        }
    }

    private void download(String url, String cd, String mime) {
        try {
            DownloadManager.Request r = new DownloadManager.Request(Uri.parse(url));
            String ck = CookieManager.getInstance().getCookie(url);
            if (ck != null) r.addRequestHeader("Cookie", ck);
            r.addRequestHeader("User-Agent", web.getSettings().getUserAgentString());
            String name = URLUtil.guessFileName(url, cd, mime);
            r.setTitle(name);
            r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            r.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name);
            ((DownloadManager) getSystemService(DOWNLOAD_SERVICE)).enqueue(r);
            Toast.makeText(this, "Mengunduh " + name, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Gagal mengunduh file", Toast.LENGTH_SHORT).show();
        }
    }

    // ------------------------------------------------------------------ tema (suntik JS)
    private String asset(String name) {
        try (InputStream in = getAssets().open(name); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return out.toString("UTF-8");
        } catch (Exception e) {
            return "";
        }
    }

    private String shimAndContent;

    private void injectIfLms(String url) {
        if (url == null || web == null) return;
        Uri u = Uri.parse(url);
        if (!Store.isLmsHost(u.getHost())) return;
        if (shimAndContent == null) shimAndContent = asset("shim_page.js") + "\n" + asset("content.js");
        String js = "(function(){try{if(!document.documentElement||window.__lmscLoaded)return;"
                + "window.__lmscLoaded=true;window.__LMSC_SETTINGS=" + Store.forPage(this).toString() + ";"
                + shimAndContent + "\n}catch(e){}})();";
        web.evaluateJavascript(js, null);
    }

    private void pushSettings() {
        web.evaluateJavascript("window.__lmscUpdate&&window.__lmscUpdate(" + Store.forPage(this).toString() + ")", null);
    }

    // ------------------------------------------------------------------ kalender
    private void sniffCalendar(String url) {
        if (url == null || !url.contains("/calendar/export")) return;
        web.evaluateJavascript(asset("scan_calendar.js"), value -> {
            String link = unquote(value);
            if (link.startsWith("https://")) {
                try {
                    JSONObject p = new JSONObject();
                    p.put("calUrl", link);
                    Store.merge(this, p);
                } catch (Exception ignored) {
                }
                connectingCal = false;
                Toast.makeText(this, "\u2705 Kalender terhubung! Pengingat sedang diatur\u2026", Toast.LENGTH_LONG).show();
                new Thread(() -> Sync.run(getApplicationContext())).start();
            } else if (connectingCal && !autoClicked) {
                autoClicked = true;
                web.evaluateJavascript(asset("autoclick_calendar.js"), null);
                Toast.makeText(this, "Jika link belum muncul, ketuk \"Get calendar URL\"", Toast.LENGTH_LONG).show();
            }
        });
    }

    private static String unquote(String value) {
        try {
            String s = new JSONArray("[" + value + "]").optString(0, "");
            return "null".equals(s) ? "" : s;
        } catch (Exception e) {
            return "";
        }
    }

    // ------------------------------------------------------------------ intent & siklus hidup
    private boolean route(Intent i) {
        if (i == null) return false;
        if (i.getBooleanExtra("connectCal", false)) {
            connectingCal = true;
            autoClicked = false;
            i.removeExtra("connectCal");
            web.loadUrl(Store.CAL_EXPORT);
            return true;
        }
        String u = i.getStringExtra("url");
        if (u != null && u.startsWith("https://") && Store.isLmsHost(Uri.parse(u).getHost())) {
            i.removeExtra("url");
            web.loadUrl(u);
            return true;
        }
        return false;
    }

    @Override
    protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        setIntent(i);
        route(i);
    }

    @Override
    protected void onResume() {
        super.onResume();
        pushSettings();
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        uploads.onResult(req, res, data);
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        web.destroy();
        super.onDestroy();
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
