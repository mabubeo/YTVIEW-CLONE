package windstudy.com.ytwind.util;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

import windstudy.com.ytwind.MainActivity;
import windstudy.com.ytwind.service.NewHeaderService3G;

public class Utils {
    public static String getTime(long time) {
        StringBuilder builder = new StringBuilder();

        long secondsInMilli = 1000;
        long minutesInMilli = secondsInMilli * 60;
        long hoursInMilli = minutesInMilli * 60;
        long daysInMilli = hoursInMilli * 24;

        long elapsedDays = time / daysInMilli;
        time = time % daysInMilli;

        long elapsedHours = time / hoursInMilli;
        time = time % hoursInMilli;

        long elapsedMinutes = time / minutesInMilli;
        time = time % minutesInMilli;

        long elapsedSeconds = time / secondsInMilli;


        if (elapsedDays > 0) {
            builder.append(elapsedDays + " ngày ");
        }
        if (elapsedHours > 0) {
            builder.append(elapsedHours + " giờ ");
        }
        if (elapsedMinutes > 0) {
            builder.append(elapsedMinutes + " phút ");
        }
        if (elapsedSeconds > 0) {
            builder.append(elapsedSeconds + " giây ");
        }

        return builder.toString();
    }

    public static void restartApp(Context context, int type){
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("type", type);
        int mPendingIntentId = 123456;
        PendingIntent mPendingIntent = PendingIntent.getActivity(context, mPendingIntentId, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 1000, mPendingIntent);
        System.exit(0);
    }

    public static void saveErrorLog(Context context, String err){
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference(Config.REF);
        mDatabase.child("e_log").child(PrefUtils.getName(context)).push().setValue(err);
    }

    public static void saveRunningStatus(Context context, int campaignIndex, int linkIndex){
        PrefUtils.saveCurrentCampaignIndex(context,campaignIndex);
        PrefUtils.saveCurrentLinkIndex(context,linkIndex);
    }

    public static void killYoutube(Context newContext) {
        try {
            Process suProcess = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(suProcess.getOutputStream());

            os.writeBytes("adb shell" + "\n");
            os.flush();

            ActivityManager activityManager = (ActivityManager) newContext.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
            for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
                if (appProcess.processName.equals("com.google.android.youtube")) {
                    os.writeBytes("am force-stop " + appProcess.processName + "\n");

                } else {
                }
            }

            os.flush();
            os.close();
            suProcess.waitFor();

        } catch (IOException ex) {
            ex.getMessage();
            Toast.makeText(newContext, ex.getMessage(), Toast.LENGTH_LONG).show();
        } catch (SecurityException ex) {
            Toast.makeText(newContext, "Can't get root access2",
                    Toast.LENGTH_LONG).show();
        } catch (Exception ex) {
            Toast.makeText(newContext, "Can't get root access3",
                    Toast.LENGTH_LONG).show();
        }
    }

    public static boolean isTablet(Context context){
        DisplayMetrics metrics = new DisplayMetrics();
        ((WindowManager)(context.getSystemService(Context.WINDOW_SERVICE))).getDefaultDisplay().getMetrics(metrics);

        float yInches= metrics.heightPixels/metrics.ydpi;
        float xInches= metrics.widthPixels/metrics.xdpi;
        double diagonalInches = Math.sqrt(xInches*xInches + yInches*yInches);
        if (diagonalInches>=6.5){
            // 6.5inch device or bigger
            return true;
        }
        return false;
    }
}
