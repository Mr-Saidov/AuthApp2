package java.com.authapp2

import android.accounts.Account
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_main.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

class MainActivity : AppCompatActivity() {

    private lateinit var appAuthUtil: AppAuthUtil
    private lateinit var adapter: AccountRecyclerAdapter
    private var accounts: MutableList<Account> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        appAuthUtil = AppAuthUtil(this)

        accounts = appAuthUtil.checkAccounts()
        val account = selectedAccount()
        if (!appAuthUtil.decryptPrefsString().isNullOrEmpty() && account != null) {
            val mainActivityIntent = Intent(this, MainActivity::class.java)
            mainActivityIntent.putExtra("ACCOUNT_NAME", account.name)
            startActivity(intent)
        } else {
            adapter = AccountRecyclerAdapter(accounts) { account ->
//                appAuthUtil.authorizeWithSavedAccount(account)
                appAuthUtil.encryptPrefsString(account.name)
                val mainActivityIntent = Intent(this, MainActivity2::class.java)
                mainActivityIntent.putExtra("ACCOUNT_NAME", account.name)
                startActivity(mainActivityIntent)
                finish()
            }

            recyclerView.layoutManager = LinearLayoutManager(this)
            recyclerView.adapter = adapter

            authorizeBtn.setOnClickListener {
                startActivityForResult(
                    appAuthUtil.getAuthIntent(),
                    appAuthUtil.AUTH_REQUEST
                )
            }
        }
    }

    private fun selectedAccount(): Account? {
        for (account in accounts) if (account.name == appAuthUtil.decryptPrefsString()) return account
        return null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == appAuthUtil.AUTH_REQUEST) {
            appAuthUtil.checkIntent(data)
        }
    }

    @Subscribe
    fun onAccountSave(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        accounts.addAll(appAuthUtil.checkAccounts())
        adapter.notifyDataSetChanged()
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }
}
