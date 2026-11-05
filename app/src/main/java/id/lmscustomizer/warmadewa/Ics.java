package id.lmscustomizer.warmadewa;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/** Pembaca sederhana format iCalendar (.ics) yang diekspor Moodle. */
final class Ics {
    private Ics() {}

    static List<Ev> parse(String text) {
        List<Ev> out = new ArrayList<>();
        String t = text.replace("\r\n", "\n").replace("\r", "\n").replaceAll("\n[ \t]", "");
        Ev cur = null;
        for (String line : t.split("\n")) {
            if (line.equals("BEGIN:VEVENT")) { cur = new Ev(); continue; }
            if (line.equals("END:VEVENT")) {
                if (cur != null && cur.start > 0 && cur.title != null) out.add(cur);
                cur = null;
                continue;
            }
            if (cur == null) continue;
            int colon = line.indexOf(':');
            if (colon < 0) continue;
            String head = line.substring(0, colon);
            String val = line.substring(colon + 1);
            String name = head;
            String params = "";
            int semi = head.indexOf(';');
            if (semi >= 0) { name = head.substring(0, semi); params = head.substring(semi + 1); }
            switch (name.toUpperCase(Locale.ROOT)) {
                case "UID": cur.uid = val.trim(); break;
                case "SUMMARY": cur.title = unescape(val); break;
                case "DESCRIPTION": cur.desc = unescape(val); break;
                case "CATEGORIES": cur.course = unescape(val); break;
                case "URL": cur.url = val.trim(); break;
                case "DTSTART": cur.start = time(params, val); break;
                case "DTEND": cur.end = time(params, val); break;
                default: break;
            }
        }
        return out;
    }

    private static String unescape(String s) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '\\' && i + 1 < s.length()) {
                char n = s.charAt(++i);
                b.append(n == 'n' || n == 'N' ? '\n' : n);
            } else {
                b.append(ch);
            }
        }
        return b.toString().trim();
    }

    private static long time(String params, String val) {
        try {
            String v = val.trim();
            TimeZone tz = TimeZone.getDefault();
            for (String p : params.split(";")) {
                if (p.toUpperCase(Locale.ROOT).startsWith("TZID=")) tz = TimeZone.getTimeZone(p.substring(5));
            }
            SimpleDateFormat f;
            if (v.endsWith("Z")) {
                f = new SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.US);
                f.setTimeZone(TimeZone.getTimeZone("UTC"));
                v = v.substring(0, v.length() - 1);
            } else if (v.indexOf('T') > 0) {
                f = new SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.US);
                f.setTimeZone(tz);
            } else {
                f = new SimpleDateFormat("yyyyMMdd", Locale.US);
                f.setTimeZone(tz);
            }
            return f.parse(v).getTime();
        } catch (Exception e) {
            return -1;
        }
    }
}
