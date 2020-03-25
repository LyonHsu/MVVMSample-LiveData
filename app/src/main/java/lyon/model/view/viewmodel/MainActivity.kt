package lyon.model.view.viewmodel

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import lyon.model.view.viewmodel.databinding.ActivityMainBinding


/**
 * 一個MVVM+Data Binding的架構，
 * View負責顯示UI，
 * 並用Data Binding和ViewModel連結；
 * ViewModel只要用set()處理本身的Observerble欄位，
 * 不需要知道是誰在取用這些資料；
 * Model也只提供method讓外部請求資料，
 * 這部分將來建立起api和資料庫時會比較清楚。
 */
class MainActivity : AppCompatActivity() {

    /**
     * Data Binding
     * 系統會依照xml的檔名自動產生Binding所需的class，
     * 例如xml名稱為main_activity.xml，
     * 系統會產生MainActivityBinding供我們使用，
     * 規則是首字大寫、去除底線並令底線後的首字大寫。
     */
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel:MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        /**
         * 原本setContentView需改成用DataBindingUtil來完成，
         * 透過binding，
         * 我們可以直接呼叫xml中的元件，
         * 如下用binding.btnRefresh就可以控制該按鈕。
         */
        //setContentView(R.layout.activity_main)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        /**
         * 透過ViewModelProviders協助我們取得ViewModel
         * of()的參數代表著ViewModel的生命範圍(scope)，
         * 在MainActivity中用of(this)表示ViewModel的生命週期會持續到MainActivity不再活動(destroy且沒有re-create)為止
         */
        viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)
        viewModel.OnFreshCallBack(object :MainViewModel.OnFreshCallBack{
            override fun OnFresh(data: String?) {
                binding.txtHelloWord.text = data
            }

        })

        /**
         * 使用observe(owner, Observer)來接收callback，
         * owner用this表示LiveData會遵照MainActivity的生命週期判斷是否發送變更。
         */
        viewModel.mData.observe(this, object : Observer<String?> {
            override fun onChanged(@Nullable data: String?) {
                binding.txtHelloWord.text = data
//                Toast.makeText(this@MainActivity, "下載完成", Toast.LENGTH_SHORT).show()
            }
        })

        /**
         * 將主要資料mData和toast分開，當首次載入資料時兩者都會觸發，
         * 在configuration change發生之後，mData會立即觸發讓畫面上顯示資料，
         * 而toastText因為value並沒有透過setValue()更新過，所以不會再次觸發。
         */
        viewModel.toastText.observe(this, Observer {
                text -> Toast.makeText(this@MainActivity, text, Toast.LENGTH_SHORT).show()
        })

        //將viewModel 放入Data Binding
        /**
         * setViewModel這個method也是由Data Binding自動產生的，因為在<data>中我們變數名為viewModel。
         */
        binding.viewModel=viewModel

        /**
         * 這樣的方式相較ButterKnife更為方便，
         * 新增元件時ButterKnife要在程式裡用@Bind綁定，
         * 而Data Binding可以直接用binding.就取得元件。
         * 刪除元件時更是如此，
         * ButterKnife如果xml中的元件刪掉但忘了刪程式裡的@Bind就會發生Runtime exception，Data Binding則會在compile時就報錯。
         */
        binding.btnRefresh.setOnClickListener(object : View.OnClickListener{
            override fun onClick(v: View?) {
                viewModel.refresh()
            }

        })
    }
}
