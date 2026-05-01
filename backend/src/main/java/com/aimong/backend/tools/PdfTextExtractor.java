package com.aimong.backend.tools;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.InflaterInputStream;

public final class PdfTextExtractor {

    private PdfTextExtractor() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            throw new IllegalArgumentException("usage: PdfTextExtractor <pdf-path>");
        }

        byte[] pdf = Files.readAllBytes(Path.of(args[0]));
        List<String> texts = extractVisibleText(pdf);
        for (String text : texts) {
            String normalized = text.replaceAll("\\s+", " ").trim();
            if (!normalized.isBlank()) {
                System.out.println(normalized);
            }
        }
    }

    static List<String> extractVisibleText(byte[] pdf) throws IOException {
        List<String> results = new ArrayList<>();
        byte[] streamToken = "stream".getBytes(StandardCharsets.ISO_8859_1);
        byte[] endStreamToken = "endstream".getBytes(StandardCharsets.ISO_8859_1);

        int index = 0;
        while ((index = indexOf(pdf, streamToken, index)) >= 0) {
            int streamStart = index + streamToken.length;
            if (streamStart < pdf.length && pdf[streamStart] == '\r') {
                streamStart++;
            }
            if (streamStart < pdf.length && pdf[streamStart] == '\n') {
                streamStart++;
            }

            int streamEnd = indexOf(pdf, endStreamToken, streamStart);
            if (streamEnd < 0) {
                break;
            }

            byte[] rawStream = slice(pdf, streamStart, streamEnd);
            byte[] decoded = tryInflate(rawStream);
            if (decoded != null) {
                String text = new String(decoded, StandardCharsets.ISO_8859_1);
                results.addAll(extractStrings(text));
            }

            index = streamEnd + endStreamToken.length;
        }
        return results;
    }

    private static List<String> extractStrings(String content) {
        List<String> results = new ArrayList<>();
        StringBuilder current = null;
        boolean escaping = false;

        for (int i = 0; i < content.length(); i++) {
            char ch = content.charAt(i);
            if (current == null) {
                if (ch == '(') {
                    current = new StringBuilder();
                }
                continue;
            }

            if (escaping) {
                current.append(switch (ch) {
                    case 'n' -> '\n';
                    case 'r' -> '\r';
                    case 't' -> '\t';
                    case 'b' -> '\b';
                    case 'f' -> '\f';
                    default -> ch;
                });
                escaping = false;
                continue;
            }

            if (ch == '\\') {
                escaping = true;
                continue;
            }

            if (ch == ')') {
                results.add(current.toString());
                current = null;
                continue;
            }

            current.append(ch);
        }

        return results;
    }

    private static byte[] tryInflate(byte[] compressed) throws IOException {
        try (InflaterInputStream input = new InflaterInputStream(new java.io.ByteArrayInputStream(compressed));
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            input.transferTo(output);
            return output.toByteArray();
        } catch (IOException ignored) {
            return null;
        }
    }

    private static int indexOf(byte[] source, byte[] target, int fromIndex) {
        outer:
        for (int i = fromIndex; i <= source.length - target.length; i++) {
            for (int j = 0; j < target.length; j++) {
                if (source[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    private static byte[] slice(byte[] source, int start, int end) {
        byte[] result = new byte[end - start];
        System.arraycopy(source, start, result, 0, result.length);
        return result;
    }
}
