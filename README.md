# Java Chat Application with PostgreSQL Persistence

A multi-client chat application built with Java Sockets and PostgreSQL that preserves chat history, user sessions, and provides message recovery for returning users.

## Features

- **Real-time messaging and Multi-threading** - Broadcast and private messaging (@username)
- **Persistent chat history** - All messages stored in PostgreSQL
- **Smart reconnection** - Returning users see messages they missed
- **User management** - Automatic user registration and session tracking
- **Personal history** - View your chat history anytime with `MYHISTORY` command
- **Active user list** - See who's currently online with `WHOISIN`

## Quick Start

### Prerequisites
- Java JDK 8+
- PostgreSQL 12+
- PostgreSQL JDBC Driver

### 1. Setup Database
```bash
# Create database
psql -U postgres
CREATE DATABASE chat;
\c chat
```

### 2. Configure Database Connection
Edit `DatabaseManager.java`:
```java
private static final String DB_USER = "postgres";
private static final String DB_PASSWORD = "your_password";
```

### 3. Add JDBC Driver

**Maven:**
```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <version>42.6.0</version>
</dependency>
```

**Or download**: [PostgreSQL JDBC Driver](https://jdbc.postgresql.org/download/)

### 4. Compile and Run
```bash
# Start server
mvn clean compile exec:java -Dexec.mainClass="com.chat.Server"
# Start client (in new terminal)
mvn clean compile exec:java -Dexec.mainClass="com.chat.Client"
```

## Usage

### Commands
| Command | Description |
|---------|-------------|
| `message` | Send broadcast to all users |
| `@username message` | Send private message |
| `WHOISHERE` | List online users |
| `MYHISTORY` | View your chat history |
| `LOGOUT` | Disconnect |

### Example Session
```
Enter username:
>Alice

=== Welcome to the chatroom! ===
[Recent chat history shown]

>Hello everyone!
14:23:15 Alice: Hello everyone!

>@Bob Hey there
14:24:30 Alice Hey there
```


## Database Schema

**Tables:**
- `users` - User accounts with IDs
- `messages` - All chat messages (broadcast & private)
- `active_sessions` - Connection tracking

## Configuration

Default settings in code:
- **Port**: 7450
- **Server**: localhost
- **History limit**: 20 messages (new users), 100 (returning users)

## Key Features Explained

### For New Users
- See last 20 broadcast messages
- Get conversation context

### For Returning Users
- See ALL messages since last logout
- Includes private messages sent to you
- Auto-catch up on missed conversations

### Privacy
- Users only see their own messages and messages sent to them
- Private conversations remain private

## Troubleshooting

**"Driver not found"**
- Add PostgreSQL JDBC driver to classpath

**"Connection refused"**
```bash
sudo systemctl start postgresql
```

**"Database does not exist"**
```bash
createdb chat
```

##  Useful Queries

### View all messages:
```sql
SELECT u.username, m.message_text, m.sent_at 
FROM messages m 
JOIN users u ON m.sender_id = u.user_id 
ORDER BY m.sent_at DESC;
```

### Active users:
```sql
SELECT u.username, s.connected_at 
FROM active_sessions s 
JOIN users u ON s.user_id = u.user_id 
WHERE s.is_active = TRUE;
```

