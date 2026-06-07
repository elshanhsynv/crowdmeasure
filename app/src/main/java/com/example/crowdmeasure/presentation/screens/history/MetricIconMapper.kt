package com.example.crowdmeasure.presentation.screens.history

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CompareArrows
import androidx.compose.material.icons.automirrored.outlined.FactCheck
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.Adb
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Battery6Bar
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.CellTower
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DataUsage
import androidx.compose.material.icons.outlined.DeveloperBoard
import androidx.compose.material.icons.outlined.Domain
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.Nightlight
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.SettingsCell
import androidx.compose.material.icons.outlined.SignalCellularAlt
import androidx.compose.material.icons.outlined.SimCard
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.ui.graphics.vector.ImageVector

internal object MetricIconMapper {
    fun iconFor(label: String): ImageVector = when (label) {
        "Device Model" -> Icons.Outlined.PhoneAndroid
        "OS Version" -> Icons.Outlined.Android
        "Android SDK" -> Icons.Outlined.Code
        "App Version" -> Icons.Outlined.Apps
        "Brand" -> Icons.AutoMirrored.Outlined.Label
        "Device Manufacturer" -> Icons.Outlined.Business
        "Device OS" -> Icons.Outlined.Adb
        "Build ID" -> Icons.Outlined.Fingerprint
        "Hardware" -> Icons.Outlined.DeveloperBoard
        "Chipset" -> Icons.Outlined.Memory
        "Chipset Manufacturer" -> Icons.Outlined.Domain
        "Memory Usage" -> Icons.Outlined.Memory
        "Thermal State" -> Icons.Outlined.Tune

        "Battery" -> Icons.Outlined.Battery6Bar
        "Charging" -> Icons.Outlined.Bolt
        "Battery Saver" -> Icons.Outlined.BatteryAlert
        "Screen On" -> Icons.Outlined.LightMode
        "Doze Mode" -> Icons.Outlined.Nightlight

        "Transport", "Connection" -> Icons.AutoMirrored.Outlined.CompareArrows
        "Internet" -> Icons.Outlined.Language
        "Captive Portal" -> Icons.Outlined.OpenInBrowser
        "VPN" -> Icons.Outlined.VpnKey
        "Metered", "Data Saver", "Data Roaming" -> Icons.Outlined.DataUsage
        "Public IP" -> Icons.Outlined.Public
        "ISP" -> Icons.Outlined.Business
        "ASN" -> Icons.Outlined.Numbers
        "DNS", "Protocol" -> Icons.Outlined.Hub
        "TLS" -> Icons.Outlined.Lock

        "Signal Strength (RSSI)", "Wi-Fi Standard" -> Icons.Outlined.Wifi
        "Frequency", "Opportunistic" -> Icons.Outlined.Tune

        "Carrier", "SIM Count", "SIM Operator", "SIM Operator ID", "Display Name", "eSIM" ->
            Icons.Outlined.SimCard

        "MCC", "MNC", "Slot Index", "Subscription ID", "Carrier ID", "Port Index", "Card ID" ->
            Icons.Outlined.Numbers

        "ISO Country Code", "Country" -> Icons.Outlined.Flag
        "RAT", "Data Network Type", "Voice Network Type", "Duplex Mode" ->
            Icons.Outlined.SettingsCell

        "Roaming" -> Icons.Outlined.Flight
        "Registered", "Collected From", "Collected Here", "Active Data", "Default Data",
        "Default Voice", "Default SMS" -> Icons.AutoMirrored.Outlined.FactCheck

        "Cell ID", "LAC", "TAC", "PCI" -> Icons.Outlined.CellTower
        "Neighbor Cells" -> Icons.Outlined.NearMe
        "Band", "ARFCN" -> Icons.Outlined.Sensors
        "RSRP", "RSRQ", "RSSNR" -> Icons.Outlined.SignalCellularAlt

        "Link Speed (legacy)", "Link Speed" -> Icons.Outlined.Speed
        "TX Link Speed", "Up", "Up P95", "Up StdDev", "UL Rate" -> Icons.Outlined.Upload
        "RX Link Speed", "Down", "Down P95", "Down StdDev", "DL Rate" -> Icons.Outlined.Download
        "TTFB Average" -> Icons.Outlined.Schedule
        "HTTP Status", "HTTP Latency P95" -> Icons.Outlined.BarChart
        "Server Region" -> Icons.Outlined.Language
        "HTTP Latency Average" -> Icons.Outlined.Timeline
        "Jitter" -> Icons.AutoMirrored.Outlined.ShowChart
        "Probe Failure %" -> Icons.Outlined.WifiOff
        "Stalls" -> Icons.Outlined.PauseCircle
        "Max Stall" -> Icons.Outlined.HourglassEmpty

        else -> Icons.Outlined.Info
    }
}
