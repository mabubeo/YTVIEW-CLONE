package windstudy.com.ytwind.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Random;

import windstudy.com.ytwind.R;
import windstudy.com.ytwind.eventbus.ActionEvent;
import windstudy.com.ytwind.model.Campaign;
import windstudy.com.ytwind.model.Link;
import windstudy.com.ytwind.model.Video;
import windstudy.com.ytwind.util.Config;
import windstudy.com.ytwind.util.Connectivity;
import windstudy.com.ytwind.util.PrefUtils;
import windstudy.com.ytwind.util.RootUtil;
import windstudy.com.ytwind.util.Utils;

public class HourHeaderService extends Service {
    private static final String TAG = "hd3g";
    private static final long DEFAULT_VALUE = -1000;
    private static final String TAG2 = "tracking";
    final int time3gOff = 3000;
    final int time3gOn = 10000;
    final long timeDefaultVideo = 10000; /* 10 sec */
    String linkDefault = "https://www.youtube.com/watch?v=95ahbau-rJk"; //muon ruou to tinh

    Button btnClose;
    TextView tvTime, tvName, tvLog, tvCampaign;

    WindowManager.LayoutParams params;
    private View infoHead;
    private WindowManager windowManager;

    //fb
    private DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference(Config.REF);
    //variables
    int runningLinkNumber = 0;
    Handler handler;
    Runnable runnable;
    CountDownTimer countDownTimer;
    long firstTime, secondTime, playlistTime, fromTime, time;
    ArrayList<Video> arrayVideo = new ArrayList<>();
    String key;

    Campaign currentCampaign;
    int currentCampaignIndex = 0;
    int currentLinkIndex = 0;
    int currentAccount = 0;

    //new
    ArrayList<Campaign> arrayCampaign = new ArrayList<>();

    final int MAX_REFRESH_3G = 10; /* số lần refresh 3g tối đa(khi bị mất kết nối -> gọi đến refresh 3g -> sau 20 lần vẫn k đc thì chuyển link */
    int currentRefresh3GCount = 0; /* số lần refresh 3g hiện tại */
    boolean is3GRoot;

    @Override
    public void onCreate() {

        super.onCreate();
        if (RootUtil.isDeviceRooted()) {
            is3GRoot = true;
            Log.d(TAG, "onCreate: rooted");
        } else {
            is3GRoot = false;
            Log.d(TAG, "onCreate: unroot");
        }


        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread paramThread, Throwable paramThrowable) {
                //Do your own error handling here
                Log.d("Alert", "Lets See if it Works !!!");
                Utils.saveErrorLog(HourHeaderService.this, "Hour: " + paramThrowable.getMessage());
                Utils.saveRunningStatus(HourHeaderService.this, currentCampaignIndex, currentLinkIndex);
                stopSelf();
                Utils.restartApp(HourHeaderService.this, PrefUtils.getType(HourHeaderService.this));
            }
        });
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        LayoutInflater li = LayoutInflater.from(this);
        infoHead = li.inflate(R.layout.head_info_layout, null, false);
        tvTime = infoHead.findViewById(R.id.txt_time);
        tvName = infoHead.findViewById(R.id.txt_name);
        tvLog = infoHead.findViewById(R.id.txt_log);
        tvCampaign = infoHead.findViewById(R.id.txt_campaign);
        btnClose = infoHead.findViewById(R.id.btn_close);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);
        } else {
            params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);
        }


        params.gravity = Gravity.BOTTOM | Gravity.RIGHT;
        params.x = 0;
        params.y = 0;

        //this code is for dragging the chat head
        infoHead.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_UP:
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX
                                - (int) (event.getRawX() - initialTouchX);
                        params.y = initialY
                                - (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(infoHead, params);
                        return true;
                }
                return false;
            }
        });
        windowManager.addView(infoHead, params);

        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //refresh
                refreshService();
