package appcontest.sorrysori;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {


    private static final int REQUEST_CODE_PERMISSIONS = 1000;
    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationClient;//현재위치를 저장할 인스턴스

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i("TAG", "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        // Fragment로 되어있는 activity_map.xml을 가져온다.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);//현재위치 얻는 과정.
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */

    //onCreate에서 MapAsync 즉, 맵 싱크가 맞으면 onMapReady를 실행시킨다(콜백을 받는다).
    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.i("TAG", "onMapReady");
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        // LatLng(위도,경도). Lat=Latitude위도, Lng=Longitude경도
        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(17.0f));//해당 위치를 잡고 처음 보이는 지도가 줌을 꽤 잡은 위치를 보여준다. 보통 2.0f~22.0f사이값.

        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                Intent intent = new Intent(Intent.ACTION_DIAL);//암시적 인텐트 사용. 클릭시 전화걸기
                intent.setData(Uri.parse("tel:01046580739"));
                if (intent.resolveActivity(getPackageManager()) != null) {//암시적인텐트가 없으면 실행햐지 않겠다는 예외문
                    startActivity(intent);
                }
            }
        });
    }

    public void onLastLocationButtonClicked(View view) {
        Log.i("TAG", "onLastLocationButtonClicked");
        //현재위치 끌어오는 것에 대한 권한 체크 Manifest.permission.ACCESS_FINE_LOCATION : 메니페스트에서 설정했던 권한. FINE만 설정해주면 COARSE도 자동으로 권한체크됨.
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            //String[]{~}은 String 배열
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_CODE_PERMISSIONS);
            return;
            //if문 안의 조건이 참이다 = 권한이 없다 = ActivityCompat으로 권한을 요청한다(requestPermissions). return하고 여기까지만 실행한다
        }

        //if문 안의 조건이 거짓이다 = 권한이 있다 = 여기부터도 실행된다.
        //mFusedLocationClient에 getLstLocation()으로 현재위치 가져오고 성공(Success)한다면 OnSuccessListener가 동작.
        mFusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    LatLng myLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.addMarker(new MarkerOptions().position(myLocation).title("현재위치"));
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(myLocation));
                    mMap.animateCamera(CameraUpdateFactory.zoomTo(17.0f));
                }
            }
        });
    }

    //사용자 요청 처리
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {//requestCode가 여러개(다양한 request)일 경우를 대비해 switch문 작성
            case REQUEST_CODE_PERMISSIONS:
                //onLastLocationButtonClicked의 if문에서 권한체크안됐을때를 체크하는 if문 다시 사용하자
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "권한 체크 거부 됨", Toast.LENGTH_SHORT).show();
                }
        }
        //권한이 있다는체 확인되면 OnSuccessListener로 연결하면 좋겠지만 지금은 그렇지 못한 수준.
        //권한 요청 수락 후 앱 나갔다 다시 들어오면 버튼을 클릭했을 때 OnSuccessListener가 동작한다.
        //그러면 현재위치를 찾아서 마커찍고(addmarker), 카메라 움직(CameraUpdateFactory.newLatLng)이고, 카메라줌인(zoomTo)한다.
    }

    public void back(View view){
        finish();
    }
}