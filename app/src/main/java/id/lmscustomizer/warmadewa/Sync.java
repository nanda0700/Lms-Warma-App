package id.lmscustomizer.warmadewa;

import android.content.Context;
import android.net.Uri;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

/** Mengunduh kalender LMS (iCal) lalu menjadwalkan ulang pengingat. */
final class Sync {
    private Sync() {}

    static boolean run(Context c) {
        String url = Store.load(c).optString("calUrl", "");
        if (url.isEmpty() || !url.startsWith("https://")) return false;
        if (!Store.isLmsHost(Uri.parse(url).getHost())) return false;
        HttpURLConnection con = null;
        try {
            con = (HttpURLConnection) new URL(url).openConnection();
            con.setConnectTimeout(15000);
            con.setReadTimeout(25000);
            con.setRequestProperty("User-Agent", "LMSWarmadewa/1.0");
            if (con.getResponseCode() != 200) return false;
            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = r.readLine()) != null) sb.append(line).append('\n');
            }
            List<Ev> evs = Ics.parse(sb.toString());
            Store.saveEvents(c, evs);
            Reminders.schedule(c);
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            if (con != null) con.disconnect();
        }
    }
}
