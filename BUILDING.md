# 构建 Blessing of Pandora 1.7.3fix

本源码面向 Minecraft 1.20.1 / Forge 47.4.10。请安装 **JDK 17**，使用仓库自带的 Gradle Wrapper（**Gradle 8.8**），不需要另外安装 Gradle。

## 准备本地编译依赖

从各模组的官方发布页面取得以下版本，将 JAR 放在项目根目录，也就是 `build.gradle` 同级目录。文件名必须与下表完全相同，包括中文前缀、方括号和空格。

| 模组 | 根目录文件名 |
| --- | --- |
| 潘多拉之咒 2.4.26 | `curseofpandora-2.4.26.jar` |
| L2 Library 2.5.3 | `l2library-2.5.3.jar` |
| 莱特兰扩充 / L2 Complements 2.6.1 | `[莱特兰-扩充] l2complements-2.6.1.jar` |
| 车万女仆 / Touhou Little Maid 1.5.3，Forge 1.20.1 | `[车万女仆] touhoulittlemaid-1.5.3-forge+mc1.20.1.jar` |

这些第三方 JAR 不随源码仓库发布，也不会打入本模组的成品。构建脚本会从它们提取或复制所需的编译 API 到 `build/`；请勿修改原 JAR 内容。

车万女仆在**编译时需要提供 API**，但在玩家游戏中仍是可选联动模组；没有安装时不会注册两颗联动宝珠。游戏环境的必需和可选前置见 [README](README.md)。

首次构建需要联网下载 Gradle、Forge、Minecraft 开发依赖、Curios、JEI 和其他 Maven 依赖。仅在依赖已完整缓存后才适合使用 `--offline`。

## 构建与检查

先确认 `java -version` 使用 JDK 17，并让 `JAVA_HOME` 指向该 JDK。Windows PowerShell：

```powershell
.\gradlew.bat build --no-daemon
```

Linux / macOS：

```sh
sh ./gradlew build --no-daemon
```

`build` 会编译源码、生成资源、运行现有核心回归检查，并完成 Forge 重混淆。成功后成品位于：

```text
build/libs/blessingofpandora-1.7.3fix.jar
```

`src/test` 中的回归检查随源码提供。开发环境的独立 GameTest、客户端截图探针和本地测试日志未包含在本次源码发布中；普通 `build` 不依赖它们。

## 许可证

项目主要源码使用根目录 [MIT License](LICENSE)。粉色流动名称的指定改编文件使用 MPL-2.0，其来源、范围和许可全文位于 `src/main/resources/META-INF/third-party/tlt-love-name/`，构建时会随成品包含对应源码和声明。
