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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.viewpager.widget.ViewPager;

import com.bluelinelabs.conductor.Router;
import com.bluelinelabs.conductor.RouterTransaction;
import com.bluelinelabs.conductor.changehandler.TransitionChangeHandlerCompat;
import com.bluelinelabs.conductor.changehandler.VerticalChangeHandler;
import com.bluelinelabs.conductor.support.RouterPagerAdapter;
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
import com.nextcloud.talk.application.NextcloudTalkApplication;
import com.nextcloud.talk.controllers.base.BaseController;
import com.nextcloud.talk.interfaces.ClickInterface;
import com.nextcloud.talk.models.database.UserEntity;
import com.nextcloud.talk.utils.ApiUtils;
import com.nextcloud.talk.utils.DisplayUtils;
import com.nextcloud.talk.utils.animations.SharedElementTransition;
import com.nextcloud.talk.utils.database.user.UserUtils;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import javax.inject.Inject;

import autodagger.AutoInjector;
import butterknife.BindView;
@AutoInjector(NextcloudTalkApplication.class)
public class MeetingsPagerController  extends BaseController {

    private int[] PAGE_COLORS = new int[]{R.color.colorPrimary, R.color.bg_bottom_sheet, R.color.black, R.color.white, R.color.nc_darkRed};

    @Inject
    UserUtils userUtils;

    @BindView(R.id.tabLayoutMeetings)
    TabLayout tabLayout;
    @BindView(R.id.viewPagerMeetings)
    ViewPager viewPager;

    private UserEntity currentUser;

    private RouterPagerAdapter pagerAdapter;



    public  MeetingsPagerController() {
        super();
        setHasOptionsMenu(true);

        pagerAdapter = new RouterPagerAdapter(this) {
            @Override
            public  void configureRouter(@NonNull Router router, int position) {
                if (!router.hasRootController()) {

                }
                if(position==0)
                {
                    router.setRoot(RouterTransaction.with(new PastMeetingsListController())
                            .pushChangeHandler(new VerticalChangeHandler())
                            .popChangeHandler(new VerticalChangeHandler())
                            .tag(PastMeetingsListController.TAG));
                }
                else {
                    router.pushController(RouterTransaction.with(new ScheduledMeetingsListController())
                            .pushChangeHandler(new VerticalChangeHandler())
                            .popChangeHandler(new VerticalChangeHandler())
                            .tag(ScheduledMeetingsListController.TAG));
                }
            }

            @Override
            public  int getCount() {
                return 2;
            }

            @Override
            public  CharSequence getPageTitle(int position)
            {
                String title="";
                if(position==0)
                {
                    title="Past Meetings";
                }
                else {
                    title="Scheduled Meetings";
                }
                return title;
            }
        };
    }

    @Override
    protected  void onViewBound(@NonNull View view) {
        super.onViewBound(view);
        viewPager.setAdapter(pagerAdapter);
        tabLayout.setupWithViewPager(viewPager);
    }
    @Override
    protected void onAttach(@NonNull View view) {
        super.onAttach(view);
    }
    @Override
    protected  void onDestroyView(@NonNull View view) {
        viewPager.setAdapter(null);
        super.onDestroyView(view);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_meetings, menu);

    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        MenuItem menuItem = menu.findItem(R.id.action_settings);
        loadUserAvatar(menuItem);
       /* new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                loadUserAvatar(menuItem);
            }
        }, 2000);*/
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
    protected  View inflateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container) {
        return inflater.inflate(R.layout.controller_meetings_pager, container, false);
    }

    private void loadUserAvatar(MenuItem menuItem) {
        if (getActivity() != null) {

            currentUser=NextcloudTalkApplication.Companion.getSharedApplication().userUtils.getCurrentUser();

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

   /* @Override
    protected  String getTitle() {
        return "ViewPager Demo";
    }*/



}