# Upstream bug fixes applied

Mapping of confirmed upstream `portswigger/mcp-server` issues to fixes in this fork.

## Phase 0 (shipped)

| Upstream issue | Description | Fix |
|----------------|-------------|-----|
| #109 / #110 | `tabName` declared nullable in JSON schema but required at runtime → clients that omit it fail | `tabName` fields given `= null` defaults; tool lambdas branch to Montoya's no-name overloads when absent (`Tools.kt`) |
| #28 | Integer params arrive as `10.0` and fail to parse | Lenient JSON decoding (`coerceInputValues = true`) in `McpTool.kt` |
| #116 (partial) | Undeclared client args rejected | `ignoreUnknownKeys = true` in `McpTool.kt` |
| — | Repo pinned to stale Montoya API 2025.10 | Bumped to `2026.7` in `gradle/libs.versions.toml` |

## P2-P4 (shipped)

| Upstream issue | Description | Fix |
|----------------|-------------|-----|
| #100 / #112 | History items truncated at 5000 chars; responses dropped | Configurable `historyItemMaxChars`/`historyFieldMaxChars` + per-field truncation |
| #111 / #30 | History only addressable by offset | `get_proxy_http_history_by_id` + request `id` in history items |
| #63 / #50 | No scan/crawl/audit control | `start_crawl` / `start_audit` / `get_scan_status` / `stop_scan` |
| #92 / #48 | Intruder payload stubs ignored | `create_intruder_attack` preserves payload positions via `HttpRequestTemplate` |
| #84 / #85 / #86 | Only Claude Desktop installer | Added OpenCode, Copilot CLI, Codex CLI installers |

## Planned (later phases)

| Upstream issue | Description | Planned fix |
|----------------|-------------|-------------|
| #117 / #90 | MCP-sent requests invisible in history/sitemap | `RequestExecutionEngine` + sitemap add |
| #37 | `get_scanner_issues` returns only 2 recurring issues | Correct issue-stream handling |
| #113 | Extension must be reinstalled after every Burp restart | Fix unload/persistence lifecycle |
| #43 | No MCP `logging/setLevel` | Implement logging capability |
| #42 / #15 / #72 | SSE 404/protocol errors; proxy drops `initialize` | Streamable HTTP transport + proxy handshake fix |
| #18 / #31 | No scope/notes tools | Scope, notes tools |