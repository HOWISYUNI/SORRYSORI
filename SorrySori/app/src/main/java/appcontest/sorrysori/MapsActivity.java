package appcontest.sorrysori;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;


import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static android.widget.Toast.LENGTH_LONG;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback{

    private static final int GPS_ENABLE_REQUEST_CODE = 2001;
    private static final int PERMISSIONS_REQUEST_CODE = 100;
    String[] REQUIRED_PERMISSIONS = {Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION};
    private GpsTracker gpsTracker;
    private GoogleMap mMap;
    private FirebaseAuth mAuth;
    private FusedLocationProviderClient mFusedLocationClient;//현재위치를 저장할 인스턴스
    DatabaseReference mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();

    public MapsActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i("TAG", "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        // Fragment로 되어있는 activity_map.xml을 가져온다.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.MapsForLocation);
        // onMapReady 메서드와 연결
        mapFragment.getMapAsync(this);

        //현재위치 얻는 과정. onLastLocationButtonClicked와 연결
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if(!checkLocationServicesStatus()){
            showdialogForLocationServicesSetting();
        }else{
            checkRunTimePermission();
        }
    }

    private void checkRunTimePermission() {
        /*런타임 퍼미션을 체크해봅시다
        위치퍼미션 체크 -> [갖고있다면] 버튼클릭해서 위치를 가져옵니다 / [퍼미션이 없다면] 퍼미션거부한적 있는경우 vs 없는경우
        거부한적 있는경우 -> 퍼미션 이유 설명 -> 퍼미션 요청 = onRequestPermissionsResult() -> 퍼미션을 갖게될지도!
        거부한적 없는경우 -> 퍼미션요청 = onRequestPermissionsResult() -> 퍼미션을 갖게될지도!
        */

        // 1. 위치퍼미션을 갖고있는지 체크합니다.
        int hasFineLocationPermission = ContextCompat.checkSelfPermission(MapsActivity.this,Manifest.permission.ACCESS_FINE_LOCATION);
        int hasCoarseLocationPermission = ContextCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION);

        //2. 이미 퍼미션을 갖고있다면
        if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED && hasCoarseLocationPermission == PackageManager.PERMISSION_GRANTED){
            // 3. 버튼을 클릭해서 퍼미션을 가져옵니다.
        }else{
            // 2. 퍼미션이 없다면

            // 3-1. 퍼미션을 거부한 적이 있다면 
            if(ActivityCompat.shouldShowRequestPermissionRationale(MapsActivity.this,REQUIRED_PERMISSIONS[0])){
                // 3-2 퍼미션 요청 전에 퍼미션이 필요한 이유를 설명
                Toast.makeText(MapsActivity.this, "이 앱을 실행하려면 위치 접근 권한이 필요합니다", Toast.LENGTH_LONG).show();
                // 3-3. 사용자에게 퍼미션을 요청. 요청결과는 onRequestPermissionResult에서 수신된다.
                ActivityCompat.requestPermissions(MapsActivity.this, REQUIRED_PERMISSIONS,PERMISSIONS_REQUEST_CODE);
            }else{
                // 4. 유저가 퍼미션 거부를 한 적이 없는 경우 퍼미션 요청을 바로한다.
                // 요청 결과는 onRequestPermissionsResult에서 수신된다.
                ActivityCompat.requestPermissions(MapsActivity.this, REQUIRED_PERMISSIONS,PERMISSIONS_REQUEST_CODE);
            }
        }
    }

    private void showdialogForLocationServicesSetting() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
        builder.setTitle("위치 서비스 비활성화 알림");
        builder.setMessage("앱을 사용하기 위해서는 위치 서비스 권한이 필요합니다.\n"+"위치 설정을 수정하시겠습니까?");
        builder.setCancelable(true);//AlertDialog가 떳을때 빈공간을 터치하면 뒤로 돌아가는걸 허용. false는 허용하지않음

        builder.setPositiveButton("설정", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent callGPSSettingIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(callGPSSettingIntent, GPS_ENABLE_REQUEST_CODE);//onActivityResult로 결과를 전달한다. GPS_ENABLE_REQUEST_CODE로 구분한다
            }
        });

        builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.create().show(); // dialog를 보여줘!!
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode){
            case GPS_ENABLE_REQUEST_CODE:
                //사용자가 GPS 활성 시켰는지 검사
                if(checkLocationServicesStatus()){
                    if(checkLocationServicesStatus()){
                        Log.d("@@@", "onActivityResult ; GPS 활성화됨");
                        checkRunTimePermission();
                        return;
                    }
                }
                break;
        }
    }

    private boolean checkLocationServicesStatus() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)||locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
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

    //onCreate에서 MapAsync 즉, 맵 싱크가 맞으면(map이 사용 가능하면) onMapReady를 실행시킨다(콜백을 받는다).
    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.i("TAG", "onMapReady");
        mMap = googleMap;

        /*
        // Add a marker in Sydney and move the camera
        // LatLng(위도,경도). Lat=Latitude위도, Lng=Longitude경도
        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(17.0f));//해당 위치를 잡고 처음 보이는 지도가 줌을 꽤 잡은 위치를 보여준다. 보통 2.0f~22.0f사이값.*/

        // onLastLocationButtonClicked 버튼이 클릭되면 연결되는 메서드
        /*mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                Intent intent = new Intent(Intent.ACTION_DIAL);//암시적 인텐트 사용. 클릭시 전화걸기
                intent.setData(Uri.parse("tel:01046580739"));
                if (intent.resolveActivity(getPackageManager()) != null) {//암시적인텐트가 없으면 실행햐지 않겠다는 예외문
                    startActivity(intent);
                }
            }
        })*/;
    }

    public void onLastLocationButtonClicked(View view) {
        Log.i("TAG", "onLastLocationButtonClicked");

        //현재위치 끌어오는 것에 대한 권한 체크 Manifest.permission.ACCESS_FINE_LOCATION : 메니페스트에서 설정했던 권한. FINE만 설정해주면 COARSE도 자동으로 권한체크됨.
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            //String[]{~}은 String 배열
            ActivityCompat.requestPermissions(this,
                    new String[] {Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSIONS_REQUEST_CODE);
            return;
            //if문 안의 조건이 참이다 = 권한이 없다 = ActivityCompat으로 권한을 요청한다(requestPermissions). return하고 여기까지만 실행한다
        }

        //if문 안의 조건이 거짓이다 = 권한이 있다 = 여기부터도 실행된다.
        //mFusedLocationClient에 getLastLocation()으로 현재위치 가져오고 성공(Success)한다면 OnSuccessListener가 동작.
        mFusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if(location != null){
                    LatLng myLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.addMarker(new MarkerOptions().position(myLocation).title("현재위치"));
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(myLocation));
                    mMap.animateCamera(CameraUpdateFactory.zoomTo(17.0f));
                }
            }
        });

        //위도(latitude) 경도(longitude)값 받아오기
        double latitude, longitude;

        gpsTracker = new GpsTracker(MapsActivity.this);
        latitude = gpsTracker.getLatitude();
        longitude = gpsTracker.getLongitude();

        String address = getCurrentAddress(latitude,longitude);
        Toast.makeText(this, address, Toast.LENGTH_LONG).show();

        FirebaseUser user = mAuth.getCurrentUser();
        String mail = user.getEmail();



        mFirebaseDatabaseReference.child("User").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override

            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

