package java.com.authapp2

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main2.*

class MainActivity2 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

        tvAccountName.text = "Account name:\n" + intent.extras?.getString("ACCOUNT_NAME")

        val account = selectedAccount()

        btnLogout.setOnClickListener {
            val accountManager = AccountManager.get(this)
            accountManager.removeAccount(account, null, null)
            startActivity(Intent(this, MainActivity2::class.java))
            finish()
        }
    }

    private fun selectedAccount(): Account? {
        val accountManager = AccountManager.get(this)
        val accounts = accountManager.getAccountsByType(this.getString(R.string.account_type))
        for (account in accounts) if (account.name == intent.extras!!.getString("ACCOUNT_NAME")) return account
        return null
    }
}