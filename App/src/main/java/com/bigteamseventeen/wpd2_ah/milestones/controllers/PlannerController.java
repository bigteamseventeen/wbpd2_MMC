package com.bigteamseventeen.wpd2_ah.milestones.controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import com.bigteamseventeen.wpd2_ah.milestones.HashBuilder;
import com.bigteamseventeen.wpd2_ah.milestones.MapBuilder;
import com.bigteamseventeen.wpd2_ah.milestones.Renderer;
import com.bigteamseventeen.wpd2_ah.milestones.SqliteDBCon;
import com.bigteamseventeen.wpd2_ah.milestones.models.Milestone;
import com.bigteamseventeen.wpd2_ah.milestones.models.Planner;
import com.bigteamseventeen.wpd2_ah.milestones.models.User;
import com.callumcarmicheal.wframe.HttpRequest;
import com.callumcarmicheal.wframe.database.exceptions.MissingColumnValueException;
import com.callumcarmicheal.wframe.database.querybuilder.QueryResults;
import com.callumcarmicheal.wframe.props.GetRequest;
import com.callumcarmicheal.wframe.props.PostRequest;
import com.google.common.collect.ImmutableMap;

public class PlannerController extends Controller {

    @GetRequest("/planner/view")
    public void viewPlanner(HttpRequest request) throws IOException {
        // Redirect the user to the respected page
        User user; // If user == null then a redirect has happened
        if ((user = getUserOrLogin(request)) == null) return;
        
        Map<String,String> query = request.getQuery();
        int plannerId = -1;
        
        // Check if we had any errors        
        if (query.containsKey("id")) {
            try {
                plannerId = Integer.parseInt(query.get("id").trim());
            } catch (NumberFormatException nfe) { 
                plannerId = -1;
            }
        }

        // We failed to get the planner id from the query
        if (plannerId == -1) {
            request.SendMessagePage("Could not find planner", "The planner id specified does not exist", 400);
            return;
        }

        // Try to find the planner
        Connection con = null;
        Planner planner = null;
        try {
            // Get the db and run the query
            con = SqliteDBCon.GetConnection();

            // Find planner by id and check if the author is the current user
            QueryResults<Planner> dbQuery = Planner
                .where(con, "id", "=", plannerId)
                .andWhere("author", "=", user.getId())
                    .setLimit(1)
                    .execute();

            // Get the first item in the query
            if (dbQuery.Successful) {
                planner = dbQuery.first();

                // Load the milestones
                planner.milestones(con);
            }
        } catch (SQLException ex) {
            // Throw the error
            request.throwException(ex);
            return;
        } finally {
            // Close the database connection
            try { if (con != null && con.isClosed()) con.close(); } catch(Exception e) {}
        }

        // Check if the planner exists
        if (planner == null) {
            request.SendMessagePage("Could not find planner", "The planner id specified does not exist", 400);
            return;
        }

        // Render the page
        new Renderer().setUser(user)
            .render(request, "planner/view", 200, HashBuilder.<String,Object>builder()
                .put("edit", true)
                .put("planner", planner)
            .build());
    }

    /* ========================== */
    /*   Share planner */
    /* ========================== */

