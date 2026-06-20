package com.roll_54.roll_mod_server.minestar;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.TagPattern;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import java.util.ArrayList;
import java.util.List;

public class TagResolverContainer {

    public final List<TagResolver> resolvers = new ArrayList<>();

    public void resolver(@TagPattern String name, Object value) {
        resolvers.add(TagResolver.resolver(name, Tag.inserting(value instanceof Component ? (Component) value : Component.text(value.toString()))));
    }
}