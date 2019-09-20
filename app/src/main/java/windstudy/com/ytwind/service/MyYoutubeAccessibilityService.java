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

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

import windstudy.com.ytwind.R;
import windstudy.com.ytwind.eventbus.ActionEvent;
import windstudy.com.ytwind.util.Config;
import windstudy.com.ytwind.util.PrefUtils;
import windstudy.com.ytwind.util.Utils;


public class MyYoutubeAccessibilityService extends AccessibilityService {

    private static final String TAG = "access_service";
    private static final String TAG2 = "test_change_acc";
    final int MAX_SCROLL = 2; /*số lần scroll 2*/
    final int MAX_FIND_SUBCRIBE_BUTTON_COUNT = 60; /* giới hạn số lần tìm kiếm nút subcribe  60 x delay 2000 = 120s*/
    int currentSubcribeButtonCount = 0;

    final int MAX_FIND_CHANNEL_BUTTON_COUNT = 60; /* giới hạn số lần tìm kiếm nút channel  60 x delay 2000 = 120s*/
    int currentChannelButtonCount = 0;

    public List<AccessibilityNodeInfo> list;
    AccessibilityNodeInfo accessibilityNodeInfo;
    //handler switch account
    Handler handlerSwitchAccount;
    //handler click account
    Handler handlerClickAccount = new Handler();
    Handler handlerClickPlay = new Handler();
    //variables
    int accountIndex = 0;
    int currentScroll = 0;
    boolean shouldChangeAccount = false;
    boolean shouldClickBackToolbar = true;

    boolean isListAccShowing = false;
    boolean isScrolled = false;
    boolean isSubcribing = false;
    //accessibility node info var
    AccessibilityNodeInfo nodeListAcc = null;
    AccessibilityNodeInfo nodeInfoChannel = null;
    AccessibilityNodeInfo nodeInfoSubcribe = null;
    AccessibilityNodeInfo nodeInfoNotify = null;
    AccessibilityNodeInfo nodeInfoPlayVideo = null;


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void onAccessibilityEvent(final AccessibilityEvent event) {

        final int eventType = event.getEventType();

        logViewHierarchy(getRootInActiveWindow(), 0);

        if (event.getSource() != null) {
            event.getSource().refresh();
        }

        switch (eventType) {
            case AccessibilityEvent.TYPE_VIEW_CLICKED:
                break;
            case AccessibilityEvent.TYPE_VIEW_FOCUSED:
                break;
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                if (event.getSource() != null)
                    accessibilityNodeInfo = event.getSource();
//                switchAccount(event.getSource());
                checkListAccShowing(event.getSource());
//                initPlayVideo(event.getSource());
                break;
            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
                Log.d(TAG, "onAccessibilityEvent: called");
                initRetry();
                initSkipAds(event.getSource());
                initPlayVideo(event.getSource());
                getChannelButton(event.getSource());
                getSubcribeButton(event.getSource());
                clickBackToolbarSupport(event.getSource());
                break;
            case AccessibilityEvent.TYPE_VIEW_SELECTED:

                break;
            case AccessibilityEvent.TYPE_VIEW_SCROLLED:
                Log.d(TAG, "onAccessibilityEvent: scrolled");
                if (event.getSource() != null && isScrolled) {
                    if (event.getSource().findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/account_list") != null &&
                            event.getSource().findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/account_list").size() > 0) {
                        Log.d(TAG, "onAccessibilityEvent: in scrolled");

                        nodeListAcc = event.getSource().findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/account_list").get(0);
                        isListAccShowing = true;
                        changeAcc(false);

                        isScrolled = false;
                    }
                }
                break;

            default:
        }
    }

