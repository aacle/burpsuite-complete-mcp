# Upstream bug fixes applied

Mapping of confirmed upstream `portswigger/mcp-server` issues to fixes in this fork.

## Phase 0 (shipped)

| Upstream issue | Description | Fix |
|----------------|-------------|-----|
| #109 / #110 | `tabName` declared nullable in JSON schema but required at runtime → clients that omit it fail | `tabName` fields given `= null` defaults; tool lambdas branch to Montoya's no-name overloads when absent (`Tools.kt`) |
| #28 | Integer params arrive as `10.0` and fail to parse | Lenient JSON decoding (`coerceInputValues = true`) in `McpTool.kt` |
| #116 (partial) | Undeclared client args rejected | `ignoreUnknownKeys = true` in `McpTool.kt` |
| — | Repo pinned to stale Montoya API 2025.10 | Bumped to `2026.7` in `gradle/libs.versions.toml` |

## Planned (later phases)

| Upstream issue | Description | Planned fix |
|----------------|-------------|-------------|
| #100 / #112 | History items truncated at 5000 chars; responses dropped | Configurable budgets + per-field windowing |
| #117 / #90 | MCP-sent requests invisible in history/sitemap | `RequestExecutionEngine` + sitemap add |
| #37 | `get_scanner_issues` returns only 2 recurring issues | Correct issue-stream handling |
| #113 | Extension must be reinstalled after every Burp restart | Fix unload/persistence lifecycle |
| #43 | No MCP `logging/setLevel` | Implement logging capability |
| #42 / #15 / #72 | SSE 404/protocol errors; proxy drops `initialize` | Streamable HTTP transport + proxy handshake fix |
| #84 / #85 / #86 | Only Claude Desktop installer | Add OpenCode/Copilot CLI/Codex/VS Code/Cline installers |
| #63 / #50 | No scan/crawl/audit control | Expose `Scanner` crawl/audit + status |
| #18 / #31 / #30 | No scope/notes/by-id history | Scope, notes, `by_id` history tools |