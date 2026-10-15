package com.collapse.combat;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class Friends {
    private static final Set<String> NAMES = ConcurrentHashMap.newKeySet();

    private Friends() {}

    public static boolean isFriend(String n) { return NAMES.contains(n.toLowerCase()); }
    public static void add(String n) { NAMES.add(n.toLowerCase()); }
    public static void remove(String n) { NAMES.remove(n.toLowerCase()); }
    public static Set<String> all() { return NAMES; }
}
