# AI漫剧Agent需求文档

## 1. 项目概述

### 1.1 项目背景
本项目旨在开发一个适用于AI漫剧创作的智能代理系统，支持两种工作模式：
- **普通模式**：用户需手动触发文生图、图生图、图生视频等操作
- **嘻嘻模式**：Agent自动完成从脚本到最终视频的全流程创作

### 1.2 设计目标
| 目标类别 | 描述 |
| :--- | :--- |
| 易用性 | 提供直观的操作界面，支持普通用户快速上手 |
| 智能化 | 在嘻嘻模式下实现自动化创作流程 |
| 灵活性 | 支持手动干预和自动执行两种模式 |
| 可扩展性 | 支持新增工具、创作流程和AI模型 |
| 多模型支持 | 支持即梦AI及其他AI模型的灵活切换 |

---

## 2. 功能需求

### 2.1 核心模式

#### 2.1.1 普通模式
- 用户手动选择并触发单个工具操作
- 支持分步执行：文生图 → 图生图 → 图生视频
- 每个步骤可独立配置参数
- 支持中间结果预览和调整

#### 2.1.2 嘻嘻模式
- 用户输入创作需求后，Agent自动规划并执行完整流程
- 支持一键生成完整漫剧作品
- 自动处理中间步骤的依赖关系
- 提供创作进度实时反馈

### 2.2 工具能力

| 工具名称 | 功能描述 | 输入参数 | 输出结果 |
| :--- | :--- | :--- | :--- |
| 文生图工具 | 根据文字描述生成漫画风格图片 | 提示词、风格、分辨率 | 图片URL/路径 |
| 图生图工具 | 基于原图进行风格转换或修改 | 原图、提示词、强度 | 新图片URL/路径 |
| 图生视频工具 | 将图片序列合成为视频 | 图片列表、时长、帧率 | 视频URL/路径 |
| 脚本生成工具 | 根据主题生成漫画脚本 | 主题、风格、集数 | 分镜脚本JSON |
| 语音合成工具 | 将文字转换为语音 | 文字内容、音色、语速 | 音频URL/路径 |
| 字幕生成工具 | 生成视频字幕 | 文字内容、样式 | 字幕文件 |
| 视频剪辑工具 | 剪辑和拼接视频片段 | 视频列表、转场效果 | 合成视频 |

### 2.3 创作流程

#### 2.3.1 普通模式流程
```
用户输入需求 → 选择工具 → 选择模型 → 配置参数 → 执行工具 → 查看结果 → 选择下一步
```

#### 2.3.2 嘻嘻模式流程
```
用户输入需求 → Agent分析需求 → 选择最优模型 → 自动规划流程 → 依次执行工具 → 返回最终结果
```

### 2.4 资料库管理

#### 2.4.1 功能定位
**资料库（Material Library）** 用于管理漫剧创作所需的素材资源，支持：
- 素材的增删改查
- 素材分类管理
- 素材标签系统
- 素材搜索和筛选
- **组别管理**：支持多用户共享同组别素材

#### 2.4.2 组别管理

**组别（Group）** 用于组织和共享素材，支持：
- 创建组别
- 添加成员到组别
- 组别内素材共享
- 权限管理

| 字段 | 类型 | 说明 |
| :--- | :--- | :--- |
| id | String | 组别唯一标识 |
| name | String | 组别名称 |
| description | String | 组别描述 |
| owner | String | 组别所有者 |
| members | List<String> | 成员列表（用户ID） |
| createdAt | DateTime | 创建时间 |

#### 2.4.3 素材结构

| 字段 | 类型 | 说明 |
| :--- | :--- | :--- |
| id | String | 素材唯一标识 |
| name | String | 素材名称 |
| imageUrl | String | 素材图片URL |
| description | String | 素材描述 |
| category | String | 素材分类 |
| tags | List<String> | 标签列表 |
| type | String | 素材类型（image/video/audio/text） |
| groupId | String | 所属组别ID |
| usageCount | Integer | 使用次数 |
| createdAt | DateTime | 创建时间 |
| updatedAt | DateTime | 更新时间 |

