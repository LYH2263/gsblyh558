# 在线考试系统 (Online Exam System)

一款基于 **Apple 设计语言** 开发的现代化在线练习与考试平台。系统采用全栈容器化架构，提供极致的视觉体验与丝滑的交互过程。

---

## ✨ 核心特性 (Features)

### 🎨 极致视觉设计
- **Apple 风格 UI**: 严格遵循苹果设计规范，使用 SF Pro / 萍方字体栈，色彩柔和，圆角过渡自然。
- **毛玻璃效果 (Glassmorphism)**: 导航栏与面板采用高斯模糊背景，提升界面层次感与高级感。
- **响应式布局**: 适配桌面端与移动端，提供一致的跨平台体验。

### � 考试与练习
- **多样化题型**: 支持单选题、多选题、判断题、填空题及简答题。
- **智能判分逻辑**:
  - 客观题自动秒批。
  - 简答题支持**关键词加权评分**，命中 60% 权重关键词即判定为正确。
- **专项练习模式**: 支持按知识分类进行顺序练习或随机练习。
- **智能错题本**: 自动收集练习/考试中的错题，支持重新挑战，挑战成功后自动移出。

### �🛠 管理功能
- **全能管理后台**: 分类管理、题库维护、考试发布一站式操作。
- **实时校验**: 考试总分与题目分值自动实时校验，确保数据准确性。

## 🛠 技术栈 (Tech Stack)

### 前端 (Frontend)
- **Framework**: Vue 3 (Composition API)
- **Build Tool**: Vite
- **UI Kit**: Bootstrap 5 (Apple Custom Style)
- **State Management**: Pinia
- **Icons**: Bootstrap Icons

### 后端 (Backend)
- **Framework**: Spring Boot 3.2.1 (Java 17)
- **Security**: Spring Security + JWT (无状态认证)
- **ORM**: Spring Data JPA (Hibernate)
- **Validation**: Jakarta Validation
- **Observability**: Spring Boot Actuator (健康检查)

### 基础设施 (Infrastructure)
- **Database**: PostgreSQL 15
- **Container**: Docker + Docker Compose
- **Web Server**: Nginx (Frontend Serving)

## 🚀 快速启动 (Quick Start)

### 环境要求
- Docker Desktop
- Node.js 20+ (仅本地开发)
- Java 17 (仅本地开发)

### 一键运行
1. 克隆项目到本地。
2. 在根目录执行：
   ```bash
   docker compose up --build
   ```
3. 等待容器启动。

## 🔗 服务地址 (Services)

| 服务 | 地址 | 备注 |
| :--- | :--- | :--- |
| **前端界面** | [http://localhost:3000](http://localhost:3000) | 默认端口 |
| **后端 API** | [http://localhost:8080](http://localhost:8080) | 包含 Swagger 文档(可选) |
| **健康检查** | [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health) | Docker 监控用 |
| **数据库** | `localhost:15432` | user: `postgres` / pass: `password` |

## 🧪 测试账号 (Test Accounts)

| 角色 | 用户名 | 密码 | 权限 |
| :--- | :--- | :--- | :--- |
| **管理员** | `admin` | `123456` | 拥有所有管理权限 |
| **普通用户** | `user` | `123456` | 练习与考试权限 |

---

## 🐳 Docker 优化规范 (Docker Optimization)

### 1. 镜像源配置
项目已预配置 Maven 阿里云镜像及 npm 淘宝镜像，解决国内环境构建慢的问题。

### 2. 构建加速 (Fast Build)
- **npm ci**: 前端构建强制使用 `npm ci` 替代 `npm install`，确保依赖一致性并提升构建速度。
- **Multi-stage Build**: 采用多阶段构建，最终运行镜像仅包含 JRE/Nginx，极大缩小镜像体积。

### 3. 健康检查 (Healthcheck)
`docker-compose.yml` 中集成了基于 Spring Actuator 的健康检查：
- 数据库就绪后才会启动后端。
- 后端健康状态 (`UP`) 确认后前端才会对外提供服务。

