package co.reachfive.identity.sdk.demo

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageButton
import android.widget.TextView
import co.reachfive.identity.sdk.core.models.responses.TrustedDevice

interface ButtonTrustedDeviceCallback {
    fun removeCredentialCallback(position: Int)
}

class TrustedDevicesAdapter (
    private val context: Context,
    private var trustedDevices: List<TrustedDevice>,
    var callback: ButtonTrustedDeviceCallback
): BaseAdapter() {
    override fun getCount(): Int {
        return trustedDevices.size
    }

    fun refresh(trustedDevices: List<TrustedDevice>) {
        this.trustedDevices = trustedDevices
        notifyDataSetChanged()
    }

    override fun getItem(p0: Int): TrustedDevice {
        return trustedDevices[p0]
    }

    override fun getItemId(p0: Int): Long {
        return trustedDevices[p0].id.hashCode().toLong()

    }
    private class ViewHolder(row: View?) {
        var userId: TextView? = null
        var createdAt: TextView? = null
        var operatingSystem: TextView? = null
        var deviceClass: TextView? = null
        var deviceName: TextView? = null
        init {
            this.userId = row?.findViewById(R.id.trustedDeviceUserId)
            this.operatingSystem = row?.findViewById(R.id.deviceOperatingSystem)
            this.createdAt = row?.findViewById(R.id.trustedDeviceCreatedAt)
            this.deviceClass = row?.findViewById(R.id.deviceClass)
            this.deviceName = row?.findViewById(R.id.deviceName)

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
            view = inflater.inflate(R.layout.trusted_device_item, null)
            viewHolder = ViewHolder(view)
            view?.tag = viewHolder
        }

        val trustedDevice = trustedDevices[position]

        viewHolder.userId?.text = trustedDevice.userId
        viewHolder.createdAt?.text = trustedDevice.createdAt.substring(0, trustedDevice.createdAt.indexOf("."))
        viewHolder.operatingSystem?.text = trustedDevice.metadata.operatingSystem
        viewHolder.deviceClass?.text = trustedDevice.metadata.deviceClass
        viewHolder.deviceName?.text = trustedDevice.metadata.deviceName

        val deleteTrustedDeviceButton = view?.findViewById(R.id.removeTrustedDevice) as ImageButton
        deleteTrustedDeviceButton.setOnClickListener {
            trustedDevices.drop(position)
            callback.removeCredentialCallback(position)
        }

        return view
    }

}