package programs

import net.minecraft.nbt.NBTTagCompound
import os.OperatingSystem
import os.couch.CouchOS

abstract class Program{
    /**
     * Will be added to a resource location
     * os_name:programs/modid/name
     */
    abstract val name: String
    /**
     * An optional renderer for desktop environments; must have an os desktop environment (renderer)
     *
     * This will need a lambda returned if you're using it
     */
    abstract val renderer: ProgramRenderer?
    /**
     * Whether this is a command line interface; true will not start the renderer
     */
    abstract val isCLI: Boolean
    /**
     * What the program does. For java users, this takes a lambda.
     * When you implement this, you're going to return a lambda that takes in a [terminal.Terminal] and [os.OperatingSystem]
     * as parameters, so you have everything you need.
     *
     * These two objects allow you to communicate with the operating system by executing commands on the os, such as
     * file io, network messages, serializing/deserializing (saving data), device communication (usb, bluetooth, etc),
     * and whatever else you may need to do on the os.
     *
     * This is specifically an easier way to communicate with the server for doing things on the os without causing
     * problems.
     *
     * Java Impl:
     * @sample TestProgram.function
     *
     */
    abstract val function: ProgramFunction

    /**
     * Use this to initialize fields/properties
     *
     * When overriding, calling this super method always!!!! or else it will not work
     */
    abstract fun init()

    /**
     * This will be called upon system shutdown
     */
    abstract fun shutdown()

    /**
     * This will be called during program initialization
     *
     * The nbt tag is taken from storage
     */
    abstract fun deserialize(nbt: NBTTagCompound)

    /**
     * This will be called during program shutdown
     */
    abstract fun serialize(): NBTTagCompound
}

interface ProgramRenderer{
    fun render(coords: RenderCoords, os: OperatingSystem)
}

data class RenderCoords(val x: Int, val y: Int, val mx: Int, val my: Int)

interface ProgramFunction{
    fun execute(os: CouchOS): Boolean
}