    @GetRequest("/planner/share")
    public void viewSharedPlanner(HttpRequest request) throws IOException {
        // Redirect the user to the respected page
        User user; // If user == null then a redirect has happened
        if ((user = getUserOrLogin(request)) == null) return;
        
        Map<String,String> query = request.getQuery();
        String shareHash = null;
        
        // Check if we had any errors        
        if (query.containsKey("code"))
            shareHash = query.get("code").trim();

        // We failed to get the hash from the query
        if (shareHash == null) {
            request.SendMessagePage("Could not find planner", "The planner hash specified does not exist", 400);
            return;
        }

        // Try to find the planner
        Connection con = null;
        Planner planner = null;
        try {
            // Get the db and run the query
            con = SqliteDBCon.GetConnection();

            // Find planner by id and check if the author is the current user
            QueryResults<Planner> dbQuery = Planner
                .where(con, "share", "=", shareHash)
                .andWhere("author", "=", user.getId())
                    .setLimit(1)
                    .execute();

            // Get the first item in the query
            if (dbQuery.Successful) {
                planner = dbQuery.first();

                // Load the milestones
                planner.milestones(con);
            }
        } catch (SQLException ex) {
            // Throw the error
            request.throwException(ex);
            return;
        } finally {
            // Close the database connection
            try { if (con != null && con.isClosed()) con.close(); } catch(Exception e) {}
        }

        // Check if the planner exists
        if (planner == null) {
            request.SendMessagePage("Could not find planner", "The planner hash specified does not exist", 400);
            return;
        }

        // Render the page
        new Renderer().setUser(user)
            .render(request, "planner/view", 200, HashBuilder.<String,Object>builder()
                .put("edit", false)
                .put("planner", planner)
            .build());
    }

    @GetRequest("/planner/share/get")
    public void sharePlanner(HttpRequest request) throws IOException {
        // Redirect the user to the respected page
        User user; // If user == null then a redirect has happened
        if ((user = getUserOrLogin(request)) == null) return;
        
        Map<String,String> query = request.getQuery();
        int plannerId = -1;
        boolean sharePlanner = true;
        
        // Check if we had any errors        
        if (query.containsKey("id")) {
            try {
                plannerId = Integer.parseInt(query.get("id").trim());
            } catch (NumberFormatException nfe) { 
                plannerId = -1;
            }
        }

        if (query.containsKey("unshare")) 
            sharePlanner = false;

        // We failed to get the planner id from the query
        if (plannerId == -1) {
            request.SendMessagePage("Could not find planner", "The planner id specified does not exist", 400);
            return;
        }

        // Try to find the planner
        Connection con = null;
        Planner planner = null;
        try {
            // Get the db and run the query
            con = SqliteDBCon.GetConnection();

            // Find planner by id and check if the author is the current user
            QueryResults<Planner> dbQuery = Planner
                .where(con, "id", "=", plannerId)
                .andWhere("author", "=", user.getId())
                    .setLimit(1)
                    .execute();

            // Get the first item in the query
            if (dbQuery.Successful) {
                planner = dbQuery.first();

                if (sharePlanner) {
                    // Check if we need to generate a new hash
                    if (planner.getShareHash() == null || planner.getShareHash().isEmpty()) {
                        planner.generateShareHashCode(con, 5);
                        planner.save();
                    } 
                } else {
                    planner.setShareHash(null);
                    planner.save();
                }
            }
        } catch (SQLException | MissingColumnValueException ex) {
            // Throw the error
            request.throwException(ex);
            return;
        } finally {
            // Close the database connection
            try { if (con != null && con.isClosed()) con.close(); } catch(Exception e) {}
        }

        // Check if the planner exists
        if (planner == null) {
            request.SendMessagePage("Could not find planner", "The planner id specified does not exist", 400);
            return;
        }

        if (!sharePlanner) {
            request.Redirect("/planner/view?id=" + plannerId);
            return;
        }

        // Render the page
        new Renderer().setUser(user)
            .render(request, "planner/share", 200, HashBuilder.<String,Object>builder()
                .put("title", planner.getTitle())
                .put("hash",  planner.getShareHash())
            .build());
    }

    /* ========================== */
    /*   New planner */
    /* ========================== */

    @GetRequest("/planner/new")
    public void newPlannerForm(HttpRequest request) throws IOException {
        // Redirect the user to the respected page
        User user; // If user == null then a redirect has happened
        if ((user = getUserOrLogin(request)) == null) return;
    
        int errorCode = -1;

        // Check if we had any errors        
        Map<String, String> query = request.getQuery();
        if (query.containsKey("error")) {
            try {
                errorCode = Integer.parseInt(query.get("error").trim());
            } catch (NumberFormatException nfe) { }
        }

        // Render the page
        new Renderer().setUser(user)
            .render(request, "planner/new", 200, HashBuilder.<String,Object>builder()
                .put("error", errorCode)
            .build());
    }

