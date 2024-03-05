package co.reachfive.identity.sdk.demo

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import co.reachfive.identity.sdk.core.models.responses.MfaCredential


class MfaCredentialsAdapter(private val context: Context, private var credentials: List<MfaCredential>): BaseAdapter() {
    override fun getCount(): Int {
        return credentials.size
    }

    fun refresh(credentials: List<MfaCredential>) {
        this.credentials = credentials
        notifyDataSetChanged()
    }

    override fun getItem(p0: Int): Any {
        return credentials[p0]
    }

    override fun getItemId(p0: Int): Long {
        return credentials[p0].friendlyName.hashCode().toLong()

    }
    private class ViewHolder(row: View?) {
        var friendlyName: TextView? = null
        var createdAt: TextView? = null
        var credential: TextView? = null
        init {
            this.friendlyName = row?.findViewById(R.id.friendlyName)
            this.credential = row?.findViewById(R.id.credential)
            this.createdAt = row?.findViewById(R.id.createdAt)
        }
    }

    override fun getView(p0: Int, convertView: View?, p2: ViewGroup?): View {
        val view: View?
        val viewHolder: ViewHolder
        if (convertView != null) {
            view = convertView
            viewHolder = view.tag as ViewHolder
        } else {
            val inflater =
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            view = inflater.inflate(R.layout.mfa_credential_item, null)
            viewHolder = ViewHolder(view)
            view?.tag = viewHolder
        }

        val credential = credentials[p0]

        viewHolder.friendlyName?.text = credential.friendlyName
        viewHolder.credential?.text = credential.email ?: credential.phoneNumber
        viewHolder.createdAt?.text = credential.createdAt

        return view as View
    }
}