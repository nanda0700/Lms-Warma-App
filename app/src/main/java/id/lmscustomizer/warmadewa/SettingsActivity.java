package id.lmscustomizer.warmadewa;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/** Layar pengaturan: halaman lokal (assets/settings) yang terhubung ke penyimpanan lewat AndroidBridge. */
public class SettingsActivity extends Activity {
    private WebView web;
    private final Uploads uploads = new Uploads();

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(0xFF1747A6);
        web = new WebView(this);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(false);

        web.addJavascriptInterface(new Bridge(), "AndroidBridge");
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, r.getUrl()));
                } catch (ActivityNotFoundException ignored) {
                }
                return true;
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams p) {
                return uploads.show(SettingsActivity.this, cb, p);
            }
        });
        web.loadUrl("file:///android_asset/settings/popup.html");
    }

    private class Bridge {
        @JavascriptInterface
        public String getAll() {
            return Store.forSettings(SettingsActivity.this).toString();
        }

        @JavascriptInterface
        public void setAll(String json) {
            try {
                JSONObject patch = new JSONObject(json);
                Store.merge(SettingsActivity.this, patch);
                boolean cal = patch.has("calUrl");
                if (cal || patch.has("remind") || patch.has("absenLead") || patch.has("tugasLead")) {
                    new Thread(() -> {
                        if (cal) Sync.run(getApplicationContext());
                        else Reminders.schedule(getApplicationContext());
                    }).start();
                }
            } catch (Exception ignored) {
            }
        }

        @JavascriptInterface
        public void clearAll() {
            Store.clear(SettingsActivity.this);
        }

        @JavascriptInterface
        public String status() {
            try {
                JSONObject o = new JSONObject();
                o.put("connected", !Store.load(SettingsActivity.this).optString("calUrl", "").isEmpty());
                o.put("lastSync", Store.lastSync(SettingsActivity.this));
                o.put("count", Store.loadEvents(SettingsActivity.this).size());
                return o.toString();
            } catch (Exception e) {
                return "{}";
            }
        }

        @JavascriptInterface
        public String upcoming() {
            JSONArray a = new JSONArray();
            try {
                long now = System.currentTimeMillis();
                List<Ev> list = Store.loadEvents(SettingsActivity.this);
                java.util.Collections.sort(list, (x, y) -> Long.compare(x.start, y.start));
                for (Ev e : list) {
                    if (e.start < now || a.length() >= 8) continue;
                    JSONObject o = new JSONObject();
                    o.put("title", e.title);
                    o.put("course", e.course);
                    o.put("start", e.start);
                    o.put("type", Reminders.type(e));
                    a.put(o);
                }
            } catch (Exception ignored) {
            }
            return a.toString();
        }

        @JavascriptInterface
        public void syncNow() {
            new Thread(() -> {
                final boolean ok = Sync.run(getApplicationContext());
                runOnUiThread(() -> web.evaluateJavascript("window.__onSync&&window.__onSync(" + ok + ")", null));
            }).start();
        }

        @JavascriptInterface
        public void testNotification() {
            Reminders.show(getApplicationContext(), 777, "\uD83D\uDC31 Tes pengingat",
                    "Kalau ini muncul, notifikasi absen & tugas sudah siap!", "");
        }

        @JavascriptInterface
        public void connectCalendar() {
            runOnUiThread(() -> {
                startActivity(new Intent(SettingsActivity.this, MainActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        .putExtra("connectCal", true));
                finish();
            });
        }

        @JavascriptInterface
        public void close() {
            runOnUiThread(() -> finish());
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        uploads.onResult(req, res, data);
    }

    @Override
    protected void onDestroy() {
        web.destroy();
        super.onDestroy();
    }
}
