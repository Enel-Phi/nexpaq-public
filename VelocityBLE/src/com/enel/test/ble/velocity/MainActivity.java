package com.enel.test.ble.velocity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class MainActivity extends Activity {
	private static final String TAG = "BLE Velocity Testing";

	private BluetoothAdapter mBTAdapter;
	private BluetoothLeScanner mScanner;
	private ScanSettings mSettings;
	private List<ScanFilter> mFilters;
	private BluetoothGatt mGatt;
	private BluetoothGattCharacteristic mWrite;

	private TextView lblConn, lblInfo, lblMaxTime, lblMinTime, lblTimeZone,
			lblStartTime, lblEndTime, lblLost;
	private Button btnStart;
	private Dialog builder;
	private ListView mBleDeviceListView;
	private static DeviceAdapter mDeviceAdapter;

	private int REQUEST_ENABLE_BT = 1;
	private long SCAN_PERIOD = 10000;
	private StringBuilder zoneContent;
	private long[] count;
	private int range = 10, number = 20;
	private int type = 0;
	private String option = "";
	private long maxTime = 0, minTime = 0, writeTime = 0, runTime = 0;
	private byte[] lastData = null;
	private int times = 0;
	private String interval = "0.0";
//	private int radix = 1000;
	private int radix = 10000;
	private boolean isConnected = false;
	private boolean hasResponse = true;
	private List<byte[]> mDataList;
	private String SDPATH = "";

	private static final UUID SERVICE_UUID = UUID
			.fromString("0000fff0-0000-1000-8000-00805f9b34fb");
	private static final UUID CHARA_WRITE = UUID
			.fromString("0000fff1-0000-1000-8000-00805f9b34fb");
	private static final UUID CHARA_NOTIFY = UUID
			.fromString("0000fff4-0000-1000-8000-00805f9b34fb");

	private final byte[] REQ_INTERVAL = { (byte) 0x0A, (byte) 0xF8 };
	private final byte[] RSP_INTERVAL = { (byte) 0x0A, (byte) 0xF9 };
	private final byte[] REQ_AUTO = { (byte) 0x0A, (byte) 0xFA };
	private final byte[] RSP_AUTO = { (byte) 0x0A, (byte) 0xFB };
	private final byte[] REQ_COUNT = { (byte) 0x0A, (byte) 0xFC };
	private final byte[] RSP_COUNT = { (byte) 0x0A, (byte) 0xFD };
	private final byte[] SET_COUNT = { (byte) 0x27, (byte) 0x10 };
//	private final byte[] SET_COUNT = { (byte) 0x03, (byte) 0xE8 };

	private static final int WHAT_CONNECTED = 1001;
	private static final int WHAT_DISCONNECTED = 1002;
	private static final int WHAT_INTERVAL = 1003;
	private static final int WHAT_TIME = 1004;
	private static final int WHAT_RESTART = 1005;
	private static final int WHAT_RESULT = 1006;
	private static final int WHAT_TIME_START = 1007;
	private static final int WHAT_TIME_END = 1008;
	private static final int WHAT_SCREEN_SHOT = 1009;

	// ===================================================
	// ===================================================
	// ===================================================

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		option = getResources().getString(R.string.txt_send);
		SDPATH = Environment.getExternalStorageDirectory().getAbsolutePath()
				+ File.separator + "Tom" + File.separator;

		checkFeatureBLE();
		initView();
		initValue();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (!isConnected) {
			scanBLE();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (mBTAdapter != null && mBTAdapter.isEnabled()) {
			scan(false);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mGatt == null) {
			return;
		}
		mGatt.close();
		mGatt = null;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == REQUEST_ENABLE_BT) {
			if (resultCode == Activity.RESULT_CANCELED) {
				finish();
				return;
			}
		}
	}

	// ===================================================
	// ===================================================
	// ===================================================

	private void checkFeatureBLE() {
		if (!getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_BLUETOOTH_LE)) {
			finish();
		}
		BluetoothManager btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		mBTAdapter = btManager.getAdapter();
	}

	private void initView() {
		lblConn = (TextView) findViewById(R.id.lblConnStatus);

		lblInfo = (TextView) findViewById(R.id.lblInfo);
		lblMaxTime = (TextView) findViewById(R.id.lblMaxTime);
		lblMinTime = (TextView) findViewById(R.id.lblMinTime);
		lblTimeZone = (TextView) findViewById(R.id.lblTimeZone);
		lblStartTime = (TextView) findViewById(R.id.lblStartTime);
		lblEndTime = (TextView) findViewById(R.id.lblEndTime);
		lblLost = (TextView) findViewById(R.id.lblLost);

		btnStart = (Button) findViewById(R.id.btnStart);
		btnStart.setEnabled(false);
		btnStart.setOnClickListener(mClickListener);
	}

	private void initValue() {
		maxTime = 0;
		minTime = 0;
		writeTime = 0;
		runTime = 0;

		String format = getResources().getString(R.string.lbl_state);
		String state = getResources().getString(R.string.lbl_disconn);
		if (isConnected) {
			state = getResources().getString(R.string.lbl_conn);
			lblConn.setText(String.format(format, state));
		}
		lblConn.setText(String.format(format, state));

		int num = 0;
		String content = "";
		switch (type) {
		case 0:
			lblMaxTime.setVisibility(View.VISIBLE);
			lblMinTime.setVisibility(View.VISIBLE);
			lblTimeZone.setVisibility(View.VISIBLE);
			lblStartTime.setVisibility(View.GONE);
			lblEndTime.setVisibility(View.GONE);
			lblLost.setVisibility(View.GONE);

			String max = getResources().getString(R.string.lbl_time_max);
			lblMaxTime.setText(String.format(max, num));
			String min = getResources().getString(R.string.lbl_time_min);
			lblMinTime.setText(String.format(min, num));
			String zone = getResources().getString(R.string.lbl_zone_content);
			lblTimeZone.setText(String.format(zone, content));

			zoneContent = new StringBuilder();
			count = new long[number + 1];
			String item = getResources().getString(R.string.txt_zone);
			String value = "";
			for (int i = 0; i < number; i++) {
				count[i] = 0;
				value = String.format(item, i * range, (i + 1) * range, 0);
				zoneContent.append(value);
			}
			String over = getResources().getString(R.string.txt_over);
			zoneContent.append(String.format(over, 0));
			content = getResources().getString(R.string.lbl_zone_content);
			lblTimeZone.setText(String.format(content, zoneContent));
			break;
		case 1:
			lblMaxTime.setVisibility(View.GONE);
			lblMinTime.setVisibility(View.GONE);
			lblTimeZone.setVisibility(View.GONE);
			lblStartTime.setVisibility(View.VISIBLE);
			lblEndTime.setVisibility(View.VISIBLE);
			lblLost.setVisibility(View.VISIBLE);

			String strat = getResources().getString(R.string.lbl_time_start);
			lblStartTime.setText(String.format(strat, num));
			String end = getResources().getString(R.string.lbl_time_stop);
			lblEndTime.setText(String.format(end, num));
			String lost = getResources().getString(R.string.lbl_lost);
			lblLost.setText(String.format(lost, num));
			break;
		}

	}

	private void scanBLE() {
		if (mBTAdapter == null || !mBTAdapter.isEnabled()) {
			Intent enableBtIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		} else {
			mScanner = mBTAdapter.getBluetoothLeScanner();
			mSettings = new ScanSettings.Builder().setScanMode(
					ScanSettings.SCAN_MODE_LOW_LATENCY).build();
			mFilters = new ArrayList<ScanFilter>();
			scan(true);
		}

	}

	private void scan(boolean enable) {
		if (enable) {
			initDialog();

			mHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					mScanner.stopScan(mScanCallback);
				}
			}, SCAN_PERIOD);
			mScanner.startScan(mFilters, mSettings, mScanCallback);
		} else {
			builder.dismiss();
			mScanner.stopScan(mScanCallback);
		}

	}

	@SuppressLint("InflateParams")
	private void initDialog() {
		builder = new Dialog(this);
		builder.show();

		LayoutInflater inflater = LayoutInflater.from(this);
		View viewDialog = inflater.inflate(R.layout.dialog_ble_list, null);
		builder.setContentView(viewDialog);

		mBleDeviceListView = (ListView) viewDialog.findViewById(R.id.list);
		mDeviceAdapter = new DeviceAdapter(this);
		mBleDeviceListView.setAdapter(mDeviceAdapter);

		mBleDeviceListView.setOnItemClickListener(itemClickListener);
	}

	private void refreshConnState(boolean result) {
		isConnected = result;
		String format = getResources().getString(R.string.lbl_state);
		String state = getResources().getString(R.string.lbl_conn);
		btnStart.setEnabled(result);
		lblConn.setText(String.format(format, state));
		if (!result) {
			state = getResources().getString(R.string.lbl_disconn);
			lblConn.setText(String.format(format, state));
			takeScreenShot();
		}
	}

	private void discoverServices() {
		BluetoothGattService service = mGatt.getService(SERVICE_UUID);
		if (service != null) {
			BluetoothGattCharacteristic notifyCharac = service
					.getCharacteristic(CHARA_NOTIFY);
			mWrite = service.getCharacteristic(CHARA_WRITE);
			if (notifyCharac != null && mWrite != null) {
				mWrite.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
				mGatt.setCharacteristicNotification(notifyCharac, true);
			}
		}
	}

	private void notification(byte[] data) {
		int node = (int) data[1];
		int contentLen = (int) data[6];
		if (contentLen == 0)
			return;
		byte[] rspType = { data[4], data[5] };
		byte[] content = new byte[contentLen];
		for (int i = 0; i < contentLen; i++) {
			content[i] = data[7 + i];
		}

		if (Arrays.equals(rspType, RSP_INTERVAL)) {
			double value = ((content[0] & 0xFF) * 16 * 16 + content[1]) * 1.25;
			DecimalFormat df = new DecimalFormat("#.00");
			interval = df.format(value);

			byte[] cmd = null;
			switch (type) {
			case 0:
				cmd = configCmd(node, REQ_COUNT, null);
				break;
			case 1:
				cmd = configCmd(node, REQ_AUTO, SET_COUNT);
				break;
			}
			mDataList.add(cmd);
		} else if (Arrays.equals(rspType, RSP_AUTO)) {
			times++;
			int count = (content[0] & 0xFF) * 16 * 16 + (content[1] & 0xFF) + 1;
			mHandler.sendEmptyMessage(WHAT_INTERVAL);

			if (count == 1) {
				mHandler.sendEmptyMessage(WHAT_TIME_START);
			} else if (count == radix) {
				mHandler.sendEmptyMessage(WHAT_TIME_END);

				byte[] cmd = configCmd(node, REQ_COUNT, null);
				mDataList.add(cmd);
			}
		} else if (Arrays.equals(rspType, RSP_COUNT)) {
			int count = (content[0] & 0xFF) * 16 * 16 + (content[1] & 0xFF) - 1;
			switch (type) {
			case 0:
				if (count <= 1) {
					times = 1;
					mHandler.sendEmptyMessage(WHAT_INTERVAL);
					byte[] cmd = autoWrite();
					mDataList.add(cmd);
				} else {
					times = count;
					mHandler.sendEmptyMessage(WHAT_INTERVAL);
					mHandler.sendEmptyMessage(WHAT_SCREEN_SHOT);
					
					mHandler.postDelayed(new Runnable() {

						@Override
						public void run() {
							mHandler.sendEmptyMessage(WHAT_RESTART);
						}
					}, 10 * 1000);
				}
				break;
			case 1:
				mHandler.sendEmptyMessage(WHAT_RESULT);
				break;
			}

		}
	}

	private void responseSend(byte[] data) {
		if (times > 0) {
			mDataList.remove(lastData);
			hasResponse = true;
			timing();
		}

		int node = (int) data[2];
		if (node == 9) {
			if (times < radix && times > 0) {
				times++;
				byte[] cmd = autoWrite();
				mDataList.add(cmd);
			} else {
				times = 0;
				byte[] cmd = configCmd(2, REQ_COUNT, null);
				mDataList.add(cmd);
			}
		}
	}

	private void refreshStartTime() {
		String format = getResources().getString(R.string.lbl_time_start);
		String time = getTime();
		lblStartTime.setText(String.format(format, time));
	}

	private void refreshEndTime() {
		String format = getResources().getString(R.string.lbl_time_stop);
		String time = getTime();
		lblEndTime.setText(String.format(format, time));
	}

	private void refreshTime() {
		String content = getResources().getString(R.string.lbl_time_max);
		lblMaxTime.setText(String.format(content, maxTime));
		content = getResources().getString(R.string.lbl_time_min);
		lblMinTime.setText(String.format(content, minTime));

		for (int i = 0; i < zoneContent.length(); i++) {
			zoneContent.delete(0, zoneContent.length());
		}
		lblTimeZone.setText(zoneContent);

		int index = (int) (runTime / range);
		if (index >= number) {
			count[number]++;
		} else {
			count[index]++;
		}

		String item = getResources().getString(R.string.txt_zone);
		for (int i = 0; i < number; i++) {
			zoneContent.append(String.format(item, i * range, (i + 1) * range,
					count[i]));
		}
		String over = getResources().getString(R.string.txt_over);
		zoneContent.append(String.format(over, count[number]));
		content = getResources().getString(R.string.lbl_zone_content);
		lblTimeZone.setText(String.format(content, zoneContent));
	}

	private void refreshInfo() {
		String info = getResources().getString(R.string.lbl_info);
		lblInfo.setText(String.format(info, option, times, interval));
		lblInfo.setVisibility(View.VISIBLE);
	}

	private void restart() {
		refreshConnState(true);
		times = 0;

		type = (type + 1) % 2;
		Log.e(TAG, "========" + type);
		switch (type) {
		case 0:
			option = getResources().getString(R.string.txt_send);
			break;
		case 1:
			option = getResources().getString(R.string.txt_receive);
			break;
		}
		initValue();
		byte[] data = configCmd(2, REQ_INTERVAL, null);
		mDataList.add(data);
	}

	private void result() {
		String lost = getResources().getString(R.string.lbl_lost);
		lblLost.setText(String.format(lost, radix - times));

		takeScreenShot();
		mHandler.postDelayed(new Runnable() {

			@Override
			public void run() {
				mHandler.sendEmptyMessage(WHAT_RESTART);
			}
		}, 10 * 1000);
	}

	private void timing() {
		runTime = (System.nanoTime() - writeTime) / 1000000;
		if (maxTime == 0 && minTime == 0) {
			maxTime = runTime;
			minTime = runTime;
		} else {
			if (runTime < minTime) {
				minTime = runTime;
			}
			if (runTime > maxTime) {
				maxTime = runTime;
			}
		}
		mHandler.sendEmptyMessage(WHAT_TIME);
	}

	private void sendCmd(byte[] value) {
		if (mGatt == null) {
			Log.e(TAG, "lost connection");
			return;
		}

		if (mWrite == null) {
			Log.e(TAG, "characteristic null");
			return;
		}
		value = CRC16.setCrcResult(value);
		mWrite.setValue(value);

		boolean result = mGatt.writeCharacteristic(mWrite);
		if (result) {
			if (times > 0) {
				writeTime = System.nanoTime();
			}
			hasResponse = false;
			Log.i(TAG, "0===" + HexDump.toHexString(value));
		} else {
			sendCmd(lastData);
		}
	}

	private byte[] autoWrite() {
		byte[] value = HexDump.intToBytes2(times);
		byte[] data = { (byte) 0x7E, (byte) 0x00, (byte) 0x09, (byte) 0x03,
				(byte) 0x00, value[1], value[0], (byte) 0xEE, (byte) 0xEE,
				(byte) 0x7E };

		return data;
	}

	private byte[] configCmd(int node, byte[] cmd, byte[] params) {
		int length = 0;
		if (params != null) {
			length = params.length;
		}
		int size = length + 10;
		byte[] data = new byte[size];
		data[0] = (byte) 0x7E;
		data[1] = (byte) 0x00;
		data[2] = (byte) node;
		data[3] = (byte) (length + 3);
		data[4] = cmd[0];
		data[5] = cmd[1];
		data[6] = (byte) length;
		for (int j = 0; j < length; j++) {
			data[j + 7] = params[j];
		}
		data[size - 3] = (byte) 0xEE;
		data[size - 2] = (byte) 0xEE;
		data[size - 1] = (byte) 0x7E;
		return data;
	}

	private boolean inCRC(byte[] data) {
		byte[] target = { data[data.length - 2], data[data.length - 3] };
		byte[] crc = CRC16.getCrcResult(data);

		if (Arrays.equals(crc, target)) {
			return true;
		} else {
			return false;
		}
	}

	private String getTime() {
		SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss SSS",
				Locale.CHINA);
		return format.format(new Date());
	}

	private void takeScreenShot() {
		SimpleDateFormat dateFormat = new SimpleDateFormat(
				"yyyy-MM-dd hh:mm:ss", Locale.CHINA);
		Date curDate = new Date();
		String name = "[" + option + "]" + dateFormat.format(curDate) + ".png";
		String path = SDPATH + name;

		View view = this.getWindow().getDecorView();
		view.setDrawingCacheEnabled(true);
		view.buildDrawingCache();
		Bitmap b1 = view.getDrawingCache();
		Rect frame = new Rect();
		view.getWindowVisibleDisplayFrame(frame);
		Bitmap bitmap = Bitmap.createBitmap(b1, 0, 0, view.getWidth(),
				view.getHeight());
		view.destroyDrawingCache();

		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(path);
			if (null != fos) {
				bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos);
				fos.flush();
				fos.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void startTesting() {
		mDataList = new ArrayList<byte[]>();

		btnStart.setVisibility(View.GONE);
		lblInfo.setVisibility(View.VISIBLE);

		byte[] data = configCmd(2, REQ_INTERVAL, null);
		mDataList.add(data);

		new Thread(new Runnable() {

			@Override
			public void run() {
				while (isConnected) {
					if (hasResponse && mDataList.size() > 0) {
						lastData = mDataList.get(0);
						if (lastData != null) {
							sendCmd(lastData);
						}
					}
				}
			}
		}).start();
	}

	// ===================================================
	// ===================================================
	// ===================================================

	OnClickListener mClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			startTesting();
		}
	};

	ScanCallback mScanCallback = new ScanCallback() {
		@Override
		public void onScanResult(int callbackType, ScanResult result) {
			BluetoothDevice device = result.getDevice();
			mDeviceAdapter.addDevice(device);
			mDeviceAdapter.notifyDataSetChanged();
		}

		@Override
		public void onBatchScanResults(List<ScanResult> results) {
		}

		@Override
		public void onScanFailed(int errorCode) {
			Log.e(TAG, "Error Code: " + errorCode);
		}
	};

	OnItemClickListener itemClickListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			BluetoothDevice device = mDeviceAdapter.getDevice(position);
			if (mGatt == null) {
				mGatt = device.connectGatt(MainActivity.this, false,
						mGattCallback);
				scan(false);
			}
		}
	};

	BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status,
				int newState) {
			super.onConnectionStateChange(gatt, status, newState);
			switch (newState) {
			case BluetoothProfile.STATE_CONNECTED:
				mHandler.sendEmptyMessage(WHAT_CONNECTED);
				gatt.discoverServices();
				break;
			case BluetoothProfile.STATE_DISCONNECTED:
				mHandler.sendEmptyMessage(WHAT_DISCONNECTED);
				break;
			default:
				Log.e(TAG, "STATE_OTHER");
			}
		}

		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				discoverServices();
			}
		}

		@Override
		public void onCharacteristicWrite(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic, int status) {
			byte[] data = characteristic.getValue();
			if (Arrays.equals(data, lastData)) {
				Log.w(TAG, "1===" + HexDump.toHexString(data));

				mDataList.remove(lastData);
				hasResponse = true;

				switch (type) {
				case 0:
					responseSend(data);
					break;
				case 1:

					break;
				}
			}
		}

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic) {
			byte[] data = characteristic.getValue();
			if (inCRC(data)) {
				Log.e(TAG, "2===" + HexDump.toHexString(data));

				notification(data);
			}
		}
	};

	@SuppressLint("HandlerLeak")
	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			int what = msg.what;
			switch (what) {
			case WHAT_CONNECTED:
				refreshConnState(true);
				break;
			case WHAT_DISCONNECTED:
				refreshConnState(false);
				break;
			case WHAT_INTERVAL:
				refreshInfo();
				break;
			case WHAT_TIME:
				refreshInfo();
				refreshTime();
				break;
			case WHAT_RESTART:
				restart();
				break;
			case WHAT_RESULT:
				result();
				break;
			case WHAT_TIME_START:
				refreshStartTime();
				break;
			case WHAT_TIME_END:
				refreshEndTime();
				break;
			case WHAT_SCREEN_SHOT:
				takeScreenShot();
				break;
			}
		};
	};

}
