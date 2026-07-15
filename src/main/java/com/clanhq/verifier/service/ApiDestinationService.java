package com.clanhq.verifier.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import javax.inject.Inject;

public final class ApiDestinationService
{
    @Inject
    public ApiDestinationService()
    {
    }

    public String describe(String configuredUrl, String clanCode)
    {
        String normalized = normalize(configuredUrl);
        if (normalized == null)
        {
            return configuredUrl == null || configuredUrl.trim().isEmpty()
                ? "API: Not configured"
                : "API: Invalid destination";
        }
        String code = clanCode == null ? "" : clanCode.trim();
        return "API: " + normalized + (code.isEmpty() ? "" : " [" + code + "]");
    }

    public String normalize(String configuredUrl)
    {
        if (configuredUrl == null || configuredUrl.trim().isEmpty())
        {
            return null;
        }
        try
        {
            URI uri = new URI(configuredUrl.trim());
            String scheme = uri.getScheme() == null ? ""
                : uri.getScheme().toLowerCase(Locale.ENGLISH);
            String host = uri.getHost();
            if (host == null || uri.getUserInfo() != null
                || uri.getQuery() != null || uri.getFragment() != null)
            {
                return null;
            }
            boolean localhost = host.equalsIgnoreCase("localhost")
                || host.equals("127.0.0.1") || host.equals("::1");
            if (!scheme.equals("https") && !(scheme.equals("http") && localhost))
            {
                return null;
            }
            String normalized = uri.toString();
            while (normalized.endsWith("/"))
            {
                normalized = normalized.substring(0, normalized.length() - 1);
            }
            return normalized;
        }
        catch (URISyntaxException exception)
        {
            return null;
        }
    }
}
