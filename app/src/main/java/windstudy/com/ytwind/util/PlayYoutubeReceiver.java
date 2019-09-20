package windstudy.com.ytwind.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import windstudy.com.ytwind.service.HeaderServiceNewHMA;

public class PlayYoutubeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        HeaderServiceNewHMA.getInstance().test();
        Log.d("a123","In playutube receiver");
    }
}
