/*
 * Nextcloud Talk application
 *
 * @author Mario Danic
 * Copyright (C) 2017-2019 Mario Danic <mario@lovelyhq.com>
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

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.viewpager.widget.ViewPager;

import com.bluelinelabs.conductor.Router;
import com.bluelinelabs.conductor.RouterTransaction;
import com.bluelinelabs.conductor.changehandler.TransitionChangeHandlerCompat;
import com.bluelinelabs.conductor.changehandler.VerticalChangeHandler;
import com.bluelinelabs.conductor.support.RouterPagerAdapter;
import com.bluelinelabs.logansquare.LoganSquare;
import com.facebook.common.executors.UiThreadImmediateExecutorService;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.datasource.BaseBitmapDataSubscriber;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.request.ImageRequest;
import com.google.android.material.tabs.TabLayout;
import com.nextcloud.talk.R;
import com.nextcloud.talk.adapters.items.MetingAttendeeItem;
import com.nextcloud.talk.application.NextcloudTalkApplication;
import com.nextcloud.talk.controllers.base.BaseController;
import com.nextcloud.talk.models.database.UserEntity;
import com.nextcloud.talk.models.json.meetings.AttendeeDetails;
import com.nextcloud.talk.models.json.meetings.MeetingsReponse;
import com.nextcloud.talk.utils.ApiUtils;
import com.nextcloud.talk.utils.DateUtils;
import com.nextcloud.talk.utils.DisplayUtils;
import com.nextcloud.talk.utils.animations.SharedElementTransition;
import com.nextcloud.talk.utils.bundle.BundleKeys;
import com.nextcloud.talk.utils.database.user.UserUtils;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.VEvent;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;

import javax.inject.Inject;

import autodagger.AutoInjector;
import butterknife.BindView;

@AutoInjector(NextcloudTalkApplication.class)
public class MeetingDetailsPagerController extends BaseController {

    private int[] PAGE_COLORS = new int[]{R.color.colorPrimary, R.color.bg_bottom_sheet, R.color.black, R.color.white, R.color.nc_darkRed};

    @Inject
    UserUtils userUtils;

    @BindView(R.id.tabLayoutMeetings)
    TabLayout tabLayout;
    @BindView(R.id.viewPagerMeetings)
    ViewPager viewPager;

    @BindView(R.id.meetingTitleEditText)
    EditText meetingTitleEditText;

    @BindView(R.id.meetingIDTextView)
    TextView meetingIDTextView;

    @BindView(R.id.meetingTypeCheckBox)
    CheckBox meetingTypeCheckBox;

    @BindView(R.id.meetingFrequencyTextView)
    TextView meetingFrequencyTextView;

    @BindView(R.id.meetingStartDateTextView)
    TextView meetingStartDateTextView;

    @BindView(R.id.meetingStartTimeTextView)
    TextView meetingStartTimeTextView;

    @BindView(R.id.meetingEndsDateTextView)
    TextView meetingEndsDateTextView;

    @BindView(R.id.meetingEndsTimeTextView)
    TextView meetingEndsTimeTextView;

    @BindView(R.id.timeZoneSpinner)
    Spinner timeZoneSpinner;

    @BindView(R.id.timeZoneEndsSpinner)
    Spinner timeZoneEndsSpinner;

    @BindView(R.id.allDayEventCheckBox)
    CheckBox allDayEventCheckBox;


    private UserEntity currentUser;

    private RouterPagerAdapter pagerAdapter;
    MeetingsReponse meetingsReponse;
    Bundle meetingData;

    public MeetingDetailsPagerController(Bundle bundle) {
        super(bundle);
        setHasOptionsMenu(false);
        meetingData = bundle;
        meetingsReponse = bundle.getParcelable(BundleKeys.INSTANCE.getKEY_MEETING_DETAILS());
        pagerAdapter = new RouterPagerAdapter(this) {
            @Override
            public void configureRouter(@NonNull Router router, int position) {
                if (!router.hasRootController()) {

                }
                switch (position) {
                    case 0:
                        router.setRoot(RouterTransaction.with(new MeetingDetailsTab(meetingData))
                                .pushChangeHandler(new VerticalChangeHandler())
                                .popChangeHandler(new VerticalChangeHandler())
                                .tag(MeetingDetailsTab.TAG));
                        break;
                    case 1:
                        router.setRoot(RouterTransaction.with(new MeetingAttendeesTab(meetingData))
                                .pushChangeHandler(new VerticalChangeHandler())
                                .popChangeHandler(new VerticalChangeHandler())
                                .tag(MeetingDetailsTab.TAG));
                        break;

                    case 3:
                        router.setRoot(RouterTransaction.with(new MeetingRepeatsTab(meetingData))
                                .pushChangeHandler(new VerticalChangeHandler())
                                .popChangeHandler(new VerticalChangeHandler())
                                .tag(MeetingDetailsTab.TAG));
                        break;
                }
            }

            @Override
            public int getCount() {
                return 4;
            }

            @Override
            public CharSequence getPageTitle(int position) {
                String title = "";

                switch (position) {
                    case 0:
                        title = getApplicationContext().getResources().getString(R.string.details_tab);
                        break;
                    case 1:
                        title = getApplicationContext().getResources().getString(R.string.details_attendees);
                        break;
                    case 2:
                        title = getApplicationContext().getResources().getString(R.string.details_reminders);
                        break;
                    case 3:
                        title = getApplicationContext().getResources().getString(R.string.details_repeating);
                        break;
                }
                return title;
            }
        };
    }

    @Override
    protected void onViewBound(@NonNull View view) {
        super.onViewBound(view);
        viewPager.setAdapter(pagerAdapter);
        tabLayout.setupWithViewPager(viewPager);

        fillUpFieldsWithData();


    }

    private void fillUpFieldsWithData() {
        meetingTitleEditText.setText(meetingsReponse.getTitle());
        meetingIDTextView.setText("Meeting ID: " + meetingsReponse.getMeetingid());
        meetingTitleEditText.setText(meetingsReponse.getTitle());
        meetingStartDateTextView.setText(DateUtils.INSTANCE.getDateTimeStringFromTimestamp(meetingsReponse.getStart(), "MM/dd/yyyy", meetingsReponse.getTimezone()));
        meetingStartTimeTextView.setText(DateUtils.INSTANCE.getDateTimeStringFromTimestamp(meetingsReponse.getStart(), "hh:mm a", meetingsReponse.getTimezone()));

        meetingEndsDateTextView.setText(DateUtils.INSTANCE.getDateTimeStringFromTimestamp(meetingsReponse.getEnd(), "MM/dd/yyyy", meetingsReponse.getTimezone()));
        meetingEndsTimeTextView.setText(DateUtils.INSTANCE.getDateTimeStringFromTimestamp(meetingsReponse.getEnd(), "hh:mm a", meetingsReponse.getTimezone()));
        if (meetingsReponse.isJsonMemberPublic()) {
            meetingTypeCheckBox.setChecked(true);
        }
        String attendeesString = meetingsReponse.getAttendees();
        StringReader sin = new StringReader(meetingsReponse.getVcalendar());

        CalendarBuilder builder = new CalendarBuilder();
        Calendar cal = null;
        try {
            cal = builder.build(sin);

            VEvent component = (VEvent) cal.getComponents().getComponent("VEVENT");
            Property rrule = component.getProperties().getProperty("RRULE");
            if (rrule != null) {
                String rule = rrule.getValue().toString();
                String startingOn = " Starting on " + DateUtils.INSTANCE.getDateTimeStringFromTimestamp(meetingsReponse.getStart(), "dd MMMM yyyy HH:mm", meetingsReponse.getTimezone());
                if (getTextForFrequency(rule).equalsIgnoreCase("")) {
                    meetingFrequencyTextView.setVisibility(View.GONE);
                } else {
                    meetingFrequencyTextView.setVisibility(View.VISIBLE);
                    meetingFrequencyTextView.setText(getTextForFrequency(rule) + " " + startingOn);
                }
            } else {
                meetingFrequencyTextView.setVisibility(View.GONE);
            }
            //FREQ=YEARLY;BYMONTH=4;BYDAY=1SU
        } catch (IOException e) {
            Log.d("calendar", "io exception" + e.getLocalizedMessage());
        } catch (ParserException e) {
            Log.d("calendar", "parser exception" + e.getLocalizedMessage());
        }


    }

    private String getTextForFrequency(String rule) {
        String[] splitted = rule.split(";");
        String frequency = "", interval = "";

        String finalText = "";
        for (int i = 0; i < splitted.length; i++) {
            if (splitted[i].contains("FREQ")) {
                frequency = (splitted[i].split("="))[1];
            }

            if (splitted[i].contains("INTERVAL")) {
                interval = ((splitted[i].split("="))[1]);
            }
        }

        if (interval.equalsIgnoreCase("0"))
            return "";

        if (frequency.equalsIgnoreCase("DAILY")) {
            if (interval.equalsIgnoreCase("1") || interval.equalsIgnoreCase("")) {
                finalText = "Every Day";
            } else {
                finalText = "Every " + interval + " day";
            }
        } else if (frequency.equalsIgnoreCase("WEEKLY")) {
            if (interval.equalsIgnoreCase("1") || interval.equalsIgnoreCase("")) {
                finalText = "Every Week";
            } else {
                finalText = "Every " + interval + " Weeks";
            }
        } else if (frequency.equalsIgnoreCase("MONTHLY")) {
            if (interval.equalsIgnoreCase("1") || interval.equalsIgnoreCase("")) {
                finalText = "Every Month";
            } else {
                finalText = "Every " + interval + " Months";
            }
        } else if (frequency.equalsIgnoreCase("YEARLY")) {
            if (interval.equalsIgnoreCase("1") || interval.equalsIgnoreCase("")) {
                finalText = "Every Year";
            } else {
                finalText = "Every " + interval + " Years";
            }
        }
        return finalText;

    }

    @Override
    protected void onAttach(@NonNull View view) {
        super.onAttach(view);
        getActionBar().setDisplayHomeAsUpEnabled(false);
    }

    @Override
    protected void onDestroyView(@NonNull View view) {
        viewPager.setAdapter(null);
        super.onDestroyView(view);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_conversation_plus_filter, menu);

    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        MenuItem menuItem = menu.findItem(R.id.action_settings);
        loadUserAvatar(menuItem);

    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                ArrayList<String> names = new ArrayList<>();
                names.add("userAvatar.transitionTag");
                getRouter().pushController((RouterTransaction.with(new SettingsController())
                        .pushChangeHandler(new TransitionChangeHandlerCompat(new SharedElementTransition(names), new VerticalChangeHandler()))
                        .popChangeHandler(new TransitionChangeHandlerCompat(new SharedElementTransition(names), new VerticalChangeHandler()))));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @NonNull
    @Override
    protected View inflateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container) {
        return inflater.inflate(R.layout.controller_meeting_details, container, false);
    }

    private void loadUserAvatar(MenuItem menuItem) {
        if (getActivity() != null) {

            currentUser = NextcloudTalkApplication.Companion.getSharedApplication().userUtils.getCurrentUser();

            int avatarSize = (int) DisplayUtils.convertDpToPixel(menuItem.getIcon().getIntrinsicHeight(), getActivity());
            ImageRequest imageRequest = DisplayUtils.getImageRequestForUrl(ApiUtils.getUrlForAvatarWithNameAndPixels(currentUser.getBaseUrl(),
                    currentUser.getUserId(), avatarSize), null);

            ImagePipeline imagePipeline = Fresco.getImagePipeline();
            DataSource<CloseableReference<CloseableImage>> dataSource = imagePipeline.fetchDecodedImage(imageRequest, null);
            dataSource.subscribe(new BaseBitmapDataSubscriber() {
                @Override
                protected void onNewResultImpl(Bitmap bitmap) {
                    if (bitmap != null && getResources() != null) {
                        RoundedBitmapDrawable roundedBitmapDrawable = RoundedBitmapDrawableFactory.create(getResources(), bitmap);
                        roundedBitmapDrawable.setCircular(true);
                        roundedBitmapDrawable.setAntiAlias(true);
                        menuItem.setIcon(roundedBitmapDrawable);
                    }
                }

                @Override
                protected void onFailureImpl(DataSource<CloseableReference<CloseableImage>> dataSource) {
                    menuItem.setIcon(R.drawable.ic_settings_white_24dp);
                }
            }, UiThreadImmediateExecutorService.getInstance());
        }
    }

}