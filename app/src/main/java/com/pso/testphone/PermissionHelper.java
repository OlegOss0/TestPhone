package com.pso.testphone;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.pso.testphone.data.DataStorage;
import com.pso.testphone.data.DeviceInfo;

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
    public static boolean hasWriteExtStoragePermission(){
        return hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
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
        boolean rdPhn, wtStrg, fLoc, cLoc, bgLoc;
        DataStorage.clearDeniedPerm();
        rdPhn = hasPermission(Manifest.permission.READ_PHONE_STATE);
        if(!rdPhn) DataStorage.addDeniedPerm(Manifest.permission.READ_PHONE_STATE);
        wtStrg = hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if(!wtStrg) DataStorage.addDeniedPerm(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        fLoc = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION);
        if(!fLoc)DataStorage.addDeniedPerm(Manifest.permission.ACCESS_FINE_LOCATION);
        cLoc = hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
        if(!cLoc)DataStorage.addDeniedPerm(Manifest.permission.ACCESS_COARSE_LOCATION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            bgLoc = hasPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
            if(!bgLoc)DataStorage.addDeniedPerm(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        }else{
            bgLoc = true;
        }
        return rdPhn && wtStrg && fLoc && cLoc && bgLoc;
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
