# MVVMSample
一個MVVM+LiveData的架構

https://ithelp.ithome.com.tw/articles/10193296

試想一種情境：當我們執行下載檔案之類的耗時任務，要在任務完成時發出Toast通知使用者，可以怎麼做？

以目前的程式可以用Data Binding的addOnPropertyChangedCallback來做，將MainActivity修改如下：

    public class MainActivity extends AppCompatActivity {

        ...

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            binding = DataBindingUtil.setContentView(this, R.layout.main_activity);
            ...
            viewModel.mData.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
                @Override
                public void onPropertyChanged(Observable observable, int i) {
                    Toast.makeText(MainActivity.this, "下載完成", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
對MainViewModel中的mData增加callback，當值發生改變時觸發並顯示Toast，執行結果：


但是，如果這個任務耗時更久，當它完成時使用者已經返回桌面做其他操作了呢？將DataModel的delay從1500改成3000來模擬：

離開畫面後Toast還是出現了，導致使用者已經在用其他app卻突然看到我們的Toast訊息，影響其使用體驗。

要解決這個問題，我們需要一種observable可以達到：

當value發生改變時發出callback通知
只有View的生命週期在前景(foreground)時才發出通知
第1點並不太特別，各個observable library都做得到，然而，第2點lifecycle-aware就是今天的主角LiveData才能辦到。

LiveData
如上所述，LiveData最強大的地方在於lifecycle-aware特性，當LiveData的value發生改變時，若View在前景便會直接發送，而View在背景的話，value將會被保留(hold)住，直到回到前景時才發送。此外，當View被destroy時，LiveData也會自動停止observe行為，避免造成memory-leak。

加入dependencies，跟昨天的ViewModel同屬於lifecycle component，如果昨天有加過今天就不用了。

    // ViewModel and LiveData
    implementation "android.arch.lifecycle:extensions:1.0.0"
    annotationProcessor "android.arch.lifecycle:compiler:1.0.0"
修改MainViewModel，將mData改成MutableLiveData

    public class MainViewModel extends ViewModel {

        ...

        public final MutableLiveData<String> mData = new MutableLiveData<>();

        ...

        public void refresh() {

            ...

            dataModel.retrieveData(new DataModel.onDataReadyCallback() {
                @Override
                public void onDataReady(String data) {
                    mData.setValue(data);
                    ...
                }
            });
        }
    }
MutableLiveData是方便我們使用的LiveData子類別，提供setValue()和postValue()兩種方式更新value，差異在於前者是在main thread執行，若需要在background thread則改用後者。

因為mData已經改用LiveData了，所以在main_activity.xml中修改一下TextView，刪掉Data Binding那一行

    <?xml version="1.0" encoding="utf-8"?>
    <layout xmlns:android="http://schemas.android.com/apk/res/android"
        xmlns:app="http://schemas.android.com/apk/res-auto"
        xmlns:tools="http://schemas.android.com/tools">

        <data>

            <import type="android.view.View" />

            <variable
                name="viewModel"
                type="ivankuo.com.itbon2018.MainViewModel" />

        </data>

        <android.support.constraint.ConstraintLayout
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            tools:context="ivankuo.com.itbon2018.MainActivity">

            ...

            <TextView
                android:id="@+id/txtHelloWord"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                app:layout_constraintBottom_toBottomOf="parent"
                ... />

        </android.support.constraint.ConstraintLayout>

    </layout>
  MainActivity中接收mData的callback

    public class MainActivity extends AppCompatActivity {

        ...

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            ...

            viewModel.mData.observe(this, new Observer<String>() {
                @Override
                public void onChanged(@Nullable String data) {
                    binding.txtHelloWord.setText(data);
                    Toast.makeText(MainActivity.this, "下載完成", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
使用observe(owner, Observer)來接收callback，owner用this表示LiveData會遵照MainActivity的生命週期判斷是否發送變更。

將delay縮短至1500並再次執行，就會看到Toast在app回到前景時才顯示：


SingleLiveEvent
上面的程式還有一個問題，當畫面旋轉時，Toast會再出現一次：

因為View在重新create後會立即收到LiveData的value，所以又觸發了一次onChanged()並顯示Toast。

因應這種情況，Google寫了SingleLiveEvent這個class來處理

    public class SingleLiveEvent<T> extends MutableLiveData<T> {

        private static final String TAG = "SingleLiveEvent";

        private final AtomicBoolean mPending = new AtomicBoolean(false);

        @MainThread
        public void observe(LifecycleOwner owner, final Observer<T> observer) {

            if (hasActiveObservers()) {
                Log.w(TAG, "Multiple observers registered but only one will be notified of changes.");
            }

            // Observe the internal MutableLiveData
            super.observe(owner, new Observer<T>() {
                @Override
                public void onChanged(@Nullable T t) {
                    if (mPending.compareAndSet(true, false)) {
                        observer.onChanged(t);
                    }
                }
            });
        }

        @MainThread
        public void setValue(@Nullable T t) {
            mPending.set(true);
            super.setValue(t);
        }

        /**
         * Used for cases where T is Void, to make calls cleaner.
         */
        @MainThread
        public void call() {
            setValue(null);
        }
    }
SingleLiveEvent只會發送更新的value，原value若已經發送過就不會再次發送，即避免了configuration change後又顯示一次同樣內容的問題。因此對於提示訊息、畫面跳轉等動作就很適合用SingleLiveEvent來處理，使用方式跟MutableLiveData一樣。

我們修改ViewModel將提示訊息改用SingleLiveEvent處理

    public class MainViewModel extends ViewModel {

        ...

        public final MutableLiveData<String> mData = new MutableLiveData<>();

        public final SingleLiveEvent<String> toastText = new SingleLiveEvent<>();

        ...

        public void refresh() {

            isLoading.set(true);

            dataModel.retrieveData(new DataModel.onDataReadyCallback() {
                @Override
                public void onDataReady(String data) {
                    mData.setValue(data);
                    toastText.setValue("下載完成");
                    isLoading.set(false);
                }
            });
        }
    }
修改MainActivity：

    public class MainActivity extends AppCompatActivity {

        ...

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            ...
            viewModel.mData.observe(this, new Observer<String>() {
                @Override
                public void onChanged(@Nullable String data) {
                    binding.txtHelloWord.setText(data);
                }
            });

            viewModel.toastText.observe(this, new Observer<String>() {
                @Override
                public void onChanged(@Nullable String text) {
                    Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
將主要資料mData和toast分開，當首次載入資料時兩者都會觸發，在configuration change發生之後，mData會立即觸發讓畫面上顯示資料，而toastText因為value並沒有透過setValue()更新過，所以不會再次觸發。

執行結果：


LiveData跟Data Binding角色有一點重疊，一般而言我會讓Data Binding處理元件的visible這類屬性，而主要顯示在UI的資料用LiveData以免除各種lifecycle衍生的問題。

Google也正在修改Data Binding library讓它也具有lifecycle-aware的特性，有興趣的可以留意#issue34
