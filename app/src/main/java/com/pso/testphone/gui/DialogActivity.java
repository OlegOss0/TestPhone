package com.pso.testphone.gui;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.pso.testphone.App;
import com.pso.testphone.BuildConfig;
import com.pso.testphone.R;
import com.pso.testphone.data.DataStorage;
import com.pso.testphone.recervers.SystemBroadcastReceiver;

import java.io.File;
import java.util.Objects;

import static com.pso.testphone.data.DataStorage.APP_INSTALL_ASSISTANT_REQUEST;
import static com.pso.testphone.interfaces.ServerTaskListener.NEED_ENABLED_ALL_LOCATION;
import static com.pso.testphone.interfaces.ServerTaskListener.REBOOT_DEVICE;
import static com.pso.testphone.interfaces.ServerTaskListener.UPDATE_APP;
import static com.pso.testphone.interfaces.ServerTaskListener.INSTALL_ASSISTANT;
import static com.pso.testphone.interfaces.ServerTaskListener.UPDATE_ASSISTANT;

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
        if (action == null || action.isEmpty()) {
            finishAffinity();
            return;
        }
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
                        Uri apkUri;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            apkUri = FileProvider.getUriForFile(App.getContext(), BuildConfig.APPLICATION_ID + ".provider", installApk);
                        } else {
                            apkUri = Uri.fromFile(installApk);
                        }
                        SystemBroadcastReceiver.createAlarm();
                        installPackage(apkUri);
                    }
                });
                break;
            case INSTALL_ASSISTANT:
            case UPDATE_ASSISTANT:
                File installApk = new File(App.getContext().getFilesDir() + "/" + DataStorage.APP_ASSISTANT_FILE_NAME);
                tv.setText(R.string.installMsgTxt);
                button.setVisibility(View.VISIBLE);
                if (action == INSTALL_ASSISTANT) {
                    button.setText(R.string.installBtnTxt);
                    if (!installApk.exists()) {
                        tv.setText(R.string.unpackingMsgTxt);
                        button.setEnabled(false);
                        App.unpackAssistant();
                        tv.setText(R.string.installMsgTxt);
                        button.setEnabled(true);
                    }
                } else {
                    button.setText(R.string.updateBtnTxt);
                    if (installApk.exists()) {
                        installApk.delete();
                    }
                    tv.setText(R.string.unpackingMsgTxt);
                    button.setEnabled(false);
                    App.unpackAssistant();
                    if (installApk.exists()) {
                        tv.setText(R.string.updateAssistantMsgTxt);
                        button.setEnabled(true);
                    } else {
                        Toast.makeText(this, "Error unpack TPAssistant", Toast.LENGTH_LONG);
                    }
                }
                button.setOnClickListener(view -> {
                    if (installApk.exists()) {
                        SystemBroadcastReceiver.createAlarm();
                        Uri packageIri;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            packageIri = FileProvider.getUriForFile(Objects.requireNonNull(getApplicationContext()),
                                    BuildConfig.APPLICATION_ID + ".provider", installApk);
                        } else {
                            packageIri = Uri.fromFile(installApk);
                        }
                        if (packageIri != null) {
                            installPackage(packageIri);
                        } else {
                            Toast.makeText(this, "Uri install packege = null", Toast.LENGTH_LONG);
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
                if (api <= 17) {
                    message = "Необходимо включить в настройках " +
                            "\"Использовать GPS\" .";
                } else if (api >= 21 & api < 23) {
                    message = "Необходимо включить в настройках" +
                            " \" Геоданные \" \n " +
                            " \" Режим обнаружения -> GPS,Wi-Fi и мобильные сети\".";
                } else if (api >= 23) {
                    message = "Необходимо включить в настройках " +
                            "\" Геолокация \" Режим \"По всем источникам\".";
                } else {
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

    @SuppressLint("InlinedApi")
    private void installPackage(Uri uri) {
        if (uri == null) {
            Toast.makeText(this, "Uri install packege = null", Toast.LENGTH_LONG);
            finishAffinity();
            return;
        }
        if ((Build.VERSION.SDK_INT < 24) && (!uri.getScheme().equals("file"))) {
            Toast.makeText(this, "PackageInstaller < Android N only supports file scheme!", Toast.LENGTH_LONG);
            finishAffinity();
            return;
        }
        if ((Build.VERSION.SDK_INT >= 24) && (!uri.getScheme().equals("content"))) {
            Toast.makeText(this, "PackageInstaller >= Android N only supports content scheme!", Toast.LENGTH_LONG);
            finishAffinity();
            return;
        }
        Intent intent = new Intent();

        if (Build.VERSION.SDK_INT < 14) {
            intent.setAction(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
        } else if (Build.VERSION.SDK_INT < 16) {
            intent.setAction(Intent.ACTION_INSTALL_PACKAGE);
            intent.setData(uri);
            intent.putExtra(Intent.EXTRA_RETURN_RESULT, true);
            intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true);
            intent.putExtra(Intent.EXTRA_ALLOW_REPLACE, true);
        } else if (Build.VERSION.SDK_INT < 24) {
            intent.setAction(Intent.ACTION_INSTALL_PACKAGE);
            intent.setData(uri);
            intent.putExtra(Intent.EXTRA_RETURN_RESULT, true);
            intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true);
        } else { // Android N
            intent.setAction(Intent.ACTION_INSTALL_PACKAGE);
            intent.setData(uri);
            // grant READ permission for this content Uri
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.putExtra(Intent.EXTRA_RETURN_RESULT, true);
            intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true);
        }

        try {
            startActivityForResult(intent, APP_INSTALL_ASSISTANT_REQUEST);
        } catch (ActivityNotFoundException e) {
            //Log.e(TAG, "ActivityNotFoundException", e);
            /*installer.sendBroadcastInstall(downloadUri, Installer.ACTION_INSTALL_INTERRUPTED,
                    "This Android rom does not support ACTION_INSTALL_PACKAGE!");*/
            finish();
        }
        /*installer.sendBroadcastInstall(downloadUri, Installer.ACTION_INSTALL_STARTED);*/
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
                if (resultCode == RESULT_OK) {
                    //Intent stopService = new Intent(getApplicationContext(), MainService.class);
                    //stopService(stopService);
                    DataStorage.setUpdateFileName("");
                    finish();
                }
                break;
            case APP_INSTALL_ASSISTANT_REQUEST:
                if (resultCode == RESULT_OK) {
                    DataStorage.setAssistantInstall(true);
                    DataStorage.noNeedUpdateTpAssistant();
                    DataStorage.TP_ASSISTANT_VER = "";
                    App.deleteAssistantIntallFile();
                    finish();
                }
                break;
        }
    }
}

