package com.stofstik.letsmeet;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

/**
 * Created by stofstik on 16/10/15.
 */
public class DialogChangeUsername extends DialogFragment {
    static AlertDialog.Builder builder;
    static View dialogView;
    EditText etUsername;

    public interface NoticeDialogListener {
        public void onDialogPositiveClick(DialogFragment dialog, String username);

        public void onDialogNegativeClick(DialogFragment dialog);

        public void onCancelled(DialogFragment dialog);
    }

    NoticeDialogListener mListener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (NoticeDialogListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() +
                    "must implement NoticeDialogListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        builder = new AlertDialog.Builder(getActivity());
        dialogView = LayoutInflater.from(getActivity())
                .inflate(R.layout.dialog_fragment_choose_username, null);
        etUsername = (EditText) dialogView.findViewById(R.id.et_dialog_username);

        // set text to current username
        SharedPreferences sharedPreferences = getActivity().getPreferences(getActivity().MODE_PRIVATE);
        etUsername.setText(sharedPreferences.getString(MainActivity.SP_KEY_USERNAME, ""));
        etUsername.selectAll();

        builder.setView(dialogView);
        builder.setTitle(R.string.dialog_change_username_title);
        builder.setPositiveButton(R.string.dialog_change_username_positive,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        mListener.onDialogPositiveClick(DialogChangeUsername.this, etUsername.getText().toString());
                    }
                });
        builder.setNegativeButton(R.string.dialog_change_username_negative,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        mListener.onDialogNegativeClick(DialogChangeUsername.this);
                    }
                });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                mListener.onCancelled(DialogChangeUsername.this);
            }
        });
        return builder.create();
    }
}