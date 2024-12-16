package com.example;

// MainWindow.java

import javax.swing.*;
import java.awt.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Главное окно приложения для поиска файлов по сигнатуре.
 */
public class MainWindow extends JFrame {
    private SignatureDatabase database;
    private JComboBox<String> signatureCombo;
    private JTextField pathField;
    private JTextArea resultArea;
    private JPanel statusPanel;
    private JProgressBar progressBar;
    private JButton searchButton;

    public MainWindow() {
        database = new SignatureDatabase();
        initializeUI();
    }

    private void initializeUI() {
        setTitle("Поиск файлов по сигнатуре");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        JPanel topPanel = createTopPanel();
        JPanel centerPanel = createCenterPanel();
        statusPanel = createStatusPanel();

        add(topPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(statusPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(800, 600));
    }

    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        signatureCombo = new JComboBox<>();
        updateSignatureCombo();
        pathField = new JTextField(30);
        JButton browseButton = new JButton("Обзор");
        searchButton = new JButton("Поиск");
        JButton addSignatureButton = new JButton("Добавить сигнатуру");

        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // Первая строка - Сигнатура
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        panel.add(new JLabel("Сигнатура:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(signatureCombo, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0.0;
        panel.add(addSignatureButton, gbc);

        // Вторая строка - Путь
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.0;
        panel.add(new JLabel("Путь:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(pathField, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0.0;
        panel.add(browseButton, gbc);

        // Третья строка - Кнопка поиска
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(searchButton, gbc);

        browseButton.addActionListener(e -> choosePath());
        searchButton.addActionListener(e -> startSearch());
        addSignatureButton.addActionListener(e -> addSignature());

        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        return panel;
    }

    private JPanel createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        resultArea = new JTextArea(20, 50);
        resultArea.setEditable(false);
        resultArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        panel.add(new JScrollPane(resultArea), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        panel.add(progressBar, BorderLayout.CENTER);
        panel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        return panel;
    }

    private void choosePath() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            pathField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void startSearch() {
        searchButton.setEnabled(false);
        progressBar.setVisible(true);

        // Запускаем поиск в отдельном потоке
        CompletableFuture.runAsync(this::searchFiles)
                .whenComplete((result, ex) -> {
                    SwingUtilities.invokeLater(() -> {
                        searchButton.setEnabled(true);
                        progressBar.setVisible(false);
                    });
                });
    }

    private void searchFiles() {
        String path = pathField.getText();
        if (path.isEmpty()) {
            showError("Выберите путь для поиска");
            return;
        }

        if (signatureCombo.getSelectedIndex() == -1) {
            showError("Выберите тип файла для поиска");
            return;
        }

        FileSignature signature = database.getSignatures().get(signatureCombo.getSelectedIndex());
        updateResultArea("Начинаю поиск файлов...\n\n");

        try {
            AtomicInteger foundFiles = new AtomicInteger(0);
            byte[] signatureBytes = signature.getSignature();
            int signatureLength = signatureBytes.length;

            Files.walk(Paths.get(path))
                    .parallel()
                    .filter(Files::isRegularFile)
                    .filter(file -> {
                        try {
                            try (FileInputStream fis = new FileInputStream(file.toFile())) {
                                byte[] buffer = new byte[signatureLength];
                                int bytesRead = fis.read(buffer, 0, signatureLength);
                                if (bytesRead != signatureLength) {
                                    return false;
                                }
                                return Arrays.equals(buffer, signatureBytes);
                            }
                        } catch (IOException e) {
                            return false;
                        }
                    })
                    .forEach(file -> {
                        try {
                            BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
                            updateResultArea(String.format(
                                    "Найден файл %d:\n" +
                                            "Путь: %s\n" +
                                            "Размер: %,d байт\n" +
                                            "Дата создания: %s\n" +
                                            "Дата изменения: %s\n\n",
                                    foundFiles.incrementAndGet(),
                                    file.toString(),
                                    attrs.size(),
                                    attrs.creationTime(),
                                    attrs.lastModifiedTime()
                            ));
                        } catch (IOException e) {
                            System.err.println("Ошибка при чтении атрибутов файла: " + file);
                        }
                    });

            if (foundFiles.get() == 0) {
                updateResultArea("Файлы не найдены.\n");
            } else {
                updateResultArea(String.format("Поиск завершен. Найдено файлов: %d\n",
                        foundFiles.get()));
            }

        } catch (IOException e) {
            showError("Ошибка при поиске файлов: " + e.getMessage());
        }
    }

    private void updateResultArea(String text) {
        SwingUtilities.invokeLater(() -> resultArea.append(text));
    }

    private void showError(String message) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, message, "Ошибка",
                    JOptionPane.ERROR_MESSAGE);
        });
    }

    private void addSignature() {
        JDialog dialog = new JDialog(this, "Добавление сигнатуры", true);
        dialog.setLayout(new GridLayout(4, 2));

        JTextField nameField = new JTextField();
        JTextField signatureField = new JTextField();
        JTextField extensionField = new JTextField();

        dialog.add(new JLabel("Название:"));
        dialog.add(nameField);
        dialog.add(new JLabel("Сигнатура (hex):"));
        dialog.add(signatureField);
        dialog.add(new JLabel("Расширение:"));
        dialog.add(extensionField);

        JButton saveButton = new JButton("Сохранить");
        saveButton.addActionListener(e -> {
            try {
                String[] hexValues = signatureField.getText().split(" ");
                byte[] signature = new byte[hexValues.length];
                for (int i = 0; i < hexValues.length; i++) {
                    signature[i] = (byte) Integer.parseInt(hexValues[i], 16);
                }

                database.addSignature(new FileSignature(
                        nameField.getText(),
                        signature,
                        extensionField.getText()
                ));

                updateSignatureCombo();
                dialog.dispose();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Неверный формат сигнатуры");
            }
        });

        dialog.add(saveButton);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void updateSignatureCombo() {
        signatureCombo.removeAllItems();
        for (FileSignature signature : database.getSignatures()) {
            signatureCombo.addItem(signature.getName());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new MainWindow().setVisible(true);
        });
    }
}