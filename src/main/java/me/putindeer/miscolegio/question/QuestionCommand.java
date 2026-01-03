package me.putindeer.miscolegio.question;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.putindeer.miscolegio.Main;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class QuestionCommand implements TabExecutor, Listener {
    private final Main plugin;
    private final Map<UUID, QuestionCreation> creationSessions = new HashMap<>();

    public QuestionCommand(Main plugin) {
        this.plugin = plugin;
        Objects.requireNonNull(plugin.getCommand("question")).setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Este comando solo puede ser usado por jugadores");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "create" -> handleCreate(player, args);
            case "list" -> handleList(player);
            case "delete" -> handleDelete(player, args);
            case "cancel" -> handleCancel(player);
            case "reload" -> handleReload(player);
            default -> sendHelp(player);
        }

        return true;
    }

    private void sendHelp(Player player) {
        plugin.utils.message(player, "<red>=== Guía de comando /question ===",
                "<yellow>/question create <id> <gray>- Crear pregunta",
                "<yellow>/question list <gray>- Muestra la lista de preguntas",
                "<yellow>/question delete <id> <gray>- Eliminar pregunta",
                "<yellow>/question reload <gray>- Recarga las preguntas desde el archivo questions.csv");
    }

    private void handleCreate(Player player, String[] args) {
        if (args.length < 2) {
            plugin.utils.message(player, "<red>Uso: /question create <id>");
            return;
        }

        String questionId = args[1];

        if (plugin.question.questionExists(questionId)) {
            plugin.utils.message(player, "<red>¡Ya existe una pregunta con ID '<yellow>" + questionId + "</yellow>'!");
            return;
        }

        if (isCreating(player.getUniqueId())) {
            plugin.utils.message(player, "<red>¡Ya estás creando una pregunta! Usa <yellow>/question cancel</yellow> para cancelar.");
            return;
        }

        startCreation(player, questionId);
        plugin.utils.message(player, "<green>Creando pregunta con ID: <yellow>" + questionId,
                "<green>Si te equivocas y necesitas cancelar, usa <red>/question cancel</red.",
                "<yellow>Por favor ingresa el texto de la pregunta:");
    }

    private void handleList(Player player) {
        if (plugin.question.getQuestions().isEmpty()) {
            plugin.utils.message(player, "<yellow>No se han creado preguntas todavía. Si has actualizado questions.csv, usa /question reload para cargar las preguntas nuevas.");
            return;
        }

        plugin.utils.message(player, "<gold>====== Lista de Preguntas ======");

        for (Question question : plugin.question.getQuestions()) {
            messageQuestion(player, question);
            plugin.utils.message(player, "<gold>=============================");
        }
    }

    private void handleDelete(Player player, String[] args) {
        if (args.length < 2) {
            plugin.utils.message(player, "<red>Uso: /question delete <id>");
            return;
        }

        String deleteId = args[1];
        if (plugin.question.deleteQuestion(deleteId)) {
            plugin.utils.message(player, "<green>✓ ¡Pregunta eliminada exitosamente!");
        } else {
            plugin.utils.message(player, "<red>No se encontró la pregunta con ID '" + deleteId + "'");
        }
    }

    private void handleCancel(Player player) {
        if (isCreating(player.getUniqueId())) {
            cancelCreation(player.getUniqueId());
            plugin.utils.message(player, "<yellow>Creación de pregunta cancelada.");
        } else {
            plugin.utils.message(player, "<red>No estás creando ninguna pregunta.");
        }
    }

    private void handleReload(Player player) {
        plugin.question.reload();
        plugin.utils.message(player, "<green>Se han actualizado las preguntas en base a questions.csv correctamente.");
    }

    public void startCreation(Player player, String id) {
        creationSessions.put(player.getUniqueId(), new QuestionCreation(id));
    }

    public boolean isCreating(UUID uuid) {
        return creationSessions.containsKey(uuid);
    }

    public void cancelCreation(UUID uuid) {
        creationSessions.remove(uuid);
    }

    private void createQuestion(Player player, QuestionCreation session) {
        Question question = new Question(
                session.id,
                session.question,
                session.optionA,
                session.optionB,
                session.optionC,
                session.optionD,
                session.answer,
                session.level,
                session.guaranteed);

        plugin.question.addQuestion(question);

        plugin.utils.message(player, "<green>✓ ¡Pregunta creada exitosamente!",
                "",
                "<gold>=== Resumen de la Pregunta ===");

        messageQuestion(player, question);

        creationSessions.remove(player.getUniqueId());
    }

    private void messageQuestion(Player player, Question question) {
        plugin.utils.message(player,
                "<yellow>ID: <white>" + question.getId(),
                "<yellow>Pregunta: <white>" + question.getQuestion(),
                formatOption("A", question.getOptionA(), question.getAnswer()),
                formatOption("B", question.getOptionB(), question.getAnswer()),
                formatOption("C", question.getOptionC(), question.getAnswer()),
                formatOption("D", question.getOptionD(), question.getAnswer()),
                "<yellow>Nivel: <aqua>" + question.getLevel(),
                "<yellow>Está garantizada: <light_purple>" + question.getGuaranteedRoundString()
        );
    }

    private String formatOption(String letter, String optionText, String correct) {
        boolean isCorrect = letter.equalsIgnoreCase(correct);
        String color = isCorrect ? "<dark_green>" : "<gray>";
        String textColor = isCorrect ? "<green>" : "<white>";

        return "  " + color + letter + ") " + textColor + optionText;
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!creationSessions.containsKey(uuid)) return;

        event.setCancelled(true);
        QuestionCreation session = creationSessions.get(uuid);
        String message = ((TextComponent) event.message()).content();

        switch (session.step) {
            case 0 -> handleStepQuestion(player, session, message);
            case 1 -> handleStepOption1(player, session, message);
            case 2 -> handleStepOption2(player, session, message);
            case 3 -> handleStepOption3(player, session, message);
            case 4 -> handleStepOption4(player, session, message);
            case 5 -> handleStepCorrectAnswer(player, session, message);
            case 6 -> handleStepLevel(player, session, message);
            case 7 -> handleStepGuaranteed(player, session, message);
        }
    }

    private void handleStepQuestion(Player player, QuestionCreation session, String message) {
        session.question = message;
        session.step++;
        plugin.utils.message(player, "<green>¡Pregunta guardada!",
                "<yellow>Ingresa la opción A:");
    }

    private void handleStepOption1(Player player, QuestionCreation session, String message) {
        session.optionA = message;
        session.step++;
        plugin.utils.message(player, "<green>¡Opción A guardada!",
                "<yellow>Ingresa la opción B:");
    }

    private void handleStepOption2(Player player, QuestionCreation session, String message) {
        session.optionB = message;
        session.step++;
        plugin.utils.message(player, "<green>¡Opción B guardada!",
                "<yellow>Ingresa la opción C:");
    }

    private void handleStepOption3(Player player, QuestionCreation session, String message) {
        session.optionC = message;
        session.step++;
        plugin.utils.message(player, "<green>¡Opción C guardada!",
                "<yellow>Ingresa la opción D:");
    }

    private void handleStepOption4(Player player, QuestionCreation session, String message) {
        session.optionD = message;
        session.step++;
        plugin.utils.message(player, "<green>¡Opción D guardada!",
                "<yellow>¿Cuál opción es correcta? (A-D):",
                "  <gray>A) <white>" + session.getOptionA(),
                "  <gray>B) <white>" + session.getOptionB(),
                "  <gray>C) <white>" + session.getOptionC(),
                "  <gray>D) <white>" + session.getOptionD());
    }

    private void handleStepCorrectAnswer(Player player, QuestionCreation session, String message) {
        String answer = message.toUpperCase();
        if (!answer.matches("[A-D]")) {
            plugin.utils.message(player, "<red>¡Por favor ingresa una letra entre A y D!");
            return;
        }
        session.answer = answer;
        session.step++;
        plugin.utils.message(player, "<green>¡Respuesta correcta guardada!",
                "<yellow>Ingresa el nivel de dificultad:",
                "<gray>Opciones: <white>kinder<gray>, <white>basica<gray>, <white>media<gray>, <white>universidad");
    }

    private void handleStepLevel(Player player, QuestionCreation session, String message) {
        String level = message.toLowerCase();
        if (!level.equals("kinder") && !level.equals("basica") &&
                !level.equals("media") && !level.equals("universidad")) {
            plugin.utils.message(player, "<red>¡Nivel inválido! Usa: kinder, basica, media o universidad");
            return;
        }
        session.level = QuestionLevel.valueOf(level.toUpperCase());
        session.step++;
        plugin.utils.message(player, "<green>¡Nivel guardado!",
                "<yellow>¿Debe esta pregunta estar asegurada sí o sí? (Ingresa 'SI' o 'NO'):");
    }

    private void handleStepGuaranteed(Player player, QuestionCreation session, String message) {
        String input = message.trim().toLowerCase();

        if (input.equals("si") || input.equals("sí") || input.equals("yes")) {
            session.setGuaranteed(true);
            createQuestion(player, session);
            return;
        }

        if (input.equals("no") || input.equals("none") || input.equals("ninguna")) {
            session.setGuaranteed(false);
            createQuestion(player, session);
            return;
        }

        plugin.utils.message(player, "<red>Responde con 'sí' o 'no'.");
    }


    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            return Collections.emptyList();
        }

        List<String> list = new ArrayList<>();

        if (args.length == 1) {
            list.addAll(Arrays.asList("create", "list", "delete", "reload"));

            if (isCreating(player.getUniqueId())) {
                list.add("cancel");
            }
        }

        if (args.length == 2) {
            String subCommand = args[0].toLowerCase();

            if (subCommand.equals("delete")) {
                list.addAll(plugin.question.getQuestions().stream().map(Question::getId).toList());
            }
        }

        list.removeIf(s -> s == null || !s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()));
        Collections.sort(list);
        return list;
    }
}