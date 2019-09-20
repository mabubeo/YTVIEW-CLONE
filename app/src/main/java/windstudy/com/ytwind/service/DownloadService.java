package windstudy.com.ytwind.service;

import android.app.DownloadManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;

import windstudy.com.ytwind.model.Version;
import windstudy.com.ytwind.util.Config;

public class DownloadService extends Service {
    private DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference(Config.REF);
    DownloadManager downloadManager;
    private long downloadReference;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("test", "onStartCommand: called servioec");

        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        registerReceiver(downloadReceiver, filter);

        initDownloadServices();
        return super.onStartCommand(intent, flags, startId);
    }

    private void initDownloadServices() {
        mDatabase.child("config").child("last_check").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                //get new version
                checkVersion();

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });

    }

    private void checkVersion() {
        mDatabase.child("config").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                final Version version = dataSnapshot.getValue(Version.class);
                Log.d("version cua firebase'", version.getVersion());
                if (!Config.VERSION.equals(version.getVersion())) {
                    new Handler().post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(DownloadService.this, "Đang tải phiên bản mới...", Toast.LENGTH_SHORT).show();
//                                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(version.getLink()));
//                                    startActivity(browserIntent);
                            String appURI = version.getLink();
                            downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                            Uri Download_Uri = Uri.parse(appURI);
                            DownloadManager.Request request = new DownloadManager.Request(Download_Uri);
//                            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);
                            request.setAllowedOverRoaming(false);
                            request.setTitle("ytver" + version.getVersion());
                            request.setDestinationInExternalFilesDir(DownloadService.this, Environment.DIRECTORY_DOWNLOADS, Config.APP_NAME + ".apk");
                            downloadReference = downloadManager.enqueue(request);
                        }
                    });

                }else{
                    Toast.makeText(DownloadService.this, "Đây là phiên bản mới nhất!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    private BroadcastReceiver downloadReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            //check if the broadcast message is for our Enqueued download
            long referenceId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (downloadReference == referenceId) {
                stopService(new Intent(DownloadService.this, HourHeaderService.class));
                stopService(new Intent(DownloadService.this, NewHeaderService3G.class));
                Log.d("test", "Downloading of the new app version complete");
                //start the installation of the latest version
//                Intent installIntent = new Intent(Intent.ACTION_VIEW);
//                installIntent.setDataAndType(downloadManager.getUriForDownloadedFile(downloadReference),
//                        "application/vnd.android.package-archive");
//                installIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                startActivity(installIntent);

                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Intent install = new Intent(Intent.ACTION_VIEW);
                    install.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                    Uri apkURI = FileProvider.getUriForFile(
                            DownloadService.this,
                            getApplicationContext()
                                    .getPackageName() + ".provider", new File(downloadManager.getUriForDownloadedFile(downloadReference).getPath()));
                    install.setDataAndType(apkURI,
                            downloadManager.getMimeTypeForDownloadedFile(downloadReference));
                    install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(install);
                } else {
//                    String destination = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/";
//                    String fileName = "ytver32.apk";
//                    destination += fileName;
                    Uri uri;
                    uri = Uri.parse("file://" + downloadManager.getUriForDownloadedFile(downloadReference));

                    Intent install = new Intent(Intent.ACTION_VIEW);
                    install.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    install.setDataAndType(uri,
                            downloadManager.getMimeTypeForDownloadedFile(downloadReference));
                    startActivity(install);
                }

            }
        }
    };

    @Override
    public void onDestroy() {
        this.unregisterReceiver(downloadReceiver);
        super.onDestroy();
    }
}
