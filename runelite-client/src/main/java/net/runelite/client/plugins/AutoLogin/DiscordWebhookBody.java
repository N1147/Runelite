package net.runelite.client.plugins.AutoLogin;

import lombok.Data;

@Data
class DiscordWebhookBody
{
    private String content;
    private Embed embed;

    @Data
    static class Embed
    {
        final UrlEmbed image;
    }

    @Data
    static class UrlEmbed
    {
        final String url;
    }
}