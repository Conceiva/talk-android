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
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.core.view.MenuItemCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.bluelinelabs.conductor.RouterTransaction;
import com.bluelinelabs.conductor.changehandler.HorizontalChangeHandler;
import com.bluelinelabs.conductor.changehandler.TransitionChangeHandlerCompat;
import com.bluelinelabs.conductor.changehandler.VerticalChangeHandler;
import com.bluelinelabs.conductor.internal.NoOpControllerChangeHandler;
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
import com.kennyc.bottomsheet.BottomSheet;
import com.nextcloud.talk.R;
import com.nextcloud.talk.activities.MagicCallActivity;
import com.nextcloud.talk.adapters.items.CallItem;
import com.nextcloud.talk.adapters.items.ConversationItem;
import com.nextcloud.talk.adapters.items.MeetingItems;
import com.nextcloud.talk.api.NcApi;
import com.nextcloud.talk.application.NextcloudTalkApplication;
import com.nextcloud.talk.controllers.base.BaseController;
import com.nextcloud.talk.controllers.bottomsheet.CallMenuController;
import com.nextcloud.talk.controllers.bottomsheet.EntryMenuController;
import com.nextcloud.talk.events.BottomSheetLockEvent;
import com.nextcloud.talk.events.EventStatus;
import com.nextcloud.talk.events.MeetingApiCallEvent;
import com.nextcloud.talk.events.MeetingApiRefreshEvent;
import com.nextcloud.talk.events.MoreMenuClickEvent;
import com.nextcloud.talk.interfaces.ClickInterface;
import com.nextcloud.talk.interfaces.ConversationMenuInterface;
import com.nextcloud.talk.jobs.DeleteConversationWorker;
import com.nextcloud.talk.models.database.UserEntity;
import com.nextcloud.talk.models.json.conversations.Conversation;
import com.nextcloud.talk.models.json.meetings.MeetingsReponse;
import com.nextcloud.talk.models.json.participants.Participant;
import com.nextcloud.talk.utils.ApiUtils;
import com.nextcloud.talk.utils.ConductorRemapping;
import com.nextcloud.talk.utils.DisplayUtils;
import com.nextcloud.talk.utils.KeyboardUtils;
import com.nextcloud.talk.utils.animations.SharedElementTransition;
import com.nextcloud.talk.utils.bundle.BundleKeys;
import com.nextcloud.talk.utils.database.user.UserUtils;
import com.nextcloud.talk.utils.preferences.AppPreferences;
import com.yarolegovich.lovelydialog.LovelySaveStateHandler;
import com.yarolegovich.lovelydialog.LovelyStandardDialog;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;
import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.Calendar;
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

import static com.nextcloud.talk.events.EventStatus.EventType.CONVERSATION_UPDATE;

