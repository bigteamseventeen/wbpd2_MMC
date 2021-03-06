package com.bigteamseventeen.wpd2_ah.milestones.models;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;

import com.bigteamseventeen.wpd2_ah.milestones.SqliteDBCon;
import com.callumcarmicheal.wframe.database.CInteger;
import com.callumcarmicheal.wframe.database.CVarchar;
import com.callumcarmicheal.wframe.database.DatabaseColumn;
import com.callumcarmicheal.wframe.database.DatabaseModel;
import com.callumcarmicheal.wframe.database.querybuilder.QueryResults;
import com.callumcarmicheal.wframe.database.querybuilder.SDWhereQuery;
import com.callumcarmicheal.wframe.database.querybuilder.SDWhereQuery.QueryValueType;
import com.callumcarmicheal.wframe.web.Session;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import at.favre.lib.crypto.bcrypt.BCrypt;

@SuppressWarnings({"rawtypes", "unchecked"})
public class User extends DatabaseModel<User> {
    final static Logger logger = LogManager.getLogger();
    final static String SESSION_ID_KEY = "USER_ID";

    // -----------------------------------------------------------
    // --                                                       --
    // ------------- Model Definition Attributes -----------------
    // --                                                       --
    // -----------------------------------------------------------
    // Creating these for each instance would be taxing so we want to cache them
    private static LinkedHashMap<String, DatabaseColumn> _ColumnsDefinition = new LinkedHashMap<>();
    private static CInteger _PrimaryKey;

    @Override public String getModelName() {
        return "User";
    }

    /**
     * Create a instance of the user model
     */
    public User(Connection c) { 
        // Setup the model instance settings
        super("users", _ColumnsDefinition, _PrimaryKey, c);
    }

    private static void _addColumn(DatabaseColumn db) {
        _ColumnsDefinition.put(db.getName(), db); 
    }

    public static boolean Initialize(Connection c) {
        _addColumn( _PrimaryKey = new CInteger("id").setPrimaryKey(true) );
        _addColumn( new CVarchar("username").setUnique(true) );
        _addColumn( new CVarchar("password").setFlagPrintInString(false) );
        _addColumn( new CVarchar("email", 320).setUnique(true) );
        _addColumn( new CInteger("isAdmin") );
        _addColumn( new CInteger("isBanned") );

        User model = new User(c);
        return model.CreateTable(true);
    }

    public static SDWhereQuery<User> where(Connection connection, String column, String comparison, Object value) {
        return where(new User(connection), column, comparison, value);
    }

    public static SDWhereQuery<User> where(Connection connection, String column, String comparison, Object value, QueryValueType qvt) {
        return where(new User(connection), column, comparison, value, qvt);
    }

    // -----------------------------------------------------------
    // --                                                       --
    // ----------------------  Queries  -------------------------- 
    // --                                                       --
    // -----------------------------------------------------------

    public static User[] All(Connection con) {
        return All(new User(con));
    }

    public static User Get(Connection connection, int id) {
        try {
            // Query the database
            QueryResults<User> query = 
                where(connection, "id", "=", id, QueryValueType.Bound)
                    .setLimit(1)
                    .execute();

            if (query.Successful)
                return query.Rows[0];
        } catch (SQLException e) {
            logger.error("Failed to find a user by id, SQL Exception", e);
            return null;
        }

        return null;
    }

    public static User Find(Connection connection, String username) {
        try {
            // Query the database
            QueryResults<User> query = 
                where(connection, "username", "=", username, QueryValueType.Bound)
                    .setLimit(1)
                    .execute();

            if (query.Successful)
                return query.Rows[0];
        } catch (SQLException e) {
            logger.error("Failed to find a user by a username, SQL Exception", e);
            return null;
        }

        return null;
    }

    public static User FindUsernameLike(Connection connection, String username) {
        return FindUsernameLike(connection, username, true, true);
    }

    public static User FindUsernameLike(Connection connection, String username, boolean front, boolean back) {
        try {
            String un = "";

            if (front) un+="%";
                un+=username;
            if (back) un+="%";

            // Query the database
            QueryResults<User> query = 
                where(connection, "username", "LIKE", un, QueryValueType.Bound)
                    .setLimit(1)
                    .execute();

            if (query.Successful)
                return query.Rows[0];
        } catch (SQLException e) {
            logger.error("Failed to find a user by an username like, SQL Exception", e);
            return null;
        }

        return null;
    }