#### 2.4.3 素材分类

| 分类 | 描述 | 示例 |
| :--- | :--- | :--- |
| character | 角色素材 | 动漫角色立绘 |
| background | 背景素材 | 场景背景图 |
| prop | 道具素材 | 道具物品 |
| effect | 特效素材 | 特效动画 |
| script | 脚本素材 | 剧本片段 |

#### 2.4.4 核心功能

| 功能 | 描述 | API |
| :--- | :--- | :--- |
| 添加素材 | 新增素材到资料库 | POST /api/library |
| 查询素材 | 根据条件查询素材 | GET /api/library |
| 更新素材 | 更新素材信息 | PUT /api/library/{id} |
| 删除素材 | 删除指定素材 | DELETE /api/library/{id} |
| 搜索素材 | 根据关键词搜索 | GET /api/library/search |
| 分类管理 | 管理素材分类 | GET/POST /api/library/categories |

### 2.5 主智能体设计

#### 2.5.1 智能体定位
**主智能体（ManjuMaster）** 是系统的核心决策组件，负责：
- 理解用户输入的自然语言需求
- 分析需求并规划执行步骤
- 调用相应的工具或子智能体完成任务
- 整合结果并反馈给用户

#### 2.4.2 智能体工作流程

```
用户输入 → 意图识别 → 需求分析 → 任务规划 → 工具调用 → 结果整合 → 返回响应
```

#### 2.4.3 核心能力

| 能力模块 | 功能描述 |
| :--- | :--- |
| 意图识别 | 识别用户需求类型（创作需求、工具调用、问题咨询等） |
| 需求分析 | 解析用户输入，提取关键信息（主题、风格、数量等） |
| 任务规划 | 根据需求生成执行计划，确定步骤顺序和依赖关系 |
| 工具调度 | 选择合适的工具/模型执行具体任务 |
| 结果整合 | 汇总各步骤结果，生成最终响应 |

#### 2.4.4 决策机制

**单步任务**（如"生成一张漫画风格的图片"）：
- 直接调用对应工具执行

**多步任务**（如"创作一个校园青春漫剧"）：
1. 生成脚本 → 2. 文生图 → 3. 图生视频

**复杂任务**（需多轮交互）：
- 先澄清需求 → 再执行任务

### 2.5 多模型支持

#### 2.5.1 模型管理
- 支持多种AI模型的配置和切换
- 当前默认使用**即梦AI**模型
- 支持动态添加新模型（如Stable Diffusion、DALL-E、其他自研模型等）

#### 2.5.2 模型列表
| 模型名称 | 类型 | 状态 | 说明 |
| :--- | :--- | :--- | :--- |
| 即梦AI | 文生图/图生视频 | 启用 | 默认模型，支持文生图、图生图、图生视频 |
| Seedream | 图像模型 | 启用 | 即梦AI自研图像生成模型 |
| Seedance | 视频模型 | 启用 | 即梦AI自研视频生成模型 |
| 其他模型 | 预留 | 待扩展 | 后期可添加其他AI模型 |

#### 2.5.3 模型选择策略
- 用户可手动指定模型
- Agent可根据任务类型自动选择最优模型
- 支持模型优先级配置

---

## 3. 非功能需求

### 3.1 性能要求
- 单个工具执行响应时间 < 30秒
- 完整流程（嘻嘻模式）执行时间 < 5分钟（标准复杂度）
- 支持并发任务数 ≥ 5

### 3.2 可用性要求
- 系统可用性 ≥ 99.5%
- 故障恢复时间 < 5分钟

### 3.3 安全性要求
- 用户数据加密存储
- API调用鉴权机制
- 敏感信息脱敏处理

### 3.4 容错与稳定性要求
- **熔断机制**：支持API调用熔断，防止服务雪崩
- **重试机制**：支持失败重试策略
- **降级机制**：服务降级，保证核心功能可用
- **限流机制**：防止恶意请求和资源耗尽

