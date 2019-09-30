package appcontest.sorrysori;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;


/**
 * A simple {@link Fragment} subclass.
 */
public class FourthFragment extends Fragment {

    Button b0;
    Button b1;
    Button b2;
    Button b3;

    public FourthFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_fourth, container, false);

        b0 = v.findViewById(R.id.b0);
        b0.setOnClickListener(new View.OnClickListener() {

            @SuppressLint("MissingPermission")
            public void onClick(View v) {

                Context c = v.getContext();
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:16612642"));

                try {
                    c.startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        b1 = v.findViewById(R.id.b1);
        b1.setOnClickListener(new View.OnClickListener() {

            @SuppressLint("MissingPermission")
            public void onClick(View v) {

                Context c = v.getContext();
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("http://www.noiseinfo.or.kr"));

                try {
                    c.startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        b2 = v.findViewById(R.id.b2);
        b2.setOnClickListener(new View.OnClickListener() {

            @SuppressLint("MissingPermission")
            public void onClick(View v) {

                Context c = v.getContext();
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("http://ecc.me.go.kr"));

                try {
                    c.startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        b3 = v.findViewById(R.id.b3);
        b3.setOnClickListener(new View.OnClickListener() {

            @SuppressLint("MissingPermission")
            public void onClick(View v) {

                Context c = v.getContext();
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("http://www.k-apt.go.kr"));

                try {
                    c.startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        return v;
    }

}
