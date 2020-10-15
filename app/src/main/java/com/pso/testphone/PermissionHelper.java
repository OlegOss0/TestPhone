package com.pso.testphone;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class PermissionHelper {

    public static void checkReadPhoneStatePermission(Activity activity) {
        if (!hasPermission(Manifest.permission.READ_PHONE_STATE)) {
            requestPermissions(activity, new String[]{Manifest.permission.READ_PHONE_STATE});
        }
    }

    public static void checkWriteExtStoragePermission(Activity activity){
        if (!hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE});
        }
    }

    public static void requestLocationPermissions(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requestPermissions(activity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION});
        }else{
            requestPermissions(activity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION});
        }
    }

    public static boolean hasLocationPermissions(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) && hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION) && hasPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        }else{
            return hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) && hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
    }


    public static boolean hasPermission(String permission) {
        return (ContextCompat.checkSelfPermission(App.getContext(), permission) ==
                PackageManager.PERMISSION_GRANTED);
    }

    public static boolean hasAllPermission(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return hasPermission(Manifest.permission.READ_PHONE_STATE) && hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    && hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) && hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION) && hasPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        }else{
            return hasPermission(Manifest.permission.READ_PHONE_STATE) && hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    && hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) && hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
    }

    public static void requestPermissions(Activity activity, String[] permissions) {
        List<String> permissionsList = new ArrayList<>();
        if (permissions.length > 0) {
            ActivityCompat.requestPermissions(activity, permissions, 123);
        }

    }


    public static boolean onRequestingPermissionsResult(String permissions[], int[] grantResults) {
        if (grantResults.length > 0) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    // TODO: 25.02.2020
                }
            }
        }
        return false;
    }
}