//                                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
//                                    if(mail.equals(snapshot.child("email").getValue())){
//                                        Log.d("MainActivity", String.valueOf(snapshot.child("email").getValue()));
//                                        Toast.makeText(MainActivity.this, "중복된 이메일입니다.", Toast.LENGTH_LONG).show();
//                                    } else{
//                                        Log.d("MainActivity", "왜 중복값이 아니죠?");
//
//                                        Log.d("MainActivity", String.valueOf(snapshot.child("email")));
                // User user1 = new User(user.getEmail(), "asdasd");
//                                        mFirebaseDatabaseReference.child("User").push().setValue(user1);
//                                    }
//                                }
                Iterator<DataSnapshot> child = dataSnapshot.getChildren().iterator();
                while (child.hasNext()) {//마찬가지로 중복 유무 확인
                    if (!(mail.equals(child.next().getKey()))) {
                        mFirebaseDatabaseReference.child("User").child("address").setValue(address);
                        return;
                    } else {
                       // User user1 = new User(user.getEmail(), "asdasd");
                       // mFirebaseDatabaseReference.child("User").push().setValue(user1);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });



       //  Map<String, Object> childUpdates = new HashMap<>(); // 디비 쌓는 User 클래스에 Hash 가 있어야합니다.
      //   Map<String, Object> postValues = null;

       // mUser 객체 생성.
        // User mUser = new User( user , email, address); // user랑 address만 수정하는 생성자가 있으면 좋겠네요.
        // postValues = User.toMap();
        // childUpdates.put("여기엔 유저정보 db 최상위 디렉터리" + user, mPsotReference.updateChildren(childUpdates);


       /* final TextView textview_address = (TextView)findViewById(R.id.textview);
        textview_address.setText(address);*/
    }

    private String getCurrentAddress(double latitude, double longitude) {

        // Geocoder : GPS 정보를 법정주소로 변환
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        List<Address> addresses;

        try {
            addresses = geocoder.getFromLocation(latitude,longitude,7);// 위도, 경도 정보를 바탕으로 주소 가져오는 메서드
        }catch(IOException ioException){//geocoder.getFromLocation을 사용하려면 try catch 예외처리를 해줘야함
            // 네트워크 문제
            Toast.makeText(this, "GEOCODER 사용불가", Toast.LENGTH_LONG).show();
            return "GEOCODER 사용불가";
        }catch(IllegalArgumentException illegalArgumentException){
            Toast.makeText(this, "잘못된 GPS 좌표", Toast.LENGTH_LONG).show();
            return  "잘못된 GPS 좌표";
        }

        if (addresses == null || addresses.size() == 0){
            Toast.makeText(this, "주소 미발견", Toast.LENGTH_LONG).show();
            return "주소 미발견";
        }

        // try 문에서 getFromLocation으로 얻은 주소는 addresses 리스트의 0번에 저장되어 있다.
        Address address = addresses.get(0); // Address 클래스는 import한 android.Location.Address로부터 사용.
        return address.getAddressLine(0).toString()+"\n";
    }

    //사용자 요청 처리
    // ActivityCompat.requestPermissions에 대한 콜백함수
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){//requestCode가 ActivityCompat.requestPermissions의 요청코드를 받음
            case PERMISSIONS_REQUEST_CODE: // 요청코드가 PERMISSIONS_REQUEST_CODE일 때.
                boolean check_result = true; // 초기화

                //모든 퍼미션을 허용했는지 체크
                for(int result : grantResults){ // 향상된 for문. grantResults 배열 길이만큼 반복. 반복 한번 당 배열의 요소를 result에 저장
                    if(result != PackageManager.PERMISSION_GRANTED){
                        check_result = false;
                        break;
                    }
                }

                if(check_result){
                    //위치값 가져올 수 있음
                    ;
                }
                else{// grantResult 즉, 퍼미션 중 하나라도 거부됐다면 이유를 설명하고 종료
                    if(ActivityCompat.shouldShowRequestPermissionRationale(this,REQUIRED_PERMISSIONS[0])||ActivityCompat.shouldShowRequestPermissionRationale(this,REQUIRED_PERMISSIONS[1])){
                        Toast.makeText(this, "퍼미션이 거부됐습니다. 앱을 다시 실행하여 권한을 허용해주세요.", Toast.LENGTH_LONG).show();
                        finish();
                    }else{
                        Toast.makeText(this, "퍼미션이 거부됐습니다. 설정(앱정보)에서 권한을 허용해주세요.", Toast.LENGTH_LONG).show();
                        finish();
                    }
                }
/*                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(this, "권한 체크 거부 됨", Toast.LENGTH_SHORT).show();
                }*/
        }
        //권한이 있다는체 확인되면 OnSuccessListener로 연결하면 좋겠지만 지금은 그렇지 못한 수준.
        //권한 요청 수락 후 앱 나갔다 다시 들어오면 버튼을 클릭했을 때 OnSuccessListener가 동작한다.
        //그러면 현재위치를 찾아서 마커찍고(addmarker), 카메라 움직(CameraUpdateFactory.newLatLng)이고, 카메라줌인(zoomTo)한다.
    }
}
