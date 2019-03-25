package uk.ac.alc.wpd2.callumcarmicheal.messageboard.web;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import uk.ac.alc.wpd2.callumcarmicheal.messageboard.Topic;
import uk.ac.alc.wpd2.callumcarmicheal.messageboard.TopicMessage;
import uk.ac.alc.wpd2.callumcarmicheal.messageboard.web.advanced.AServer;

public class Main {
	private static int PORT = 8080;
    
    
    public static void main(String[] args) {
        try {
            BasicConfigurator.configure();

            System.err.println("Starting server!");

            ServiceWarmup();
    
            // AServer server = new AServer(PORT);
            // server.Start();
            
           Server server = new Server(PORT);
           server.Start();
            
            System.err.println("Server started on port: " + PORT + "!");
        } catch (Exception e) {
            System.err.println("Failed to start server!");
            e.printStackTrace();
        }
    }

    private static void ServiceWarmup() {
        Topic t = new Topic("Default Topic");
        t.setDescription("A generic place where people can come together and discuss anything.");
        t.addNewMessage(new TopicMessage("SYSTEM", "New topic created!"));
        t.addNewMessage(new TopicMessage("Callum", "Hey guys :-) !"));
        
        WebBoard.MB.addTopic(t);
    }
}
