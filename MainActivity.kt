package com.AGexample.blog1

import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

import android.os.Bundle
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.Toast

import com.wowza.gocoder.sdk.api.WowzaGoCoder
import com.wowza.gocoder.sdk.api.broadcast.WOWZBroadcast
import com.wowza.gocoder.sdk.api.broadcast.WOWZBroadcastConfig
import com.wowza.gocoder.sdk.api.configuration.WOWZMediaConfig
import com.wowza.gocoder.sdk.api.devices.WOWZAudioDevice
import com.wowza.gocoder.sdk.api.devices.WOWZCameraView
import com.wowza.gocoder.sdk.api.errors.WOWZError
import com.wowza.gocoder.sdk.api.errors.WOWZStreamingError
import com.wowza.gocoder.sdk.api.status.WOWZState
import com.wowza.gocoder.sdk.api.status.WOWZStatus
import com.wowza.gocoder.sdk.api.status.WOWZStatusCallback

class MainActivity : AppCompatActivity(), WOWZStatusCallback, View.OnClickListener {

    // The top-level GoCoder API interface
    private var goCoder: WowzaGoCoder? = null

    // The GoCoder SDK camera view
    private var goCoderCameraView: WOWZCameraView? = null

    // The GoCoder SDK audio device
    private var goCoderAudioDevice: WOWZAudioDevice? = null

    // The GoCoder SDK broadcaster
    private var goCoderBroadcaster: WOWZBroadcast? = null

    // The broadcast configuration settings
    private var goCoderBroadcastConfig: WOWZBroadcastConfig? = null
    private var mPermissionsGranted = true
    private val mRequiredPermissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize the GoCoder SDK
        goCoder = WowzaGoCoder.init(applicationContext, "KEY-GOES-HERE")

        if (goCoder == null) {
            // If initialization failed, retrieve the last error and display it
            val goCoderInitError = WowzaGoCoder.getLastError()
            Toast.makeText(this,
                    "GoCoder SDK error: " + goCoderInitError.errorDescription,
                    Toast.LENGTH_LONG).show()
            return
        }
        // Associate the WOWZCameraView defined in the U/I layout with the corresponding class member
        goCoderCameraView = findViewById<View>(R.id.camera_preview) as WOWZCameraView

        // Create an audio device instance for capturing and broadcasting audio
        goCoderAudioDevice = WOWZAudioDevice()

        // Associate the onClick() method as the callback for the broadcast button's click event
        val broadcastButton = findViewById<View>(R.id.broadcast_button) as Button
        broadcastButton.setOnClickListener(this)
    }

    //
    // Enable Android's immersive, sticky full-screen mode
    //
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        val rootView = window.decorView.findViewById<View>(android.R.id.content)
        if (rootView != null)
            rootView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }

    //
    // Called when an activity is brought to the foreground
    //
    override fun onResume() {
        super.onResume()

        // If running on Android 6 (Marshmallow) and later, check to see if the necessary permissions
        // have been granted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mPermissionsGranted = hasPermissions(this, mRequiredPermissions)
            if (!mPermissionsGranted) {
            }
            ActivityCompat.requestPermissions(this, mRequiredPermissions, PERMISSIONS_REQUEST_CODE)
        } else
            mPermissionsGranted = true

        // Start the camera preview display
        if (mPermissionsGranted && goCoderCameraView != null) {
            if (goCoderCameraView!!.isPreviewPaused)
                goCoderCameraView!!.onResume()
            else
                goCoderCameraView!!.startPreview()
        }

        // Create a broadcaster instance
        goCoderBroadcaster = WOWZBroadcast()

        // Create a configuration instance for the broadcaster
        goCoderBroadcastConfig = WOWZBroadcastConfig(WOWZMediaConfig.FRAME_SIZE_1920x1080)

        // Set the connection properties for the target Wowza Streaming Engine server or Wowza Streaming Cloud live stream
        goCoderBroadcastConfig!!.hostAddress = "HOST-NAME"
        goCoderBroadcastConfig!!.portNumber = 1935
        goCoderBroadcastConfig!!.applicationName = "APP-NAME"
        goCoderBroadcastConfig!!.streamName = "STREAM-NAME"

        // Designate the camera preview as the video source
        goCoderBroadcastConfig!!.videoBroadcaster = goCoderCameraView

        // Designate the audio device as the audio broadcaster
        goCoderBroadcastConfig!!.audioBroadcaster = goCoderAudioDevice

    }

    //
    // Callback invoked in response to a call to ActivityCompat.requestPermissions() to interpret
    // the results of the permissions request
    //
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        mPermissionsGranted = true
        when (requestCode) {
            PERMISSIONS_REQUEST_CODE -> {
                // Check the result of each permission granted
                for (grantResult in grantResults) {
                    if (grantResult != PackageManager.PERMISSION_GRANTED) {
                        mPermissionsGranted = false
                    }
                }
            }
        }
    }

    // The callback invoked upon changes to the state of the broadcast
    //
    override fun onWZStatus(goCoderStatus: WOWZStatus) {
        // A successful status transition has been reported by the GoCoder SDK
        val statusMessage = StringBuffer("Broadcast status: ")

        when (goCoderStatus.state) {
            WOWZState.STARTING -> statusMessage.append("Broadcast initialization")

            WOWZState.READY -> statusMessage.append("Ready to begin streaming")

            WOWZState.RUNNING -> statusMessage.append("Streaming is active")

            WOWZState.STOPPING -> statusMessage.append("Broadcast shutting down")

            WOWZState.IDLE -> statusMessage.append("The broadcast is stopped")

            else -> return
        }

        // Display the status message using the U/I thread
        Handler(Looper.getMainLooper()).post { Toast.makeText(this@MainActivity, statusMessage, Toast.LENGTH_LONG).show() }
    }

    //
    // The callback invoked when an error occurs during a broadcast
    //
    override fun onWZError(goCoderStatus: WOWZStatus) {
        // If an error is reported by the GoCoder SDK, display a message
        // containing the error details using the U/I thread
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this@MainActivity,
                    "Streaming error: " + goCoderStatus.lastError.errorDescription,
                    Toast.LENGTH_LONG).show()
        }
    }

    // The callback invoked when the broadcast button is tapped
    //
    override fun onClick(view: View) {
        // return if the user hasn't granted the app the necessary permissions
        if (!mPermissionsGranted) return

        // Ensure the minimum set of configuration settings have been specified necessary to
        // initiate a broadcast streaming session
        val configValidationError = goCoderBroadcastConfig!!.validateForBroadcast()

        if (configValidationError != null) {
            Toast.makeText(this, configValidationError.errorDescription, Toast.LENGTH_LONG).show()
        } else if (goCoderBroadcaster!!.status.isRunning) {
            // Stop the broadcast that is currently running
            goCoderBroadcaster!!.endBroadcast(this)
        } else {
            // Start streaming
            goCoderBroadcaster!!.startBroadcast(goCoderBroadcastConfig, this)
        }
    }

    companion object {

        // Properties needed for Android 6+ permissions handling
        private val PERMISSIONS_REQUEST_CODE = 0x1

        //
        // Utility method to check the status of a permissions request for an array of permission identifiers
        //
        private fun hasPermissions(context: Context, permissions: Array<String>): Boolean {
            for (permission in permissions)
                if (context.checkCallingOrSelfPermission(permission) != PackageManager.PERMISSION_GRANTED)
                    return false

            return true
        }
    }


}
