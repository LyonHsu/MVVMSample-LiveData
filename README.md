# MVVMSample
一個MVVM+Data Binding的架構

https://ithelp.ithome.com.tw/articles/10192829

在各種程式語言和架構中，關注點分離(Separation of Concerns)一直都是非常重要的原則，而Android原本的架構在這方面就做的不是很好。原架構雖然說是MVC，但顯示UI及邏輯處理都是在Activity/Fragment中，變成V跟C混在一起而難以測試維護。

因應這情況，開發者們陸續發展出MVP和MVVM這兩個主流架構，兩者沒有優劣之分，我個人覺得MVVM的概念和之後要實作的Architecture Components很像，所以系列文章就從建立一個最簡單的MVVM開始，再陸續加入其他功能和library。

MVVM架構
MVVM是Model-View-ViewModel的簡稱，三者扮演的角色為：

    Model：管理資料來源如API和本地資料庫
    View：顯示UI和接收使用者動作
    ViewModel：從Model取得View所需的資料
...
    https://ithelp.ithome.com.tw/upload/images/20171222/20103849tCKSYwze3T.png
　　　　　　　　　　　Model-View-ViewModel class structure

View是Activity、Fragment或custom view，本身不做邏輯處理，當使用者跟UI有互動時將指令傳給ViewModel處理，透過其獲得所需的資料並顯示。

ViewModel接收View的指令並對Model請求資料，將取得的資料保存起來供View使用。

Model管理所有的資料來源，例如API、資料庫和SharedPreference，當ViewModel來請求資料時從正確的來源取得資料並回傳。

Code Sample
我們直接做一個最簡單的MVVM範例，功能只要「使用者按下按鈕時，更新畫面上的文字」。

從Model開始，建立一個class DataModel，返回一個字串資料，最簡單的寫法是這樣：

    public class DataModel {

        public String retrieveData() {
            return "New Data";
        }
    }
不過通常取得資料都是非同步(async)的，所以我們模擬一下：

    public class DataModel {
        public void retrieveData(final onDataReadyCallback callback) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    callback.onDataReady("New Data");
                }
            }, 1500);
        }
        interface onDataReadyCallback {
            void onDataReady(String data);
        }
    }
我們新增一個interface onDataReadyCallback，並用Handler做個1.5秒的延遲模擬從API請求資料的情況，當取得資料時透過onDataReady將資料返回。

接著是ViewModel，功能很簡單就是呼叫Model的取得資料method就好

    public class MainViewModel {
        private DataModel dataModel = new DataModel();
        public void refresh() {
            dataModel.retrieveData(new DataModel.onDataReadyCallback() {
                @Override
                public void onDataReady(String data) {
                    // TODO: exposes data to View
                }
            });
        }
    }
此時就是MVVM和MVP最不同的地方，ViewModel並不使用callback的方式來通知View，而是用Observer pattern的概念，由View來訂閱(subscribe)ViewModel中它要的資料，並在資料異動時才更新UI，因此，MVVM都會搭配如Data Binding等library來實現Observer pattern。

在加入Data Binding之前，先看看View的原樣，即我們的Activity：

    <?xml version="1.0" encoding="utf-8"?>
    <android.support.constraint.ConstraintLayout
        xmlns:android="http://schemas.android.com/apk/res/android"
        xmlns:app="http://schemas.android.com/apk/res-auto"
        xmlns:tools="http://schemas.android.com/tools"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        tools:context="ivankuo.com.itbon2018.MainActivity">

        <Button
            android:id="@+id/btnRefresh"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Refresh"
            app:layout_constraintLeft_toLeftOf="parent"
            app:layout_constraintTop_toTopOf="parent"/>

        <ProgressBar
            android:id="@+id/progressBar"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            app:layout_constraintBottom_toBottomOf="parent"
            app:layout_constraintLeft_toLeftOf="parent"
            app:layout_constraintRight_toRightOf="parent"
            app:layout_constraintTop_toTopOf="parent" />

        <TextView
            android:id="@+id/txtHelloWord"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Hello World!"
            app:layout_constraintBottom_toBottomOf="parent"
            app:layout_constraintLeft_toLeftOf="parent"
            app:layout_constraintRight_toRightOf="parent"
            app:layout_constraintTop_toTopOf="parent" />

    </android.support.constraint.ConstraintLayout>
