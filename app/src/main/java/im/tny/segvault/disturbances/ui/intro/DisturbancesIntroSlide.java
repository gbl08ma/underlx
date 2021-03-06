package im.tny.segvault.disturbances.ui.intro;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import androidx.annotation.Nullable;
import com.google.android.material.snackbar.Snackbar;
import androidx.fragment.app.Fragment;
import androidx.core.widget.CompoundButtonCompat;
import androidx.appcompat.widget.AppCompatCheckBox;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import im.tny.segvault.disturbances.Connectivity;
import im.tny.segvault.disturbances.Coordinator;
import im.tny.segvault.disturbances.PreferenceNames;
import im.tny.segvault.disturbances.R;
import im.tny.segvault.disturbances.Util;
import im.tny.segvault.subway.Line;

/**
 * Created by Gabriel on 27/07/2017.
 */

public class DisturbancesIntroSlide extends Fragment {
    private OnFragmentInteractionListener mListener;
    private View view;

    public static DisturbancesIntroSlide newInstance() {
        DisturbancesIntroSlide sampleSlide = new DisturbancesIntroSlide();
        return sampleSlide;
    }

    public DisturbancesIntroSlide() {

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_intro_disturbances, container, false);

        view.findViewById(R.id.select_lines_button).setOnClickListener(v -> {
            List<Line> lines = Coordinator.get(getContext()).getMapManager().getAllLines();
            Collections.sort(lines, (line, t1) -> Integer.valueOf(line.getOrder()).compareTo(t1.getOrder()));

            if (lines.size() == 0) {
                if (Connectivity.isConnected(getContext())) {
                    Snackbar.make(view, R.string.intro_disturbances_misc_error, Snackbar.LENGTH_LONG).show();
                } else {
                    Snackbar.make(view, R.string.intro_disturbances_connection_error, Snackbar.LENGTH_LONG).show();
                }
                return;
            }
            view.findViewById(R.id.description_layout).setVisibility(View.GONE);
            view.findViewById(R.id.select_lines_button).setVisibility(View.GONE);

            LinearLayout checkboxLayout = view.findViewById(R.id.checkbox_layout);

            SharedPreferences sharedPref = getContext().getSharedPreferences("notifsettings", Context.MODE_PRIVATE);
            Set<String> linePref = sharedPref.getStringSet(PreferenceNames.NotifsLines, null);

            for (final Line l : lines) {
                AppCompatCheckBox checkBox = new AppCompatCheckBox(view.getContext());
                checkBox.setText(Util.getLineNames(getContext(), l)[0]);
                checkBox.setTextColor(Color.WHITE);
                ColorStateList colorStateList = new ColorStateList(
                        new int[][]{
                                new int[]{-android.R.attr.state_checked}, // unchecked
                                new int[]{android.R.attr.state_checked}, // checked
                        },
                        new int[]{l.getColor(), l.getColor(),}
                );
                CompoundButtonCompat.setButtonTintList(checkBox, colorStateList);

                checkBox.setChecked(linePref == null || linePref.contains(l.getId()));

                checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> updateShowLineNotifs(l.getId(), isChecked));

                checkboxLayout.addView(checkBox);
            }

            view.findViewById(R.id.lines_layout).setVisibility(View.VISIBLE);
        });

        return view;
    }

    private void updateShowLineNotifs(String lineId, boolean show) {
        SharedPreferences sharedPref = getContext().getSharedPreferences("notifsettings", Context.MODE_PRIVATE);
        Set<String> defaultSet = new HashSet<String>();
        defaultSet.addAll(Arrays.asList(getResources().getStringArray(R.array.default_notif_lines)));
        Set<String> linePref = sharedPref.getStringSet(PreferenceNames.NotifsLines, defaultSet);
        if (show) {
            linePref.add(lineId);
        } else {
            linePref.remove(lineId);
        }
        SharedPreferences.Editor e = sharedPref.edit();
        e.putStringSet(PreferenceNames.NotifsLines, linePref);
        e.apply();
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (OnFragmentInteractionListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnFragmentInteractionListener {
    }
}