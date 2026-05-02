package mods.onboarding

import mods.utils.LogUtils
import mods.utils.StoreUtils
import mods.extensions.OkHttpClient
import mods.extensions.RequestBuilder
import mods.extensions.build
import mods.extensions.enqueue
import mods.extensions.setHeader
import mods.extensions.url
import mods.extensions.post
import mods.extensions.toRequestBody
import mods.preference.Prefs
import mods.constants.PreferenceKeys
import org.json.JSONArray
import org.json.JSONObject

/**
 * NeonX Cord — Server Onboarding Auto-Select
 *
 * When a user joins a server that has onboarding enabled,
 * this module automatically selects all default onboarding options
 * and submits the onboarding survey, so the user skips manual setup.
 *
 * Hooked from the smali-patched WidgetServerOnboarding / guild JOIN event.
 */
object OnboardingAutoSelect {

    private const val TAG = "OnboardingAutoSelect"

    /** Master toggle — can be toggled from NeonX settings */
    var enabled: Boolean
        get() = Prefs.getBoolean(PreferenceKeys.NEONX_AUTO_ONBOARDING, true)
        set(value) = Prefs.setBoolean(PreferenceKeys.NEONX_AUTO_ONBOARDING, value)

    /**
     * Called when the app detects a GUILD_CREATE or onboarding prompt event.
     *
     * @param guildId  The Discord guild (server) snowflake ID
     * @param onboardingData  Raw JSON from the Discord API GET /guilds/{id}/onboarding
     */
    fun handleOnboarding(guildId: Long, onboardingData: JSONObject?) {
        if (!enabled) {
            LogUtils.log(TAG, "Auto-onboarding disabled, skipping guild $guildId")
            return
        }

        try {
            val prompts = onboardingData?.optJSONArray("prompts") ?: return

            val selectedOptions = JSONArray()

            for (i in 0 until prompts.length()) {
                val prompt = prompts.getJSONObject(i)
                val options = prompt.optJSONArray("options") ?: continue
                val required = prompt.optBoolean("required", false)
                val singleSelect = prompt.optBoolean("single_select", false)

                if (options.length() == 0) continue

                if (singleSelect) {
                    // Pick the first (default) option
                    val firstOption = options.getJSONObject(0)
                    selectedOptions.put(firstOption.getString("id"))
                    LogUtils.log(TAG, "Auto-selected single option: ${firstOption.optString("title")} for guild $guildId")
                } else {
                    // Select ALL options for multi-select prompts
                    for (j in 0 until options.length()) {
                        val option = options.getJSONObject(j)
                        selectedOptions.put(option.getString("id"))
                        LogUtils.log(TAG, "Auto-selected option: ${option.optString("title")} for guild $guildId")
                    }
                }
            }

            submitOnboarding(guildId, selectedOptions)

        } catch (e: Throwable) {
            LogUtils.log(TAG, "Error during auto-onboarding for guild $guildId", e)
        }
    }

    /**
     * Fetches onboarding data for the guild from Discord's API,
     * then processes + submits automatically.
     *
     * This is the entry point triggered when a user joins a server.
     */
    fun autoCompleteOnboarding(guildId: Long) {
        if (!enabled) return

        try {
            val token = StoreUtils.getAuthToken() ?: return
            val client = OkHttpClient()

            // Step 1: Fetch onboarding prompt data
            client.newCall(
                RequestBuilder().apply {
                    url("https://discord.com/api/v10/guilds/$guildId/onboarding")
                    setHeader("Authorization", token)
                    setHeader("Content-Type", "application/json")
                    setHeader("User-Agent", mods.rn.ReactNativeSpoof.userAgent())
                }.build()
            ).enqueue({ (_, response) ->
                try {
                    val body = response.body?.string() ?: return@enqueue
                    if (response.code == 200) {
                        val data = JSONObject(body)
                        handleOnboarding(guildId, data)
                    } else if (response.code == 404) {
                        LogUtils.log(TAG, "Guild $guildId has no onboarding, skipping")
                    } else {
                        LogUtils.log(TAG, "Onboarding fetch failed (${response.code}) for $guildId")
                    }
                } catch (e: Throwable) {
                    LogUtils.log(TAG, "Error parsing onboarding response", e)
                }
            }, { (_, e) ->
                LogUtils.log(TAG, "Network error fetching onboarding for $guildId", e)
            })
        } catch (e: Throwable) {
            LogUtils.log(TAG, "autoCompleteOnboarding failed", e)
        }
    }

    /**
     * PUT /guilds/{guildId}/onboarding-responses
     * Submits the selected onboarding option IDs to Discord.
     */
    private fun submitOnboarding(guildId: Long, selectedOptionIds: JSONArray) {
        if (selectedOptionIds.length() == 0) {
            LogUtils.log(TAG, "No options to submit for guild $guildId")
            return
        }

        try {
            val token = StoreUtils.getAuthToken() ?: return
            val client = OkHttpClient()

            val payload = JSONObject().apply {
                put("onboarding_responses", selectedOptionIds)
                put("onboarding_prompts_seen", selectedOptionIds)
            }

            client.newCall(
                RequestBuilder().apply {
                    url("https://discord.com/api/v10/guilds/$guildId/onboarding-responses")
                    setHeader("Authorization", token)
                    setHeader("Content-Type", "application/json")
                    setHeader("User-Agent", mods.rn.ReactNativeSpoof.userAgent())
                    post(payload.toString().toRequestBody())
                }.build()
            ).enqueue({ (_, response) ->
                if (response.code in 200..204) {
                    LogUtils.log(TAG, "✅ Auto-onboarding complete for guild $guildId")
                } else {
                    LogUtils.log(TAG, "❌ Onboarding submit failed (${response.code}) for $guildId")
                }
            }, { (_, e) ->
                LogUtils.log(TAG, "Network error submitting onboarding for $guildId", e)
            })
        } catch (e: Throwable) {
            LogUtils.log(TAG, "submitOnboarding failed", e)
        }
    }
}
