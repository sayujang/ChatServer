package com.chat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.naming.spi.DirStateFactory.Result;

public class DatabaseManager
{
    private static final String DB_URL="jdbc:postgresql://localhost:5432/chat";
    private static final String DB_USER="sayuj";
    private static final String DB_PASSWORD="sayuj596";
    private Connection connection;
    
    public DatabaseManager() throws SQLException
    {
        try{
            //load jdbc driver for postgresql
            Class.forName("org.postgresql.Driver");//throws classnotfoundexception
            connection=DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            System.out.println("Database connection established: success");
        }
        catch(ClassNotFoundException e)
        {
            throw new SQLException("JDBC driver not found"+e);
        }

    }

    //get(also updates lastlogin) or create user
    //returns user_id
    public int getUserId(String username) throws SQLException
    {
        String selectQuery="select user_id from users where username = ? ";
        try(PreparedStatement prst=connection.prepareStatement(selectQuery)) 
        {
            prst.setString(1, username);//prevents sql injection: clearly separates data and code and prevents data(i.e. malicious userinput)to be taken as sql command
            ResultSet rs=prst.executeQuery();
            if (rs.next()) // turns the pointer to the first row(if exists returns true)
            {
                int userId=rs.getInt("user_id");//specify column of above row
                updateLastLogin(userId);
                return userId;
            }
        } //autoclose connection
        String insertQuery="insert into users (username) values (?) returning user_id";
        try(PreparedStatement prst=connection.prepareStatement(insertQuery))
        {
            prst.setString(1, username);
            ResultSet rs=prst.executeQuery();
            if (rs.next())
            {
                return rs.getInt("user_id");
            }
            throw new SQLException("Couldn't create user");

        }        
    }
    private void updateLastLogin(int userId) throws SQLException
    {
        String updateQuery="update users set last_login = current_timestamp where user_id = ?";
        try(PreparedStatement prst=connection.prepareStatement(updateQuery))
        {
            prst.setInt(1, userId);
            prst.executeUpdate(); //updates
        }
    }
    public void saveMessage(int senderId, Integer receiverId, String messageText, boolean isPrivate) throws SQLException
    {
        String query="insert into messages (sender_id, receiver_id, message_text, is_private) values (?,?,?,?)";
        try(PreparedStatement prst=connection.prepareStatement(query))
        {
            prst.setInt(1, senderId);
            if(receiverId != null) 
                prst.setInt(2, receiverId);
            else
                prst.setInt(2,Types.INTEGER);
            prst.setString(3, messageText);
            prst.setBoolean(4, isPrivate);
            prst.executeUpdate();
        }
    }
    public List<ChatHistoryMessage> getRecentMessages(int limit) throws SQLException
        {
            List<ChatHistoryMessage> messages=new ArrayList<>();
            String query = "SELECT m.message_id, u1.username AS sender, u2.username AS receiver, " +
                      "m.message_text, m.is_private, m.sent_at " +
                      "FROM messages m " +
                      "JOIN users u1 ON m.sender_id = u1.user_id " +
                      "LEFT JOIN users u2 ON m.receiver_id = u2.user_id " +
                      "ORDER BY m.sent_at DESC LIMIT ?";
            try(PreparedStatement prst=connection.prepareStatement(query))
            {
                prst.setInt(1, limit);
                ResultSet rs= prst.executeQuery();
                while(rs.next())
                {
                    ChatHistoryMessage msg = new ChatHistoryMessage(
                    rs.getInt("message_id"),
                    rs.getString("sender"),
                    rs.getString("receiver"),
                    rs.getString("message_text"),
                    rs.getBoolean("is_private"),
                    rs.getTimestamp("sent_at"));   
                    messages.add(msg);
                }
            }
            Collections.reverse(messages);
            return messages;
        }
    
