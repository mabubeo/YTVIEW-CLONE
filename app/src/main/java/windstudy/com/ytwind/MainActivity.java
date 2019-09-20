package windstudy.com.ytwind;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import windstudy.com.ytwind.model.Version;
import windstudy.com.ytwind.service.DownloadService;
import windstudy.com.ytwind.service.HourHeaderService;
import windstudy.com.ytwind.service.NewHeaderService3G;
import windstudy.com.ytwind.service.SubViewHeaderService;
import windstudy.com.ytwind.util.Config;
import windstudy.com.ytwind.util.LoadingDialog;
import windstudy.com.ytwind.util.PrefUtils;
import windstudy.com.ytwind.util.RootUtil;
import windstudy.com.ytwind.util.Utils;

public class MainActivity extends AppCompatActivity {
    final int ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE = 999;
    private static final int REQUEST_WRITE_PERMISSION = 786;

    String TAG = "YTVIEW";
    private DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference(Config.REF);

    Button btnSave, btn3G, btnNew, btnUpdate, btnStartHour;
    EditText edtNameFixed, edtViewCount, edtAccountNum;
    TextView tvName, tvStatus, tvVersion, tvRoot;

    Intent intent, intentSubViewWifi, intentHour;
    boolean isRooted;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkPermission();
        btnSave = findViewById(R.id.btnSave);
        btnUpdate = findViewById(R.id.btnUpdate);
        edtNameFixed = findViewById(R.id.edt_name_fix);
        edtViewCount = findViewById(R.id.edt_view_count);
        edtAccountNum = findViewById(R.id.edt_account_num);
        btn3G = findViewById(R.id.btnStart3G);
        tvName = findViewById(R.id.tv_name);
        tvStatus = findViewById(R.id.tv_status);
        tvVersion = findViewById(R.id.tvVersion);
        btnNew = findViewById(R.id.btn_new);
        btnStartHour = findViewById(R.id.btnStartHour);
        tvRoot = findViewById(R.id.tv_root);

        if (RootUtil.isDeviceRooted()) {
            isRooted = true;
            Log.d(TAG, "onCreate: rooted");
            tvRoot.setText("Device rooted");
        } else {
            isRooted = false;
            tvRoot.setText("Device not root");
            Log.d(TAG, "onCreate: unroot");
        }

        if (Utils.isTablet(this)) {
            Log.d(TAG, "onCreate: tablet");
        } else {
            Log.d(TAG, "onCreate: not tablet");
        }

        requestPermission();

        getVersion();


        if (!isMyServiceRunning(DownloadService.class))
            startService(new Intent(this, DownloadService.class));
//        initDownloadServices();

        intent = new Intent(MainActivity.this, NewHeaderService3G.class);
//        intentNew = new Intent(MainActivity.this, HeaderServiceNewHMA.getInstance().getClass());
        intentHour = new Intent(MainActivity.this, HourHeaderService.class);
        intentSubViewWifi = new Intent(MainActivity.this, SubViewHeaderService.class);

        tvName.setText(PrefUtils.getName(MainActivity.this));
        edtAccountNum.setText(PrefUtils.getAccountNum(MainActivity.this) + "");

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveMobileConfig();

            }
        });

        btn3G.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startView3G(false);
            }
        });

        btnStartHour.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startViewHour();
            }
        });

        handleIntent();
