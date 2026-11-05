package id.lmscustomizer.warmadewa;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        Reminders.show(c, i.getIntExtra("code", 1), i.getStringExtra("title"),
                i.getStringExtra("text"), i.getStringExtra("url"));
    }
}
