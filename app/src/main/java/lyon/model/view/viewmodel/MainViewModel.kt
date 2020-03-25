package lyon.model.view.viewmodel

import android.app.Application
import android.content.Context
import androidx.databinding.ObservableBoolean
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import lyon.model.view.viewmodel.DataModel.onDataReadyCallback


class MainViewModel(application: Application) : AndroidViewModel(application) {
    /**
     * 使用ViewModel有一點需特別注意的是不要儲存Activity/Fragment的內容或context在ViewModel中，
     * 因為configuration changes時當前的Activity及其內容會被destroy，
     * 就會變成存放被destroy的內容在ViewModel中而產生memory leak。
     * 若需要在ViewModel中使用Context的話可以改成使用AndroidViewModel，
     * 其constructor帶有application供我們取得context
     */
    private var mContext: Context= application.getApplicationContext()
    private val dataModel = DataModel(mContext)

    /**
     * 將mData ObservableField<String> 改為 LiveData，
     * LiveData最強大的地方在於lifecycle-aware特性，
     * 當LiveData的value發生改變時，若View在前景便會直接發送，
     * 而View在背景的話，value將會被保留(hold)住，直到回到前景時才發送。
     * 此外，當View被destroy時，LiveData也會自動停止observe行為，
     * 避免造成memory-leak。
     */
    //    var mData = ObservableField<String>()
    var mData = MutableLiveData<String>() //MutableLiveData
    val toastText = SingleLiveEvent<String>() //SingleLiveEvent
    /**
     * 加入ObservableBoolean控制ProgressBar的顯示與否，
     * 在refresh()中依照情況用set()更新它們的value，
     */
    var isLoading = ObservableBoolean(false)

    var onFreshCallBack:OnFreshCallBack ?= null

    fun refresh() {
        isLoading.set(true);
        dataModel.retrieveData(object : onDataReadyCallback {
            override fun onDataReady(data: String?) {
//                mData.set(data);
                mData.value=data
                isLoading.set(false);
                toastText.setValue("下載完成");
                if(onFreshCallBack!=null)
                    onFreshCallBack!!.OnFresh(data)

            }
        })
    }

    fun OnFreshCallBack(onFreshCallBack: OnFreshCallBack){
        this.onFreshCallBack=onFreshCallBack
    }

    interface OnFreshCallBack{
        fun OnFresh(data: String?)
    }
}