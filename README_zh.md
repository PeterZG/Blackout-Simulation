# Blackout — 轨道通信模拟器

在木星周围建立设备与卫星网络，观察通信范围、文件传输和仿真时间变化。

维护者：**Weihou Zeng（PeterZG）**。

## 功能

支持手持、笔记本和桌面设备，以及标准、中继和传送卫星。模拟包括视线与距离检查、带宽共享、存储容量和通信中断。

操作台提供实体检查、位置编辑、文件读取/编辑/删除、传输进度、活动记录和独立视觉动效开关。卫星位置来自 Java 模型。

木星采用真正的 Three.js/WebGL 3D 球体。鼠标拖拽或单指触控可 360° 环绕，滚轮/双指缩放；按钮支持俯仰、俯视、重置和自动环绕。画布获得焦点后，方向键旋转，+/− 缩放，Home 重置。相机操作不改变仿真位置；实体仍遵循原有单一轨道平面规则。

## 目录

- `app/src/main/java/unsw/`：Java 仿真、模型与 HTTP 适配。
- `app/src/main/resources/app/`：页面、样式、API 模块与 3D 场景源码。
- `app/build/generated/frontend/`：构建产物，不手动编辑。
- `app/src/test/blackout/`：Java 测试。
- `scripts/browser-check.cjs`：浏览器测试。

## 启动

安装 JDK 17+、Node.js 22+ 和 npm：

```sh
./gradlew run
```

Gradle 自动安装锁定的 npm 依赖并构建浏览器模块。首次构建需要联网。Windows 使用 `gradlew.bat`。打开 `http://127.0.0.1:4567/app/`。更换端口的方式见 [英文 README](README.md)。

## 第一次传输

1. 在 0° 添加 Handheld Device。
2. 在 0° 添加 Standard Satellite，半径设为 80,000 km。
3. 选择设备，添加 `telemetry.txt`，内容为 `Hello Jupiter`。
4. 选择 Send file，目标设为该卫星。
5. 选择目标，使用 +1 min 或 Run simulation 查看接收进度。

半径从木星中心计算。标准卫星每分钟接收一个字符，示例文件需要多个时间步。

Pause visual motion 只暂停装饰动画；Run simulation 每秒推进一个模型分钟。时钟显示本次浏览器会话推进的时间。

## 测试与限制

浏览器检查覆盖普通界面文字至少 16px、对比度、六种宽度及原生 200% 缩放，长名称在列表和场景中换行。GitHub 流水线构建并测试 Java 和浏览器模块，启动打包应用执行真实流程并保存截图；原 GitLab 文件供课程运行器使用。没有自动公网部署，本地通过与远程 CI 结果分别确认。

运行 `./gradlew test lint assemble`。Java 在 4577 端口运行时，执行 `npm run test:browser`，通过隔离会话操作真实 API。

状态保存在内存中，闲置一小时过期，重启服务器会清空。容量使用字符数量规则。图形需要 WebGL 2，不可用时仍可操作网络列表。3D 展示不扩展原有物理规则。服务器用于本地演示，不是公网多用户服务。

[English](README.md) · [代码导览](CODE_GUIDE.md)
