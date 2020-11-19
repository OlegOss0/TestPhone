package com.pso.testphone.gui;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.pso.testphone.App;
import com.pso.testphone.BuildConfig;
import com.pso.testphone.R;
import com.pso.testphone.data.DataStorage;
import com.pso.testphone.network.RemoteServerHelper;
import com.pso.testphone.recervers.SystemBroadcastReceiver;
import com.pso.testphone.services.MainService;

import java.io.File;
import java.util.Objects;

import static com.pso.testphone.interfaces.ServerTaskListener.NEED_ENABLED_ALL_LOCATION;
import static com.pso.testphone.interfaces.ServerTaskListener.REBOOT_DEVICE;
import static com.pso.testphone.interfaces.ServerTaskListener.UPDATE_APP;
import static com.pso.testphone.interfaces.ServerTaskListener.INSTALL_ASSISTANT;

public class DialogActivity extends AppCompatActivity {
    public static boolean isShow = false;
    Button button;
    TextView tv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isShow = true;
        setContentView(R.layout.activity_dialog);
        Intent intent = getIntent();
        String action = intent.getAction();
        if (action == null || action.isEmpty())
            finishAffinity();

        tv = findViewById(R.id.textView);
        button = findViewById(R.id.btn_install);

        switch (action) {
            case UPDATE_APP:
                tv.setText(R.string.updateMsgTxt);
                button.setVisibility(View.VISIBLE);
                button.setText(R.string.updateBtnTxt);
                button.setOnClickListener(view -> {
                    File installApk = new File(App.getContext().getFilesDir() + "/" + DataStorage.getUpdateFileName());
                    if (installApk.exists()) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            Uri apkUri = FileProvider.getUriForFile(App.getContext(), BuildConfig.APPLICATION_ID + ".provider", installApk);
                            Intent intent1 = new Intent(Intent.ACTION_INSTALL_PACKAGE);
                            intent1.setData(apkUri);
                            intent1.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            intent1.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            startActivityForResult(intent1, DataStorage.APP_UPDATE_REQUEST);
                        } else {
                            Intent installApp = new Intent(Intent.ACTION_INSTALL_PACKAGE);
                            installApp.setData(Uri.fromFile(installApk));
                            installApp.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true);
                            installApp.putExtra(Intent.EXTRA_RETURN_RESULT, true);
                            installApp.putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, getApplicationContext().getApplicationInfo().packageName);
                            startActivityForResult(installApp, DataStorage.APP_UPDATE_REQUEST);
                        }
                    }
                });
                break;
            case INSTALL_ASSISTANT:
                File installApk = new File(App.getContext().getFilesDir() + "/" + DataStorage.APP_ASSISTANT_FILE_NAME);
                tv.setText(R.string.installMsgTxt);
                button.setVisibility(View.VISIBLE);
                button.setText(R.string.installBtnTxt);
                if(!installApk.exists()) {
                    tv.setText(R.string.unpackingMsgTxt);
                    button.setEnabled(false);
                    App.unpackAssistant();
                    tv.setText(R.string.installMsgTxt);
                    button.setEnabled(true);
                }
                button.setOnClickListener(view -> {
                    if (installApk.exists()) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            Uri apkUri = FileProvider.getUriForFile(Objects.requireNonNull(getApplicationContext()),
                                    BuildConfig.APPLICATION_ID + ".provider", installApk);
                            Intent intent1 = new Intent(Intent.ACTION_INSTALL_PACKAGE);
                            intent1.setData(apkUri);
                            intent1.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent1.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            DataStorage.setAssistantInstall(true);
                            startActivityForResult(intent1, DataStorage.APP_INSTALL_ASSISTANT_REQUEST);
                        } else {
                            Intent installApp = new Intent(Intent.ACTION_INSTALL_PACKAGE);
                            installApp.setData(Uri.fromFile(installApk));
                            installApp.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true);
                            installApp.putExtra(Intent.EXTRA_RETURN_RESULT, true);
                            installApp.putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, getApplicationContext().getApplicationInfo().packageName);
                            DataStorage.setAssistantInstall(true);
                            startActivityForResult(installApp, DataStorage.APP_INSTALL_ASSISTANT_REQUEST);
                        }
                    }
                });
                break;
            case REBOOT_DEVICE:
                tv.setText(R.string.rebootMsg);
                button.setVisibility(View.INVISIBLE);
                DataStorage.setLastTimeShowRebootMsg(System.currentTimeMillis());
                break;
            case NEED_ENABLED_ALL_LOCATION:
                int api = Build.VERSION.SDK_INT;
                String message = "";
                if(api <= 17){
                    message = "Необходимо включить в настройках " +
                            "\"Использовать GPS\" .";
                }else if(api >= 21 & api < 23){
                    message = "Необходимо включить в настройках" +
                            " \" Геоданные \" \n " +
                            " \" Режим обнаружения -> GPS,Wi-Fi и мобильные сети\".";
                }else if(api >= 23){
                    message = "Необходимо включить в настройках " +
                            "\" Геолокация \" Режим \"По всем источникам\".";
                }else {
                    message = "Необходимо включить \"Геоданные\" \n" +
                            " \"Метод обнаружения\" -> \"Высокая точность\" .";
                }
                tv.setText(message);
                button.setVisibility(View.VISIBLE);
                button.setText(R.string.settings);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        openLocationsSettings();
                    }
                });
                break;

        }
    }

    public void openLocationsSettings() {
        Intent appSettingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivityForResult(appSettingsIntent, DataStorage.REGUEST_CODE_GPS_ENABLE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        /*isShow = true;*/
    }

    @Override
    protected void onDestroy() {
        isShow = false;
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        //isShow = false;
        super.onStop();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case DataStorage.APP_UPDATE_REQUEST:
                SystemBroadcastReceiver.createAlarm();

                Intent stopService = new Intent(getApplicationContext(), MainService.class);
                stopService(stopService);

                DataStorage.setUpdateFileName("");
                finish();
                break;
            case DataStorage.APP_INSTALL_ASSISTANT_REQUEST:
                DataStorage.setAssistantInstall(true);
                DataStorage.noNeedUpdateTpAssistant();
                finish();
                break;
        }
    }
}