//                EventBus.getDefault().post(new ActionEvent<>(Config.ACTION_REFRESH_ACCESSIBILITY, null));
                stopService(new Intent(HourHeaderService.this, MyYoutubeAccessibilityService.class));

                stopSelf();
            }
        });

        (infoHead.findViewById(R.id.btn_settings)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });

        tvName.setText(PrefUtils.getName(HourHeaderService.this));

        handler = new Handler();

        runnable = new Runnable() {
            @Override
            public void run() {
                //lấy random time
                getCountdownTime();

                //đếm ngược
                startCountdownTimer();

                //cập nhật link đang chạy của máy
                updateCurrentRunningLink();


                //cập nhật dữ liệu tracking
//                updateTrackingData();
            }
        };

        //bắt sự kiện fb khi 3g đã bật
        setupDatabase();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(ActionEvent event) {
        Log.d(TAG, "onMessageEvent: called");
        switch (event.getType()) {
            case Config.ACTION_REFRESH_3G:
                if (Connectivity.isConnectedWifi(this)) {
                    refreshWifi();
                } else if (Connectivity.isConnectedMobile(this)) {
                    refresh3G();
                }
                break;
            case Config.ACTION_START_VIDEO:
                Log.d(TAG, "onMessageEvent: " + event.getData());
                startVideo((Integer) event.getData());
                break;
        }
    }

    private void startVideo(int state) {
        Log.d(TAG, "startVideo: called");
        if (state == Config.STATE_LOAD_NEXT_LINK) {
            //trường hợp k tìm thấy button -> load link mới
            loadNextLink(false);
        } else {
            //trường hợp view bình thường
            playVideo(currentCampaign.getLink().get(currentLinkIndex));
            handler.postDelayed(runnable, 2000);

        }
    }

    private void refreshWifi() {
        turnOffWifi();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        turnOnWifi();
    }

    private void refresh3G() {
        Log.d(TAG, "initRetry: refresh 3G called");
        tvTime.setText("Có sự cố, đang chạy lại 3G");
        currentRefresh3GCount++;
        Log.d(TAG, "refresh3G: " + currentRefresh3GCount);
        if (currentRefresh3GCount >= MAX_REFRESH_3G) {
            currentRefresh3GCount = 0;
            if (!check3G()) {
                turnOn3G(true);
            }
            EventBus.getDefault().post(new ActionEvent<>(Config.ACTION_REFRESH_ACCESSIBILITY, null));
            loadNextLink(false);
            return;
        }

        turnOff3G();
        try {
            Thread.sleep(time3gOff);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        turnOn3G(true);
    }

    private void turnOff3G() {
        if (!is3GRoot) {
            setMobileDataEnabled(HourHeaderService.this, false);
            setNetworkState(false);
        } else {
            setConnection(false, HourHeaderService.this);
        }

    }


    private void turnOn3G(boolean shouldWait) {
        if (!check3G()) {
//            tvLog.setText("Đang kiểm tra mạng...");

            setMobileDataEnabled(HourHeaderService.this, true);
            setNetworkState(true);
            setConnection(true, HourHeaderService.this);

            if (shouldWait) {
                try {
                    //thời gian đợi 3g bật
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    public void setNetworkState(boolean mobileDataEnabled) {
        ConnectivityManager dataManager;
        dataManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        Method dataMtd = null;
        try {
            dataMtd = ConnectivityManager.class.getDeclaredMethod("setMobileDataEnabled", boolean.class);
        } catch (NoSuchMethodException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        dataMtd.setAccessible(mobileDataEnabled);
        try {
            dataMtd.invoke(dataManager, mobileDataEnabled);
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    public boolean check3G() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        boolean is3g = manager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)
                .isConnectedOrConnecting();
        return is3g;
    }

    private void setMobileDataEnabled(Context context, boolean enabled) {
        try {
            final ConnectivityManager conman = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            final Class conmanClass;

            conmanClass = Class.forName(conman.getClass().getName());

            final Field iConnectivityManagerField = conmanClass.getDeclaredField("mService");
            iConnectivityManagerField.setAccessible(true);
            final Object iConnectivityManager = iConnectivityManagerField.get(conman);
            final Class iConnectivityManagerClass = Class.forName(iConnectivityManager.getClass().getName());
            final Method setMobileDataEnabledMethod = iConnectivityManagerClass.getDeclaredMethod("setMobileDataEnabled", Boolean.TYPE);
            setMobileDataEnabledMethod.setAccessible(true);

            setMobileDataEnabledMethod.invoke(iConnectivityManager, enabled);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    //for rom root
    public static void setConnection(boolean enable, Context context) {

        String command;
        if (enable)
            command = Config.COMMAND_L_ON;
        else
            command = Config.COMMAND_L_OFF;

        try {
            Process su = Runtime.getRuntime().exec(Config.COMMAND_SU);
            DataOutputStream outputStream = new DataOutputStream(su.getOutputStream());

            outputStream.writeBytes(command);
            outputStream.flush();

            outputStream.writeBytes("exit\n");
            outputStream.flush();
            try {
                su.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void startCountdownTimer() {
        tvLog.setText("Chạy link, bắt đầu đếm ngược");
        Log.d(TAG, "chạy link, bắt đầu đếm ngược");
        refreshCountDownTimer();
        countDownTimer.start();
    }

    private void getCountdownTime() {
        if (currentCampaign.getLink().get(currentLinkIndex).getLink().contains("list")) {
            time = currentCampaign.getLink().get(currentLinkIndex).getPlaylistTime();
        } else {
            time = new Random().nextInt((int) ((currentCampaign.getLink().get(currentLinkIndex).getSecondTime() - currentCampaign.getLink().get(currentLinkIndex).getFirstTime()) + 1)) + currentCampaign.getLink().get(currentLinkIndex).getFirstTime();
        }
        Log.d(TAG, "time " + time);
    }

    private void updateCurrentRunningLink() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        mDatabase.child("mobiles").child(PrefUtils.getName(HourHeaderService.this)).child("link").setValue(currentCampaign.getLink().get(currentLinkIndex).getLink());
        mDatabase.child("mobiles").child(PrefUtils.getName(HourHeaderService.this)).child("last_time").setValue(simpleDateFormat.format(Calendar.getInstance().getTime()));
    }

    private void updateCount() {
        //lưu trên firebase
        final String runningLink = currentCampaign.getLink().get(currentLinkIndex).getLink().split("=")[1];
        mDatabase.child("counting").child(currentCampaign.getName()).child(runningLink).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int count = 0;
                if (dataSnapshot.getValue() != null) {
                    count = Integer.parseInt(dataSnapshot.getValue().toString());
                }
                mDatabase.child("counting").child(currentCampaign.getName()).child(runningLink).setValue(++count).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        loadNextLink(false);
                    }
                });

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        //lưu trên máy
        if (currentCampaign.getType() != Config.TYPE_SUBCRIBE) {
            int count = PrefUtils.getCurrentViewCount(this);
            PrefUtils.saveCurrentViewCount(this, ++count);
        }
    }

    public void setupDatabase() {
        mDatabase.child("campaigns").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                tvTime.setText("Đang lấy dữ liệu chạy lần đầu...");
                arrayCampaign.clear();
//                EventBus.getDefault().post(new ActionEvent<>(Config.ACTION_REFRESH_ACCESSIBILITY, null));

                //get all campaigns
                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    Campaign campaign = postSnapshot.getValue(Campaign.class);
                    campaign.setName(postSnapshot.getKey());
                    if (campaign.getType() == Config.TYPE_VIEW_HOUR) {
                        arrayCampaign.add(campaign);
                    }
                }

                setupCampaignLinks();

                loadNextCampaign(true);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void loadNextCampaign(boolean isFirstTime) {
        if (!isFirstTime) {
            currentCampaignIndex++;
        } else
            currentCampaignIndex = 0;
        if (currentCampaignIndex >= arrayCampaign.size()) {
            currentCampaignIndex = 0;
        }
        currentCampaign = arrayCampaign.get(currentCampaignIndex);

        if (currentCampaign.getType() != Config.TYPE_VIEW_HOUR) {
            loadNextCampaign(false);
            return;
        }

        if (currentCampaign.getStatus() == Config.STATUS_PAUSED) {
            loadNextCampaign(false);
            return;
        }

        //set name
        String type = "Subcribe";
        if (currentCampaign.getType() == Config.TYPE_VIEW) type = "View";
        if (currentCampaign.getType() == Config.TYPE_VIEW_HOUR) type = "Cày giờ";
        tvCampaign.setText(currentCampaign.getName() + " - " + type);

        Log.d(TAG, "loadNextCampaign: " + currentCampaign.toString());

        loadNextLink(true);
    }

    private void loadNextLink(final boolean isFirstTime) {
        if (!isFirstTime)
            currentLinkIndex++;
        else
            currentLinkIndex = 0;

        Log.d(TAG, "currentLinkIndex: " + currentLinkIndex);
        //chuyển campaign khi hết list link
        if (currentLinkIndex >= currentCampaign.getLink().size()) {
            tvLog.setText("Hoàn thành CD, chuyển CD mới...");
            loadNextCampaign(false);
            return;
        }

        //kiểm tra số view/sub link hiện tại
        String runningLink = currentCampaign.getLink().get(currentLinkIndex).getLink().split("=")[1];
        Log.d(TAG, "run: " + runningLink);
        mDatabase.child("counting").child(currentCampaign.getName()).child(runningLink).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d(TAG, "onDataChange: called");
                //th dã đủ view/sub
                if (dataSnapshot.getValue() != null && Integer.parseInt(dataSnapshot.getValue().toString()) >= currentCampaign.getLink().get(currentLinkIndex).getCount()) {
                    tvLog.setText("Link đã đủ số view, chuyển link mới...");
                    loadNextLink(false);
                    return;
                }

                startCampaignView();

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


    }


    private void startCampaignView() {
//        playVideo(currentCampaign.getLink().get(currentLinkIndex));
//        tvLog.setText("Đang xem link " + (currentLinkIndex + 1));
//        handler.postDelayed(runnable, 2000);
        playVideo(currentCampaign.getLink().get(currentLinkIndex));
        if (isEnoughViewMobile()) {
            PrefUtils.saveCurrentViewCount(HourHeaderService.this, 0);
            tvLog.setText("Đã đủ số view máy, tiến hành chuyển account...");
            EventBus.getDefault().post(new ActionEvent<>(Config.ACTION_CHANGE_ACCOUNT, null));
        } else {
            tvLog.setText("Đang xem link " + (currentLinkIndex + 1));
            handler.postDelayed(runnable, 2000);
        }
    }

    private boolean isEnoughViewMobile() {
        int currentViewCount = PrefUtils.getCurrentViewCount(this);
        int viewCount = PrefUtils.getViewCount(this);
        Log.d(TAG, "isEnoughViewMobile: " + currentViewCount + " - " + viewCount);
        if (currentViewCount >= viewCount) return true;
        return false;
    }

    private void setupCampaignLinks() {
        for (Campaign campaign : arrayCampaign) {
            if (campaign.getLink() != null && campaign.getLink().size() > 0) {
                for (Iterator<Link> it = campaign.getLink().iterator(); it.hasNext(); ) {
                    Link l = it.next();
                    if (l == null) {
                        it.remove();
                    } else {
                        if (l.getFirstTime() == DEFAULT_VALUE)
                            l.setFirstTime(campaign.getConfig().getFirstTime());
                        if (l.getSecondTime() == DEFAULT_VALUE)
                            l.setSecondTime(campaign.getConfig().getSecondTime());
                        if (l.getFromTime() == DEFAULT_VALUE)
                            l.setFromTime(campaign.getConfig().getFromTime());
                        if (l.getPlaylistTime() == DEFAULT_VALUE)
                            l.setPlaylistTime(campaign.getConfig().getPlaylistTime());
                    }
                }
            }
        }
    }

    private void refreshService() {
        handler.removeCallbacksAndMessages(null);
        if (countDownTimer != null) countDownTimer.cancel();
        runningLinkNumber = 0;
    }


    private void refreshCountDownTimer() {
        if (countDownTimer != null) countDownTimer.cancel();

        countDownTimer = new CountDownTimer(time, 1000) {

            public void onTick(long millisUntilFinished) {
                tvTime.setText("Thời gian: " + millisUntilFinished / 1000);
            }

            public void onFinish() {
                Log.d(TAG, "onFinish: called.");
                tvTime.setText("Hoàn thành đếm ngược");
                //cập nhật lượt count của link (nếu ko phải subcribe mới update, subcribe có luồng callback riêng)
                if (currentCampaign.getType() != Config.TYPE_SUBCRIBE)
                    updateCount();

            }

        };
    }


    public void playVideo(Link link) {
        if (!link.getLink().contains("list")) {
            String key = link.getLink().split("=")[1];
            String time = "&t=" + link.getFromTime() / 1000;
            Log.d(TAG, "playVideo: " + link.getLink() + time);
            Log.d(TAG, "playVideo: " + key);
//            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:" + key + time));
//         Check if the youtube app exists on the device
//            if (intent.resolveActivity(getPackageManager()) == null) {
//             If the youtube app doesn't exist, then use the browser
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("http://www.youtube.com/watch?v=" + key + time));
//            }
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else {
            playPlaylist(link);
        }
    }

    public void playPlaylist(Link link) {
        //"http://www.youtube.com/watch?v=dEba2fGPqjI&list=PL5sVZxqc4zPxmigAq_MNWknxjU3hCY9YB"
        String time = "&t=" + link.getFromTime();
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(link.getLink() + time));
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
    }

    public void turnOnWifi() {
        WifiManager wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) wifiManager.setWifiEnabled(true);
    }

    public void turnOffWifi() {
        WifiManager wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        if (wifiManager.isWifiEnabled()) wifiManager.setWifiEnabled(false);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        if (infoHead != null) {
            windowManager.removeView(infoHead);
            stopSelf();
            handler.removeCallbacksAndMessages(null);
            if (countDownTimer != null) countDownTimer.cancel();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
