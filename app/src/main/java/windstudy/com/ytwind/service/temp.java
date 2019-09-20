package windstudy.com.ytwind.service;

/**
 * Created by TRUONG THANG on 8/3/2015.
 */

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.util.List;

import windstudy.com.ytwind.util.PlayYoutubeReceiver;
import windstudy.com.ytwind.util.PrefUtils;


public class temp extends AccessibilityService {

    private static final String TAG = "access_service";
    public AccessibilityNodeInfo accessibilityBackButton;
    public AccessibilityNodeInfo accessibilityCaptureButton;
    public List<AccessibilityNodeInfo> list;
    boolean isConnected = false;
    boolean isGotNewIP = false;
    boolean shouldGetNewLocation = true;
    final int NUM = 2; //so lan reset ip
    final int MAX_SCROLL = 50; //so lan scroll

    int currentNum = -1;
    int currentLocation = 0;
    int scrollTime = 0;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

        final int eventType = event.getEventType();
        Log.i("HMA", event.toString());


        logViewHierarchy(getRootInActiveWindow(), 0);


        switch (eventType) {
            case AccessibilityEvent.TYPE_VIEW_CLICKED:
                break;
            case AccessibilityEvent.TYPE_VIEW_FOCUSED:
                break;
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                if (PrefUtils.isAppRunning(this)) {
                    initSkipAds(event.getSource());
                    clickScreen(event.getSource());
                } else {
                    Log.d("a123", "App not running");
                }

                break;
            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
                break;
            case AccessibilityEvent.TYPE_VIEW_SELECTED:

                break;
            case AccessibilityEvent.TYPE_VIEW_SCROLLED:
                Log.d("testt", "event scrolled");
                break;

            default:
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    protected void clickScreen(final AccessibilityNodeInfo info) {
        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                if (isMainScreen(info)) {
                    list = info.findAccessibilityNodeInfosByViewId("com.hidemyass.hidemyassprovpn:id/slider");
                    for (AccessibilityNodeInfo node : list) {
                        if (!node.isChecked()) {
                            node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            node.recycle();
                            isConnected = true;
                            break;
                        } else {
                            isConnected = true;
                        }
                    }
                }

            }

        }, 1000);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d("a123", "shouldGet " + shouldGetNewLocation + " . " + "isGotNewIP " + isGotNewIP + " . " + "isConnected " + isConnected);
                //tim new location
                if (isConnected && !isGotNewIP && shouldGetNewLocation) {

                    Log.d("a123", "should get new location");
                    if (isChangeLocationScreen(info))
                        HeaderServiceNewHMA.getTvLog().setText("Đang lấy location mới");

                    list = info.findAccessibilityNodeInfosByViewId("com.hidemyass.hidemyassprovpn:id/change_location_button");
                    for (AccessibilityNodeInfo node : list) {
                        node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        node.recycle();
                        break;
                    }

                    //click all
                    list = info.findAccessibilityNodeInfosByViewId("com.hidemyass.hidemyassprovpn:id/button_favourites");
                    for (AccessibilityNodeInfo node : list) {
                        node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        node.recycle();
                        break;
                    }

                    //scroll
                    Log.d("a123", "scroll time" + scrollTime);
                    if (scrollTime >= MAX_SCROLL) {
                        scrollTime = 0;
                        Log.d("a123", "reset scroll " + scrollTime);
                    }
                    for (int i = 0; i < scrollTime; i++) {
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                list = info.findAccessibilityNodeInfosByViewId("com.hidemyass.hidemyassprovpn:id/locations_list");
                                for (AccessibilityNodeInfo node : list) {
                                    if (node.isScrollable()) {
                                        node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
                                        node.recycle();
                                    }
                                }
                            }

                        }, 1000);
                    }

                    //click new location
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
//                            List<AccessibilityNodeInfo> list3 = info.findAccessibilityNodeInfosByViewId("com.hidemyass.hidemyassprovpn:id/location_title_container");
                            list = info.findAccessibilityNodeInfosByViewId("com.hidemyass.hidemyassprovpn:id/locations_list");
                            AccessibilityNodeInfo listLocation;
                            if (!list.isEmpty()) {
                                listLocation = list.get(3);

                                //log location
                                for (int i = 0; i < listLocation.getChildCount(); i++) {
                                    if (listLocation.getChild(i).findAccessibilityNodeInfosByViewId("com.hidemyass.hidemyassprovpn:id/location_title").size() > 0) {
                                        Log.d("a1234", "loc: " + listLocation.getChild(i).findAccessibilityNodeInfosByViewId("com.hidemyass.hidemyassprovpn:id/location_title").get(0).getText());

                                    }
                                }

                                Log.d("a123", "currentLoc " + currentLocation + " / " + listLocation.getChildCount());

                                list = listLocation.getChild(currentLocation).findAccessibilityNodeInfosByViewId("com.hidemyass.hidemyassprovpn:id/location_title_container");
                                //btn favourite
                                if (list.get(0) != null && list.get(0).isClickable() && list.get(0).isEnabled()) {
                                    Log.d("a123", "pos clickable" + currentLocation);
                                    Log.d("a123", "pos clickable" + list.get(0).getClassName());
                                    list.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
                                    isGotNewIP = true;
                                    shouldGetNewLocation = false;
                                    //change loc
                                    currentLocation++;
                                    if (currentLocation >= listLocation.getChildCount() - 1) {
                                        currentLocation = 0;
                                        scrollTime += 2;
                                        Log.d("a123", "reset current loc");
                                    }
                                } else {
                                    //trường hợp kbh xảy ra ?
                                    Log.d("a123", "pos un clickable" + currentLocation);
                                    currentLocation++;
                                    if (currentLocation >= listLocation.getChildCount() - 1) {
                                        currentLocation = 0;
                                        scrollTime++;
                                        Log.d("a123", "reset current loc");
                                    }
                                    list = info.findAccessibilityNodeInfosByViewId("com.hidemyass.hidemyassprovpn:id/search_toolbar");
                                    for (AccessibilityNodeInfo nodeInfo : list) {
                                        nodeInfo.getChild(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
                                        nodeInfo.recycle();
                                        break;
                                    }
                                }
                                //btn all
//                                if (listLocation.getChild(currentLocation) != null && listLocation.getChild(currentLocation).isClickable() && listLocation.getChild(currentLocation).isEnabled() && !listLocation.getChild(currentLocation).getChild(1).getClassName().toString().equals("android.widget.TextView")) {
//                                    Log.d("a123", "pos clickable" + currentLocation);
//                                    listLocation.getChild(currentLocation).findAccessibilityNodeInfosByViewId("com.hidemyass.hidemyassprovpn:id/location_title").get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
//                                    isGotNewIP = true;
//                                    shouldGetNewLocation = false;
//                                    //change loc
//                                    currentLocation++;
//                                    if (currentLocation >= listLocation.getChildCount() - 1) {
//                                        currentLocation = 0;
//                                        scrollTime+=2;
//                                        Log.d("a123", "reset current loc");
//                                    }
//                                } else {
//                                    //trường hợp chữ A,B
//                                    Log.d("a123", "pos un clickable" + currentLocation);
//                                    currentLocation++;
//                                    if (currentLocation >= listLocation.getChildCount() - 1) {
//                                        currentLocation = 0;
//                                        scrollTime++;
//                                        Log.d("a123", "reset current loc");
//                                    }
//                                    list = info.findAccessibilityNodeInfosByViewId("com.hidemyass.hidemyassprovpn:id/search_toolbar");
//                                    for (AccessibilityNodeInfo nodeInfo : list) {
//                                        nodeInfo.getChild(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
//                                        nodeInfo.recycle();
//                                        break;
//                                    }
//                                }
                            }
                        }
                    }, 8000);


                }

                // reset ip cua location cu
                else if (isConnected && !isGotNewIP && !shouldGetNewLocation) {
                    Log.d("a123", " ! should get new location");
                    HeaderServiceNewHMA.getTvLog().setText("Đang reset ip lần thứ " + (currentNum + 1));
                    //click nut info
                    list = info.findAccessibilityNodeInfosByViewId("com.hidemyass.hidemyassprovpn:id/location_info_icon");
                    for (AccessibilityNodeInfo node : list) {
                        if (node.isEnabled()) {
                            node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            node.recycle();
                        }
                        break;
                    }

                    //click thay doi dia chi ip
                    list = info.findAccessibilityNodeInfosByViewId("com.hidemyass.hidemyassprovpn:id/btn_change_ip");
                    for (AccessibilityNodeInfo node : list) {
                        if (node.isEnabled()) {
                            node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            node.recycle();
                            isGotNewIP = true;
                        }
                        break;
                    }

                    //click back
                    list = info.findAccessibilityNodeInfosByViewId("com.hidemyass.hidemyassprovpn:id/base_fragment_toolbar");
                    for (AccessibilityNodeInfo node : list) {
                        node.getChild(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        node.recycle();
                        break;
                    }

                } else {
                    //da co ip moi, chay youtube
                    if (isMainScreen(info)) {
                        // + so lan reset ip, so sanh voi so goc
                        if (!shouldGetNewLocation) {
                            currentNum++;
                            if (currentNum >= NUM) {
                                shouldGetNewLocation = true;
                                currentNum = -1;
                            }
                            Log.d("a123", "lan doi ip thu " + currentNum);
                        }
                        isGotNewIP = false;
                        Intent i = new Intent(getApplicationContext(), PlayYoutubeReceiver.class);
                        sendBroadcast(i);
                    }

                }
            }

        }, 3000);
    }




    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public boolean isMainScreen(AccessibilityNodeInfo info) {
        list = info.findAccessibilityNodeInfosByViewId("com.hidemyass.hidemyassprovpn:id/location_mode");
        if (list.size() < 1) return false;
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public boolean isLoadingScreen(AccessibilityNodeInfo info) {
        list = info.findAccessibilityNodeInfosByViewId(" com.hidemyass.hidemyassprovpn:id/loading_view");
        if (list.size() < 1) return false;
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public boolean isChangeLocationScreen(AccessibilityNodeInfo info) {
        list = info.findAccessibilityNodeInfosByViewId("com.hidemyass.hidemyassprovpn:id/button_all");
        if (list.size() < 1) return false;
        return true;
    }

    //ghi lại nhật ký nút click
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static void logViewHierarchy(AccessibilityNodeInfo nodeInfo, final int depth) {

        if (nodeInfo == null) return;

        String spacerString = "";

        for (int i = 0; i < depth; ++i) {
            spacerString += '-';
        }

        for (int i = 0; i < nodeInfo.getChildCount(); ++i) {
            logViewHierarchy(nodeInfo.getChild(i), depth + 1);
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    protected void initSkipAds(final AccessibilityNodeInfo info) {
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(isAdsOn(info)){
                    //ads found
                    Log.d(TAG, "run: ads found");
                    AccessibilityNodeInfo accessibilityNodeInfo = info.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/skip_ad_button").get(0);
                    if(accessibilityNodeInfo != null && accessibilityNodeInfo.isClickable()){
                        accessibilityNodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    }
                }
                handler.postDelayed(this,1000);

            }

        }, 1000);
    }


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public boolean isAdsOn(AccessibilityNodeInfo info) {
        List<AccessibilityNodeInfo> list = info.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/skip_ad_button");
        if (list.size() < 1) return false;
        return true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction() != null && intent.getAction().toString().contains("back")) {

        }
        if (intent.getAction() != null && intent.getAction().toString().contains("capture")) {

        }
        return super.onStartCommand(intent, flags, startId);
    }


    @Override
    public void onInterrupt() {
        Toast.makeText(this, "onInterrupt", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        Toast.makeText(this, "onServiceConnected", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean stopService(Intent name) {
        return super.stopService(name);
    }

}


