package examples.spring;

import com.mem0.Mem0;
import com.mem0.config.Mem0Configuration;
import com.mem0.core.Memory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * Spring Boot集成示例 - Spring Boot Integration Example
 * 
 * 展示如何在Spring Boot应用中集成mem0-java
 * Demonstrates how to integrate mem0-java in Spring Boot applications
 */
@SpringBootApplication
public class SpringBootIntegrationExample {
    
    public static void main(String[] args) {
        SpringApplication.run(SpringBootIntegrationExample.class, args);
    }
}

/**
 * Mem0配置类 - Mem0 Configuration Class
 */
@Configuration
class Mem0Config {
    
    @Bean
    public Mem0Configuration mem0Configuration() {
        Mem0Configuration config = new Mem0Configuration();
        
        // 从环境变量或配置文件读取配置
        config.setLlmApiKey(System.getenv("OPENAI_API_KEY"));
        config.setLlmModel("gpt-4");
        config.setEmbeddingModel("text-embedding-ada-002");
        
        // 应用配置
        config.setApplicationName("SpringBootMemoryApp");
        config.setEnvironment("production");
        
        return config;
    }
    
    @Bean
    public Mem0 mem0Client(Mem0Configuration config) {
        return new Mem0(config);
    }
}

/**
 * 内存管理REST控制器 - Memory Management REST Controller
 */
@RestController
@RequestMapping("/api/memory")
@CrossOrigin(origins = "*")
class MemoryController {
    
    @Resource
    private Mem0 mem0Client;
    
    /**
     * 添加新记忆 - Add new memory
     */
    @PostMapping("/add")
    public Map<String, Object> addMemory(@RequestBody AddMemoryRequest request) {
        try {
            String memoryId = mem0Client.add(request.getText(), request.getUserId());
            return Map.of(
                "success", true,
                "memoryId", memoryId,
                "message", "Memory added successfully"
            );
        } catch (Exception e) {
            return Map.of(
                "success", false,
                "error", e.getMessage()
            );
        }
    }
    
    /**
     * 批量添加记忆 - Add memories in batch
     */
    @PostMapping("/batch")
    public Map<String, Object> addBatchMemories(@RequestBody BatchMemoryRequest request) {
        try {
            List<String> memoryIds = mem0Client.addBatch(request.getTexts(), request.getUserId());
            return Map.of(
                "success", true,
                "memoryIds", memoryIds,
                "count", memoryIds.size(),
                "message", "Batch memories added successfully"
            );
        } catch (Exception e) {
            return Map.of(
                "success", false,
                "error", e.getMessage()
            );
        }
    }
    
    /**
     * 搜索记忆 - Search memories
     */
    @PostMapping("/search")
    public Map<String, Object> searchMemories(@RequestBody SearchRequest request) {
        try {
            List<Memory> memories = mem0Client.search(
                request.getQuery(), 
                request.getUserId(),
                request.getLimit()
            );
            
            return Map.of(
                "success", true,
                "memories", memories,
                "count", memories.size()
            );
        } catch (Exception e) {
            return Map.of(
                "success", false,
                "error", e.getMessage()
            );
        }
    }
    
    /**
     * 获取用户所有记忆 - Get all user memories
     */
    @GetMapping("/user/{userId}")
    public Map<String, Object> getUserMemories(@PathVariable String userId) {
        try {
            List<Memory> memories = mem0Client.getAll(userId);
            return Map.of(
                "success", true,
                "memories", memories,
                "count", memories.size()
            );
        } catch (Exception e) {
            return Map.of(
                "success", false,
                "error", e.getMessage()
            );
        }
    }
    
    /**
     * 更新记忆 - Update memory
     */
    @PutMapping("/{memoryId}")
    public Map<String, Object> updateMemory(
            @PathVariable String memoryId,
            @RequestBody UpdateMemoryRequest request) {
        try {
            mem0Client.update(memoryId, request.getNewText(), request.getUserId());
            return Map.of(
                "success", true,
                "message", "Memory updated successfully"
            );
        } catch (Exception e) {
            return Map.of(
                "success", false,
                "error", e.getMessage()
            );
        }
    }
    
    /**
     * 删除记忆 - Delete memory
     */
    @DeleteMapping("/{memoryId}")
    public Map<String, Object> deleteMemory(@PathVariable String memoryId) {
        try {
            mem0Client.delete(memoryId);
            return Map.of(
                "success", true,
                "message", "Memory deleted successfully"
            );
        } catch (Exception e) {
            return Map.of(
                "success", false,
                "error", e.getMessage()
            );
        }
    }
    
    /**
     * 获取记忆历史 - Get memory history
     */
    @GetMapping("/{memoryId}/history")
    public Map<String, Object> getMemoryHistory(@PathVariable String memoryId) {
        try {
            List<Memory> history = mem0Client.getHistory(memoryId);
            return Map.of(
                "success", true,
                "history", history,
                "count", history.size()
            );
        } catch (Exception e) {
            return Map.of(
                "success", false,
                "error", e.getMessage()
            );
        }
    }
}

/**
 * 请求数据模型 - Request Data Models
 */
class AddMemoryRequest {
    private String text;
    private String userId;
    
    // Getters and Setters
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
}

class BatchMemoryRequest {
    private List<String> texts;
    private String userId;
    
    // Getters and Setters
    public List<String> getTexts() { return texts; }
    public void setTexts(List<String> texts) { this.texts = texts; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
}

class SearchRequest {
    private String query;
    private String userId;
    private int limit = 10;
    
    // Getters and Setters
    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public int getLimit() { return limit; }
    public void setLimit(int limit) { this.limit = limit; }
}

class UpdateMemoryRequest {
    private String newText;
    private String userId;
    
    // Getters and Setters
    public String getNewText() { return newText; }
    public void setNewText(String newText) { this.newText = newText; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
}