    private void clickBackToolbarSupport(AccessibilityNodeInfo info) {
        if (info != null) {
//            Log.d(TAG, "clickBackToolbarSupport: called");
            List<AccessibilityNodeInfo> toolbarNode = info.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/toolbar");
            if (toolbarNode != null && toolbarNode.size() > 0) {
//                Log.d(TAG, "clickBackToolbarSupport: called in success");
                if (!toolbarNode.get(0).getChild(0).getContentDescription().equals("Đóng") &&
                        !toolbarNode.get(0).getChild(0).getContentDescription().equals("Close"))
                    toolbarNode.get(0).getChild(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
                shouldClickBackToolbar = false;
            }
        }
    }

    private void getNotifyIcon(AccessibilityNodeInfo accessibilityNodeInfo) {
        if (nodeInfoNotify != null) {
            Log.d(TAG, "get Notify: not null");
        } else {
            Log.d(TAG, "get Notify: ");

        }

        if (accessibilityNodeInfo != null
                && accessibilityNodeInfo.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/notification_toggle_view") != null
                && accessibilityNodeInfo.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/notification_toggle_view").size() > 0) {
            nodeInfoNotify = accessibilityNodeInfo.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/notification_toggle_view").get(0);

        } else {
//            nodeInfoNotify = null;
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(ActionEvent event) {
        Log.d(TAG, "onMessageEvent: called");
        switch (event.getType()) {
            case Config.ACTION_CHANGE_ACCOUNT:
                changeAccountEvent();
                break;
            case Config.ACTION_REFRESH_ACCESSIBILITY:
                refreshAccessibility();
                break;
            case Config.ACTION_SUBCRIBE:
                subcribe();
                break;
            case Config.ACTION_CHECK_SUBCRIBE:
                checkSubcribe();
                break;
        }
    }

    private void checkSubcribe() {
        nodeInfoSubcribe = null;

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (nodeInfoSubcribe != null) {
                    String text = nodeInfoSubcribe.getText().toString();
                    Log.d(TAG, "run: " + text);
                    if (text.equals(getResources().getString(R.string.NOT_SUBSCRIBE_VIE))
                            || text.equals(getResources().getString(R.string.NOT_SUBSCRIBE_ENG))) {
                        //chưa sub
                        Log.d(TAG, "run: checksubcirbe chưa sub ");
                        EventBus.getDefault().post(new ActionEvent<>(Config.ACTION_NOTIFY, Config.STATE_NOT_SUBCRIBED)); //true: đã click subcribe
                    } else {
                        //đã sub
                        Log.d(TAG, "run: checksubcirbe đã sub ");
                        isSubcribing = true;
                        EventBus.getDefault().post(new ActionEvent<>(Config.ACTION_CHANGE_ACCOUNT, null));
                    }

                } else {
                    getSubcribeButton(accessibilityNodeInfo);
                    handler.postDelayed(this, 2000);
                }
            }
        }, 2000);
    }

    public void getSubcribeButton(AccessibilityNodeInfo accessibilityNodeInfo) {
        if (nodeInfoSubcribe != null) {
            Log.d(TAG, "getSubcribe: not null");
        } else {
            Log.d(TAG, "getSubcribe: ");

        }

        if (accessibilityNodeInfo != null
                && accessibilityNodeInfo.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/subscribe_button") != null
                && accessibilityNodeInfo.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/subscribe_button").size() > 0) {
            nodeInfoSubcribe = accessibilityNodeInfo.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/subscribe_button").get(0);

        } else {
//            nodeInfoChannel = null;
        }
    }

