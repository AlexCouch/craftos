package network

import net.minecraft.nbt.NBTBase
import system.CouchDesktopSystem

interface Router{
    val connectedDevices: ArrayList<CouchDesktopSystem>
    val bandwidth: Int
    val routerConnection: RouterConnection

    fun sendData(data: NBTBase)
    fun validateDataConnection(): Boolean
}

class RouterConnection{
    fun sendData(device1: CouchDesktopSystem, device2: CouchDesktopSystem){

    }
}