    public static User FindEmail(Connection connection, String email) {
        try {
            // Query the database
            QueryResults<User> query = 
                where(connection, "email", "=", email, QueryValueType.Bound)
                    .setLimit(1)
                    .execute();

            if (query.Successful)
                return query.Rows[0];
        } catch (SQLException e) {
            logger.error("Failed to find a user by an email, SQL Exception", e);
            return null;
        }

        return null;
    }

    /**
     * Get a user from the session
     * @param session
     * @return
     */
    public static User GetSessionUser(Session session) {
        // Check if we have a user in the session
        int user_id = session.get(SESSION_ID_KEY, -1);

        // We dont have a user 
        if (user_id == -1)
           return null;

        // Our query results
        QueryResults<User> query;
        Connection con = null;

        try {
            // Get database
            con = SqliteDBCon.GetConnection();

            // Find the user by the id
            query = User.where(con, "id", "=", user_id).execute();
        } catch (SQLException e) {
            logger.error("Failed to find a user by an id (session), SQL Exception", e);
            return null;
        } finally {
            try { if (con != null && !con.isClosed()) con.close(); } catch (Exception e) {};
        }
        
        // Check if we have a user
        if (query.Length == 0) 
            return null; 
        
        // Get the user
        return query.first();
    }

    /**
     * Checks if a session is authenticated
     * @param session
     * @return
     */
    public static boolean IsSessionAuthenticated(Session session) {
        return GetSessionUser(session) != null;
    }

    /**
     * Set the session to the current user
     * @param session
     */
    public void authenticateSession(Session session) {
        // Set SESSION_ID_KEY to the current user id
        session.set(SESSION_ID_KEY, this.getId());
    }
    
    /**
     * Remove a session from the current user
     * @param session
     */
    public void logoutSession(Session session) {
        // Remove the SESSION_ID_KEY from the session
        if (session.containsKey(SESSION_ID_KEY))
            session.remove(SESSION_ID_KEY);
    }

    // -----------------------------------------------------------
    // --                                                       --
    // ----------------------- Operations ------------------------ 
    // --                                                       --
    // -----------------------------------------------------------

    public boolean checkPassword(String password) {
        BCrypt.Result result = BCrypt.verifyer().verify(password.toCharArray(), getPassword());
        return result.verified;
    }

    public User setPasswordEncrypted(String password) {
        setPassword(BCrypt.withDefaults().hashToString(12, password.toCharArray()));
        return this;
    }

    // -----------------------------------------------------------
    // --                                                       --
    // ------------------- Getters and Setters ------------------- 
    // --                                                       --
    // -----------------------------------------------------------

    public int getId() {
        return (int) values.get("id").Value;
    }

    public User setUsername(String value) {
        values.get("username").Value = value; return this;
    }

    public String getUsername() {
        return (String) values.get("username").Value;
    }

    public User setPassword(String value) {
        values.get("password").Value = value; return this;
    }

    public String getPassword() {
        return (String) values.get("password").Value;
    }

    public User setEmail(String email) {
        values.get("email").Value = email; return this;
    }

    public String getEmail() {
        return (String) values.get("email").Value;
    }

    public User setAdmin(int isAdmin) {
        values.get("isAdmin").Value = isAdmin; return this;
    }
    
    public int getAdmin() {
        return (int) values.get("isAdmin").Value;
    }

    public boolean isAdmin() {
        // If the account is banned then the user can't be an admin
        if (this.isBanned())
            return false;

        return ((int)values.get("isAdmin").Value) == 1;
    }

    public User setBanned(int banned) {
        values.get("isBanned").Value = banned; return this;
    }
    
    public int getBanned() {
        return (int) values.get("isBanned").Value;
    }

    public boolean isBanned() {
        return ((int) values.get("isBanned").Value) == 1;
    }

    public String getStatusText() {
        if (this.isBanned())
            return "Banned";
        if (this.isAdmin())
            return "Admin";

        return "User";
    }
}