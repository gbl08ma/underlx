package im.tny.segvault.disturbances.ui.activity;

import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.codetroopers.betterpickers.radialtimepicker.RadialTimePickerDialogFragment;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import im.tny.segvault.disturbances.Application;
import im.tny.segvault.disturbances.R;
import im.tny.segvault.disturbances.model.NotificationRule;
import io.realm.Realm;
import io.realm.RealmList;

public class EditNotifScheduleActivity extends TopActivity implements RadialTimePickerDialogFragment.OnTimeSetListener {
    private Realm realm;
    private NotificationRule rule;
    private String ruleId;
    private boolean deleted = false;

    private ListView listView;
    private SimpleAdapter adapter;

    private Map<String, String> startTimeItem;
    private Map<String, String> endTimeItem;
    private Map<String, String> weekDaysItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            ruleId = getIntent().getStringExtra(EXTRA_RULE_ID);
        } else {
            ruleId = savedInstanceState.getString(STATE_RULE_ID);
        }

        setContentView(R.layout.activity_edit_notif_schedule);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        realm = Application.getDefaultRealmInstance(this);

        realm.executeTransaction(realm -> {
            if (ruleId == null) {
                rule = realm.createObject(NotificationRule.class, UUID.randomUUID().toString());
            } else {
                rule = realm.where(NotificationRule.class).equalTo("id", ruleId).findFirst();
                if (rule == null) {
                    finish();
                    return;
                }
            }

            ruleId = rule.getId();

            List<Map<String, String>> data = new ArrayList<>();
            weekDaysItem = new HashMap<>(3);
            weekDaysItem.put("title", getString(R.string.act_edit_notif_schedule_days));
            weekDaysItem.put("desc", "");
            data.add(weekDaysItem);

            startTimeItem = new HashMap<>(3);
            startTimeItem.put("title", getString(R.string.act_edit_notif_schedule_start_time));
            startTimeItem.put("desc", "");
            data.add(startTimeItem);

            endTimeItem = new HashMap<>(3);
            endTimeItem.put("title", getString(R.string.act_edit_notif_schedule_end_time));
            endTimeItem.put("desc", "");
            data.add(endTimeItem);

            listView = findViewById(R.id.list_view);
            listView.setOnItemClickListener((parent, view, position, id) -> {
                if (position == 0) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(EditNotifScheduleActivity.this);

                    DateFormatSymbols symbols = new DateFormatSymbols();
                    String[] dayNames = Arrays.copyOfRange(symbols.getWeekdays(), 1, 8);
                    final boolean[] checkedDays = new boolean[7];
                    for (int day : rule.getWeekDays()) {
                        checkedDays[day - 1] = true;
                    }

                    builder.setMultiChoiceItems(dayNames, checkedDays, (dialog, which, isChecked) -> checkedDays[which] = isChecked);
                    builder.setTitle(getString(R.string.act_edit_notif_schedule_days));
                    builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        realm.executeTransaction(realm1 -> {
                            RealmList<Integer> days = rule.getWeekDays();
                            days.clear();
                            for (int i = 0; i < 7; i++) {
                                if (checkedDays[i]) {
                                    days.add(i + 1);
                                }
                            }
                        });
                        updateUI();
                    });

                    builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> {

                    });

                    builder.create().show();
                } else if (position == 1) {
                    RadialTimePickerDialogFragment rtpd = new RadialTimePickerDialogFragment()
                            .setOnTimeSetListener(EditNotifScheduleActivity.this)
                            .setStartTime((int) TimeUnit.MILLISECONDS.toHours(rule.getStartTime()) % 24, (int) TimeUnit.MILLISECONDS.toMinutes(rule.getStartTime()) % 60)
                            .setDoneText(getString(android.R.string.ok))
                            .setCancelText(getString(android.R.string.cancel))
                            .setThemeDark();
                    rtpd.show(getSupportFragmentManager(), TAG_START_TIME_PICKER);
                } else if (position == 2) {
                    RadialTimePickerDialogFragment rtpd = new RadialTimePickerDialogFragment()
                            .setOnTimeSetListener(EditNotifScheduleActivity.this)
                            .setStartTime((int) TimeUnit.MILLISECONDS.toHours(rule.getEndTime()) % 24, (int) TimeUnit.MILLISECONDS.toMinutes(rule.getEndTime()) % 60)
                            .setDoneText(getString(android.R.string.ok))
                            .setCancelText(getString(android.R.string.cancel))
                            .setThemeDark();
                    rtpd.show(getSupportFragmentManager(), TAG_END_TIME_PICKER);
                }
            });

            adapter = new SimpleAdapter(EditNotifScheduleActivity.this, data,
                    android.R.layout.simple_list_item_2,
                    new String[]{"title", "desc"},
                    new int[]{android.R.id.text1, android.R.id.text2});
            listView.setAdapter(adapter);

            updateUI();
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.edit_notif_schedule, menu);
        return true;
    }

    private void updateUI() {
        Formatter f = new Formatter(new StringBuilder(50), Locale.getDefault());
        DateUtils.formatDateRange(this, f, rule.getStartTime(), rule.getStartTime(), DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_NO_NOON, Time.TIMEZONE_UTC);
        startTimeItem.put("desc", f.toString());
        f = new Formatter(new StringBuilder(50), Locale.getDefault());
        DateUtils.formatDateRange(this, f, rule.getEndTime(), rule.getEndTime(), DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_NO_NOON, Time.TIMEZONE_UTC);
        endTimeItem.put("desc", f.toString());

        DateFormatSymbols symbols = new DateFormatSymbols();
        String[] dayNames = symbols.getShortWeekdays();
        ArrayList<String> enabledDays = new ArrayList<>();
        if (rule.getWeekDays().size() == 0 || !rule.isEnabled()) {
            weekDaysItem.put("desc", getString(R.string.act_edit_notif_schedule_days_none));
        } else {
            for (int day : rule.getWeekDays()) {
                enabledDays.add(dayNames[day]);
            }
            weekDaysItem.put("desc", TextUtils.join(", ", enabledDays));
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onPause() {
        if (!deleted) {
            realm.executeTransaction(realm -> realm.copyToRealmOrUpdate(rule));
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        realm.close();
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                return true;
            case R.id.menu_delete:
                deleted = true;
                realm.executeTransaction(realm -> rule.deleteFromRealm());
                finish();
        }
        return super.onOptionsItemSelected(item);
    }

    private static final String TAG_START_TIME_PICKER = "startTime";
    private static final String TAG_END_TIME_PICKER = "endTime";

    public static final String STATE_RULE_ID = "ruleId";

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putString(STATE_RULE_ID, ruleId);

        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    public static final String EXTRA_RULE_ID = "im.tny.segvault.disturbances.extra.EditNotifScheduleActivity.ruleid";

    @Override
    public void onTimeSet(final RadialTimePickerDialogFragment dialog, final int hourOfDay, final int minute) {
        realm.executeTransaction(realm -> {
            switch (dialog.getTag()) {
                case TAG_START_TIME_PICKER:
                    rule.setStartTime(TimeUnit.HOURS.toMillis(hourOfDay) + TimeUnit.MINUTES.toMillis(minute));
                    break;
                case TAG_END_TIME_PICKER:
                    rule.setEndTime(TimeUnit.HOURS.toMillis(hourOfDay) + TimeUnit.MINUTES.toMillis(minute));
                    if (rule.getEndTime() <= rule.getStartTime()) {
                        rule.setEndTime(rule.getEndTime() + TimeUnit.HOURS.toMillis(24));
                    }
                    break;
            }
        });

        updateUI();
    }
}
