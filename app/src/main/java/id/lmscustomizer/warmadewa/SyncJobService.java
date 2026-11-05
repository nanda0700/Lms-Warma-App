package id.lmscustomizer.warmadewa;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;

/** Sinkron kalender otomatis setiap beberapa jam di latar belakang. */
public class SyncJobService extends JobService {
    static final int JOB_ID = 1001;

    static void ensure(Context c) {
        JobScheduler js = c.getSystemService(JobScheduler.class);
        if (js.getPendingJob(JOB_ID) != null) return;
        js.schedule(new JobInfo.Builder(JOB_ID, new ComponentName(c, SyncJobService.class))
                .setPeriodic(3L * 60 * 60 * 1000)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPersisted(true)
                .build());
    }

    @Override
    public boolean onStartJob(final JobParameters p) {
        new Thread(() -> {
            Sync.run(getApplicationContext());
            jobFinished(p, false);
        }).start();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters p) {
        return false;
    }
}