//        btnNew.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                if (checkWifi()) {
//                    if (PrefUtils.getName(MainActivity.this) != "") {
//                        stopService(intent);
//                        Toast.makeText(MainActivity.this, "Vui lòng đợi...", Toast.LENGTH_LONG).show();
//                        Handler handler = new Handler();
//                        handler.postDelayed(new Runnable() {
//                            @Override
//                            public void run() {
//                                startService(intentNew);
//                            }
//                        }, 2000); //thời gian đợi fb xóa dl cũ nếu cập nhật tên máy
//
//                    } else {
//                        Toast.makeText(MainActivity.this, "Vui lòng đặt tên máy", Toast.LENGTH_SHORT).show();
//                    }
//                } else {
//                    Toast.makeText(MainActivity.this, "Vui lòng bật wifi", Toast.LENGTH_SHORT).show();
//                }
//            }
//        });

    }

    private void requestPermission() {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        999);

            }
        }

    }


    private void handleIntent() {
        if (getIntent().hasExtra("type")) {
            turnOn3G(false);
            int type = getIntent().getIntExtra("type", 0);
            if (type == Config.TYPE_3G) {
                Log.d(TAG, "handleIntent: type3g");
                startView3G(true);
            } else if (type == Config.TYPE_HOUR) {
                Log.d(TAG, "handleIntent: type hour");
                startViewHour();
            }  else if (type == Config.TYPE_WIFI) {
                Log.d(TAG, "handleIntent: type wifi");
                startSubViewWifi(true);
            }
        }
    }

    private void startSubViewWifi(boolean isAutoRestart) {

        if (!isRooted) {
            Toast.makeText(this, "Thiết bị chưa được root.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (PrefUtils.getName(MainActivity.this) != "" && PrefUtils.getViewCount(MainActivity.this) != 0 && PrefUtils.getAccountNum(MainActivity.this) != 0) {
            stopService(intentSubViewWifi);
            PrefUtils.saveType(this, Config.TYPE_WIFI);
            Toast.makeText(MainActivity.this, "Vui lòng đợi...", Toast.LENGTH_LONG).show();
            //trường hợp k phải do crash khởi động lại -> reset status
            if (!isAutoRestart) {
                Utils.saveRunningStatus(MainActivity.this, 0, 0);
            }
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
//                    intent.putExtra(Config.IS_3G_ROOT, isRooted);
                    startService(intentSubViewWifi);
                }
            }, 2000); //thời gian đợi fb xóa dl cũ nếu cập nhật tên máy

        } else {
            Toast.makeText(MainActivity.this, "Cần cấu hình dầy đủ thông tin", Toast.LENGTH_SHORT).show();
        }


    }

    private void startView3G(boolean isAutoRestart) {

        if (!isRooted) {
            Toast.makeText(this, "Thiết bị chưa được root.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (check3G()) {

            if (PrefUtils.getName(MainActivity.this) != "" && PrefUtils.getViewCount(MainActivity.this) != 0 && PrefUtils.getAccountNum(MainActivity.this) != 0) {
                stopService(intent);
                PrefUtils.saveType(this, Config.TYPE_3G);
                Toast.makeText(MainActivity.this, "Vui lòng đợi...", Toast.LENGTH_LONG).show();
                //trường hợp k phải do crash khởi động lại -> reset status
                if (!isAutoRestart) {
                    Utils.saveRunningStatus(MainActivity.this, 0, 0);
                }
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        intent.putExtra(Config.IS_3G_ROOT, isRooted);
                        startService(intent);
                    }
                }, 2000); //thời gian đợi fb xóa dl cũ nếu cập nhật tên máy

            } else {
                Toast.makeText(MainActivity.this, "Cần cấu hình dầy đủ thông tin", Toast.LENGTH_SHORT).show();
            }

        } else {
            Toast.makeText(MainActivity.this, "Vui lòng bật 3g", Toast.LENGTH_SHORT).show();
        }

    }

    private void startViewHour() {
        if (checkWifi() || check3G()) {
            if (PrefUtils.getName(MainActivity.this) != "") {
                stopService(intentHour);
                PrefUtils.saveType(this, Config.TYPE_HOUR);
                Toast.makeText(MainActivity.this, "Vui lòng đợi...", Toast.LENGTH_LONG).show();
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startService(intentHour);
                    }
                }, 2000); //thời gian đợi fb xóa dl cũ nếu cập nhật tên máy

            } else {
                Toast.makeText(MainActivity.this, "Vui lòng đặt tên máy", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(MainActivity.this, "Vui lòng bật mạng", Toast.LENGTH_SHORT).show();
        }
    }

    private void getVersion() {
        tvVersion.setText("Phiên bản hiện tại: " + Config.VERSION);

        btnUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LoadingDialog.getInstance().showLoading(MainActivity.this);
                mDatabase.child("config").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        LoadingDialog.getInstance().hideLoading();
                        final Version version = dataSnapshot.getValue(Version.class);
                        Log.d(TAG, "onDataChange: " + version.toString());
                        if (!Config.VERSION.equals(version.getVersion())) {
                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(version.getLink()));
                            startActivity(browserIntent);
                        } else {
                            Toast.makeText(MainActivity.this, "Đã là phiên bản mới nhất", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        LoadingDialog.getInstance().hideLoading();
                        Toast.makeText(MainActivity.this, "Có lỗi xảy ra!", Toast.LENGTH_SHORT).show();
                    }
                });

            }
        });

    }

    private void saveMobileConfig() {
        if (PrefUtils.getViewCount(MainActivity.this) == 0 && TextUtils.isEmpty(edtViewCount.getText())) {
            Toast.makeText(MainActivity.this, "Chưa cấu hình số view", Toast.LENGTH_SHORT).show();
            return;
        }

        //máy chưa có tên
        if (PrefUtils.getName(MainActivity.this) == "" && TextUtils.isEmpty(edtNameFixed.getText())) {
            Toast.makeText(MainActivity.this, "Vui lòng nhập tên máy", Toast.LENGTH_SHORT).show();
            return;
        }

        //máy chưa setup tổng số account
        if (PrefUtils.getAccountNum(MainActivity.this) == 0 && TextUtils.isEmpty(edtAccountNum.getText())) {
            Toast.makeText(MainActivity.this, "Vui lòng nhập số account", Toast.LENGTH_SHORT).show();
            return;
        }

        //lưu tổng account
        if (!TextUtils.isEmpty(edtAccountNum.getText())) {
            PrefUtils.saveAccountNum(MainActivity.this, Integer.parseInt(edtAccountNum.getText().toString()));
            edtAccountNum.setText("" + PrefUtils.getAccountNum(MainActivity.this));
        }

        //lưu tổng view
        if (!TextUtils.isEmpty(edtViewCount.getText())) {
            PrefUtils.saveViewCount(MainActivity.this, Integer.parseInt(edtViewCount.getText().toString()));
            PrefUtils.saveCurrentViewCount(MainActivity.this, 0);
            edtViewCount.setText("" + PrefUtils.getViewCount(MainActivity.this));
            tvStatus.setText("Số view đã chạy: " + PrefUtils.getCurrentViewCount(MainActivity.this) + " / " + PrefUtils.getViewCount(MainActivity.this));
        }

        //cập nhật tên máy mới
        if (!TextUtils.isEmpty(edtNameFixed.getText())) {
            //xóa dữ liệu cũ
            if (PrefUtils.getName(MainActivity.this) != "") {
                if (!checkWifi() && !check3G()) {
                    Toast.makeText(MainActivity.this, "Vui lòng bật mạng", Toast.LENGTH_SHORT).show();
                } else {
                    mDatabase.child("mobiles").child(PrefUtils.getName(MainActivity.this)).removeValue();
                    stopService(intent);
                    PrefUtils.saveName(MainActivity.this, edtNameFixed.getText().toString());
                    tvName.setText(PrefUtils.getName(MainActivity.this));
                }
            } else {
                //cap nhat ten lan dau
                PrefUtils.saveName(MainActivity.this, edtNameFixed.getText().toString());
                tvName.setText(PrefUtils.getName(MainActivity.this));
            }
        }
    }

    public boolean checkWifi() {
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected();
        return mWifi;
    }

    public boolean check3G() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        boolean is3g = manager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)
                .isConnectedOrConnecting();
        return is3g;
    }

    public void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE);
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE) {
            if (!Settings.canDrawOverlays(this)) {
                // You don't have permission
                checkPermission();
            } else {
                finish();
            }
        }
    }


    private boolean isAccessibilitySettingsOn(Context mContext) {
        int accessibilityEnabled = 0;
        final String service = "windstudy.com.ytwind/windstudy.com.ytwind.service.MyYoutubeAccessibilityService";
        boolean accessibilityFound = false;
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                    mContext.getApplicationContext().getContentResolver(),
                    android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);
            Log.v(TAG, "accessibilityEnabled = " + accessibilityEnabled);
        } catch (Settings.SettingNotFoundException e) {
            Log.e(TAG, "Error finding setting, default accessibility to not found: "
                    + e.getMessage());
        }
        TextUtils.SimpleStringSplitter mStringColonSplitter = new TextUtils.SimpleStringSplitter(':');

        if (accessibilityEnabled == 1) {
            Log.v(TAG, "***ACCESSIBILIY IS ENABLED*** -----------------");
            String settingValue = Settings.Secure.getString(
                    mContext.getApplicationContext().getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (settingValue != null) {
                TextUtils.SimpleStringSplitter splitter = mStringColonSplitter;
                splitter.setString(settingValue);
                while (splitter.hasNext()) {
                    String accessabilityService = splitter.next();
                    Log.v(TAG, "-------------- > accessabilityService :: " + accessabilityService);
                    if (accessabilityService.equalsIgnoreCase(service)) {
                        Log.v(TAG, "We've found the correct setting - accessibility is switched on!");
                        return true;
                    }
                }
            }
        } else {
            Log.v(TAG, "***ACCESSIBILIY IS DISABLED***");
        }
        return accessibilityFound;
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!isAccessibilitySettingsOn(this)) {
            Intent intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivityForResult(intent, 0);
        }
        if (PrefUtils.getCurrentViewCount(this) > PrefUtils.getViewCount(this))
            PrefUtils.saveCurrentViewCount(this, 0);
        tvStatus.setText("Số view đã chạy: " + PrefUtils.getCurrentViewCount(this) + " / " + PrefUtils.getViewCount(this));
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(intent);
        stopService(intentHour);
        stopService(intentSubViewWifi);
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

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 999: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    finish();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void turnOn3G(boolean shouldWait) {
        if (!check3G()) {
//            tvLog.setText("Đang kiểm tra mạng...");

            setConnection(true, MainActivity.this);

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


}
