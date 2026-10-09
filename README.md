# 🔐 TreP — 原生 Android 密码管理器

> **The Password (Android)** · 基于 Jetpack Compose 与 Material Design 3 构建的本地密码管理器

TreP 是一个**完全离线**的 Android 密码管理器：密码库以 **PBKDF2 + AES-GCM** 加密后仅存储在本机，不联网、不上传、不依赖任何云端服务，并内置原生 **2FA (TOTP)** 动态验证码。

本项目由网页版 **TreP (The Password Web)** 重构移植而来，**加密数据格式与网页端保持一致**。

---

## ✨ 功能特性

### 🔒 安全与加密

| 能力 | 实现 |
| --- | --- |
| 密钥派生 | `PBKDF2WithHmacSHA256`，**200,000** 次迭代，随机 **16 字节** 盐，派生 **256 位** 密钥 |
| 数据加密 | `AES/GCM/NoPadding`，**128 位** 认证标签，每次写入生成新的 **12 字节** 随机 IV |
| 存储位置 | 应用私有 `SharedPreferences`，不写入外部存储 |
| 自动锁定 | 支持 **1 / 5 / 15 分钟** 无操作自动锁定（可关闭） |
| 主密码管理 | 支持开启、修改（重新派生密钥并整体重加密）、关闭（降级为明文存储） |
| 备份防护 | `allowBackup=false` + `backup_rules.xml` / `data_extraction_rules.xml` 排除云端备份与数据库导出 |
| 内存安全 | 锁定时立即清除派生密钥、盐与会话缓存；仅在解锁期间持有密钥 |

### ⏱️ 2FA (TOTP) 动态验证码

- 遵循 **RFC 6238 / RFC 4226**：`HMAC-SHA1` + 动态截断，30 秒步长，6 位验证码。
- 内置独立 **Base32** 解码器，写入密钥时即校验字符合法性并自动规范化（去空格/连字符、转大写）。
- 列表中实时刷新，配环形倒计时指示器，**剩余 ≤ 5 秒** 自动转为警示色。
- 动态验证码、账号、密码均可一键复制到剪贴板。

### 🎲 强密码生成器

- 基于 `SecureRandom`，默认生成 **16 位** 密码。
- 保证同时包含大写字母、小写字母、数字与特殊符号（`!@#$%^&*()_+[]{}<>?/|`），
  再经 **Fisher–Yates 洗牌** 打乱位置，避免字符类型可预测。

### 🎨 界面与交互

- **Material Design 3 (Material You)**：Android 12+ (API 31+) 自动提取壁纸配色（动态取色）；API 31 以下降级到内置浅蓝/浅粉配色。
- **深色模式**：完整支持 `values-night` 与系统深色主题，状态栏/导航栏图标随主题自适应。
- **国际化**：`zh-CN` **简体中文** 界面，支持 RTL，边到边 (edge-to-edge) 布局。
- **M3 组件**：`ElevatedCard`、`ModalBottomSheet`、`CenterAlignedTopAppBar`、`SuggestionChip`、`ExtendedFloatingActionButton`、`OutlinedTextField`、`SnackbarHost`、`AlertDialog`。
- **搜索**：按 **名称 / 账号 / 备注 / 2FA 密钥** 实时过滤。
- **密码显示**：逐条独立控制显隐，锁定后自动收回所有已显示密码。

### 🔑 账号条目

- 单个密码项可存 **名称**、**账号**、**密码**、**2FA 密钥**、**备注** 与创建/更新时间。
- `2FA 密钥（Base32）` 字段可用于只存验证码（例如托管账号的 TOTP）而无需保存账号密码。
- 表单校验：名称必填；账号与密码必须成对填写；至少填写「账号+密码」或「2FA 密钥」。

---

## 🏗️ 项目结构

