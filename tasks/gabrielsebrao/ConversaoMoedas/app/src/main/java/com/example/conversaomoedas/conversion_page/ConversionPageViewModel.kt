package com.example.conversaomoedas.conversion_page

import android.content.res.Resources
import android.util.Log
import android.widget.Button
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.conversaomoedas.classes.CurrencyApi
import com.example.conversaomoedas.classes.CurrencyEnum
import com.example.conversaomoedas.classes.CurrencyJsonItems
import com.example.conversaomoedas.classes.RetrofitInstance
import com.example.conversaomoedasapi.R
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.concurrent.TimeUnit


class ConversionPageViewModel: ViewModel() {

    private lateinit var initialCurrency: CurrencyEnum
    private lateinit var finalCurrency: CurrencyEnum

    var convertedValue: Double = 0.0
    var finalValue: Double = 0.0
    var isLoading = MutableLiveData(true)
    var hasError = MutableLiveData(false)
    var conversionSuccess = MutableLiveData(false)
    private var disposable: Disposable? = null

    lateinit var resources: Resources

    fun currencies(initialCurrency: CurrencyEnum, finalCurrency: CurrencyEnum): ConversionPageViewModel = apply {
        this.initialCurrency = initialCurrency
        this.finalCurrency = finalCurrency
    }

    fun resources(resources: Resources): ConversionPageViewModel = apply {
        this.resources = resources
    }

    fun convertValues(button: Button): Disposable? {

        val initialCurrencyObservable = getApiSingle(initialCurrency.getCode(resources))
            .subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
            .delay(10, TimeUnit.SECONDS)
        // DELAY APLICADO DE 30 SEGUNDOS PARA DETECTAR O MEMORY LEAK

        val finalCurrencyObservable = getApiSingle(finalCurrency.getCode(resources))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())

        disposable = Single.zip(initialCurrencyObservable, finalCurrencyObservable) { initialValue, finalValue ->

            initialCurrency.valueToReal = if(initialValue.isEmpty()) 1.0 else initialValue[0].bid.toDoubleOrNull() ?: 0.0
            finalCurrency.valueToReal = if(finalValue.isEmpty()) 1.0 else finalValue[0].bid.toDoubleOrNull() ?: 0.0

            Log.e("RX_DEBUG (OBSERVER)", "disposable is disposed: ${disposable?.isDisposed}, disposable location: ${System.identityHashCode(disposable)}")

        }.subscribe({

            isLoading.postValue(false)
            conversionSuccess.postValue(true)

            // LEVEI O BOTÃO DA ATIVIDADE PARA A SUBSCRIBE DO RX, ASSIM OCORRERÁ MEMORY LEAK
            button.text = "enfim oi"

            Log.e("RX_DEBUG (ON SUCCESS)", "disposable is disposed: ${disposable?.isDisposed}, disposable location: ${System.identityHashCode(disposable)}")

        }, { error ->

            isLoading.postValue(false)
            hasError.postValue(true)

            Log.e("RX_DEBUG (ON ERROR)", "Error during conversion: ${error.message}")

        })

        return disposable

    }

    fun onSuccess() {

        convertedValue *= initialCurrency.valueToReal
        finalValue = convertedValue / finalCurrency.valueToReal
        //disposable?.dispose()

    }

    private fun getApiSingle(currencyCode: String): Single<List<CurrencyJsonItems>> {

        if (currencyCode == resources.getString(R.string.currency_code_brazilian_real)) return Single.just(listOf())

        return RetrofitInstance.getRetrofitInstance()
            .create(CurrencyApi::class.java)
            .getCurrencies(currencies = currencyCode)

    }

}