    @PostRequest("/planner/new")
    public void createNewPlanner(HttpRequest request) throws IOException {
        // Redirect the user to the respected page
        User user; // If user == null then a redirect has happened
        if ((user = getUserOrLogin(request)) == null) return;
        
        // Get the form
        Map<String,String> post = request.GetPostForm();

        // Error checking
        String[] requiredKeys = new String[] { "title", "description" };
        for (String input : requiredKeys) {
            if (!post.containsKey(input)) {
                
                // Display error message
                request.Redirect("/planner/new");
                return;
            }
        }

        // Create a database connection
        Connection con = null;
        try {
            // Connect to the database
            con = SqliteDBCon.GetConnection();

            // Create a new project
            new Planner(con)
                .setAuthorId(user.getId())
                .setTitle(post.get("title"))
                .setDescription(post.get("description"))
                .setPublicStatus(post.containsKey("public"))
                .save();

            // Redirect home
            request.Redirect("/");
        } catch (SQLException | MissingColumnValueException ex) {
            // Throw the error
            request.throwException(ex);
            return;
        } finally {
            // Close the database connection
            try { if (con != null && con.isClosed()) con.close(); } catch(Exception e) {}
        }
    }

    /* ========================== */
    /*   Edit planner */
    /* ========================== */

    @GetRequest("/planner/edit")
    public void editPlannerForm(HttpRequest request) throws IOException {
        // Redirect the user to the respected page
        User user; // If user == null then a redirect has happened
        if ((user = getUserOrLogin(request)) == null) return;

        Map<String,String> query = request.getQuery();
        int plannerId = -1;
        int errorId = -1;

        // Check if we had any errors        
        if (query.containsKey("id")) {
            try {
                plannerId = Integer.parseInt(query.get("id").trim());
            } catch (NumberFormatException nfe) { 
                plannerId = -1;
            }

            try {
                if (query.containsKey("error")) 
                    errorId = Integer.parseInt(query.get("error").trim());
            } catch (NumberFormatException nfe) { 
                errorId = -1;
            }
        }

        // We failed to get the planner id from the query
        if (plannerId == -1) {
            request.SendMessagePage("Could not find planner", "The planner id specified does not exist", 400);
            return;
        }

        // Try to find the planner
        Connection con = null;
        Planner planner = null;
        try {
            // Get the db and run the query
            con = SqliteDBCon.GetConnection();

            // Find planner by id and check if the author is the current user
            QueryResults<Planner> dbQuery = Planner
                .where(con, "id", "=", plannerId)
                .andWhere("author", "=", user.getId())
                    .setLimit(1)
                    .execute();

            // Get the first item in the query
            if (dbQuery.Successful) {
                planner = dbQuery.first();
            }
        } catch (SQLException ex) {
            // Throw the error
            request.throwException(ex);
            return;
        } finally {
            // Close the database connection
            try { if (con != null && con.isClosed()) con.close(); } catch(Exception e) {}
        }

        // Check if the planner exists
        if (planner == null) {
            request.SendMessagePage("Could not find planner", "The planner id specified does not exist", 400);
            return;
        }

        // Render the page
        new Renderer().setUser(user)
            .render(request, "planner/edit", 200, HashBuilder.<String,Object>builder()
                .put("planner", planner)
                .put("error", errorId)
            .build());
    }

