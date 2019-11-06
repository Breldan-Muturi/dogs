package turi.d.dogs.viewmodel

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.launch
import turi.d.dogs.model.DogBreed
import turi.d.dogs.model.DogDatabase

class DetailViewModel(application: Application): BaseViewModel(application) {
    val dogLiveData = MutableLiveData<DogBreed>()
    fun fetch(uuid: Int){
        launch {
            val dog = DogDatabase(getApplication()).dogDao().getDog(uuid)
            dogLiveData.value = dog
        }
    }
}