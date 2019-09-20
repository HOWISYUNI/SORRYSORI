package appcontest.sorrysori;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.os.Bundle;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.bumptech.glide.Glide;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

import static java.lang.System.load;

public class ChatActivity extends AppCompatActivity {

    private FirebaseAuth mFirebaseAuth;
    private FirebaseRecyclerAdapter<ChatMessage, MessageViewHolder> mFirebaseAdapter;
    private FirebaseUser mFirebaseUser;
    private GoogleApiClient mGoogleApiClient;
    private String mUsername;
    private String mPhotoUrl;
    private EditText mMessageEditText;
    private RecyclerView mMessageRecyclerView;
    private Button send_button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        Context context = getApplicationContext();
        DatabaseReference mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
        mMessageEditText = findViewById(R.id.send_text);
        mMessageRecyclerView = findViewById(R.id.recyclerView);
        send_button = findViewById(R.id.send_button);

        send_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ChatMessage chatMessage = new ChatMessage(mMessageEditText.getText().toString(), mUsername, mPhotoUrl);
                mFirebaseDatabaseReference.child("message").push().setValue(chatMessage);
                mMessageEditText.setText("");
            }
        });



        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();

        /*mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, (GoogleApiClient.OnConnectionFailedListener) this)
            .addApi(Auth.GOOGLE_SIGN_IN_API)
                .build();
                */

        if(mFirebaseUser != null){
            mUsername = mFirebaseUser.getDisplayName();
            if(mFirebaseUser.getPhotoUrl() != null)
                mPhotoUrl = mFirebaseUser.getPhotoUrl().toString();
        }

        DatabaseReference query = mFirebaseDatabaseReference.child("message");
        final FirebaseRecyclerOptions<ChatMessage>  options = (new FirebaseRecyclerOptions.Builder()).setQuery((Query)query, ChatMessage.class).build();

        mFirebaseAdapter = new FirebaseRecyclerAdapter<ChatMessage, MessageViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull MessageViewHolder holder, int positon, @NonNull ChatMessage model) {
                holder.messageTextView.setBackgroundResource(R.drawable.left);
                holder.messageTextView.setText(model.text);
                holder.nameTextView.setText(model.name);

                if (model.photoUrl == null) {
                    holder.photoImageView.setImageDrawable(
                            ContextCompat.getDrawable(context, R.drawable.ic_account_circle_black_24dp));
                } else {
                    Glide.with(context).load(model.photoUrl).into(holder.photoImageView);
                }
            }
            @NonNull
            @Override
            public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(context).inflate(R.layout.item_message, parent, false);
                return new MessageViewHolder(view);
            }
        };

        mMessageRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mMessageRecyclerView.setAdapter(mFirebaseAdapter);
    }

    @Override
    public void onStart(){
        super.onStart();
        mFirebaseAdapter.startListening();
    }

    @Override
    public void onStop(){
        super.onStop();
        mFirebaseAdapter.startListening();
    }

}
