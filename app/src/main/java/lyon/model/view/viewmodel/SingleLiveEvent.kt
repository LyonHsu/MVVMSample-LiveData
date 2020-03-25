package lyon.model.view.viewmodel

import android.util.Log
import androidx.annotation.MainThread
import androidx.annotation.Nullable
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import java.util.concurrent.atomic.AtomicBoolean


/**
 * {@link LiveData} which publicly exposes {@link #setValue(T)} and {@link #postValue(T)} method.
 *
 * @param <T> The type of data hold by this instance
 *
 * SingleLiveEvent只會發送更新的value，
 * 原value若已經發送過就不會再次發送，
 * 即避免了configuration change後又顯示一次同樣內容的問題。
 * 因此對於提示訊息、畫面跳轉等動作就很適合用SingleLiveEvent來處理，
 * 使用方式跟MutableLiveData一樣。
 */
@SuppressWarnings("WeakerAccess")
class SingleLiveEvent<T> : MutableLiveData<T>(){
    private val TAG = "SingleLiveEvent"

    private val mPending: AtomicBoolean = AtomicBoolean(false)


    override fun observe(owner: LifecycleOwner, observer: Observer<in T>) {
        if (hasActiveObservers()) {
            Log.w(TAG, "Multiple observers registered but only one will be notified of changes.")
        }

        // Observe the internal MutableLiveData
        super.observe(owner!!, object : Observer<T?> {
            override fun onChanged(@Nullable t: T?) {
                if (mPending.compareAndSet(true, false)) {
                    observer.onChanged(t)
                }
            }
        })
    }


    @MainThread
    override fun setValue(@Nullable t: T?) {
        mPending.set(true)
        super.setValue(t)
    }

    /**
     * Used for cases where T is Void, to make calls cleaner.
     */
    @MainThread
    fun call() {
        value = null
    }
}