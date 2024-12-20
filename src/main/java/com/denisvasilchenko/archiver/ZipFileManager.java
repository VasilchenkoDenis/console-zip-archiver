package com.denisvasilchenko.archiver;

import com.denisvasilchenko.archiver.exception.PathIsNotFoundException;
import com.denisvasilchenko.archiver.exception.WrongZipFileException;
import com.denisvasilchenko.archiver.exception.WrongZipLevelException;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipFileManager {
    // Полный путь zip файла
    private final Path zipFile;

    public ZipFileManager(Path zipFile) {
        this.zipFile = zipFile;
    }

    public void createZip(Path source) throws Exception {
        // Проверка наличия директории создания архива/создание директории при необходимости
        Path zipDirectory = zipFile.getParent();
        if (Files.notExists(zipDirectory))
            Files.createDirectories(zipDirectory);

        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(zipFile))) {

            if (Files.isDirectory(source)) {
                // Получение списка файлов в директории
                FileManager fileManager = new FileManager(source);
                List<Path> fileNames = fileManager.getFileList();

                // добавление файлов в архив
                for (Path fileName : fileNames)
                    addNewZipEntry(zipOutputStream, source, fileName);

            } else if (Files.isRegularFile(source)) {

                // получение директории и имени файла при архивировании отдельного файла
                addNewZipEntry(zipOutputStream, source.getParent(), source.getFileName());
            } else {

                // если источник не файл и не директория
                throw new PathIsNotFoundException();
            }
        }
    }

    public void extractAll(Path outputFolder) throws Exception {
        // проверяется наличие zip файла
        if (!Files.isRegularFile(zipFile)) {
            throw new WrongZipFileException();
        }

        try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(zipFile))) {
            // создание директории вывода при необходимости
            if (Files.notExists(outputFolder))
                Files.createDirectories(outputFolder);

            // чтение содержимого zip-потока
            ZipEntry zipEntry = zipInputStream.getNextEntry();

            while (zipEntry != null) {
                String fileName = zipEntry.getName();
                Path fileFullName = outputFolder.resolve(fileName);

                Path parent = fileFullName.getParent();
                if (Files.notExists(parent))
                    Files.createDirectories(parent);

                try (OutputStream outputStream = Files.newOutputStream(fileFullName)) {
                    copyData(zipInputStream, outputStream);
                }
                zipEntry = zipInputStream.getNextEntry();
            }
        }
    }

    public void removeFile(Path path) throws Exception {
        removeFiles(Collections.singletonList(path));
    }

    public void removeFiles(List<Path> pathList) throws Exception {

        if (!Files.isRegularFile(zipFile)) {
            throw new WrongZipFileException();
        }

        // создание временного файла
        Path tempZipFile = Files.createTempFile(null, null);

        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(tempZipFile))) {
            try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(zipFile))) {

                ZipEntry zipEntry = zipInputStream.getNextEntry();
                while (zipEntry != null) {

                    Path archivedFile = Paths.get(zipEntry.getName());

                    if (!pathList.contains(archivedFile)) {
                        String fileName = zipEntry.getName();
                        zipOutputStream.putNextEntry(new ZipEntry(fileName));

                        copyData(zipInputStream, zipOutputStream);

                        zipOutputStream.closeEntry();
                        zipInputStream.closeEntry();
                    } else {
                        ConsoleHelper.writeMessage(String.format("Файл '%s' удален из архива.", archivedFile.toString()));
                    }
                    zipEntry = zipInputStream.getNextEntry();
                }
            }
        }

        // замена старого файла новым(исходного временным)
        Files.move(tempZipFile, zipFile, StandardCopyOption.REPLACE_EXISTING);
    }

    public void changeCompressionLevel(int level) throws Exception {
        if (!Files.isRegularFile(zipFile)) {
            throw new WrongZipFileException();
        }
        if (level < 0 || level > 9) {
            throw new WrongZipLevelException();
        }
        Path tempFile = Files.createTempFile(null, null);
        try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(zipFile));
             ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(tempFile))) {
            zipOutputStream.setLevel(level);
            ZipEntry zipEntry = zipInputStream.getNextEntry();
            while (zipEntry != null) {
                String fileName = zipEntry.getName();
                zipOutputStream.putNextEntry(new ZipEntry(fileName));
                copyData(zipInputStream, zipOutputStream);
                zipOutputStream.closeEntry();
                zipInputStream.closeEntry();
                zipEntry = zipInputStream.getNextEntry();
            }
            Files.move(tempFile, zipFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public void addFile(Path absolutePath) throws Exception {
        addFiles(Collections.singletonList(absolutePath));
    }

    public void addFiles(List<Path> absolutePathList) throws Exception {

        if (!Files.isRegularFile(zipFile)) {
            throw new WrongZipFileException();
        }

        Path tempZipFile = Files.createTempFile(null, null);
        List<Path> archiveFiles = new ArrayList<>();

        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(tempZipFile))) {
            try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(zipFile))) {

                ZipEntry zipEntry = zipInputStream.getNextEntry();
                while (zipEntry != null) {
                    String fileName = zipEntry.getName();
                    archiveFiles.add(Paths.get(fileName));

                    zipOutputStream.putNextEntry(new ZipEntry(fileName));
                    copyData(zipInputStream, zipOutputStream);

                    zipInputStream.closeEntry();
                    zipOutputStream.closeEntry();

                    zipEntry = zipInputStream.getNextEntry();
                }
            }

            // добавление в архив новых файлов
            for (Path file : absolutePathList) {
                if (Files.isRegularFile(file)) {
                    if (archiveFiles.contains(file.getFileName()))
                        ConsoleHelper.writeMessage(String.format("Файл '%s' уже существует в архиве.", file.toString()));
                    else {
                        addNewZipEntry(zipOutputStream, file.getParent(), file.getFileName());
                        ConsoleHelper.writeMessage(String.format("Файл '%s' добавлен в архиве.", file.toString()));
                    }
                } else
                    throw new PathIsNotFoundException();
            }
        }


        Files.move(tempZipFile, zipFile, StandardCopyOption.REPLACE_EXISTING);
    }

    public List<FileProperties> getFilesList() throws Exception {

        if (!Files.isRegularFile(zipFile)) {
            throw new WrongZipFileException();
        }

        List<FileProperties> files = new ArrayList<>();

        try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry zipEntry = zipInputStream.getNextEntry();

            while (zipEntry != null) {
                // чтение файла для получения информации о полях "размер"/"сжатый размер"
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                copyData(zipInputStream, baos);

                FileProperties file = new FileProperties(zipEntry.getName(), zipEntry.getSize(), zipEntry.getCompressedSize(), zipEntry.getMethod());
                files.add(file);
                zipEntry = zipInputStream.getNextEntry();
            }
        }

        return files;
    }

    private void addNewZipEntry(ZipOutputStream zipOutputStream, Path filePath, Path fileName) throws Exception {
        Path fullPath = filePath.resolve(fileName);
        try (InputStream inputStream = Files.newInputStream(fullPath)) {
            ZipEntry entry = new ZipEntry(fileName.toString());

            zipOutputStream.putNextEntry(entry);

            copyData(inputStream, zipOutputStream);

            zipOutputStream.closeEntry();
        }
    }

    private void copyData(InputStream in, OutputStream out) throws Exception {
        byte[] buffer = new byte[8 * 1024];
        int len;
        while ((len = in.read(buffer)) > 0) {
            out.write(buffer, 0, len);
        }
    }
}
