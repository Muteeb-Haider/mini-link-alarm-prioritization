package com.example;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.nio.file.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class AlarmPrioritizerCLITest {

    private String findBinary() {
        String[] candidates = {
            "build/alarm_cli",
            "./build/alarm_cli",
            "../build/alarm_cli"
        };
        for (String c : candidates) {
            if (Files.isExecutable(Paths.get(c))) return c;
        }
        fail("alarm_cli binary not found. Build the C++ project first.");
        return null;
    }

    @Test
    public void testPrioritizationOrderAndFields() throws Exception {
        String bin = findBinary();

        // Get the project root directory - handle both local and Docker environments
        String projectRoot;
        String userDir = System.getProperty("user.dir");
        if (userDir.endsWith("java-tests")) {
            // Local development
            projectRoot = Paths.get(userDir).getParent().toString();
        } else {
            // Docker environment - working directory is /app
            projectRoot = userDir;
        }
        String configPath = projectRoot + "/config/scoring.json";
        String inputPath = projectRoot + "/data/sample_alarms.json";

        ProcessBuilder pb = new ProcessBuilder(
            bin,
            "--input", inputPath,
            "--config", configPath,
            "--format", "json",
            "--top", "3"
        );
        pb.redirectErrorStream(true);
        Process p = pb.start();
        String out = new String(p.getInputStream().readAllBytes());
        int code = p.waitFor();
        assertEquals(0, code, "CLI should exit 0, was: " + code + "\nOUT:\n" + out);

        ObjectMapper mapper = new ObjectMapper();
        ArrayNode arr = (ArrayNode) mapper.readTree(out);

        assertTrue(arr.size() == 3, "Should return top 3 alarms");

        // Validate required fields exist
        for (int i = 0; i < arr.size(); i++) {
            ObjectNode o = (ObjectNode) arr.get(i);
            assertTrue(o.has("id"));
            assertTrue(o.has("nodeId"));
            assertTrue(o.has("severity"));
            assertTrue(o.has("score"));
            assertTrue(o.has("rank"));
            assertTrue(o.has("reason"));
        }

        // Top alarm should reflect high severity
        String reason0 = arr.get(0).get("reason").asText();
        assertTrue(reason0.contains("Critical") || reason0.contains("critical") || reason0.contains("Critical severity"),
                   "Top reason should reflect high severity");
    }

    @Test
    public void testScoringAlgorithmAccuracy() throws Exception {
        String bin = findBinary();
        String projectRoot = getProjectRoot();
        String configPath = projectRoot + "/config/scoring.json";
        
        // Create test alarms with known values for scoring validation
        String testAlarms = createTestAlarmsForScoring();
        String inputPath = writeTestFile(testAlarms, "scoring_test.json");
        
        ProcessBuilder pb = new ProcessBuilder(
            bin,
            "--input", inputPath,
            "--config", configPath,
            "--format", "json",
            "--top", "5"
        );
        pb.redirectErrorStream(true);
        Process p = pb.start();
        String out = new String(p.getInputStream().readAllBytes());
        int code = p.waitFor();
        assertEquals(0, code, "CLI should exit 0 for scoring test");
        
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode arr = (ArrayNode) mapper.readTree(out);
        
        // Validate scoring logic: Critical + high frequency + high impact should score highest
        ObjectNode topAlarm = (ObjectNode) arr.get(0);
        double topScore = topAlarm.get("score").asDouble();
        String topSeverity = topAlarm.get("severity").asText();
        
        assertTrue(topScore > 100, "Top score should be significantly high for Critical alarms");
        assertEquals("Critical", topSeverity, "Top alarm should be Critical severity");
        
        // Clean up test file
        Files.deleteIfExists(Paths.get(inputPath));
    }

    @Test
    public void testDifferentOutputFormats() throws Exception {
        String bin = findBinary();
        String projectRoot = getProjectRoot();
        String configPath = projectRoot + "/config/scoring.json";
        String inputPath = projectRoot + "/data/sample_alarms.json";
        
        // Test JSON format
        ProcessBuilder pbJson = new ProcessBuilder(
            bin,
            "--input", inputPath,
            "--config", configPath,
            "--format", "json",
            "--top", "2"
        );
        pbJson.redirectErrorStream(true);
        Process pJson = pbJson.start();
        String outJson = new String(pJson.getInputStream().readAllBytes());
        int codeJson = pJson.waitFor();
        assertEquals(0, codeJson, "JSON format should work");
        
        // Test table format
        ProcessBuilder pbTable = new ProcessBuilder(
            bin,
            "--input", inputPath,
            "--config", configPath,
            "--format", "table",
            "--top", "2"
        );
        pbTable.redirectErrorStream(true);
        Process pTable = pbTable.start();
        String outTable = new String(pTable.getInputStream().readAllBytes());
        int codeTable = pTable.waitFor();
        assertEquals(0, codeTable, "Table format should work");
        
        // Validate JSON output is parseable
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode jsonArr = (ArrayNode) mapper.readTree(outJson);
        assertEquals(2, jsonArr.size(), "Should return exactly 2 alarms in JSON format");
        
        // Validate table output contains expected text
        assertTrue(outTable.contains("RANK"), "Table output should contain 'RANK' header");
        assertTrue(outTable.contains("SCORE"), "Table output should contain 'SCORE' header");
        assertTrue(outTable.contains("SEV"), "Table output should contain 'SEV' header");
    }

    @Test
    public void testErrorHandlingAndEdgeCases() throws Exception {
        String bin = findBinary();
        String projectRoot = getProjectRoot();
        String configPath = projectRoot + "/config/scoring.json";
        
        // Test with non-existent input file
        ProcessBuilder pbInvalid = new ProcessBuilder(
            bin,
            "--input", "non_existent_file.json",
            "--config", configPath,
            "--format", "json",
            "--top", "5"
        );
        pbInvalid.redirectErrorStream(true);
        Process pInvalid = pbInvalid.start();
        String outInvalid = new String(pInvalid.getInputStream().readAllBytes());
        int codeInvalid = pInvalid.waitFor();
        assertNotEquals(0, codeInvalid, "Should fail with non-existent input file");
        
        // Test with invalid JSON
        String invalidJson = "{ invalid json content }";
        String invalidInputPath = writeTestFile(invalidJson, "invalid_test.json");
        
        ProcessBuilder pbInvalidJson = new ProcessBuilder(
            bin,
            "--input", invalidInputPath,
            "--config", configPath,
            "--format", "json",
            "--top", "5"
        );
        pbInvalidJson.redirectErrorStream(true);
        Process pInvalidJson = pbInvalidJson.start();
        String outInvalidJson = new String(pInvalidJson.getInputStream().readAllBytes());
        int codeInvalidJson = pInvalidJson.waitFor();
        assertNotEquals(0, codeInvalidJson, "Should fail with invalid JSON");
        
        // Clean up test file
        Files.deleteIfExists(Paths.get(invalidInputPath));
    }

    @Test
    public void testPerformanceAndLargeDataset() throws Exception {
        String bin = findBinary();
        String projectRoot = getProjectRoot();
        String configPath = projectRoot + "/config/scoring.json";
        
        // Create a larger dataset for performance testing
        String largeDataset = createLargeDataset(100);
        String inputPath = writeTestFile(largeDataset, "performance_test.json");
        
        long startTime = System.currentTimeMillis();
        
        ProcessBuilder pb = new ProcessBuilder(
            bin,
            "--input", inputPath,
            "--config", configPath,
            "--format", "json",
            "--top", "50"
        );
        pb.redirectErrorStream(true);
        Process p = pb.start();
        String out = new String(p.getInputStream().readAllBytes());
        int code = p.waitFor();
        long endTime = System.currentTimeMillis();
        
        assertEquals(0, code, "Should handle large dataset successfully");
        
        // Performance assertion: should process 100 alarms in reasonable time
        long processingTime = endTime - startTime;
        assertTrue(processingTime < 5000, "Should process 100 alarms in under 5 seconds, took: " + processingTime + "ms");
        
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode arr = (ArrayNode) mapper.readTree(out);
        assertEquals(50, arr.size(), "Should return top 50 alarms");
        
        // Clean up test file
        Files.deleteIfExists(Paths.get(inputPath));
    }

    // Helper methods
    private String getProjectRoot() {
        String userDir = System.getProperty("user.dir");
        if (userDir.endsWith("java-tests")) {
            return Paths.get(userDir).getParent().toString();
        } else {
            return userDir;
        }
    }

    private String createTestAlarmsForScoring() {
        return "[" +
            "{\"id\":\"TEST-001\",\"nodeId\":\"MINI-LINK-ALPHA-01\",\"severity\":\"Critical\",\"firstSeen\":\"2025-08-24T16:00:00Z\",\"lastSeen\":\"2025-08-24T16:05:00Z\",\"occurrencesPerHour\":20.0,\"affectedLinks\":10,\"trafficImpactPct\":95,\"serviceAffecting\":true,\"description\":\"Critical link failure\"}," +
            "{\"id\":\"TEST-002\",\"nodeId\":\"MINI-LINK-BETA-02\",\"severity\":\"Major\",\"firstSeen\":\"2025-08-24T16:00:00Z\",\"lastSeen\":\"2025-08-24T16:05:00Z\",\"occurrencesPerHour\":5.0,\"affectedLinks\":3,\"trafficImpactPct\":30,\"serviceAffecting\":false,\"description\":\"Major performance issue\"}," +
            "{\"id\":\"TEST-003\",\"nodeId\":\"MINI-LINK-GAMMA-03\",\"severity\":\"Minor\",\"firstSeen\":\"2025-08-24T16:00:00Z\",\"lastSeen\":\"2025-08-24T16:05:00Z\",\"occurrencesPerHour\":1.0,\"affectedLinks\":1,\"trafficImpactPct\":5,\"serviceAffecting\":false,\"description\":\"Minor warning\"}" +
            "]";
    }

    private String createLargeDataset(int count) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < count; i++) {
            if (i > 0) sb.append(",");
            sb.append("{\"id\":\"PERF-").append(String.format("%03d", i)).append("\",");
            sb.append("\"nodeId\":\"MINI-LINK-PERF-").append(String.format("%02d", (i % 8) + 1)).append("\",");
            sb.append("\"severity\":\"").append(getSeverityForIndex(i)).append("\",");
            sb.append("\"firstSeen\":\"2025-08-24T16:00:00Z\",");
            sb.append("\"lastSeen\":\"2025-08-24T16:05:00Z\",");
            sb.append("\"occurrencesPerHour\":").append((i % 20) + 1).append(".0,");
            sb.append("\"affectedLinks\":").append(i % 10).append(",");
            sb.append("\"trafficImpactPct\":").append((i % 100) + 1).append(",");
            sb.append("\"serviceAffecting\":").append(i % 3 == 0).append(",");
            sb.append("\"description\":\"Performance test alarm ").append(i).append("\"}");
        }
        sb.append("]");
        return sb.toString();
    }

    private String getSeverityForIndex(int index) {
        String[] severities = {"Critical", "Major", "Minor", "Warning", "Info"};
        return severities[index % severities.length];
    }

    private String writeTestFile(String content, String filename) throws IOException {
        Path tempDir = Files.createTempDirectory("alarm_test_");
        Path filePath = tempDir.resolve(filename);
        Files.write(filePath, content.getBytes());
        return filePath.toString();
    }
}
