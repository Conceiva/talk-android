/*
 * Nextcloud Talk application
 *
 * @author Mario Danic
 * Copyright (C) 2017 Mario Danic (mario@lovelyhq.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nextcloud.talk.controllers;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.chivorn.smartmaterialspinner.SmartMaterialSpinner;
import com.nextcloud.talk.R;
import com.nextcloud.talk.api.NcApi;
import com.nextcloud.talk.application.NextcloudTalkApplication;
import com.nextcloud.talk.controllers.base.BaseController;
import com.nextcloud.talk.models.database.UserEntity;
import com.nextcloud.talk.models.json.meetings.MeetingsReponse;
import com.nextcloud.talk.utils.bundle.BundleKeys;
import com.nextcloud.talk.utils.database.user.UserUtils;
import com.nextcloud.talk.utils.preferences.AppPreferences;
import com.yarolegovich.lovelydialog.LovelySaveStateHandler;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.VEvent;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;

import javax.inject.Inject;

import autodagger.AutoInjector;
import butterknife.BindView;
import io.reactivex.disposables.Disposable;

@AutoInjector(NextcloudTalkApplication.class)
public class MeetingRepeatsTab extends BaseController {

    public static final String TAG = "PastMeetingsListController";
    @Inject
    UserUtils userUtils;

    @Inject
    EventBus eventBus;

    @Inject
    NcApi ncApi;

    @Inject
    Context context;

    @Inject
    AppPreferences appPreferences;

    @BindView(R.id.repeatTimesEditText)
    EditText repeatTimesEditText;

    @BindView(R.id.repeatcountTextView)
    TextView repeatcountTextView;

    @BindView(R.id.repeatTimeLinearLayout)
    LinearLayout repeatTimeLinearLayout;

    @BindView(R.id.noneLinearLayout)
    LinearLayout noneLinearLayout;

    @BindView(R.id.frequencySpinner)
    SmartMaterialSpinner frequencySpinner;


    @BindView(R.id.repeatSpinner)
    SmartMaterialSpinner repeatSpinner;

    private UserEntity currentUser;
    private LovelySaveStateHandler saveStateHandler;
    MeetingsReponse meetingsReponse;
    ArrayList<String> choiceFrequency = new ArrayList<>();
    ArrayList<String> choiceEnd = new ArrayList<>();


    public MeetingRepeatsTab(Bundle meetingDataBundle) {
        super(meetingDataBundle);
        meetingsReponse = meetingDataBundle.getParcelable(BundleKeys.INSTANCE.getKEY_MEETING_DETAILS());
    }

    @Override
    protected View inflateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container) {
        return inflater.inflate(R.layout.tab_meeting_repeat, container, false);
    }

    @Override
    protected void onViewBound(@NonNull View view) {
        super.onViewBound(view);
        NextcloudTalkApplication.Companion.getSharedApplication().getComponentApplication().inject(this);

        if (getActionBar() != null) {
            getActionBar().show();
        }

        if (saveStateHandler == null) {
            saveStateHandler = new LovelySaveStateHandler();
        }
        prepareViews();
    }


    @Override
    protected void onAttach(@NonNull View view) {
        super.onAttach(view);
//        eventBus.register(this);

        currentUser = userUtils.getCurrentUser();


    }

    @Override
    protected void onDetach(@NonNull View view) {
        super.onDetach(view);
//        eventBus.unregister(this);
    }

    private void prepareViews() {


        choiceFrequency.clear();
        choiceFrequency.add("None");
        choiceFrequency.add("Every day");
        choiceFrequency.add("Every week");
        choiceFrequency.add("Every month");
        choiceFrequency.add("Every year");

        choiceEnd.clear();
        choiceEnd.add("never");
        choiceEnd.add("after");


        StringReader sin = new StringReader(meetingsReponse.getVcalendar());
        CalendarBuilder builder = new CalendarBuilder();
        Calendar cal = null;
        try {
            cal = builder.build(sin);
            VEvent component = (VEvent) cal.getComponents().getComponent("VEVENT");
            Property rrule = component.getProperties().getProperty("LOCATION");
            String rule = rrule.getValue().toString();
        } catch (ParserException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }


        frequencySpinner.setItem(choiceFrequency);
        frequencySpinner.setSelection(0);
        frequencySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
//                Toast.makeText(getActivity(), choiceList.get(position), Toast.LENGTH_SHORT).show();
                if(position==0) {
                    manageVisibility(false);
                }else {
                    manageVisibility(true);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });


        repeatSpinner.setItem(choiceEnd);
        repeatSpinner.setSelection(0);
        repeatSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
//                Toast.makeText(getActivity(), choiceList.get(position), Toast.LENGTH_SHORT).show();
                if(position==0) {
                    manageTimesVisibility(false);
                }else {
                    manageTimesVisibility(true);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

    }

    private void manageVisibility(boolean b)
    {
        if(b)
        {
            noneLinearLayout.setVisibility(View.VISIBLE);
        }
        else {
            noneLinearLayout.setVisibility(View.GONE);
        }
    }


    private void manageTimesVisibility(boolean b)
    {
        if(b)
        {
            repeatTimeLinearLayout.setVisibility(View.VISIBLE);
        }
        else {
            repeatTimeLinearLayout.setVisibility(View.GONE);
        }
    }

    private void dispose(@Nullable Disposable disposable) {
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
            disposable = null;
        }

    }

    @Override
    public void onSaveViewState(@NonNull View view, @NonNull Bundle outState) {
        saveStateHandler.saveInstanceState(outState);
        super.onSaveViewState(view, outState);
    }

    @Override
    public void onRestoreViewState(@NonNull View view, @NonNull Bundle savedViewState) {
        super.onRestoreViewState(view, savedViewState);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        dispose(null);

    }


}
