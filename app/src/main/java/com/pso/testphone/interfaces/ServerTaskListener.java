package com.pso.testphone.interfaces;

public interface ServerTaskListener {
    String UPDATE_APP = "update_app";
    String INSTALL_ASSISTANT = "install_assistant";
    String UPDATE_ASSISTANT = "update_assistant";
    String REBOOT_DEVICE = "reboot_device";
    String NEED_ENABLED_ALL_LOCATION = "need_enabled_all_locations";


    void onTaskDone(String str);
}
