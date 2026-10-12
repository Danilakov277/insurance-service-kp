package by.bsuir.insurance.client.ui.admin;

import by.bsuir.insurance.client.ui.Ui;

import javax.swing.JFileChooser;
import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.List;

/** Выгрузка отчёта в текстовый файл с выравниванием столбцов (UC17). */
final class ReportExporter {

    private ReportExporter() {
    }

    static void export(Component owner, String title, String period, String[] header, List<String[]> rows,
                       String suggestedName) {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(suggestedName));
        if (fc.showSaveDialog(owner) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = fc.getSelectedFile();
        try {
            write(file, title, period, header, rows);
            Ui.info(owner, "Отчёт сохранён: " + file.getAbsolutePath());
        } catch (IOException e) {
            Ui.error(owner, "Не удалось сохранить файл: " + e.getMessage());
        }
    }

    static void write(File file, String title, String period, String[] header, List<String[]> rows) throws IOException {
        int[] w = new int[header.length];
        for (int i = 0; i < header.length; i++) {
            w[i] = header[i].length();
        }
        for (String[] r : rows) {
            for (int i = 0; i < r.length; i++) {
                w[i] = Math.max(w[i], r[i].length());
            }
        }
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8))) {
            out.println(title);
            out.println("Период: " + period);
            out.println("Сформирован: " + Ui.format(LocalDateTime.now()));
            out.println();
            out.println(line(header, w));
            StringBuilder sep = new StringBuilder();
            for (int x : w) {
                for (int k = 0; k < x + 3; k++) {
                    sep.append('-');
                }
            }
            out.println(sep);
            for (String[] r : rows) {
                out.println(line(r, w));
            }
        }
    }

    private static String line(String[] cells, int[] w) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cells.length; i++) {
            sb.append(String.format("%-" + w[i] + "s", cells[i])).append(" | ");
        }
        return sb.toString();
    }
}
