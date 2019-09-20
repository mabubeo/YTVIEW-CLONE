package windstudy.com.ytwind.service;

import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.net.ConnectivityManager;
import android.net.Uri;
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
import android.widget.Toast;

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
import java.util.List;
import java.util.Random;

import windstudy.com.ytwind.R;
import windstudy.com.ytwind.eventbus.ActionEvent;
import windstudy.com.ytwind.model.Campaign;
import windstudy.com.ytwind.model.Link;
import windstudy.com.ytwind.model.Video;
import windstudy.com.ytwind.util.Config;
import windstudy.com.ytwind.util.PrefUtils;
import windstudy.com.ytwind.util.Utils;

public class NewHeaderService3G extends Service {
    private static final String TAG = "hd3g";
    private static final long DEFAULT_VALUE = -1000;
    private static final String TAG2 = "tracking";
    final int time3gOff = 3000; /* thời gian chờ 3g tắt */
    final int time3gOn = 10000; /* thời gian chờ 3g bật */
    final int MAX_REFRESH_3G = 10; /* số lần refresh 3g tối đa(khi bị mất kết nối -> gọi đến refresh 3g -> sau 20 lần vẫn k đc thì chuyển link */
    int currentRefresh3GCount = 0; /* số lần refresh 3g hiện tại */

    Button btnClose;
    TextView tvTime, tvName, tvLog, tvCampaign;

    WindowManager.LayoutParams params;
    private View infoHead;
    private WindowManager windowManager;

    Handler limitTimeHandler = new Handler();
    int limitTime = 60000; /* 60 sec */
    boolean isLagged = true;

    //fb
    private DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference(Config.REF);
    //variables
    int runningLinkNumber = 0;
    Handler handler;
    Runnable runnable;
    CountDownTimer countDownTimer;
    long time;
    ArrayList<Video> arrayVideo = new ArrayList<>();
    boolean is3GRoot = false;
    boolean isFirstStart = true;

    Campaign currentCampaign;
    int currentCampaignIndex = 0;
    int currentLinkIndex = 0;
    int currentAccount = 0;

    //new
    ArrayList<Campaign> arrayCampaign = new ArrayList<>();


    @Override
    public void onCreate() {

        super.onCreate();

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread paramThread, Throwable paramThrowable) {
                //Do your own error handling here
                Log.d("Alert", "Lets See if it Works 3g !!!");
                Utils.saveErrorLog(NewHeaderService3G.this, "3G: " + paramThrowable.getMessage());
                Utils.saveRunningStatus(NewHeaderService3G.this, currentCampaignIndex, currentLinkIndex);
                stopSelf();
                Utils.restartApp(NewHeaderService3G.this, PrefUtils.getType(NewHeaderService3G.this));
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
                EventBus.getDefault().post(new ActionEvent<>(Config.ACTION_REFRESH_ACCESSIBILITY, null));
                stopService(new Intent(NewHeaderService3G.this, MyYoutubeAccessibilityService.class));
                EventBus.getDefault().unregister(this);
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

        tvName.setText(PrefUtils.getName(NewHeaderService3G.this));

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

                /* ============= chuyển xuống đếm khi countdown timer hoàn thành xong
                //cập nhật lượt count của link (nếu ko phải subcribe mới update, subcribe có luồng callback riêng)
                if (currentCampaign.getType() != Config.TYPE_SUBCRIBE)
                    updateCount();
                */

                //cập nhật dữ liệu tracking
//                updateTrackingData();
            }
        };

        //bật 3g
        turnOn3G(true);

        //bắt sự kiện fb khi 3g đã bật
        setupDatabase();

