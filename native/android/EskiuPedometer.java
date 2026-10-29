// <pedometer> on Android: the step counter sensor (steps since the last boot), after the
// ACTIVITY_RECOGNITION permission on Android 10 and later. pedometer.c calls start, stop,
// query and sample; counts and answers go to the "pedometer" channel (src/pedometer.esk).
//
// The sensor keeps no history, so each reading is saved as a sample (the time, the count since
// boot) in the app's preferences, and a query adds up the steps between the samples around its
// range. A count lower than the sample before it means the device rebooted: counting starts
// again from 0 there.
package org.eskiu.pedometer;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import java.util.ArrayList;
import java.util.Locale;
import org.eskiu.ui.UiActivity;

public final class EskiuPedometer {
    private static SensorEventListener listener;
    private static boolean running;
    private static long startCount = -1;
    private static long lastSaved;
    // Samples are kept for 30 days, at most one a minute while counting.
    private static final long KEEP_MS = 30L * 24 * 60 * 60 * 1000;
    private static final long EVERY_MS = 60 * 1000;

    public static void setup() {}

    private static void withPermission(Activity activity, java.util.function.Consumer<Boolean> done) {
        if (Build.VERSION.SDK_INT < 29) { done.accept(true); return; }
        UiActivity.requestPermissions(new String[] {Manifest.permission.ACTIVITY_RECOGNITION}, done);
    }

    private static Sensor counter(Activity activity) {
        SensorManager manager = (SensorManager)activity.getSystemService(Context.SENSOR_SERVICE);
        return manager == null ? null : manager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
    }

    public static void start() {
        Activity activity = UiActivity.current();
        if (activity == null) return;
        activity.runOnUiThread(() -> {
            running = true;
            startCount = -1;
            withPermission(activity, granted -> {
                if (!running) return;
                if (!granted) { UiActivity.emit("pedometer", "denied"); return; }
                Sensor sensor = counter(activity);
                if (sensor == null) { UiActivity.emit("pedometer", "unavailable"); return; }
                unlisten(activity);
                listener = new SensorEventListener() {
                    @Override public void onSensorChanged(SensorEvent event) {
                        long count = (long)event.values[0];
                        save(activity, count, false);
                        if (!running) return;
                        if (startCount < 0 || count < startCount) startCount = count;
                        UiActivity.emit("pedometer", "steps " + (count - startCount) + " -1");
                    }
                    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}
                };
                ((SensorManager)activity.getSystemService(Context.SENSOR_SERVICE))
                    .registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI);
                UiActivity.emit("pedometer", "steps 0 -1");
            });
        });
    }

    private static void unlisten(Activity activity) {
        if (listener == null) return;
        ((SensorManager)activity.getSystemService(Context.SENSOR_SERVICE)).unregisterListener(listener);
        listener = null;
    }

    public static void stop() {
        Activity activity = UiActivity.current();
        if (activity == null) return;
        activity.runOnUiThread(() -> {
            running = false;
            unlisten(activity);
        });
    }

    // One reading of the counter, saved as a sample; then `after` runs on the UI thread.
    private static void readOnce(Activity activity, Runnable after) {
        Sensor sensor = counter(activity);
        if (sensor == null) { after.run(); return; }
        SensorManager manager = (SensorManager)activity.getSystemService(Context.SENSOR_SERVICE);
        boolean[] done = {false};
        SensorEventListener once = new SensorEventListener() {
            @Override public void onSensorChanged(SensorEvent event) {
                if (done[0]) return;
                done[0] = true;
                manager.unregisterListener(this);
                save(activity, (long)event.values[0], true);
                after.run();
            }
            @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}
        };
        manager.registerListener(once, sensor, SensorManager.SENSOR_DELAY_FASTEST);
        // A sensor with nothing to report (no step since boot) never calls back: answer from
        // the samples there are.
        activity.getWindow().getDecorView().postDelayed(() -> {
            if (done[0]) return;
            done[0] = true;
            manager.unregisterListener(once);
            after.run();
        }, 2000);
    }

    public static void sample() {
        Activity activity = UiActivity.current();
        if (activity == null) return;
        activity.runOnUiThread(() -> {
            if (Build.VERSION.SDK_INT >= 29 &&
                activity.checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) != android.content.pm.PackageManager.PERMISSION_GRANTED) return;
            readOnce(activity, () -> {});
        });
    }

    private static SharedPreferences store(Context context) {
        return context.getSharedPreferences("eskiu.pedometer", Context.MODE_PRIVATE);
    }

    // Samples as "time:count" separated by commas, oldest first.
    private static ArrayList<long[]> samples(Context context) {
        ArrayList<long[]> out = new ArrayList<>();
        String text = store(context).getString("samples", "");
        for (String part : text.split(",")) {
            int colon = part.indexOf(':');
            if (colon <= 0) continue;
            try { out.add(new long[] {Long.parseLong(part.substring(0, colon)), Long.parseLong(part.substring(colon + 1))}); }
            catch (NumberFormatException ignored) {}
        }
        return out;
    }

    private static void save(Context context, long count, boolean always) {
        long now = System.currentTimeMillis();
        if (!always && now - lastSaved < EVERY_MS) return;
        lastSaved = now;
        ArrayList<long[]> list = samples(context);
        StringBuilder text = new StringBuilder();
        for (long[] s : list) {
            if (now - s[0] > KEEP_MS) continue;
            if (text.length() > 0) text.append(',');
            text.append(s[0]).append(':').append(s[1]);
        }
        if (text.length() > 0) text.append(',');
        text.append(now).append(':').append(count);
        store(context).edit().putString("samples", text.toString()).apply();
    }

    // The steps between two samples' counts: their difference, or from 0 after a reboot.
    private static long between(long before, long after) { return after >= before ? after - before : after; }

    // The steps in [from, to]: the steps between each pair of samples, the part of each pair
    // inside the range in proportion to time.
    static long stepsIn(ArrayList<long[]> list, long from, long to) {
        double total = 0;
        for (int i = 1; i < list.size(); i++) {
            long t0 = list.get(i - 1)[0], t1 = list.get(i)[0];
            long steps = between(list.get(i - 1)[1], list.get(i)[1]);
            if (t1 <= from || t0 >= to || steps == 0) continue;
            if (t1 == t0) { total += steps; continue; }
            long a = Math.max(t0, from), b = Math.min(t1, to);
            total += steps * (double)(b - a) / (double)(t1 - t0);
        }
        return Math.round(total);
    }

    public static void query(int id, long from, long to) {
        Activity activity = UiActivity.current();
        if (activity == null) return;
        activity.runOnUiThread(() -> withPermission(activity, granted -> {
            if (!granted) { UiActivity.emit("pedometer", "query " + id + " denied"); return; }
            if (counter(activity) == null) { UiActivity.emit("pedometer", "query " + id + " unavailable"); return; }
            readOnce(activity, () -> UiActivity.emit("pedometer",
                String.format(Locale.US, "query %d %d", id, stepsIn(samples(activity), from, to))));
        }));
    }
}
