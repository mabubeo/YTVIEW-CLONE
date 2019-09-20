package windstudy.com.ytwind.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
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

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Random;

import windstudy.com.ytwind.util.LaunchHMAReceiver;
import windstudy.com.ytwind.R;
import windstudy.com.ytwind.util.Config;
import windstudy.com.ytwind.util.PrefUtils;


public class HeaderServiceNewHMA extends Service {
    WindowManager.LayoutParams params;
    private View infoHead;
    private WindowManager windowManager;
    Button btnClose;
    static TextView tvTime, tvName, tvLog;
    static String link;
    static String linkDefault = "8TszjrFJOe0";
    static long time, timeDf = 10000, playlistTime;
    static int runningLinkNumber = 0;
    static Handler handler;
    static Runnable runnable;
    static CountDownTimer countDownTimer;
    static ArrayList<String> listShortLink;
    ArrayList<String> listLink;
    private DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference(Config.REF);
    static long firstTime, secondTime;
    static PackageManager packageManager;
    static Context context;

    private static HeaderServiceNewHMA headerServiceNewHMA = null;
    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        LayoutInflater li = LayoutInflater.from(this);
        infoHead = li.inflate(R.layout.head_info_hma_layout, null, false);
        tvTime = infoHead.findViewById(R.id.txt_time);
        tvName = infoHead.findViewById(R.id.txt_name);
        tvLog = infoHead.findViewById(R.id.txt_log);
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

        PrefUtils.saveState(this,true);

