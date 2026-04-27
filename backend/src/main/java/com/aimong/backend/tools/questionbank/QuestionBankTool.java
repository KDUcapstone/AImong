package com.aimong.backend.tools.questionbank;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class QuestionBankTool {

    private QuestionBankTool() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            throw new IllegalArgumentException("usage: QuestionBankTool <generate|validate|review|sql|serve-sql|full> <input> <output>");
        }

        ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        QuestionBankGenerator generator = new QuestionBankGenerator();
        QuestionBankValidator validator = new QuestionBankValidator();
        QuestionBankReviewWriter reviewWriter = new QuestionBankReviewWriter();
        QuestionBankSqlExporter sqlExporter = new QuestionBankSqlExporter(objectMapper);
        QuestionBankAuditLoader auditLoader = new QuestionBankAuditLoader(objectMapper);

        String command = args[0];
        Path input = Path.of(args[1]);
        Path output = Path.of(args[2]);

        switch (command) {
            case "generate" -> {
                CurriculumManifest manifest = objectMapper.readValue(Files.readString(input), CurriculumManifest.class);
                QuestionBankDraft draft = generator.generate(manifest);
                writeParent(output);
                objectMapper.writeValue(output.toFile(), draft);
            }
            case "validate" -> {
                QuestionBankDraft draft = objectMapper.readValue(Files.readString(input), QuestionBankDraft.class);
                List<String> errors = validator.validate(draft);
                writeParent(output);
                Files.writeString(output, errors.isEmpty() ? "OK\n" : String.join(System.lineSeparator(), errors) + System.lineSeparator());
                if (!errors.isEmpty()) {
                    throw new IllegalStateException("validation failed");
                }
            }
            case "review" -> {
                QuestionBankDraft draft = objectMapper.readValue(Files.readString(input), QuestionBankDraft.class);
                writeParent(output);
                Files.writeString(output, reviewWriter.write(draft));
            }
            case "sql" -> {
                QuestionBankDraft draft = objectMapper.readValue(Files.readString(input), QuestionBankDraft.class);
                writeParent(output);
                Files.writeString(output, sqlExporter.export(draft));
            }
            case "serve-sql" -> {
                AuditQuestionBank bank = auditLoader.load(input);
                writeParent(output);
                Files.writeString(output, sqlExporter.exportServeBank(bank));
            }
            case "full" -> {
                Path base = output;
                writeParent(base.resolve("placeholder.txt"));
                CurriculumManifest manifest = objectMapper.readValue(Files.readString(input), CurriculumManifest.class);
                QuestionBankDraft draft = generator.generate(manifest);
                List<String> errors = validator.validate(draft);
                if (!errors.isEmpty()) {
                    throw new IllegalStateException(String.join(System.lineSeparator(), errors));
                }
                objectMapper.writeValue(base.resolve("question-bank.json").toFile(), draft);
                Files.writeString(base.resolve("validation.txt"), "OK\n");
                Files.writeString(base.resolve("review.md"), reviewWriter.write(draft));
                Files.writeString(base.resolve("question-bank.sql"), sqlExporter.export(draft));
            }
            default -> throw new IllegalArgumentException("unknown command: " + command);
        }
    }

    private static void writeParent(Path path) throws Exception {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }
}
