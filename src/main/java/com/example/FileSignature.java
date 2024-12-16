package com.example;

import java.io.Serializable;

public class FileSignature implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private byte[] signature;
    private String extension;

    public FileSignature(String name, byte[] signature, String extension) {
        this.name = name;
        this.signature = signature;
        this.extension = extension;
    }

    public String getName() { return name; }
    public byte[] getSignature() { return signature; }
    public String getExtension() { return extension; }
}