畫面有一個Button用來觸發資料更新，ProgressBar用來表示讀取中和TextView顯示更新後的資料。

    public class MainActivity extends AppCompatActivity {

        private Button btnRefresh;

        private MainViewModel viewModel = new MainViewModel();

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.main_activity);

            btnRefresh = findViewById(R.id.btnRefresh);
            btnRefresh.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    viewModel.refresh();
                }
            });

            // TODO: 使用Data Binding訂閱ViewModel中的資料以更新畫面
        }
    }
按下Button時會呼叫ViewModel更新資料，接著須完成的就是用Data Binding讓View的資料能自動更新。

Data Binding
Data Bindin是官方提供的library，專門用來處理View的更新。只要在module gradle中加入下面這段就可以啟用Data Binding：

    android {
        ....
        dataBinding {
            enabled = true
        }
    }
記得按Sync Now讓專案重新build，啟用之後修改一下main_activity.xml，在最外層用<layout>將內容包起來：

    <?xml version="1.0" encoding="utf-8"?>
    <layout xmlns:android="http://schemas.android.com/apk/res/android"
        xmlns:app="http://schemas.android.com/apk/res-auto"
        xmlns:tools="http://schemas.android.com/tools">

        <android.support.constraint.ConstraintLayout
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            tools:context="ivankuo.com.itbon2018.MainActivity">

            <Button
                android:id="@+id/btnRefresh"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Refresh"
                app:layout_constraintLeft_toLeftOf="parent"
                app:layout_constraintTop_toTopOf="parent" />

            <ProgressBar
                android:id="@+id/progressBar"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                app:layout_constraintBottom_toBottomOf="parent"
                app:layout_constraintLeft_toLeftOf="parent"
                app:layout_constraintRight_toRightOf="parent"
                app:layout_constraintTop_toTopOf="parent" />

            <TextView
                android:id="@+id/txtHelloWord"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Hello World!"
                app:layout_constraintBottom_toBottomOf="parent"
                app:layout_constraintLeft_toLeftOf="parent"
                app:layout_constraintRight_toRightOf="parent"
                app:layout_constraintTop_toTopOf="parent" />

        </android.support.constraint.ConstraintLayout>

    </layout>
接著用Android Studio的Build -> Make Project或快速鍵Ctrl+F9重新build專案，系統會依照xml的檔名自動產生Binding所需的class，例如xml名稱為main_activity.xml，系統會產生MainActivityBinding供我們使用，規則是首字大寫、去除底線並令底線後的首字大寫。



修改MainActivity：

    public class MainActivity extends AppCompatActivity {

        private MainActivityBinding binding;

        private MainViewModel viewModel = new MainViewModel();

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            binding = DataBindingUtil.setContentView(this, R.layout.main_activity);

            binding.btnRefresh.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    viewModel.refresh();
                }
            });

            // TODO: 使用Data Binding訂閱ViewModel中的資料以更新畫面
        }
    }
原本setContentView需改成用DataBindingUtil來完成，透過binding，我們可以直接呼叫xml中的元件，如上例用binding.btnRefresh就可以控制該按鈕。

這樣的方式相較ButterKnife更為方便，新增元件時ButterKnife要在程式裡用@Bind綁定，而Data Binding可以直接用binding.就取得元件。刪除元件時更是如此，ButterKnife如果xml中的元件刪掉但忘了刪程式裡的@Bind就會發生Runtime exception，Data Binding則會在compile時就報錯。

接著是最後步驟，用Data Binding中的Observable來讓View和ViewModel溝通。

