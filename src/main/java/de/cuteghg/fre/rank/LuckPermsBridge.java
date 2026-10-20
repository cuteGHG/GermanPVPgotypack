package de.cuteghg.fre.rank;

import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.Optional;

public final class LuckPermsBridge {

    public Optional<String> getPrimaryGroup(Player player) {
        try {
            Class<?> providerClass = Class.forName("net.luckperms.api.LuckPermsProvider");
            Object luckPerms = providerClass.getMethod("get").invoke(null);
            Method getUserManager = luckPerms.getClass().getMethod("getUserManager");
            Object userManager = getUserManager.invoke(luckPerms);
            Object user = userManager.getClass().getMethod("getUser", java.util.UUID.class).invoke(userManager, player.getUniqueId());
            if (user == null) {
                return Optional.empty();
            }
            Object group = user.getClass().getMethod("getPrimaryGroup").invoke(user);
            return Optional.ofNullable(group).map(Object::toString);
        } catch (Throwable ignored) {
            return Optional.empty();
        }
    }
}
