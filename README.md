<div align="center">

# 网安日报速递

> 每天一份网络安全简报，覆盖 CVE、漏洞通报、安全事件、威胁情报。AI 生成，人工校验，自动发布。

[![GitHub Pages](https://img.shields.io/badge/Deploy-GitHub_Pages-blue)](https://unclecheng-li.github.io/cybersecurity-daily/)
[![Status](https://img.shields.io/badge/Status-Daily-green)](#运行方式)
[![Auto](https://img.shields.io/badge/Automation-WorkBuddy-orange)](#技术栈)

**在线阅读**：https://unclecheng-li.github.io/cybersecurity-daily/

</div>

---

## 这是什么

一个自动化网安日报项目。每天中午自动抓取当天的网络安全动态，筛选出 5-10 条值得看的内容，生成 Markdown 报告和 HTML 页面，推送到 GitHub Pages。

不是新闻聚合器，不是 RSS 转发。每条内容都经过筛选、整理、加了解读和出处链接。

---

## 为什么要搞这个

安全圈的信息密度很高，但噪音也大。CVE 每天几十个，真正需要你动手的可能就两三个。安全事件铺天盖地，但大部分看完就忘。

这个项目想做的事情很简单，帮你过滤一遍，把当天真正值得关注的东西挑出来，附上为什么值得关注，让你花 5 分钟就能跟上当天的安全态势。

---

## 内容长什么样

每期日报包含：

| 板块 | 说明 |
|------|------|
| 头条 | 当天最值得关注的事件，通常带攻击链分析 |
| 重点分析 | 3-6 条深度内容，包含 CVE 编号、CVSS 评分、漏洞机制、利用方式 |
| 速览区 | 2-4 条一句话概括 |
| 可视化 | Mermaid 图表（象限图、流程图、时序图、饼图等，按内容选配） |
| 出处链接 | 每条内容的原始来源，方便你进一步追踪 |

HTML 页面用了报纸风格的排版，暗色背景 + 暖色调，支持移动端和打印。

---

## 运行方式

日报由 WorkBuddy 自动化工作流驱动，每天中午 12:00 自动执行：

1. 搜索当天网络安全新闻（CVE、漏洞通报、安全事件、APT 活动等）
2. 筛选 5-10 条最有价值的内容
3. 生成 Markdown 报告（含 Mermaid 图表）
4. 基于 HTML 模板生成日报页面
5. 更新首页存档列表
6. 推送到 GitHub 仓库，Pages 自动更新

---

## 技术栈

- **内容生成**：WorkBuddy 自动化 + AI 模型（信息检索、筛选、摘要）
- **页面**：纯静态 HTML + CSS，无框架依赖
- **图表**：Mermaid.js（象限图、流程图、时序图、饼图、甘特图）
- **部署**：GitHub Pages，推送到 `main` 分支即发布
- **自动化**：WorkBuddy 定时任务（`rrule: FREQ=DAILY;BYHOUR=12`）

---

## 项目结构

```text
cybersecurity-daily/
├── index.html              # 首页（存档列表，数据内联）
├── assets/
│   └── style.css           # 公共样式（报纸风格）
├── daily/
│   ├── 2026-06-05.html     # 日报页面（按日期命名）
│   ├── 2026-06-04.html
│   └── ...
└── deploy.sh               # 部署脚本（复制HTML + 更新首页 + git push）
```

---

## 本地预览

```bash
git clone https://github.com/Unclecheng-li/cybersecurity-daily.git
cd cybersecurity-daily

# 直接用浏览器打开
open index.html        # macOS
start index.html       # Windows
```

或者起个本地服务器：

```bash
python -m http.server 8080
# 访问 http://localhost:8080
```

---

## 许可证

本项目使用 [MIT License](LICENSE)。

---

<div align="center">

**每天 5 分钟，跟上安全态势。**

</div>
