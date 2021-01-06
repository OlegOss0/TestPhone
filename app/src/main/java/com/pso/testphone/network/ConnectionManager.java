package com.pso.testphone.network;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.pso.testphone.App;
import com.pso.testphone.AppLogger;
import com.pso.testphone.data.DataStorage;
import com.pso.testphone.gui.MainActivityPresenter;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;

import io.ipinfo.api.IPInfo;
import io.ipinfo.api.errors.RateLimitedException;
import io.ipinfo.api.model.IPResponse;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ConnectionManager {
    private static final String[] IP_SERVICE_URLS = {"https://api.my-ip.io/ip.json", "https://api.myip.com/"};
    private final static String GOOGLE_ADRESS = "www.google.com";

    public static void forcedСonnectionСheck() {
        App.getBgHandler().post(() ->{
            if(googleAdrressAvailable()){
                App.getMainHandler().post(App::registerReseivers);
            }
        });
    }

    private static boolean googleAdrressAvailable(){
        try {
            InetAddress ipAddr = InetAddress.getByName(GOOGLE_ADRESS);
            return !ipAddr.equals("");

        } catch (Exception e) {
            return false;
        }
    }


    public static class ConnectionChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
            if (activeNetInfo == null || !activeNetInfo.isConnected()/*|| !activeNetInfo.isAvailable()*/) {
                DataStorage.networkAvailable.set(false);
                DataStorage.activeNetInfoStr.set("");
                refreshIp();
                return;
            }
            if (activeNetInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                DataStorage.activeNetInfoStr.set("WIFI");
                setNetworkAvailable();
                refreshIp();
                return;
            }

            int netSubtype = activeNetInfo.getSubtype();
            switch (netSubtype) {
                case TelephonyManager.NETWORK_TYPE_CDMA:
                case TelephonyManager.NETWORK_TYPE_IDEN:
                case TelephonyManager.NETWORK_TYPE_1xRTT:
                case TelephonyManager.NETWORK_TYPE_EDGE:
                case TelephonyManager.NETWORK_TYPE_GPRS:
                case TelephonyManager.NETWORK_TYPE_GSM:
                    DataStorage.activeNetInfoStr.set("2G");
                    setNetworkAvailable();
                    refreshIp();
                    break;
                case TelephonyManager.NETWORK_TYPE_UMTS:
                case TelephonyManager.NETWORK_TYPE_EVDO_0:
                case TelephonyManager.NETWORK_TYPE_EVDO_A:
                case TelephonyManager.NETWORK_TYPE_HSDPA:
                case TelephonyManager.NETWORK_TYPE_HSUPA:
                case TelephonyManager.NETWORK_TYPE_HSPA:
                case TelephonyManager.NETWORK_TYPE_EVDO_B:
                case TelephonyManager.NETWORK_TYPE_EHRPD:
                case TelephonyManager.NETWORK_TYPE_HSPAP:
                case TelephonyManager.NETWORK_TYPE_TD_SCDMA:
                    DataStorage.activeNetInfoStr.set("3G");
                    setNetworkAvailable();
                    refreshIp();
                    break;
                case TelephonyManager.NETWORK_TYPE_LTE:
                case TelephonyManager.NETWORK_TYPE_IWLAN:
                    DataStorage.activeNetInfoStr.set("4G");
                    setNetworkAvailable();
                    refreshIp();
                    break;
                case TelephonyManager.NETWORK_TYPE_NR:
                    DataStorage.activeNetInfoStr.set("5G");
                    setNetworkAvailable();
                    refreshIp();
                    break;
                default:
                    DataStorage.activeNetInfoStr.set("");
                    DataStorage.networkAvailable.set(false);
                    break;
            }
        }

        private void setNetworkAvailable() {
            DataStorage.networkAvailable.set(true);
            RemoteServerHelper.getINSTANCE().completeFirstTask();
        }

        private void refreshIp() {
            App.getBgHandler().post(() -> {
                if (!DataStorage.networkAvailable.get()) {
                    DataStorage.ip.set("");
                    AppLogger.e("IP", "IP = " + DataStorage.ip);
                    MainActivityPresenter.refreshTitle();
                    return;
                }
                for (String url : IP_SERVICE_URLS) {
                    if (getIpFromService(url)) {
                        MainActivityPresenter.refreshTitle();
                        return;
                    }
                }
                MainActivityPresenter.refreshTitle();
            });
        }

        private boolean getIpFromService(final String url) {
            String responce = "";
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder().url(url).build();

            try (Response response = client.newCall(request).execute()) {
                responce = response.body().string();
                JSONObject obj = new JSONObject(responce);
                String ip = obj.getString("ip");
                if (!ip.isEmpty()) {
                    DataStorage.ip.set(ip);
                    AppLogger.e("IP", "IP = " + DataStorage.ip);
                }
                return true;
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
            return false;
        }

        private boolean getIpFromIpInfo() throws ClassNotFoundException {
            try {
                IPInfo ipInfo = IPInfo.builder().setToken(DataStorage.getIpInfoToken()).build();
                IPResponse response = ipInfo.lookupIP("");
                DataStorage.ip.set(response.getIp());
                AppLogger.e("IP", "IP = " + DataStorage.ip);
                return true;
            } catch (RateLimitedException ex) {
            }
            return false;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static class ConnectionStateMonitor extends ConnectivityManager.NetworkCallback {
        final NetworkRequest networkRequest;

        public ConnectionStateMonitor() {
            networkRequest = new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .build();
        }

        public void enable(Context context) {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            connectivityManager.registerNetworkCallback(networkRequest, this);
        }

        @Override
        public void onUnavailable() {
            super.onUnavailable();
        }

        @Override
        public void onAvailable(Network network) {
            Toast.makeText(App.getContext(), "ConnectionStateMonitor", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onLosing(@NonNull Network network, int maxMsToLive) {
            super.onLosing(network, maxMsToLive);
        }

        @Override
        public void onLost(@NonNull Network network) {
            super.onLost(network);
        }

        @Override
        public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
            super.onCapabilitiesChanged(network, networkCapabilities);
        }

        @Override
        public void onLinkPropertiesChanged(@NonNull Network network, @NonNull LinkProperties linkProperties) {
            super.onLinkPropertiesChanged(network, linkProperties);
        }

        @Override
        public void onBlockedStatusChanged(@NonNull Network network, boolean blocked) {
            super.onBlockedStatusChanged(network, blocked);
        }
    }
}
