package com.lsx.core.common.Util;

public class UserContext {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<Long> COMMUNITY_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> ROLE = new ThreadLocal<>();

    public static void setUserId(Long userId) {
        USER_ID.set(userId);
    }

    public static Long getCurrentUserId() {
        return USER_ID.get();
    }

    public static void setCommunityId(Long communityId) {
        COMMUNITY_ID.set(communityId);
    }

    public static Long getCommunityId() {
        return COMMUNITY_ID.get();
    }

    public static void setRole(String role) {
        ROLE.set(role);
    }

    public static String getRole() {
        return ROLE.get();
    }

    public static void clear() {
        USER_ID.remove();
        COMMUNITY_ID.remove();
        ROLE.remove();
    }
}