```
TreP/
├── app/
│   ├── build.gradle.kts                  # 应用模块构建脚本
│   ├── proguard-rules.pro                # 混淆规则（保留 kotlinx.serialization 元数据）
│   └── src/main/
│       ├── AndroidManifest.xml           # 仅声明 USE_BIOMETRIC，无任何网络权限
│       ├── java/com/trep/passwordmanager/
│       │   ├── TrePApplication.kt         # Application，持有全局单例 Repository
│       │   ├── MainActivity.kt            # 入口 Activity + 页面切换与事件分发
│       │   ├── data/
│       │   │   ├── crypto/
│       │   │   │   ├── CryptoManager.kt      # PBKDF2 派生 + AES-GCM 加解密
│       │   │   │   ├── TotpGenerator.kt      # Base32 解码 + RFC 6238 TOTP
│       │   │   │   └── PasswordGenerator.kt  # SecureRandom 强密码生成
│       │   │   ├── model/
│       │   │   │   ├── PasswordItem.kt          # 密码项实体
│       │   │   │   ├── EncryptedVaultPayload.kt # 落盘密文结构 (salt/iv/data/updatedAt)
│       │   │   │   └── VaultMode.kt             # SECURE / PLAIN 存储模式
│       │   │   └── repository/
│       │   │       └── VaultRepository.kt    # 状态源 + 持久化 + 模式切换
│       │   └── ui/
│       │       ├── screens/       # AuthScreen / VaultScreen / SecuritySettingsScreen
│       │       ├── components/    # PasswordCard / PasswordItemDialog / TotpIndicator / CommonDialogs
│       │       ├── viewmodel/     # VaultViewModel + VaultUiState
│       │       └── theme/         # Color / Type / Theme（M3 配色与动态取色）
│       └── res/
│           ├── values/            # 配色、主题、字符串
│           ├── values-night/      # 深色主题
│           ├── drawable/          # 自适应图标前景 / 背景
│           ├── mipmap-anydpi-v26/ # 自适应图标
│           └── xml/               # 备份与数据提取规则
├── gradle/
│   ├── libs.versions.toml          # 版本目录（Version Catalog）
│   └── wrapper/
│       ├── gradle-wrapper.jar        # Wrapper 引导程序（必须提交）
│       └── gradle-wrapper.properties # 固定 Gradle 8.7
├── gradlew                         # 构建脚本（Linux / macOS）
├── gradlew.bat                     # 构建脚本（Windows）
├── build.gradle.kts                # 根构建脚本
├── settings.gradle.kts             # 模块声明与仓库配置
├── gradle.properties               # 全局 Gradle 参数
├── .gitignore                      # 忽略 build/、.gradle/、.idea/、local.properties 等
├── .gitattributes                  # 统一换行符（gradlew 固定 LF）
├── LICENSE                         # MIT 许可证
└── README.md                       # 本文档
```

> 仓库共 **21 个 Kotlin 源文件** 与 **10 个 XML**（含 `AndroidManifest.xml`），克隆后无需任何额外文件即可构建。

### 架构说明

采用 **MVVM + Repository** 分层：`VaultRepository` 是唯一的数据真相来源，通过
`StateFlow` 暴露 `isUnlocked` / `items` / `vaultMode` / `autoLockMinutes`；
`VaultViewModel` 订阅这些流并组合成单一 `VaultUiState`，同时负责输入校验、
TOTP 每秒刷新与自动锁定计时；Compose 界面只消费 `VaultUiState` 并回调事件，不直接接触加密逻辑。

---

## 🛠️ 技术栈

| 项目 | 版本 / 说明 |
| --- | --- |
| 语言 | Kotlin `2.0.20` |
| 构建工具 | Gradle `8.7` + Android Gradle Plugin `8.5.2` |
| JDK | **17**（`sourceCompatibility` / `jvmTarget` 均为 17） |
| UI | Jetpack Compose（BOM `2024.09.02`）+ Material 3 + `material-icons-extended` |
| 导航 | `androidx.navigation:navigation-compose` `2.8.1` |
| 异步 | Kotlin Coroutines + `StateFlow` |
| 序列化 | `kotlinx-serialization-json` `1.7.2` |
| 生物识别 | `androidx.biometric:biometric` `1.2.0-alpha05`（已预置，入口尚未实现） |
| 最低版本 | Android 8.0（API 26） |
| 编译 / 目标版本 | API 35（Android 15） |
| 应用版本 | `versionCode 1` / `versionName 1.0.0` |
| 应用 ID | `com.trep.passwordmanager` |
| 仓库规模 | 21 个 Kotlin 文件 + 10 个 XML + Gradle Wrapper |

---

## 🚀 构建与运行

### 环境要求

- **Android Studio**（Koala / Ladybug 或更高版本）
- **JDK 17 或 21** — 这是硬性要求。AGP 8.5.2 与 Gradle 8.7 **不支持 Java 25**，
  若你的环境只有更新的 JDK，请另外安装 JDK 17/21，并在
  *Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK* 中选择它。
- Android SDK：**API 35** 平台与对应 Build-Tools

### 方式一：Android Studio（推荐）

1. `File → Open`，选择本仓库根目录（包含 `settings.gradle.kts` 的那一层）。
2. 等待 Gradle Sync 完成（首次会自动下载 Gradle 8.7 发行版与依赖）。
3. 连接设备或启动模拟器，点击 **Run ▶**。

### 方式二：命令行（仓库已内置 Gradle Wrapper）

```bash
# macOS / Linux
./gradlew assembleDebug

# Windows
gradlew.bat assembleDebug
```

