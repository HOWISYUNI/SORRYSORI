package appcontest.sorrysori;


import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.List;
import java.util.Locale;


/**
 * A simple {@link Fragment} subclass.
 */
public class SecondFragment extends Fragment {
    public static String result= "1";
    private GpsTracker gpsTracker;
    private Button btn;
    private FirebaseAuth mAuth;
    double latitude, longitude;
    DatabaseReference mFirebaseDatabaseReference;

    public SecondFragment() {
        // Required empty public constructor
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        mAuth = FirebaseAuth.getInstance();
        View view = inflater.inflate(R.layout.fragment_second, container, false);
        btn = view.findViewById(R.id.chat_start);
        Context context = container.getContext();
        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
        gpsTracker = new GpsTracker(context);
        latitude = gpsTracker.getLatitude();
        longitude = gpsTracker.getLongitude();
        String address = getCurrentAddress(latitude,longitude);
        Log.d("address", address );

        mFirebaseDatabaseReference.child("User").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Log.d("address2", String.valueOf(snapshot.child("address").getValue()));
                    Log.d("email", String.valueOf(snapshot.child("email").getValue()));
                    if((snapshot.child("address").equals(address))){
                        Log.d("database user", String.valueOf(snapshot.child("email").getValue()));
                        result = "same";
                        Log.d("result", result);
                        return;
                    }
                    Log.d("if", String.valueOf(snapshot.child("address").equals(address)));
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (result.equals("same")) {
                    Intent intent = new Intent(getActivity(), ChatActivity.class);
                    startActivity(intent);
                }else
                    Toast.makeText(context, "대화상대를 찾지 못했습니다.", Toast.LENGTH_LONG).show();
            }
        });
        return view;
    }

    private String getCurrentAddress(double latitude, double longitude) {

        // Geocoder : GPS 정보를 법정주소로 변환
        Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
        List<Address> addresses;

        try {
            addresses = geocoder.getFromLocation(latitude,longitude,7);// 위도, 경도 정보를 바탕으로 주소 가져오는 메서드
        }catch(IOException ioException){//geocoder.getFromLocation을 사용하려면 try catch 예외처리를 해줘야함
            // 네트워크 문제
            Toast.makeText(getContext(), "GEOCODER 사용불가", Toast.LENGTH_LONG).show();
            return "GEOCODER 사용불가";
        }catch(IllegalArgumentException illegalArgumentException){
            Toast.makeText(getContext(), "잘못된 GPS 좌표", Toast.LENGTH_LONG).show();
            return  "잘못된 GPS 좌표";
        }

        if (addresses == null || addresses.size() == 0){
            Toast.makeText(getContext(), "주소 미발견", Toast.LENGTH_LONG).show();
            return "주소 미발견";
        }

        // try 문에서 getFromLocation으로 얻은 주소는 addresses 리스트의 0번에 저장되어 있다.
        Address address = addresses.get(0); // Address 클래스는 import한 android.Location.Address로부터 사용.
        return address.getAddressLine(0).toString()+"\n";
    }

}