    //get chatmessages relevant to a specific user:
    public List<ChatHistoryMessage> getUserRelevantMessages(int userId, int limit) throws SQLException {
        List<ChatHistoryMessage> messages = new ArrayList<>();
        String query = "SELECT m.message_id, u1.username AS sender, u2.username AS receiver, " +
                      "m.message_text, m.is_private, m.sent_at " +
                      "FROM messages m " +
                      "JOIN users u1 ON m.sender_id = u1.user_id " +
                      "LEFT JOIN users u2 ON m.receiver_id = u2.user_id " +
                      "WHERE m.sender_id = ? OR m.receiver_id = ? OR m.is_private = FALSE " +
                      "ORDER BY m.sent_at DESC LIMIT ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, userId);
            pstmt.setInt(3, limit);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                ChatHistoryMessage msg = new ChatHistoryMessage(
                    rs.getInt("message_id"),
                    rs.getString("sender"),
                    rs.getString("receiver"),
                    rs.getString("message_text"),
                    rs.getBoolean("is_private"),
                    rs.getTimestamp("sent_at")
                );
                messages.add(msg);
            }
        }

        
        java.util.Collections.reverse(messages);
        return messages;
    }
    public List<ChatHistoryMessage> getMessagesSinceLastLogin(int userId) throws SQLException
    {
        List<ChatHistoryMessage> messages=new ArrayList<>();
        String lastDisconnectQuery="select disconnected_at from active_sessions where "+
                                    "user_id=? and disconnected_at is not null " +
                                    "order by disconnected_at desc limit 1";
        Timestamp lastDisconnect=null;
        try(PreparedStatement prst=connection.prepareStatement(lastDisconnectQuery))
        {
            prst.setInt(1,userId);
            ResultSet rs=prst.executeQuery();
            if(rs.next())
            {
                lastDisconnect=rs.getTimestamp("disconnected_at");
            }
        }
        if (lastDisconnect==null)
        {
            return getUserRelevantMessages(userId, 20);
        }
        String query="SELECT m.message_id, u1.username AS sender, u2.username AS receiver, " +
                      "m.message_text, m.is_private, m.sent_at " +
                      "FROM messages m " +
                      "JOIN users u1 ON m.sender_id = u1.user_id " +
                      "LEFT JOIN users u2 ON m.receiver_id = u2.user_id " +
                      "WHERE m.sent_at > ? AND " +
                      "(m.sender_id = ? OR m.receiver_id = ? OR m.is_private = FALSE) " +
                      "ORDER BY m.sent_at ASC LIMIT 100";
        try(PreparedStatement prst=connection.prepareStatement(query))
        {
            prst.setTimestamp(1, lastDisconnect);
            prst.setInt(2, userId);
            prst.setInt(3, userId);
            ResultSet rs=prst.executeQuery();
            while(rs.next())
            {
                ChatHistoryMessage msg=new ChatHistoryMessage(
                    rs.getInt("message_id"),
                     rs.getString("sender"),
                    rs.getString("receiver"),
                    rs.getString("message_text"),
                    rs.getBoolean("is_private"),
                    rs.getTimestamp("sent_at")
                );
                messages.add(msg);
            }
        }
        return messages;
    }
    //checking if this is first login
    public boolean isFirstLogin(int userId) throws SQLException {
        String query = "SELECT COUNT(*) as session_count FROM active_sessions WHERE user_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("session_count") <= 1; // 1 because current session already created
            }
        }
        return true;
    }
     public List<ChatHistoryMessage> getPrivateMessages(int userId1, int userId2, int limit) throws SQLException {
        List<ChatHistoryMessage> messages = new ArrayList<>();
        String query = "SELECT m.message_id, u1.username AS sender, u2.username AS receiver, " +
                      "m.message_text, m.is_private, m.sent_at " +
                      "FROM messages m " +
                      "JOIN users u1 ON m.sender_id = u1.user_id " +
                      "LEFT JOIN users u2 ON m.receiver_id = u2.user_id " +
                      "WHERE (m.sender_id = ? AND m.receiver_id = ?) " +
                      "OR (m.sender_id = ? AND m.receiver_id = ?) " +
                      "ORDER BY m.sent_at DESC LIMIT ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, userId1);
            pstmt.setInt(2, userId2);
            pstmt.setInt(3, userId2);
            pstmt.setInt(4, userId1);
            pstmt.setInt(5, limit);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                ChatHistoryMessage msg = new ChatHistoryMessage(
                    rs.getInt("message_id"),
                    rs.getString("sender"),
                    rs.getString("receiver"),
                    rs.getString("message_text"),
                    rs.getBoolean("is_private"),
                    rs.getTimestamp("sent_at")
                );
                messages.add(msg);
            }
        }
        
        java.util.Collections.reverse(messages);
        return messages;
    }
    public int createSession(int userId) throws SQLException {
        String query = "INSERT INTO active_sessions (user_id) VALUES (?) RETURNING session_id";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("session_id");
            }
        }
        throw new SQLException("Failed to create session");
    }
    public void endSession(int sessionId) throws SQLException {
        String query = "UPDATE active_sessions SET disconnected_at = CURRENT_TIMESTAMP, " +
                      "is_active = FALSE WHERE session_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, sessionId);
            pstmt.executeUpdate();
        }
    }
    public String getUsername(int userId) throws SQLException {
        String query = "SELECT username FROM users WHERE user_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getString("username");
            }
        }
        return null;
    }
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database connection closed");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static class ChatHistoryMessage
    {
        private int messageId;
        private String sender;
        private String receiver;
        private String messageText;
        private boolean isPrivate;
        private Timestamp sentAt;
        public ChatHistoryMessage(int messageId, String sender, String receiver, String messageText, boolean isPrivate,
                Timestamp sentAt) {
            this.messageId = messageId;
            this.sender = sender;
            this.receiver = receiver;
            this.messageText = messageText;
            this.isPrivate = isPrivate;
            this.sentAt = sentAt;
        }
        public String formatMessage(){
            SimpleDateFormat sdf= new SimpleDateFormat("HH:mm:ss");
            String time=sdf.format(sentAt);
            if (isPrivate && receiver != null) {
                return time + " [Private] " + sender + " -> " + receiver + ": " + messageText;
            } else {
                return time + " " + sender + ": " + messageText;
            }

        }
        public int getMessageId() {
            return messageId;
        }
        public String getSender() {
            return sender;
        }
        public String getReceiver() {
            return receiver;
        }
        public String getMessageText() {
            return messageText;
        }
        public boolean isPrivate() {
            return isPrivate;
        }
        public Timestamp getSentAt() {
            return sentAt;
        }
        
        

    }

    
}