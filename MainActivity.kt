package com.example.esp32test

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp


import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import kotlin.math.roundToInt

import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.runtime.mutableStateListOf

import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.runtime.LaunchedEffect

import androidx.compose.material3.CircularProgressIndicator
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            Log.d("BLE_App", "Όλες οι άδειες δόθηκαν επιτυχώς!")
        } else {
            Log.e("BLE_App", "Κάποια άδεια απορρίφθηκε από τον χρήστη.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        checkAndRequestPermissions()

        setContent {
            MaterialTheme {
                App_Navigation(this)
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsNeeded = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }

        val missingPermissions = permissionsNeeded.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }
}


// κομμάτι 2


@Composable
fun App_Navigation(context: Context) {
    var app_State by remember { mutableStateOf("SCAN") }

    var username by remember { mutableStateOf("user") }
    var password by remember { mutableStateOf("5678") }
    var is_24_Hour_Selected by remember { mutableStateOf(false) }
    var day_Volume by remember { mutableStateOf(0f) }
    var night_Volume by remember { mutableStateOf(0f) }

    val profile_Names = listOf("Κούκος", "Big Ben", "Close Encounters", "Καμπάνες", "Μέγιστη Ισχύς")
    var selected_Profile_Index by remember { mutableStateOf(0) }

    val color_Names = listOf("Κόκκινο", "Πράσινο", "Μπλε", "Λευκό")
    var selected_Color_Index by remember { mutableStateOf(0) }

    val checkbox_Labels = listOf(
        "Θερινή ώρα", "Ήχος στα ακριβώς", "Ήχος τετάρτων", "Εκφώνηση Ανατολής / Δύσης",
        "Αναγγελία δευτερολέπτων", "Αναγγελία ημερομηνίας", "Οπτικές ενδείξεις", "LED δευτερολέπτων",
        "Αναγγελία ωρών λειτουργίας", "Αναγγελία Συγχρονισμού"
    )

    val checkbox_States = remember {
        mutableStateListOf<Boolean>().apply { repeat(10) { add(false) } }
    }

    // Κρατάμε την ενεργή σύνδεση Bluetooth GATT στη μνήμη του κινητού
    var activeGatt by remember { mutableStateOf<android.bluetooth.BluetoothGatt?>(null) }

    val SERVICE_UUID = "4fa4c201-1fb5-459e-8fcc-c5c9c331914b"
    val CHARACTERISTIC_TX_UUID = "5fa4c202-1fb5-459e-8fcc-c5c9c331914b"
    val CHARACTERISTIC_RX_UUID = "5fa4c203-1fb5-459e-8fcc-c5c9c331914b"

    when (app_State) {
        "SCAN" -> Scan_Screen(
            context = context,
            serviceUuidString = SERVICE_UUID,
            txUuidString = CHARACTERISTIC_TX_UUID,
            on_Connected = { gattInstance, rUser, rPass, rDayVol, rNightVol, rProfile, rColor, rFlags ->
                activeGatt = gattInstance
                username = rUser
                password = rPass
                day_Volume = rDayVol
                night_Volume = rNightVol
                selected_Profile_Index = rProfile
                selected_Color_Index = rColor

                if (rFlags.size >= 10) {
                    for (i in 0 until 10) {
                        checkbox_States[i] = rFlags[i]
                    }
                }
                app_State = "MAIN"
            }
        )
        "MAIN" -> Main_Screen(
            username = username, on_User_Change = { username = it },
            password = password, on_Pass_Change = { password = it },
            day_Volume = day_Volume, on_Day_Vol_Change = { day_Volume = it },
            night_Volume = night_Volume, on_Night_Vol_Change = { night_Volume = it },
            is_24_Hour_Selected = is_24_Hour_Selected, on_24H_Change = { is_24_Hour_Selected = it },
            selected_Profile_Index = selected_Profile_Index, on_Profile_Change = { selected_Profile_Index = it },
            selected_Color_Index = selected_Color_Index, on_Color_Change = { selected_Color_Index = it },
            profile_Names = profile_Names, color_Names = color_Names,
            checkbox_Labels = checkbox_Labels, checkbox_States = checkbox_States,
            on_Checkbox_Change = { index, value -> checkbox_States[index] = value },
            on_Def_Clicked = {
                username = "Admin"
                password = "1234"
                day_Volume = 19f
                night_Volume = 14f
                is_24_Hour_Selected = false
                selected_Profile_Index = 2
                selected_Color_Index = 1
                checkbox_States[0] = false
                checkbox_States[1] = true
                checkbox_States[2] = false
                checkbox_States[3] = true
                checkbox_States[4] = false
                checkbox_States[5] = false
                checkbox_States[6] = false
                checkbox_States[7] = true
                checkbox_States[8] = true
                checkbox_States[9] = true
            },
            on_Send_Clicked = {
                val generated_Json = Create_Json_String(
                    user = username,
                    pass = password,
                    day_Vol = day_Volume.roundToInt(),
                    night_Vol = night_Volume.roundToInt(),
                    is_24H = is_24_Hour_Selected,
                    profile = selected_Profile_Index,
                    color = selected_Color_Index,
                    states = checkbox_States
                )

                Log.d("ESP32_JSON", "Έτοιμο JSON για ESP32: $generated_Json")

                activeGatt?.let { gatt ->
                    val serviceUuid = java.util.UUID.fromString(SERVICE_UUID)
                    val rxUuid = java.util.UUID.fromString(CHARACTERISTIC_RX_UUID)
                    val service = gatt.getService(serviceUuid)
                    val rxCharacteristic = service?.getCharacteristic(rxUuid)

                    if (rxCharacteristic != null) {
                        rxCharacteristic.value = generated_Json.toByteArray(Charsets.UTF_8)
                        gatt.writeCharacteristic(rxCharacteristic)
                        Log.d("BLE_APP", "Το JSON στάλθηκε επιτυχώς στον αέρα!")
                    } else {
                        Log.e("BLE_APP", "Αποτυχία: Δεν βρέθηκε το κανάλι RX στο ESP32.")
                    }
                }

                app_State = "END"
            }
        )
        "END" -> End_Screen(
            username = username, password = password, day_Volume = day_Volume, night_Volume = night_Volume,
            is_24_Hour_Selected = is_24_Hour_Selected, profile_Name = profile_Names[selected_Profile_Index],
            color_Name = color_Names[selected_Color_Index], checkbox_Labels = checkbox_Labels, checkbox_States = checkbox_States
        )
    }
}


// κομμάτι 3


fun Create_Json_String(
    user: String, pass: String, day_Vol: Int, night_Vol: Int,
    is_24H: Boolean, profile: Int, color: Int, states: List<Boolean>
): String {
    val sb = java.lang.StringBuilder()
    sb.append("{")
    sb.append("\"user\":\"$user\",")
    sb.append("\"pass\":\"$pass\",")
    sb.append("\"dayVol\":$day_Vol,")
    sb.append("\"nightVol\":$night_Vol,")
    sb.append("\"is24H\":$is_24H,")
    sb.append("\"profile\":$profile,")
    sb.append("\"color\":$color,")
    sb.append("\"flags\":[")

    for (i in states.indices) {
        sb.append(states[i])
        if (i < states.size - 1) {
            sb.append(",")
        }
    }

    sb.append("]")
    sb.append("}")
    return sb.toString()
}


// κομμάτι 4


@Composable
fun Scan_Screen(
    context: Context,
    serviceUuidString: String,
    txUuidString: String,
    on_Connected: (android.bluetooth.BluetoothGatt?, String, String, Float, Float, Int, Int, List<Boolean>) -> Unit
) {
    var is_Scanning by remember { mutableStateOf(false) }
    var connection_Message by remember { mutableStateOf("Εφαρμογή σύνδεσης") }

    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    val isPreview = androidx.compose.ui.platform.LocalInspectionMode.current
    val bluetoothAdapter: BluetoothAdapter? = if (isPreview) null else bluetoothManager.adapter

    if (!isPreview && is_Scanning && bluetoothAdapter != null && bluetoothAdapter.isEnabled) {
        LaunchedEffect(Unit) {
            connection_Message = "Γίνεται σάρωση για ESP32..."
            val leScanner = bluetoothAdapter.bluetoothLeScanner

            val scanCallback = object : android.bluetooth.le.ScanCallback() {
                override fun onScanResult(callbackType: Int, result: android.bluetooth.le.ScanResult?) {
                    result?.device?.let { device ->
                        if (device.name == "ESP32_Config") {
                            connection_Message = "Βρέθηκε η συσκευή! Σύνδεση..."
                            leScanner?.stopScan(this)

                            device.connectGatt(context, false, object : android.bluetooth.BluetoothGattCallback() {
                                override fun onConnectionStateChange(gatt: android.bluetooth.BluetoothGatt?, status: Int, newState: Int) {
                                    if (newState == android.bluetooth.BluetoothProfile.STATE_CONNECTED) {
                                        connection_Message = "Συνδέθηκε! Ανάγνωση υπηρεσιών..."
                                        gatt?.discoverServices()
                                    } else if (newState == android.bluetooth.BluetoothProfile.STATE_DISCONNECTED) {
                                        connection_Message = "Η σύνδεση χάθηκε."
                                        is_Scanning = false
                                    }
                                }

                                override fun onServicesDiscovered(gatt: android.bluetooth.BluetoothGatt?, status: Int) {
                                    if (status == android.bluetooth.BluetoothGatt.GATT_SUCCESS) {
                                        val serviceUuid = java.util.UUID.fromString(serviceUuidString)
                                        val txUuid = java.util.UUID.fromString(txUuidString)

                                        val service = gatt?.getService(serviceUuid)
                                        val characteristic = service?.getCharacteristic(txUuid)

                                        if (characteristic != null) {
                                            connection_Message = "Ανάγνωση δεδομένων JSON..."
                                            gatt.readCharacteristic(characteristic)
                                        } else {
                                            connection_Message = "Σφάλμα: Δεν βρέθηκε το κανάλι TX."
                                            is_Scanning = false
                                        }
                                    }
                                }

                                @Deprecated("Deprecated in Java")
                                override fun onCharacteristicRead(
                                    gatt: android.bluetooth.BluetoothGatt?,
                                    characteristic: android.bluetooth.BluetoothGattCharacteristic?,
                                    status: Int
                                ) {
                                    if (status == android.bluetooth.BluetoothGatt.GATT_SUCCESS && characteristic != null) {
                                        val rawData = characteristic.value
                                        val jsonString = String(rawData, Charsets.UTF_8)
                                        Log.d("BLE_RECEIVED", "Λήφθηκε από ESP32: $jsonString")

                                        try {
                                            val user = jsonString.substringAfter("\"user\":\"").substringBefore("\"")
                                            val pass = jsonString.substringAfter("\"pass\":\"").substringBefore("\"")
                                            val dayVol = jsonString.substringAfter("\"dayVol\":").substringBefore(",").toFloatOrNull() ?: 20f
                                            val nightVol = jsonString.substringAfter("\"nightVol\":").substringBefore(",").toFloatOrNull() ?: 14f
                                            val profile = jsonString.substringAfter("\"profile\":").substringBefore(",").toIntOrNull() ?: 0
                                            val color = jsonString.substringAfter("\"color\":").substringBefore(",").toIntOrNull() ?: 0

                                            val flagsRaw = jsonString.substringAfter("\"flags\":[").substringBefore("]")
                                            val flagsList = flagsRaw.split(",").map { it.trim().toBoolean() }

                                            connection_Message = "Επιτυχής λήψη ρυθμίσεων!"
                                            on_Connected(gatt, user, pass, dayVol, nightVol, profile, color, flagsList)
                                        } catch (e: Exception) {
                                            connection_Message = "Σφάλμα ανάγνωσης JSON."
                                            is_Scanning = false
                                        }
                                    }
                                }
                            })
                        }
                    }
                }
            }

            leScanner?.startScan(scanCallback)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Black
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = connection_Message,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                if (!is_Scanning) {
                    Button(
                        onClick = { is_Scanning = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                    ) {
                        Text(text = "Scan", style = MaterialTheme.typography.bodyLarge)
                    }
                } else if (connection_Message == "Γίνεται σάρωση για ESP32...") {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 4.dp)
                }
            }
        }
    }
}


