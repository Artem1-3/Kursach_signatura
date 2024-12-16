package com.example;

import java.io.Serializable;

/**
 * Класс, представляющий файл с сигнатурой.
 * Содержит имя файла, его сигнатуру и расширение.
 * Используется для сериализации данных файла.
 */
public class FileSignature implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private byte[] signature;
    private String extension;

    /**
     * Конструктор для создания объекта FileSignature.
     *
     * @param name      имя файла
     * @param signature сигнатура файла
     * @param extension расширение файла
     */
    public FileSignature(String name, byte[] signature, String extension) {
        this.name = name;
        this.signature = signature;
        this.extension = extension;
    }

    /**
     * Получить имя файла.
     *
     * @return имя файла
     */
    public String getName() { return name; }
    public byte[] getSignature() { return signature; }
    public String getExtension() { return extension; }
}