### 3.5 多智能体机制
- **思考智能体**：使用千问模型进行任务规划和决策
- **执行智能体**：负责调用具体工具执行任务
- **协作机制**：多智能体之间的信息传递和协作

---

## 4. 架构设计

### 4.1 架构风格
采用分层架构，结合Agent模式实现智能调度

### 4.2 模块划分

| 模块 | 职责 | 说明 |
| :--- | :--- | :--- |
| Controller层 | REST API控制 | 处理HTTP请求，参数校验 |
| Service层 | 业务逻辑处理 | 模式管理、流程编排 |
| Agent层 | 智能代理核心 | 状态管理、工具调用、决策逻辑 |
| Tool层 | 工具实现 | 各类AI工具的封装 |
| Model层 | AI模型抽象 | 模型接口定义、模型工厂、模型选择器 |
| Library层 | 资料库管理 | 素材资源的存储和管理 |
| Config层 | 配置管理 | 工具注册、参数配置、模型配置 |
| Memory层 | 会话记忆 | 对话历史、状态持久化 |

### 4.3 核心类设计

#### 4.3.1 Agent相关类

| 类名 | 职责 | 继承关系 |
| :--- | :--- | :--- |
| BaseAgent | 抽象基础代理类 | - |
| **ManjuMaster** | **主智能体，负责意图识别、需求分析、任务规划和工具调度** | extends BaseAgent |
| ManjuAgent | 漫剧创作代理（普通模式） | extends BaseAgent |
| XixiAgent | 嘻嘻模式代理（自动创作） | extends ManjuAgent |
| AgentState | 代理状态枚举 | - |

#### 4.3.1.1 主智能体（ManjuMaster）详细设计

**核心职责**：
1. **意图识别**：识别用户需求类型（创作需求、工具调用、问题咨询等）
2. **需求分析**：解析用户输入，提取关键信息
3. **任务规划**：生成执行计划，确定步骤顺序
4. **工具调度**：选择合适的工具/模型执行任务
5. **结果整合**：汇总结果并生成响应

**关键方法**：

| 方法名 | 功能描述 | 参数 | 返回值 |
| :--- | :--- | :--- | :--- |
| `analyzeInput` | 分析用户输入，识别意图 | `input: String` | `IntentAnalysis` |
| `planTask` | 根据分析结果生成任务计划 | `analysis: IntentAnalysis` | `TaskPlan` |
| `executePlan` | 执行任务计划 | `plan: TaskPlan` | `ExecutionResult` |
| `summarizeResult` | 汇总执行结果 | `results: List<StepResult>` | `String` |

**意图类型**：

| 意图类型 | 描述 | 示例 |
| :--- | :--- | :--- |
| TEXT_TO_IMAGE | 文生图需求 | "生成一张漫画风格的少女" |
| IMAGE_TO_IMAGE | 图生图需求 | "把这张图改成卡通风格" |
| IMAGE_TO_VIDEO | 图生视频需求 | "把这些图片做成视频" |
| SCRIPT_GENERATION | 脚本生成需求 | "写一个校园故事脚本" |
| FULL_PRODUCTION | 完整创作需求 | "创作一个校园青春漫剧" |
| QUESTION | 问题咨询 | "如何使用文生图功能？" |
| UNKNOWN | 未知意图 | "帮我做些事情" |

#### 4.3.2 工具相关类

| 类名 | 职责 |
| :--- | :--- |
| TextToImageTool | 文生图工具 |
| ImageToImageTool | 图生图工具 |
| ImageToVideoTool | 图生视频工具 |
| ScriptGeneratorTool | 脚本生成工具 |
| SpeechSynthesisTool | 语音合成工具 |
| SubtitleGeneratorTool | 字幕生成工具 |
| VideoEditTool | 视频剪辑工具 |
| ToolRegistration | 工具注册配置 |

#### 4.3.3 AI模型相关类

| 类名 | 职责 |
| :--- | :--- |
| AiModel | AI模型接口 |
| JimengModel | 即梦AI模型实现 |
| ModelFactory | 模型工厂，负责创建和管理模型实例 |
| ModelSelector | 模型选择器，根据任务选择最优模型 |
| ModelConfig | 模型配置信息 |

