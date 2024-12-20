# 卫星模拟 - Blackout-Simulation

该项目模拟了设备和卫星在网络环境中的交互，展示了面向对象编程（OOP）原则和稳健的软件设计。该模拟支持文件传输、实体移动和动态范围计算。

## 功能

1. **设备和卫星管理**：
   - 模拟设备（如手持设备、台式机）和卫星（如中继卫星、标准卫星）。
   - 处理设备和卫星的动态移动和状态更新。

2. **文件传输模拟**：
   - 模拟通过卫星在设备之间传输文件。
   - 跟踪传输进度并动态更新实体状态。

3. **时间推进**：
   - 每个时间步骤更新模拟，反映移动、传输和可用性变化。

4. **可扩展性**：
   - 易于扩展以支持新的设备或卫星类型。

## 设计原则

1. **模块化**：
   - 核心逻辑封装在 `BlackoutController` 和 `Transfer` 等模块中。

2. **封装性**：
   - 私有属性和公共方法确保数据完整性。

3. **关注点分离**：
   - 不同的类处理模拟控制、实体管理和实用功能。

4. **错误处理**：
   - 实现自定义异常（如 `FileTransferException`）以确保稳健的错误管理。

## 技术栈

### 后端：
- **语言**：Java
- **框架**：Spark（轻量级HTTP框架，用于处理模拟请求）
- **JSON 序列化**：Gson（用于处理JSON转换）
- **设计模式**：
  - 控制器模式：`BlackoutController` 管理模拟。
  - 自定义异常模式：`FileTransferException` 处理特定错误。

### 前端：
- 未明确包括；交互通过HTTP请求或CLI模拟。

## 文件结构

```
src/
├── main/
│   ├── java/
│   │   ├── scintilla/          # 工具类和辅助函数
│   │   ├── unsw/               # 核心功能
│   │   │   ├── App.java        # 模拟的入口点
│   │   │   ├── blackout/       # 主要模拟逻辑（如：BlackoutController）
│   │   │   ├── item/           # 文件和资源管理
│   │   │   ├── response/       # 响应模型和处理器
│   │   │   ├── utils/          # 辅助方法（如：角度计算）
│   ├── resources/              # 静态资源或配置文件
├── test/
│   ├── blackout/               # 模拟组件的单元测试
```

## 使用方法

### 编译
使用Gradle编译项目：
```bash
./gradlew build
```

### 运行模拟
通过Spark服务器运行模拟：
```bash
./gradlew run
```

### 测试
运行测试套件以验证实现：
```bash
./gradlew test
```

## 示例模拟
与模拟交互的示例HTTP请求：
```bash
# 创建一个新设备
curl -X POST -d '{"device": "handheld", "id": "D1"}' http://localhost:4567/device

# 模拟时间推进
curl -X POST -d '{"time": 10}' http://localhost:4567/progress

# 获取所有实体的当前状态
curl -X GET http://localhost:4567/state
```

## 未来改进

1. **增强前端**:
   - 添加基于Web的界面，实时与模拟交互n.

2. **优化算法**:
   - 改进文件传输逻辑，提高可扩展性。

3. **增加测试覆盖率**:
   - 添加更多边界情况测试，以验证系统的稳健性。

## 鸣谢

本项目作为UNSW COMP2511课程的作业1进行开发。感谢课程提供的学习资源和支持，帮助我们理解和掌握面向对象编程的核心概念与实践。
