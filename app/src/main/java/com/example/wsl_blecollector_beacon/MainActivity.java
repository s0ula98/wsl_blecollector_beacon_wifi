package com.example.wsl_blecollector_beacon;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.wsl_blecollector.R;

public class MainActivity extends AppCompatActivity {

    final String TAG = "MainActivity";
    int REQUEST_ENABLE_BT = 1;
    Button bt_on, bt_off, bt_pairing, bt_beacon, bt_wifi;
    BluetoothAdapter myBluetoothAdapter;
    Intent btEnableIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bt_on = (Button) findViewById(R.id.bt_bluetooth_on);
        bt_off = (Button) findViewById(R.id.bt_bluetooth_off);
        bt_pairing = (Button) findViewById(R.id.bt_pairing);
        bt_beacon = (Button) findViewById(R.id.bt_beacon);
        bt_wifi = (Button) findViewById(R.id.bt_wifi);

        //1. 지금 이 기기가 블루투스를 지원 하는지 안하는지 체크 한다. 요즘도 안되는게 있나?
        //2. 지금 블루투스 사용이 활성화 되어 있는지 체크 한다.
        //3. 안되어 있으면 가능 하도록 설정한다.
        myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);

        if(permissionCheck == PackageManager.PERMISSION_DENIED){ //위치 권한 확인

            //위치 권한 요청
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
        }

        bluetoothOnMethod(); //켜기 버튼과 연계
        bluetoothOffMethod();//끄기 버튼과 연계
        bluetoothPairing();
        scanBeacon();
        scanWifi();
    }

    private void bluetoothOffMethod() {
        bt_off.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (myBluetoothAdapter.isEnabled()) {
                    myBluetoothAdapter.disable();
                    Toast.makeText(getApplicationContext(), "블루투스를 비활성화 되었습니다.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void bluetoothOnMethod() {
        bt_on.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (myBluetoothAdapter == null) {
                    //Device does not support Bluetooth
                    //할게 없다 앱을 종료 한다. 블루투스앱인데 블루투스 지원 안하는데 뭘 하겠어.
                    Toast.makeText(getApplicationContext(), "블루투스를 지원하지 않는 기기입니다.", Toast.LENGTH_SHORT).show();
                } else {
                    //블루투스 되는 기기이다.
                    //그렇다면 지금 현재 블루투스 기능이 켜져 있는지 체크 해야 한다.
                    if (!myBluetoothAdapter.isEnabled()) { //false이면
                        //지금 꺼져 있으니 켜야 한다.
                        btEnableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(btEnableIntent, REQUEST_ENABLE_BT);
                        //위 결과값을 받아서 처리 한다.
                    }
                }
            }
        });
    }

    //startActivityForResult 실행 후 결과를 처리하는 부분
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                //블루투스가 활성화 되었다.
                Toast.makeText(getApplicationContext(), "블루투스가 활성화 되었습니다.", Toast.LENGTH_SHORT).show();
            } else if (resultCode == RESULT_CANCELED) {
                //블루투스 켜는것을 취소했다.
                Toast.makeText(getApplicationContext(), "블루투스가 활성화 되지 않았습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void bluetoothPairing() {
        bt_pairing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //1.블루투스가 활성화 되어 있어야 한다.
                //2.새로운 액티비티를 열어서 페어링된 기기 목록을 보여 준다. 리스트뷰 사용함
                //3.새로운 액티비티에서 기기를 연결한다.
                //4.새로운 액티비티를 닫는다. 원하는 기기와의 연결확인.
                //이미 페어링된 기기가 없으면 새로 기기를 검색해야 한다. 여기서는 다루지 않는다.
                //즉, 다른 기기를 검색하고 페어링하는 단계는 폰의 내장된 블루투스 메뉴에서 하라는 말이다.
                if(myBluetoothAdapter.isEnabled()) {
                    //새로운 액티비티를 연다.
                    Intent pairingIntent = new Intent(MainActivity.this, PairingListVeiw.class);
                    startActivity(pairingIntent);
                } else {
                    Toast.makeText(getApplicationContext(), "먼저 블루투스를 활성화 하세요.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void scanBeacon() {
        bt_beacon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(myBluetoothAdapter.isEnabled()) {
                    //새로운 액티비티를 연다.
                    Intent pairingIntent = new Intent(MainActivity.this, BeaconListView.class);
                    startActivity(pairingIntent);
                } else {
                    Toast.makeText(getApplicationContext(), "먼저 블루투스를 활성화 하세요.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void scanWifi() {
        bt_wifi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(myBluetoothAdapter.isEnabled()) {
                    //새로운 액티비티를 연다.
                    Intent pairingIntent = new Intent(MainActivity.this, WifiListView.class);
                    startActivity(pairingIntent);
                } else {
                    Toast.makeText(getApplicationContext(), "먼저 블루투스를 활성화 하세요.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}