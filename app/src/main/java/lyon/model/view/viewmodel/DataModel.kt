package lyon.model.view.viewmodel

import android.content.Context
import android.os.Handler

class DataModel(val context: Context) {

    fun retrieveData(callback:onDataReadyCallback){
        Handler().postDelayed(Runnable { callback.onDataReady(context.getString(R.string.app_name)) }, 1500)
    }

    interface onDataReadyCallback {
        fun onDataReady(data: String?)
    }
}