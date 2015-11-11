package com.stofstik.letsmeet;

import android.app.Activity;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.io.IOException;
import java.util.ArrayList;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link StartMeetFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link StartMeetFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class StartMeetFragment extends Fragment implements
        View.OnClickListener {
    private static final String LOG = "StartMeetFragment";

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    private Button bStartMeet, bJoinMeet;
    private EditText etMeetName;
    private TextView tvUsername, tvUrl, tvCoordinates;

    private String currentLobby;
    private String strUsername;
    private Double dLatitude, dLongitude;
    private Location mCurrentLocation;

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor sharedPreferencesEditor;

    private ResponseParser responseParser = new ResponseParser();

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment StartMeet.
     */
    // TODO: Rename and change types and number of parameters
    public static StartMeetFragment newInstance(String param1, String param2) {
        StartMeetFragment fragment = new StartMeetFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    public StartMeetFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_start_meet, container, false);

        bStartMeet = (Button) view.findViewById(R.id.b_start_meet);
        bStartMeet.setOnClickListener(this);
        bJoinMeet = (Button) view.findViewById(R.id.b_join_meet);
        bJoinMeet.setOnClickListener(this);
        etMeetName = (EditText) view.findViewById(R.id.et_meet_name);
        tvUsername = (TextView) view.findViewById(R.id.tv_username);
        tvUrl = (TextView) view.findViewById(R.id.tv_url);
        tvCoordinates = (TextView) view.findViewById(R.id.tv_coordinates);

        // TODO check best practice for loading shared preferences in fragment
        sharedPreferences = getActivity().getPreferences(getContext().MODE_PRIVATE);
        strUsername = sharedPreferences.getString(MainActivity.SP_KEY_USERNAME, "Your Username");
        tvUsername.setText(strUsername);

        return view;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.b_start_meet:
                startMeet();
                break;
            case R.id.b_join_meet:
                joinMeet();
                break;
        }
    }

    private void startMeet() {
        Log.d(LOG, "clicked lets meet!");
        dLatitude = (mCurrentLocation != null) ? mCurrentLocation.getLatitude() : 0;
        dLongitude = (mCurrentLocation != null) ? mCurrentLocation.getLongitude() : 0;

        // Instantiate a RequestQueue
        RequestQueue queue = Volley.newRequestQueue(getActivity());
        // Build a valid url TODO ensure correct conversion of space char
        Uri.Builder builder = Uri.parse("http://192.168.0.10:3000").buildUpon()
                .appendPath("startlobby")
                .appendPath(strUsername)
                .appendPath("" + dLatitude)
                .appendPath("" + dLongitude);

        final String url = builder.toString();
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(LOG, "startMeet response: " + response);
                try {
                    currentLobby = responseParser.parseLobbyName(response);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                tvUrl.setText(url);
                etMeetName.setText(currentLobby);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(LOG, "startMeet error: " + error.toString());
                Toast.makeText(getActivity(), error.toString(), Toast.LENGTH_SHORT).show();
            }
        });
        queue.add(stringRequest);
    }

    private void joinMeet() {
        Log.d(LOG, "clicked join meet!");
        dLatitude = (mCurrentLocation != null) ? mCurrentLocation.getLatitude() : 0;
        dLongitude = (mCurrentLocation != null) ? mCurrentLocation.getLongitude() : 0;

        // Instantiate a RequestQueue
        RequestQueue queue = Volley.newRequestQueue(getActivity());
        // Build a valid url TODO ensure correct conversion of space char
        Uri.Builder builder = Uri.parse("http://192.168.0.10:3000").buildUpon()
                .appendPath("joinlobby")
                .appendPath(currentLobby)
                .appendPath(strUsername)
                .appendPath("" + dLatitude)
                .appendPath("" + dLongitude);

        String url = builder.toString();
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(LOG, "joinMeet response: " + response);
                try {
                    ArrayList<User> users = responseParser.parseUsers(response);
                    for (User user : users) {
                        Log.d(LOG, user.getUsername());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(LOG, "joinMeet error: " + error.toString());
                Toast.makeText(getActivity(), error.toString(), Toast.LENGTH_SHORT).show();
            }
        });
        queue.add(stringRequest);
    }

    public void setLocation(Location location) {
        if (location == null) {
            Log.d(LOG, "Location == null");
            return;
        }
        mCurrentLocation = location;
        StringBuilder sb = new StringBuilder();
        sb.append("lat: ");
        sb.append(mCurrentLocation.getLatitude());
        sb.append(", ");
        sb.append("long: ");
        sb.append(mCurrentLocation.getLongitude());
        tvCoordinates.setText(sb.toString());
    }

    public void setUsername(String username) {
        this.strUsername = username;
        tvUsername.setText("Username: " + username);
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(Uri uri);
    }

}
