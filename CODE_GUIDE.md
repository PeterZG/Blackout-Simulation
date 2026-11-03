# Blackout 代码导览

## 调用链

界面操作 → `mission-control.js` → `simulation-api.js` → Java App → BlackoutController → 实体和文件传输规则。

## Java 模块

| 路径                                      | 职责               |
| ----------------------------------------- | ------------------ |
| `app/src/main/java/unsw/App.java`         | HTTP 与会话处理    |
| `app/src/main/java/unsw/SimulationInput.java` | HTTP 数值、布尔值和单位边界验证 |
| `app/src/main/java/unsw/blackout/`        | 仿真和传输生命周期 |
| `app/src/main/java/unsw/item/`            | 设备、卫星与文件   |
| `app/src/main/java/unsw/utils/`           | 角度与几何计算     |
| `app/src/main/java/unsw/response/models/` | 对外响应模型       |
| `app/src/test/blackout/`                  | Java 测试          |

## 浏览器模块

所有界面源码位于 `app/src/main/resources/app/`：

- `index.html`：页面结构和原生对话框。
- `mission-control.css`：排版、配色、响应式和焦点。
- `mission-control.js`：网络列表、实体详情、操作表单和运行状态。
- `simulation-api.js`：后端请求、超时和错误处理。
- `orbital-scene.js`：Three.js 球体、OrbitControls 相机、轨道、连接线和避让标签。

Gradle 的 `buildFrontend` 调用 esbuild，产物在 `app/build/generated/frontend/`，随 Java 静态资源一起发布到本地服务。不手动修改 bundle；`node_modules` 与构建目录不纳入 Git。

## 时间和坐标

页面输入角度为度，Java API 使用弧度。卫星半径从木星中心计算。

运行仿真会推进 Java 模型；视觉暂停只控制球体旋转和自动相机环绕。手动相机操作始终可用。画面上的实体位置来自模型响应，单一轨道平面保持不变；不增加任意物理规则。

每个浏览器会话保存独立控制器和坡度配置，闲置一小时过期。编辑实体先验证参数，再取消相关传输；完成的文件保留。编辑设备文件必须等待它的发送任务结束。删除源文件时清理未完成接收副本。

文件大小按 Java 字符数计算，不是 UTF-8 字节数，空文件也是有效文件。输入解析在独立的 `SimulationInput` 中测试，不能将无效布尔值悄悄解释为 `false`。

## 界面与维护约定

操作字段使用明确标签；弹窗支持 Escape，删除前需要确认。手机端引导放在画布下方，避免覆盖场景。文字放不下时采用换行或重排。

实体详情先显示完整名称，编辑与移除在下一行两侧对齐，并保留至少 44px 的点击高度。名称不会挤压操作按钮，移除操作与日常编辑保持视觉距离。

实体长名称完整换行，标签按实际宽高避让。相机复位先清除拖动惯性，确保低帧率下 Home 和复位按钮也回到同一视角。浏览器回归覆盖这些行为及 360° 环绕、两极、触摸、缩放、字号与颜色对比。

选择实体的异步响应必须仍属于当前选择，过期请求的成功或失败都不能覆盖新选择。从浏览器往返缓存恢复时重启原有渲染循环，不销毁后再使用旧 GPU 对象；真正离开页面时才释放场景。活动日志只保留最近六条，避免长时间使用持续积累内存。

Java 公共接口与原有模型规则保持兼容。注释解释单位、传输状态和权限/错误边界。修改后运行 Java 测试及 `scripts/browser-check.cjs`。

[启动与使用](README.md)
