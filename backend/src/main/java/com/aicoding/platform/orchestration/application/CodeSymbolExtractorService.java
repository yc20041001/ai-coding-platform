package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.orchestration.domain.CodeSymbolType;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

@Service
public class CodeSymbolExtractorService {

    // Java patterns
    private static final Pattern JAVA_CLASS = Pattern.compile(
            "(?:public\\s+|private\\s+|protected\\s+|static\\s+|abstract\\s+|final\\s+)*(?:class|interface|enum|@interface)\\s+(\\w+)");
    private static final Pattern JAVA_METHOD = Pattern.compile(
            "(?:public\\s+|private\\s+|protected\\s+|static\\s+|final\\s+|abstract\\s+|synchronized\\s+)*(?:\\w+(?:<[^>]*>)?\\s+)?(\\w+)\\s*\\([^)]*\\)\\s*(?:throws\\s+\\w+(?:\\s*,\\s*\\w+)*)?\\s*\\{");

    // TS/JS/Vue patterns
    private static final Pattern TS_FUNCTION = Pattern.compile(
            "(?:export\\s+)?(?:async\\s+)?function\\s+(\\w+)");
    private static final Pattern TS_CONST_FN = Pattern.compile(
            "(?:export\\s+)?(?:const|let|var)\\s+(\\w+)\\s*(?::\\s*\\w+(?:<[^>]*>)?)?\\s*=\\s*(?:\\([^)]*\\)|\\w+)\\s*(?:=>|:\\s*\\w+\\s*=>)");
    private static final Pattern TS_CLASS = Pattern.compile(
            "(?:export\\s+)?(?:abstract\\s+)?class\\s+(\\w+)");
    private static final Pattern TS_INTERFACE = Pattern.compile(
            "(?:export\\s+)?interface\\s+(\\w+)");
    private static final Pattern TS_COMPONENT = Pattern.compile(
            "(?:export\\s+)?(?:const|let|var)\\s+(\\w+)\\s*[:=]\\s*(?:defineComponent|defineAsyncComponent)");
    private static final Pattern VUE_COMPONENT = Pattern.compile(
            "<script\\s+[^>]*setup[^>]*>");

    // SQL patterns
    private static final Pattern SQL_TABLE = Pattern.compile(
            "CREATE\\s+TABLE(?:\\s+IF\\s+NOT\\s+EXISTS)?\\s+(?:\\w+\\.)?(\\w+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SQL_INDEX = Pattern.compile(
            "CREATE(?:\\s+UNIQUE)?\\s+INDEX\\s+(\\w+)", Pattern.CASE_INSENSITIVE);

    // Markdown patterns
    private static final Pattern MD_HEADING = Pattern.compile("^#{1,6}\\s+(.+)$", Pattern.MULTILINE);

    public List<SymbolExtraction> extractSymbols(String language, String fileName, String content, int lines) {
        List<SymbolExtraction> symbols = new ArrayList<>();
        if (content == null || content.isBlank()) return symbols;

        String[] lineArray = content.split("\n", -1);

        switch (language != null ? language.toLowerCase() : "") {
            case "java":
                extractJavaSymbols(content, lineArray, symbols);
                break;
            case "ts":
            case "js":
            case "tsx":
            case "jsx":
                extractTsJsSymbols(content, lineArray, symbols);
                break;
            case "vue":
                extractVueSymbols(content, lineArray, symbols);
                break;
            case "sql":
                extractSqlSymbols(content, symbols);
                break;
            case "md":
                extractMdSymbols(content, symbols);
                break;
            default:
                // No extraction for unknown languages
                break;
        }

        return symbols;
    }

    private void extractJavaSymbols(String content, String[] lineArray, List<SymbolExtraction> symbols) {
        // Class/interface/enum
        Matcher classMatcher = JAVA_CLASS.matcher(content);
        while (classMatcher.find()) {
            String name = classMatcher.group(1);
            int pos = classMatcher.start();
            int line = findLine(lineArray, pos);
            String type = determineJavaType(content, classMatcher.group());
            symbols.add(new SymbolExtraction(name, type, content, line, line + 10, line));
        }

        // Method
        Matcher methodMatcher = JAVA_METHOD.matcher(content);
        while (methodMatcher.find()) {
            String name = methodMatcher.group(1);
            if (isKeyword(name)) continue;
            int pos = methodMatcher.start();
            int line = findLine(lineArray, pos);
            String snippet = extractSnippet(lineArray, line, 3);
            symbols.add(new SymbolExtraction(name, CodeSymbolType.METHOD.name(), snippet, line, line + 3, line));
        }
    }

