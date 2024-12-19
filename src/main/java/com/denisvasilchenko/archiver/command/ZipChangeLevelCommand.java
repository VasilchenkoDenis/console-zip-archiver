package com.denisvasilchenko.archiver.command;


import com.denisvasilchenko.archiver.ConsoleHelper;
import com.denisvasilchenko.archiver.ZipFileManager;
import com.denisvasilchenko.archiver.exception.WrongZipLevelException;

public class ZipChangeLevelCommand extends ZipCommand {
    @Override
    public void execute() throws Exception {
        try {
            ConsoleHelper.writeMessage("Изменение степени сжатия архива.");
            ZipFileManager zipFileManager = getZipFileManager();
            ConsoleHelper.writeMessage("Введите новую степень сжатия с диапазоне от 0 до 9:");
            zipFileManager.changeCompressionLevel(ConsoleHelper.readInt());
            ConsoleHelper.writeMessage("Степень сжатия архива изменена.");
        }
        catch (WrongZipLevelException wrongZipLevelException){
            ConsoleHelper.writeMessage("Введенный уровень сжатия не поддерживается.");
        }
    }
}
