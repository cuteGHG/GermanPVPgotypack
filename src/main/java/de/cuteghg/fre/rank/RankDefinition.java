package de.cuteghg.fre.rank;

public record RankDefinition(
        String key,
        String permission,
        String luckPermsGroup,
        String label,
        String icon,
        int priority
) {
}
