package id.lmscustomizer.warmadewa;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;

/** Pemilih file untuk unggah tugas dan memilih gambar latar. */
final class Uploads {
    static final int REQ = 4242;
    private ValueCallback<Uri[]> cb;

    boolean show(Activity a, ValueCallback<Uri[]> callback, WebChromeClient.FileChooserParams params) {
        if (cb != null) cb.onReceiveValue(null);
        cb = callback;
        try {
            a.startActivityForResult(params.createIntent(), REQ);
            return true;
        } catch (Exception e) {
            cb = null;
            return false;
        }
    }

    void onResult(int req, int res, Intent data) {
        if (req != REQ || cb == null) return;
        cb.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(res, data));
        cb = null;
    }
}
