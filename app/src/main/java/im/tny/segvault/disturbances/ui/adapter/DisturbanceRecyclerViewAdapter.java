package im.tny.segvault.disturbances.ui.adapter;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Spannable;
import android.text.Spanned;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.sufficientlysecure.htmltextview.HtmlTextView;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import im.tny.segvault.disturbances.API;
import im.tny.segvault.disturbances.InternalLinkHandler;
import im.tny.segvault.disturbances.OurHtmlHttpImageGetter;
import im.tny.segvault.disturbances.R;
import im.tny.segvault.disturbances.Util;
import im.tny.segvault.disturbances.ui.activity.LineActivity;
import im.tny.segvault.disturbances.ui.fragment.top.DisturbanceFragment.OnListFragmentInteractionListener;
import im.tny.segvault.disturbances.ui.util.RichTextUtils;
import im.tny.segvault.subway.Line;
import im.tny.segvault.subway.Network;

/**
 * {@link RecyclerView.Adapter} that can display a {@link DisturbanceItem} and makes a call to the
 * specified {@link OnListFragmentInteractionListener}.
 */
public class DisturbanceRecyclerViewAdapter extends RecyclerView.Adapter<DisturbanceRecyclerViewAdapter.ViewHolder> {

    private final List<DisturbanceItem> mValues;
    private final OnListFragmentInteractionListener mListener;
    private Context context;
    private boolean withCards = true;

    public DisturbanceRecyclerViewAdapter(List<DisturbanceItem> items, OnListFragmentInteractionListener listener) {
        mValues = items;
        mListener = listener;
    }

    public DisturbanceRecyclerViewAdapter(List<DisturbanceItem> items, OnListFragmentInteractionListener listener, boolean withCards) {
        mValues = items;
        mListener = listener;
        this.withCards = withCards;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(withCards ? R.layout.fragment_disturbance : R.layout.fragment_disturbance_nocard, parent, false);
        context = parent.getContext();
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);
        holder.mLineNameView.setText(String.format(holder.mView.getContext().getString(R.string.frag_disturbance_line), holder.mItem.lineName));
        holder.mLineNameView.setTextColor(holder.mItem.lineColor);
        holder.mDateView.setText(DateUtils.formatDateTime(holder.mView.getContext(), holder.mItem.startTime.getTime(), DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NO_YEAR));

        Drawable drawable = ContextCompat.getDrawable(holder.mView.getContext(), Util.getDrawableResourceIdForLineId(holder.mItem.lineId));
        drawable.setColorFilter(holder.mItem.lineColor, PorterDuff.Mode.SRC_ATOP);

        if (Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
            holder.iconLayout.setBackgroundDrawable(drawable);
        } else {
            holder.iconLayout.setBackground(drawable);
        }

        if (holder.mItem.ended) {
            holder.mOngoingView.setVisibility(View.GONE);
        } else {
            holder.mOngoingView.setVisibility(View.VISIBLE);
        }

        if (!holder.mItem.notes.isEmpty()) {
            holder.notesView.setHtml(holder.mItem.notes, new OurHtmlHttpImageGetter(holder.notesView, null, OurHtmlHttpImageGetter.ParentFitType.FIT_PARENT_WIDTH));
            holder.notesView.setText(RichTextUtils.replaceAll((Spanned) holder.notesView.getText(), URLSpan.class, new RichTextUtils.URLSpanConverter(), new InternalLinkHandler(context)));
            holder.notesLayout.setVisibility(View.VISIBLE);
        }

        holder.mLayout.removeAllViews();
        for (DisturbanceItem.Status s : mValues.get(position).statuses) {
            holder.mLayout.addView(new StatusView(holder.mView.getContext(), s));
        }

        holder.mView.setOnClickListener(v -> {
            if (null != mListener) {
                // Notify the active callbacks interface (the activity, if the
                // fragment is attached to one) that an item has been selected.
                mListener.onListFragmentInteraction(holder.mItem);
            }
        });

        View.OnClickListener lineClickListener = v -> {
            Intent intent = new Intent(context, LineActivity.class);
            intent.putExtra(LineActivity.EXTRA_LINE_ID, holder.mItem.lineId);
            intent.putExtra(LineActivity.EXTRA_NETWORK_ID, holder.mItem.networkId);
            context.startActivity(intent);
        };