产物路径：`app/build/outputs/apk/debug/app-debug.apk`

> 若 `gradlew` 缺少可执行权限，执行：`chmod +x gradlew`。
> 打包 release 需要自行配置签名；`app/release/` 已被 `.gitignore` 忽略，不会误提交。

---

## ❓ 常见问题

| 现象 | 原因与处理 |
| --- | --- |
| 同步/构建报 `What went wrong: 25.0.3`，或提示不支持的 class 文件版本 | Gradle 使用的 JDK 是 **Java 25**。AGP 8.5.2 + Gradle 8.7 最高支持 Java 21，请把 Gradle JDK 切到 **17 或 21**（实测 Java 21 可正常构建）。 |
| 首次 Sync 很慢 | 需要联网下载 Gradle 8.7 发行版与全部依赖，属正常现象；之后走缓存。 |
| `./gradlew: Permission denied` | 执行 `chmod +x gradlew`（Windows 克隆不受影响）。 |
| 忘记主密码 | **无法恢复**。密钥只由主密码派生，请自行备份重要账号。 |
| 换设备后数据没了 | 这是设计使然：`allowBackup=false`，数据不参与云备份与迁移。 |

---

## 🔐 加密与数据格式

### 解锁 / 保存流程

```
主密码 ──PBKDF2-HMAC-SHA256(盐, 200000 次)──▶ 256 位 AES 密钥
                                                  │
密码项列表 ──kotlinx.serialization JSON──▶ AES-GCM(随机 IV, 128 位 Tag) ──▶ Base64 密文
```

### 落盘结构

加密模式下 `SharedPreferences`（`trep_vault_prefs`）中保存 `pm_secure_v1`：

```json
{
  "salt": "<16 字节随机盐, Base64>",
  "iv":   "<12 字节随机 IV, Base64>",
  "data": "<AES-GCM 密文(含认证标签), Base64>",
  "updatedAt": 1730000000000
}
```

- 每次保存都会**重新生成 IV**，因此相同明文两次写入产生不同密文。
- GCM 认证标签使密文具备**防篡改**能力：主密码错误或数据被改动都会在解密时失败，
  界面统一提示「解锁失败：主密码错误或数据损坏」。
- 明文模式（关闭主密码）下数据以 JSON 存放于 `pm_plain_v1`，仅适合明确接受该风险的场景。
- ⚠️ **主密码一旦遗忘无法恢复**，请自行妥善保管。

---

## 📖 使用指南

| 场景 | 操作 |
| --- | --- |
| 首次使用 | 输入并确认主密码（至少 8 位）→ 自动创建加密密码库 |
| 解锁 | 输入主密码，可勾选「本次会话记住」免去重复输入 |
| 添加条目 | 右下角 **添加密码** → 填写名称、（账号 + 密码）或 2FA 密钥、备注 |
| 生成密码 | 在编辑表单中点击 **随机密码 (16位)** 一键填充 |
| 查看 2FA | 填写 Base32 密钥后，列表中实时显示 6 位验证码与倒计时环 |
| 复制 | 账号 / 密码 / 动态验证码各自独立的一键复制按钮 |
| 安全设置 | 顶部状态标签进入：切换主密码开关、修改主密码、设置自动锁定、清空本机数据 |
| 锁定 | 顶部锁形按钮立即锁定，或等待自动锁定触发 |

---

## 🔒 隐私说明

- 本应用**不声明任何网络权限**，代码中没有网络请求，密码数据不会离开设备。
- 敏感数据存放于应用私有存储，且已排除系统云端备份与数据库导出。
- 唯一声明的权限是 `USE_BIOMETRIC`。

> **实现状态提醒**：`androidx.biometric` 依赖与 `USE_BIOMETRIC` 权限已在工程中预置，
> 但**指纹/面容解锁入口尚未实现**，当前版本请使用主密码解锁。

---

## 🤝 贡献

欢迎提交 Issue 与 Pull Request。参与开发时请遵守以下约定：

1. 保持现有分层：加密相关改动必须落在 `data/crypto/`，不要在 Composable 中直接处理密钥。
2. 若修改加密参数或数据格式，请同步更新本 README 并说明**与网页端的兼容性影响**。
3. 提交前请确保 `gradlew assembleDebug` 可通过。
4. 不要提交 `local.properties`、`build/`、`.idea/` 与任何签名文件（`.gitignore` 已默认忽略）。

---

## 📄 许可证

本项目基于 [MIT License](LICENSE) 开源。

## ⚠️ 免责声明

本软件按「原样」提供，不附带任何明示或暗示的担保。请在使用前自行评估其安全性，
并始终为重要账号保留独立的恢复手段。
