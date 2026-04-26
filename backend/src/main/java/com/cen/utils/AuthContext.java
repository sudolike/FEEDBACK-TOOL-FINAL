package com.cen.utils;

import com.cen.common.Constants;
import com.cen.exception.ServiceException;

/**
 * 线程本地的当前请求上下文。
 * JwtInterceptor 在进入 Controller 前写入，DispatcherServlet 完成后由
 * afterCompletion 清理，保证不会跨请求泄漏。
 */
public final class AuthContext {

    private static final ThreadLocal<Long>    USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String>  ROLE    = new ThreadLocal<>();
    private static final ThreadLocal<String>  USERNAME = new ThreadLocal<>();

    private AuthContext() {}

    public static void set(Long userId, String role, String username) {
        USER_ID.set(userId);
        ROLE.set(role);
        USERNAME.set(username);
    }

    public static void clear() {
        USER_ID.remove();
        ROLE.remove();
        USERNAME.remove();
    }

    public static Long currentUserId() {
        return USER_ID.get();
    }

    public static String currentRole() {
        return ROLE.get();
    }

    public static String currentUsername() {
        return USERNAME.get();
    }

    /** 必须已登录，否则 401 */
    public static Long requireUserId() {
        Long id = USER_ID.get();
        if (id == null) {
            throw new ServiceException(Constants.CODE_401, "请先登录");
        }
        return id;
    }

    /** 必须满足指定角色（admin / teacher / student），否则 403 */
    public static Long requireRole(String role) {
        Long id = requireUserId();
        if (!role.equals(ROLE.get())) {
            throw new ServiceException(Constants.CODE_403,
                    "权限不足：需要 " + role + " 角色");
        }
        return id;
    }
}
