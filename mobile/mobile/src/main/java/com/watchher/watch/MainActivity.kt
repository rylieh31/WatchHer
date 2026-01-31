package com.watchher.watch

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.wearable.ChannelClient
import com.google.android.gms.wearable.Wearable

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        var channelClient: ChannelClient?

        val nodeClient = Wearable.getNodeClient(this)
        nodeClient.connectedNodes.addOnSuccessListener { nodes ->
            for (node in nodes) {
                Log.d("WEAR", "Found node: ${node.displayName} (${node.id})")

                val nodeId = node.id
                val path = "/watchher"

                channelClient = Wearable.getChannelClient(this)
                channelClient.openChannel(nodeId, path)
                .addOnSuccessListener { channel ->
                    Log.d("WEAR", "Channel opened: ${channel.path}")
                }
                .addOnFailureListener { e ->
                    Log.e("WEAR", "Failed to open channel", e)
                }
            }
        }
    }
}