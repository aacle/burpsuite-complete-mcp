# Changelog

## [2.3.1] - 2026-08-21

Expert-review fixes:

### Fixed
- **Shadow store memory/perf**: captured request/response bodies are now truncated at 8k chars each; orphaned pending entries (responses that never arrive) are evicted after 5 minutes; new `listMetadata()` lets the Traffic tab poll without copying bodies.
- **`get_cookies` domain filter was advertised but never implemented** — now filters by exact domain.
- **Scan tasks**: `ScanTaskRegistry.list()` no longer throws every 2s on stopped/deleted tasks; new safe `stop()` shared by the tool and the Scans tab Stop button.

### Security
- **Active scans now require host approval** — `start_crawl`/`start_audit` pass every seed URL through the same approval gate as request sends (previously they scanned any host unapproved), and all seed URLs are validated before anything starts.
- **Cookie jar reads are gated** behind a new Cookie-jar data-access approval (with its own "Always allow" toggle in the MCP tab), matching history/organizer gating.

## [2.3.0] - 2026-08-21

### Fixed
- **Repeater read-back is now reliable.** Replaced the non-functional Swing UI bridge with a `RepeaterTabStore` that records every tab the MCP stages (request + response). `list_repeater_tabs` / `read_repeater_tab_request` / `read_repeater_tab_response` now return the exact per-tab content instead of falling back to the last exchange. `create_repeater_tab` now returns the tab name it used.

## [2.2.0] - 2026-08-21

### Added
- **MCP tab control room** — the MCP tab is now a tabbed dashboard, not just config:
  - **Dashboard** — server status, host:port, edition, tool count, traffic captured, active scans
  - **Activity** — live feed of every AI action (tool + result, errors in red)
  - **Traffic** — table of the exchange shadow store (source / method / url / status)
  - **Scans** — live crawl/audit status with a Stop button
  - **Tools** — searchable catalog of every MCP tool and description
- Central `ActivityLog` and `ToolCatalog` singletons, hooked into every tool's registration/invocation.

## [2.1.0] - 2026-08-21

### Added
- **Intruder staging with payload positions** — `create_intruder_attack` preserves payload markers via `HttpRequestTemplate` (fixes the "payload stubs ignored" bug #92).

## [2.0.0] - 2026-08-21

A GPLv3 fork of [`portswigger/mcp-server`](https://github.com/portswigger/mcp-server), upgraded and extended.

### Added
- **Repeater read access** — `list_repeater_tabs`, `read_repeater_tab_request`, `read_repeater_tab_response`, `send_repeater_request`.
  Backed by an exchange shadow store (`registerHttpHandler`) and a Swing UI bridge, since the Montoya API has no Repeater read access.
- **Intruder read access** — `get_intruder_attack_results` reads shadow-store INTRUDER traffic.
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