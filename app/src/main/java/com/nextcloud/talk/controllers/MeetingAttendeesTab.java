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
import android.database.Cursor;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bluelinelabs.logansquare.LoganSquare;
import com.chivorn.smartmaterialspinner.SmartMaterialSpinner;
import com.nextcloud.talk.R;
import com.nextcloud.talk.adapters.items.MetingAttendeeItem;
import com.nextcloud.talk.adapters.items.NotificationSoundItem;
import com.nextcloud.talk.api.NcApi;
import com.nextcloud.talk.application.NextcloudTalkApplication;
import com.nextcloud.talk.controllers.base.BaseController;
import com.nextcloud.talk.models.database.UserEntity;
import com.nextcloud.talk.models.json.meetings.AttendeeDetails;
import com.nextcloud.talk.models.json.meetings.MeetingsReponse;
import com.nextcloud.talk.utils.bundle.BundleKeys;
import com.nextcloud.talk.utils.database.user.UserUtils;
import com.nextcloud.talk.utils.preferences.AppPreferences;
import com.yarolegovich.lovelydialog.LovelySaveStateHandler;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import autodagger.AutoInjector;
import butterknife.BindView;
import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.SelectableAdapter;
import eu.davidea.flexibleadapter.common.SmoothScrollLinearLayoutManager;
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem;
import io.reactivex.disposables.Disposable;

@AutoInjector(NextcloudTalkApplication.class)
public class MeetingAttendeesTab extends BaseController {

    public static final String TAG = "MeetingAttendeesTab";
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


    @BindView(R.id.recycler_view)
    RecyclerView recyclerView;

    private FlexibleAdapter adapter;
    private RecyclerView.AdapterDataObserver adapterDataObserver;
    private List<AbstractFlexibleItem> abstractFlexibleItemList = new ArrayList<>();

    private UserEntity currentUser;
    private LovelySaveStateHandler saveStateHandler;
    MeetingsReponse meetingsReponse;
    public MeetingAttendeesTab(Bundle bundle) {
        super(bundle);
        meetingsReponse = bundle.getParcelable(BundleKeys.INSTANCE.getKEY_MEETING_DETAILS());
    }

    @Override
    protected View inflateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container) {
        return inflater.inflate(R.layout.controller_attendees_details, container, false);
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

        if (adapter == null) {
            adapter = new FlexibleAdapter<>(abstractFlexibleItemList, getActivity(), false);

            adapter.setNotifyChangeOfUnfilteredItems(true)
                    .setMode(SelectableAdapter.Mode.SINGLE);

            adapter.addListener(this);

        }

        adapter.addListener(this);

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
        ArrayList<String> choiceList = new ArrayList<>();

        RecyclerView.LayoutManager layoutManager = new SmoothScrollLinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(adapter);

        adapterDataObserver = new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
//                findSelectedSound();
            }
        };

        adapter.registerAdapterDataObserver(adapterDataObserver);

        prepareRecyclerView();


    }

    private void prepareRecyclerView()
    {
        if (getActivity() != null) {
            MetingAttendeeItem attendeeItem;
            String attendeesString=meetingsReponse.getAttendees();
            ArrayList<AttendeeDetails> attendeeList=new ArrayList<>();
            try
            {
                JSONArray mainJsonArray=new JSONArray(attendeesString);
                for (int i=0;i<mainJsonArray.length();i++)
                {
                    JSONArray jsonArray= (JSONArray) mainJsonArray.get(i);
                    AttendeeDetails attendeeDetails= LoganSquare.parse(jsonArray.getJSONObject(1).toString(),AttendeeDetails.class);
                    attendeeList.add(attendeeDetails);

                    attendeeItem = new MetingAttendeeItem(attendeeDetails.getCn(), attendeeDetails.getRole());
                    abstractFlexibleItemList.add(attendeeItem);

                }

            } catch (JSONException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }



        }

        adapter.updateDataSet(abstractFlexibleItemList, false);

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
