package com.rhoonart.unearth.common.util;

import com.rhoonart.unearth.user.dto.UserDto;
import com.rhoonart.unearth.user.entity.Role;
import com.rhoonart.unearth.user.exception.ForbiddenException;
import com.rhoonart.unearth.user.exception.UnauthorizedException;
import jakarta.servlet.http.HttpSession;

import java.util.Arrays;

public class SessionUserUtil {
    private static final String LOGIN_USER_KEY = "LOGIN_USER";

    /**
     * 세션에서 로그인한 사용자 정보를 가져옵니다.
     * 
     * @param session HttpSession
     * @return UserDto 또는 null (로그인하지 않은 경우)
     */
    public static UserDto getLoginUser(HttpSession session) {
        return (UserDto) session.getAttribute(LOGIN_USER_KEY);
    }

    /**
     * 로그인 여부를 확인합니다.
     * 
     * @param session HttpSession
     * @return 로그인 여부
     */
    public static boolean isLoggedIn(HttpSession session) {
        return getLoginUser(session) != null;
    }

    /**
     * 로그인을 요구합니다. 로그인하지 않은 경우 예외를 발생시킵니다.
     * 
     * @param session HttpSession
     * @return UserDto
     * @throws UnauthorizedException 로그인하지 않은 경우
     */
    public static UserDto requireLogin(HttpSession session) {
        UserDto user = getLoginUser(session);
        if (user == null) {
            throw new UnauthorizedException("로그인이 필요합니다.");
        }
        return user;
    }

    /**
     * 특정 권한을 요구합니다. 권한이 없는 경우 예외를 발생시킵니다.
     * 
     * @param session       HttpSession
     * @param requiredRoles 필요한 권한들 (하나라도 일치하면 통과)
     * @return UserDto
     * @throws UnauthorizedException 로그인하지 않은 경우
     * @throws ForbiddenException    권한이 없는 경우
     */
    public static UserDto requireRole(HttpSession session, Role... requiredRoles) {
        UserDto user = requireLogin(session);

        if (requiredRoles.length == 0) {
            return user;
        }

        boolean hasRole = Arrays.stream(requiredRoles)
                .anyMatch(role -> role == user.getRole());

        if (!hasRole) {
            throw new ForbiddenException("접근 권한이 없습니다.");
        }

        return user;
    }

    /**
     * SUPER_ADMIN 또는 ADMIN 권한을 요구합니다.
     * 
     * @param session HttpSession
     * @return UserDto
     */
    public static UserDto requireAdminRole(HttpSession session) {
        return requireRole(session, Role.SUPER_ADMIN, Role.ADMIN);
    }

    /**
     * 사용자 ID를 가져옵니다.
     * 
     * @param session HttpSession
     * @return 사용자 ID 또는 null
     */
    public static String getUserId(HttpSession session) {
        UserDto user = getLoginUser(session);
        return user != null ? user.getId() : null;
    }

    /**
     * 사용자 권한을 가져옵니다.
     * 
     * @param session HttpSession
     * @return 사용자 권한 또는 null
     */
    public static String getUserRole(HttpSession session) {
        UserDto user = getLoginUser(session);
        return user != null ? user.getRole().name() : null;
    }

    /**
     * 관리자 권한(SUPER_ADMIN, ADMIN)을 확인합니다.
     * 
     * @param session HttpSession
     * @return 관리자 권한 여부
     */
    public static boolean isAdmin(HttpSession session) {
        String role = getUserRole(session);
        return "SUPER_ADMIN".equals(role) || "ADMIN".equals(role);
    }

    /**
     * 특정 권리자의 데이터에 접근할 권한이 있는지 확인합니다.
     * 
     * @param session       HttpSession
     * @param rightHolderId 권리자 ID
     * @return 접근 권한 여부
     */
    public static boolean hasAccessToRightHolder(HttpSession session, String rightHolderId) {
        UserDto user = getLoginUser(session);
        if (user == null) {
            return false;
        }

        // 관리자는 모든 권리자 데이터에 접근 가능
        if (isAdmin(session)) {
            return true;
        }

        // 권리자는 본인 데이터만 접근 가능
        return user.getRole() == Role.RIGHT_HOLDER && user.getId().equals(rightHolderId);
    }
}