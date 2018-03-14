package im.tny.segvault.disturbances.ui.activity;

import android.Manifest;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.util.ArrayList;
import java.util.Collection;

import im.tny.segvault.disturbances.ui.adapter.AnnouncementRecyclerViewAdapter;
import im.tny.segvault.disturbances.ui.fragment.TopFragment;
import im.tny.segvault.disturbances.ui.fragment.top.DisturbanceFragment;
import im.tny.segvault.disturbances.ui.adapter.DisturbanceRecyclerViewAdapter;
import im.tny.segvault.disturbances.FeedbackUtil;
import im.tny.segvault.disturbances.ui.fragment.top.GeneralPreferenceFragment;
import im.tny.segvault.disturbances.ui.fragment.HomeLinesFragment;
import im.tny.segvault.disturbances.ui.fragment.HomeStatsFragment;
import im.tny.segvault.disturbances.ui.fragment.HtmlDialogFragment;
import im.tny.segvault.disturbances.ui.adapter.LineRecyclerViewAdapter;
import im.tny.segvault.disturbances.LineStatusCache;
import im.tny.segvault.disturbances.MainService;
import im.tny.segvault.disturbances.ui.fragment.top.MapFragment;
import im.tny.segvault.disturbances.ui.fragment.top.NotifPreferenceFragment;
import im.tny.segvault.disturbances.PreferenceNames;
import im.tny.segvault.disturbances.R;
import im.tny.segvault.disturbances.ui.fragment.top.RouteFragment;
import im.tny.segvault.disturbances.ui.fragment.TripFragment;
import im.tny.segvault.disturbances.ui.fragment.top.TripHistoryFragment;
import im.tny.segvault.disturbances.ui.adapter.TripRecyclerViewAdapter;
import im.tny.segvault.disturbances.ui.fragment.UnconfirmedTripsFragment;
import im.tny.segvault.disturbances.Util;
import im.tny.segvault.disturbances.model.Trip;
import im.tny.segvault.disturbances.ui.intro.IntroActivity;
import im.tny.segvault.disturbances.ui.fragment.top.AboutFragment;
import im.tny.segvault.disturbances.ui.fragment.top.AnnouncementFragment;
import im.tny.segvault.disturbances.ui.fragment.top.HelpFragment;
import im.tny.segvault.disturbances.ui.fragment.top.HomeFragment;
import im.tny.segvault.subway.Line;
import im.tny.segvault.subway.Network;
import im.tny.segvault.subway.Station;
import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt;

