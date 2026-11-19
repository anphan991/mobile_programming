package com.example.myapplication;

/**
 * Mô hình tài khoản người dùng phục vụ phân quyền và quản lý trong bảng users.
 */
public class UserAccount {
    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_USER = "user";

    private final int id;
    private String username;
    private String password;
    private String role;

    public UserAccount(int id, String username, String password, String role) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = normalizeRole(role, username);
    }

    public static String normalizeRole(String role, String username) {
        if (ROLE_ADMIN.equalsIgnoreCase(role) || "admin".equalsIgnoreCase(username)) {
            return ROLE_ADMIN;
        }
        return ROLE_USER;
    }

    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getRole() { return role; }
    public boolean isAdmin() { return ROLE_ADMIN.equalsIgnoreCase(role); }

    public void setUsername(String username) { this.username = username; }
    public void setPassword(String password) { this.password = password; }
    public void setRole(String role) { this.role = normalizeRole(role, username); }
}