        packageManager = this.getPackageManager();
        context = this.getApplicationContext();

        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PrefUtils.saveState(HeaderServiceNewHMA.this,false);
                stopSelf();
                handler.removeCallbacksAndMessages(null);
                if (countDownTimer != null) countDownTimer.cancel();
            }
        });
        Log.d("a123","phone name " + PrefUtils.getName(HeaderServiceNewHMA.this));
        tvName.setText(PrefUtils.getName(HeaderServiceNewHMA.this) + "");

        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                if (listShortLink.get(runningLinkNumber).contains("list")) {
                    time = playlistTime;
                } else {
                    time = new Random().nextInt((int) ((secondTime - firstTime) + 1)) + firstTime;
                }

                Log.d("haee", firstTime + " - " + secondTime + "reuslt2: " + time);
                if (listShortLink.size() == 1) {
                    refreshCountDownTimerOnly1Link();
                } else {
                    refreshCountDownTimer();
                }
                countDownTimer.start();
                tvLog.setText("Chạy link, bắt đầu đếm ngược");

                Log.d("hae", "chạy link, bắt đầu đếm ngược");

                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                mDatabase.child("mobiles").child(PrefUtils.getName(HeaderServiceNewHMA.this)).child("link").setValue(listLink.get(runningLinkNumber));
                mDatabase.child("mobiles").child(PrefUtils.getName(HeaderServiceNewHMA.this)).child("last_time").setValue(simpleDateFormat.format(Calendar.getInstance().getTime()));
                Log.d("hae", "cập nhật dữ liệu máy lên firebase");


            }
        };

        setupDatabase();
    }





    public void setupDatabase() {
        mDatabase.child("configHMA").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                PrefUtils.saveState(HeaderServiceNewHMA.this,true);
                handler.removeCallbacksAndMessages(null);
                if (countDownTimer != null) countDownTimer.cancel();
                runningLinkNumber = 0;
                tvTime.setText("Đang khởi tạo lần chạy đầu...");


                String allLink = (String) dataSnapshot.child("linkYoutube").getValue();
                listLink = new ArrayList<>(Arrays.asList(allLink.split(",")));
                listShortLink = new ArrayList();

                for (String link : listLink) {
                    if (link.length() > 11 && link.contains("list")) {
                        listShortLink.add(link);
                    } else if (link.length() > 11 && link.contains("=")) {
                        listShortLink.add((link.split("="))[1]);
                    } else listShortLink.add(link);
                    Log.d("hae", link + " = " + (link.split("="))[1]);
                }

                for (String link : listShortLink) {
                    Log.d("a123", link);
                }

                if (dataSnapshot.hasChild("firstTime") && dataSnapshot.hasChild("secondTime") && dataSnapshot.hasChild("playlistTime")) {
                    firstTime = (long) dataSnapshot.child("firstTime").getValue();
                    secondTime = (long) dataSnapshot.child("secondTime").getValue();
                    playlistTime = (long) dataSnapshot.child("playlistTime").getValue();
                }


                if (firstTime == 0 || secondTime == 0 || playlistTime == 0) {
                    tvLog.setText("Cần cấu hình thời gian trên web trước");
                    Toast.makeText(HeaderServiceNewHMA.this, "Cài đặt thời gian trên web trước", Toast.LENGTH_SHORT).show();
                } else {
                    playVideo(listShortLink.get(0));
                    handler.postDelayed(runnable, 2000);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void refreshCountDownTimer() {
        if (countDownTimer != null) countDownTimer.cancel();

        countDownTimer = new CountDownTimer(time, 1000) {

            public void onTick(long millisUntilFinished) {
                tvTime.setText("Thời gian: " + millisUntilFinished / 1000);
            }

            public void onFinish() {
                tvTime.setText("Đợi lần chạy tiếp theo...");

                //nhieu link
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (runningLinkNumber != listShortLink.size() - 1) {
                            runningLinkNumber++;
                            Log.d("hae", "Đang play link vị trí " + runningLinkNumber);

                            playVideo(listShortLink.get(runningLinkNumber));
                            tvLog.setText("Đang play link: " + (runningLinkNumber + 1));
                            handler.postDelayed(runnable, 2000);
                        } else{
                            runningLinkNumber = 0;
                            tvLog.setText("Đang chạy hma");
                            Intent intent = new Intent(getApplicationContext(), LaunchHMAReceiver.class);
                            sendBroadcast(intent);
                        }


                    }
                }, 500);


            }
        };
    }


    private void refreshCountDownTimerOnly1Link() {
        if (countDownTimer != null) countDownTimer.cancel();
        countDownTimer = new CountDownTimer(time, 1000) {

            public void onTick(long millisUntilFinished) {
                tvTime.setText("Thời gian: " + millisUntilFinished / 1000);
            }

            public void onFinish() {
                tvTime.setText("Đợi lần chạy tiếp theo...");

                Log.d("a123", "in 1 link");
                //1 link
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        tvLog.setText("Đang play link default trong " + timeDf / 1000 + " giây");
                        playVideo(linkDefault);
                    }
                }, 500);
                Log.d("hae", "delay" + timeDf);

                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        tvLog.setText("Đang chạy hma");
                        Intent intent = new Intent(getApplicationContext(),LaunchHMAReceiver.class);
                        sendBroadcast(intent);
                    }
                },timeDf);
            }

        };
    }


    public boolean checkWifi() {
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected();
        return mWifi;
    }

    public void playVideo(String key) {
        if (!key.contains("list")) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:" + key));
            if(packageManager==null) Log.d("a123","package null");
            else Log.d("a123","packeg not null");
            if (intent.resolveActivity(packageManager) == null) {
                intent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("http://www.youtube.com/watch?v=" + key));
            }
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } else {
            if(packageManager==null) Log.d("a123","package null");
            else Log.d("a123","packeg not null");
            playPlaylist(key);
        }
    }

    public void playPlaylist(String url) {
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        PrefUtils.saveState(this,false);
        if (infoHead != null) {
            windowManager.removeView(infoHead);
            stopSelf();
            handler.removeCallbacksAndMessages(null);
            if (countDownTimer != null) countDownTimer.cancel();
        }
    }

    public void test() {
        Log.d("a123", "inhma service");
//        setupUICurrentIP();
        if (listShortLink.size() == 1) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    playVideo(listShortLink.get(0));
                    handler.postDelayed(runnable, 2000);
                }
            }, timeDf);
        } else {
            playVideo(listShortLink.get(runningLinkNumber));
            tvLog.setText("Đang play link: " + (runningLinkNumber + 1));
            handler.postDelayed(runnable, 2000);
        }
    }

    public static HeaderServiceNewHMA getInstance(){
        if(headerServiceNewHMA==null){
            headerServiceNewHMA = new HeaderServiceNewHMA();
            Log.d("a123","sv null");
        }else{
            Log.d("a123","sv not null");
        }
        return headerServiceNewHMA;
    }

    public static TextView getTvLog(){
        return tvLog;
    }

}
