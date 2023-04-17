package com.example.wsl_blecollector_beacon;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.IntentFilter;
import android.os.Bundle;

import com.example.wsl_blecollector.R;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class BeaconListView extends AppCompatActivity implements com.example.wsl_blecollector_beacon.Beacon, BeaconConsumer {
    private static final String TAG = "Beacontest";
    private BeaconManager beaconManager;

    private List<Beacon> beaconList = new ArrayList<>();
    TextView textView;

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    ArrayAdapter<String> scanAdapter;
    ListView listView_scan;
    ArrayList<String> scanList;
    Button bt_cancel, bt_scan, bt_excel;
    BluetoothAdapter myBluetoothAdapter;

    EditText rp, filename;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pairing_list_veiw);

        bt_cancel = (Button) findViewById(R.id.bt_cancel);
        bt_scan = (Button) findViewById(R.id.bt_scan);
        bt_excel = (Button) findViewById(R.id.bt_excel);
        listView_scan = (ListView) findViewById(R.id.listview_scan);
        rp = (EditText) findViewById(R.id.rp);
        filename = (EditText) findViewById(R.id.filename);
        scanList = new ArrayList<>();

        beaconManager = BeaconManager.getInstanceForApplication(this);

        //비콘 매니저에서 layout 설정 'm:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25'
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"));

        //beaconManager 설정 bind
        beaconManager.bind(this);

        // 버튼이 클릭되면 textView 에 비콘들의 정보를 뿌린다.
        bt_scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 아래에 있는 handleMessage를 부르는 함수. 맨 처음에는 0초간격이지만 한번 호출되고 나면
                // 1초마다 불러온다.
                handler.sendEmptyMessage(0);
            }
        });

        scanAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, scanList);
        listView_scan.setAdapter(scanAdapter);

        bt_excel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int blue_size = scanAdapter.getCount();
                Log.v("아이탬 개수", String.valueOf(blue_size));
                if (rp.length() == 0 ) {
                    Toast.makeText(getApplicationContext(), "label 값을 입력해 주세요.", Toast.LENGTH_SHORT).show();
                } else if(filename.length() == 0) {
                    Toast.makeText(getApplicationContext(), "엑셀 파일 이름을 입력해주세요", Toast.LENGTH_SHORT).show();
                } else {
                    String[][] bluetooth_excel = new String[blue_size][];
                    for(int i = 0; i < blue_size; i++) {
                        String save = (String) listView_scan.getAdapter().getItem(i);
                        bluetooth_excel[i] = save.split("\n");
                    }
                    saveExcel(blue_size, bluetooth_excel);
                }
            }
        });

        //액티비티 닫기 버튼
        bt_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //액티비티 닫기
                finish();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        beaconManager.unbind(this);
    }

    @Override
    public void onBeaconServiceConnect() {
        beaconManager.addRangeNotifier(new RangeNotifier() {
            @Override
            // 비콘이 감지되면 해당 함수가 호출된다. Collection<Beacon> beacons에는 감지된 비콘의 리스트가,
            // region에는 비콘들에 대응하는 Region 객체가 들어온다.
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                if (beacons.size() > 0) {
                    beaconList.clear();
                    for (Beacon beacon : beacons) {
                        beaconList.add(beacon);
                    }
                }
            }
        });

        try {
            beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
        } catch (RemoteException e) {   }
    }

    Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            scanList.clear();
            // 비콘의 아이디와 거리를 측정하여 textView에 넣는다.
            for(Beacon beacon : beaconList){
                String uuid = beacon.getId1().toString(); //beacon uuid
                int major = beacon.getId2().toInt(); //beacon major
                int minor = beacon.getId3().toInt();// beacon minor
                String address = beacon.getBluetoothAddress();
                String rssi = String.valueOf(beacon.getRssi());
                scanList.add(address + "\n" + rssi);
                scanAdapter.notifyDataSetChanged();
            }

            // 자기 자신을 1초마다 호출
//            handler.sendEmptyMessageDelayed(0, 1000);
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }

                    });
                    builder.show();
                }
                return;
            }
        }
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
    }

    private void saveExcel(int blue_size, String[][] bluetooth_excel) {

        File dir = Environment.getExternalStorageDirectory();
        String abPath = dir.getAbsolutePath(); //패키지명을 구한다.
        String packageName = getPackageName();
        String path = abPath + "/android/data/" + packageName + "/files/";
        String fname = filename.getText().toString() + ".xls";
        String ffname = path + fname;

        Log.v("파일명", ffname);

        File file = new File(ffname);
        if (file.exists()) {
            HSSFWorkbook wb = null;
            try {
                FileInputStream fileInputStream = new FileInputStream(file);
                wb = new HSSFWorkbook(fileInputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }

            HSSFSheet sheet = wb.getSheetAt(0);
            int rowmax = sheet.getPhysicalNumberOfRows();
            Log.v("row 개수", String.valueOf(rowmax));

            Row row;
            Cell cell;

            for (int i = rowmax; i < blue_size + rowmax; i++) { // 데이터 엑셀에 입력
                row = sheet.createRow(i);
                cell = row.createCell(0);
                cell.setCellValue(bluetooth_excel[i - rowmax][0]);
                cell = row.createCell(1);
                cell.setCellValue(bluetooth_excel[i - rowmax][1]);
                cell = row.createCell(2);
                cell.setCellValue(rp.getText().toString());
            }
            File xlsFile = new File(getExternalFilesDir(null), fname);
            try {
                FileOutputStream os = new FileOutputStream(xlsFile);
                wb.write(os); // 외부 저장소에 엑셀 파일 생성
            } catch (IOException e) {
                e.printStackTrace();
            }
            Toast.makeText(getApplicationContext(), xlsFile.getAbsolutePath() + "에 추가 되었습니다", Toast.LENGTH_LONG).show();
            Log.v("엑셀파일", xlsFile.getAbsolutePath());

        } else {
            Workbook workbook = new HSSFWorkbook();

            Sheet sheet = workbook.createSheet(); // 새로운 시트 생성

            Row row = sheet.createRow(0); // 새로운 행 생성
            Cell cell;

            cell = row.createCell(0); // 1번 셀 생성
            cell.setCellValue("mac Address"); // 1번 셀 값 입력

            cell = row.createCell(1); // 2번 셀 생성
            cell.setCellValue("RSSI"); // 2번 셀 값 입력

            cell = row.createCell(2); // 3번 셀 생성
            cell.setCellValue("rp");    // 3번 셀 값 입력

            for (int i = 0; i < blue_size; i++) { // 데이터 엑셀에 입력
                row = sheet.createRow(i + 1);
                cell = row.createCell(0);
                cell.setCellValue(bluetooth_excel[i][0]);
                cell = row.createCell(1);
                cell.setCellValue(bluetooth_excel[i][1]);
                cell = row.createCell(2);
                cell.setCellValue(rp.getText().toString());
            }

            File xlsFile = new File(getExternalFilesDir(null), fname);
            try {
                FileOutputStream os = new FileOutputStream(xlsFile);
                workbook.write(os); // 외부 저장소에 엑셀 파일 생성
            } catch (IOException e) {
                e.printStackTrace();
            }
            Toast.makeText(getApplicationContext(), xlsFile.getAbsolutePath() + "에 저장되었습니다", Toast.LENGTH_LONG).show();
            Log.v("엑셀파일", xlsFile.getAbsolutePath());
        }
    }
}