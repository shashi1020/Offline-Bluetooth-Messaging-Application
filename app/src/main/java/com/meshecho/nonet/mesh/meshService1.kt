package com.meshecho.nonet.mesh

import com.google.gson.Gson
import com.meshecho.nonet.model.EchoPacket
import com.meshecho.nonet.model.RoutedPacket
import com.meshecho.nonet.protocol.BitchatPacket

class MeshService1(
    private val connectionManager: BluetoothConnectionManager,
    val myPeerID: String // store it here instead of a lambda if easier
) {
    private val gson = Gson()

    fun sendPacket(packet: EchoPacket) {
        val json = gson.toJson(packet)

        val bitchatPacket = BitchatPacket(
            type = 1u,
            ttl = 5u,
            senderID = myPeerID,
            payload = json.toByteArray()
        )

        val routedPacket = RoutedPacket(bitchatPacket)
        connectionManager.broadcastPacket(routedPacket)
    }
}