//        (infoHead.findViewById(R.id.btn_crash)).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//
//            }
//        });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(ActionEvent event) {
        Log.d(TAG, "onMessageEvent: called");
        switch (event.getType()) {
            case Config.ACTION_START_VIDEO:
                Log.d(TAG, "onMessageEvent: " + event.getData());
                startVideo((Integer) event.getData());
                break;
            case Config.ACTION_NOTIFY:
                checkNotify((Integer) event.getData());
                break;
            case Config.ACTION_REFRESH_3G:
                refresh3G();
                break;
        }
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
            loadNextLink(false, false);
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


    private void startCountdownTimer() {
        tvTime.setText("Chạy link, bắt đầu đếm ngược");
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
        mDatabase.child("mobiles").child(PrefUtils.getName(NewHeaderService3G.this)).child("link").setValue(currentCampaign.getLink().get(currentLinkIndex).getLink());
        mDatabase.child("mobiles").child(PrefUtils.getName(NewHeaderService3G.this)).child("last_time").setValue(simpleDateFormat.format(Calendar.getInstance().getTime()));
    }

    private void updateCount() {
        Utils.killYoutube(NewHeaderService3G.this);
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
                        // view
                        //tắt 3g
                        turnOffOn3G();
                        loadNextLink(false, true);
                        setLimitTime();
                    }
                });

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        //lưu trên máy
        if (currentCampaign.getType() == Config.TYPE_VIEW) {
            int count = PrefUtils.getCurrentViewCount(this);
            PrefUtils.saveCurrentViewCount(this, ++count);
        }
    }

    private void updateLastSub() {
        String runningLink = currentCampaign.getLink().get(currentLinkIndex).getLink().split("=")[1];
        mDatabase.child("last_sub").child(PrefUtils.getName(this)).child(runningLink).setValue(Calendar.getInstance().getTimeInMillis());
    }

    private void checkNotify(Integer data) {
        if (data == Config.STATE_NOT_SUBCRIBED) {
            //chưa sub -> tiến hành sub
            handler.postDelayed(runnable, 1000);
//            EventBus.getDefault().post(new ActionEvent<>(Config.ACTION_SUBCRIBE, null));
        } else if (data == Config.STATE_CHANGED_ACCOUNT) {
            //trường hợp đã subcribe từ trước nên chuyển account, sẽ play lại video và post event subcribe
            currentAccount++;
            if (currentAccount >= PrefUtils.getAccountNum(NewHeaderService3G.this)) {
                //trường hợp tất cả account đã sub hết
                currentAccount = 0;
                tvLog.setText("Tất cả account đã subscribe, chuyển link mới");
                loadNextLink(false, false);
                return;
            }
            playVideo(currentCampaign.getLink().get(currentLinkIndex));
            EventBus.getDefault().post(new ActionEvent<>(Config.ACTION_CHECK_SUBCRIBE, null));
        }
    }


    private void startVideo(int state) {
        Log.d(TAG, "startVideo: called");

        //trường hợp đã click subcribe
        if (currentCampaign.getType() == Config.TYPE_SUBCRIBE && state == Config.STATE_SUBCRIBED) {
            updateLastSub();
            updateCurrentRunningLink();
            updateCount();
//            new Handler().postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    loadNextLink(false, false);
//                }
//            }, 1000);
        } else if (state == Config.STATE_LOAD_NEXT_LINK) {
            //trường hợp k tìm thấy button -> load link mới
            loadNextLink(false, false);
        } else {
            //trường hợp view bình thường
            playVideo(currentCampaign.getLink().get(currentLinkIndex));
            handler.postDelayed(runnable, 2000);

        }
    }

    public void setupDatabase() {
        mDatabase.child("campaigns").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (countDownTimer != null) countDownTimer.cancel();
                limitTimeHandler.removeCallbacksAndMessages(null);
                currentLinkIndex = 0;
                currentCampaign = null;
                tvTime.setText("Đang lấy dữ liệu chạy lần đầu...");
                arrayCampaign.clear();
                EventBus.getDefault().post(new ActionEvent<>(Config.ACTION_REFRESH_ACCESSIBILITY, null));

                //get all campaigns
                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    Campaign campaign = postSnapshot.getValue(Campaign.class);

                    if (campaign.getType() != Config.TYPE_VIEW_HOUR && campaign.getStatus() != Config.STATUS_PAUSED) {
                        campaign.setName(postSnapshot.getKey());
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
        } else {
            //trường hợp chạy lần đầu -> lấy trạng thái running (nếu có)
            currentCampaignIndex = PrefUtils.getCurrentCampaignIndex(NewHeaderService3G.this);
            if (currentCampaignIndex >= arrayCampaign.size()) currentCampaignIndex = 0;

        }
        if (currentCampaignIndex >= arrayCampaign.size()) {
            currentCampaignIndex = 0;
        }
        currentCampaign = arrayCampaign.get(currentCampaignIndex);

        if (currentCampaign.getStatus() == Config.STATUS_PAUSED) {
            loadNextCampaign(false);
            return;
        }

        //set name
        String type = "Subcribe";
        if (currentCampaign.getType() == Config.TYPE_VIEW) type = "View";
        tvCampaign.setText(currentCampaign.getName() + " - " + type);

        Log.d(TAG, "loadNextCampaign: " + currentCampaign.toString());

        loadNextLink(true, false);
    }

    private void loadNextLink(final boolean isFirstTime, boolean shouldDelay) {
        int time = 0;
        if (shouldDelay) time = time3gOff + time3gOn;
        turnOn3G(false);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                turnOn3G(true);
                if (!isFirstTime)
                    currentLinkIndex++;
                else {
                    //trường hợp chạy lần đầu -> lấy trạng thái running (nếu có)
                    currentLinkIndex = 0;
                    if (isFirstStart) {
                        isFirstStart = false;
                        currentLinkIndex = PrefUtils.getCurrentLinkIndex(NewHeaderService3G.this);
                        if (currentLinkIndex >= currentCampaign.getLink().size())
                            currentLinkIndex = 0;
                    }
                }

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
                        isLagged = false;
                        //th dã đủ view/sub
                        if (dataSnapshot.getValue() != null && Integer.parseInt(dataSnapshot.getValue().toString()) >= currentCampaign.getLink().get(currentLinkIndex).getCount()) {
                            tvLog.setText("Link đã đủ số view, chuyển link mới...");
                            loadNextLink(false, false);
                            return;
                        }

                        //th campaign view
                        if (currentCampaign.getType() == Config.TYPE_VIEW) {
                            startCampaignView();
                        }

                        //th campaign sub
                        if (currentCampaign.getType() == Config.TYPE_SUBCRIBE) {
                            startCampaignSubcribe();
                        }

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.d(TAG, "onCancelled: " + databaseError.getMessage());
                    }
                });


            }
        }, time);
    }

    private void startCampaignSubcribe() {
        String runningLink = currentCampaign.getLink().get(currentLinkIndex).getLink().split("=")[1];

        //check delay
        mDatabase.child("last_sub").child(PrefUtils.getName(this)).child(runningLink).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                long lastTimeSub = 0;
                if (dataSnapshot != null && dataSnapshot.getValue() != null) {
                    lastTimeSub = (long) dataSnapshot.getValue();
                }
                long now = Calendar.getInstance().getTimeInMillis();
                long delay = currentCampaign.getDelay();
                Log.d(TAG, "delaytime: " + (now - lastTimeSub));

//                tvLog.setText("Thời gian đã delay " + new SimpleDateFormat("hh:mm:ss").format((now-lastTimeSub) - delay));
                tvLog.setText("Thời gian đã delay " + Utils.getTime((now - lastTimeSub)) + " / " + Utils.getTime(delay));

                if (lastTimeSub == 0) tvLog.setText("Lần đầu chạy subcribe...");

                if (now - lastTimeSub >= delay) {
                    Log.d(TAG, "onDataChange:  đủ delay");
                    //trường hợp đủ delay
                    playVideo(currentCampaign.getLink().get(currentLinkIndex));
//                    handler.postDelayed(runnable, 2000);
                    EventBus.getDefault().post(new ActionEvent<>(Config.ACTION_CHECK_SUBCRIBE, null));
                } else {
                    //chưa delay xong
                    Log.d(TAG, "onDataChange:  k đủ delay");

                    loadNextLink(false, false);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void startCampaignView() {
        playVideo(currentCampaign.getLink().get(currentLinkIndex));
        if (isEnoughViewMobile()) {
            PrefUtils.saveCurrentViewCount(NewHeaderService3G.this, 0);
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
        currentAccount = 0;
        currentLinkIndex = 0;
        currentRefresh3GCount = 0;
        currentCampaignIndex = 0;
    }


    private void refreshCountDownTimer() {
        if (countDownTimer != null) countDownTimer.cancel();

        countDownTimer = new CountDownTimer(time, 1000) {

            public void onTick(long millisUntilFinished) {
                tvTime.setText("Thời gian: " + millisUntilFinished / 1000);
            }

            public void onFinish() {


                isLagged = true;

                Log.d(TAG, "onFinish: called.");
                tvTime.setText("Hoàn thành đếm ngược");

                //cập nhật lượt count của link (nếu ko phải subcribe mới update, subcribe có luồng callback riêng)
                if (currentCampaign.getType() != Config.TYPE_SUBCRIBE)
                    updateCount();


                // subcribe
                if (currentCampaign.getType() == Config.TYPE_SUBCRIBE) {
                    tvLog.setText("Tiến hành subcribe...");
                    EventBus.getDefault().post(new ActionEvent<>(Config.ACTION_SUBCRIBE, null));
                    return;
                }


            }

        };
    }

    private void setLimitTime() {
        limitTimeHandler.removeCallbacksAndMessages(null);
        limitTimeHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isLagged) {
                    loadNextCampaign(false);
                    Log.d(TAG, "run: " + isLagged);
                    limitTimeHandler.postDelayed(this, limitTime);
                }
            }
        }, limitTime);
    }

    private void turnOffOn3G() {
//        if (check3G()) {
        turnOff3G();
        Log.d(TAG, "tắt 3g");
        tvLog.setText("Đang tắt 3G");
//        }
        try {
            Thread.sleep(time3gOff);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        turnOn3G(false);
        Log.d(TAG, "đã tắt 3G, đang bật 3G");
        tvLog.setText("Đã tắt 3G, đang bật 3G");


//        handler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//
//
//            }
//        }, time3gOff); //thời gian đợi 3g tắt
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

    private void turnOn3G(boolean shouldWait) {
        if (!check3G()) {
//            tvLog.setText("Đang kiểm tra mạng...");

            if (!is3GRoot) {
                setMobileDataEnabled(NewHeaderService3G.this, true);
                setNetworkState(true);
            } else {
                setConnection(true, NewHeaderService3G.this);
            }

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

    private void turnOff3G() {
        if (!is3GRoot) {
            setMobileDataEnabled(NewHeaderService3G.this, false);
            setNetworkState(false);
        } else {
            setConnection(false, NewHeaderService3G.this);
        }

    }

    public boolean check3G() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        boolean is3g = manager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)
                .isConnectedOrConnecting();
        return is3g;
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
        is3GRoot = intent.getBooleanExtra(Config.IS_3G_ROOT, false);

        Log.d(TAG, "onStartCommand: " + is3GRoot);
        return START_NOT_STICKY;
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



    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


}
