package im.tny.segvault.disturbances;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import im.tny.segvault.disturbances.exception.APIException;
import im.tny.segvault.disturbances.model.RStation;
import im.tny.segvault.disturbances.model.Trip;
import im.tny.segvault.subway.Line;
import im.tny.segvault.subway.Network;
import io.realm.Realm;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnListFragmentInteractionListener}
 * interface.
 */
public class TripHistoryFragment extends TopFragment {
    private static final String ARG_COLUMN_COUNT = "column-count";
    private int mColumnCount = 1;
    private OnListFragmentInteractionListener mListener;

    private RecyclerView recyclerView = null;
    private TextView emptyView;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public TripHistoryFragment() {
    }

    @SuppressWarnings("unused")
    public static TripHistoryFragment newInstance(int columnCount) {
        TripHistoryFragment fragment = new TripHistoryFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COLUMN_COUNT, columnCount);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        setUpActivity(getString(R.string.frag_trip_history_title), 0, false, false);
        //setHasOptionsMenu(true);

        View view = inflater.inflate(R.layout.fragment_trip_history_list, container, false);

        // Set the adapter
        Context context = view.getContext();
        emptyView = (TextView) view.findViewById(R.id.no_trips_view);
        recyclerView = (RecyclerView) view.findViewById(R.id.list);
        if (mColumnCount <= 1) {
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
        } else {
            recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
        }
        recyclerView.addItemDecoration(new SimpleDividerItemDecoration(context));

        // fix scroll fling. less than ideal, but apparently there's still no other solution
        recyclerView.setNestedScrollingEnabled(false);

        IntentFilter filter = new IntentFilter();
        filter.addAction(MainActivity.ACTION_LOCATION_SERVICE_BOUND);
        filter.addAction(MainService.ACTION_UPDATE_TOPOLOGY_FINISHED);
        LocalBroadcastManager bm = LocalBroadcastManager.getInstance(context);
        bm.registerReceiver(mBroadcastReceiver, filter);

        if (mListener != null && mListener.getMainService() != null) {
            new TripHistoryFragment.UpdateDataTask().execute();
        }
        return view;
    }

    /*@Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.disturbance_list, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menu_refresh) {
            new TripHistoryFragment.UpdateDataTask().execute();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }*/

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnListFragmentInteractionListener) {
            mListener = (OnListFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnListFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    private class UpdateDataTask extends AsyncTask<Void, Integer, Boolean> {
        private List<TripRecyclerViewAdapter.TripItem> items = new ArrayList<>();

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        protected Boolean doInBackground(Void... v) {
            if (!Connectivity.isConnected(getContext())) {
                return false;
            }
            if (mListener == null || mListener.getMainService() == null) {
                return false;
            }
            Collection<Network> networks = mListener.getMainService().getNetworks();
            Realm realm = Realm.getDefaultInstance();
            for (Trip t : realm.where(Trip.class).findAll()) {
                items.add(new TripRecyclerViewAdapter.TripItem(t, networks));
            }
            if(items.size() == 0) {
                return false;
            }
            Collections.sort(items, Collections.<TripRecyclerViewAdapter.TripItem>reverseOrder(new Comparator<TripRecyclerViewAdapter.TripItem>() {
                @Override
                public int compare(TripRecyclerViewAdapter.TripItem tripItem, TripRecyclerViewAdapter.TripItem t1) {
                    return Long.valueOf(tripItem.originTime.getTime()).compareTo(Long.valueOf(t1.originTime.getTime()));
                }
            }));
            return true;
        }

        protected void onProgressUpdate(Integer... progress) {

        }

        protected void onPostExecute(Boolean result) {
            if (!isAdded()) {
                // prevent onPostExecute from doing anything if no longer attached to an activity
                return;
            }
            if (result && recyclerView != null && mListener != null) {
                recyclerView.setAdapter(new TripRecyclerViewAdapter(items, mListener));
                recyclerView.invalidate();
                emptyView.setVisibility(View.GONE);
            } else {
                emptyView.setVisibility(View.VISIBLE);
            }
        }
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
    public interface OnListFragmentInteractionListener extends OnInteractionListener {
        void onListFragmentInteraction(TripRecyclerViewAdapter.TripItem item);

        MainService getMainService();
    }

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case MainActivity.ACTION_LOCATION_SERVICE_BOUND:
                case MainService.ACTION_UPDATE_TOPOLOGY_FINISHED:
                    if(getActivity() != null) {
                        new TripHistoryFragment.UpdateDataTask().execute();
                    }
                    break;
            }
        }
    };
}
