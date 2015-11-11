package com.stofstik.letsmeet;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.SimpleAdapter;
import android.widget.SimpleCursorAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by stofstik on 8-10-15.
 * TODO Get all contacts
 * TODO - get email address
 * TODO - check if lets meet user
 * TODO -
 */
public class ContactListFragment extends ListFragment implements
        LoaderManager.LoaderCallbacks<Cursor>,
        AdapterView.OnItemSelectedListener,
        AdapterView.OnItemClickListener {

    private static final String KEY_NAME = "KEY_NAME";
    private static final String KEY_EMAIL = "KEY_EMAIL";
    private static final String KEY_CHECKED = "KEY_CHECKED";

    private static final String[] FROM_CONTACT_COLUMNS = { // Read this data
            Data.DISPLAY_NAME_PRIMARY,
            Data.CONTACT_ID,
    };
    private static final int[] TO_IDS = { // Adapt that data to these views
            R.id.tv_name,
            R.id.tv_email
            //R.id.cb_send_invite
    };
    private static final String[] PROJECTION = new String[]{
            Data._ID,
            Data.DISPLAY_NAME_PRIMARY,
            Data.CONTACT_ID,
            Data.LOOKUP_KEY
    };
    // Defines the selection clause
    private static final String SELECTION = Contacts.DISPLAY_NAME_PRIMARY + " LIKE ?";
    // Defines a variable for the search string
    private String mSearchString = "";
    // Defines the array to hold values that replace the ?
    private String[] mSelectionArgs = { mSearchString };
    /*
     * Defines a variable to contain the selection value. Once you
     * have the Cursor from the Contacts table, and you've selected
     * the desired row, move the row's LOOKUP_KEY value into this
     * variable.
     */
    private String mLookupKey;
    private static final String SORT_ORDER = Data.MIMETYPE;
    private static final int DETAILS_QUERY_ID = 0;


    // The column index for the _ID column
    private static final int CONTACT_ID_INDEX = 0;
    // The column index for the LOOKUP_KEY column
    private static final int LOOKUP_KEY_INDEX = 1;
    // Define variables for the contact the user selects
    // The contact's _ID value
    long mContactId;
    // The contact's LOOKUP_KEY
    String mContactKey;
    // A content URI for the selected contact
    Uri mContactUri;
    // An adapter that binds the result Cursor to the ListView
    private CursorAdapter mCursorAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contacts_list, container, false);
        /*
            TODO Show loading spinner
            TODO Load contact name and email into arraylist

            TODO Argh! This is easier said than done I'm afraid. We need to query two times
            TODO First for the contact and then a second time for that contacts email address
            TODO It's probably better to lay down some more foundation at this point, than to keep
            hammering on this particular problem

            TODO plan of attack:
            Thoroughly read the docs on this one!!!
            Create a custom CursorAdapter
            Query the ContactsProvider
            Pass the name into a ViewHolder
            Pass the contact to another cursor loader
            query the contact for an email address
            pass the email into the viewholder

            Sidenote... THIS IS RIDICULOUS! WHY WOULD'NT YOU PUT ALL CONTACT DATA IN ONE PLACE!!!???



            TODO When done hide loading spinner
            TODO ...
            TODO ...
            TODO Check if contacts use Let's meet
         */

        /**
         Simple adapter memory bump
         */
        SimpleAdapter adapter;
        List<Map<String, Object>> contacts = new ArrayList<>();
        String[] from = {KEY_NAME, KEY_EMAIL, KEY_CHECKED};
        int[] to = {R.id.tv_name, R.id.tv_email, R.id.cb_send_invite};
        Map<String, Object> contact = new HashMap<>();
        contact.put(KEY_NAME, "bla");
        contact.put(KEY_EMAIL, "email");
        contact.put(KEY_CHECKED, false);
        contacts.add(contact);
        adapter = new SimpleAdapter(getContext(), contacts, R.layout.list_item_choose_contacts, from, to);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        /*
            Use a CursorAdapter to go over all contacts, we can even supply criteria.
            So it's pretty likely we need to use this approach in the future as well.
         */
        mCursorAdapter = new SimpleCursorAdapter(
                getContext(),
                R.layout.list_item_choose_contacts,
                null,
                FROM_CONTACT_COLUMNS, TO_IDS, 0);

        getListView().setAdapter(mCursorAdapter);
        getListView().setOnItemSelectedListener(this);
        // Initialize the loader
        getLoaderManager().initLoader(DETAILS_QUERY_ID, null, this);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        // TODO KEY_CHECKED to true on clicked id

    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @Override
    public void onItemClick(
            AdapterView<?> parent, View item, int position, long rowID) {

    }

    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle bundle) {
        CursorLoader mLoader = null;
        // Choose the proper action
        switch (loaderId) {
            case DETAILS_QUERY_ID:
                // Assigns the selection parameter
                mSelectionArgs[0] = "%" + mSearchString + "%";
                // Starts the query
                mLoader = new CursorLoader(
                                getActivity(),
                                Data.CONTENT_URI,
                                PROJECTION,
                                SELECTION,
                                mSelectionArgs,
                                SORT_ORDER
                        );
        }
        return mLoader;
    }

    @Override
      public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        switch (loader.getId()) {
            case DETAILS_QUERY_ID:
                    /*
                     * Process the resulting Cursor here.
                     */
                mCursorAdapter.swapCursor(cursor);
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
            case DETAILS_QUERY_ID:
                /*
                 * If you have current references to the Cursor,
                 * remove them here.
                 */
                mCursorAdapter.swapCursor(null);
                break;
        }
    }
}
