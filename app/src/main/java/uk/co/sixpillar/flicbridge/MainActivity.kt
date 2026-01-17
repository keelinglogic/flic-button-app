package uk.co.sixpillar.flicbridge

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import io.flic.flic2libandroid.Flic2Button
import io.flic.flic2libandroid.Flic2Manager
import io.flic.flic2libandroid.Flic2ScanCallback
import uk.co.sixpillar.flicbridge.adapter.ButtonAdapter
import uk.co.sixpillar.flicbridge.databinding.ActivityMainBinding
import uk.co.sixpillar.flicbridge.service.FlicBridgeService

class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "MainActivity"
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var buttonAdapter: ButtonAdapter
    private var isScanning = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            Log.i(TAG, "All permissions granted")
            startFlicService()
        } else {
            Toast.makeText(this, "Bluetooth permissions required", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        setupRecyclerView()
        setupButtons()
        checkPermissions()
    }

    override fun onResume() {
        super.onResume()
        refreshButtonList()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupRecyclerView() {
        buttonAdapter = ButtonAdapter { button ->
            // On button click - show info
            Toast.makeText(
                this,
                "Button: ${button.name ?: button.bdAddr}\nBattery: ${button.lastKnownBatteryLevel}%",
                Toast.LENGTH_SHORT
            ).show()
        }

        binding.recyclerButtons.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = buttonAdapter
        }
    }

    private fun setupButtons() {
        binding.btnScan.setOnClickListener {
            if (isScanning) {
                stopScanning()
            } else {
                startScanning()
            }
        }

        binding.btnStartService.setOnClickListener {
            startFlicService()
            Toast.makeText(this, "Service started", Toast.LENGTH_SHORT).show()
        }

        binding.btnStopService.setOnClickListener {
            stopFlicService()
            Toast.makeText(this, "Service stopped", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkPermissions() {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        }

        // Location for BLE scanning (Android 6-11)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        // Notifications (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions.toTypedArray())
        } else {
            startFlicService()
        }
    }

    private fun startFlicService() {
        val intent = Intent(this, FlicBridgeService::class.java)
        ContextCompat.startForegroundService(this, intent)
        updateServiceStatus(true)
    }

    private fun stopFlicService() {
        val intent = Intent(this, FlicBridgeService::class.java)
        stopService(intent)
        updateServiceStatus(false)
    }

    private fun updateServiceStatus(running: Boolean) {
        binding.txtServiceStatus.text = if (running) "Service: Running" else "Service: Stopped"
        binding.txtServiceStatus.setTextColor(
            ContextCompat.getColor(
                this,
                if (running) android.R.color.holo_green_dark else android.R.color.holo_red_dark
            )
        )
    }

    private fun startScanning() {
        Log.i(TAG, "Starting Flic scan")
        isScanning = true
        binding.btnScan.text = "Stop Scanning"
        binding.progressScanning.visibility = View.VISIBLE

        try {
            Flic2Manager.getInstance().startScan(object : Flic2ScanCallback {
                override fun onDiscoveredAlreadyPairedButton(button: Flic2Button) {
                    Log.i(TAG, "Discovered already paired button: ${button.bdAddr}")
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            "Found paired button: ${button.name ?: button.bdAddr}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onDiscovered(bdAddr: String) {
                    Log.i(TAG, "Discovered new button: $bdAddr")
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            "Found new button - pairing...",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    // Stop scan and connect
                    Flic2Manager.getInstance().stopScan()
                }

                override fun onComplete(
                    result: Int,
                    subCode: Int,
                    button: Flic2Button?
                ) {
                    runOnUiThread {
                        isScanning = false
                        binding.btnScan.text = "Scan for Buttons"
                        binding.progressScanning.visibility = View.GONE

                        when (result) {
                            Flic2ScanCallback.RESULT_SUCCESS -> {
                                button?.let {
                                    Log.i(TAG, "Successfully paired button: ${it.bdAddr}")
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Paired: ${it.name ?: it.bdAddr}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    refreshButtonList()
                                }
                            }
                            Flic2ScanCallback.RESULT_CANCELED -> {
                                Log.i(TAG, "Scan canceled")
                            }
                            else -> {
                                Log.e(TAG, "Scan failed: result=$result, subCode=$subCode")
                                Toast.makeText(
                                    this@MainActivity,
                                    "Scan failed (code: $result)",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error starting scan", e)
            isScanning = false
            binding.btnScan.text = "Scan for Buttons"
            binding.progressScanning.visibility = View.GONE
            Toast.makeText(this, "Scan error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun stopScanning() {
        Log.i(TAG, "Stopping Flic scan")
        try {
            Flic2Manager.getInstance().stopScan()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping scan", e)
        }
        isScanning = false
        binding.btnScan.text = "Scan for Buttons"
        binding.progressScanning.visibility = View.GONE
    }

    private fun refreshButtonList() {
        try {
            val buttons = Flic2Manager.getInstance().buttons
            Log.i(TAG, "Refreshing button list: ${buttons.size} buttons")
            buttonAdapter.submitList(buttons.toList())

            binding.txtNoButtons.visibility = if (buttons.isEmpty()) View.VISIBLE else View.GONE
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing button list", e)
        }
    }
}
