package com.example.wsl_blecollector;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

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
import java.util.Date;
import java.util.UUID;
import java.text.SimpleDateFormat;


public class PairingListVeiw extends AppCompatActivity {
    final String TAG = "SubActivity";
    ArrayAdapter<String> scanAdapter;
    ListView  listView_scan;
    ArrayList<String> scanList;
    Button bt_cancel, bt_scan, bt_excel;
    BluetoothAdapter myBluetoothAdapter;
    EditText x_coordinate, y_coordinate, rp, filename;
    protected static UUID MY_UUID;
    private SimpleDateFormat mFormat = new SimpleDateFormat("yyyy_M_d"); // 날짜 포맷
    Date date;
    long mNow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pairing_list_veiw);

        bt_cancel = (Button) findViewById(R.id.bt_cancel);
        bt_scan = (Button) findViewById(R.id.bt_scan);
        bt_excel = (Button) findViewById(R.id.bt_excel);
        listView_scan = (ListView) findViewById(R.id.listview_scan);
        x_coordinate = (EditText) findViewById(R.id.x_coordinate);
        y_coordinate = (EditText) findViewById(R.id.y_coordinate);
        rp = (EditText) findViewById(R.id.rp);
        filename = (EditText) findViewById(R.id.filename);

        //블루투스 어답터
        myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        //기기스캔 목록
        scanList = new ArrayList<>();

        //간단한 방법 복잡한 방법 여러가지네??? 심지어 온라인에서 만들어 주는데고 있다.
        MY_UUID = UUID.randomUUID();
        Log.d(TAG, MY_UUID.toString());


        //스캔버튼을 클릭하면 주변 기기를 모두 스캐닝 함
        bt_scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (myBluetoothAdapter.isDiscovering()) {
                    myBluetoothAdapter.cancelDiscovery();
                }
                scanList.clear();//기존 목록을 크리어함
                myBluetoothAdapter.startDiscovery();
            }
        });

        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(myReceiver, intentFilter);//onDestory()에서 언레지스터하는 것을 추가해 줄것.

        scanAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, scanList);
        listView_scan.setAdapter(scanAdapter);

        //리스트 항목 클릭시
        listView_scan.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String selectedItem = (String) adapterView.getItemAtPosition(i);
                Toast.makeText(getApplicationContext(), "연결 기기: " + selectedItem, Toast.LENGTH_SHORT).show();
                // 선택한 기기를 페어링 목록에 추가한다.
            }
        });

        bt_excel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int blue_size =scanAdapter.getCount();
                Log.v("아이탬 개수", String.valueOf(blue_size));
                if(x_coordinate.length() == 0 || y_coordinate.length() == 0 || rp.length() == 0 ) {
                    Toast.makeText(getApplicationContext(), "좌표 또는 rp 값을 입력해 주세요.", Toast.LENGTH_SHORT).show();
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

    //브로드캐스트 리시버
    final BroadcastReceiver myReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //ListView 갱신함
                //먼저 기존 데이터를 비워주고 시작해야 할듯 중복 추가되는 문제
                // 해결 위해서...
                short rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI,  Short.MIN_VALUE);
                scanList.add(device.getName() + "\n" + device.getAddress() + "\n" + rssi);
                scanAdapter.notifyDataSetChanged();
            }
        }
    };

    private void saveExcel(int blue_size, String[][] bluetooth_excel) {
        String Date = getDate();

        File dir = Environment.getExternalStorageDirectory();
        String abPath = dir.getAbsolutePath(); //패키지명을 구한다.
        String packageName = getPackageName();
        String path = abPath + "/android/data/" + packageName +"/files/";
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
                cell.setCellValue(bluetooth_excel[i - rowmax][2]);
                cell = row.createCell(3);
                cell.setCellValue(x_coordinate.getText().toString());
                cell = row.createCell(4);
                cell.setCellValue(y_coordinate.getText().toString());
                cell = row.createCell(5);
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
            cell.setCellValue("블루투스 기기 이름"); // 1번 셀 값 입력

            cell = row.createCell(1); // 2번 셀 생성
            cell.setCellValue("mac Address"); // 2번 셀 값 입력

            cell = row.createCell(2); // 3번 셀 생성
            cell.setCellValue("RSSI"); // 3번 셀 값 입력

            cell = row.createCell(3); // 4번 셀 생성
            cell.setCellValue("X 좌표"); // 4번 셀 값 입력

            cell = row.createCell(4); // 5번 셀 생성
            cell.setCellValue("Y 좌표"); // 5번 셀 값 입력

            cell = row.createCell(5); // 5번 셀 생성
            cell.setCellValue("rp");    // 6번 셀 값 입력

            for (int i = 0; i < blue_size; i++) { // 데이터 엑셀에 입력
                row = sheet.createRow(i + 1);
                cell = row.createCell(0);
                cell.setCellValue(bluetooth_excel[i][0]);
                cell = row.createCell(1);
                cell.setCellValue(bluetooth_excel[i][1]);
                cell = row.createCell(2);
                cell.setCellValue(bluetooth_excel[i][2]);
                cell = row.createCell(3);
                cell.setCellValue(x_coordinate.getText().toString());
                cell = row.createCell(4);
                cell.setCellValue(y_coordinate.getText().toString());
                cell = row.createCell(5);
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

    private String getDate(){
        mNow = System.currentTimeMillis();
        date = new Date(mNow);
        return mFormat.format(date);
    }

}