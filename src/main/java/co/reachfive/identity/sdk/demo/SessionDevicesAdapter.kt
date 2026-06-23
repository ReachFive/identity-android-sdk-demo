package co.reachfive.identity.sdk.demo

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageButton
import android.widget.TextView
import co.reachfive.identity.sdk.core.models.responses.SessionDevice

interface ButtonSessionDeviceCallback {
    fun removeSessionDeviceCallback(position: Int)
}

class SessionDevicesAdapter(private val context: Context, private var sessionDevices: List<SessionDevice>, var callback: ButtonSessionDeviceCallback): BaseAdapter() {
    override fun getCount(): Int {
        return sessionDevices.size
    }

    fun refresh(sessionDevices: List<SessionDevice>) {
        this.sessionDevices = sessionDevices
        notifyDataSetChanged()
    }

    override fun getItem(p0: Int): Any {
        return sessionDevices[p0]
    }

    override fun getItemId(p0: Int): Long {
        return sessionDevices[p0].id.hashCode().toLong()
    }

    private class ViewHolder(row: View?) {
        var id: TextView? = null
        var ip: TextView? = null
        var country: TextView? = null
        var city: TextView? = null

        var createdAt: TextView? = null
        var tokenType: TextView? = null
        var lastConnection: TextView? = null
        init {
            this.id = row?.findViewById(R.id.sessionDeviceId)
            this.tokenType = row?.findViewById(R.id.sessionDeviceTokenType)
            this.ip = row?.findViewById(R.id.sessionDeviceIp)
            this.country = row?.findViewById(R.id.sessionDeviceCountry)
            this.city = row?.findViewById(R.id.sessionDeviceCity)
            this.createdAt = row?.findViewById(R.id.sessionDeviceCreatedAt)
            this.lastConnection = row?.findViewById(R.id.sessionDeviceLastConnection)
        }
    }

    override fun getView(position: Int, convertView: View?, p2: ViewGroup?): View {
        val view: View?
        val viewHolder: ViewHolder
        if (convertView != null) {
            view = convertView
            viewHolder = view.tag as ViewHolder
        } else {
            val inflater =
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            view = inflater.inflate(R.layout.session_device_item, null)
            viewHolder = ViewHolder(view)
            view?.tag = viewHolder
        }

        val sessionDevice = sessionDevices[position]

        viewHolder.id?.text = sessionDevice.id
        viewHolder.ip?.text = sessionDevice.ip
        viewHolder.tokenType?.text = sessionDevice.tokenType.name
        viewHolder.createdAt?.text = sessionDevice.createdAt.substring(0, sessionDevice.createdAt.indexOf("."))
        viewHolder.lastConnection?.text = sessionDevice.lastConnection.substring(0, sessionDevice.createdAt.indexOf("."))
        viewHolder.country?.text = sessionDevice.country
        viewHolder.city?.text = sessionDevice.city

        val deleteSessionDeviceButton = view?.findViewById(R.id.removeSessionDevice) as ImageButton
        deleteSessionDeviceButton.setOnClickListener {
            callback.removeSessionDeviceCallback(position)
        }

        return view
    }

}