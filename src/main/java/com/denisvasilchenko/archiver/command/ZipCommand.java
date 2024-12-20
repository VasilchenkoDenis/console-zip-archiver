package com.denisvasilchenko.archiver.command;


import com.denisvasilchenko.archiver.ConsoleHelper;
import com.denisvasilchenko.archiver.ZipFileManager;

import java.nio.file.Path;
import java.nio.file.Paths;

public abstract class ZipCommand implements Command{
    public ZipFileManager getZipFileManager() throws Exception{
        ConsoleHelper.writeMessage("Введите полный путь файла архива:");
        Path zipPath = Paths.get(ConsoleHelper.readString());
        return new ZipFileManager(zipPath);

    }

}