#### 4.3.4 熔断与容错相关类

| 类名 | 职责 |
| :--- | :--- |
| CircuitBreaker | 熔断机制封装 |
| CircuitBreakerConfig | 熔断配置 |
| RetryTemplate | 重试模板 |
| RateLimiter | 限流器 |

#### 4.3.5 多智能体机制

| 类名 | 职责 |
| :--- | :--- |
| ThinkAgent | 思考智能体，使用千问模型进行任务规划 |
| ExecuteAgent | 执行智能体，负责调用工具执行具体任务 |
| AgentCoordinator | 智能体协调器，管理多智能体协作 |
| TaskPlan | 任务计划，包含步骤和依赖关系 |

### 4.4 状态转换

```
IDLE → RUNNING (启动执行)
RUNNING → FINISHED (执行完成)
RUNNING → ERROR (执行失败)
RUNNING → PAUSED (手动暂停)
PAUSED → RUNNING (继续执行)
ERROR → IDLE (重置)
FINISHED → IDLE (重置)
```

---

## 5. API接口设计

### 5.1 代理执行接口

#### 5.1.1 执行代理（普通模式）
- **路径**: `/api/agent/execute`
- **方法**: POST
- **请求体**:
```json
{
  "mode": "normal",
  "toolName": "textToImage",
  "model": "jimeng",
  "parameters": {
    "prompt": "漫画风格的少女",
    "style": "anime",
    "resolution": "1024x1024"
  },
  "chatId": "session-123"
}
```
- **响应体**:
```json
{
  "success": true,
  "result": {
    "imageUrl": "https://example.com/image.png"
  },
  "step": 1,
  "totalSteps": 3
}
```

#### 5.1.2 执行代理（嘻嘻模式）
- **路径**: `/api/agent/xixi`
- **方法**: POST
- **请求体**:
```json
{
  "mode": "xixi",
  "prompt": "创作一个关于校园青春的3集漫剧",
  "style": "日系动漫",
  "model": "jimeng",
  "chatId": "session-123"
}
```
- **响应体**:
```json
{
  "success": true,
  "status": "running",
  "taskId": "task-456",
  "progress": 25
}
```

### 5.2 任务状态接口

#### 5.2.1 查询任务状态
- **路径**: `/api/agent/task/{taskId}`
- **方法**: GET
- **响应体**:
```json
{
  "taskId": "task-456",
  "status": "running",
  "progress": 50,
  "currentStep": "图生图处理",
  "results": [
    {
      "step": 1,
      "tool": "scriptGenerator",
      "result": "脚本生成完成"
    }
  ]
}
```

### 5.3 工具列表接口

#### 5.3.1 获取可用工具
- **路径**: `/api/tools`
- **方法**: GET
- **响应体**:
```json
{
  "tools": [
    {
      "name": "textToImage",
      "description": "文生图工具",
      "parameters": [
        {"name": "prompt", "type": "string", "required": true},
        {"name": "style", "type": "string", "required": false},
        {"name": "resolution", "type": "string", "required": false}
      ],
      "supportedModels": ["jimeng", "seedream"]
    }
  ]
}
```

### 5.4 模型管理接口

#### 5.4.1 获取可用模型
- **路径**: `/api/models`
- **方法**: GET
- **响应体**:
```json
{
  "models": [
    {
      "name": "jimeng",
      "displayName": "即梦AI",
      "description": "字节跳动旗下AI创意平台，支持文生图、图生视频",
      "status": "enabled",
      "capabilities": ["textToImage", "imageToImage", "imageToVideo"],
      "default": true
    },
    {
      "name": "seedream",
      "displayName": "Seedream",
      "description": "即梦AI自研图像生成模型",
      "status": "enabled",
      "capabilities": ["textToImage", "imageToImage"],
      "default": false
    },
    {
      "name": "seedance",
      "displayName": "Seedance",
      "description": "即梦AI自研视频生成模型",
      "status": "enabled",
      "capabilities": ["imageToVideo"],
      "default": false
    }
  ]
}
```

