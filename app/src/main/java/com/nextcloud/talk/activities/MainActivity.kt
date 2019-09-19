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
import android.widget.Toast
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
import com.kennyc.bottomsheet.BottomSheet
import com.nextcloud.talk.R
import com.nextcloud.talk.api.NcApi
import com.nextcloud.talk.application.NextcloudTalkApplication
import com.nextcloud.talk.controllers.*
import com.nextcloud.talk.controllers.base.providers.ActionBarProvider
import com.nextcloud.talk.controllers.bottomsheet.CallMenuController
import com.nextcloud.talk.controllers.bottomsheet.EntryMenuController
import com.nextcloud.talk.events.MeetingItemClickEvent
import com.nextcloud.talk.events.MeetingItemJoinMeetingClickEvent
import com.nextcloud.talk.interfaces.ConversationMenuInterface
import com.nextcloud.talk.models.database.UserEntity
import com.nextcloud.talk.models.json.conversations.Conversation
import com.nextcloud.talk.models.json.conversations.RoomOverall
import com.nextcloud.talk.models.json.landingpage.LandingPageResponse
import com.nextcloud.talk.models.json.meetings.MeetingsReponse
import com.nextcloud.talk.models.json.participants.Participant
import com.nextcloud.talk.utils.*
import com.nextcloud.talk.utils.bundle.BundleKeys
import com.nextcloud.talk.utils.database.user.UserUtils
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.requery.Persistable
import io.requery.android.sqlcipher.SqlCipherDatabaseSource
import io.requery.reactivex.ReactiveEntityStore
import org.greenrobot.eventbus.Subscribe
import org.parceler.Parcels
import retrofit2.HttpException
import java.util.ArrayList
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
    private var bottomSheet: BottomSheet? = null
    /*@Inject
    internal var ncApi: NcApi? = null
    */
    @Inject
    lateinit var ncApi: NcApi

    private var roomsQueryDisposable: Disposable? = null
    private val disposableList = ArrayList<Disposable>()
    private var adapter: FlexibleAdapter<AbstractFlexibleItem<*>>? = null
    private var isRefreshing: Boolean = false
    private var checkingLobbyStatus: Boolean = false
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
        /*val bundle = Bundle()
        bundle.putParcelable(BundleKeys.KEY_USER_ENTITY, userUtils.currentUser)
        bundle.putString(BundleKeys.KEY_ROOM_TOKEN, meetingResponse.response.meetingid.toString())
        bundle.putString(BundleKeys.KEY_ROOM_ID, meetingResponse.response.modpin)
        bundle.putParcelable(BundleKeys.KEY_ACTIVE_CONVERSATION, Parcels.wrap(Conversation()))
//        bundle.putParcelable(BundleKeys.KEY_LANDING_PAGE_RESPONSE, Parcels.wrap(landingPageResponse.landingPageOcs.data))
        PreferenceHelper.setSharedPreferenceString(this,"MEETING","");
        PreferenceHelper.setSharedPreferenceString(this,"HOST","");
        this!!.router?.let {
            ConductorRemapping.remapChatController(it, userUtils.currentUser.getId(),
                    meetingResponse.response.meetingid.toString(), bundle, false)
        }*/
        PreferenceHelper.setSharedPreferenceString(this,"MEETING","");
        PreferenceHelper.setSharedPreferenceString(this,"HOST","");
        if(meetingResponse.response.isOwner)
            fetchDataForHost(meetingResponse.response)
        else
            fetchData(meetingResponse.response)
    }

    companion object {
        private val TAG = "MainActivity"
    }

    private fun fetchDataForHost(meetingResponse: MeetingsReponse) {
        dispose(null)

        isRefreshing = true
         var currentUser: UserEntity? = userUtils.currentUser
        credentials = ApiUtils.getCredentials(currentUser?.getUsername(), currentUser?.getToken())

        var retrofitBucket = ApiUtils.getRetrofitBucketForLandingPage(currentUser?.getBaseUrl(), currentUser?.email, meetingResponse.modpin, meetingResponse.meetingid.toString())


        roomsQueryDisposable = ncApi?.getLandingPage( retrofitBucket.url, retrofitBucket.queryMap)?.subscribeOn(Schedulers.io())
                ?.observeOn(AndroidSchedulers.mainThread())?.subscribe({ landingPageResponse ->

                    if(landingPageResponse!=null)
                    {
                        if (meetingResponse != null ) {
                            if(landingPageResponse.landingPageOcs.data.meetingSession!=null) {
                                PreferenceHelper.setSharedPreferenceString(NextcloudTalkApplication.sharedApplication!!.applicationContext, "MEETING", landingPageResponse.landingPageOcs.data.meetingSession)
                            }
                            if(landingPageResponse.landingPageOcs.data.hostSession!=null)
                            {
                                PreferenceHelper.setSharedPreferenceString(NextcloudTalkApplication.sharedApplication!!.applicationContext, "HOST", landingPageResponse.landingPageOcs.data.hostSession)
                            }
                            val conversation: Conversation = Conversation()

                                getRoomInfo(landingPageResponse,userUtils.currentUser,meetingResponse);

                            }
                    }


        }, { throwable ->

            if (throwable is HttpException) {
                val exception = throwable as HttpException
                Toast.makeText(this,getString(R.string.err_landingpage),Toast.LENGTH_LONG).show()
            }

            dispose(roomsQueryDisposable)
        }, {
            dispose(roomsQueryDisposable)

            isRefreshing = false
        })

    }

    private fun fetchData(meetingResponse: MeetingsReponse) {
        dispose(null)

        isRefreshing = true
        var currentUser: UserEntity? = userUtils.currentUser
        credentials = ApiUtils.getCredentials(currentUser?.getUsername(), currentUser?.getToken())

        var retrofitBucket = ApiUtils.getRetrofitBucketForLandingPage(currentUser?.getBaseUrl(), currentUser?.email, meetingResponse.modpin, meetingResponse.meetingid.toString())


        roomsQueryDisposable = ncApi?.getLandingPagePrivate(credentials, retrofitBucket.url)?.subscribeOn(Schedulers.io())
                ?.observeOn(AndroidSchedulers.mainThread())?.subscribe({ landingPageResponse ->

                    if(landingPageResponse!=null)
                    {
                        if (meetingResponse != null ) {
                            if(landingPageResponse.landingPageOcs.data.meetingSession!=null) {
                                PreferenceHelper.setSharedPreferenceString(NextcloudTalkApplication.sharedApplication!!.applicationContext, "MEETING", landingPageResponse.landingPageOcs.data.meetingSession)
                            }
                            if(landingPageResponse.landingPageOcs.data.hostSession!=null)
                            {
                                PreferenceHelper.setSharedPreferenceString(NextcloudTalkApplication.sharedApplication!!.applicationContext, "HOST", landingPageResponse.landingPageOcs.data.hostSession)
                            }
                            val conversation: Conversation = Conversation()

                            getRoomInfo(landingPageResponse,userUtils.currentUser,meetingResponse);
                            /*this!!.router?.let {
                                ConductorRemapping.remapChatController(it, userUtils.currentUser.getId(),
                                        meetingResponse.meetingid.toString(), bundle, false)*/
                        }
                    }


                }, { throwable ->

                    if (throwable is HttpException) {
                        val exception = throwable as HttpException
                        Toast.makeText(this@MainActivity,getString(R.string.err_landingpage),Toast.LENGTH_LONG).show()
                    }

                    dispose(roomsQueryDisposable)
                }, {
                    dispose(roomsQueryDisposable)

                    isRefreshing = false
                })

    }


    private fun getRoomInfo(landingPageResponse: LandingPageResponse, currentUser: UserEntity, meetingResponse: MeetingsReponse) {
        val shouldRepeat = currentUser.hasSpreedFeatureCapability("webinary-lobby")
        if (shouldRepeat) {
            checkingLobbyStatus = true
        }

        ncApi.getRoom(credentials, ApiUtils.getRoom(currentUser.getBaseUrl(),meetingResponse.meetingid.toString() ))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : Observer<RoomOverall> {
                    override fun onSubscribe(d: Disposable) {
                        disposableList.add(d)
                    }

                    override fun onNext(roomOverall: RoomOverall) {
                        var currentConversation: Conversation? = null
                        currentConversation = roomOverall.ocs.data
                        val bundle = Bundle()
                        bundle.putParcelable(BundleKeys.KEY_USER_ENTITY, currentUser)
                        bundle.putString(BundleKeys.KEY_ROOM_TOKEN, meetingResponse.meetingid.toString())
                        bundle.putString(BundleKeys.KEY_ROOM_ID, currentConversation.roomId)
                        bundle.putParcelable(BundleKeys.KEY_ACTIVE_CONVERSATION, Parcels.wrap(currentConversation))
                        bundle.putParcelable(BundleKeys.KEY_LANDING_PAGE_RESPONSE, Parcels.wrap(landingPageResponse.landingPageOcs.data))

                        /*if(landingPageResponse.landingPageOcs.data.scheduledNow)
                        {*/
                            if (currentConversation.hasPassword && (currentConversation.participantType == Participant.ParticipantType.GUEST || currentConversation.participantType == Participant.ParticipantType.USER_FOLLOWING_LINK)) {
                                bundle.putInt(BundleKeys.KEY_OPERATION_CODE, 99)
                                prepareAndShowBottomSheetWithBundle(bundle, false)
                            } else {

                                if (currentUser.hasSpreedFeatureCapability("chat-v2")) {
                                    bundle.putParcelable(BundleKeys.KEY_ACTIVE_CONVERSATION, Parcels.wrap<Conversation>(currentConversation))
                                    router?.let {
                                        ConductorRemapping.remapChatController(it, currentUser.id,
                                                currentConversation.token, bundle, false)
                                    }
                                } else {
                                    /*overridePushHandler(NoOpControllerChangeHandler())
                                    overridePopHandler(NoOpControllerChangeHandler())*/
                                    val callIntent = Intent(this@MainActivity, MagicCallActivity::class.java)
                                    callIntent.putExtras(bundle)
                                    startActivity(callIntent)
                                }
                            }

                        /*}
                        else{
                            Toast.makeText(this@MainActivity,resources.getString(R.string.str_meeting_ended),Toast.LENGTH_LONG).show()
                        }*/
                    }

                    override fun onError(e: Throwable) {
                        Toast.makeText(this@MainActivity,getString(R.string.err_room_info),Toast.LENGTH_LONG).show()
                    }

                    override fun onComplete() {
                    }
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

    private fun prepareAndShowBottomSheetWithBundle(bundle: Bundle, shouldShowCallMenuController: Boolean) {
        var view: View? = null;
        if (view == null) {
            view = this!!.getLayoutInflater().inflate(R.layout.bottom_sheet, null, false)
        }
            router?.setRoot(
                    RouterTransaction.with(EntryMenuController(bundle))
                            .popChangeHandler(VerticalChangeHandler())
                            .pushChangeHandler(VerticalChangeHandler()))
        if (bottomSheet == null) {
            bottomSheet = BottomSheet.Builder(this).setView(view).create()
        }

        bottomSheet?.setOnShowListener({ dialog -> KeyboardUtils(this, bottomSheet!!.getLayout(), true) })
        bottomSheet?.setOnDismissListener({ dialog -> actionBar!!.setDisplayHomeAsUpEnabled(router?.getBackstackSize()!! > 1) })
        bottomSheet?.show()
    }


}
