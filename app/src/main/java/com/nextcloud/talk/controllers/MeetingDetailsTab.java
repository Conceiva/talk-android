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

import android.app.SearchManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.core.view.MenuItemCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager.widget.ViewPager;

import com.bluelinelabs.conductor.RouterTransaction;
import com.bluelinelabs.conductor.changehandler.HorizontalChangeHandler;
import com.bluelinelabs.conductor.changehandler.VerticalChangeHandler;
import com.chivorn.smartmaterialspinner.SmartMaterialSpinner;
import com.facebook.common.executors.UiThreadImmediateExecutorService;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.datasource.BaseBitmapDataSubscriber;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.request.ImageRequest;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.jaredrummler.materialspinner.MaterialSpinner;
import com.kennyc.bottomsheet.BottomSheet;
import com.nextcloud.talk.R;
import com.nextcloud.talk.activities.MainActivity;
import com.nextcloud.talk.adapters.items.MeetingItems;
import com.nextcloud.talk.api.NcApi;
import com.nextcloud.talk.application.NextcloudTalkApplication;
import com.nextcloud.talk.controllers.base.BaseController;
import com.nextcloud.talk.events.MeetingApiCallEvent;
import com.nextcloud.talk.events.MeetingApiRefreshEvent;
import com.nextcloud.talk.interfaces.ConversationMenuInterface;
import com.nextcloud.talk.models.database.UserEntity;
import com.nextcloud.talk.models.json.meetings.MeetingsReponse;
import com.nextcloud.talk.utils.ApiUtils;
import com.nextcloud.talk.utils.DisplayUtils;
import com.nextcloud.talk.utils.bundle.BundleKeys;
import com.nextcloud.talk.utils.database.user.UserUtils;
import com.nextcloud.talk.utils.preferences.AppPreferences;
import com.yarolegovich.lovelydialog.LovelySaveStateHandler;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.component.VEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;

import autodagger.AutoInjector;
import butterknife.BindView;
import eu.davidea.fastscroller.FastScroller;
import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.common.SmoothScrollLinearLayoutManager;
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import retrofit2.HttpException;

@AutoInjector(NextcloudTalkApplication.class)
public class MeetingDetailsTab extends BaseController {

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

    @BindView(R.id.meetingTitleEditText)
    EditText meetingTitleEditText;

    @BindView(R.id.descriptionEditText)
    EditText descriptionEditText;


    @BindView(R.id.contactStatusSpinner)
    SmartMaterialSpinner contactStatusSpinner;

    private UserEntity currentUser;
    private LovelySaveStateHandler saveStateHandler;
    MeetingsReponse meetingsReponse;

    public MeetingDetailsTab(Bundle meetingDataBundle) {
        super(meetingDataBundle);
        meetingsReponse = meetingDataBundle.getParcelable(BundleKeys.INSTANCE.getKEY_MEETING_DETAILS());
    }

    @Override
    protected View inflateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container) {
        return inflater.inflate(R.layout.tab_meeting_details, container, false);
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
        ArrayList<String> choiceList = new ArrayList<>();

        choiceList.add("When Shared show full event");
        choiceList.add("When Shared show only busy");
        choiceList.add("When Shared hide event");

        meetingTitleEditText.setText(meetingsReponse.getDescription() + "");
        StringReader sin = new StringReader(meetingsReponse.getVcalendar());
        CalendarBuilder builder = new CalendarBuilder();
        Calendar cal = null;
        try {
            cal = builder.build(sin);
         VEvent component = (VEvent) cal.getComponents().getComponent("VEVENT");

        } catch (ParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        contactStatusSpinner.setItem(choiceList);
        contactStatusSpinner.setSelection(0);
        contactStatusSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
//                Toast.makeText(getActivity(), choiceList.get(position), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

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
