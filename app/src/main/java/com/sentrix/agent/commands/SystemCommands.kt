package com.sentrix.agent.commands

import android.content.Context
import android.hardware.camera2.CameraManager
import com.sentrix.agent.util.RootUtil

object SystemCommands {

    fun torchOn(ctx: Context): String {
        return try {
            val cm = ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val id = cm.cameraIdList.firstOrNull() ?: return "No camera"
            cm.setTorchMode(id, true)
            "Flashlight ON"
        } catch (e: Exception) { "Flashlight error: ${e.message}" }
    }

    fun torchOff(ctx: Context): String {
        return try {
            val cm = ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val id = cm.cameraIdList.firstOrNull() ?: return "No camera"
            cm.setTorchMode(id, false)
            "Flashlight OFF"
        } catch (e: Exception) { "Flashlight error: ${e.message}" }
    }

    fun goHome(): String {
        if (!RootUtil.isRooted()) return "Home button requires root"
        return if (RootUtil.run("input keyevent 3")) "Home screen" else "Failed"
    }

    fun pressBack(): String {
        if (!RootUtil.isRooted()) return "Back button requires root"
        return if (RootUtil.run("input keyevent 4")) "Back pressed" else "Failed"
    }

    fun collapseStatusBar(): String {
        if (!RootUtil.isRooted()) return "Status bar collapse requires root"
        return if (RootUtil.run("cmd statusbar collapse")) "Status bar collapsed" else "Failed"
    }
}
