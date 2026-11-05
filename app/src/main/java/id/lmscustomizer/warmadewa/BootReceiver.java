package id.lmscustomizer.warmadewa;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Alarm hilang saat HP restart / aplikasi diperbarui, jadi dijadwalkan ulang di sini. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        SyncJobService.ensure(c);
        Reminders.schedule(c);
    }
}
