package appcontest.sorrysori;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SignUpActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        mAuth = FirebaseAuth.getInstance();

    }
    public void sign(View view){
        Log.d("버튼 클릭", "버튼 클릭");
        signUp();
    }

    private void signUp(){
        Log.d("signUp", "signUp 완료");

        String email = ((EditText)findViewById(R.id.sign_id)).getText().toString();
        String password = ((EditText)findViewById(R.id.sign_password)).getText().toString();

        if((email.length() >= 15 && email.length() <= 25) && (password.length() >= 6 && password.length() <= 14)) {
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                Log.d("회원가입", "회원가입 완료");
                                FirebaseUser user = mAuth.getCurrentUser();
                                Toast.makeText(SignUpActivity.this, "회원가입에 성공하였습니다.", Toast.LENGTH_LONG).show();
                                finish();
                            } else {
                                Toast.makeText(SignUpActivity.this, "이미 중복된 이메일 입니다.", Toast.LENGTH_LONG).show();
                            }
                        }

                    });
        }
        else if(email.isEmpty() || password.isEmpty())
            Toast.makeText(this, "이메일과 비밀번호를 입력해주세요.", Toast.LENGTH_LONG).show();
        else if(email.length() > 25 || password.length() > 14)
            Toast.makeText(this, "이메일 또는 패스워드가 깁니다.", Toast.LENGTH_LONG).show();
        else
            Toast.makeText(this, "이메일 또는 패스워드가 짧습니다.", Toast.LENGTH_LONG).show();
    }
}