// κομμάτι 5

@Composable
fun Main_Screen(
    username: String, on_User_Change: (String) -> Unit,
    password: String, on_Pass_Change: (String) -> Unit,
    day_Volume: Float, on_Day_Vol_Change: (Float) -> Unit,
    night_Volume: Float, on_Night_Vol_Change: (Float) -> Unit,
    is_24_Hour_Selected: Boolean, on_24H_Change: (Boolean) -> Unit,
    selected_Profile_Index: Int, on_Profile_Change: (Int) -> Unit,
    selected_Color_Index: Int, on_Color_Change: (Int) -> Unit,
    profile_Names: List<String>, color_Names: List<String>,
    checkbox_Labels: List<String>, checkbox_States: List<Boolean>,
    on_Checkbox_Change: (Int, Boolean) -> Unit,
    on_Def_Clicked: () -> Unit,
    on_Send_Clicked: () -> Unit
) {
    val day_min_Volume = 0f
    val day_max_Volume = 24f
    val night_min_Volume = 0f
    val night_max_Volume = 18f

    val username_Focus_Requester = remember { FocusRequester() }
    val password_Focus_Requester = remember { FocusRequester() }

    val invalid_Characters = " !@#$%^&*()+=[]{};:'\",<>/?\\|~"

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Black
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Ρυθμίσεις ESP32",
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Username", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(
                value = username,
                onValueChange = { input ->
                    if (input.length <= 16 && input.none { it in invalid_Characters }) {
                        on_User_Change(input)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(username_Focus_Requester),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                    focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.White, unfocusedIndicatorColor = Color.Gray
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "Password", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(
                value = password,
                onValueChange = { input ->
                    if (input.length <= 16 && input.none { it in invalid_Characters }) {
                        on_Pass_Change(input)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(password_Focus_Requester),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                    focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.White, unfocusedIndicatorColor = Color.Gray
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(text = "Ένταση ήχου", color = Color.White, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Text(text = "Ημερήσια ένταση: ${day_Volume.roundToInt()}", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            Slider(
                value = day_Volume, onValueChange = { on_Day_Vol_Change(it) }, valueRange = day_min_Volume..day_max_Volume,
                colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White, inactiveTrackColor = Color.DarkGray)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(text = "Νυκτερινή ένταση: ${night_Volume.roundToInt()}", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            Slider(
                value = night_Volume, onValueChange = { on_Night_Vol_Change(it) }, valueRange = night_min_Volume..night_max_Volume,
                colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White, inactiveTrackColor = Color.DarkGray)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(text = "Εκφώνηση 12 / 24", color = Color.White, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = !is_24_Hour_Selected, onClick = { on_24H_Change(false) },
                        colors = RadioButtonDefaults.colors(selectedColor = Color.White, unselectedColor = Color.Gray)
                    )
                    Text(text = "12ωρη εκφώνηση", color = Color.Gray)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = is_24_Hour_Selected, onClick = { on_24H_Change(true) },
                        colors = RadioButtonDefaults.colors(selectedColor = Color.White, unselectedColor = Color.Gray)
                    )
                    Text(text = "24ωρη εκφώνηση", color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(text = "Προφίλ", color = Color.White, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))

            Column {
                profile_Names.forEachIndexed { index, name ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        RadioButton(
                            selected = (selected_Profile_Index == index), onClick = { on_Profile_Change(index) },
                            colors = RadioButtonDefaults.colors(selectedColor = Color.White, unselectedColor = Color.Gray)
                        )
                        Text(text = name, color = Color.Gray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(text = "Χρώμα φωτισμού", color = Color.White, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))

            Column {
                color_Names.forEachIndexed { index, name ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        RadioButton(
                            selected = (selected_Color_Index == index), onClick = { on_Color_Change(index) },
                            colors = RadioButtonDefaults.colors(selectedColor = Color.White, unselectedColor = Color.Gray)
                        )
                        Text(text = name, color = Color.Gray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(text = "Επιπλέον Λειτουργίες", color = Color.White, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))

            Column {
                checkbox_Labels.forEachIndexed { index, label ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Checkbox(
                            checked = checkbox_States[index],
                            onCheckedChange = { on_Checkbox_Change(index, it) },
                            colors = CheckboxDefaults.colors(checkedColor = Color.White, uncheckedColor = Color.Gray, checkmarkColor = Color.Black)
                        )
                        Text(text = label, color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = { on_Def_Clicked() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray, contentColor = Color.White)
                ) {
                    Text(text = "Def")
                }

                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    onClick = {
                        if (username.isEmpty()) {
                            username_Focus_Requester.requestFocus()
                        } else if (password.isEmpty()) {
                            password_Focus_Requester.requestFocus()
                        } else {
                            on_Send_Clicked()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                ) {
                    Text(text = "Αποστολή")
                }
            }
        }
    }
}


// κομμάτι 6


@Composable
fun End_Screen(

    username: String, password: String, day_Volume: Float, night_Volume: Float,
    is_24_Hour_Selected: Boolean, profile_Name: String, color_Name: String,
    checkbox_Labels: List<String>, checkbox_States: List<Boolean>
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Black
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "τέλος",
                color = Color.White,
                style = MaterialTheme.typography.headlineLarge
            )

            Spacer(modifier = Modifier.height(24.dp))

            Show_Summary(
                username = username, password = password, day_Volume = day_Volume, night_Volume = night_Volume,
                is_24_Hour_Selected = is_24_Hour_Selected, profile_Name = profile_Name, color_Name = color_Name,
                checkbox_Labels = checkbox_Labels, checkbox_States = checkbox_States
            )
        }
    }
}

@Composable
fun Show_Summary(
    username: String, password: String, day_Volume: Float, night_Volume: Float,
    is_24_Hour_Selected: Boolean, profile_Name: String, color_Name: String,
    checkbox_Labels: List<String>, checkbox_States: List<Boolean>
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Text(text = "Σύνοψη Δεδομένων Αποστολής:", color = Color.White, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(12.dp))

        Text(text = "Username: $username", color = Color.Gray)
        Text(text = "Password: $password", color = Color.Gray)
        Text(text = "Ημερήσια Ένταση: ${day_Volume.roundToInt()}", color = Color.Gray)
        Text(text = "Νυκτερινή Ένταση: ${night_Volume.roundToInt()}", color = Color.Gray)
        Text(text = "Μορφή Ώρας: ${if (is_24_Hour_Selected) "24ωρη" else "12ωρη"}", color = Color.Gray)
        Text(text = "Επιλεγμένο Προφίλ: $profile_Name", color = Color.Gray)
        Text(text = "Χρώμα Φωτισμού: $color_Name", color = Color.Gray)

        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Κατάσταση Λειτουργιών:", color = Color.White, style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(8.dp))

        checkbox_Labels.forEachIndexed { index, label ->
            val status = if (checkbox_States[index]) "Ενεργό [✓]" else "Ανενεργό [ ]"
            Text(text = "• $label: $status", color = Color.Gray)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    App_Navigation(androidx.compose.ui.platform.LocalContext.current)
}



