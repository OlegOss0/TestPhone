package com.pso.testphone.location;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.GnssStatus;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;

import com.pso.testphone.App;
import com.pso.testphone.data.DataStorage;
import com.pso.testphone.data.DeviceInfo;
import com.pso.testphone.PermissionHelper;
import com.pso.testphone.db.Provider;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static android.location.GnssStatus.CONSTELLATION_BEIDOU;
import static android.location.GnssStatus.CONSTELLATION_GALILEO;
import static android.location.GnssStatus.CONSTELLATION_GLONASS;
import static android.location.GnssStatus.CONSTELLATION_GPS;
import static android.location.GnssStatus.CONSTELLATION_IRNSS;
import static android.location.GnssStatus.CONSTELLATION_QZSS;
import static android.location.GnssStatus.CONSTELLATION_SBAS;

public class LocationManager {
    private LocationListenerNetwork networkLocationListener;
    private LocationListenerGPS gpsLocationListener;
    private mGnssStatusCallback mGnssStatusCallback;
    private GpsStatusListener mGpsStatusListener;
    private android.location.LocationManager locationManager;
    public static AtomicReference<String> satellitesInfo = new AtomicReference<>("");
    private static final String TAG = LocationManager.class.getSimpleName();

    public LocationManager(){
        locationManager = (android.location.LocationManager) App.getContext().getSystemService(Context.LOCATION_SERVICE);
    }

