param([switch]$SkipImageBuild)
$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$taskOutput = Join-Path $taskRoot 'target/ocr'
New-Item -ItemType Directory -Force -Path $taskOutput | Out-Null
if (-not $SkipImageBuild) {
    # 只导出系统已信任的公开根证书；以构建 secret 使用，不写入镜像。
    $taskCa = Join-Path $taskOutput 'trusted-root.pem'
    Get-ChildItem Cert:/LocalMachine/Root,Cert:/CurrentUser/Root | ForEach-Object {
        '-----BEGIN CERTIFICATE-----' + [Environment]::NewLine +
        [Convert]::ToBase64String($_.RawData, [Base64FormattingOptions]::InsertLineBreaks) +
        [Environment]::NewLine + '-----END CERTIFICATE-----'
    } | Set-Content -LiteralPath $taskCa -Encoding ascii
    docker build --secret "id=apt-ca,src=$taskCa" -t dfknowledge-tesseract:5 $PSScriptRoot
    if ($LASTEXITCODE -ne 0) { throw 'OCR 镜像构建失败' }
}
$taskCompiler = Join-Path $env:WINDIR 'Microsoft.NET/Framework64/v4.0.30319/csc.exe'
if (-not (Test-Path -LiteralPath $taskCompiler)) { throw '未找到 Windows .NET Framework C# 编译器，请安装本地 Tesseract 或 .NET Framework' }
$taskAssembly = Join-Path $taskOutput 'tesseract-docker.exe'
& $taskCompiler /nologo /target:exe "/out:$taskAssembly" (Join-Path $PSScriptRoot 'TesseractDockerBridge.cs')
if ($LASTEXITCODE -ne 0) { throw 'OCR 桥接程序编译失败' }
Write-Output (Join-Path $taskOutput 'tesseract-docker.exe')