@AutoInjector(NextcloudTalkApplication.class)
public class PastMeetingsListController extends BaseController implements SearchView.OnQueryTextListener,
        FlexibleAdapter.OnItemClickListener, FlexibleAdapter.OnItemLongClickListener, FastScroller
                .OnScrollStateChangeListener, ConversationMenuInterface {

    public static final String TAG = "PastMeetingsListController";
    public static final int ID_DELETE_CONVERSATION_DIALOG = 0;
    private static final String KEY_SEARCH_QUERY = "ContactsController.searchQuery";
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

    @BindView(R.id.noDataTextView)
    TextView noDataTextView;

    @BindView(R.id.sendHiTextView)
    TextView sendHiTextView;

    @BindView(R.id.swipeRefreshLayoutView)
    SwipeRefreshLayout swipeRefreshLayout;

    @BindView(R.id.progressBar)
    ProgressBar progressBarView;

    @BindView(R.id.emptyLayout)
    RelativeLayout emptyLayoutView;

    @BindView(R.id.fast_scroller)
    FastScroller fastScroller;

/*
    @BindView(R.id.floatingActionButton)
    FloatingActionButton floatingActionButton;
*/

    private UserEntity currentUser;
    private Disposable roomsQueryDisposable;
    private FlexibleAdapter<AbstractFlexibleItem> adapter;
    private List<AbstractFlexibleItem> callItems = new ArrayList<>();

    private BottomSheet bottomSheet;
    private MenuItem searchItem;
    private SearchView searchView;
    private String searchQuery;
    MaterialToolbar toolbar;
    float toolbarElevation =0;
    private View view;
    private boolean shouldUseLastMessageLayout;

    private String credentials;

    private boolean adapterWasNull = true;

    private boolean isRefreshing;

    private LovelySaveStateHandler saveStateHandler;

    private Bundle conversationMenuBundle = null;

    public PastMeetingsListController() {
        super();
//        setHasOptionsMenu(true);
    }

    @Override
    protected View inflateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container) {
        return inflater.inflate(R.layout.controller_meetings_rv, container, false);
    }

    @Override
    protected void onViewBound(@NonNull View view) {
        super.onViewBound(view);
        NextcloudTalkApplication.Companion.getSharedApplication().getComponentApplication().inject(this);

        if (getActionBar() != null) {
            getActionBar().show();
            toolbarElevation=getActionBar().getElevation();
            getActionBar().setElevation(0);
        }

        if (saveStateHandler == null) {
            saveStateHandler = new LovelySaveStateHandler();
        }

        if (adapter == null) {
            adapter = new FlexibleAdapter<>(callItems, getActivity(), true);
        } else {
            progressBarView.setVisibility(View.GONE);
        }

        adapter.addListener(this);
        prepareViews();
    }

    private void loadUserAvatar(MenuItem menuItem) {
        if (getActivity() != null) {
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

    @Override
    protected void onAttach(@NonNull View view) {
        super.onAttach(view);
        eventBus.register(this);

        currentUser = userUtils.getCurrentUser();

        if (currentUser != null) {
            credentials = ApiUtils.getCredentials(currentUser.getUsername(), currentUser.getToken());
            shouldUseLastMessageLayout = currentUser.hasSpreedFeatureCapability("last-room-activity");
//            fetchData(false);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    fetchData(false);
                }
            },500);

        }

    }

    @Override
    protected void onDetach(@NonNull View view) {
        super.onDetach(view);
        eventBus.unregister(this);
    }

    private void initSearchView() {
        if (getActivity() != null) {
            SearchManager searchManager = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
            if (searchItem != null) {
                searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
                searchView.setMaxWidth(Integer.MAX_VALUE);
                searchView.setInputType(InputType.TYPE_TEXT_VARIATION_FILTER);
                int imeOptions = EditorInfo.IME_ACTION_DONE | EditorInfo.IME_FLAG_NO_FULLSCREEN;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && appPreferences.getIsKeyboardIncognito()) {
                    imeOptions |= EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING;
                }
                searchView.setImeOptions(imeOptions);
                searchView.setQueryHint(getResources().getString(R.string.nc_search));
                if (searchManager != null) {
                    searchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));
                }
                searchView.setOnQueryTextListener(this);
            }
        }
    }

 /*   @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                ArrayList<String> names = new ArrayList<>();
                names.add("userAvatar.transitionTag");
                getParentController().getRouter().pushController((RouterTransaction.with(new SettingsController())
                        .pushChangeHandler(new TransitionChangeHandlerCompat(new SharedElementTransition(names), new VerticalChangeHandler()))
                        .popChangeHandler(new TransitionChangeHandlerCompat(new SharedElementTransition(names), new VerticalChangeHandler()))));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_conversation_plus_filter, menu);
        searchItem = menu.findItem(R.id.action_search);
        initSearchView();
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        searchItem.setVisible(callItems.size() > 0);
        if (adapter.hasFilter()) {
            searchItem.expandActionView();
            searchView.setQuery(adapter.getFilter(String.class), false);
        }

        MenuItem menuItem = menu.findItem(R.id.action_settings);
        loadUserAvatar(menuItem);
    }*/


    @Subscribe()
    public void onMeetingListAPIRespone(MeetingApiRefreshEvent event ) {
        fetchData(false);
    }

    private void fetchData(boolean fromBottomSheet) {
        dispose(null);

        isRefreshing = true;

        callItems = new ArrayList<>();

        roomsQueryDisposable = ncApi.getAllMeetings(credentials, ApiUtils.getUrlForGetMeetings(currentUser.getBaseUrl()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(meetingsReponses -> {

                    if (adapterWasNull) {
                        adapterWasNull = false;
                        progressBarView.setVisibility(View.GONE);
                        noDataTextView.setVisibility(View.GONE);
                    }


                   /* if (meetingsReponses.size() > 0) {
                        if (emptyLayoutView.getVisibility() != View.GONE) {
                            emptyLayoutView.setVisibility(View.GONE);
                        }

                        if (swipeRefreshLayout.getVisibility() != View.VISIBLE) {
                            swipeRefreshLayout.setVisibility(View.VISIBLE);
                        }
                    } else {
                        if (emptyLayoutView.getVisibility() != View.VISIBLE) {
                            emptyLayoutView.setVisibility(View.VISIBLE);
                        }

                            swipeRefreshLayout.setVisibility(View.GONE);
                    }*/


                    Collections.sort(meetingsReponses, new Comparator<MeetingsReponse>() {
                        @Override
                        public int compare(MeetingsReponse meetingsReponse, MeetingsReponse t1) {
                            return Integer.valueOf(t1.getEnd()).compareTo(meetingsReponse.getEnd()) ;
                        }
                    });

                    manageListItemsData(meetingsReponses);



                    adapter.updateDataSet(callItems, false);
                    if (searchItem != null) {
                        searchItem.setVisible(callItems.size() > 0);
                    }

                    if (swipeRefreshLayout != null) {
                        swipeRefreshLayout.setRefreshing(false);
                    }

                }, throwable -> {
                    if (searchItem != null) {
                        searchItem.setVisible(false);
                    }

                    if (throwable instanceof HttpException) {
                        HttpException exception = (HttpException) throwable;
                        switch (exception.code()) {
                            case 401:
                                if (getParentController() != null &&
                                        getParentController().getRouter() != null) {
                                    getParentController().getRouter().pushController((RouterTransaction.with
                                            (new WebViewLoginController(currentUser.getBaseUrl(),
                                                    true))
                                            .pushChangeHandler(new VerticalChangeHandler())
                                            .popChangeHandler(new VerticalChangeHandler())));
                                }
                                break;
                            default:
                                break;
                        }
                    }
                    if (swipeRefreshLayout != null) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                    dispose(roomsQueryDisposable);
                }, () -> {
                    dispose(roomsQueryDisposable);
                    if (swipeRefreshLayout != null) {
                        swipeRefreshLayout.setRefreshing(false);
                    }

                    if (fromBottomSheet) {
                        new Handler().postDelayed(() -> {
                            bottomSheet.setCancelable(true);
                            if (bottomSheet.isShowing()) {
                                bottomSheet.cancel();
                            }
                        }, 2500);
                    }

                    isRefreshing = false;
                });

    }

    private void manageListItemsData(List<MeetingsReponse> meetingsReponses)
    {
        MeetingsReponse meeting;
        List<MeetingsReponse> scheduledMeetingResponses=new ArrayList<>();
        for (int i = 0; i < meetingsReponses.size(); i++)
        {
            meeting = meetingsReponses.get(i);
            if(checkIfScheduledMeeting(meeting))
            {
                scheduledMeetingResponses.add(meeting);
            }
            else {
                MeetingItems conversationItem = new MeetingItems(meeting, currentUser);
                callItems.add(conversationItem);
            }
        }

        if (callItems.size() > 0) {
                emptyLayoutView.setVisibility(View.GONE);
                swipeRefreshLayout.setVisibility(View.VISIBLE);
        } else {
                emptyLayoutView.setVisibility(View.VISIBLE);
            swipeRefreshLayout.setVisibility(View.GONE);
        }

        eventBus.post(new MeetingApiCallEvent(scheduledMeetingResponses));

    }

    private boolean checkIfScheduledMeeting(MeetingsReponse meeting)
    {
        if(meeting.isRecurring())
        {
            return true;
        }
        Calendar c = Calendar.getInstance();
//        int utcOffset = c.get(Calendar.ZONE_OFFSET) + c.get(Calendar.DST_OFFSET);
        Long utcMilliseconds = c.getTimeInMillis() ;
        long currentTimeStamp = utcMilliseconds/1000;
        if (meeting.getEnd()>currentTimeStamp)
        {
            return true;
        }
        return false;
    }

    private void prepareViews() {
        SmoothScrollLinearLayoutManager layoutManager =
                new SmoothScrollLinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);

        recyclerView.setAdapter(adapter);
        sendHiTextView.setText(context.getResources().getString(R.string.str_nodata_past_meeting));
        swipeRefreshLayout.setOnRefreshListener(() -> fetchData(false));
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);

//        emptyLayoutView.setOnClickListener(v -> showNewConversationsScreen());
      /*  floatingActionButton.setOnClickListener(v -> {
            showNewConversationsScreen();
        });
*/
        fastScroller.addOnScrollStateChangeListener(this);
        adapter.setFastScroller(fastScroller);

        fastScroller.setBubbleTextCreator(position -> {
            String displayName;
            if (shouldUseLastMessageLayout) {
                displayName = ((MeetingItems) adapter.getItem(position)).getModel().getTitle();
            } else {
                displayName = ((MeetingItems) adapter.getItem(position)).getModel().getTitle();
            }

            if (displayName.length() > 8) {
                displayName = displayName.substring(0, 4) + "...";
            }
            return displayName;
        });

    }

    private void showNewConversationsScreen() {
        Bundle bundle = new Bundle();
        bundle.putBoolean(BundleKeys.INSTANCE.getKEY_NEW_CONVERSATION(), true);
        getRouter().pushController((RouterTransaction.with(new ContactsController(bundle))
                .pushChangeHandler(new HorizontalChangeHandler())
                .popChangeHandler(new HorizontalChangeHandler())));
    }

    private void dispose(@Nullable Disposable disposable) {
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
            disposable = null;
        } else if (disposable == null &&
                roomsQueryDisposable != null && !roomsQueryDisposable.isDisposed()) {
            roomsQueryDisposable.dispose();
            roomsQueryDisposable = null;

        }
    }

    @Override
    public void onSaveViewState(@NonNull View view, @NonNull Bundle outState) {
        saveStateHandler.saveInstanceState(outState);

        if (searchView != null && !TextUtils.isEmpty(searchView.getQuery())) {
            outState.putString(KEY_SEARCH_QUERY, searchView.getQuery().toString());
        }

        super.onSaveViewState(view, outState);
    }

    @Override
    public void onRestoreViewState(@NonNull View view, @NonNull Bundle savedViewState) {
        super.onRestoreViewState(view, savedViewState);
        searchQuery = savedViewState.getString(KEY_SEARCH_QUERY, "");
        if (LovelySaveStateHandler.wasDialogOnScreen(savedViewState)) {
            //Dialog won't be restarted automatically, so we need to call this method.
            //Each dialog knows how to restore its state
//            showLovelyDialog(LovelySaveStateHandler.getSavedDialogId(savedViewState), savedViewState);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        dispose(null);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getActionBar().setElevation(toolbarElevation);
        }
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        if (adapter.hasNewFilter(newText) || !TextUtils.isEmpty(searchQuery)) {

            if (!TextUtils.isEmpty(searchQuery)) {
                adapter.setFilter(searchQuery);
                searchQuery = "";
                adapter.filterItems();
            } else {
                adapter.setFilter(newText);
                adapter.filterItems(300);
            }
        }

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setEnabled(!adapter.hasFilter());
        }

        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return onQueryTextChange(query);
    }




    @Override
    protected String getTitle() {
        return getResources().getString(R.string.nc_app_name);
    }

    @Override
    public void onFastScrollerStateChange(boolean scrolling) {
        swipeRefreshLayout.setEnabled(!scrolling);
    }

    @Override
    public boolean onItemClick(View view, int position) {
        /*Object clickedItem = adapter.getItem(position);
        if (clickedItem != null && getActivity() != null) {
            Conversation conversation;
            if (shouldUseLastMessageLayout) {
                conversation = ((ConversationItem) clickedItem).getModel();
            } else {
                conversation = ((CallItem) clickedItem).getModel();
            }


            Bundle bundle = new Bundle();
            bundle.putParcelable(BundleKeys.INSTANCE.getKEY_USER_ENTITY(), currentUser);
            bundle.putString(BundleKeys.INSTANCE.getKEY_ROOM_TOKEN(), conversation.getToken());
            bundle.putString(BundleKeys.INSTANCE.getKEY_ROOM_ID(), conversation.getRoomId());

            if (conversation.hasPassword && (conversation.participantType.equals(Participant.ParticipantType.GUEST) ||
                    conversation.participantType.equals(Participant.ParticipantType.USER_FOLLOWING_LINK))) {
                bundle.putInt(BundleKeys.INSTANCE.getKEY_OPERATION_CODE(), 99);
                prepareAndShowBottomSheetWithBundle(bundle, false);
            } else {
                currentUser = userUtils.getCurrentUser();

                if (currentUser.hasSpreedFeatureCapability("chat-v2")) {
                    bundle.putParcelable(BundleKeys.INSTANCE.getKEY_ACTIVE_CONVERSATION(), Parcels.wrap(conversation));
                    ConductorRemapping.INSTANCE.remapChatController(getRouter(), currentUser.getId(),
                            conversation.getToken(), bundle, false);
                } else {
                    overridePushHandler(new NoOpControllerChangeHandler());
                    overridePopHandler(new NoOpControllerChangeHandler());
                    Intent callIntent = new Intent(getActivity(), MagicCallActivity.class);
                    callIntent.putExtras(bundle);
                    startActivity(callIntent);
                }
            }
        }*/

        return true;
    }

    @Override
    public void onItemLongClick(int position) {

    }


    @Override
    public void openLovelyDialogWithIdAndBundle(int dialogId, @NotNull Bundle bundle) {

    }
}
