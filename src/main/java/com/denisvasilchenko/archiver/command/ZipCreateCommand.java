package com.denisvasilchenko.archiver.command;


import com.denisvasilchenko.archiver.ConsoleHelper;
import com.denisvasilchenko.archiver.ZipFileManager;
import com.denisvasilchenko.archiver.exception.PathIsNotFoundException;

import java.nio.file.Path;
import java.nio.file.Paths;

public class ZipCreateCommand extends ZipCommand{
    @Override
    public void execute() throws Exception {
        try {
            ConsoleHelper.writeMessage("Создание архива.");
            ZipFileManager zipFileManager = getZipFileManager();
            ConsoleHelper.writeMessage("Введите полное имя файла или директории которые необходимо заархивировать:");
            Path fileOrDirectory = Paths.get(ConsoleHelper.readString());
            zipFileManager.createZip(fileOrDirectory);
            ConsoleHelper.writeMessage("Архив создан.");
        }
        catch (PathIsNotFoundException nofFound){
            ConsoleHelper.writeMessage("Вы неверно указали имя файла или директории.");
        }

    }
}
