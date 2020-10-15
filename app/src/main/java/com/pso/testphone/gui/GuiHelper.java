package com.pso.testphone.gui;

import android.content.Intent;

import com.pso.testphone.App;

public class GuiHelper {

    public static void startMainActivity(Boolean fromActivity){
        Intent intent = new Intent(App.getContext(), MainActivity.class);
        intent.setFlags(fromActivity ? Intent.FLAG_ACTIVITY_REORDER_TO_FRONT : Intent.FLAG_ACTIVITY_NEW_TASK );
        App.getContext().startActivity(intent);
    }
}
