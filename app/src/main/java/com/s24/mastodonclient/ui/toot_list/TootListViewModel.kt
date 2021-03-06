package com.s24.mastodonclient.ui.toot_list

import android.app.Application
import androidx.lifecycle.*
import com.s24.mastodonclient.entity.Account
import com.s24.mastodonclient.entity.Toot
import com.s24.mastodonclient.entity.UserCredential
import com.s24.mastodonclient.repository.AccountRepository
import com.s24.mastodonclient.repository.TootRepository
import com.s24.mastodonclient.repository.UserCredentialRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import retrofit2.HttpException

class TootListViewModel(
    private val instanceUrl: String,
    private val username: String,
    private val timelineType: TimelineType,
    private val coroutineScope: CoroutineScope,
    application: Application
) : AndroidViewModel(application), LifecycleObserver {
    private val userCredentialRepository = UserCredentialRepository(
        application
    )
    private lateinit var tootRepository: TootRepository
    private lateinit var accountRepository: AccountRepository
    private lateinit var userCredential: UserCredential

    val isLoading = MutableLiveData<Boolean>()
    val accountInfo = MutableLiveData<Account>()
    var hasNext = true

    val tootList = MutableLiveData<ArrayList<Toot>>()

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun onCreate(){
        coroutineScope.launch {
            userCredential = userCredentialRepository
                .find(instanceUrl, username) ?: return@launch
            tootRepository = TootRepository(userCredential)
            //accountRepository = AccountRepository(userCredential)
            loadNext()
        }
    }

    fun clear() {
        val tootListSnapshot = tootList.value?: return
        tootListSnapshot.clear()
    }
    fun loadNext() {
        coroutineScope.launch {
            //updateAccountInfo()
            isLoading.postValue(true)

            val tootListSnapshot = tootList.value?: ArrayList()
            val maxId = tootListSnapshot.lastOrNull()?.id
            val tootListResponse = when (timelineType){
                TimelineType.PublicTimeline -> {
                    tootRepository.fetchPublicTimeline(
                        maxId = maxId,
                        onlyMedia = true
                    )
                }
                TimelineType.HomeTimeline -> {
                    tootRepository.fetchHomeTimeLine(
                        maxId = maxId
                    )
                }
            }
            tootListSnapshot.addAll(tootListResponse)
            tootList.postValue(tootListSnapshot)

            hasNext = tootListResponse.isNotEmpty()
            isLoading.postValue(false)
        }
    }
    private suspend fun updateAccountInfo(){
        val accountInfoSnapshot = accountInfo.value
            ?: accountRepository.verifyAccountCredential()

        accountInfo.postValue(accountInfoSnapshot)
    }

    fun delete(toot: Toot) {
        coroutineScope.launch {
            tootRepository.delete(toot.id)


            val tootListSnapshot = tootList.value
            tootListSnapshot?.remove(toot)
            tootList.postValue(tootListSnapshot!!)



        }
    }
}