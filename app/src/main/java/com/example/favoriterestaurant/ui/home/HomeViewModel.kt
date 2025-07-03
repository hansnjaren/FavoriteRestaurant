package com.example.favoriterestaurant.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HomeViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is home Fragment"
    }
    private val _text2 = MutableLiveData<String>().apply {
        value = "Hello, World!"
    }
    val text: LiveData<String> = _text
    val text2: LiveData<String> = _text2
}