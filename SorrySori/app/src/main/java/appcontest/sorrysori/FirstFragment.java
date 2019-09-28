package appcontest.sorrysori;
import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

public class FirstFragment extends Fragment {
    public FirstFragment() {
        // Required empty public constructor
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //Intent intent1 = new Intent(getActivity(), Decibelfragment.class);
        //startActivity(intent1);
        View view = inflater.inflate(R.layout.activity_imsi_decibel, container, false);
        return view;
    }
}