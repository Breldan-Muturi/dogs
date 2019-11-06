package turi.d.dogs.viewmodel

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.launch
import turi.d.dogs.model.DogBreed
import turi.d.dogs.model.DogDatabase
import turi.d.dogs.model.DogsApiService
import turi.d.dogs.util.NotificationsHelper
import turi.d.dogs.util.SharedPreferencesHelper
import java.lang.NumberFormatException

class ListViewModel(application: Application) : BaseViewModel(application) {
    private var prefHelper = SharedPreferencesHelper(getApplication())
    private var refreshTime = 5 * 60 * 1000 * 1000 * 1000L
    private val dogsService = DogsApiService()
    private val disposable = CompositeDisposable()

    val dogs = MutableLiveData<List<DogBreed>>()
    val dogsLoadError = MutableLiveData<Boolean>()
    val loading = MutableLiveData<Boolean>()

    fun refresh() {
        checkCacheDuration()
        val updateTime = prefHelper.getUpdateTime()
        if (updateTime != null && updateTime != 0L && System.nanoTime() - updateTime < refreshTime) {
            fetchFromDatabase()
        } else {
            fetchFromRemote()
        }
    }

    private fun checkCacheDuration(){
        val cachePreference = prefHelper.getCacheDuration()
        try {
            val cachePreferenceInt = cachePreference?.toInt() ?: 5 * 60
            refreshTime = cachePreferenceInt.times( 1000 * 1000 * 1000L)
        } catch (e: NumberFormatException){
            e.printStackTrace()
        }
    }
    fun refreshByPassCache(){
        fetchFromRemote()
    }

    private fun fetchFromDatabase(){
        loading.value = true
        launch{
            val dogs = DogDatabase(getApplication()).dogDao().getAllDogs()
            dogsRetrieved(dogs)
            Toast.makeText(getApplication(),"Dogs from our Database", Toast.LENGTH_LONG).show()
        }
    }

    private fun fetchFromRemote() {
        loading.value = true
        disposable.add(
            dogsService.getDogs()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableSingleObserver<List<DogBreed>>() {
                    override fun onSuccess(dogList: List<DogBreed>) {
                        storeDogsLocally(dogList)
                        Toast.makeText(getApplication(),"Dogs from our API  Endpoint", Toast.LENGTH_LONG).show()
                        NotificationsHelper(getApplication()).createNotification()
                    }

                    override fun onError(e: Throwable) {
                        dogsLoadError.value = true
                        loading.value = false
                        e.printStackTrace()
                    }
                })
        )
    }

    private fun dogsRetrieved(dogList: List<DogBreed>) {
        dogs.value = dogList
        dogsLoadError.value = false
        loading.value = false
    }

    private fun storeDogsLocally(list: List<DogBreed>) {
        launch {
            val dao = DogDatabase(getApplication()).dogDao()
            dao.deleteAllDogs()
            val result = dao.insertAll(*list.toTypedArray())
            var i = 0
            while (i < list.size) {
                list[i].uuid = result[i].toInt()
                ++i
            }
            dogsRetrieved(list)
        }
        prefHelper.saveUpdateTime(System.nanoTime())
    }

    override fun onCleared() {
        super.onCleared()
        disposable.clear()
    }
}