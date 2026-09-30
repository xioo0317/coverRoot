# coverRoot

基于 [KernelSU](https://github.com/tiann/KernelSU) manager UI 的空壳应用。UI 层完整保留自上游，root / 内核业务已全部移除。

## 保留的 UI 能力

- Material 3 Expressive / Miuix 双 UI 模式（运行时切换）
- 莫奈动态取色（materialKolor，TonalSpot / 基础色 / 色彩风格 / 色彩规格全可调）
- Miuix 模糊（miuix-blur）与苹果液态玻璃悬浮底栏（FloatingBottomBar + liquid）
- 预测性返回手势（navigationevent-compose）
- 模块页、主页、设置页、关于页、调色板页完整交互框架

## 构建

```bash
cd manager
./gradlew assembleDebug
```

要求 JDK 21、Android SDK 37。

## 说明

- `applicationId`：`com.coverroot`，版本 `v1.0.0`
- root 相关代码（KernelSU 内核接口、模块安装刷入、WebUI 容器、超级用户授权等）不在本项目范围内，相关入口在 UI 上以空操作保留

## License

[GPL-3.0](LICENSE)（继承自上游 KernelSU）
