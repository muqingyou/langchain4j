# 硅谷小智医疗AI助手 - 使用指南

## 🚀 快速开始

### 1. 环境准备

#### 必需的环境变量
```bash
# 阿里百炼API密钥
export DASH_SCOPE_API_KEY="your_dash_scope_api_key"

# Baidu搜索API密钥
export BAIDU_SEARCH_API_KEY="your_bing_search_api_key"

# Pinecone API密钥
export PINECONE_API_KEY="your_pinecone_api_key"
```

#### 数据库配置
- **MongoDB**: 用于存储聊天记录和记忆
- **MySQL**: 用于存储预约信息
- **Pinecone**: 用于向量知识库存储

### 2. 启动应用

```bash
# 克隆项目
git clone <repository-url>
cd java-ai-langchain4j

# 安装依赖
mvn clean install

# 启动应用
mvn spring-boot:run
```

### 3. 初始化知识库

应用启动后，访问以下接口初始化知识库：

```bash
curl -X POST http://localhost:8080/knowledge/init
```

## 🏥 功能使用

### 1. 智能分导诊

#### 基础对话
```bash
curl -X POST http://localhost:8080/xiaozhi/chat \
  -H "Content-Type: application/json" \
  -d '{
    "memoryId": 1,
    "message": "我最近头痛头晕，应该挂哪个科室？"
  }'
```

#### 测试分导诊功能
```bash
curl -X POST "http://localhost:8080/test/guidance?symptoms=头痛头晕&memoryId=1"
```

### 2. 预约挂号

#### 预约挂号
```bash
curl -X POST http://localhost:8080/xiaozhi/chat \
  -H "Content-Type: application/json" \
  -d '{
    "memoryId": 1,
    "message": "我想预约神经内科，姓名张三，身份证123456789012345678，日期2025-01-15，时间上午"
  }'
```

#### 取消预约
```bash
curl -X POST http://localhost:8080/xiaozhi/chat \
  -H "Content-Type: application/json" \
  -d '{
    "memoryId": 1,
    "message": "取消我的预约，姓名张三，身份证123456789012345678，科室神经内科，日期2025-01-15，时间上午"
  }'
```

### 3. 知识库查询

#### 查询医疗信息
```bash
curl -X POST "http://localhost:8080/test/knowledge?query=高血压&memoryId=1"
```

#### 获取知识库状态
```bash
curl -X GET http://localhost:8080/knowledge/stats
```

### 4. 记忆功能测试

#### 测试记忆压缩
```bash
curl -X POST http://localhost:8080/xiaozhi/chat-hierarchical \
  -H "Content-Type: application/json" \
  -d '{
    "memoryId": 1,
    "message": "我想了解心脏病的症状"
  }'
```

## 🔧 配置说明

### 应用配置 (application.properties)

```properties
# 服务器端口
server.port=8080

# 阿里百炼配置
langchain4j.community.dashscope.chat-model.api-key=${DASH_SCOPE_API_KEY}
langchain4j.community.dashscope.chat-model.model-name=qwen-max
langchain4j.community.dashscope.embedding-model.api-key=${DASH_SCOPE_API_KEY}
langchain4j.community.dashscope.embedding-model.model-name=text-embedding-v3

# MongoDB配置
spring.data.mongodb.uri=mongodb://localhost:27017/chat_memory_db

# MySQL配置
spring.datasource.url=jdbc:mysql://localhost:3306/guiguxiaozhi
spring.datasource.username=root
spring.datasource.password=root

# Bing搜索配置
bing.search.api.key=${BING_SEARCH_API_KEY}
bing.search.api.url=https://api.bing.microsoft.com/v7.0/search
bing.search.api.count=5

# 知识库配置
knowledge.base.path=knowledge/knowledge
knowledge.auto.init=true

# 记忆压缩配置
xiaozhi.compression.enabled=true
xiaozhi.compression.threshold=10
xiaozhi.compression.chunk-size=5
xiaozhi.compression.keep-recent-messages=3
```

## 📊 API文档

启动应用后，访问以下地址查看API文档：

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **Knife4j**: http://localhost:8080/doc.html

## 🧪 测试用例

### 1. 分导诊测试
```bash
# 测试头痛症状
curl -X POST "http://localhost:8080/test/guidance?symptoms=头痛&memoryId=1"

# 测试心脏症状
curl -X POST "http://localhost:8080/test/guidance?symptoms=胸痛心悸&memoryId=1"

# 测试消化症状
curl -X POST "http://localhost:8080/test/guidance?symptoms=胃痛腹痛&memoryId=1"
```

### 2. 预约测试
```bash
# 测试预约功能
curl -X POST "http://localhost:8080/test/appointment?message=我想预约心内科&memoryId=1"

# 测试取消预约
curl -X POST "http://localhost:8080/test/appointment?message=取消我的预约&memoryId=1"
```

### 3. 知识库测试
```bash
# 测试疾病查询
curl -X POST "http://localhost:8080/test/knowledge?query=高血压&memoryId=1"

# 测试科室查询
curl -X POST "http://localhost:8080/test/knowledge?query=神经内科&memoryId=1"
```

## 🔍 故障排除

### 1. 常见问题

#### API密钥问题
```
错误: API密钥未设置
解决: 确保设置了所有必需的环境变量
```

#### 数据库连接问题
```
错误: 无法连接到MongoDB/MySQL
解决: 确保数据库服务正在运行，检查连接配置
```

#### 知识库加载失败
```
错误: 知识库初始化失败
解决: 检查knowledge目录是否存在，确保有医疗文档
```

### 2. 日志查看

```bash
# 查看应用日志
tail -f logs/application.log

# 查看错误日志
grep "ERROR" logs/application.log
```

### 3. 性能监控

```bash
# 查看知识库状态
curl -X GET http://localhost:8080/knowledge/stats

# 查看记忆压缩状态
curl -X GET http://localhost:8080/compression/status
```

## 📝 开发说明

### 1. 添加新的工具

1. 创建工具类，使用`@Component`注解
2. 在`XiaozhiAgent`中添加工具配置
3. 使用`@Tool`注解定义工具方法

### 2. 扩展知识库

1. 将医疗文档放入`knowledge/knowledge`目录
2. 支持格式：`.md`、`.txt`、`.pdf`
3. 重启应用或调用重新加载接口

### 3. 自定义提示词

修改`src/main/resources/zhaozhi-prompt-template.txt`文件，调整AI助手的行为。

## 🎯 最佳实践

1. **定期更新知识库**: 保持医疗信息的时效性
2. **监控API使用**: 注意API调用限制
3. **备份数据**: 定期备份MongoDB和MySQL数据
4. **性能优化**: 根据使用情况调整记忆压缩参数
5. **安全考虑**: 保护用户隐私和医疗数据安全

## 📞 技术支持

如有问题，请查看：
1. 项目文档
2. API文档
3. 日志文件
4. 错误信息

---

**注意**: 本系统仅供学习和测试使用，不能替代专业医疗诊断。如有医疗问题，请咨询专业医生。