#### 5.4.2 获取模型详情
- **路径**: `/api/models/{modelName}`
- **方法**: GET
- **响应体**:
```json
{
  "name": "jimeng",
  "displayName": "即梦AI",
  "description": "字节跳动旗下AI创意平台",
  "status": "enabled",
  "capabilities": ["textToImage", "imageToImage", "imageToVideo"],
  "config": {
    "apiUrl": "https://api.jimeng.com",
    "supportedStyles": ["anime", "realistic", "cartoon"],
    "maxResolution": "4096x4096",
    "maxVideoDuration": 180
  }
}
```

---

## 6. 数据库与数据结构

### 6.1 任务记录表

| 字段名 | 类型 | 说明 |
| :--- | :--- | :--- |
| id | VARCHAR(36) | 任务ID |
| chatId | VARCHAR(36) | 会话ID |
| mode | VARCHAR(20) | 模式（normal/xixi） |
| status | VARCHAR(20) | 状态（running/finished/error） |
| progress | INT | 进度百分比 |
| prompt | TEXT | 用户提示词 |
| results | TEXT | 结果JSON |
| createdAt | DATETIME | 创建时间 |
| updatedAt | DATETIME | 更新时间 |

### 6.2 会话记忆表

| 字段名 | 类型 | 说明 |
| :--- | :--- | :--- |
| id | VARCHAR(36) | 记录ID |
| chatId | VARCHAR(36) | 会话ID |
| messageType | VARCHAR(20) | 消息类型（user/assistant/tool） |
| content | TEXT | 消息内容 |
| timestamp | DATETIME | 时间戳 |

---

## 7. 部署与集成

### 7.1 依赖服务
- AI绘画服务（如Stable Diffusion API）
- 视频处理服务（如FFmpeg）
- 语音合成服务（如阿里云TTS）
- 向量数据库（可选，用于RAG）

### 7.2 配置说明

```yaml
# application.yml
agent:
  max-steps: 20
  timeout-minutes: 30

tools:
  text-to-image:
    api-url: ${TEXT_TO_IMAGE_API_URL}
    api-key: ${TEXT_TO_IMAGE_API_KEY}
  image-to-video:
    ffmpeg-path: /usr/bin/ffmpeg

xixi:
  auto-execute: true
  default-style: anime

models:
  default: jimeng
  jimeng:
    api-url: ${JIMENG_API_URL:https://api.jimeng.com}
    api-key: ${JIMENG_API_KEY}
    enabled: true
    capabilities:
      - textToImage
      - imageToImage
      - imageToVideo
  seedream:
    api-url: ${SEEDREAM_API_URL}
    api-key: ${SEEDREAM_API_KEY}
    enabled: true
    capabilities:
      - textToImage
      - imageToImage
  seedance:
    api-url: ${SEEDANCE_API_URL}
    api-key: ${SEEDANCE_API_KEY}
    enabled: true
    capabilities:
      - imageToVideo

circuit-breaker:
  enabled: true
  failure-threshold: 5
  success-threshold: 3
  wait-duration-ms: 60000
  timeout-ms: 30000

rate-limit:
  enabled: true
  requests-per-second: 10
  burst-capacity: 20

multi-agent:
  think-model: qianwen
  think-agent:
    system-prompt: "你是一个智能任务规划助手，负责分析用户需求并生成详细的执行计划。"
  execute-agent:
    max-parallel-tasks: 5
```

---

## 8. 安全性

### 8.1 认证机制
- JWT Token认证
- API Key管理

### 8.2 权限控制
- 用户级资源隔离
- 操作日志记录

---

## 9. 版本历史

| 版本 | 日期 | 变更说明 |
| :--- | :--- | :--- |
| v1.0 | 2026-05-02 | 初始需求文档 |

---

## 附录：参考架构

参考项目：`D:\桌面\学习资料\yu-ai-agent-master`

核心借鉴点：
1. Agent分层架构设计
2. 工具注册与调用机制
3. 状态管理模式
4. SSE流式输出支持
5. 会话记忆实现