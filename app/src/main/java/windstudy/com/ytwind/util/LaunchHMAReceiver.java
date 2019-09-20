package windstudy.com.ytwind.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class LaunchHMAReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            Intent LaunchIntent = context.getPackageManager().getLaunchIntentForPackage("com.hidemyass.hidemyassprovpn");
            context.startActivity(LaunchIntent);
            Log.d("a123","dang launch hma");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
