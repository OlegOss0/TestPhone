package com.pso.testphone.gui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.storage.StorageManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.pso.testphone.App;
import com.pso.testphone.BuildConfig;
import com.pso.testphone.PermissionHelper;
import com.pso.testphone.R;
import com.pso.testphone.data.DataStorage;
import com.pso.testphone.network.RemoteServerHelper;
import com.pso.testphone.interfaces.ServerTaskListener;

import static com.pso.testphone.PermissionHelper.checkWriteExtStoragePermission;
import static com.pso.testphone.PermissionHelper.hasPermission;
import static com.pso.testphone.recervers.SystemBroadcastReceiver.startDataCollectorService;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, ServerTaskListener {
    private Button btnSend, btnClearDB, btnClosed, btnCheckUpdate, btnClearInfo;
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
        btnClearInfo.setOnClickListener(this);
        btnSend.setOnClickListener(this);
        btnClearDB.setOnClickListener(this);
        btnClosed.setOnClickListener(this);
        btnCheckUpdate.setOnClickListener(this);
        tv = findViewById(R.id.tvInfo);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getSupportActionBar().getTitle() + " " + BuildConfig.VERSION_NAME);
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
        if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) || !hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            hasLocationPermission = false;
            PermissionHelper.requestLocationPermissions(this);
        }
        if (!hasPermission(Manifest.permission.READ_PHONE_STATE)) {
            hasReadPhoneStatePermission = false;
            PermissionHelper.checkReadPhoneStatePermission(this);
        }
        if (!hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            hasWriteExternalStoragePermission = false;
            PermissionHelper.checkWriteExtStoragePermission(this);
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
        MainActivityPresenter.onViewDestroy();
        super.onPause();
    }

    private void startMainService() {
        startDataCollectorService(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        for (int i = 0; i < permissions.length; i++) {
            if (permissions[i].equals(Manifest.permission.ACCESS_COARSE_LOCATION) && grantResults[i] == PackageManager.PERMISSION_GRANTED && hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
                hasLocationPermission = true;
            }
            if (permissions[i].equals(Manifest.permission.ACCESS_FINE_LOCATION) && grantResults[i] == PackageManager.PERMISSION_GRANTED && hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                hasLocationPermission = true;
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

    /**//*@Override
    public void onTaskFailed() {
        Toast.makeText(App.getContext(), "Download filed", Toast.LENGTH_LONG).show();
    }*/

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnSend:
                if (hasReadPhoneStatePermission) {
                    RemoteServerHelper.getINSTANCE().sendFileToServer();
                } else {
                    PermissionHelper.checkReadPhoneStatePermission(getParent());
                }
                break;
            case R.id.btn_closed:
                finish();
                break;
            case R.id.btnClear:
                App.getDataBase().clearAllTables();
                break;
            case R.id.btn_chk_update:
                if (!hasWriteExternalStoragePermission) {
                    checkWriteExtStoragePermission(this);
                } else {
                    RemoteServerHelper.getINSTANCE().bindServerTaskListener(this);
                    RemoteServerHelper.getINSTANCE().downloadUpdateFile();
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
