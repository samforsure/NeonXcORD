package mods.preference;

import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.preference.Preference;
import android.util.AttributeSet;

import mods.dialog.Dialogs;
import mods.utils.ToastUtil;

/**
 * NeonX Cord — Token Extractor (Settings entry)
 *
 * Appears in Settings → NeonX Tools.
 * Tapping it:
 *   1. Checks clipboard for a Discord user-token pattern immediately (no delay).
 *   2. If found → shows the token in a dialog for easy copy.
 *   3. If not found → shows the step-by-step extraction guide.
 */
@SuppressWarnings("deprecation")
public class TokenExtractorPreference extends Preference {

    // Discord user token regex: three dot-separated Base64url segments
    // Format:  [base64(userId)] . [timestamp] . [HMAC]
    private static final String TOKEN_REGEX =
            "[A-Za-z0-9+/=]{20,}\\.[A-Za-z0-9_\\-]+\\.[A-Za-z0-9_\\-]+";

    public TokenExtractorPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TokenExtractorPreference(Context context) {
        super(context);
    }

    @Override
    protected void onClick() {
        extractAndShow(getContext());
    }

    // ────────────────────────────────────────────────────────────────────
    //  Core extraction logic — runs instantly, zero delay
    // ────────────────────────────────────────────────────────────────────
    public static void extractAndShow(Context context) {
        String found = getTokenFromClipboard(context);

        if (found != null) {
            // Token detected in clipboard → show it
            Dialogs.newBuilder(context)
                    .setTitle("🔴 Token Found in Clipboard")
                    .setMessage(
                            "A Discord user token was detected in your clipboard:\n\n" +
                                    maskToken(found) + "\n\n" +
                                    "Use 'Copy Full Token' to copy it, then tap " +
                                    "'Login With Token' on the login screen to log in.")
                    .setPositiveButton("Copy Full Token", (d, w) -> {
                        copyToClipboard(context, found);
                        ToastUtil.toastShort("Token copied to clipboard!");
                    })
                    .setNegativeButton("Close")
                    .showSafely();
        } else {
            // No token in clipboard → show extraction guide
            showGuide(context);
        }
    }

    // ────────────────────────────────────────────────────────────────────
    //  Clipboard scanner — no delays, no background threads needed
    // ────────────────────────────────────────────────────────────────────
    private static String getTokenFromClipboard(Context context) {
        try {
            ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm == null || !cm.hasPrimaryClip()) return null;
            android.content.ClipData clip = cm.getPrimaryClip();
            if (clip == null || clip.getItemCount() == 0) return null;
            CharSequence text = clip.getItemAt(0).getText();
            if (text == null) return null;
            String candidate = text.toString().trim();
            if (candidate.matches(TOKEN_REGEX)) return candidate;
        } catch (Throwable ignored) { }
        return null;
    }

    /** Masks all but the first 10 and last 4 chars so it's not fully exposed on screen */
    private static String maskToken(String token) {
        if (token.length() <= 14) return "***";
        return token.substring(0, 10) +
                "*".repeat(token.length() - 14) +
                token.substring(token.length() - 4);
    }

    private static void copyToClipboard(Context context, String text) {
        try {
            ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(android.content.ClipData.newPlainText("NeonX Token", text));
            }
        } catch (Throwable ignored) { }
    }

    // ────────────────────────────────────────────────────────────────────
    //  Step-by-step token extraction guide
    // ────────────────────────────────────────────────────────────────────
    private static void showGuide(Context context) {
        Dialogs.basicAlert(context,
                "🔴 Token Extractor — How to get your token",
                "No token was detected in your clipboard yet.\n\n" +
                        "Follow ONE of these methods, then come back here:\n\n" +

                        "━━━━━━━━━━━━━━━━━━━━\n" +
                        "METHOD 1 — PC Browser (Easiest)\n" +
                        "━━━━━━━━━━━━━━━━━━━━\n" +
                        "1. Open discord.com in Chrome / Firefox\n" +
                        "2. Log in with your account\n" +
                        "3. Press F12 (or Ctrl+Shift+I) → open DevTools\n" +
                        "4. Click the 'Network' tab\n" +
                        "5. Reload the page (F5)\n" +
                        "6. Click any request that goes to 'discord.com/api'\n" +
                        "7. Scroll down in 'Headers' → find 'authorization'\n" +
                        "8. Copy that value — it is your token!\n\n" +

                        "━━━━━━━━━━━━━━━━━━━━\n" +
                        "METHOD 2 — Application Storage (PC)\n" +
                        "━━━━━━━━━━━━━━━━━━━━\n" +
                        "1. Open DevTools (F12) on discord.com\n" +
                        "2. Go to: Application → Local Storage\n" +
                        "   → https://discord.com\n" +
                        "3. Find the key named 'token'\n" +
                        "4. Copy its value (without the quotes)\n\n" +

                        "━━━━━━━━━━━━━━━━━━━━\n" +
                        "AFTER COPYING\n" +
                        "━━━━━━━━━━━━━━━━━━━━\n" +
                        "• Copy the token to your clipboard\n" +
                        "• Return here — it will be detected automatically\n" +
                        "• OR go to the login screen → tap 'Login With Token'\n" +
                        "  and paste it there\n\n" +

                        "⚠️  Use your USER token only.\n" +
                        "    Bot tokens will be rejected.\n" +
                        "🔒  NeonX Cord never transmits your token externally."
        );
    }
}
