/*
 *
 *   Nextcloud Talk application
 *
 *   @author Mario Danic
 *   Copyright (C) 2017 Mario Danic (mario@lovelyhq.com)
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.nextcloud.talk.activities;

import android.app.KeyguardManager
import android.app.PendingIntent.getActivity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import autodagger.AutoInjector
import butterknife.BindView
import butterknife.ButterKnife
import com.bluelinelabs.conductor.Conductor
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.changehandler.HorizontalChangeHandler
import com.bluelinelabs.conductor.changehandler.VerticalChangeHandler
import com.bluelinelabs.conductor.internal.NoOpControllerChangeHandler
import com.google.android.material.appbar.MaterialToolbar
import com.nextcloud.talk.R
import com.nextcloud.talk.activities.BaseActivity
import com.nextcloud.talk.adapters.items.CallItem
import com.nextcloud.talk.adapters.items.ConversationItem
import com.nextcloud.talk.api.NcApi
import com.nextcloud.talk.application.NextcloudTalkApplication
import com.nextcloud.talk.controllers.*
import com.nextcloud.talk.controllers.base.providers.ActionBarProvider
import com.nextcloud.talk.events.MeetingItemClickEvent
import com.nextcloud.talk.events.MeetingItemJoinMeetingClickEvent
import com.nextcloud.talk.models.database.UserEntity
import com.nextcloud.talk.models.json.conversations.Conversation
import com.nextcloud.talk.models.json.meetings.MeetingsReponse
import com.nextcloud.talk.models.json.participants.Participant
import com.nextcloud.talk.utils.ApiUtils
import com.nextcloud.talk.utils.ConductorRemapping
import com.nextcloud.talk.utils.PreferenceHelper
import com.nextcloud.talk.utils.SecurityUtils
import com.nextcloud.talk.utils.bundle.BundleKeys
import com.nextcloud.talk.utils.database.user.UserUtils
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.requery.Persistable
import io.requery.android.sqlcipher.SqlCipherDatabaseSource
import io.requery.reactivex.ReactiveEntityStore
import org.greenrobot.eventbus.Subscribe
import org.parceler.Parcels
import retrofit2.HttpException
import java.util.*
import javax.inject.Inject

@AutoInjector(NextcloudTalkApplication::class)
class MainActivity : BaseActivity(), ActionBarProvider {

    @BindView(R.id.toolbar)
    lateinit var toolbar: MaterialToolbar
    @BindView(R.id.controller_container)
    lateinit var container: ViewGroup

    @Inject
    lateinit var userUtils: UserUtils
    @Inject
    lateinit var dataStore: ReactiveEntityStore<Persistable>
    @Inject
    lateinit var sqlCipherDatabaseSource: SqlCipherDatabaseSource

    /*@Inject
    internal var ncApi: NcApi? = null
    */
    @Inject
    lateinit var ncApi: NcApi

    private var roomsQueryDisposable: Disposable? = null
    private var adapter: FlexibleAdapter<AbstractFlexibleItem<*>>? = null
    private var isRefreshing: Boolean = false
    private var router: Router? = null
    var isInRoot=false;
    private var credentials: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        NextcloudTalkApplication.sharedApplication!!.componentApplication.inject(this)
        ButterKnife.bind(this)

        setSupportActionBar(toolbar)

        router = Conductor.attachRouter(this, container, savedInstanceState)

        var hasDb = true

        try {
            sqlCipherDatabaseSource.writableDatabase
        } catch (exception: Exception) {
            hasDb = false
        }

        if (intent.hasExtra(BundleKeys.KEY_FROM_NOTIFICATION_START_CALL)) {
            if (!router!!.hasRootController()) {
                router!!.setRoot(RouterTransaction.with(MeetingsPagerController())
                        .pushChangeHandler(HorizontalChangeHandler())
                        .popChangeHandler(HorizontalChangeHandler()))
            }
            onNewIntent(intent)
        } else if (!router!!.hasRootController()) {
            if (hasDb) {
                if (userUtils.anyUserExists()) {
                    router!!.setRoot(RouterTransaction.with(MeetingsPagerController())
                            .pushChangeHandler(HorizontalChangeHandler())
                            .popChangeHandler(HorizontalChangeHandler()))
                } else {
                    router!!.setRoot(RouterTransaction.with(ServerSelectionController())
                            .pushChangeHandler(HorizontalChangeHandler())
                            .popChangeHandler(HorizontalChangeHandler()))
                }
            } else {
                router!!.setRoot(RouterTransaction.with(ServerSelectionController())
                        .pushChangeHandler(HorizontalChangeHandler())
                        .popChangeHandler(HorizontalChangeHandler()))

            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkIfWeAreSecure()
        }
    }

    fun getUserUtilsFromActivity(): UserUtils {
        return userUtils;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    fun checkIfWeAreSecure() {
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (keyguardManager.isKeyguardSecure && appPreferences.isScreenLocked) {
            if (!SecurityUtils.checkIfWeAreAuthenticated(appPreferences.screenLockTimeout)) {
                if (router != null && router!!.getControllerWithTag(LockedController.TAG) == null) {
                    router!!.pushController(RouterTransaction.with(LockedController())
                            .pushChangeHandler(VerticalChangeHandler())
                            .popChangeHandler(VerticalChangeHandler())
                            .tag(LockedController.TAG))
                }
            }
        }
    }


    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        if (intent.hasExtra(BundleKeys.KEY_FROM_NOTIFICATION_START_CALL)) {
            if (intent.getBooleanExtra(BundleKeys.KEY_FROM_NOTIFICATION_START_CALL, false)) {
                router!!.pushController(RouterTransaction.with(CallNotificationController(intent.extras))
                        .pushChangeHandler(HorizontalChangeHandler())
                        .popChangeHandler(HorizontalChangeHandler()))
            } else {
                ConductorRemapping.remapChatController(router!!, intent.getLongExtra(BundleKeys.KEY_INTERNAL_USER_ID, -1),
                        intent.getStringExtra(BundleKeys.KEY_ROOM_TOKEN), intent.extras!!, false)
            }
        }
    }


    override fun onBackPressed() {
        if (router!!.getControllerWithTag(LockedController.TAG) != null) {
            return
        }
        if(router!!.backstackSize>1) {
            router!!.popToRoot()
            isInRoot=true;
        }
        else
        {

            super.onBackPressed()
        /*    if (!router!!.handleBack()) {
                super.onBackPressed()
            } else {

            }*/
        }
    }

    @Subscribe
    fun onMeetinItemClicked(meetingResponse: MeetingItemClickEvent)
    {
        val bundle = Bundle()
        bundle.putParcelable(BundleKeys.KEY_MEETING_DETAILS, meetingResponse.response)

        router?.pushController(RouterTransaction.with(MeetingDetailsPagerController(bundle))
                .pushChangeHandler(HorizontalChangeHandler())
                .popChangeHandler(HorizontalChangeHandler()))
    }

    @Subscribe
    fun onJoinMeetinItemClicked(meetingResponse: MeetingItemJoinMeetingClickEvent)
    {
        fetchData(meetingResponse.response)
    }

    companion object {
        private val TAG = "MainActivity"
    }

    private fun fetchData(meetingResponse: MeetingsReponse) {
        dispose(null)

        isRefreshing = true
         var currentUser: UserEntity? = userUtils.currentUser
        credentials = ApiUtils.getCredentials(currentUser?.getUsername(), currentUser?.getToken())

        var retrofitBucket = ApiUtils.getRetrofitBucketForLandingPage(currentUser?.getBaseUrl(), currentUser?.email, meetingResponse.modpin, meetingResponse.meetingid.toString())


        roomsQueryDisposable = ncApi?.getLandingPage( retrofitBucket.url, currentUser?.email,meetingResponse.modpin)?.subscribeOn(Schedulers.io())
                ?.observeOn(AndroidSchedulers.mainThread())?.subscribe({ landingPageResponse ->

                    if(landingPageResponse!=null)
                    {
                        if (meetingResponse != null ) {


                            if(landingPageResponse.landingPageOcs.data.meetingSession!=null) {
                                PreferenceHelper.setSharedPreferenceString(NextcloudTalkApplication.sharedApplication!!.applicationContext, "COOKIE", landingPageResponse.landingPageOcs.data.meetingSession)
                            }
                            val conversation: Conversation = Conversation()
                            val bundle = Bundle()
                            bundle.putParcelable(BundleKeys.KEY_USER_ENTITY, currentUser)
                            bundle.putString(BundleKeys.KEY_ROOM_TOKEN, meetingResponse.meetingid.toString())
                            bundle.putString(BundleKeys.KEY_ROOM_ID, meetingResponse.modpin)


                                currentUser = userUtils.currentUser


                                    bundle.putParcelable(BundleKeys.KEY_ACTIVE_CONVERSATION, Parcels.wrap(conversation))


                            /*this!!.router?.let {
                                ConductorRemapping.remapChatController(it, userUtils.currentUser.getId(),
                                        meetingResponse.meetingid.toString(), bundle, false)*/
                            }

                        }



                    }


        }, { throwable ->

            if (throwable is HttpException) {
                val exception = throwable as HttpException

            }

            dispose(roomsQueryDisposable)
        }, {
            dispose(roomsQueryDisposable)

            isRefreshing = false
        })

    }

    private fun dispose(disposable: Disposable?) {
        var disposable = disposable
        if (disposable != null && !disposable.isDisposed) {
            disposable.dispose()
            disposable = null
        } else if (disposable == null &&
                roomsQueryDisposable != null && !roomsQueryDisposable!!.isDisposed()) {
            roomsQueryDisposable!!.dispose()
            roomsQueryDisposable = null

        }
    }


}
