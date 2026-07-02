# OurcraftGuard

[![Version](https://img.shields.io/badge/version-1.0.0-blue.svg)](https://github.com/Yuncan050115/OurcraftGuard)
[![Paper](https://img.shields.io/badge/Paper-1.21--26.X-green.svg)](https://papermc.io)
[![Folia](https://img.shields.io/badge/Folia-supported-success.svg)](https://papermc.io/software/folia)
[![License](https://img.shields.io/badge/license-GPL--3.0-orange.svg)](LICENSE)
[![bStats](https://img.shields.io/badge/bStats-enabled-brightgreen.svg)](https://bstats.org/plugin/bukkit/OurcraftGuard/32310)
[![JDK](https://img.shields.io/badge/JDK-21+-red.svg)](https://adoptium.net)

> Ourcraft 服务器轻量反加速内核 (Java) — Folia 兼容，分组 bypass 权限，只防 XZ 轴水平高速移动

## 功能

- **XZ 轴高速移动检测**: 跑 Minecraft 原版运动方程预测每 tick 最大合法位移
- **分组 Bypass**: 通过 LuckPerms 权限节点分组免检 (速度/飞行/计时器独立控制)
- **宽松默认配置**: leniency=10，原版附魔位移 (风爆/突进/风弹/盾牌冲刺/灵魂疾行等) 不误判
- **Y 轴不检测**: flight=false 默认关闭，垂直方向不管，避免跳跃/风爆等误判
- **Folia 兼容**: 自动检测 RegionizedServer，调度全部走抽象层
- **多版本适配**: 使用 ProtocolLib 替代 NMS 通道注入，1.21+ 全版本通用
- **bStats 集成**: 匿名统计数据 (pluginId 32310)，Shadow 重定位避免冲突

## 命令

| 命令 | 别名 | 说明 |
|------|------|------|
| `/ourcraftguard reload` | `/og reload` | 重载配置 (需 admin) |
| `/ourcraftguard status` | `/og status` | 查看状态和你的 bypass 权限 |
| `/ourcraftguard help`   | `/og help`   | 显示帮助 |

## 权限

| 权限节点 | 默认 | 说明 |
|----------|------|------|
| `ourcraftguard.admin`            | OP  | 管理命令 (重载、状态查询) |
| `ourcraftguard.bypass`           | 无  | 绕过全部检查 (速度+飞行+计时器) |
| `ourcraftguard.bypass.speed`     | 无  | 仅绕过速度检查 |
| `ourcraftguard.bypass.flight`    | 无  | 仅绕过飞行检查 |
| `ourcraftguard.bypass.timer`     | 无  | 仅绕过计时器检查 |

### LuckPerms 分组配置示例

```bash
# 让 vip 组绕过全部检查
/lp group vip permission set ourcraftguard.bypass true

# 让 builder 组只绕过飞行 (建造时方便)，仍检测加速
/lp group builder permission set ourcraftguard.bypass.flight true
```

## 原版功能兼容

OurcraftGuard 采用**宽松配置**而非免检窗口方案，确保原版附魔位移不被误判：

| 能力 | 兼容方式 | 说明 |
|------|----------|------|
| **风爆 (Wind Burst)**     | leniency=10 容差 | 重锤重击风弹爆炸推飞玩家 |
| **突进 (Lunge)**          | leniency=10 容差 | 三叉戟戳刺冲刺 |
| **激流 (Riptide)**        | riptide 模型 + 容差 | 雨天三叉戟激流冲刺 |
| **风弹 (Wind Charge)**    | leniency=10 容差 | 投掷风弹后气流推动 |
| **灵魂疾行 (Soul Speed)** | leniency=10 容差 | 灵魂沙/土上加速 |
| **盾牌冲刺 (1.21+)**      | leniency=10 容差 | 持盾潜行冲刺 |
| **击退 (Knockback)**      | knockback 模型   | 被击中造成的位移 |
| **末影龙击退**             | 5s 大容差        | 龙击退造成的大位移 |
| **鞘翅滑翔 (Elytra)**     | 物理模型         | 鞘翅物理模型单独建模 |

### 设计思路

**为什么不用免检窗口？**
免检窗口方案 (监听事件 → 设置 abilityExemptUntilMs → 期间跳过检测) 存在时序问题：突进/风爆等附魔的位移包可能在监听器触发之前就到达，导致窗口还没设置就被回弹。

**为什么用宽松配置？**
直接把 `leniency-multiplier` 调到 10，`per-tick-tolerance` 调到 0.5，`violation-threshold` 调到 5.0，让物理引擎的容差大到足以容纳所有原版附魔位移，但仍能抓住瞬间传送 (比如从 0 到 100 方块/tick)。

**为什么关闭 flight 和 timer？**
- `flight: false` — Y 轴垂直方向不管，避免跳跃/风爆等垂直位移误判
- `timer: false` — 计时器检测对高 ping 和原版附魔容易误判

这样只保留 XZ 轴水平高速移动检测，正是反加速的核心诉求。

## 配置文件

`plugins/OurcraftGuard/config.yml`:

```yaml
checks:
  # 全局宽松倍率。10 = 允许 10 倍正常速度，容纳原版附魔位移
  leniency-multiplier: 10

  speed:
    per-tick-tolerance: 0.5      # 每 tick 容差 (方块)
    violation-threshold: 5.0     # 累计超限触发违规
    violation-decay: 0.5         # 干净包衰减

    knockback:
      multiplier: 6.0
      duration: 1000  # ms

    riptide:
      multiplier: 2.5
      duration: 3000

    elytra:
      landing-duration: 1500

    vehicle-speed-multiplier: 1.5
    vehicle-ice-speed-multiplier: 4.3

  timer:
    enabled: false               # 计时器检测 (默认关闭)

  flight:
    enabled: false               # Y 轴飞行检测 (默认关闭，只防 XZ 轴)

settings:
  debug-mode: false
```

## 依赖

| 插件 | 版本 | 必需 | 说明 |
|------|------|------|------|
| **ProtocolLib** | 5.x+ | ✅ | 跨版本数据包监听 (替代 NMS 通道注入) |
| LuckPerms | 5.x+ | 软依赖 | 分组 bypass 权限管理 |

> **无需 Vault / PlaceholderAPI / 经济插件** — 本插件只做反加速，零外部依赖 (除 ProtocolLib)。

## 兼容性

| Minecraft | Paper / Purpur | Folia | 状态 |
|-----------|---------------|-------|------|
| 1.21.x | 1.21+ | 1.21+ | ✅ |
| 26.1.X | 26.1.X | 26.1.X | ✅ |

> 本插件基于 ProtocolLib 而非 NMS 通道注入，理论上兼容所有 ProtocolLib 支持的版本。上表为已测试版本。

## 构建

```bash
git clone https://github.com/Yuncan050115/OurcraftGuard.git
cd OurcraftGuard
./gradlew shadowJar
# 输出: build/libs/OurcraftGuard-1.0.0.jar
```

**要求**: JDK 21+, Gradle 9.x + Shadow (项目自带 wrapper)

### 依赖打包

- **ProtocolLib** — `compileOnly`，运行时由服务器插件目录提供
- **bStats Bukkit 3.2.1** — `implementation` + Shadow 重定位到 `com.ourcraft.guard.libs.bstats`
- **Paper API 1.21.1** — `compileOnly`，运行时由服务端提供

## 安装

1. 将 `OurcraftGuard-1.0.0.jar` 放入服务器的 `plugins/` 目录
2. 确保 `ProtocolLib.jar` 已安装 (前置)
3. 重启服务器
4. (可选) 通过 LuckPerms 给指定组分配 bypass 权限

## 技术细节

### 反作弊原理

OurcraftGuard 使用物理引擎层检测 XZ 轴水平高速移动：

每个移动包都跑一次 Minecraft 原版运动方程 (含地面摩擦、空气阻力、跳跃初速度、药水效果、方块滑度等)，预测玩家该 tick 的最大合法位移。如果实际位移超过预测值 × leniency + tolerance，累加到违规缓冲区；缓冲区超过阈值 (5.0 方块) 则回弹。

leniency=10 意味着允许 10 倍于正常速度的位移 (约 13 方块/tick)，足以容纳所有原版附魔位移，但仍能抓住瞬间传送作弊。

### Folia 兼容

通过 `SchedulerUtil` 抽象层兼容 Folia：

- **同步任务** — Folia 用 `GlobalRegionScheduler`，Paper 用 `BukkitScheduler`
- **实体任务** — Folia 用 `EntityScheduler` (安全传送玩家)，Paper 回退到主线程
- **异步/定时任务** — Folia 用 `AsyncScheduler`，Paper 用 `BukkitScheduler`

### 多版本适配

- 所有数据包监听通过 **ProtocolLib** 完成，不直接引用 NMS 类
- 较新版本才有的实体类型 (如 `HAPPY_GHAST`) 通过 `EntityType.name()` 字符串比较
- 较新版本才有的附魔 (如 `wind_burst`、`lunge`) 通过 `NamespacedKey.minecraft(key)` 查找
- ProtocolLib 字段读取前检查 `StructureModifier.size()`，并对所有监听器加 try-catch 兜底，防止版本差异导致 `FieldAccessException` 打断 Netty 管线

## bStats

本插件使用 [bStats](https://bstats.org/plugin/bukkit/OurcraftGuard/32310) 收集匿名使用数据。可在 `plugins/bStats/config.yml` 中关闭。

## 更新日志

### v1.0.0 (2026-07-01)
- 🎉 基于 VelocityGuard 3.3 物理引擎重写为独立实现
- ✨ 分组 bypass 权限节点 (ourcraftguard.bypass[.speed|.flight|.timer])
- ✨ Folia 兼容 (SchedulerUtil 抽象层，自动检测 RegionizedServer)
- ✨ 多版本适配 (ProtocolLib 替代 NMS 通道注入)
- ✨ bStats 集成 (pluginId 32310，Shadow 重定位)
- ✨ ASCII 启动横幅 (Yuncan 风格)
- 🔧 宽松默认配置 (leniency=10)，原版附魔位移不误判
- 🔧 移除免检窗口方案 (时序问题)，改为物理容差
- 🔧 默认关闭 flight/timer 检查，只防 XZ 轴水平高速移动
- 🐛 修复 ProtocolLib VEHICLE_MOVE 包字段读取越界 (FieldAccessException)

## 贡献指南

欢迎提交 Issue 和 Pull Request!

1. Fork 本仓库
2. 创建分支 (`git checkout -b feature/你的分支名字`)
3. 提交更改 (`git commit -m '你的分支名字'`)
4. 推送到分支 (`git push origin feature/你的分支名字`)
5. 创建 Pull Request

请确保代码风格一致，新功能有适当的配置项。

## 致谢

- **AlphaAlex115** — 原版 VelocityGuard 3.3 物理引擎与检测逻辑
- **dmulloy2** — ProtocolLib 跨版本数据包抽象
- **bStats** — 匿名统计服务

## 作者

- **OurcraftGuard**: [Yuncan](https://yuncan.xyz)

## 许可证

本项目基于 [GPL-3.0](https://www.gnu.org/licenses/gpl-3.0.html) 许可证开源。

```
OurcraftGuard — Ourcraft 服务器轻量反加速内核
Copyright (C) 2026 Yuncan

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.
```
