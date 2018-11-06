Messages Wrapper
======================
This simple messages wrapper allows you to customize and manage your network messages 
without needing to create and register a message and a handler. It also handles the sided programming for you.
The use is very simple. When you first use it, it can get kinda clunky but it's worthwhile 
cause your sided code is right next to each other in the same scope, and you're guaranteed your code
will be executed on the side you need them to be. Here's how it works:

First, you create a function type (aka, an anonymous function stored as an object) that prepares data to be sent and processed by
your targeted side. This is where you're gonna package data that you need to send into an NBTTagCompound. This will then be passed
into another function type called `processData` (upon invocation in the message's handler)
and that's where you tell minecraft what to do with that data.

An example of this would be terminal commands. Typing in a command in the gui that is then sent to the terminal to be executed
will take the name of the command and the arguments and package them into an NBTTagCompound in the prepareData function type.
Then, in the processData function type, you unpack the NBTTagCompound and do what you need to do. You are also passed the instance
of the current side's world, the tile entity's BlockPos, as well as the player instance of the current side. 
So if you're processing data on server side, you're given the server's current world, the tile entity's blockpos, and the current instance
of the player as EntityPlayerMP. Processing data on client side will yield you the client's world instance 
and the player instance as EntityPlayerSP. The handlers invoke your processData function type inside an IThreadListener#addScheduledTask
call. For client side, it's done inside `Minecraft.getMinecraft().addScheduledTask(lambda)`. 
For servers it's `WorldServer#addScheduledTask`.

When creating your processData function type, make sure you use the arguments supplied by the lambda rather than capturing them
outside the lambda. This will cause side related issues since not everything you do on the client is gonna be visible on the server,
escpecially if they're local. In fact, everything you're gonna be doing in processData is gonna mostly require the arguments supplied.
If you need access to the system, then there is a helper function provided on common side that gives you the current computer, 
yielded as a TileEntityDesktopComputer, based on the world, blockpos, and the player you're given in the processData 
lambda. The player is needed because when you get the desktop tile entity, the player is null, so you're
gonna need to provide the player that you are using, which will then be shared with the CouchDesktopSystem instance, which is then
accessible by the CouchOperatingSystem instance and then by everything else.

Responsive Messages
-----------------
A Responsive Message is a message that is sent to one side and yields a message back to the original side. A Responsive Server Message
is a message that is sent to the server and yields a message back to the client. In order for this to happen, you need to provide
function types that tell the wrapper what data to be sent back to the original side and how that data should be processed.
Just like explained above, just for the original side, where prepareMessageData function type is going to be done on the sending side,
and processMessageData will be done on the receiving side. Afterwards, the prepareResponseData is gonna be done on the receiving side,
and the processResponseData will be done on the sending side. For example, if I send a responsive message from client to server, 
I will need to package data to be sent on the client and process it on the server. Then I'll need to provide details about data being
packaged on the server to be processed on the client.

Code examples
-------------------
Terminal Command Execution From Client to Server

```Kotlin
override fun sendCommand(commandName: String, commandArgs: Array<String>){
    val prepareData: () -> NBTTagCompound = { //Executed Client side
        val nbt = NBTTagCompound()
        nbt.setString("name", commandName)
        val argsList = NBTTagList()
        for(a in commandArgs){
            argsList.appendTag(NBTTagString(a))
        }
        nbt.setTag("args", argsList)
        nbt //This is returning to the lambda the nbt tag
    }
    //ProcessData is a typealias of (NBTTagCompound, World, BlockPos, EntityPlayer) -> Unit
    val processData: ProcessData = { data, world, pos, player -> //Executed Server side
        if(data.hasKey("name") && data.hasKey("args")){
            val name = data.getString("name")
            //This is a helper function for immediately getting the current computer based on the current side
            val te = getCurrentComputer(world, pos, player)!!
            val argsList = data.getTagList("args", Constants.NBT.TAG_STRING)
            val args = arrayListOf<String>()
            for(a in argsList){
                val str = (a as NBTTagString).string
                args += str
            }
            val terminal = te.system.os?.terminal!!
            val command = terminal.getCommand(name)
            terminal.executeCommand(player as EntityPlayerMP, command, args.toTypedArray())
        }
    }
    MessageFactory.sendDataToServer(this.os.screen!!.te.pos, prepareData, processData)
}
```
Starting the Terminal from Gui Screen Responsive Client to Server message

```Kotlin
//This is called from the boot screen upon the user pressing enter when prompted
private fun startTerminal(){
    val prepareMessageData = { NBTTagCompound() } //Note how I don't even send data
    val processMessageData: ProcessData = { _, world, pos, player -> //Executed Server Side
        val comp = getCurrentComputer(world, pos, player)!!
        val terminal = comp.system.os!!.terminal
        terminal.start(player as EntityPlayerMP)
    }
    val prepareResponseData = { NBTTagCompound() } //And again, not even sending data, just provoking sided execution
    val processResponseData: ProcessData = { _, world, pos, player ->
        player.openGui(DevicesPlus, 0, world, pos.x, pos.y, pos.z) //Executed on Client Side
    }
    MessageFactory.sendDataToServerWithResponse(
            this.system.desktop.pos,
            prepareMessageData,
            processMessageData,
            prepareResponseData,
            processResponseData
    )
}
```