    private void subcribe() {
        nodeInfoSubcribe = null;

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (nodeInfoSubcribe != null) {
                    currentSubcribeButtonCount = 0;
                    String text = nodeInfoSubcribe.getText().toString();
                    Log.d(TAG, "run: " + text);
                    if (text.equals(getResources().getString(R.string.NOT_SUBSCRIBE_VIE))
                            || text.equals(getResources().getString(R.string.NOT_SUBSCRIBE_ENG))) {
                        //chưa sub
                        Log.d(TAG, "run: chưa sub ");
                        nodeInfoSubcribe.performAction(AccessibilityNodeInfo.ACTION_CLICK);

                        //handler đợi nút thông báo xuất hiện để click
                        final Handler handlerNotify = new Handler();
                        handlerNotify.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (nodeInfoNotify != null) {
                                    nodeInfoNotify.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                                    EventBus.getDefault().post(new ActionEvent<>(Config.ACTION_START_VIDEO, Config.STATE_SUBCRIBED)); //true: đã click subcribe
                                    nodeInfoSubcribe = null;
                                } else {
                                    getNotifyIcon(accessibilityNodeInfo);
                                    handlerNotify.postDelayed(this, 2000);
                                }
                            }
                        }, 1000);
                    } else {
                        //đã sub
                        Log.d(TAG, "run: đã sub ");
                        isSubcribing = true;
                        EventBus.getDefault().post(new ActionEvent<>(Config.ACTION_CHANGE_ACCOUNT, null));
                    }
                } else {
                    Log.d(TAG, "run: " + currentSubcribeButtonCount);
                    currentSubcribeButtonCount++;
                    if (currentSubcribeButtonCount >= MAX_FIND_SUBCRIBE_BUTTON_COUNT) {
                        EventBus.getDefault().post(new ActionEvent<>(Config.ACTION_CHANGE_ACCOUNT, null));
                    } else {
                        getSubcribeButton(accessibilityNodeInfo);
                        handler.postDelayed(this, 2000);
                    }
                }
            }
        }, 2000);
    }

    private void initRetry() {
        if (accessibilityNodeInfo != null) {
            if (accessibilityNodeInfo.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/secondary_retry_button").size() > 0) {
                Log.d(TAG, "initRetry: secondary retry button");
                accessibilityNodeInfo.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/secondary_retry_button").get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
        }

        if (accessibilityNodeInfo != null) {
            if (accessibilityNodeInfo.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/error_retry_button").size() > 0) {
                Log.d(TAG, "initRetry: error retry button");
                accessibilityNodeInfo.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/error_retry_button").get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
        }

        if (accessibilityNodeInfo != null) {
            if (accessibilityNodeInfo.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/error_message_text").size() > 0) {
                Log.d(TAG, "initRetry: error message text");
                accessibilityNodeInfo.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/error_message_text").get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
        }


        if (accessibilityNodeInfo != null) {
            if (accessibilityNodeInfo.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/player_error_view").size() > 0) {
                Log.d(TAG, "initRetry: player error view");
                if (accessibilityNodeInfo.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/player_fragment_container").size() > 0) {
                    Log.d(TAG, "initRetry: player fragment container");
                    accessibilityNodeInfo.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/player_fragment_container").get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
                }
                EventBus.getDefault().post(new ActionEvent<>(Config.ACTION_REFRESH_3G, null));
            }
        }
    }


    private void changeAccountEvent() {
        Log.d(TAG, "run: change account event called");
        shouldChangeAccount = true;
        nodeInfoChannel = null;
//        if (nodeInfoChannel != null) {
//            nodeInfoChannel.performAction(AccessibilityNodeInfo.ACTION_CLICK);
//            switchAccount(accessibilityNodeInfo);
//            return;
//        }

        //delay đến khi lấy đc nút channel
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "run: handler running");
                if (nodeInfoChannel != null) {
                    do {
                        Log.d(TAG, "run: looping do while");
                        nodeInfoChannel.performAction(AccessibilityNodeInfo.ACTION_CLICK);
//                        nodeInfoChannel = null;
                        getChannelButton(accessibilityNodeInfo);
                    } while (nodeInfoChannel != null);

                    switchAccount(accessibilityNodeInfo);
                } else {
                    Log.d(TAG, "run: " + currentChannelButtonCount);
                    currentChannelButtonCount++;
                    if (currentChannelButtonCount >= MAX_FIND_CHANNEL_BUTTON_COUNT) {
                        //tìm k thấy button -> load sang link tiếp theo
                        EventBus.getDefault().post(new ActionEvent<>(Config.ACTION_START_VIDEO, Config.STATE_LOAD_NEXT_LINK));
                        currentChannelButtonCount = 0;
                        return;
                    }
                    getChannelButton(accessibilityNodeInfo);
                    handler.postDelayed(this, 2000);
                }
            }
        }, 2000);

    }

    void checkListAccShowing(final AccessibilityNodeInfo info) {
        if (info != null && shouldChangeAccount) {
            if (info.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/account_list") != null &&
                    info.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/account_list").size() > 0) {

                nodeListAcc = info.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/account_list").get(0);

                //scroll
                Log.d(TAG2, "checkListAccShowing: " + currentScroll);
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (nodeListAcc.getChildCount() > 1) {
                            if (currentScroll != 0 && nodeListAcc.isScrollable()) {
                                //scroll
                                for (int i = 0; i < currentScroll; i++) {
                                    if (i == currentScroll - 1) isScrolled = true;
                                    nodeListAcc.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
                                }
                            } else {
                                changeAcc(false);
                            }
                        }
                        //trường hợp ko load đc ds account vì mạng yếu
                        else {
                            nodeListAcc.performAction(AccessibilityNodeInfo.ACTION_CLICK);

//                            Log.d(TAG, "run: cant scroll");
                            nodeListAcc = info.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/account_list").get(0);

                            handler.postDelayed(this, 2000);
                        }
                    }
                }, 2000);

            } else {
                nodeListAcc = null;
            }
        }
    }


    void changeAcc(boolean shouldStop) {
        Log.d(TAG, "changeAcc: " + nodeListAcc.getChildCount());
        if (nodeListAcc != null && nodeListAcc.getChildCount() > 1) {
            //for test
            for (int i = 0; i < nodeListAcc.getChildCount(); i++) {
                Log.d(TAG2, "node acc " + nodeListAcc.getChild(i).getText());
            }

            //kiểm tra index max chưa
            checkCurrentAccountIndex();

            //kiểm tra dòng hiện tại k phải tv
            while (nodeListAcc.getChild(accountIndex).getClassName().equals("android.widget.TextView")) {
                accountIndex++;
                //nếu phần tử cuối là textview : reset index, lần đổi tiếp theo sẽ tăng scroll lên 1
                checkCurrentAccountIndex();
            }

            Log.d(TAG2, "scroll " + currentScroll);
            Log.d(TAG2, "account index " + accountIndex);

            isListAccShowing = false;

            //chọn account ở index hiện tại
            clickAccount();
        }
    }

    private void clickAccount() {
        handlerClickAccount.postDelayed(new Runnable() {
            @Override
            public void run() {

                if (nodeListAcc != null) {
                    List<AccessibilityNodeInfo> list = null;
                    do {
                        if (nodeListAcc != null && nodeListAcc.getChild(accountIndex) != null) //phải check khác null vì có những trường hợp database thay đổi
                            nodeListAcc.getChild(accountIndex).performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        if (accessibilityNodeInfo != null && accessibilityNodeInfo.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/account_list") != null &&
                                accessibilityNodeInfo.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/account_list").size() > 0) {
                            list = accessibilityNodeInfo.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/account_list");
                        } else {
                            list = null;
                        }
                    } while (list != null);

                    //reset
                    accountIndex++;
                    shouldChangeAccount = false;
                    shouldClickBackToolbar = true;
                    if (handlerSwitchAccount != null)
                        handlerSwitchAccount.removeCallbacksAndMessages(null);
                    nodeInfoChannel = null;
                    nodeInfoSubcribe = null;
                    currentSubcribeButtonCount = 0;
                    currentChannelButtonCount = 0;

                    if (isSubcribing) {
                        EventBus.getDefault().post(new ActionEvent<>(Config.ACTION_NOTIFY, Config.STATE_CHANGED_ACCOUNT));
                        isSubcribing = false;
                    } else {
                        EventBus.getDefault().post(new ActionEvent<>(Config.ACTION_START_VIDEO, Config.STATE_NORMAL));
                    }
                } else {
                    handlerClickAccount.postDelayed(this, 2000);
                }
            }
        }, 4000);
    }

    private void checkCurrentAccountIndex() {
        if (accountIndex >= nodeListAcc.getChildCount()) {
            //reset
            accountIndex = 0;
            //tăng số lần scroll
            if (currentScroll == MAX_SCROLL) {
                currentScroll = 0;
            } else {
                currentScroll++;
            }
        }
    }

    public void getChannelButton(AccessibilityNodeInfo accessibilityNodeInfo) {
        if (nodeInfoChannel != null) {
            Log.d(TAG, "getChannelButton: not null");
        } else {
            Log.d(TAG, "getChannelButton: ");

        }

        if (accessibilityNodeInfo != null
                && accessibilityNodeInfo.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/channel_navigation_container") != null
                && accessibilityNodeInfo.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/channel_navigation_container").size() > 0) {
            try {
                nodeInfoChannel = accessibilityNodeInfo.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/channel_navigation_container").get(0);

            } catch (IndexOutOfBoundsException e) {
                nodeInfoChannel = null;
                Log.d(TAG, "getChannelButton: index out of bounds ");
            }

        } else {
            nodeInfoChannel = null;
        }
    }