    private void extractTsJsSymbols(String content, String[] lineArray, List<SymbolExtraction> symbols) {
        // Function declarations
        Matcher fnMatcher = TS_FUNCTION.matcher(content);
        while (fnMatcher.find()) {
            String name = fnMatcher.group(1);
            int pos = fnMatcher.start();
            int line = findLine(lineArray, pos);
            String snippet = extractSnippet(lineArray, line, 3);
            symbols.add(new SymbolExtraction(name, CodeSymbolType.FUNCTION.name(), snippet, line, line + 3, line));
        }

        // Const arrow functions
        Matcher constFnMatcher = TS_CONST_FN.matcher(content);
        while (constFnMatcher.find()) {
            String name = constFnMatcher.group(1);
            int pos = constFnMatcher.start();
            int line = findLine(lineArray, pos);
            String snippet = extractSnippet(lineArray, line, 2);
            symbols.add(new SymbolExtraction(name, CodeSymbolType.FUNCTION.name(), snippet, line, line + 2, line));
        }

        // Classes
        Matcher classMatcher = TS_CLASS.matcher(content);
        while (classMatcher.find()) {
            String name = classMatcher.group(1);
            int pos = classMatcher.start();
            int line = findLine(lineArray, pos);
            String snippet = extractSnippet(lineArray, line, 3);
            symbols.add(new SymbolExtraction(name, CodeSymbolType.CLASS.name(), snippet, line, line + 3, line));
        }

        // Interfaces
        Matcher ifMatcher = TS_INTERFACE.matcher(content);
        while (ifMatcher.find()) {
            String name = ifMatcher.group(1);
            int pos = ifMatcher.start();
            int line = findLine(lineArray, pos);
            String snippet = extractSnippet(lineArray, line, 3);
            symbols.add(new SymbolExtraction(name, CodeSymbolType.INTERFACE.name(), snippet, line, line + 3, line));
        }

        // Components
        Matcher compMatcher = TS_COMPONENT.matcher(content);
        while (compMatcher.find()) {
            String name = compMatcher.group(1);
            int pos = compMatcher.start();
            int line = findLine(lineArray, pos);
            String snippet = extractSnippet(lineArray, line, 2);
            symbols.add(new SymbolExtraction(name, CodeSymbolType.COMPONENT.name(), snippet, line, line + 2, line));
        }
    }

    private void extractVueSymbols(String content, String[] lineArray, List<SymbolExtraction> symbols) {
        // Vue setup script indicates a component
        Matcher vueMatcher = VUE_COMPONENT.matcher(content);
        if (vueMatcher.find()) {
            String name = extractVueComponentName(lineArray);
            symbols.add(new SymbolExtraction(name != null ? name : "VueComponent",
                    CodeSymbolType.COMPONENT.name(), "Vue SFC with setup script", 0, 0, 0));
        }

        // Also extract TS/JS symbols from script content
        extractTsJsSymbols(content, lineArray, symbols);
    }

    private void extractSqlSymbols(String content, List<SymbolExtraction> symbols) {
        Matcher tableMatcher = SQL_TABLE.matcher(content);
        while (tableMatcher.find()) {
            String name = tableMatcher.group(1);
            symbols.add(new SymbolExtraction(name, CodeSymbolType.CLASS.name(),
                    "TABLE: " + name, 0, 0, 0));
        }

        Matcher indexMatcher = SQL_INDEX.matcher(content);
        while (indexMatcher.find()) {
            String name = indexMatcher.group(1);
            symbols.add(new SymbolExtraction(name, CodeSymbolType.CONSTANT.name(),
                    "INDEX: " + name, 0, 0, 0));
        }
    }

    private void extractMdSymbols(String content, List<SymbolExtraction> symbols) {
        Matcher headingMatcher = MD_HEADING.matcher(content);
        while (headingMatcher.find()) {
            String name = headingMatcher.group(1).trim();
            if (name.length() > 100) name = name.substring(0, 100);
            symbols.add(new SymbolExtraction(name, CodeSymbolType.CONSTANT.name(),
                    "HEADING: " + name, 0, 0, 0));
        }
    }

    private String determineJavaType(String content, String declaration) {
        if (declaration.contains("interface")) return CodeSymbolType.INTERFACE.name();
        if (declaration.contains("enum")) return CodeSymbolType.ENUM.name();
        if (declaration.contains("@interface")) return CodeSymbolType.INTERFACE.name();
        return CodeSymbolType.CLASS.name();
    }

    private boolean isKeyword(String name) {
        return "if".equals(name) || "for".equals(name) || "while".equals(name)
                || "switch".equals(name) || "catch".equals(name) || "synchronized".equals(name)
                || "ifPresent".equals(name);
    }

    private int findLine(String[] lineArray, int pos) {
        int charCount = 0;
        for (int i = 0; i < lineArray.length; i++) {
            charCount += lineArray[i].length() + 1;
            if (charCount > pos) return i + 1;
        }
        return 1;
    }

    private String extractSnippet(String[] lineArray, int startLine, int contextLines) {
        StringBuilder sb = new StringBuilder();
        int start = Math.max(0, startLine - 1 - contextLines);
        int end = Math.min(lineArray.length, startLine + contextLines);
        for (int i = start; i < end; i++) {
            sb.append(lineArray[i]).append("\n");
        }
        return sb.toString().trim();
    }

    private String extractVueComponentName(String[] lineArray) {
        for (String line : lineArray) {
            if (line.contains("name:")) {
                int idx = line.indexOf("name:");
                String rest = line.substring(idx + 5).trim();
                if (rest.startsWith("'") || rest.startsWith("\"")) {
                    char quote = rest.charAt(0);
                    int end = rest.indexOf(quote, 1);
                    if (end > 0) return rest.substring(1, end);
                }
            }
        }
        return null;
    }

    public static class SymbolExtraction {
        private final String symbolName;
        private final String symbolType;
        private final String snippet;
        private final int startLine;
        private final int endLine;
        private final int line;

        public SymbolExtraction(String symbolName, String symbolType, String snippet,
                                 int startLine, int endLine, int line) {
            this.symbolName = symbolName;
            this.symbolType = symbolType;
            this.snippet = snippet;
            this.startLine = startLine;
            this.endLine = endLine;
            this.line = line;
        }

        public String getSymbolName() { return symbolName; }
        public String getSymbolType() { return symbolType; }
        public String getSnippet() { return snippet; }
        public int getStartLine() { return startLine; }
        public int getEndLine() { return endLine; }
        public int getLine() { return line; }
    }
}
