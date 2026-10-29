package com.sentrix.agent.util

import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Handles root detection and privileged shell command execution.
 * All root-gated features check RootUtil.isRooted() before running.
 */
object RootUtil {

    private const val TAG = "RootUtil"

    /**
     * Checks whether the device has a usable root shell.
     * Tries to run `id` via `su` and checks for uid=0.
     */
    fun isRooted(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val output  = process.inputStream.bufferedReader().readText()
            process.waitFor()
            output.contains("uid=0")
        } catch (e: Exception) {
            Log.d(TAG, "Root check failed: ${e.message}")
            false
        }
    }

    /**
     * Executes a shell command as root.
     * @return Pair(stdout, success)
     */
    fun exec(command: String): Pair<String, Boolean> {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            val stdout  = BufferedReader(InputStreamReader(process.inputStream)).readText().trim()
            val stderr  = BufferedReader(InputStreamReader(process.errorStream)).readText().trim()
            val exit    = process.waitFor()

            if (exit == 0) {
                Pair(stdout, true)
            } else {
                Log.w(TAG, "Command '$command' exited $exit: $stderr")
                Pair(stderr, false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "exec failed: ${e.message}")
            Pair(e.message ?: "unknown error", false)
        }
    }

    /**
     * Fire-and-forget root command — no output needed.
     */
    fun run(command: String): Boolean = exec(command).second
}