先在MainViewModel新增我們要的欄位：一個String保存資料，以及一個boolean表示目前是否在讀取資料中以顯示ProgressBar

    public class MainViewModel {

        public final ObservableField<String> mData = new ObservableField<>();

        public final ObservableBoolean isLoading = new ObservableBoolean(false);

        private DataModel dataModel = new DataModel();

        public void refresh() {

            isLoading.set(true);

            dataModel.retrieveData(new DataModel.onDataReadyCallback() {
                @Override
                public void onDataReady(String data) {
                    mData.set(data);
                    isLoading.set(false);
                }
            });
        }
    }
加入ObservableField用來存放資料，以及ObservableBoolean控制ProgressBar的顯示與否，在refresh()中依照情況用set()更新它們的value，記得要用public才可以被別人訂閱。

接著要讓MainActivity能觀察到MainViewModel中數值的改變，在main_activity.xml中加入<data>

    <layout xmlns:android="http://schemas.android.com/apk/res/android"
        xmlns:app="http://schemas.android.com/apk/res-auto"
        xmlns:tools="http://schemas.android.com/tools">

        <data>

            <variable
                name="viewModel"
                type="ivankuo.com.itbon2018.MainViewModel" />

        </data>

        <android.support.constraint.ConstraintLayout
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            tools:context="ivankuo.com.itbon2018.MainActivity">

            ...

        </android.support.constraint.ConstraintLayout>

    </layout>
在<data>中加入了MainViewModel因為我們的Oberverble變數都在其中，接著就可以開始寫元件和那兩個變數的互動：

    <?xml version="1.0" encoding="utf-8"?>
    <layout xmlns:android="http://schemas.android.com/apk/res/android"
        xmlns:app="http://schemas.android.com/apk/res-auto"
        xmlns:tools="http://schemas.android.com/tools">
        ...

            <Button
                android:id="@+id/btnRefresh"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Refresh"
                android:enabled="@{viewModel.isLoading ? false : true}"
                ... />

            <ProgressBar
                android:id="@+id/progressBar"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:visibility="@{viewModel.isLoading ? View.VISIBLE : View.GONE}"
                ... />

            <TextView
                android:id="@+id/txtHelloWord"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="@{viewModel.mData}"
                ... />

        </android.support.constraint.ConstraintLayout>

    </layout>
依照MainViewModel中的isLoading欄位決定Button的enable及ProgressBar是否顯示，而TextView的文字就放MainViewModel取得的data。

因為用到View.VISIBLE的關係，須在<data>中import不然會報錯

    <data>

        <import type="android.view.View"/>

        <variable
            name="viewModel"
            type="ivankuo.com.itbon2018.MainViewModel" />

    </data>
最後，在Activity中加上setViewModel

    @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            binding = DataBindingUtil.setContentView(this, R.layout.main_activity);

            binding.setViewModel(viewModel);

            binding.btnRefresh.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    viewModel.refresh();
                }
            });
        }
setViewModel這個method也是由Data Binding自動產生的，因為在<data>中我們變數名為viewModel。此外Data Binding也可以綁定事件，例如onClick

    <Button
        android:id="@+id/btnRefresh"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Refresh"
        android:enabled="@{viewModel.isLoading ? false : true}"
        android:onClick="@{() -> viewModel.refresh()}"
        ... />
這樣的話Activity中的setOnClickListener也不用寫了

    public class MainActivity extends AppCompatActivity {

        ...
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            binding = DataBindingUtil.setContentView(this, R.layout.main_activity);
            binding.setViewModel(viewModel);
        }
    }
這樣就是一個MVVM+Data Binding的架構了，View負責顯示UI，並用Data Binding和ViewModel連結；ViewModel只要用set()處理本身的Observerble欄位，不需要知道是誰在取用這些資料；Model也只提供method讓外部請求資料，這部分將來建立起api和資料庫時會比較清楚。
