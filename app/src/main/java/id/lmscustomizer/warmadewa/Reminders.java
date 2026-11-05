package id.lmscustomizer.warmadewa;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Penjadwalan alarm pengingat absen dan tugas. */
final class Reminders {
    static final String CHANNEL = "pengingat";
    static final String ACTION = "id.lmscustomizer.warmadewa.REMIND";
    static final int ABSEN = 0, TUGAS = 1, LAIN = 2;

    private static final Pattern P_ABSEN = Pattern.compile("(attendance|absen|presensi|kehadiran)", Pattern.CASE_INSENSITIVE);
    private static final Pattern P_TUGAS = Pattern.compile(
            "(due|jatuh tempo|deadline|tenggat|batas|assign|tugas|submit|kumpul|quiz|kuis|ujian|closes|ditutup|laporan|upload)",
            Pattern.CASE_INSENSITIVE);

    private Reminders() {}

    static int type(Ev e) {
        String t = (e.title == null ? "" : e.title);
        if (P_ABSEN.matcher(t).find()) return ABSEN;
        if (P_TUGAS.matcher(t).find()) return TUGAS;
        return LAIN;
    }

    static void ensureChannel(Context c) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        NotificationChannel ch = new NotificationChannel(CHANNEL, "Pengingat absen & tugas", NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription("Pengingat jadwal absen dan batas pengumpulan tugas");
        nm.createNotificationChannel(ch);
    }

    private static int[] leads(String csv) {
        List<Integer> l = new ArrayList<>();
        for (String s : csv.split(",")) {
            try {
                int v = Integer.parseInt(s.trim());
                if (v >= 0) l.add(v);
            } catch (Exception ignored) {
            }
        }
        int[] r = new int[l.size()];
        for (int i = 0; i < r.length; i++) r[i] = l.get(i);
        return r;
    }

    static String human(int min) {
        if (min == 0) return "sekarang";
        if (min % 1440 == 0) return (min / 1440) + " hari lagi";
        if (min % 60 == 0) return (min / 60) + " jam lagi";
        return min + " menit lagi";
    }

    static void cancelAll(Context c) {
        SharedPreferences p = Store.prefs(c);
        AlarmManager am = c.getSystemService(AlarmManager.class);
        for (String s : p.getString("alarmCodes", "").split(",")) {
            if (s.isEmpty()) continue;
            try {
                Intent i = new Intent(c, ReminderReceiver.class).setAction(ACTION);
                PendingIntent pi = PendingIntent.getBroadcast(c, Integer.parseInt(s), i,
                        PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
                if (pi != null) {
                    am.cancel(pi);
                    pi.cancel();
                }
            } catch (Exception ignored) {
            }
        }
        p.edit().putString("alarmCodes", "").apply();
    }

    static void schedule(Context c) {
        cancelAll(c);
        JSONObject s = Store.load(c);
        if (!s.optBoolean("remind", true)) return;
        AlarmManager am = c.getSystemService(AlarmManager.class);
        long now = System.currentTimeMillis();
        long horizon = now + 21L * 24 * 3600 * 1000;
        int[] absen = leads(s.optString("absenLead", "30,10"));
        int[] tugas = leads(s.optString("tugasLead", "1440,180"));
        SimpleDateFormat jam = new SimpleDateFormat("HH:mm", new Locale("id", "ID"));
        SimpleDateFormat tgl = new SimpleDateFormat("EEE, d MMM HH:mm", new Locale("id", "ID"));
        StringBuilder codes = new StringBuilder();

        for (Ev e : Store.loadEvents(c)) {
            int ty = type(e);
            if (ty == LAIN) continue;
            int[] ls = ty == ABSEN ? absen : tugas;
            String id = (e.uid == null || e.uid.isEmpty()) ? e.title : e.uid;
            String name = (e.course != null && !e.course.isEmpty()) ? e.course : e.title;
            String link = null;
            if (e.url != null && e.url.startsWith("https://") && Store.isLmsHost(Uri.parse(e.url).getHost())) link = e.url;

            for (int lead : ls) {
                long at = e.start - lead * 60000L;
                if (at <= now || at > horizon) continue;
                String title;
                String text;
                if (ty == ABSEN) {
                    title = "\u23F0 Absen: " + name;
                    text = lead == 0
                            ? "Sesi absen dimulai sekarang (" + jam.format(new Date(e.start)) + ")"
                            : "Mulai " + human(lead) + " \u2022 " + jam.format(new Date(e.start));
                } else {
                    title = "\uD83D\uDCDD " + e.title;
                    text = (name.equals(e.title) ? "" : name + "\n")
                            + "Batas " + human(lead) + " \u2022 " + tgl.format(new Date(e.start));
                }
                int code = (id + "|" + e.start + "|" + lead).hashCode();
                Intent i = new Intent(c, ReminderReceiver.class).setAction(ACTION)
                        .putExtra("title", title).putExtra("text", text)
                        .putExtra("url", link == null ? "" : link).putExtra("code", code);
                PendingIntent pi = PendingIntent.getBroadcast(c, code, i,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
                codes.append(code).append(',');
            }
        }
        Store.prefs(c).edit().putString("alarmCodes", codes.toString()).apply();
    }

    static void show(Context c, int id, String title, String text, String url) {
        ensureChannel(c);
        Intent open = new Intent(c, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (url != null && !url.isEmpty()) open.putExtra("url", url);
        PendingIntent pi = PendingIntent.getActivity(c, id, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification n = new Notification.Builder(c, CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_cat)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setColor(0xFF1747A6)
                .build();
        c.getSystemService(NotificationManager.class).notify(id, n);
    }
}
