package com.pso.testphone.gui;

import android.content.Intent;

import com.pso.testphone.App;

public class GuiHelper {

    public static void startMainActivity(Boolean fromActivity){
        if(MainActivityPresenter.isActivityShow()){
            return;
        }
        Intent intent = new Intent(App.getContext(), MainActivity.class);
        intent.setFlags(fromActivity ? Intent.FLAG_ACTIVITY_REORDER_TO_FRONT : Intent.FLAG_ACTIVITY_NEW_TASK );
        App.getContext().startActivity(intent);
    }

    public static void startDialogActivity(String task) {
        if (!DialogActivity.isShow) {
            App.getMainHandler().postDelayed(() -> {
                Intent intent = new Intent(App.getContext(), DialogActivity.class);
                intent.setAction(task);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                App.getContext().startActivity(intent);
            }, 100);
        }
    }
}
