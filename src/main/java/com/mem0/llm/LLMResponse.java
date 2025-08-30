package com.mem0.llm;

import java.time.Instant;
import java.util.Map;

/**
 * LLM响应结果封装模型 / LLM Response Encapsulation Model
 * 
 * 用于封装大语言模型(LLM)的响应结果，包含响应内容、模型信息、性能指标和错误处理。
 * 支持成功和失败两种响应状态，提供详细的响应元数据和性能监控数据。
 * Encapsulates Large Language Model (LLM) response results, including content, model information, 
 * performance metrics, and error handling. Supports both success and failure response states with 
 * detailed metadata and performance monitoring data.
 * 
 * <h3>核心功能 / Key Features:</h3>
 * <ul>
 *   <li>LLM响应内容封装和管理 / LLM response content encapsulation and management</li>
 *   <li>性能指标记录和监控 / Performance metrics recording and monitoring</li>
 *   <li>成功和失败状态区分处理 / Success and failure state differentiation</li>
 *   <li>响应元数据扩展支持 / Response metadata extension support</li>
 *   <li>自动时间戳记录 / Automatic timestamp recording</li>
 * </ul>
 * 
 * <h3>响应状态类型 / Response State Types:</h3>
 * <pre>
 * Success Response:
 *   content: String     // 生成内容
 *   model: String       // 使用模型
 *   tokensUsed: int     // 消耗token数
 *   responseTimeMs: long // 响应时间(毫秒)
 *   metadata: Map       // 元数据信息
 * 
 * Error Response:
 *   error: String       // 错误信息
 *   model: String       // 使用模型
 *   responseTimeMs: long // 响应时间(毫秒)
 * </pre>
 * 
 * <h3>使用示例 / Usage Examples:</h3>
 * <pre>{@code
 * // 创建成功响应
 * Map<String, Object> metadata = new HashMap<>();
 * metadata.put("temperature", 0.7);
 * metadata.put("max_tokens", 1024);
 * 
 * LLMResponse successResponse = new LLMResponse(
 *     "生成的文本内容",
 *     "gpt-3.5-turbo", 
 *     856, 
 *     1250,
 *     metadata
 * );
 * 
 * // 检查响应状态
 * if (successResponse.isSuccess()) {
 *     String content = successResponse.getContent();
 *     int tokens = successResponse.getTokensUsed();
 *     System.out.println("生成内容: " + content);
 *     System.out.println("使用token: " + tokens);
 * }
 * 
 * // 创建错误响应
 * LLMResponse errorResponse = new LLMResponse(
 *     "API调用限制超出",
 *     "gpt-3.5-turbo",
 *     500
 * );
 * 
 * if (!errorResponse.isSuccess()) {
 *     String error = errorResponse.getError();
 *     System.err.println("错误信息: " + error);
 * }
 * 
 * // 性能监控
 * long responseTime = successResponse.getResponseTimeMs();
 * System.out.println("响应时间: " + responseTime + "ms");
 * }</pre>
 * 
 * <h3>性能考虑 / Performance Considerations:</h3>
 * <ul>
 *   <li><b>不可变性</b>: 所有字段为final，确保线程安全 / Immutability: All fields are final for thread safety</li>
 *   <li><b>内存效率</b>: 错误响应不存储content和metadata / Memory efficiency: Error responses don't store content/metadata</li>
 *   <li><b>时间记录</b>: 自动记录创建时间戳 / Time recording: Automatic creation timestamp</li>
 *   <li><b>状态区分</b>: 通过success字段快速判断响应状态 / State differentiation: Quick status check via success field</li>
 * </ul>
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 * @see com.mem0.llm.LLMProvider
 */
public class LLMResponse {
    
    private final String content;
    private final String model;
    private final int tokensUsed;
    private final long responseTimeMs;
    private final Instant timestamp;
    private final Map<String, Object> metadata;
    private final boolean success;
    private final String error;
    
    public LLMResponse(String content, String model, int tokensUsed, 
                      long responseTimeMs, Map<String, Object> metadata) {
        this.content = content;
        this.model = model;
        this.tokensUsed = tokensUsed;
        this.responseTimeMs = responseTimeMs;
        this.timestamp = Instant.now();
        this.metadata = metadata;
        this.success = true;
        this.error = null;
    }
    
    public LLMResponse(String error, String model, long responseTimeMs) {
        this.content = null;
        this.model = model;
        this.tokensUsed = 0;
        this.responseTimeMs = responseTimeMs;
        this.timestamp = Instant.now();
        this.metadata = null;
        this.success = false;
        this.error = error;
    }
    
    // Getters
    public String getContent() { return content; }
    public String getModel() { return model; }
    public int getTokensUsed() { return tokensUsed; }
    public long getResponseTimeMs() { return responseTimeMs; }
    public Instant getTimestamp() { return timestamp; }
    public Map<String, Object> getMetadata() { return metadata; }
    public boolean isSuccess() { return success; }
    public String getError() { return error; }
    
    @Override
    public String toString() {
        if (success) {
            return String.format("LLMResponse{model='%s', tokens=%d, time=%dms, success=true}", 
                               model, tokensUsed, responseTimeMs);
        } else {
            return String.format("LLMResponse{model='%s', time=%dms, error='%s', success=false}", 
                               model, responseTimeMs, error);
        }
    }
}