//    public void getChannelButton(AccessibilityNodeInfo accessibilityNodeInfo) {
//        if (nodeInfoChannel != null) {
//            Log.d(TAG, "getChannelButton: not null");
//        } else {
//            Log.d(TAG, "getChannelButton: ");
//
//        }
//
//        if (accessibilityNodeInfo != null
//                && accessibilityNodeInfo.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/channel_navigation_container") != null
//                && accessibilityNodeInfo.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/channel_navigation_container").size() > 0) {
//            nodeInfoChannel = accessibilityNodeInfo.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/channel_navigation_container").get(0);
//
//        } else {
////            nodeInfoChannel = null;
//        }
//    }

    protected void switchAccount(final AccessibilityNodeInfo accessibilityNodeInfo) {
        Log.d(TAG, "run: switchAccount: called");
        if (handlerSwitchAccount == null) {
            handlerSwitchAccount = new Handler();
        } else {
            handlerSwitchAccount.removeCallbacksAndMessages(null);
        }
        handlerSwitchAccount.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (shouldChangeAccount) {
                    //click channel
//                    clickChannelButton(accessibilityNodeInfo);

                    //click back toolbar
                    clickBackToolbar();

                    //click image toolbar
                    clickImageToolbar();

                    //click account list
                    clickAccountList();
                } else {
                    Log.d(TAG, "run: shouldnt change acc");
                }
                handlerSwitchAccount.postDelayed(this, 2000);
            }
        }, 2000);

    }

    private void clickAccountList() {
        List<AccessibilityNodeInfo> accountNameNode = accessibilityNodeInfo.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/account_name");
        List<AccessibilityNodeInfo> changeAccountNode = accessibilityNodeInfo.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/list");
        if (accountNameNode.size() > 0) {
            //account đầu tiên
            accountNameNode.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
        } else if (changeAccountNode.size() > 0 && Utils.isTablet(MyYoutubeAccessibilityService.this)) {
            //các account khác va la tablet
            changeAccountNode.get(0).getChild(2).performAction(AccessibilityNodeInfo.ACTION_CLICK);
        } else if (changeAccountNode.size() > 0){
            //cac account khac va la dt binh thuong
            changeAccountNode.get(0).getChild(3).performAction(AccessibilityNodeInfo.ACTION_CLICK);
        }
    }

    private void clickImageToolbar() {
        List<AccessibilityNodeInfo> imvAvatarNode = accessibilityNodeInfo.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/mobile_topbar_avatar");
        if (imvAvatarNode != null && imvAvatarNode.size() > 0) {
            imvAvatarNode.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
        }
    }


    private void clickBackToolbar() {
//        Log.d(TAG, "clickBackToolbar: called" + shouldClickBackToolbar);
        List<AccessibilityNodeInfo> toolbarNode = accessibilityNodeInfo.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/toolbar");
        if (toolbarNode != null && toolbarNode.size() > 0) {
//            Log.d(TAG, "clickBackToolbar in clickable: called");
            if (!toolbarNode.get(0).getChild(0).getContentDescription().equals("Đóng") &&
                    !toolbarNode.get(0).getChild(0).getContentDescription().equals("Close"))
                toolbarNode.get(0).getChild(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
//            shouldClickBackToolbar = false;
        }
    }

    private void initPlayVideo(AccessibilityNodeInfo info) {
//        if (info != null) {
//            if (info.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/player_control_play_pause_replay_button").size() > 0) {
//                AccessibilityNodeInfo accessibilityNodeInfo = info.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/player_control_play_pause_replay_button").get(0);
//                if (accessibilityNodeInfo != null && accessibilityNodeInfo.getContentDescription().equals(getString(R.string.PLAY_VIDEO_VIE))) {
//                    accessibilityNodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
//                }
//            }
//        }
        if (info != null) {
            if (info.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/player_control_play_pause_replay_button").size() > 0) {
                nodeInfoPlayVideo = info.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/player_control_play_pause_replay_button").get(0);
                Log.d(TAG, "initPlayVideo: called");
            } else {
                Log.d(TAG, "initPlayVideo: called null");
//                nodeInfoPlayVideo = null;
            }
        }
    }



    private void createPLayVideoHandler() {
        handlerClickPlay.removeCallbacksAndMessages(null);
        handlerClickPlay.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "initPlayVideo: handler running");
                if (nodeInfoPlayVideo != null && (nodeInfoPlayVideo.getContentDescription().equals(getString(R.string.PLAY_VIDEO_VIE))
                        || nodeInfoPlayVideo.getContentDescription().equals(getString(R.string.PLAY_VIDEO_ENG)))) {
                    nodeInfoPlayVideo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                }
                handlerClickPlay.postDelayed(this, 2000);
            }
        }, 2000);

    }

    protected void initSkipAds(final AccessibilityNodeInfo info) {
        if (info != null) {
            if (info.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/skip_ad_button").size() > 0) {
                AccessibilityNodeInfo accessibilityNodeInfo = info.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/skip_ad_button").get(0);
                if (accessibilityNodeInfo != null && accessibilityNodeInfo.isClickable()) {
                    accessibilityNodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                }
            }
        }
    }


    private void refreshAccessibility() {
        Log.d(TAG, "onMessageEvent: RefreshAccessibilityEvent event");
        if (handlerClickAccount != null) handlerClickAccount.removeCallbacksAndMessages(null);
        if (handlerSwitchAccount != null) handlerSwitchAccount.removeCallbacksAndMessages(null);

        accountIndex = 0;
        currentScroll = 0;
        shouldChangeAccount = false;
        shouldClickBackToolbar = true;
        isScrolled = false;
        isListAccShowing = false;
        isSubcribing = false;
        currentChannelButtonCount = 0;
        currentSubcribeButtonCount = 0;
//        nodeInfoChannel = null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction() != null && intent.getAction().toString().contains("back")) {

        }
        if (intent.getAction() != null && intent.getAction().toString().contains("capture")) {

        }
        return START_NOT_STICKY;
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

    @Override
    public void onCreate() {
        super.onCreate();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread paramThread, Throwable paramThrowable) {
                //Do your own error handling here
                refreshAccessibility();
                Utils.saveErrorLog(MyYoutubeAccessibilityService.this,"Access: "+paramThrowable.getMessage());
                Utils.restartApp(MyYoutubeAccessibilityService.this, PrefUtils.getType(MyYoutubeAccessibilityService.this));
            }
        });

        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }

        createPLayVideoHandler();
    }


    //ghi lại nhật ký nút click
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

}


