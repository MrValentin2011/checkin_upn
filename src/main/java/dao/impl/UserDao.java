/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dao.impl;

import config.db.DBConnection;
import java.sql.*;
import java.util.*;
import model.User;

/**
 *
 * @author USER
 */
public class UserDao {

    public User findByUsername(String username) {
        String sql = "SELECT * FROM Users WHERE username = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return map(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<User> listAll() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM Users";
        try (Connection conn = DBConnection.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                users.add(map(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    public User findById(int id) {
        String sql = "SELECT * FROM Users WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean insert(User u) {
        String sql = "INSERT INTO Users (username, password_hash, full_name, email, role_id, active) VALUES (?,?,?,?,?,?)";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, u.getUsername());
            ps.setString(2, u.getPassword());
            ps.setString(3, u.getFullName());
            ps.setString(4, u.getEmail());
            ps.setInt(5, u.getId_role());
            ps.setBoolean(6, u.isActive());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private User map(ResultSet rs) throws SQLException {
        return new User(
                rs.getInt("user_id"),
                rs.getString("username"),
                rs.getString("password_hash"),
                rs.getString("full_name"),
                rs.getString("email"),
                rs.getInt("role_id"),
                rs.getBoolean("active")
        );

    }
}
