package com.bigteamseventeen.wpd2_ah.milestones;

import java.sql.Connection;
import java.sql.SQLException;

import com.callumcarmicheal.wframe.Resource;
import com.callumcarmicheal.wframe.Server;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.sqlite.core.DB;

public class Main {
    // Package class that will contain all the controllers
    private static final String CONTROLLERSPACKAGE = "com.bigteamseventeen.wpd2_ah.milestones.controllers";
    private static final int PORT = 8080; // Web Server port

    final static Logger logger = Logger.getLogger(Main.class);

    // Server instance
    private static Server server = null;

    public static void main(String[] args) throws Exception {
        logger.info("BigTeamSeventeen WPD2 Group AH: Bootstrapping Application");

        // Setup our logging class
        BasicConfigurator.configure();

        // Setup the database
        if (SetupDatabase()) {
            logger.error("BigTeamSeventeen WPDB2 Group AH: Stopping application because SetupDatabase() failed.");
            System.exit(1);
        }

        // Start the server
        if (SetupServer()) {
            logger.error("BigTeamSeventeen WPDB2 Group AH: Stopping application because SetupServer() failed.");
            System.exit(1);
        }
    }

    public static boolean SetupDatabase() {
        logger.info("Connecting to database");
        
        Connection DB;

        try {
            SqliteDBCon.InitializeDatabase();
            DB = SqliteDBCon.GetConnection();

            if (DB == null) {
                logger.error("ERROR: Failed to connect to database.");
                return true;
            }
        } catch (SQLException e) {
            logger.error("Failed to initialize database", e);
            return true;
        }

        System.out.println("Initializing Database");
        SqliteDBCon.SetupORM(DB);

        // Release the database resource
        try { DB.close(); } 
        catch(Exception ex) {
            logger.error("Failed to release test database connection to pool", ex);            
        };

        return false;
    }

    public static boolean SetupServer() {
        try {
            // We are starting the server
            System.out.println("Starting server!");
            new Server(PORT, CONTROLLERSPACKAGE).setResourcesEnabled(false)
                    .setResourcesDirectory(Resource.getWorkingDirection())
                    .start();

            // We have started the server
            System.out.println("Server started on port: " + PORT + "!");

            return false;
        } catch (Exception e) {
            logger.error("Failed to start server instance", e);
            return true;
        }
    }
}