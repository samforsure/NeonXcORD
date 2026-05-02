package mods.rpc

import mods.constants.PreferenceKeys
import mods.preference.Prefs
import mods.utils.LogUtils
import mods.utils.StoreUtils
import org.json.JSONObject

/**
 * NeonX Cord — Custom Rich Presence (RPC) Module
 *
 * Sets a custom Discord Rich Presence for token-login users.
 * Application ID: 1499942273130041406
 */
object NeonXRPC {

    private const val TAG = "NeonXRPC"

    /** Discord application ID for NeonX Cord RPC */
    const val APPLICATION_ID = "1499942273130041406"

    /** Whether to broadcast our custom RPC */
    var enabled: Boolean
        get() = Prefs.getBoolean(PreferenceKeys.NEONX_RPC_ENABLED, true)
        set(value) = Prefs.setBoolean(PreferenceKeys.NEONX_RPC_ENABLED, value)

    /**
     * Build the activity payload that Discord's gateway expects when
     * setting a custom rich presence.
     *
     * Sent as an `UPDATE_PRESENCE` (opcode 3) gateway message.
     */
    fun buildPresencePayload(): JSONObject {
        return JSONObject().apply {
            put("since", System.currentTimeMillis())
            put("status", "online")
            put("afk", false)
            put("activities", org.json.JSONArray().apply {
                put(buildActivity())
            })
        }
    }

    private fun buildActivity(): JSONObject {
        val username = try {
            StoreUtils.getSelf()?.username ?: "User"
        } catch (e: Throwable) {
            "User"
        }

        return JSONObject().apply {
            put("type", 0)                          // Playing
            put("application_id", APPLICATION_ID)
            put("name", "NeonX Cord")
            put("details", "🔴 Custom Client")
            put("state", "Logged in as $username")
            put("timestamps", JSONObject().apply {
                put("start", System.currentTimeMillis())
            })
            put("assets", JSONObject().apply {
                put("large_image", "neonx_logo")
                put("large_text", "NeonX Cord — Premium Experience")
                put("small_image", "neonx_small")
                put("small_text", "Token Login Active")
            })
            put("buttons", org.json.JSONArray().apply {
                put("NeonX Cord")
            })
            put("metadata", JSONObject().apply {
                put("button_urls", org.json.JSONArray().apply {
                    put("https://discord.gg/saiyaara")
                })
            })
        }
    }

    /**
     * Called when we want to push our RPC to the gateway.
     * Hook into GatewaySocket or call directly after identification.
     */
    fun applyPresence(gateway: Any?) {
        if (!enabled) return
        if (!Prefs.getBoolean(PreferenceKeys.WAS_TOKEN_LOGIN, false) &&
            !Prefs.getBoolean(PreferenceKeys.NEONX_RPC_ALWAYS, false)) return
        try {
            val payload = buildPresencePayload()
            LogUtils.log(TAG, "Sending NeonX RPC presence: $payload")
            // Actual gateway send is hooked via smali patch in GatewaySocket
        } catch (e: Throwable) {
            LogUtils.log(TAG, "Failed to apply NeonX RPC", e)
        }
    }
}