    @PostRequest("/planner/edit")
    public void editPlanner(HttpRequest request) throws IOException {
        // Redirect the user to the respected page
        User user; // If user == null then a redirect has happened
        if ((user = getUserOrLogin(request)) == null) return;

        Map<String,String> post = request.GetPostForm();
        Map<String,String> query = request.getQuery();
        int plannerId = -1;

        // Check if we had any errors        
        if (query.containsKey("id")) {
            try {
                plannerId = Integer.parseInt(query.get("id").trim());
            } catch (NumberFormatException nfe) { 
                plannerId = -1;
            }
        }

        // We failed to get the planner id from the query
        if (plannerId == -1) {
            request.SendMessagePage("Could not find planner", "The planner id specified does not exist", 400);
            return;
        }

        // Error checking
        String[] requiredKeys = new String[] { "title", "description" };
        for (String input : requiredKeys) {
            if (!post.containsKey(input)) {
                
                // Display error message
                request.Redirect("/planner/edit?id=" + plannerId + "&error=1" );
                return;
            }
        }

        // Try to find the planner
        Connection con = null;
        Planner planner = null;
        try {
            // Get the db and run the query
            con = SqliteDBCon.GetConnection();

            // Find planner by id and check if the author is the current user
            QueryResults<Planner> dbQuery = Planner
                .where(con, "id", "=", plannerId)
                .andWhere("author", "=", user.getId())
                    .setLimit(1)
                    .execute();

            // Get the first item in the query
            if (dbQuery.Successful) 
                planner = dbQuery.first();
        } catch (SQLException ex) {
            // Throw the error
            request.throwException(ex);
            return;
        } finally {
            // Close the database connection
            try { if (con != null && con.isClosed()) con.close(); } catch(Exception e) {}
        }

        // Check if the planner exists
        if (planner == null) {
            request.SendMessagePage("Could not find planner", "The planner id specified does not exist", 400);
            return;
        }

        // Update the model
        con = null;
        try {
            // Get the db and run the query
            con = SqliteDBCon.GetConnection();

            // Update our fields
            planner.setConnection(con);
            planner.setTitle(post.get("title").trim());
            planner.setDescription(post.get("description").trim());
            planner.setPublicStatus(post.containsKey("public"));
            planner.save();

            request.Redirect("/planner/view?id=" + plannerId);
        } catch (SQLException | MissingColumnValueException ex) {
            // Throw the error
            request.throwException(ex);
            return;
        } finally {
            // Close the database connection
            try { if (con != null && con.isClosed()) con.close(); } catch(Exception e) {}
        }
    }

    /* ========================== */
    /*   Add milestone */
    /* ========================== */

    @GetRequest("/planner/milestones/add")
    public void addMilestoneForm(HttpRequest request) throws IOException {
        // Redirect the user to the respected page
        User user; // If user == null then a redirect has happened
        if ((user = getUserOrLogin(request)) == null) return;

        Map<String,String> query = request.getQuery();
        int plannerId = -1;
        int errorId = -1;

        // Check if we had any errors        
        if (query.containsKey("id")) {
            try {
                plannerId = Integer.parseInt(query.get("id").trim());
            } catch (NumberFormatException nfe) { 
                plannerId = -1;
            }

            try {
                if (query.containsKey("error")) 
                    errorId = Integer.parseInt(query.get("error").trim());
            } catch (NumberFormatException nfe) { 
                errorId = -1;
            }
        }

        // We failed to get the planner id from the query
        if (plannerId == -1) {
            request.SendMessagePage("Could not find planner", "The planner id specified does not exist", 400);
            return;
        }

        // Try to find the planner
        Connection con = null;
        Planner planner = null;
        try {
            // Get the db and run the query
            con = SqliteDBCon.GetConnection();

            // Find planner by id and check if the author is the current user
            QueryResults<Planner> dbQuery = Planner
                .where(con, "id", "=", plannerId)
                .andWhere("author", "=", user.getId())
                    .setLimit(1)
                    .execute();

            // Get the first item in the query
            if (dbQuery.Successful) {
                planner = dbQuery.first();
            }
        } catch (SQLException ex) {
            // Throw the error
            request.throwException(ex);
            return;
        } finally {
            // Close the database connection
            try { if (con != null && con.isClosed()) con.close(); } catch(Exception e) {}
        }

        // Check if the planner exists
        if (planner == null) {
            request.SendMessagePage("Could not find planner", "The planner id specified does not exist", 400);
            return;
        }

        // Render the page
        new Renderer().setUser(user)
            .render(request, "planner/milestones/new", 200, HashBuilder.<String,Object>builder()
                .put("planner", planner)
                .put("error", errorId)
            .build());
    }

