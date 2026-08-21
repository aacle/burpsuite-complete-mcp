# AI-First Roadmap — Burp as a set-and-forget backend for AI agents

## The vision

A hunter runs **Claude / Codex / OpenCode** as the driver. The AI does the whole test — recon, request
crafting, fuzzing, scanning, finding bugs, writing notes. The hunter opens **Burp only to verify**: they
glance at the named, highlighted, annotated **Repeater tabs** the AI staged, and trust the findings it filed.

For that, Burp must be:
1. **Fully drivable over MCP** (every Montoya surface exposed as a clean tool).
2. **Visible** — the AI sees all traffic, scope, session state, and findings.
3. **Presentable** — the AI stages results so a human verifies in seconds (names + highlight colors + notes).
4. **Safe** — scope enforcement, approvals, no credential leakage.

## Design principles

- **Read-first:** the AI must be able to see everything before it can act well.
- **Lean context:** field selection + structured filters so real traffic fits in an LLM context window.
- **Verify-by-glance:** every finding gets a color + note + named tab in the GUI.
- **Session-aware:** the AI can read/write the cookie jar so authenticated apps are testable.
- **Concurrency:** batch sends via `RequestExecutionEngine` for AI-generated fuzzing.

## Priority tiers

### P0 — Scope + Session (safety & auth) — highest impact
| Tool | Montoya API | Why |
|------|-------------|-----|
| `get_scope` / `add_to_scope` / `remove_from_scope` / `is_in_scope` | `api.scope()` | The AI must know and enforce the target scope before touching anything |
| `get_cookies` / `set_cookie` | `api.http().cookieJar()` | Authenticated testing — 80% of real targets need a session |

### P1 — Visibility (AI sees everything, cheaply)
| Tool | Montoya API | Why |
|------|-------------|-----|
| History **field selection** (`fields` param) | serialization layer | Stop dumping full req/resp; return only url/status/method/headers |
| History **structured filters** (method/status/mime) | `ProxyHistoryFilter` | Faster, cheaper than regex |
| `get_sitemap` | `api.siteMap()` | Passive recon surface the official server never exposed |
| MCP sends visible in sitemap | `siteMap().add()` | #117/#90 — AI can see what it already did |
| `newest_first` history option | history list | Newest traffic first (upstream #77) |

### P2 — Findings & verification UX (the "just open Burp" part)
| Tool | Montoya API | Why |
|------|-------------|-----|
| `set_notes` / `get_notes` on any request/response | `annotations().setNotes()` | #31 — findings live next to the evidence |
| `set_highlight` | `annotations().setHighlightColor()` | Color-coded findings (RED=vuln, YELLOW=interesting) |
| `send_to_organizer` / `get_organizer_items` | `api.organizer()` | Stage findings into the Organizer tab |
| `send_to_comparer` | `api.comparer()` | Diff base vs mutated response |
| `send_to_decoder` | `api.decoder()` | Push payloads to Decoder |

### P3 — Acting (concurrent send + analysis)
| Tool | Montoya API | Why |
|------|-------------|-----|
| `send_http_requests_batch` | `RequestExecutionEngine` | AI generates N payloads, sends concurrently/rate-limited |
| Response-variation analysis | `createResponseVariationsAnalyzer()` | "Did this payload change anything?" |
| Keyword analysis | `createResponseKeywordsAnalyzer()` | Highlight interesting tokens in responses |

### P4 — Protocol & client ergonomics
| Item | Detail |
|------|--------|
| Streamable HTTP transport | Codex/remote clients |
| API-key auth | expose the server safely beyond loopback |
| `logging/setLevel` | #43 |
| MCP resources | expose scope/findings as resources |
| Fix proxy `initialize` race | #72 (needs `mcp-proxy` repo) |

### P5 — Reporting
| Tool | Detail |
|------|--------|
| `export_findings` | Markdown report from scanner issues + AI notes |
| Fix `get_scanner_issues` | #37 (correct issue-stream handling) |

## Status

- ✅ Done: Repeater read/stage · Intruder positions + results · scanner crawl/audit/status · by-id history · truncation fix · contract fixes · installers (Claude/OpenCode/Copilot/Codex).
- ✅ Done (P0): scope tools (`is_in_scope`/`add_to_scope`/`remove_from_scope`) · cookie jar (`get_cookies`/`set_cookie`).
- ✅ Done (P1): `get_proxy_http_history_summary` (summarize-then-zoom) · `get_sitemap` (passive recon) · MCP sends visible in sitemap (#117/#90).
- ✅ Done (P2): notes + highlight (`set_history_item_notes`/`set_history_item_highlight`) · `send_to_organizer_by_id` · `send_to_comparer_by_id`.
- ⏭ Next: `send_http_requests_batch` (RequestExecutionEngine) · Streamable HTTP + auth · `logging/setLevel` · fix `get_scanner_issues` (#37) · `export_findings` report.