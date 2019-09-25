package appcontest.sorrysori;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;


import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;



/**
 * Map tab content on measurement activity
 */
public class MapFragment extends Fragment {
    private View view;
    private WebView leaflet;
    private boolean isLocationLayerAdded = false;
    private final AtomicBoolean pageLoaded = new AtomicBoolean(false);
    private MapFragmentAvailableListener mapFragmentAvailableListener;
    private List<String> cachedCommands = new ArrayList<>();
/*
    public void setMapFragmentAvailableListener(MapFragmentAvailableListener mapFragmentAvailableListener) {
        this.mapFragmentAvailableListener = mapFragmentAvailableListener;
    }
*/
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if(view == null) {
            leaflet.clearCache(true);
            leaflet.clearHistory();
            WebSettings webSettings = leaflet.getSettings();
            webSettings.setJavaScriptEnabled(true);
            WebSettings settings = leaflet.getSettings();
            settings.setAppCachePath(new File(getContext().getCacheDir(), "webview").getPath());
            settings.setAppCacheEnabled(true);
            leaflet.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    pageLoaded.set(true);
                    if(mapFragmentAvailableListener != null) {
                        mapFragmentAvailableListener.onPageLoaded(MapFragment.this);
                    }
                }

                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(browserIntent);
                    return true;
                }
            });
            if(mapFragmentAvailableListener != null) {
                mapFragmentAvailableListener.onMapFragmentAvailable(this);
            }
        }
        return view;
    }

    /**
     * @return The WebView control
     */
    /*
    public WebView getWebView() {
        return leaflet;
    }

    public void loadUrl(String url) {
        if(leaflet != null) {
            leaflet.loadUrl(url);
        }
    }
    */

    public boolean runJs(String js) {
        if(leaflet != null && pageLoaded.get()) {
            // Run cached commands before this new command
            cachedCommands.add(js);
            while(!cachedCommands.isEmpty()) {
                js = cachedCommands.remove(0);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    leaflet.evaluateJavascript(js, null);
                } else {
                    leaflet.loadUrl("javascript:" + js);
                }
            }
            return true;
        }
        return false;
    }
    /*
    public void updateLocationMarker(LatLng newLocation, double precision) {
        if(runJs("updateLocation(["+newLocation.getLat()+","+newLocation.getLng()+"], "+precision+")")) {
            if (!isLocationLayerAdded) {
                addLocationMarker();
                isLocationLayerAdded = true;
            }
        }
    }
    */
    private void addLocationMarker() {
        runJs("userLocationLayer.addTo(map)");
    }
    /*
    public void addMeasurement(LatLng location, String htmlColor) {
        if(lastPt != null) {
            float[] result = new float[3];
            Location.distanceBetween(lastPt.lat, lastPt.lng, location.lat, location.lng, result);
            if(result[0] < ignoreNewPointDistanceDelta) {
                return;
            }
        }
        lastPt = location;
        String command = "addMeasurementPoint(["+location.getLat()+","+location.getLng()+"], '"+htmlColor+"')";
        if(!runJs(command)) {
            cachedCommands.add(command);
        }
    }
    */
    public static final class LatLng {
        double lat;
        double lng;
        double alt = 0;

        public LatLng(double lat, double lng) {
            this.lat = lat;
            this.lng = lng;
        }

        public LatLng(double lat, double lng, double alt) {
            this.lat = lat;
            this.lng = lng;
            this.alt = alt;
        }

        public double getLat() {
            return lat;
        }

        public double getLng() {
            return lng;
        }
    }

    public interface MapFragmentAvailableListener {
        void onMapFragmentAvailable(MapFragment mapFragment);
        void onPageLoaded(MapFragment mapFragment);
    }

}
