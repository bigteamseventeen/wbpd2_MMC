package com.bigteamseventeen.wpd2_ah.milestones.console.commands;

import com.bigteamseventeen.wpd2_ah.milestones.Main;
import com.bigteamseventeen.wpd2_ah.milestones.console.IConsoleCommand;
import com.bigteamseventeen.wpd2_ah.milestones.console.TerminalProcessor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ExitCommand implements IConsoleCommand {
    final static Logger logger = LogManager.getLogger();

    @Override public String getCommand() {
        return "exit";
    }

    @Override public String getDescription() {
        return "Exist the application";
    }

    @Override public String getParameterHint() {
        return null;
    }

    @Override public void process(TerminalProcessor processor, String[] args) {
        logger.warn("§cUser shutdown command recieved§r, Server shutting down...");

        // Shutdown the processor
        processor.shutdown();
    }
}