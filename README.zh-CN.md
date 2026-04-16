# Adjust for Cocos Creator

[English](./README.md)

这是一个给 Cocos Creator Android 项目使用的 Adjust 封装，仓库里同时提供了：

- TS 层封装代码：`assets/script/adjust/`
- Android 原生桥接模块：`adjust_cocos_creator/`
- 可直接集成的产物：`adjust_cocos_creator-release.aar`

## 快速接入

当前推荐两种接入方式，优先推荐直接引用 `aar`。

### 方案一：直接引用 AAR

适合希望快速复用桥接能力、不想把原生源码整体并入项目的场景。

1. 把 `adjust_cocos_creator-release.aar` 放到你的 Android 工程中并完成引用。
2. 把 `assets/script/adjust/` 拷贝到你的 Cocos Creator 项目里。
3. 在 Android 工程里增加 Adjust SDK 依赖：

```gradle
implementation 'com.adjust.sdk:adjust-android:5.6.0'
```

4. 在游戏启动阶段引入并初始化 `AdjustManager`。

```ts
import { AdjustManager } from './adjust/adjust_manager';

AdjustManager.init({
    appToken: 'your_app_token',
    product: 'production',
    logLevel: 'INFO',
});
```

也就是说，如果你已经把 `aar` 加进项目里，实际还需要做的事情就两步：

- 引入 `assets/script/adjust/`
- 增加 `Adjust SDK` 依赖

### 方案二：引用源码模块

适合需要继续维护原生桥源码，或者希望在自己项目里直接修改 Java 实现的场景。

1. 将 `adjust_cocos_creator/` 作为 Android library module 引入你的工程。
2. 将 `assets/script/adjust/` 拷贝到 Cocos Creator 项目中。
3. 确保模块能访问你们现有的 Cocos Android 构建环境。
4. 增加 Adjust SDK 依赖：

```gradle
implementation 'com.adjust.sdk:adjust-android:5.6.0'
```

5. 在 TS 启动代码里调用 `AdjustManager.init(...)`。

## 目录说明

- `assets/script/adjust/adjust_manager.ts`
  游戏侧主要使用的公开 API。
- `assets/script/adjust/adjust_config.ts`
  初始化配置定义。
- `assets/script/adjust/adjust_types.ts`
  事件、回调、返回值等共享类型。
- `assets/script/adjust/adjust_message_dispatcher.ts`
  JS 与原生之间的消息派发、事件注册、超时处理。
- `adjust_cocos_creator/src/main/java/com/game/magic/AdjustCocos.java`
  Android 原生 Adjust 能力封装与回调转发。
- `adjust_cocos_creator/src/main/java/com/game/magic/AdjustCocosBridge.java`
  JSB 桥注册逻辑。
- `adjust_cocos_creator/src/main/java/com/game/magic/AdjustCocosInit.java`
  桥初始化入口。

## 工作方式

这个封装主要有两条调用链路：

1. 派发链路
   用于需要原生回调或异步返回值的接口。TS 先发消息到原生，原生执行后再把结果回传回来。
2. 反射链路
   用于简单的 Android 静态接口。`AdjustManager` 通过 `native.reflection.callStaticMethod(...)` 直接调用 `com.adjust.sdk.Adjust`。

桥接初始化由 TS 侧触发，通常调用 `AdjustManager.initBridge()` 或 `AdjustManager.init(...)` 即可。

## 已支持能力

- SDK 初始化与生命周期管理
- 事件上报
- 广告收益上报
- adid、attribution、install referrer、deeplink、sdk version 读取
- deferred deeplink 回调转发
- Google Play 订单校验
- 部分静态 Adjust API 的直接调用

## 基础用法

### 初始化

```ts
import { AdjustManager } from './adjust/adjust_manager';

AdjustManager.init({
    appToken: 'your_app_token',
    product: 'production',
    logLevel: 'INFO',
});
```

### 事件上报

```ts
AdjustManager.trackEvent({
    eventToken: 'abc123',
    revenue: 1.99,
    currency: 'USD',
    orderId: 'order_001',
});
```

### 带超时读取

```ts
const adid = await AdjustManager.getAdidWithTimeout(3000);
const attribution = await AdjustManager.getAttributionWithTimeout(3000);
const googleAdId = await AdjustManager.getGoogleAdIdWithTimeout(3000);
```

### 广告收益上报

```ts
AdjustManager.trackAdRevenue({
    source: 'admob_sdk',
    revenue: 0.12,
    currency: 'USD',
    adRevenueNetwork: 'admob',
    adRevenueUnit: 'rewarded',
    adRevenuePlacement: 'level_complete',
});
```

## 接入说明

- 这个封装当前面向 Android 原生集成。
- 反射相关辅助接口依赖 Android 运行时以及 `native.reflection`。
- deferred deeplink 的原生返回值不能由 TS 同步控制。
- 当前原生模块依赖的 Adjust 版本是 `com.adjust.sdk:adjust-android:5.6.0`。

## 验证

当前原生模块已通过以下命令编译验证：

```powershell
./gradlew.bat :adjust_cocos_creator:compileDebugJavaWithJavac
```
