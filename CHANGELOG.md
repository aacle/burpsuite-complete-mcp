# Changelog

## [2.1.0] - 2026-08-21

### Added
- **Intruder staging + custom payloads** — `create_intruder_attack` (payload positions via `HttpRequestTemplate`), `set_intruder_payloads`, and a registered "MCP Payloads" `PayloadGeneratorProvider`.

## [2.0.0] - 2026-08-21

A GPLv3 fork of [`portswigger/mcp-server`](https://github.com/portswigger/mcp-server), upgraded and extended.

### Added
- **Repeater read access** — `list_repeater_tabs`, `read_repeater_tab_request`, `read_repeater_tab_response`, `send_repeater_request`.
  Backed by an exchange shadow store (`registerHttpHandler`) and a Swing UI bridge, since the Montoya API has no Repeater read access.
- **Intruder read + staging** — `create_intruder_attack` (payload positions + custom payloads), `set_intruder_payloads`, and `get_intruder_attack_results` (reads shadow-store INTRUDER traffic). Custom payloads are served by a registered "MCP Payloads" `PayloadGeneratorProvider`.
- **Scanner control** — `start_crawl`, `start_audit`, `get_scan_status`, `stop_scan` (Professional only).
- **`get_proxy_http_history_by_id`** — address history by Burp request id, not just offset.
- **One-click installers** for OpenCode, Copilot CLI, and Codex CLI (in addition to Claude Desktop).
- **Configurable history truncation** (`historyItemMaxChars`, `historyFieldMaxChars`) with per-field truncation so responses are no longer dropped.

### Fixed
- `tabName` nullable-in-schema but required-at-runtime bug (#109/#110).
- Integer parameters arriving as `10.0` failing to parse (#28).
- Undeclared client arguments rejected (#116).
- History items truncated at 5000 chars with the response often lost (#100/#112).
- History only addressable by offset (#111/#30).

### Changed
- Rebranded to **Burp Suite Complete MCP** (`com.aacle`, v2.0.0).
- Montoya API bumped `2025.10` → `2026.7`.

## [1.3.0] - upstream

Initial fork point (PortSwigger's `mcp-server`).