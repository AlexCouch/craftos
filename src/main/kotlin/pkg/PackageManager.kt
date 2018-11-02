package pkg

import net.minecraft.entity.player.EntityPlayerMP
import network.NetworkPort
import os.OperatingSystem
import terminal.Terminal
import utils.printstr

interface Package{
    val name: String
    val version: String
    fun func(player: EntityPlayerMP, os: OperatingSystem, args: ArrayList<String>)
}

class PackageManager(val terminal: Terminal){
    val availablePackages = hashSetOf<Package>()
    val installedPackages = hashSetOf<Package>()

    init{
//        availablePackages += NetworkingPackage
//        installedPackages += NetworkingPackage
    }

    fun installPackage(packname: String){
        if(availablePackages.stream().anyMatch { it.name == packname }){
            val pack = availablePackages.stream().filter { it.name == packname }.findFirst().get()
            if(installedPackages.stream().anyMatch { it.name == pack.name }){
                printstr("That package is already installed: $packname", this.terminal)
                return
            }
            this.installedPackages += pack
            printstr("Package '$packname' has been installed.")
            return
        }
        printstr("There is no such package with name: '$packname'")
    }
}

object NetworkingPackage : Package{
    override val name: String
        get() = "cnetman"
    override val version: String
        get() = "1.0"

    override fun func(player: EntityPlayerMP, os: OperatingSystem, args: ArrayList<String>) {
        if(args.size == 1){
            if(args[0] == "list"){
                val ports = os.ports
                printstr("Found Ports:")
                ports.forEach {
                    printstr("\tId: ${it.portId}", os.terminal)
                    printstr("\tAvailable: ${it.available}", os.terminal)
                    printstr("\tPort Type: ${it.t?.javaClass?.name}", os.terminal)
                    printstr("", os.terminal)
                }
            }else if(args[0] == "-c"){
                printstr("Attempting connection with network device ${args[1]}", os.terminal)
                val name = args[1]
                printstr("Getting the next available network port...")
                val nextPort = os.getNextAvailablePort(NetworkPort::class.java) as? NetworkPort ?: return
                printstr("Starting port...", os.terminal)
                nextPort.start()
                printstr("Attempting connection with port to network device $name...")
                nextPort.connect(name)
            }
        }
    }

}