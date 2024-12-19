package com.denisvasilchenko.archiver.command;


import com.denisvasilchenko.archiver.ConsoleHelper;

public class ExitCommand implements Command {
    @Override
    public void execute() throws Exception {
        ConsoleHelper.writeMessage("До встречи!");
    }
}
