package com.roll_54.roll_mod_server.minestar;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.function.Consumer;

public class Adventure {
    public static Component miniMessage(String miniMessage) {
        var component = MiniMessage.miniMessage().deserialize(miniMessage);
        String jsonComponent = JSONComponentSerializer.json().serialize(component);
        return Component.Serializer.fromJson(jsonComponent, ServerLifecycleHooks.getCurrentServer().registryAccess());
    }

    public static Component miniMessage(String miniMessage, Consumer<TagResolverContainer> tagResolverConsumer) {
        TagResolverContainer tagResolverContainer = new TagResolverContainer();
        tagResolverConsumer.accept(tagResolverContainer);
        var component = MiniMessage.miniMessage().deserialize(miniMessage, tagResolverContainer.resolvers.toArray(new TagResolver[0]));
        String jsonComponent = JSONComponentSerializer.json().serialize(component);
        return Component.Serializer.fromJson(jsonComponent, ServerLifecycleHooks.getCurrentServer().registryAccess());
    }

    public static String miniMessageJson(String miniMessage) {
        var component = MiniMessage.miniMessage().deserialize(miniMessage);
        return JSONComponentSerializer.json().serialize(component);
    }
}