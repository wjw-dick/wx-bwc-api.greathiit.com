# 一键更新部署：补修复 -> 后端重打重启 -> 后台重 build 发布 -> 小程序上传
# 用法：
#   .\tools\deploy.ps1                          # 全流程
#   .\tools\deploy.ps1 -SkipMini                # 不动小程序
#   .\tools\deploy.ps1 -MiniVersion 1.3.0       # 指定版本号
param(
    [switch]$SkipBackend,
    [switch]$SkipAdmin,
    [switch]$SkipMini,
    [string]$MiniVersion = 'AUTO'
)

$ErrorActionPreference = 'Stop'
$root = 'D:\ai\wx-bwc-api.greathiit.com'
$appDir = 'D:\app\bwc'
$buildTmp = Join-Path $appDir 'build-tmp'
$elevated = Join-Path $appDir 'svc-elevated.ps1'
$powershell = 'C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe'
$videoLog = Join-Path $appDir 'logs\elevated.log'

function Step([string]$msg) { Write-Output ''; Write-Output ('=== ' + $msg + ' ===') }

function Wait-Health([int]$timeoutSec = 120) {
    $deadline = (Get-Date).AddSeconds($timeoutSec)
    while ((Get-Date) -lt $deadline) {
        try {
            $r = Invoke-WebRequest -Uri 'http://127.0.0.1:18090/api/public/health' -TimeoutSec 4 -UseBasicParsing
            if ($r.StatusCode -eq 200) { return $true }
        } catch { }
        Start-Sleep -Seconds 3
    }
    return $false
}

function Invoke-Elevated([string]$cmd) {
    if (Test-Path -LiteralPath $videoLog) { Remove-Item -LiteralPath $videoLog -Force -ErrorAction SilentlyContinue }
    Start-Process -FilePath $powershell -ArgumentList '-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', $elevated, $cmd -Verb RunAs
    for ($i = 0; $i -lt 20; $i++) {
        Start-Sleep -Seconds 2
        if ((Test-Path -LiteralPath $videoLog) -and ((Get-Content -LiteralPath $videoLog -Raw -Encoding utf8) -match 'done')) { break }
    }
    if (Test-Path -LiteralPath $videoLog) { Get-Content -LiteralPath $videoLog -Encoding utf8 | ForEach-Object { Write-Output ('  ' + $_) } }
}

# ---------- 0. 补回被覆盖的修复 ----------
Step '0/4 检查并补回历史修复'
& (Join-Path $root 'tools\fix-regressions.ps1')

# ---------- 1. 后端 ----------
if (-not $SkipBackend) {
    Step '1/4 后端编译'
    if (Test-Path -LiteralPath $buildTmp) { Remove-Item -LiteralPath $buildTmp -Recurse -Force }
    New-Item -ItemType Directory -Path $buildTmp -Force | Out-Null
    Copy-Item -LiteralPath (Join-Path $root 'backend\src') -Destination (Join-Path $buildTmp 'src') -Recurse -Force
    Copy-Item -LiteralPath (Join-Path $root 'backend\pom.xml') -Destination (Join-Path $buildTmp 'pom.xml') -Force

    $env:JAVA_HOME = 'D:\BtSoft\jdk\jdk17'
    & 'D:\soft\apache-maven-3.8.8\bin\mvn.cmd' -f (Join-Path $buildTmp 'pom.xml') clean package -DskipTests -q 2>&1 |
        Select-Object -Last 20 | ForEach-Object { Write-Output ('  ' + $_.ToString()) }
    if ($LASTEXITCODE -ne 0) { throw '后端编译失败，已中止（线上未改动）' }

    $newJar = Join-Path $buildTmp 'target\bwc-api.jar'
    if (-not (Test-Path -LiteralPath $newJar)) { throw '未生成 jar，已中止' }
    Write-Output ('  jar: ' + (Get-Item -LiteralPath $newJar).Length + ' 字节')

    Step '1/4 停服换包并重启'
    Invoke-Elevated 'app-stop'
    Copy-Item -LiteralPath (Join-Path $appDir 'app.jar') -Destination (Join-Path $appDir ('app.jar.bak-' + (Get-Date -Format 'yyyyMMdd-HHmm'))) -Force
    Copy-Item -LiteralPath $newJar -Destination (Join-Path $appDir 'app.jar') -Force
    Invoke-Elevated 'app-start'

    if (-not (Wait-Health 180)) { throw '后端启动后健康检查未通过，请查 logs\app.out.log' }
    Write-Output ('  health 200, pid=' + (Get-Content -LiteralPath (Join-Path $appDir 'app.pid') -Raw).Trim())

    # 校验关键修复是否真的进包
    $clsTmp = Join-Path $appDir 'checkcls'
    if (Test-Path -LiteralPath $clsTmp) { Remove-Item -LiteralPath $clsTmp -Recurse -Force }
    New-Item -ItemType Directory -Path $clsTmp -Force | Out-Null
    Push-Location -LiteralPath $clsTmp
    & 'D:\BtSoft\jdk\jdk17\bin\jar.exe' xf (Join-Path $appDir 'app.jar') 'BOOT-INF/classes/com/greathiit/bwc/module/system/controller/PublicController.class'
    $p = Join-Path $clsTmp 'BOOT-INF\classes\com\greathiit\bwc\module\system\controller\PublicController.class'
    $hasFix = [Text.Encoding]::UTF8.GetString([IO.File]::ReadAllBytes($p)) -match 'blankToNull'
    Pop-Location
    if ($hasFix) { Write-Output '  校验: jar 内 blankToNull = True' } else { Write-Output '  [警告] jar 内未发现 blankToNull，失物墙可能又空' }
}

