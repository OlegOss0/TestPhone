package com.pso.testphone.gui;

import com.pso.testphone.App;
import com.pso.testphone.gui.MainActivity;

public class MainActivityPresenter {
    private static MainActivity mMainActivity;
    private static String message = "";
    private static boolean activityShow = false;

    public static void onViewReady(MainActivity activity) {
        mMainActivity = activity;
        mMainActivity.setMsg(message);
    }

    public static void onViewDestroy() {
        mMainActivity = null;
    }

    public static boolean isActivityShow(){
        return mMainActivity != null;
    }

    public static void addMsg(boolean neadNewSting, String msg) {
        if (mMainActivity != null) {
            StringBuilder sb = new StringBuilder();
            sb.append(message);
            if (neadNewSting) {
                sb.append('\n');
            }
            sb.append(msg);
            message = sb.toString();
            App.getMainHandler().post(() -> {
                if(mMainActivity != null){
                    mMainActivity.clearMsg();
                    mMainActivity.setMsg(message);
                }
            });
        }
    }

    public static void clearMsg() {
        message = "";
        if (mMainActivity != null) {
            App.getMainHandler().post(() -> mMainActivity.clearMsg());
        }
    }
}