    @PostRequest("/planner/milestones/add")
    public void addMilestone(HttpRequest request) throws IOException {
        // Redirect the user to the respected page
        User user; // If user == null then a redirect has happened
        if ((user = getUserOrLogin(request)) == null) return;

        Map<String,String> post = request.GetPostForm();
        Map<String,String> query = request.getQuery();
        int plannerId = -1;

        // Check if we had any errors        
        if (query.containsKey("id")) {
            try {
                plannerId = Integer.parseInt(query.get("id").trim());
            } catch (NumberFormatException nfe) { 
                plannerId = -1;
            }
        }

        // We failed to get the planner id from the query
        if (plannerId == -1) {
            request.SendMessagePage("Could not find planner", "The planner id specified does not exist", 400);
            return;
        }

        // Error checking
        String[] requiredKeys = new String[] { "name", "description", "due" };
        for (String input : requiredKeys) {
            if (!post.containsKey(input)) {
                // Display error message
                request.Redirect("/planner/milestones/add?id=" + plannerId + "&error=1" );
                return;
            }
        }

        // Try to find the planner
        Connection con = null;
        Planner planner = null;
        try {
            // Get the db and run the query
            con = SqliteDBCon.GetConnection();

            // Find planner by id and check if the author is the current user
            QueryResults<Planner> dbQuery = Planner
                .where(con, "id", "=", plannerId)
                .andWhere("author", "=", user.getId())
                    .setLimit(1)
                    .execute();

            // Get the first item in the query
            if (dbQuery.Successful) {
                planner = dbQuery.first();

                // Create a new milestone
                Milestone ms = new Milestone(con, planner)
                    .setName(post.get("name").trim())
                    .setDescription(post.get("description").trim())
                    .setDueDate(post.get("due").trim());

                if (post.containsKey("completion"))
                    ms.setCompletedOn(post.get("completion").trim());
                
                // Save changes
                ms.save();

                request.Redirect("/planner/view?id=" + plannerId);   
                return;     
            }
        } catch (SQLException | MissingColumnValueException ex) {
            // Throw the error
            request.throwException(ex);
            return;
        } finally {
            // Close the database connection
            try { if (con != null && con.isClosed()) con.close(); } catch(Exception e) {}
        }

        // Check if the planner exists
        if (planner == null) {
            request.SendMessagePage("Could not find planner", "The planner id specified does not exist", 400);
            return;
        }

        request.Redirect("/planner/view?id=" + plannerId);        
    }
   
    /* ========================== */
    /*   Edit milestone */
    /* ========================== */