    @SuppressLint("MissingPermission")
    public void initLocationListeners() {
        locationManager = (android.location.LocationManager) App.getContext().getSystemService(Context.LOCATION_SERVICE);
        assert locationManager != null;
        networkLocationListener = new LocationListenerNetwork();
        gpsLocationListener = new LocationListenerGPS();
        locationManager.requestLocationUpdates(android.location.LocationManager.GPS_PROVIDER, 5000, 10, gpsLocationListener, App.getBgHandler().getLooper());
        locationManager.requestLocationUpdates(android.location.LocationManager.NETWORK_PROVIDER, 5000, 10, networkLocationListener, App.getBgHandler().getLooper());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mGnssStatusCallback = new mGnssStatusCallback();
            locationManager.registerGnssStatusCallback(mGnssStatusCallback);
        } else {
            mGpsStatusListener = new GpsStatusListener();
            locationManager.addGpsStatusListener(mGpsStatusListener);
        }
    }

    @SuppressLint("MissingPermission")
    public HashMap<String, Provider> getAvailableProviders() {
        if(!PermissionHelper.hasLocationPermissions())
            return null;
        HashMap<String, Provider> providers = new HashMap<>();
        Location location = null;
        float bestAccuracy = -1;
        long minTime = -1, bestTime = -1;

        List<String> matchingProviders = locationManager.getAllProviders();
        for (String providerName : matchingProviders) {
            DeviceInfo.setProviderState(providerName, locationManager.isProviderEnabled(providerName));
            if (PermissionHelper.hasLocationPermissions()) {
                location = locationManager.getLastKnownLocation(providerName);
            }
            if (location != null) {
                float accuracy = location.getAccuracy();
                long time = location.getTime();

                if (location.getProvider().equals("gps")) {
                    DeviceInfo.FICTITIOUS_LOC = DeviceInfo.isMockLocationOn(location, App.getContext());
                    DataStorage.setGpsTime(time);
                }
                Provider provider = new Provider(providerName, location.getTime(), formatDouble(location.getLatitude(), 4)
                        , formatDouble(location.getLongitude(), 4), formatDouble(location.getAccuracy(), 0), 0);
                Log.e(TAG, "Provider = " + provider.name + ", longi = " + provider.longitude + ", lat = " + provider.latitude + ", accurate = " + provider.accurate);

                if (minTime == -1 && bestAccuracy == -1) {
                    minTime = time;
                    bestAccuracy = accuracy;
                }

                if (time > minTime && accuracy < bestAccuracy) {
                    bestAccuracy = accuracy;
                    bestTime = time;
                    provider.isBest = 1;
                } else if (time < minTime && bestAccuracy == Float.MAX_VALUE && time > bestTime) {
                    bestTime = time;
                }
                providers.put(providerName, provider);
            }
        }
        if (providers.size() > 1 && providers.containsKey("passive")) {
            providers.remove("passive");
        }
        return providers;
    }

    public void remoteLocationListeners() {
        satellitesInfo.set("");
        locationManager.removeUpdates(gpsLocationListener);
        locationManager.removeUpdates(networkLocationListener);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            locationManager.unregisterGnssStatusCallback(mGnssStatusCallback);
        }else{
            locationManager.removeGpsStatusListener(mGpsStatusListener);
        }
    }

    @SuppressLint("MissingPermission")
    private class GpsStatusListener implements GpsStatus.Listener {
        @Override
        public void onGpsStatusChanged(int event) {
            synchronized (satellitesInfo) {
                String satInfoStr = "";
                int i = 0;
                GpsStatus gpsStatus = locationManager.getGpsStatus(null);
                if (gpsStatus != null) {
                    Iterable<GpsSatellite> satellites = gpsStatus.getSatellites();
                    Iterator<GpsSatellite> sat = satellites.iterator();
                    while (sat.hasNext()) {
                        GpsSatellite satellite = sat.next();
                        satInfoStr += ". [Satellite = " + i++ + ". " + (satellite.usedInFix() ? "Used" : "Not used") + ". Signal " + (int) satellite.getSnr() + "%]";
                    }
                }
                String headerStr = "Satellite count = " + i;
                satellitesInfo.set("");
                satellitesInfo.set(headerStr + satInfoStr);
            }
        }
    }

    @SuppressLint("NewApi")
    private class mGnssStatusCallback extends GnssStatus.Callback {

        @Override
        public void onSatelliteStatusChanged(GnssStatus status) {
            synchronized (satellitesInfo) {
                String satInfoStr = "";
                int satCount = status.getSatelliteCount();
                satInfoStr = "Satellite count = " + satCount;
                for (int i = 0; i < satCount; i++) {
                    satInfoStr += ". [Satellite = " + i + ". " + (status.usedInFix(i) ? "Used" : "Not used") + ". " + consellationToName(status.getConstellationType(i)) + ". Signal " + (int) status.getCn0DbHz(i) + "%]";
                }
                satellitesInfo.set("");
                satellitesInfo.set(satInfoStr);
            }
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

    private static String consellationToName(int constellationType) {
        switch (constellationType) {
            case CONSTELLATION_BEIDOU:
                return "BEIDOU";
            case CONSTELLATION_GALILEO:
                return "GALILEO";
            case CONSTELLATION_GLONASS:
                return "GLONASS";
            case CONSTELLATION_GPS:
                return "GPS";
            case CONSTELLATION_IRNSS:
                return "IRNSS";
            case CONSTELLATION_QZSS:
                return "QZSS";
            case CONSTELLATION_SBAS:
                return "SBAS";
        }
        return "UNKNOWN";
    }

    private double formatDouble(double d, int dz) {
        double dd = Math.pow(10, dz);
        return Math.round(d * dd) / dd;
    }

    private static class LocationListenerNetwork implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {
            if (location != null) {
                Log.i(TAG, "onLocationChanged in Network");
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.i(TAG, "onStatusChanged provider" + provider);

        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.i(TAG, "onProviderEnabled " + provider);

        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.i(TAG, "onProviderDisabled " + provider);
        }
    }

    private static class LocationListenerGPS implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {
            if (location != null) {
                Log.i(TAG, "onLocationChanged in GPS");
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.i(TAG, "onStatusChanged provider" + provider);

        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.i(TAG, "onProviderEnabled " + provider);

        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.i(TAG, "onProviderDisabled " + provider);
        }
    }
}
