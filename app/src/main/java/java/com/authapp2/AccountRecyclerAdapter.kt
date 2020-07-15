package java.com.authapp2

import android.accounts.Account
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView

class AccountRecyclerAdapter(
    private val list: MutableList<Account>,
    private val clickListener: (account: Account) -> (Unit)
) :
    RecyclerView.Adapter<AccountRecyclerAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var name: TextView = view.findViewById(R.id.name)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = parent.context
            .getSystemService(AppCompatActivity.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        return ViewHolder(inflater.inflate(R.layout.account_item, parent, false))
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val account = list[position]
        holder.name.text = account.name
        holder.itemView.setOnClickListener {
            clickListener(account)
        }
    }
}