        holder.mLineNameView.setOnClickListener(lineClickListener);
        holder.iconLayout.setOnClickListener(lineClickListener);
        holder.shareButton.setOnClickListener(view -> {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.setType("text/plain");
            sendIntent.putExtra(Intent.EXTRA_TEXT, String.format(Locale.US, context.getString(R.string.link_format_disturbance), holder.mItem.id));
            context.startActivity(Intent.createChooser(sendIntent, null));
        });
        holder.webButton.setOnClickListener(view -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(String.format(context.getString(R.string.link_format_disturbance), holder.mItem.id)));
            try {
                context.startActivity(browserIntent);
            } catch (ActivityNotFoundException e) {
                // oh well
            }
        });
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final LinearLayout mLayout;
        public final TextView mLineNameView;
        public final TextView mDateView;
        public final TextView mOngoingView;
        public final FrameLayout iconLayout;
        public final LinearLayout notesLayout;
        public final HtmlTextView notesView;
        public final ImageButton webButton;
        public final ImageButton shareButton;
        public DisturbanceItem mItem;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mLayout = view.findViewById(R.id.disturbance_status_layout);
            mLineNameView = view.findViewById(R.id.line_name_view);
            mDateView = view.findViewById(R.id.date_view);
            mOngoingView = view.findViewById(R.id.ongoing_view);
            iconLayout = view.findViewById(R.id.frame_icon);
            notesLayout = view.findViewById(R.id.disturbance_notes_layout);
            notesView = view.findViewById(R.id.disturbance_notes_view);
            webButton = view.findViewById(R.id.web_button);
            shareButton = view.findViewById(R.id.share_button);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mItem.id + "'";
        }
    }

    public static class DisturbanceItem implements Serializable {
        public final String id;
        public final Date startTime;
        public final Date endTime;
        public final boolean ended;
        public final String networkId;
        public final String lineId;
        public final String lineName;
        public final int lineColor;
        public final List<Status> statuses;
        public final String notes;

        public DisturbanceItem(API.Disturbance disturbance, Collection<Network> networks, Context context) {
            this.id = disturbance.id;
            this.startTime = new Date(disturbance.startTime[0] * 1000);
            this.endTime = new Date(disturbance.endTime[0] * 1000);
            this.ended = disturbance.ended;
            this.lineId = disturbance.line;
            this.notes = disturbance.notes;
            String name = "Unknown line";
            String netId = "";
            int color = 0;
            for (Network n : networks) {
                if (n.getId().equals(disturbance.network)) {
                    netId = n.getId();
                    for (Line l : n.getLines()) {
                        if (l.getId().equals(disturbance.line) && context != null) {
                            name = Util.getLineNames(context, l)[0];
                            color = l.getColor();
                            break;
                        }
                    }
                }
            }
            this.lineName = name;
            this.lineColor = color;
            this.networkId = netId;
            statuses = new ArrayList<>();
            for (API.Status s : disturbance.statuses) {
                Date stime = new Date(s.time[0] * 1000);
                Spannable text = Util.enrichLineStatus(context, netId, lineId, s.status, s.msgType,
                        stime, new InternalLinkHandler(context));
                statuses.add(new Status(stime, text, s.downtime, s.isOfficial()));
            }
            Collections.sort(statuses, (o1, o2) -> o1.date.compareTo(o2.date));
        }

        @Override
        public String toString() {
            return id;
        }

        public static class Status {
            public final Date date;
            public final Spannable status;
            public final boolean isDowntime;
            public final boolean isOfficial;

            public Status(Date date, Spannable status, boolean isDowntime, boolean isOfficial) {
                this.date = date;
                this.status = status;
                this.isDowntime = isDowntime;
                this.isOfficial = isOfficial;
            }
        }
    }


    private static class StatusView extends LinearLayout {
        private DisturbanceItem.Status status;

        public StatusView(Context context, DisturbanceItem.Status status) {
            super(context);
            this.setOrientation(HORIZONTAL);
            this.status = status;
            initializeViews(context);
        }

        public StatusView(Context context, AttributeSet attrs,
                          DisturbanceItem.Status status) {
            super(context, attrs);
            this.setOrientation(HORIZONTAL);
            this.status = status;
            initializeViews(context);
        }

        public StatusView(Context context,
                          AttributeSet attrs,
                          int defStyle,
                          DisturbanceItem.Status status) {
            super(context, attrs, defStyle);
            this.setOrientation(HORIZONTAL);
            this.status = status;
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
            inflater.inflate(R.layout.fragment_disturbance_status_view, this);

            TextView timeView = findViewById(R.id.time_view);
            ImageView communityView = findViewById(R.id.community_view);
            TextView statusView = findViewById(R.id.status_view);
            ImageView iconView = findViewById(R.id.icon_view);

            timeView.setText(DateUtils.formatDateTime(context, status.date.getTime(), DateUtils.FORMAT_SHOW_TIME));
            statusView.setText(status.status);
            statusView.setClickable(true);
            statusView.setMovementMethod(LinkMovementMethod.getInstance());

            if (status.isDowntime) {
                iconView.setVisibility(GONE);
            } else {
                iconView.setVisibility(VISIBLE);
            }

            if (!status.isOfficial) {
                communityView.setVisibility(VISIBLE);
            }
        }

        @Override
        protected void onFinishInflate() {
            super.onFinishInflate();
        }
    }
}

