package com.enel.test.ble.velocity;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class DeviceAdapter extends BaseAdapter {

	private ArrayList<BluetoothDevice> mLeDevices;
	private LayoutInflater mInflator;
	private Activity mContext;

	public DeviceAdapter(Activity c) {
		super();
		mContext = c;
		mLeDevices = new ArrayList<BluetoothDevice>();
		mInflator = mContext.getLayoutInflater();
	}

	public void addDevice(BluetoothDevice device) {
		if (!mLeDevices.contains(device)) {
			mLeDevices.add(device);
		}
	}

	public BluetoothDevice getDevice(int position) {
		return mLeDevices.get(position);
	}

	public void clear() {
		mLeDevices.clear();
	}

	@Override
	public int getCount() {
		return mLeDevices.size();
	}

	@Override
	public Object getItem(int position) {
		return mLeDevices.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@SuppressLint("InflateParams")
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder viewHolder;
		if (convertView == null) {
			convertView = mInflator.inflate(R.layout.ble_listitem, null);
			viewHolder = new ViewHolder();
			viewHolder.address = (TextView) convertView
					.findViewById(R.id.device_address);
			viewHolder.name = (TextView) convertView
					.findViewById(R.id.device_name);
			convertView.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}

		BluetoothDevice device = mLeDevices.get(position);
		final String deviceName = device.getName();
		if (deviceName != null && deviceName.length() > 0)
			viewHolder.name.setText(deviceName);
		else
			viewHolder.name.setText("Unknown device");
		viewHolder.address.setText(device.getAddress());

		return convertView;
	}

	class ViewHolder {
		TextView name, address;
	}

}
