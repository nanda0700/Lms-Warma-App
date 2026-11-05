package id.lmscustomizer.warmadewa;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Penyimpanan pengaturan, gambar latar, dan daftar acara. */
final class Store {
    static final String HOST_SUFFIX = "warmadewa.ac.id";
    static final String HOME = "https://lms.warmadewa.ac.id/my/";
    static final String CAL_EXPORT = "https://lms.warmadewa.ac.id/calendar/export.php";
    static final String BG_PATH = "/__lmsc_bg";

    private Store() {}

    static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("lmsc", Context.MODE_PRIVATE);
    }

    static boolean isLmsHost(String host) {
        return host != null && (host.equals(HOST_SUFFIX) || host.endsWith("." + HOST_SUFFIX));
    }

    // ---------------- pengaturan ----------------
    static JSONObject load(Context c) {
        try {
            return new JSONObject(prefs(c).getString("settings", "{}"));
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    static void merge(Context c, JSONObject patch) {
        try {
            JSONObject cur = load(c);
            Iterator<String> it = patch.keys();
            while (it.hasNext()) {
                String k = it.next();
                if (k.equals("bg")) { saveBg(c, patch.optString("bg", "")); continue; }
                cur.put(k, patch.get(k));
            }
            prefs(c).edit().putString("settings", cur.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    static void clear(Context c) {
        prefs(c).edit().clear().apply();
        bgFile(c).delete();
        Reminders.cancelAll(c);
    }

    // ---------------- gambar latar ----------------
    static File bgFile(Context c) {
        return new File(c.getFilesDir(), "bg.bin");
    }

    static boolean hasBgFile(Context c) {
        return "file".equals(prefs(c).getString("bgMode", "none")) && bgFile(c).exists();
    }

    static String bgMime(Context c) {
        return prefs(c).getString("bgMime", "image/png");
    }

    private static void saveBg(Context c, String bg) {
        SharedPreferences.Editor e = prefs(c).edit();
        if (bg.startsWith("data:") && bg.indexOf(',') > 0) {
            int comma = bg.indexOf(',');
            String meta = bg.substring(5, comma);
            String mime = meta.contains(";") ? meta.substring(0, meta.indexOf(';')) : meta;
            try (FileOutputStream fo = new FileOutputStream(bgFile(c))) {
                fo.write(Base64.decode(bg.substring(comma + 1), Base64.DEFAULT));
            } catch (Exception ex) {
                return;
            }
            e.putString("bgMode", "file").putString("bgMime", mime).putString("bgUrl", "");
        } else if (bg.isEmpty()) {
            bgFile(c).delete();
            e.putString("bgMode", "none").putString("bgUrl", "");
        } else {
            bgFile(c).delete();
            e.putString("bgMode", "url").putString("bgUrl", bg);
        }
        e.apply();
    }

    private static byte[] readAll(File f) throws IOException {
        try (FileInputStream in = new FileInputStream(f); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return out.toByteArray();
        }
    }

    /** Untuk halaman pengaturan: gambar latar berupa data URL (agar bisa jadi thumbnail). */
    static JSONObject forSettings(Context c) {
        JSONObject o = load(c);
        try {
            String mode = prefs(c).getString("bgMode", "none");
            if (hasBgFile(c)) {
                o.put("bg", "data:" + bgMime(c) + ";base64," + Base64.encodeToString(readAll(bgFile(c)), Base64.NO_WRAP));
            } else if (mode.equals("url")) {
                o.put("bg", prefs(c).getString("bgUrl", ""));
            } else {
                o.put("bg", "");
            }
        } catch (Exception ignored) {
        }
        return o;
    }

    /** Untuk halaman LMS: gambar lokal dilayani lewat alamat virtual, link kalender tidak ikut. */
    static JSONObject forPage(Context c) {
        JSONObject o = load(c);
        o.remove("calUrl");
        try {
            if (hasBgFile(c)) {
                o.put("bg", BG_PATH + "?v=" + bgFile(c).lastModified());
            } else if ("url".equals(prefs(c).getString("bgMode", "none"))) {
                o.put("bg", prefs(c).getString("bgUrl", ""));
            } else {
                o.put("bg", "");
            }
        } catch (Exception ignored) {
        }
        return o;
    }

    // ---------------- acara ----------------
    static void saveEvents(Context c, List<Ev> list) {
        JSONArray a = new JSONArray();
        try {
            for (Ev e : list) {
                JSONObject o = new JSONObject();
                o.put("uid", e.uid == null ? "" : e.uid);
                o.put("title", e.title == null ? "" : e.title);
                o.put("course", e.course == null ? "" : e.course);
                o.put("url", e.url == null ? "" : e.url);
                o.put("start", e.start);
                o.put("end", e.end);
                a.put(o);
            }
        } catch (Exception ignored) {
        }
        prefs(c).edit().putString("events", a.toString()).putLong("lastSync", System.currentTimeMillis()).apply();
    }

    static List<Ev> loadEvents(Context c) {
        List<Ev> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(prefs(c).getString("events", "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                Ev e = new Ev();
                e.uid = o.optString("uid");
                e.title = o.optString("title");
                e.course = o.optString("course");
                e.url = o.optString("url");
                e.start = o.optLong("start", -1);
                e.end = o.optLong("end", -1);
                out.add(e);
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    static long lastSync(Context c) {
        return prefs(c).getLong("lastSync", 0);
    }
}
