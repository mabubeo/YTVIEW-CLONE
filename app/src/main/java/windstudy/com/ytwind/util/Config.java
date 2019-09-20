package windstudy.com.ytwind.util;

public class Config {
    public static String REF = "development/";
//    public static String REF = "release/";
    public final static String COMMAND_L_ON = "svc data enable\n ";
    public final static String COMMAND_L_OFF = "svc data disable\n ";
    public final static String COMMAND_SU = "su";
    public final static String VERSION = "3.11";
    public final static String APP_NAME = "ytver311"; //k cần dùng

    public final static String IS_3G_ROOT = "is3gRoot";

    public final static int TYPE_3G = 0;
    public final static int TYPE_HOUR = 1;

    public final static int TYPE_VIEW = 0;
    public final static int TYPE_SUBCRIBE = 1;
    public final static int TYPE_VIEW_HOUR = 2;

    public final static int ACTION_REFRESH_ACCESSIBILITY = 0;
    public final static int ACTION_START_VIDEO = 1;
    public final static int ACTION_CHANGE_ACCOUNT = 2;
    public final static int ACTION_SUBCRIBE = 3;
    public static final int ACTION_CHECK_SUBCRIBE = 4;
    public static final int ACTION_NOTIFY = 5;
    public static final int ACTION_REFRESH_3G = 6;


    //call back start video
    public static final int STATE_NORMAL = 0;
    public static final int STATE_SUBCRIBED = 1;
    public static final int STATE_NOT_SUBCRIBED = 2;
    public static final Object STATE_CHANGED_ACCOUNT = 3;
    public static final int STATE_LOAD_NEXT_LINK = 4;

    public static final int STATUS_PAUSED = 0;
    public static final int STATUS_RUNNING = 1;


}
