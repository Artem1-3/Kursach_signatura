package com.example;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class SignatureDatabase {
    private List<FileSignature> signatures;
    private static final String DATABASE_FILE = "signatures.dat";

    /**
     * Конструктор для создания базы данных.
     * Загружает существующую базу данных или создает новую с примерами.
     */
    public SignatureDatabase() {
        signatures = new ArrayList<>();
        loadDatabase();
    }

    /**
     * Добавить сигнатуру в базу данных.
     *
     * @param signature сигнатура для добавления
     */
    public void addSignature(FileSignature signature) {
        signatures.add(signature);
        saveDatabase();
    }

    /**
     * Получить все сигнатуры из базы данных.
     *
     * @return список всех сигнатур
     */
    public List<FileSignature> getSignatures() {
        return new ArrayList<>(signatures);
    }

    /**
     * Загружает базу данных из файла. Если файл не существует или возникла ошибка,
     * создается новая база данных с примерами.
     */
    private void loadDatabase() {
        Path databasePath = Paths.get(DATABASE_FILE);

        if (!Files.exists(databasePath)) {
            System.out.println("База данных не найдена. Создаю новую базу с примерами...");
            createDefaultSignatures();
            saveDatabase();
            return;
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(DATABASE_FILE))) {
            signatures = (List<FileSignature>) ois.readObject();
            System.out.println("База данных успешно загружена");
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Ошибка при чтении базы данных. Создаю новую базу...");
            createDefaultSignatures();
            saveDatabase();
        }
    }

    /**
     * Создает стандартные сигнатуры для базы данных.
     */
    private void createDefaultSignatures() {
        signatures.clear();

        // Добавляем стандартные сигнатуры
        signatures.add(new FileSignature(
                "PDF Документ",
                new byte[] {0x25, 0x50, 0x44, 0x46, 0x2D},
                "pdf"
        ));

        signatures.add(new FileSignature(
                "PNG Изображение",
                new byte[] {(byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A},
                "png"
        ));

        signatures.add(new FileSignature(
                "JPEG Изображение",
                new byte[] {(byte)0xFF, (byte)0xD8, (byte)0xFF},
                "jpg"
        ));

        signatures.add(new FileSignature(
                "ZIP Архив",
                new byte[] {0x50, 0x4B, 0x03, 0x04},
                "zip"
        ));

        System.out.println("Создано " + signatures.size() + " стандартных сигнатур");
    }

    /**
     * Сохраняет текущую базу данных в файл.
     */
    private void saveDatabase() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DATABASE_FILE))) {
            oos.writeObject(signatures);
            System.out.println("База данных успешно сохранена в " + DATABASE_FILE);
        } catch (IOException e) {
            System.err.println("Ошибка при сохранении базы данных: " + e.getMessage());
            e.printStackTrace();
        }
    }
}