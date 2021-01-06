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
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.pso.testphone.App;
import com.pso.testphone.BuildConfig;
import com.pso.testphone.R;
import com.pso.testphone.data.DataStorage;
import com.pso.testphone.data.DeviceInfo;
import com.pso.testphone.recervers.SystemBroadcastReceiver;

import java.io.File;
import java.util.Objects;

import static com.pso.testphone.data.DataStorage.APP_INSTALL_ASSISTANT_REQUEST;
import static com.pso.testphone.data.DataStorage.APP_UPDATE_ASSISTANT_REQUEST;
import static com.pso.testphone.data.DataStorage.APP_UPDATE_REQUEST;
import static com.pso.testphone.interfaces.ServerTaskListener.NEED_ENABLED_ALL_LOCATION;
import static com.pso.testphone.interfaces.ServerTaskListener.REBOOT_DEVICE;
import static com.pso.testphone.interfaces.ServerTaskListener.UPDATE_APP;
import static com.pso.testphone.interfaces.ServerTaskListener.INSTALL_ASSISTANT;
import static com.pso.testphone.interfaces.ServerTaskListener.UPDATE_ASSISTANT;

public class DialogActivity extends AppCompatActivity implements View.OnClickListener {
    public static boolean isShow = false;
    Button button;
    TextView tv;
    private String action = "";
    private File finalInstallApk;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isShow = true;
        setContentView(R.layout.activity_dialog);
        Intent intent = getIntent();
        String intentAction = intent.getAction();
        if (intentAction == null || intentAction.isEmpty()) {
            finishAffinity();
            return;
        }
        this.action = intentAction;
        tv = findViewById(R.id.textView);
        button = findViewById(R.id.btn_install);
        button.setOnClickListener(this);
        finalInstallApk = null;

        switch (action) {
            case UPDATE_APP:
                tv.setText(R.string.updateMsgTxt);
                button.setVisibility(View.VISIBLE);
                button.setText(R.string.updateBtnTxt);
                finalInstallApk = DataStorage.getAppUpdateFile();
                break;
            case INSTALL_ASSISTANT:
                finalInstallApk = new File(App.getContext().getFilesDir() + "/" + DataStorage.APK_ASSISTANT_FILE_NAME);
                button.setVisibility(View.VISIBLE);
                tv.setText(R.string.installMsgTxt);
                button.setText(R.string.installBtnTxt);
                if (!finalInstallApk.exists()) {
                    button.setEnabled(false);
                    App.unpackAssistant();
                    button.setEnabled(true);
                }
                break;
            case UPDATE_ASSISTANT:
                finalInstallApk = DataStorage.getAssistantUpdateFile();
                button.setVisibility(View.VISIBLE);
                button.setText(R.string.updateBtnTxt);
                tv.setText(R.string.updateAssistantMsgTxt);
                button.setEnabled(true);
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
                break;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        isShow = false;
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }


    @SuppressLint("InlinedApi")
    private void installPackage(Uri uri, int requestCode) {
        if (uri == null) {
            Toast.makeText(this, "Uri install package = null", Toast.LENGTH_SHORT).show();
            finishAffinity();
            return;
        }
        if ((Build.VERSION.SDK_INT < 24) && (!uri.getScheme().equals("file"))) {
            Toast.makeText(this, "PackageInstaller < Android N only supports file scheme!", Toast.LENGTH_SHORT).show();
            finishAffinity();
            return;
        }
        if ((Build.VERSION.SDK_INT >= 24) && (!uri.getScheme().equals("content"))) {
            Toast.makeText(this, "PackageInstaller >= Android N only supports content scheme!", Toast.LENGTH_SHORT).show();
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
            intent.putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, getApplicationContext().getApplicationInfo().packageName);
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
            startActivityForResult(intent, requestCode);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "ActivityNotFoundException ", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    public void onClick(View view) {
        if (action != null) {
            switch (action) {
                case UPDATE_APP:
                case UPDATE_ASSISTANT:
                case INSTALL_ASSISTANT:
                    if (finalInstallApk != null && finalInstallApk.exists()) {
                        int requestCode = generateRequestCode(action);
                        SystemBroadcastReceiver.createAlarm();
                        Uri packageIri;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            packageIri = FileProvider.getUriForFile(Objects.requireNonNull(getApplicationContext()),
                                    BuildConfig.APPLICATION_ID + ".provider", finalInstallApk);
                        } else {
                            packageIri = Uri.fromFile(finalInstallApk);
                        }
                        if (packageIri != null) {
                            installPackage(packageIri, requestCode);
                        } else {
                            Toast.makeText(this, "Uri install package = null", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(this, "Error unpacking the archive ", Toast.LENGTH_LONG).show();
                        finishAffinity();
                    }
                    break;
                case NEED_ENABLED_ALL_LOCATION:
                    openLocationsSettings();
                    finishAffinity();
                    break;
            }
        }
    }

    private int generateRequestCode(String action) {
        if (action.equals(UPDATE_APP)) {
            return APP_UPDATE_REQUEST;
        } else if (action.equals(INSTALL_ASSISTANT)) {
            return APP_INSTALL_ASSISTANT_REQUEST;
        } else if (action.equals(UPDATE_ASSISTANT)) {
            return APP_UPDATE_ASSISTANT_REQUEST;
        }
        return -1;
    }


    public void openLocationsSettings() {
        Intent appSettingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivityForResult(appSettingsIntent, DataStorage.REGUEST_CODE_GPS_ENABLE);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case APP_UPDATE_REQUEST:
                if (resultCode == RESULT_OK) {
                    App.deleteApkFile(finalInstallApk);
                    finalInstallApk = null;
                    resetVersionValues(APP_UPDATE_REQUEST);
                    finish();
                } else if (requestCode == RESULT_FIRST_USER) {
                    App.deleteApkFile(finalInstallApk);
                }
                break;
            case APP_INSTALL_ASSISTANT_REQUEST:
                if (resultCode == RESULT_OK) {
                    DataStorage.setAssistantInstall(true);
                    App.deleteApkFile(finalInstallApk);
                    DeviceInfo.getInstallApplications(App.getContext());
                    finalInstallApk = null;
                    finish();
                } else if (requestCode == RESULT_FIRST_USER) {
                    App.deleteApkFile(finalInstallApk);
                }
                break;
            case APP_UPDATE_ASSISTANT_REQUEST:
                if (resultCode == RESULT_OK) {
                    DataStorage.setAssistantCurVersion(DataStorage.getAppAssistantAvailableVersion());
                    DataStorage.setAssistantInstall(true);
                    App.deleteApkFile(finalInstallApk);
                    finalInstallApk = null;
                    finish();
                } else if (requestCode == RESULT_FIRST_USER) {
                    App.deleteApkFile(finalInstallApk);
                }
                break;
        }
    }

    private void resetVersionValues(int request) {
        switch (request) {
            case APP_UPDATE_REQUEST:
                DataStorage.setAppAvailableVersion("");
                break;
            case APP_UPDATE_ASSISTANT_REQUEST:
                DataStorage.setAssistantCurVersion("");
                DataStorage.setAssistantAvailableVersion("");
                break;
        }
    }
}