# ---------- 2. 后台 ----------
if (-not $SkipAdmin) {
    Step '2/4 后台 admin 构建'
    Push-Location -LiteralPath (Join-Path $root 'admin-web')
    & npm run build 2>&1 | Select-Object -Last 12 | ForEach-Object { Write-Output ('  ' + $_.ToString()) }
    $npmExit = $LASTEXITCODE
    Pop-Location
    if ($npmExit -ne 0) { throw 'admin 构建失败，站点未改动' }

    $dist = Join-Path $root 'admin-web\dist'
    $site = 'D:\wwwroot\bwc-admin.wangjiuwen.store'
    $stamp = Get-Date -Format 'yyyyMMdd-HHmm'
    $bakDir = Join-Path $appDir 'bak'
    if (-not (Test-Path -LiteralPath $bakDir)) { New-Item -ItemType Directory -Path $bakDir -Force | Out-Null }
    if (Test-Path -LiteralPath (Join-Path $site 'assets')) { Copy-Item -LiteralPath (Join-Path $site 'assets') -Destination (Join-Path $bakDir ('admin-assets-' + $stamp)) -Recurse -Force }
    if (Test-Path -LiteralPath (Join-Path $site 'index.html')) { Copy-Item -LiteralPath (Join-Path $site 'index.html') -Destination (Join-Path $bakDir ('admin-index-' + $stamp + '.html')) -Force }
    Remove-Item -LiteralPath (Join-Path $site 'assets') -Recurse -Force -ErrorAction SilentlyContinue
    Copy-Item -LiteralPath (Join-Path $dist 'assets') -Destination (Join-Path $site 'assets') -Recurse -Force
    Copy-Item -LiteralPath (Join-Path $dist 'index.html') -Destination (Join-Path $site 'index.html') -Force
    Write-Output '  站点已更新: D:\wwwroot\bwc-admin.wangjiuwen.store'
}

# ---------- 3. 小程序 ----------
if (-not $SkipMini) {
    Step '3/4 小程序上传'
    if ($MiniVersion -eq 'AUTO') {
        $verFile = Join-Path $appDir 'last-mini-version.txt'
        $last = '1.2.6'
        if (Test-Path -LiteralPath $verFile) { $last = (Get-Content -LiteralPath $verFile -Raw).Trim() }
        $parts = $last.Split('.')
        if ($parts.Count -eq 3) { $MiniVersion = $parts[0] + '.' + $parts[1] + '.' + ([int]$parts[2] + 1) }
        else { $MiniVersion = $last }
    }
    & 'D:\soft\微信web开发者工具\cli.bat' upload --project (Join-Path $root 'miniprogram') -v $MiniVersion -d '同步本地最新代码' 2>&1 |
        Select-Object -Last 8 | ForEach-Object { Write-Output ('  ' + $_.ToString().Trim()) }
    if ($LASTEXITCODE -eq 0) {
        [IO.File]::WriteAllText((Join-Path $appDir 'last-mini-version.txt'), $MiniVersion, (New-Object Text.UTF8Encoding($false)))
        Write-Output ('  已上传版本: ' + $MiniVersion + '（体验版会自动指向最新开发版）')
    } else {
        Write-Output '  [警告] 小程序上传失败，看上面输出'
    }
}

# ---------- 4. 收尾自检 ----------
Step '4/4 线上自检'
try {
    $lost = Invoke-WebRequest -Uri 'https://bwc-api.wangjiuwen.store/api/public/tenant/DEMO/lost?category=undefined&keyword=undefined' -TimeoutSec 15 -UseBasicParsing
    Write-Output ('  失物接口(带undefined): ' + $lost.StatusCode + ' / ' + $lost.Content.Length + ' 字节')
} catch {
    Write-Output ('  [警告] 失物接口异常: ' + $_.Exception.Message)
}
foreach ($u in @('https://bwc-admin.wangjiuwen.store/', 'https://bwc.wangjiuwen.store/', 'https://portal-web.wangjiuwen.store/h5/')) {
    try {
        $r = Invoke-WebRequest -Uri $u -TimeoutSec 12 -UseBasicParsing
        Write-Output ('  ' + $u + ' => ' + $r.StatusCode)
    } catch {
        Write-Output ('  ' + $u + ' => 异常')
    }
}
Write-Output ''
Write-Output '完成。'
