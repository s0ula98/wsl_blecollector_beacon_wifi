package com.example.wsl_blecollector_beacon;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.example.wsl_blecollector.R;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

public class WifiListView extends AppCompatActivity {

    WifiManager wifiManager;

    ArrayAdapter<String> scanAdapter;
    ListView listView_scan;
    ArrayList<String> scanList;
    Button bt_cancel, bt_scan, bt_excel;
    BluetoothAdapter myBluetoothAdapter;

    EditText rp, filename;

    private void scanSuccess() {
        List<ScanResult> scanResult = wifiManager.getScanResults();
        Log.e("wifi-info",scanResult.toString());
        for (int i = 0; i < scanResult.size(); i++) {
            ScanResult result = scanResult.get(i);
            scanList.add(result.BSSID + "\n" + result.level);
            scanAdapter.notifyDataSetChanged();
        }
        Log.i("scanlist", scanList.toString());
    }

    private void scanFailure() {
        // handle failure: new scan did NOT succeed
        // consider using old scan results: these are the OLD results!
        List<ScanResult> results = wifiManager.getScanResults();
    }

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

        wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);

        BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent intent) {
                boolean success = intent.getBooleanExtra(
                        WifiManager.EXTRA_RESULTS_UPDATED, false);
                if (success) {
                    scanSuccess();
                    Log.e("wifi","scanSuccess !!!!!!!!!!!!!!!");
                } else {
                    // scan failure handling
                    scanFailure();
                    Log.e("wifi","scanFailure ..............");
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(wifiScanReceiver, intentFilter);

        bt_scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scanList.clear();
                boolean success = wifiManager.startScan();
                if (!success) {
                    // scan failure handling
                    scanFailure();
                    Log.e("wifi", "scanFailure ..............");
                }
//                scanAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, scanList);
//                listView_scan.setAdapter(scanAdapter);
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
            cell.setCellValue("BSSID"); // 1번 셀 값 입력

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