package com.pso.testphone.gui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.pso.testphone.App;
import com.pso.testphone.AppLogger;
import com.pso.testphone.BuildConfig;
import com.pso.testphone.PermissionHelper;
import com.pso.testphone.R;
import com.pso.testphone.data.DataStorage;
import com.pso.testphone.network.RemoteServerHelper;
import com.pso.testphone.interfaces.ServerTaskListener;

import static com.pso.testphone.PermissionHelper.checkWriteExtStoragePermission;
import static com.pso.testphone.PermissionHelper.hasPermission;
import static com.pso.testphone.data.Codes.BTN_PRESSED;
import static com.pso.testphone.data.Codes.CHECK_UPDATE_PRESSED_MSG;
import static com.pso.testphone.data.Codes.CLEAR_DB_PRESSED_MSG;
import static com.pso.testphone.data.Codes.CLOSED_PRESSED_MSG;
import static com.pso.testphone.data.Codes.SEND_DATA_FILE_SEND_PRESSED_MSG;
import static com.pso.testphone.data.Codes.SEND_LOG_FILE_SEND_PRESSED_MSG;
import static com.pso.testphone.recervers.SystemBroadcastReceiver.startDataCollectorService;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, ServerTaskListener {
    private Button btnSend, btnClearDB, btnClosed, btnCheckUpdate, btnClearInfo, btnSendLogs;
    private boolean hasLocationPermission = true;
    private boolean hasReadPhoneStatePermission = true;
    private boolean hasWriteExternalStoragePermission = true;
    private TextView tv;
    private View mainContainer, passwordContainer;
    private EditText editTextTextPassword;
    private int optionMenuId = -1;
    private Menu mMenu;
    private Button btnCheckPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mainContainer = findViewById(R.id.main_container);
        passwordContainer = findViewById(R.id.password_container);
        editTextTextPassword = findViewById(R.id.editTextTextPassword);
        btnCheckPassword = findViewById(R.id.check_password);
        btnCheckPassword.setOnClickListener(this);
        btnSend = findViewById(R.id.btnSend);
        btnClearDB = findViewById(R.id.btnClear);
        btnClosed = findViewById(R.id.btn_closed);
        btnCheckUpdate = findViewById(R.id.btn_chk_update);
        btnClearInfo = findViewById(R.id.btn_clear_info);
        btnSendLogs = findViewById(R.id.btn_send_logs);
        btnSendLogs.setOnClickListener(this);
        btnClearInfo.setOnClickListener(this);
        btnSend.setOnClickListener(this);
        btnClearDB.setOnClickListener(this);
        btnClosed.setOnClickListener(this);
        btnCheckUpdate.setOnClickListener(this);
        tv = findViewById(R.id.tvInfo);

        refreshStatusBar();
        MainActivityPresenter.onViewReady(this);
    }

    public void refreshStatusBar() {
        String appName = "TestPhone";
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("");
            getSupportActionBar().setTitle(appName + " " + BuildConfig.VERSION_NAME + " IP=" + DataStorage.ip);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        mMenu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switchView(true);
        optionMenuId = item.getItemId();
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected boolean onPrepareOptionsPanel(View view, Menu menu) {
        menu.getItem(0).setChecked(DataStorage.isAdditionalLoggingEnabled());
        return super.onPrepareOptionsPanel(view, menu);
    }

    private void switchView(boolean passwordVisible) {
        if (passwordVisible) {
            mainContainer.setVisibility(View.GONE);
            passwordContainer.setVisibility(View.VISIBLE);
            editTextTextPassword.requestFocus();
        } else {
            mainContainer.setVisibility(View.VISIBLE);
            passwordContainer.setVisibility(View.GONE);
        }
    }

    private void checkAllPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) || !hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION) || !hasPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                hasLocationPermission = false;
                PermissionHelper.requestLocationPermissions(this);
            }
        } else {
            if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) || !hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                hasLocationPermission = false;
                PermissionHelper.requestLocationPermissions(this);
            }

        }
        if (!hasPermission(Manifest.permission.READ_PHONE_STATE)) {
            hasReadPhoneStatePermission = false;
            PermissionHelper.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE});
        }
        if (!hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            hasWriteExternalStoragePermission = false;
            PermissionHelper.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE});
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        MainActivityPresenter.onViewDestroy();
        RemoteServerHelper.getINSTANCE().unBindServerTaskListener(this);
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        MainActivityPresenter.onViewReady(this);
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkAllPermission();
        }
        if (hasLocationPermission && hasReadPhoneStatePermission && hasWriteExternalStoragePermission) {
            startMainService();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private void startMainService() {
        startDataCollectorService(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        for (int i = 0; i < permissions.length; i++) {
            if ((Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)){
                if(hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION) && hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)){
                    hasLocationPermission = true;
                }
            }else{
                if(hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION) && hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) && hasPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)){
                    hasLocationPermission = true;
                }
            }
            if (permissions[i].equals(Manifest.permission.READ_PHONE_STATE) && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                hasReadPhoneStatePermission = true;
            }
            if (permissions[i].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE) && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                hasWriteExternalStoragePermission = true;
            }
        }
        if (hasLocationPermission && hasReadPhoneStatePermission) {
            startDataCollectorService(this);
        }
    }

    @Override
    public void onTaskDone(String string) {
        RemoteServerHelper.getINSTANCE().unBindServerTaskListener(this);
        Intent intent = new Intent(App.getContext(), DialogActivity.class);
        intent.setAction(string);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnSend:
                AppLogger.writeLog(BTN_PRESSED, SEND_DATA_FILE_SEND_PRESSED_MSG);
                RemoteServerHelper.getINSTANCE().completeTask(RemoteServerHelper.TaskType.SendData);
                break;
            case R.id.btn_send_logs:
                AppLogger.writeLog(BTN_PRESSED, SEND_LOG_FILE_SEND_PRESSED_MSG);
                RemoteServerHelper.getINSTANCE().completeTask(RemoteServerHelper.TaskType.SendLogs);
                break;
            case R.id.btn_closed:
                AppLogger.writeLog(BTN_PRESSED, CLOSED_PRESSED_MSG);
                finish();
                break;
            case R.id.btnClear:
                AppLogger.writeLog(BTN_PRESSED, CLEAR_DB_PRESSED_MSG);
                App.getDataBase().clearAllTables();
                break;
            case R.id.btn_chk_update:
                if (!hasWriteExternalStoragePermission) {
                    checkWriteExtStoragePermission(this);
                } else {
                    AppLogger.writeLog(BTN_PRESSED, CHECK_UPDATE_PRESSED_MSG);
                    RemoteServerHelper.getINSTANCE().bindServerTaskListener(this);
                    RemoteServerHelper.getINSTANCE().completeTask(RemoteServerHelper.TaskType.getUpdate);
                }
                break;
            case R.id.btn_clear_info: {
                MainActivityPresenter.clearMsg();
                break;
            }
            case R.id.check_password:
                String pass = editTextTextPassword.getText().toString();
                hideKeyboard();
                if (!pass.isEmpty() && pass.equals(DataStorage.getAdminPassword())) {
                    switch (optionMenuId) {
                        case R.id.logging_menu_item:
                            DataStorage.switchAdditionalLogging(!mMenu.getItem(0).isChecked());
                            mMenu.getItem(0).setChecked(!mMenu.getItem(0).isChecked());

                            break;
                        case R.id.clear_db_menu_item:
                            App.getDataBase().clearAllTables();
                            break;
                    }
                    switchView(false);
                } else {
                    Toast.makeText(this, "Wrong password", Toast.LENGTH_LONG).show();
                    switchView(false);
                }
                editTextTextPassword.setText("");
        }
    }

    public void setMsg(String msg) {
        tv.append(msg);
    }

    public void clearMsg() {
        tv.setText("");
    }

    public void hideKeyboard() {
        InputMethodManager imm = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            imm = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
        }
        if (imm != null) {
            View view = this.getCurrentFocus();
            if (view != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }
}
