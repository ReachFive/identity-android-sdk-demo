package co.reachfive.identity.sdk.demo

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.TextView
import co.reachfive.identity.sdk.core.models.responses.webAuthn.DeviceCredential

interface ButtonCallbacks {
    fun removeDeviceCallback(position: Int)
}

class DevicesAdapter(
    private val context: Context,
    private var devices: List<DeviceCredential>,
    var callbacks: ButtonCallbacks
) : BaseAdapter() {
    fun refresh(devices: List<DeviceCredential>) {
        this.devices = devices
        notifyDataSetChanged()
    }

    override fun getItem(position: Int): DeviceCredential {
        return devices[position]
    }

    override fun getItemId(position: Int): Long {
        return devices[position].friendlyName.hashCode().toLong()
    }

    override fun getCount(): Int {
        return devices.size
    }

    private class ViewHolder(row: View?) {
        var friendlyName: TextView? = null
        var createdAt: TextView? = null
        var lastUsedAt: TextView? = null
        var provider: TextView? = null

        init {
            this.friendlyName = row?.findViewById(R.id.friendlyName)
            this.createdAt = row?.findViewById(R.id.createdAt)
            this.lastUsedAt = row?.findViewById(R.id.lastUsedAt)
            this.provider = row?.findViewById(R.id.provider)
        }
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View?
        val viewHolder: ViewHolder
        if (convertView != null) {
            view = convertView
            viewHolder = view.tag as ViewHolder
        } else {
            val inflater =
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            view = inflater.inflate(R.layout.device_item, null)
            viewHolder = ViewHolder(view)
            view?.tag = viewHolder
        }

        val device = devices[position]

        viewHolder.friendlyName?.text = device.friendlyName
        viewHolder.createdAt?.text = device.createdAt
        viewHolder.lastUsedAt?.text = device.lastUsedAt
        viewHolder.provider?.text = device.aaguid

        val deleteDeviceButton = view?.findViewById(R.id.removeDevice) as Button
        deleteDeviceButton.setOnClickListener {
            devices.drop(position)
            callbacks.removeDeviceCallback(position)
        }

        return view
    }
}