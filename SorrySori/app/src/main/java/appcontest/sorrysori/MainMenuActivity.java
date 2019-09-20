package appcontest.sorrysori;

import android.os.Bundle;

import androidx.annotation.NonNull;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class MainMenuActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
//    FirebaseUser user = mAuth.getCurrentUser();
    private FragmentManager fragmentManager = getSupportFragmentManager();
    private FirstFragment firstFragment = new FirstFragment();
    private SecondFragment secondFragment = new SecondFragment();
    private ThirdFragment thirdFragment = new ThirdFragment();
    private FourthFragment fourthFragment = new FourthFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);
        mAuth = FirebaseAuth.getInstance();

        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.frame_layout, firstFragment).commitAllowingStateLoss();
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                FragmentTransaction transaction = fragmentManager.beginTransaction();

                switch (item.getItemId()) {
                    case R.id.navigation_home:
                        transaction.replace(R.id.frame_layout, firstFragment).commitAllowingStateLoss();
                        break;

                    case R.id.navigation_dashboard:
                        transaction.replace(R.id.frame_layout, secondFragment).commitAllowingStateLoss();
                        break;

                    case R.id.navigation_notifications:
                        transaction.replace(R.id.frame_layout, thirdFragment).commitAllowingStateLoss();
                        break;

                    case R.id.navigation_notifications2:
                        transaction.replace(R.id.frame_layout, fourthFragment).commitAllowingStateLoss();
                        break;
                }
                return true;
            }
        });
    }

    public void logOut(View view){
     //   Log.d("로그아웃 전", "계정 이름 : " + user);
        FirebaseAuth.getInstance().signOut();
        Toast.makeText(MainMenuActivity.this, "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show();
    }

}

