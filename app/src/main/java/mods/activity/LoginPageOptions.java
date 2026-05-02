package mods.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.discord.app.AppActivity;
import com.discord.app.AppFragment;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import mods.DiscordTools;
import mods.constants.PreferenceKeys;
import mods.dialog.Dialogs;
import mods.preference.AccountSwitcher;
import mods.preference.Prefs;
import mods.utils.AuthenticationUtils;
import mods.utils.ToastUtil;
import mods.utils.TokenChecker;

/**
 * NeonX Cord — Login Page
 *
 * Handles:
 *  - Auto token restore on launch (from temp prefs)
 *  - Token Login button → shows a clean dialog for REAL USER tokens
 *  - Proxy Settings button
 *  - Account Restore button
 *
 * Token Extractor lives in Settings → NeonX Tools (see prefs_neonx.xml)
 */
public class LoginPageOptions {

    @SuppressLint("ApplySharedPref")
    public static void init(final AppFragment fragment) {

        // ── Auto token restore (called when app re-launches after token login) ──
        if (Prefs.getBoolean(PreferenceKeys.TEMP_LOGIN_WITH_TOKEN, false)) {
            String token = Prefs.getString(PreferenceKeys.TEMP_TOKEN, null);
            String fingerprint = Prefs.getString(PreferenceKeys.TEMP_FINGERPRINT, null);

            if (token != null) {
                Prefs.getPreferences()
                        .edit()
                        .putString("STORE_AUTHED_TOKEN", token)
                        .putBoolean(PreferenceKeys.WAS_TOKEN_LOGIN, true)
                        .putBoolean(PreferenceKeys.TEMP_NEEDS_GCM_TOKEN, true)
                        .commit();

                if (fingerprint != null) {
                    Prefs.getDurablePrefs()
                            .edit()
                            .putString("STORE_AUTHED_FINGERPRINT", fingerprint)
                            .commit();
                    FirebaseCrashlytics.getInstance().setCustomKey("fingerprint", fingerprint);
                }
                Prefs.removeValues(
                        PreferenceKeys.TEMP_LOGIN_WITH_TOKEN,
                        PreferenceKeys.TEMP_TOKEN,
                        PreferenceKeys.TEMP_FINGERPRINT);
                DiscordTools.restartDiscord(fragment.requireContext());
                return;
            }
        }

        View view = fragment.getView();
        if (view == null) return;

        // ── Account Restore button ──
        View accountRestoreButton = view.findViewById(android.R.id.secondaryProgress);
        if (accountRestoreButton != null) {
            accountRestoreButton.setOnClickListener(v ->
                    AccountSwitcher.restoreBackup(fragment.getContext()));
        }

        // ── Proxy Settings button ──
        View proxySettingsButton = view.findViewById(android.R.id.switchInputMethod);
        if (proxySettingsButton != null) {
            proxySettingsButton.setOnClickListener(v ->
                    fragment.startActivity(new Intent(fragment.getActivity(), BlueSettingsActivity.class)
                            .putExtra(BlueSettingsActivity.EXTRA_PREF_KEY, com.bluecord.R.xml.prefs_proxy)
                            .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)));
        }

        // ── Token Login button (REAL USER tokens only) ──
        View restoreTokenButton = view.findViewById(android.R.id.primary);
        if (restoreTokenButton == null) return;

        restoreTokenButton.setOnClickListener(v -> showTokenLoginDialog(fragment));
    }

    // ────────────────────────────────────────────────────────────────────
    //  Token Login Dialog — real user tokens, no bot tokens
    // ────────────────────────────────────────────────────────────────────
    private static void showTokenLoginDialog(final AppFragment fragment) {
        final EditText input = new EditText(fragment.requireContext());
        input.setHintTextColor(Color.parseColor("#FF4444"));
        input.setTextColor(Color.WHITE);
        input.setHint("Paste your Discord user token here…");
        input.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));

        Dialogs.newBuilder(fragment.requireContext())
                .setTitle("Login With Token")
                .setView(input)
                .setNegativeButton("Cancel")
                .setNeutralButton("How to get token", (d, w) ->
                        Dialogs.basicAlert(
                                fragment.requireContext(),
                                "How to get your token",
                                "Your Discord token lets you log in without a password.\n\n" +
                                        "HOW TO FIND IT:\n\n" +
                                        "On PC (browser):\n" +
                                        "1. Open Discord in your browser\n" +
                                        "2. Press F12 to open DevTools\n" +
                                        "3. Go to the Network tab\n" +
                                        "4. Look for any request to discord.com/api\n" +
                                        "5. Click it → Headers → find 'authorization'\n" +
                                        "   That value is your token.\n\n" +
                                        "On Android (Chrome):\n" +
                                        "1. Open discord.com in Chrome\n" +
                                        "2. Enable Desktop mode (⋮ → Desktop site)\n" +
                                        "3. DevTools is not available on Android Chrome directly —\n" +
                                        "   use a PC to grab the token first.\n\n" +
                                        "⚠️  This must be a USER token (your own account).\n" +
                                        "    Bot tokens will NOT work.\n\n" +
                                        "🔒  Never share your token. NeonX Cord never\n" +
                                        "    uploads, logs, or stores it externally.\n\n" +
                                        "💡  Tip: Go to Settings → NeonX Tools for the\n" +
                                        "    built-in Token Extractor guide."
                        ))
                .setPositiveButton("Login", (d, w) -> {
                    final AppActivity activity = fragment.getAppActivity();
                    // Strip anything that's not a valid token character
                    final String token = input.getText().toString().trim()
                            .replaceAll("[^A-Za-z0-9\\-\\_\\.\\+]", "");

                    if (token.isEmpty()) {
                        ToastUtil.toastShort("Token cannot be empty");
                        return;
                    }

                    // Basic sanity check: Discord user tokens have 3 dot-separated segments
                    if (!token.contains(".") || token.split("\\.").length < 3) {
                        ToastUtil.toastShort("That doesn't look like a valid user token");
                        return;
                    }

                    // Validate the token against Discord's API (no delay, instant network call)
                    TokenChecker.check(activity, token).subscribe(result -> {
                        switch (result) {
                            case OK:
                                AuthenticationUtils.restoreToken(activity, token);
                                break;
                            case INVALID_NO_CONNECTION:
                                ToastUtil.toast("No internet connection. Check and try again.");
                                break;
                            case INVALID_NOT_AUTHORIZED:
                                ToastUtil.toast("Invalid token — not authorized by Discord.\n" +
                                        "Make sure you copied a USER token, not a bot token.");
                                break;
                        }
                    });
                })
                .showSafely();
    }
}
