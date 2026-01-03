package me.putindeer.miscolegio.question;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.putindeer.miscolegio.Main;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
public class QuestionManager {
    private final Main plugin;
    @Getter
    private final File file;
    @Getter
    private final List<Question> questions = new ArrayList<>();

    private final CSVFormat CSV_FORMAT = CSVFormat.DEFAULT.builder()
            .setHeader("id", "question", "optionA", "optionB", "optionC", "optionD", "answer", "level", "guaranteed")
            .setSkipHeaderRecord(true)
            .get();

    public QuestionManager(Main plugin) {
        this.plugin = plugin;
        file = new File(plugin.getDataFolder(), "questions.csv");
        initialize();
        loadQuestions();
    }

    private void initialize() {
        if (!file.exists()) {
            try {
                boolean created = file.createNewFile();
                if (created) {
                    plugin.utils.log("El archivo questions.csv se ha creado correctamente.");
                }
                try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8);
                     CSVPrinter printer = new CSVPrinter(writer, CSV_FORMAT)) {
                    plugin.utils.log("Creando el formato de questions.csv... ");
                    printer.printRecord("ID", "Pregunta", "Opción A", "Opción B", "Opción C", "Opción D", "Opción correcta", "Nivel", "Garantizado");
                    printer.flush();
                }
                plugin.utils.log("Se ha creado el archivo questions.csv con el formato correcto.");
            } catch (IOException e) {
                plugin.utils.severe("Ha ocurrido un error mientras se intentaba crear el archivo questions.csv: " + e.getMessage());
            }
        }
    }

    private void loadQuestions() {
        questions.clear();

        try (Reader reader = new FileReader(file, StandardCharsets.UTF_8);
             CSVParser parser = CSVParser.builder().setReader(reader).setFormat(CSV_FORMAT).get()) {

            for (CSVRecord record : parser) {
                Question question = Question.fromCSVRecord(record);
                if (question != null) {
                    questions.add(question);
                }
            }
            plugin.utils.log("Se han cargado " + questions.size() + " preguntas de questions.csv");
        } catch (IOException e) {
            plugin.utils.severe("Ha ocurrido un error mientras se cargaban las preguntas: " + e.getMessage());
        }
    }

    private void saveToCsv() {
        try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(writer, CSV_FORMAT)) {

            printer.printRecord("ID", "Pregunta", "Opción A", "Opción B", "Opción C", "Opción D", "Opción correcta", "Nivel", "Ronda garantizada");

            for (Question question : questions) {
                printer.printRecord(question.toCSVArray());
            }

            printer.flush();
        } catch (IOException e) {
            plugin.utils.severe("Ha ocurrido un error mientras se guardaban las preguntas: " + e.getMessage());
        }
    }

    public void reload() {
        loadQuestions();
    }

    public boolean questionExists(String id) {
        return questions.stream().anyMatch(question -> question.getId().equals(id));
    }

    public void addQuestion(Question question) {
        questions.add(question);
        saveToCsv();
    }

    public boolean deleteQuestion(String id) {
        boolean removed = questions.removeIf(question -> question.getId().equals(id));
        if (removed) {
            saveToCsv();
        }
        return removed;
    }

    public Optional<Question> getQuestion(String id) {
        return questions.stream()
                .filter(question -> question.getId().equals(id))
                .findFirst();
    }
}
