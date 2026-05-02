package com.discord.models.guild;

import com.discord.api.emoji.GuildEmoji;
import com.discord.api.emoji.GuildExplicitContentFilter;
import com.discord.api.guild.GuildFeature;
import com.discord.api.guild.GuildHubType;
import com.discord.api.guild.GuildMaxVideoChannelUsers;
import com.discord.api.guild.GuildVerificationLevel;
import com.discord.api.guild.welcome.GuildWelcomeScreen;
import com.discord.api.role.GuildRole;
import com.discord.api.sticker.Sticker;

import java.util.List;
import java.util.Set;

public class Guild {

    public Long afkChannelId;
    public int afkTimeout;
    public int approximatePresenceCount;
    public String banner;
    public int defaultMessageNotifications;
    public String description;
    public List<GuildEmoji> emojis;
    public GuildExplicitContentFilter explicitContentFilter;
    public Set<GuildFeature> features;
    public GuildHubType hubType;
    public String icon;
    public long id;
    public String joinedAt;
    public GuildMaxVideoChannelUsers maxVideoChannelUsers;
    public int memberCount;
    public int mfaLevel;
    public String name;
    public boolean nsfw;
    public long ownerId;
    public String preferredLocale;
    public int premiumSubscriptionCount;
    public int premiumTier;
    public Long publicUpdatesChannelId;
    public String region;
    public List<GuildRole> roles;
    public Long rulesChannelId;
    public String shortName;
    public String splash;
    public List<Sticker> stickers;
    public int systemChannelFlags;
    public Long systemChannelId;
    public boolean unavailable;
    public String vanityUrlCode;
    public GuildVerificationLevel verificationLevel;
    public GuildWelcomeScreen welcomeScreen;

}
