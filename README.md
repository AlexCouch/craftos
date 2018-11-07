CraftOS Minecraft In-Game Operating System
=========================================
This is a realistic operating system that I am making inside Minecraft. <br>
The purpose of this is really just for fun.<br>
I like the idea of operating systems as well as Minecraft mods<br>
and I thought 'would it be possible to make realistic oses inside Minecraft? So I did.

Current Implementations:
  * Terminal (with commands of course)
  * File System
  * Basic Network Message Wrapper
  * Basic Separation of System-OS-Terminal

Planned Implementations:
  * Package Manager
  * Realistic Hardware
  * Text Editor
  * A realistic shell (with shell scripting)
  * Ports (for usb, visual, audio, and networking)
  * Audio/Visual Hardware to connect to computer
  * Realistic Servers (including GET/POST requests)
  * And More

Running
========================
You can really only run this inside of a development environment for now. 
There's not much you can do with it so why allow people to install it and play with it?
If you wanna test it, you can just clone this repo into a local repo and open the project via build.gradle.
If you're on eclipse, I don't really know what to tell you. Please use Intellij cause the way this project
is setup uses Intellij and also you can easily import this project with the gradle script. Do your normal
mod setup by running setupDecompWorkspace and genIntellijRuns (or eclipse but please use Intellij).

Contributing
=======================
If you would like to contribute, I have some rules:
  * Please use Kotlin. If you ABSOLUTELY CANNOT USE KOTLIN THEN I WILL CONVERT ALL NON-KOTLIN CODE TO KOTLIN
  * Please check with me on what could be done. I don't want anybody making big changes that I can't follow
  * Always work within your own branch. Just as the os stuff is inside operating_system branch and will be merged into master.
If you cannot follow these rules, I will reject your pull requests. 
I know it seems harsh but I have to do this to make sure nothing breaks and everything is consistent. 
