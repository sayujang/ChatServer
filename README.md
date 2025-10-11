# Java Chat Application with PostgreSQL Persistence

A multi-client chat application built with Java Sockets and PostgreSQL that preserves chat history, user sessions, and provides message recovery for returning users.

## 1.Features

- **Real-time messaging** - Broadcast and private messaging (@username)
- **Persistent chat history** - All messages stored in PostgreSQL
- **Smart reconnection** - Returning users see messages they missed
- **User management** - Automatic user registration and session tracking
- **Personal history** - View your chat history anytime with `MYHISTORY` command
- **Active user list** - See who's currently online with `WHOISHERE`

## 2.Quick Start

### Prerequisites
- Java JDK 8+
- PostgreSQL 12+
- PostgreSQL JDBC Driver

### 3. Setup Database
```bash
# Create database
psql -U postgres
CREATE DATABASE chat;
\c chat_app

# Run the schema (copy from chat_db_schema.sql)
