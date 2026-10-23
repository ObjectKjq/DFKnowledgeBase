# 文档入库与扫描 PDF OCR 部署

## 数据库升级

先备份业务库，再依序执行已有增量脚本及 `sql/upgrade/014-ingest-position.sql`。全新安装执行 `sql/schema.sql`。脚本保留原文档和分块，按原创建时间回填提交排序；不会删除已有数据。新增位置只对重新入库的文档生成，存量分块的工作表为空且 OCR 来源为 false。

MySQL 文档表的排队、解析中、索引中状态就是持久任务状态，`ingest_submit_time` 保存本次提交次序；重试和恢复重新提交，服务重启不改次序。不需要 MQ。单实例全局默认并发 2、同一知识库默认并发 1。调度只领取可执行任务，线程池无积压队列且使用拒绝策略；饱和时待处理任务留在 MySQL，下次调度再领取，HTTP 上传线程不执行解析。服务重启会继续未完成任务。

## 本地 OCR 引擎

部署运行后端的机器须安装 Tesseract 5 及 `chi_sim`、`eng` 语言包。Linux Debian/Ubuntu 示例：

```sh
sudo apt-get update
sudo apt-get install tesseract-ocr tesseract-ocr-chi-sim tesseract-ocr-eng
tesseract --list-langs
```

Windows 按 [Tesseract 官方安装说明](https://tesseract-ocr.github.io/tessdoc/Installation.html) 安装发行版，将 `chi_sim.traineddata`、`eng.traineddata` 放进安装目录的 `tessdata`，确保后端服务账户可执行二进制并读语言包。不要仅在交互用户 PATH 配好而遗漏服务账户。用 `tesseract --list-langs` 确认两个语言均存在。

在 `application-local.yaml` 或部署环境中配置：

```yaml
knowledge:
  ocr:
    binary: 'C:/Program Files/Tesseract-OCR/tesseract.exe' # Linux 可用 /usr/bin/tesseract
    languages: chi_sim+eng
    timeout-seconds: 120
  ingest:
    poll-delay-ms: 500
```

参数逐个传给标准进程，不经 shell。默认 binary 为 `tesseract`。每次识别使用独立系统临时目录，需服务账户具备临时目录读写权限；识别完成、失败或中断后清理。OCR 原文不会发送到远端。二进制缺失、语言包缺失、进程错误、超时、非空图像无法提取文字均使整份文档失败，原因包含物理页码，保留首次失败原因。接口不回显进程命令、临时目录或 stderr。

PDFBox 按物理页直接抽取文字；无文字且有可见内容的页以 200 DPI 渲染再交给 OCR，真正空白页保留页码且不产生分块。混合 PDF 逐页选择。全部页成功后同事务写入分块，再索引；任何页失败均不发布部分分块。PPT 使用幻灯片编号，Excel 使用工作表名称；Office 表格保留表头及列归属，不承诺还原原始排版。

组件依据：[Tesseract 标准命令行](https://tesseract-ocr.github.io/tessdoc/Command-Line-Usage.html)、[PDFBox 官方渲染实现](https://github.com/apache/pdfbox/blob/trunk/pdfbox/src/main/java/org/apache/pdfbox/rendering/PDFRenderer.java)。OCR/工作表元数据由 MySQL 权威分块提供，ES 写入字段集合不变，因此本次无需修改既有 strict mapping。

## Windows 使用现有 Docker 的本机桥接方案

不想全局安装 Tesseract 的 Windows 部署，可在仓库根运行：

```powershell
./scripts/ocr/build-windows.ps1
$env:KNOWLEDGE_OCR_BINARY = (Resolve-Path target/ocr/tesseract-docker.exe).Path
```

脚本按仓库 `scripts/ocr/Dockerfile` 构建 `dfknowledge-tesseract:5`，使用 Ubuntu 官方包安装真实中英 Tesseract，再通过系统自带 .NET Framework C# 编译器生成局部可执行桥接程序。也可在镜像已构建时使用 `-SkipImageBuild` 仅编译。构建需要网络可达软件源，运行识别时容器使用 `--network none`；只挂载本次随机临时目录，不挂载业务库或整个仓库，不修改系统 PATH。后端服务账户必须可访问 Docker Desktop。每个容器内部最多执行 110 秒，后端 `timeout-seconds` 应至少 120 秒，以便正常清理临时目录。输出仍由后端读取真实 OCR 结果，未用固定文字替代。

可使用同一进程适配器验证完整 HTTP 旅程：

```powershell
$env:P0_OCR_BINARY = (Resolve-Path target/ocr/tesseract-docker.exe).Path
mvn '-Dtest=DocIngestJourneyTest' test
mvn '-Dtest=DocIngestMysqlJourneyTest' '-Dp0.mysql.enabled=true' test
```

真实 MySQL 旅程默认跳过，显式开关后数据库不可用会报失败。测试只重建固定的 `df_knowledge_base_p0_21_test`；默认连接本机 3306，测试用户/密码从 `P0_MYSQL_USER`、`P0_MYSQL_PASSWORD` 读取（缺省 root/root），不访问业务库。常规替身测试与真实依赖结果应分别统计。

## 验收限制

只有设置真实 `P0_OCR_BINARY` 并执行中文扫描 HTTP 旅程通过，才能确认该进程桥接方案可用；默认跳过不能计为验收通过。部署后必须用实际后端服务账户完成中文扫描 PDF、混合 PDF、缺失语言包及超时失败的实际旅程；再验证正文检索和引用预览。自动测试或替身结果不能替代该检查。