public class MainActivity extends TopActivity
        implements SearchView.OnSuggestionListener,
        NavigationView.OnNavigationItemSelectedListener,
        HomeFragment.OnFragmentInteractionListener,
        RouteFragment.OnFragmentInteractionListener,
        MapFragment.OnFragmentInteractionListener,
        HelpFragment.OnFragmentInteractionListener,
        AboutFragment.OnFragmentInteractionListener,
        HomeLinesFragment.OnListFragmentInteractionListener,
        HomeStatsFragment.OnFragmentInteractionListener,
        AnnouncementFragment.OnListFragmentInteractionListener,
        DisturbanceFragment.OnListFragmentInteractionListener,
        NotifPreferenceFragment.OnFragmentInteractionListener,
        GeneralPreferenceFragment.OnFragmentInteractionListener,
        TripHistoryFragment.OnListFragmentInteractionListener,
        TripFragment.OnFragmentInteractionListener,
        UnconfirmedTripsFragment.OnListFragmentInteractionListener, SearchView.OnQueryTextListener {

    private MainService locService;
    private boolean locBound = false;

    private NavigationView navigationView;
    private MenuItem searchItem;
    private SearchView searchView;

    private LocalBroadcastManager bm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Object conn = getLastCustomNonConfigurationInstance();
        if (conn != null && ((LocServiceConnection) conn).getBinder() != null) {
            // have the service connection survive through activity configuration changes
            // (e.g. screen orientation changes)
            mConnection = (LocServiceConnection) conn;
            locService = mConnection.getBinder().getService();
            locBound = true;
        } else if (!locBound) {
            startService(new Intent(this, MainService.class));
            getApplicationContext().bindService(new Intent(getApplicationContext(), MainService.class), mConnection, Context.BIND_AUTO_CREATE);
        }

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        if (savedInstanceState == null) {
            // show initial fragment
            Fragment newFragment = null;
            if (getIntent() != null) {
                String id = getIntent().getStringExtra(EXTRA_INITIAL_FRAGMENT);
                if (id != null) {
                    newFragment = getNewFragment(pageStringToResourceId(id));
                    planRouteTo = getIntent().getStringExtra(EXTRA_PLAN_ROUTE_TO_STATION);
                }
            }
            if (newFragment == null) {
                newFragment = new HomeFragment();
            }
            replaceFragment(newFragment, false);
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(MainService.ACTION_UPDATE_TOPOLOGY_PROGRESS);
        filter.addAction(MainService.ACTION_UPDATE_TOPOLOGY_FINISHED);
        filter.addAction(MainService.ACTION_TOPOLOGY_UPDATE_AVAILABLE);
        filter.addAction(MainService.ACTION_CACHE_EXTRAS_PROGRESS);
        filter.addAction(MainService.ACTION_CACHE_EXTRAS_FINISHED);
        filter.addAction(FeedbackUtil.ACTION_FEEDBACK_PROVIDED);
        bm = LocalBroadcastManager.getInstance(this);
        bm.registerReceiver(mBroadcastReceiver, filter);

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                SharedPreferences sharedPref = getSharedPreferences("settings", MODE_PRIVATE);
                boolean isFirstStart = sharedPref.getBoolean("fuse_first_run", true);

                if (isFirstStart) {
                    // intro will request permission for us
                    final Intent i = new Intent(MainActivity.this, IntroActivity.class);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            startActivity(i);
                            showTargetPrompt();
                        }
                    });
                } else {
                    boolean locationEnabled = sharedPref.getBoolean(PreferenceNames.LocationEnable, true);
                    if (locationEnabled &&
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION);
                                }
                            }
                        });

                    }
                }
            }
        });

        // Start the thread
        t.start();
    }

    public static final int PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION = 10001;

    private void showTargetPrompt() {
        new MaterialTapTargetPrompt.Builder(MainActivity.this)
                .setTarget(Util.getToolbarNavigationIcon((Toolbar) findViewById(R.id.toolbar)))
                .setPrimaryText(R.string.act_main_nav_taptarget_title)
                .setSecondaryText(R.string.act_main_nav_taptarget_subtitle)
                .setPromptStateChangeListener(new MaterialTapTargetPrompt.PromptStateChangeListener() {
                    @Override
                    public void onPromptStateChanged(MaterialTapTargetPrompt prompt, int state) {
                        if (state == MaterialTapTargetPrompt.STATE_FOCAL_PRESSED) {
                            // User has pressed the prompt target
                        }
                    }
                })
                .setFocalColour(ContextCompat.getColor(this, R.color.colorAccent))
                .setBackgroundColour(ContextCompat.getColor(this, R.color.colorPrimaryLight))
                .show();
    }

    private boolean isVisible = false;

    @Override
    protected void onStart() {
        super.onStart();
        isVisible = true;
    }

    @Override
    protected void onStop() {
        isVisible = false;
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        bm.unregisterReceiver(mBroadcastReceiver);

        // Unbind from the service
        if (locBound && isFinishing()) {
            getApplicationContext().unbindService(mConnection);
            locBound = false;
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        SharedPreferences sharedPref = getSharedPreferences("settings", MODE_PRIVATE);
        boolean devMode = sharedPref.getBoolean(PreferenceNames.DeveloperMode, false);
        if (!devMode) {
            menu.findItem(R.id.menu_debug).setVisible(false);
        }

        searchItem = menu.findItem(R.id.search);
        searchView = (SearchView) searchItem.getActionView();
        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setOnSuggestionListener(this);
        searchView.setOnQueryTextListener(this);
        // Detect SearchView open / close
        MenuItemCompat.setOnActionExpandListener(searchItem, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem menuItem) {
                setItemsVisibility(menu, searchItem, false);
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem menuItem) {
                invalidateOptionsMenu();
                return true;
            }
        });

        return true;
    }

    private void setItemsVisibility(Menu menu, MenuItem exception, boolean visible) {
        for (int i = 0; i < menu.size(); ++i) {
            MenuItem item = menu.getItem(i);
            if (item != exception) item.setVisible(visible);
        }
    }

    private Fragment getNewFragment(int id) {
        try {
            switch (id) {
                case R.id.nav_home:
                    return HomeFragment.newInstance();
                case R.id.nav_plan_route:
                    return RouteFragment.newInstance(MainService.PRIMARY_NETWORK_ID);
                case R.id.nav_trip_history:
                    return TripHistoryFragment.newInstance(1);
                case R.id.nav_map:
                    return MapFragment.newInstance(MainService.PRIMARY_NETWORK_ID);
                case R.id.nav_announcements:
                    return AnnouncementFragment.newInstance(MainService.PRIMARY_NETWORK_ID, 1);
                case R.id.nav_disturbances:
                    return DisturbanceFragment.newInstance(1);
                case R.id.nav_notif:
                    return NotifPreferenceFragment.newInstance();
                case R.id.nav_settings:
                    return GeneralPreferenceFragment.newInstance();
                case R.id.nav_help:
                    return HelpFragment.newInstance();
                case R.id.nav_about:
                    return AboutFragment.newInstance();
                default:
                    return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static int pageStringToResourceId(String id) {
        switch (id) {
            case "nav_home":
                return R.id.nav_home;
            case "nav_plan_route":
                return R.id.nav_plan_route;
            case "nav_trip_history":
                return R.id.nav_trip_history;
            case "nav_map":
                return R.id.nav_map;
            case "nav_announcements":
                return R.id.nav_announcements;
            case "nav_disturbances":
                return R.id.nav_disturbances;
            case "nav_notif":
                return R.id.nav_notif;
            case "nav_settings":
                return R.id.nav_settings;
            case "nav_help":
                return R.id.nav_help;
            case "nav_about":
                return R.id.nav_about;
            default:
                return R.id.nav_home;
        }
    }

    private void replaceFragment(Fragment newFragment, boolean addToBackStack) {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.main_fragment_container);
        if (currentFragment == null || !currentFragment.isAdded()) {
            currentFragment = getSupportFragmentManager().findFragmentById(R.id.alt_fragment_container);
        }
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if (currentFragment != null) {
            transaction.remove(currentFragment);
        }
        int destContainer = R.id.main_fragment_container;
        if (newFragment instanceof TopFragment) {
            TopFragment newFrag = (TopFragment) newFragment;
            if (!newFrag.isScrollable()) {
                destContainer = R.id.alt_fragment_container;
            }
        }
        transaction.replace(destContainer, newFragment);
        if (addToBackStack) {
            transaction.addToBackStack(null);
        }
        transaction.commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Action bar menu item selected

        switch (item.getItemId()) {
            case R.id.menu_debug:
                new AsyncTask<Void, Void, String>() {

                    @Override
                    protected String doInBackground(Void... voids) {
                        if (locBound) {
                            return locService.dumpDebugInfo();
                        }
                        return "";
                    }

                    @Override
                    protected void onPostExecute(String s) {
                        if (!isFinishing() && isVisible) {
                            DialogFragment newFragment = HtmlDialogFragment.newInstance(s, false);
                            newFragment.show(getSupportFragmentManager(), "debugtext");
                        }
                    }
                }.execute();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        Fragment newFragment = getNewFragment(item.getItemId());

        if (newFragment != null) {
            replaceFragment(newFragment, true);
        } else {
            Snackbar.make(findViewById(R.id.fab), R.string.status_not_yet_implemented, Snackbar.LENGTH_LONG).show();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onHelpLinkClicked(String destination) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.main_fragment_container, HelpFragment.newInstance(destination));
        transaction.addToBackStack(null);
        transaction.commit();
    }

    @Override
    public void onStationLinkClicked(String destination) {
        onStationLinkClicked(destination, null);
    }

    public void onStationLinkClicked(String destination, String lobby) {
        onStationLinkClicked(destination, lobby, null);
    }

    public void onStationLinkClicked(String destination, String lobby, String exit) {
        if (locService != null) {
            for (Network network : locService.getNetworks()) {
                Station station;
                if ((station = network.getStation(destination)) != null) {
                    Intent intent = new Intent(this, StationActivity.class);
                    intent.putExtra(StationActivity.EXTRA_STATION_ID, station.getId());
                    intent.putExtra(StationActivity.EXTRA_NETWORK_ID, network.getId());
                    if (lobby != null && !lobby.isEmpty()) {
                        intent.putExtra(StationActivity.EXTRA_LOBBY_ID, lobby);
                    }
                    if (exit != null && !exit.isEmpty()) {
                        String[] coordStr = exit.split(",");
                        intent.putExtra(StationActivity.EXTRA_EXIT_COORD_LAT, Float.parseFloat(coordStr[0]));
                        intent.putExtra(StationActivity.EXTRA_EXIT_COORD_LONG, Float.parseFloat(coordStr[1]));
                    }
                    startActivity(intent);
                    return;
                }
            }
        }
    }

    public void onLineLinkClicked(String destination) {
        if (locService != null) {
            for (Network network : locService.getNetworks()) {
                Line line;
                if ((line = network.getLine(destination)) != null) {
                    Intent intent = new Intent(this, LineActivity.class);
                    intent.putExtra(LineActivity.EXTRA_LINE_ID, line.getId());
                    intent.putExtra(LineActivity.EXTRA_NETWORK_ID, network.getId());
                    startActivity(intent);
                    return;
                }
            }
        }
    }

    public void onPOILinkClicked(String destination) {
        Intent intent = new Intent(this, POIActivity.class);
        intent.putExtra(POIActivity.EXTRA_POI_ID, destination);
        startActivity(intent);
    }

    @Override
    public void onMailtoLinkClicked(String address) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:")); // only email apps should handle this
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{address});
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    @Override
    public void onLinkClicked(String destination) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(destination));
        if (browserIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(browserIntent);
        }
    }

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private LocServiceConnection mConnection = new LocServiceConnection();

    @Override
    public boolean onQueryTextSubmit(String query) {
        // make submit be a no-op, our results are the "suggestions"
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }

    class LocServiceConnection implements ServiceConnection {
        MainService.LocalBinder binder;

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            binder = (MainService.LocalBinder) service;
            locService = binder.getService();
            locBound = true;
            Intent intent = new Intent(ACTION_MAIN_SERVICE_BOUND);
            bm.sendBroadcast(intent);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            locBound = false;
        }

        public MainService.LocalBinder getBinder() {
            return binder;
        }
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        // have the service connection survive through activity configuration changes
        // (e.g. screen orientation changes)
        return mConnection;
    }

    private Snackbar topologyUpdateSnackbar = null;
    private Snackbar cacheExtrasSnackbar = null;

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case MainService.ACTION_UPDATE_TOPOLOGY_PROGRESS:
                    final int progress = intent.getIntExtra(MainService.EXTRA_UPDATE_TOPOLOGY_PROGRESS, 0);
                    final String msg = String.format(getString(R.string.update_topology_progress), progress);
                    if (topologyUpdateSnackbar == null) {
                        topologyUpdateSnackbar = Snackbar.make(findViewById(R.id.fab), msg, Snackbar.LENGTH_INDEFINITE)
                                .setAction(R.string.update_topology_cancel_action, new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        if (locBound) {
                                            locService.cancelTopologyUpdate();
                                        }
                                    }
                                });
                    } else {
                        topologyUpdateSnackbar.setText(msg);
                    }
                    if (!topologyUpdateSnackbar.isShown()) {
                        topologyUpdateSnackbar.show();
                    }
                    break;
                case MainService.ACTION_UPDATE_TOPOLOGY_FINISHED:
                    final boolean success = intent.getBooleanExtra(MainService.EXTRA_UPDATE_TOPOLOGY_FINISHED, false);
                    if (topologyUpdateSnackbar != null) {
                        topologyUpdateSnackbar.setAction("", null);
                        topologyUpdateSnackbar.setDuration(Snackbar.LENGTH_LONG);
                        if (success) {
                            topologyUpdateSnackbar.setText(R.string.update_topology_success);
                        } else {
                            topologyUpdateSnackbar.setText(R.string.update_topology_failure);
                        }
                        topologyUpdateSnackbar.show();
                        topologyUpdateSnackbar = null;
                    }
                    break;
                case MainService.ACTION_TOPOLOGY_UPDATE_AVAILABLE:
                    Snackbar.make(findViewById(R.id.fab), R.string.update_topology_available, 10000)
                            .setAction(R.string.update_topology_update_action, new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    if (locBound) {
                                        locService.updateTopology();
                                    }
                                }
                            }).show();
                    break;
                case MainService.ACTION_CACHE_EXTRAS_PROGRESS:
                    final int progressCurrent = intent.getIntExtra(MainService.EXTRA_CACHE_EXTRAS_PROGRESS_CURRENT, 0);
                    final int progressTotal = intent.getIntExtra(MainService.EXTRA_CACHE_EXTRAS_PROGRESS_TOTAL, 1);
                    final String msg2 = String.format(getString(R.string.cache_extras_progress), (progressCurrent * 100) / progressTotal);
                    if (cacheExtrasSnackbar == null) {
                        cacheExtrasSnackbar = Snackbar.make(findViewById(R.id.fab), msg2, Snackbar.LENGTH_INDEFINITE);
                    } else {
                        cacheExtrasSnackbar.setText(msg2);
                        if (!cacheExtrasSnackbar.isShown()) {
                            cacheExtrasSnackbar.show();
                        }
                    }
                    break;
                case MainService.ACTION_CACHE_EXTRAS_FINISHED:
                    final boolean success2 = intent.getBooleanExtra(MainService.EXTRA_CACHE_EXTRAS_FINISHED, false);
                    if (cacheExtrasSnackbar != null) {
                        cacheExtrasSnackbar.setDuration(Snackbar.LENGTH_LONG);
                        if (success2) {
                            cacheExtrasSnackbar.setText(R.string.cache_extras_success);
                        } else {
                            cacheExtrasSnackbar.setText(R.string.cache_extras_failure);
                        }
                        cacheExtrasSnackbar.show();
                        cacheExtrasSnackbar = null;
                    }
                    break;
                case FeedbackUtil.ACTION_FEEDBACK_PROVIDED:
                    String msg3;
                    if (intent.getBooleanExtra(FeedbackUtil.EXTRA_FEEDBACK_PROVIDED_DELAYED, false)) {
                        msg3 = getString(R.string.feedback_provided_delayed);
                    } else {
                        msg3 = getString(R.string.feedback_provided);
                    }
                    Snackbar.make(findViewById(R.id.fab), msg3, Snackbar.LENGTH_LONG).show();
                    break;
            }
        }
    };

    @Override
    public void checkNavigationDrawerItem(int id) {
        MenuItem item = navigationView.getMenu().findItem(id);
        if (item != null) {
            item.setChecked(true);
        } else {
            int size = navigationView.getMenu().size();
            for (int i = 0; i < size; i++) {
                navigationView.getMenu().getItem(i).setChecked(false);
            }
        }
    }

    @Override
    public void switchToPage(String pageString) {
        Fragment newFragment = getNewFragment(pageStringToResourceId(pageString));

        if (newFragment != null) {
            replaceFragment(newFragment, true);
        } else {
            Snackbar.make(findViewById(R.id.fab), R.string.status_not_yet_implemented, Snackbar.LENGTH_LONG).show();
        }
    }

    private String planRouteTo = null;

    @Override
    public String getRouteDestination() {
        String s = planRouteTo;
        planRouteTo = null;
        return s;
    }

    @Override
    public Collection<Network> getNetworks() {
        if (locBound) {
            return locService.getNetworks();
        } else {
            return new ArrayList<>();
        }
    }

    @Override
    public void updateNetworks(String... network_ids) {
        if (locBound) {
            locService.updateTopology(network_ids);
        }
    }

    @Override
    public void cacheAllExtras(String... network_ids) {
        if (locBound) {
            locService.cacheAllExtras(network_ids);
        }
    }

    @Override
    public void onListFragmentInteraction(LineRecyclerViewAdapter.LineItem item) {
        if (locService != null) {
            for (Network network : locService.getNetworks()) {
                Line line;
                if ((line = network.getLine(item.id)) != null) {
                    Intent intent = new Intent(this, LineActivity.class);
                    intent.putExtra(LineActivity.EXTRA_LINE_ID, line.getId());
                    intent.putExtra(LineActivity.EXTRA_NETWORK_ID, network.getId());
                    startActivity(intent);
                    return;
                }
            }
        }
    }

    @Override
    public void onLinesFinishedRefreshing() {
        SwipeRefreshLayout srl = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        srl.setRefreshing(false);
    }

    @Override
    public void onStatsFinishedRefreshing() {
        SwipeRefreshLayout srl = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        srl.setRefreshing(false);
    }

    @Override
    public void onListFragmentInteraction(DisturbanceRecyclerViewAdapter.DisturbanceItem item) {

    }

    @Override
    public void onListFragmentClick(TripRecyclerViewAdapter.TripItem item) {
        TripFragment f = TripFragment.newInstance(item.networkId, item.id);
        f.show(getSupportFragmentManager(), "trip-fragment");
    }

    @Override
    public void onListFragmentConfirmButtonClick(TripRecyclerViewAdapter.TripItem item) {
        Trip.confirm(item.id);
    }

    @Override
    public void onListFragmentCorrectButtonClick(TripRecyclerViewAdapter.TripItem item) {
        Intent intent = new Intent(this, TripCorrectionActivity.class);
        intent.putExtra(TripCorrectionActivity.EXTRA_NETWORK_ID, item.networkId);
        intent.putExtra(TripCorrectionActivity.EXTRA_TRIP_ID, item.id);
        startActivity(intent);
    }

    @Override
    public void onListFragmentInteraction(AnnouncementRecyclerViewAdapter.AnnouncementItem item) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(item.url));
        startActivity(browserIntent);
    }


    @Override
    public MainService getMainService() {
        return locService;
    }

    @Override
    public LineStatusCache getLineStatusCache() {
        if (locService == null) {
            return null;
        }
        return locService.getLineStatusCache();
    }

    @Override
    public boolean onSuggestionSelect(int position) {
        return false;
    }

    @Override
    public boolean onSuggestionClick(int position) {
        Cursor cursor = (Cursor) searchView.getSuggestionsAdapter().getItem(position);
        int intentDataColumn = cursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_INTENT_DATA);
        String intentData = cursor.getString(intentDataColumn);

        String[] parts = intentData.split(":");
        switch (parts[0]) {
            case "station":
                if (parts.length > 3 && parts[2].equals("lobby")) {
                    if (parts.length > 5 && parts[4].equals("exit")) {
                        onStationLinkClicked(parts[1], parts[3], parts[5]);
                    } else {
                        onStationLinkClicked(parts[1], parts[3]);
                    }
                } else {
                    onStationLinkClicked(parts[1]);
                }
                break;
            case "line":
                onLineLinkClicked(parts[1]);
                break;
            case "poi":
                onPOILinkClicked(parts[1]);
                break;
        }
        searchView.setIconified(true);
        searchView.clearFocus();
        // call your request, do some stuff..

        // collapse the action view
        if (searchItem != null) {
            searchItem.collapseActionView();
        }
        return true;
    }

    public static final String ACTION_MAIN_SERVICE_BOUND = "im.tny.segvault.disturbances.action.MainActivity.mainservicebound";

    public static final String EXTRA_INITIAL_FRAGMENT = "im.tny.segvault.disturbances.extra.MainActivity.initialfragment";

    public static final String EXTRA_PLAN_ROUTE_TO_STATION = "im.tny.segvault.disturbances.extra.MainActivity.planroute.to.station";
}