    @GetRequest("/planner/milestones/edit")
    public void editMilestoneForm(HttpRequest request) throws IOException {
        // Redirect the user to the respected page
        User user; // If user == null then a redirect has happened
        if ((user = getUserOrLogin(request)) == null) return;

        Map<String,String> query = request.getQuery();
        int milestoneId = -1;
        int errorId = -1;

        // Check if we had any errors        
        if (query.containsKey("id")) {
            try {
                milestoneId = Integer.parseInt(query.get("id").trim());
            } catch (NumberFormatException nfe) { 
                milestoneId = -1;
            }

            try {
                if (query.containsKey("error")) 
                    errorId = Integer.parseInt(query.get("error").trim());
            } catch (NumberFormatException nfe) { 
                errorId = -1;
            }
        }

        // We failed to get the milestone id from the query
        if (milestoneId == -1) {
            request.SendMessagePage("Could not find milestone", "The milestone id specified does not exist", 400);
            return;
        }

        // Try to find the milestone
        Connection con = null;
        Milestone milestone = null;
        try {
            // Get the db and run the query
            con = SqliteDBCon.GetConnection();

            // Find milestone by id
            QueryResults<Milestone> dbQuery = Milestone
                .where(con, "id", "=", milestoneId)
                    .setLimit(1)
                    .execute();

            // Get the first item in the query
            if (dbQuery.Successful) {
                milestone = dbQuery.first();

                // Check if the user cannot edit this milestone 
                if (!milestone.userCanEdit(con, user)) {
                    request.SendMessagePage("You cannot edit this milestone", "You dont own this planner.", 400);
                    return;
                }
            }
        } catch (SQLException ex) {
            // Throw the error
            request.throwException(ex);
            return;
        } finally {
            // Close the database connection
            try { if (con != null && con.isClosed()) con.close(); } catch(Exception e) {}
        }

        // Check if the milestone exists
        if (milestone == null) {
            request.SendMessagePage("Could not find planner", "The planner id specified does not exist", 400);
            return;
        }

        // Render the page
        new Renderer().setUser(user)
            .render(request, "planner/milestones/edit", 200, HashBuilder.<String,Object>builder()
                .put("ms", milestone)
                .put("error", errorId)
            .build());
    }

    @PostRequest("/planner/milestones/edit")
    public void editMilestone(HttpRequest request) throws IOException {
        // Redirect the user to the respected page
        User user; // If user == null then a redirect has happened
        if ((user = getUserOrLogin(request)) == null) return;

        Map<String,String> post = request.GetPostForm();
        Map<String,String> query = request.getQuery();
        int milestoneId = -1;

        // Check if we had any errors        
        if (query.containsKey("id")) {
            try {
                milestoneId = Integer.parseInt(query.get("id").trim());
            } catch (NumberFormatException nfe) { 
                milestoneId = -1;
            }
        }

        // We failed to get the milestone id from the query
        if (milestoneId == -1) {
            request.SendMessagePage("Could not find milestone", "The milestone id specified does not exist", 400);
            return;
        }

        // Error checking
        String[] requiredKeys = new String[] { "name", "description", "due" };
        for (String input : requiredKeys) {
            if (!post.containsKey(input)) {
                // Display error message
                request.Redirect("/planner/milestones/edit?id=" + milestoneId + "&error=1" );
                return;
            }
        }

        // Try to find the planner
        Connection con = null;
        Milestone ms = null;
        try {
            // Get the db and run the query
            con = SqliteDBCon.GetConnection();

            // Find planner by id and check if the author is the current user
            QueryResults<Milestone> dbQuery = Milestone
                .where(con, "id", "=", milestoneId)
                    .setLimit(1)
                    .execute();

            // Get the first item in the query
            if (dbQuery.Successful) {
                ms = dbQuery.first();

                // Check if the user cannot edit this milestone 
                if (!ms.userCanEdit(con, user)) {
                    request.SendMessagePage("You cannot edit this milestone", "You dont own this planner.", 400);
                    return;
                }
                
                // Update the fields
                ms.setName(post.get("name").trim());
                ms.setDescription(post.get("description").trim());
                ms.setDueDate(post.get("due").trim());

                if (post.containsKey("completion"))
                    ms.setCompletedOn(post.get("completion").trim());

                // Save changes
                ms.save();
                
                request.Redirect("/planner/view?id=" + ms.getPlannerId());   
                return;     
            }
        } catch (SQLException | MissingColumnValueException ex) {
            // Throw the error
            request.throwException(ex);
            return;
        } finally {
            // Close the database connection
            try { if (con != null && con.isClosed()) con.close(); } catch(Exception e) {}
        }

        // Check if the planner exists
        if (ms == null) {
            request.SendMessagePage("Could not find planner", "The planner id specified does not exist", 400);
            return;
        }

        request.Redirect("/planner/view?id=" + ms.getPlannerId());
    }
}