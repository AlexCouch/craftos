package pkg

import network.NetworkPort
import network.Port
import os.OperatingSystem

interface Package{
    val name: String
    val version: String
    fun func(os: OperatingSystem, args: ArrayList<String>)
}

class NetworkingPackage : Package{
    override val name: String
        get() = "cnetman"
    override val version: String
        get() = "1.0"

    override fun func(os: OperatingSystem, args: ArrayList<String>) {
        if(args.size == 1){
            val name = args[0]
            val nextPort = os.getNextAvailablePort(NetworkPort::class.java) as NetworkPort
            nextPort.start()
            nextPort.connect(name)
        }
    }

}