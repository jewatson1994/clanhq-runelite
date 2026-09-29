package com.clanhq.verifier.bingo;

/**
 * Resolves which value to show for each Bingo overlay password line.
 *
 * The ClanHQ Bingo manifest can optionally carry a clan-wide password set
 * by the event organizer; when it does, that value always wins. When the
 * manifest does not set one (the common case), each player falls back to
 * their own local RuneLite config value, typed in exactly the way the
 * standalone Clan Events plugin works today.
 */
public final class BingoPasswordResolver
{
    private BingoPasswordResolver()
    {
    }

    /**
     * @param localValue the player's own local config value, may be null
     * @param manifestValue the server-pushed value from the active Bingo
     *     manifest, may be null or absent
     * @return the value to display; never null, empty when neither source
     *     has one
     */
    public static String resolve(String localValue, String manifestValue)
    {
        String manifestTrimmed = manifestValue == null ? "" : manifestValue.trim();
        if (!manifestTrimmed.isEmpty())
        {
            return manifestTrimmed;
        }
        return localValue == null ? "" : localValue.trim();
    }
}
