package prj.anhzxje.aiagent.agent;

import org.springaicommunity.agent.tools.FileSystemTools;
import org.springaicommunity.agent.tools.GlobTool;
import org.springaicommunity.agent.tools.GrepTool;
import org.springaicommunity.agent.tools.ListDirectoryTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.google.genai.common.GoogleGenAiSafetySetting;
import org.springframework.ai.google.genai.common.GoogleGenAiSafetySetting.HarmCategory;
import org.springframework.ai.google.genai.common.GoogleGenAiSafetySetting.HarmBlockThreshold;
import org.springframework.ai.google.genai.common.GoogleGenAiSafetySetting.HarmBlockMethod;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * AI Agent thực hiện code review bằng cách tự khám phá codebase.
 * Trách nhiệm: CHỈ lo reasoning + tool calling + prompt.
 */
@Component
public class CodeReviewAgent {

    private final ChatClient chatClient;

    /**
     * Safety settings cho Gemini — hạ threshold để model không từ chối
     * các yêu cầu code review hợp lệ.
     */
    private final List<GoogleGenAiSafetySetting> safetySettings = List.of(
            new GoogleGenAiSafetySetting(
                    HarmCategory.HARM_CATEGORY_DANGEROUS_CONTENT,
                    HarmBlockThreshold.BLOCK_ONLY_HIGH,
                    HarmBlockMethod.PROBABILITY
            ),
            new GoogleGenAiSafetySetting(
                    HarmCategory.HARM_CATEGORY_HARASSMENT,
                    HarmBlockThreshold.BLOCK_ONLY_HIGH,
                    HarmBlockMethod.PROBABILITY
            ),
            new GoogleGenAiSafetySetting(
                    HarmCategory.HARM_CATEGORY_HATE_SPEECH,
                    HarmBlockThreshold.BLOCK_ONLY_HIGH,
                    HarmBlockMethod.PROBABILITY
            ),
            new GoogleGenAiSafetySetting(
                    HarmCategory.HARM_CATEGORY_SEXUALLY_EXPLICIT,
                    HarmBlockThreshold.BLOCK_ONLY_HIGH,
                    HarmBlockMethod.PROBABILITY
            )
    );

    public CodeReviewAgent(ChatClient.Builder chatClientBuilder) {

        // Cấu hình safety settings — hạ threshold để Gemini không từ chối code review
        var optionsBuilder = GoogleGenAiChatOptions.builder()
                .safetySettings(safetySettings);

        this.chatClient = chatClientBuilder
                .defaultSystem(loadSystemPrompt())
                .defaultOptions(optionsBuilder)
                .defaultTools(
                        ListDirectoryTool.builder().build(),
                        GlobTool.builder().build(),
                        FileSystemTools.builder().build(),
                        GrepTool.builder().build()
                )
                .build();
    }

    /**
     * Review codebase tại projectPath.
     *
     * @param projectPath đường dẫn tuyệt đối tới project cần review
     * @param focus       (nullable) lĩnh vực tập trung
     * @return kết quả review thô dạng JSON string từ LLM
     */
    public String reviewPath(String projectPath, String focus) {

        String focusInstruction;

        if (focus != null && !focus.isBlank()) {
            focusInstruction = switch (focus.toLowerCase()) {
                case "security" ->
                        """
                        Focus on reviewing code practices related to:
                        authentication flow, authorization checks, token handling,
                        sensitive data in responses, endpoint access control,
                        Spring configuration best practices.
                        """;

                case "performance" ->
                        """
                        Focus on reviewing code for performance:
                        database access patterns, N+1 queries,
                        unnecessary processing, memory usage.
                        """;

                case "bug" ->
                        """
                        Focus on finding logic errors:
                        null handling, exception handling,
                        incorrect behavior, edge cases.
                        """;

                case "code_quality" ->
                        """
                        Focus on code quality:
                        readability, maintainability, duplication,
                        naming, structure and design patterns.
                        """;

                default ->
                        "Focus on: " + focus + ".";
            };
        } else {
            focusInstruction = """
            Perform a comprehensive code review covering:
            bugs, best practices, performance and code quality.
            """;
        }

        String prompt = """
                Review the source code of the project located at:
                %s

                %s

                IMPORTANT INSTRUCTIONS:
                - You MUST use your file system tools to read actual source code files.
                - Start by listing the directory structure.
                - Then read the key source files.
                - Use grep to search for patterns across the codebase.
                - Base your findings ONLY on code you have actually read.
                - Do not guess or invent code that you have not read.
                - Only report issues that have concrete evidence in the source code.
                - ALL textual content in the JSON (summary, description, recommendation, suggestions) MUST be written in Vietnamese language.

                Return ONLY a valid JSON object.
                Do NOT use markdown code blocks.
                Do NOT add any text before or after the JSON.

                The JSON MUST follow exactly this structure:

                {
                  "summary": "Tổng quan ngắn gọn bằng tiếng Việt",
                  "issues": [
                    {
                      "severity": "HIGH | MEDIUM | LOW",
                      "category": "BUG | SECURITY | PERFORMANCE | CODE_QUALITY",
                      "file": "relative file path",
                      "description": "Mô tả chi tiết bằng tiếng Việt",
                      "recommendation": "Đề xuất sửa lỗi bằng tiếng Việt"
                    }
                  ],
                  "suggestions": [
                    "Gợi ý cải thiện 1 bằng tiếng Việt",
                    "Gợi ý cải thiện 2 bằng tiếng Việt"
                  ]
                }

                If no significant issues are found, return an empty issues array.
                """.formatted(projectPath, focusInstruction);

        String result = chatClient
                .prompt()
                .user(prompt)
                .call()
                .content();

        return cleanJsonResponse(result);
    }

    private String cleanJsonResponse(String response) {
        if (response == null) return null;
        String cleaned = response.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        return cleaned.trim();
    }

    /**
     * Load system prompt từ classpath.
     */
    private String loadSystemPrompt() {
        try {
            ClassPathResource resource =
                    new ClassPathResource("prompts/code-reviewer.md");

            return resource.getContentAsString(StandardCharsets.UTF_8);

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Cannot load system prompt: prompts/code-reviewer.md",
                    e
            );
        }
    }
}