# AOE4 Forum

一个基于 Spring Boot + Vue 3 构建的现代化游戏论坛平台，专为《帝国时代4》玩家社区打造。


## 项目概述

AOE4论坛是一个功能完整的社区平台，提供帖子发布、用户互动、实时通知等核心功能。系统采用前后端分离架构，后端使用微服务模式，前端采用现代化的Vue 3生态。

## 技术架构

### 后端技术栈
- **框架**: Spring Boot 2.7.18
- **数据库**: MySQL 8.0.23 + MyBatis
- **缓存**: Redis (用户会话、热点数据)
- **搜索**: Elasticsearch 8.15.0
- **消息队列**: RabbitMQ

### 前端技术栈
- **框架**: Vue 3.4.38 + TypeScript
- **构建工具**: Vite 5.4.2


### 项目结构
```
aoe4Forum/
├── aoe4Forum-common/     # 公共模块 (实体类、工具类、服务层)
├── aoe4Forum-admin/      # 管理后台模块
├── aoe4Forum-web/        # Web API模块
└── pom.xml              # 父级Maven配置

webapp/                   # 前端Vue应用
├── src/
│   ├── components/      # Vue组件
│   ├── utils/          # 工具函数
│   └── main.ts         # 应用入口
├── public/             # 静态资源
└── package.json        # 前端依赖配置
```

#### 3. 后端启动
```bash
# 进入项目根目录
cd aoe4Forum

# 修改配置文件
# 编辑 aoe4Forum-web/src/main/resources/application.yml
# 配置数据库连接、Redis连接等

# 编译和启动
mvn clean install
cd aoe4Forum-web
mvn spring-boot:run
```

#### 4. 前端启动
```bash
# 进入前端目录
cd webapp

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```
