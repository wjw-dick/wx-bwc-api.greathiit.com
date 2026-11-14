# 修复回归：把「失物墙 undefined 参数」相关修复重新打回源码
# 幂等：已经存在则跳过，可反复执行
param(
    [switch]$Quiet
)

$ErrorActionPreference = 'Stop'
$root = 'D:\ai\wx-bwc-api.greathiit.com'
$backendFile = Join-Path $root 'backend\src\main\java\com\greathiit\bwc\module\system\controller\PublicController.java'
$miniFile = Join-Path $root 'miniprogram\pages\lost\index.js'
$changed = New-Object System.Collections.Generic.List[string]

function Save-Text([string]$path, [string]$text) {
    [IO.File]::WriteAllText($path, $text, (New-Object Text.UTF8Encoding($false)))
}

function Say([string]$msg) {
    if (-not $Quiet) { Write-Output $msg }
}

# ---------- 1. 后端 PublicController ----------
if (-not (Test-Path -LiteralPath $backendFile)) {
    throw "找不到后端文件: $backendFile"
}
$be = Get-Content -LiteralPath $backendFile -Raw -Encoding utf8

if ($be -notmatch 'blankToNull') {
    # 在 lost() 方法体开头插入参数归一化
    $be = [regex]::Replace($be,
        '(@GetMapping\("/tenant/\{code\}/lost"\)[\s\S]*?\)\s*\{)(\s*return\s+withTenant\()',
        {
            param($m)
            $m.Groups[1].Value + "`r`n        category = blankToNull(category);`r`n        keyword = blankToNull(keyword);`r`n" + $m.Groups[2].Value.TrimStart()
        })

    # 在 next 方法前插入 helper
    $helper = "    private static String blankToNull(String value) {`r`n" +
              "        if (value == null) {`r`n" +
              "            return null;`r`n" +
              "        }`r`n" +
              "        String trimmed = value.trim();`r`n" +
              "        if (trimmed.isEmpty()`r`n" +
              "                || `"undefined`".equalsIgnoreCase(trimmed)`r`n" +
              "                || `"null`".equalsIgnoreCase(trimmed)) {`r`n" +
              "            return null;`r`n" +
              "        }`r`n" +
              "        return trimmed;`r`n" +
              "    }`r`n`r`n"
    $be = [regex]::Replace($be,
        '(?m)(\s*@GetMapping\("/tenant/\{code\}/lost-notices"\))',
        { param($m) $helper + $m.Groups[1].Value.TrimStart() })

    if ($be -notmatch 'blankToNull') { throw '后端补丁插入失败：未找到插入锚点' }
    Save-Text $backendFile $be
    $changed.Add('后端 PublicController：已补 blankToNull 参数归一化')
    Say '[已修复] 后端 PublicController'
} else {
    Say '[跳过]   后端 PublicController 修复已在'
}

# ---------- 2. 小程序 pages/lost/index.js ----------
if (-not (Test-Path -LiteralPath $miniFile)) {
    throw "找不到小程序文件: $miniFile"
}
$mi = Get-Content -LiteralPath $miniFile -Raw -Encoding utf8

if ($mi -match 'category:\s*this\.data\.category\s*\|\|\s*undefined') {
    # params 对象改为按需携带
    $mi = [regex]::Replace($mi,
        'params:\s*\{\s*category:\s*this\.data\.category\s*\|\|\s*undefined,\s*keyword:\s*this\.data\.keyword\s*\|\|\s*undefined\s*\}',
        'params')

    $idx = $mi.IndexOf('const rows = await request({')
    if ($idx -lt 0) { throw '小程序补丁插入失败：未找到 request 调用' }

    $insert = "      const params = {}`r`n" +
              "      if (this.data.category) params.category = this.data.category`r`n" +
              "      if (this.data.keyword) params.keyword = this.data.keyword`r`n"
    $mi = $mi.Insert($idx, $insert)

    if ($mi -notmatch 'const params = \{\}') { throw '小程序补丁插入失败' }
    Save-Text $miniFile $mi
    $changed.Add('小程序 lost/index.js：参数改为有值才带')
    Say '[已修复] 小程序 pages/lost/index.js'
} else {
    Say '[跳过]   小程序 pages/lost/index.js 修复已在'
}

Say ''
if ($changed.Count -eq 0) {
    Say '结论：本次无需改动，两处修复都在。'
} else {
    Say ('结论：补回 ' + $changed.Count + ' 处修复')
    $changed | ForEach-Object { Say ("  - " + $_) }
}
