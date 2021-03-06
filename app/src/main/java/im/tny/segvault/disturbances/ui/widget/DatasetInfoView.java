package im.tny.segvault.disturbances.ui.widget;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import im.tny.segvault.disturbances.Coordinator;
import im.tny.segvault.disturbances.MapManager;
import im.tny.segvault.disturbances.R;
import im.tny.segvault.disturbances.Util;
import im.tny.segvault.disturbances.ui.fragment.top.AboutFragment;
import im.tny.segvault.subway.Network;

public class DatasetInfoView extends LinearLayout {
    private Button updateButton;
    private Button cacheAllExtrasButton;
    private Network net;
    private AboutFragment containingFragment;

    public DatasetInfoView(Context context, Network net, AboutFragment aboutFragment) {
        super(context);
        this.setOrientation(VERTICAL);
        this.net = net;
        this.containingFragment = aboutFragment;
        initializeViews(context);
    }

    public DatasetInfoView(Context context, AttributeSet attrs, Network net, AboutFragment aboutFragment) {
        super(context, attrs);
        this.setOrientation(VERTICAL);
        this.net = net;
        this.containingFragment = aboutFragment;
        initializeViews(context);
    }

    public DatasetInfoView(Context context,
                           AttributeSet attrs,
                           int defStyle, Network net, AboutFragment aboutFragment) {
        super(context, attrs, defStyle);
        this.setOrientation(VERTICAL);
        this.net = net;
        this.containingFragment = aboutFragment;
        initializeViews(context);
    }

    /**
     * Inflates the views in the layout.
     *
     * @param context the current context for the view.
     */
    private void initializeViews(Context context) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.dataset_info_view, this);

        TextView nameView = findViewById(R.id.dataset_info_name);
        TextView versionView = findViewById(R.id.dataset_info_version);
        TextView authorsView = findViewById(R.id.dataset_info_authors);
        updateButton = findViewById(R.id.dataset_update_button);
        cacheAllExtrasButton = findViewById(R.id.dataset_cache_all_button);

        String[] names = Util.getNetworkNames(context, net);
        if(names.length == 1) {
            nameView.setText(names[0]);
        } else {
            SpannableStringBuilder str = new SpannableStringBuilder(names[0] + "\t\t\t" + names[1]);
            int start = names[0].length() + 3;
            int end = names[0].length() + names[1].length() + 3;
            str.setSpan(new android.text.style.StyleSpan(Typeface.ITALIC), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            nameView.setText(str);
        }
        versionView.setText(String.format(context.getString(R.string.dataset_info_version), net.getDatasetVersion()));

        String authors = "";
        for (String author : net.getDatasetAuthors()) {
            authors += author + ", ";
        }
        authorsView.setText(authors.substring(0, authors.length() - 2));

        updateButton.setOnClickListener(view -> containingFragment.updateNetworks(net.getId()));
        cacheAllExtrasButton.setOnClickListener(view -> containingFragment.cacheAllExtras(net.getId()));

        IntentFilter filter = new IntentFilter();
        filter.addAction(MapManager.ACTION_UPDATE_TOPOLOGY_PROGRESS);
        filter.addAction(MapManager.ACTION_UPDATE_TOPOLOGY_FINISHED);
        filter.addAction(MapManager.ACTION_UPDATE_TOPOLOGY_CANCELLED);
        filter.addAction(Coordinator.ACTION_CACHE_EXTRAS_PROGRESS);
        filter.addAction(Coordinator.ACTION_CACHE_EXTRAS_FINISHED);
        LocalBroadcastManager bm = LocalBroadcastManager.getInstance(containingFragment.getContext());
        bm.registerReceiver(mBroadcastReceiver, filter);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
    }

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case MapManager.ACTION_UPDATE_TOPOLOGY_PROGRESS:
                    updateButton.setEnabled(false);
                    break;
                case MapManager.ACTION_UPDATE_TOPOLOGY_FINISHED:
                case MapManager.ACTION_UPDATE_TOPOLOGY_CANCELLED:
                    updateButton.setEnabled(true);
                    break;
                case Coordinator.ACTION_CACHE_EXTRAS_PROGRESS:
                    cacheAllExtrasButton.setEnabled(false);
                    break;
                case Coordinator.ACTION_CACHE_EXTRAS_FINISHED:
                    cacheAllExtrasButton.setEnabled(true);
                    break;
            }
        }
    };
}
