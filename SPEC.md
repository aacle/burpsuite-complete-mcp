# Burp Suite Complete MCP — Project Spec

> Full read/write access to Burp Suite (including live Repeater & Intruder state) for AI agents, via MCP.
> A GPLv3 fork of `portswigger/mcp-server`, upgraded and extended.

## 1. Charter

- **Name:** Burp Suite Complete MCP
- **Gradle group:** `com.aacle`
- **Version:** 2.0.0
- **License:** GPLv3 (preserved from upstream)
- **Upstream:** `https://github.com/portswigger/mcp-server`

## 2. Why this exists (verified limitations)

The official server cannot read Repeater or Intruder state. This is a **Montoya API gap** — even the
latest `montoya-api 2026.7` exposes only:

```java
// Repeater.class — write-only
void sendToRepeater(HttpRequest request);
void sendToRepeater(HttpRequest request, String tabName);

// Intruder.class — write-only
void sendToIntruder(...);
```

A human tester's live Repeater work is therefore invisible to any AI client. The official server also
lacks scan control, sitemap, scope, notes, cookie-jar, comparer/decoder, and modern transport/auth.

## 3. Goals

1. **Read + drive Repeater and Intruder** (the headline gap).
2. Expose the full Montoya surface (scanner, sitemap, scope, cookies, comparer/decoder/bambda, utilities, websockets, config, task engine).
3. Fix every confirmed upstream bug (see `docs/BUGFIXES.md`).
4. Modern transport: Streamable HTTP + SSE + stdio; API-key auth.
5. Zero-friction install: BApp Store + GitHub Releases + one-click client installers.
6. Safe-by-default tiering; Pro/Community auto-gating.

## 4. Non-goals (v1)

- No standalone GUI beyond the existing MCP settings tab.
- No cloud/multi-agent coordination.
- No bypassing Burp license gates (Pro-only features stay Pro-only).

## 5. Architecture (single-jar, in-process Kotlin + Ktor + MCP Kotlin SDK)

```
AI Client ──MCP(stdio | SSE | Streamable-HTTP, API key)──► Server (in Burp process)
   ├─ Contract layer      nullable defaults · lenient ints · validation
   ├─ Exchange Shadow Store (http.registerHttpHandler → tag all traffic)
   ├─ Repeater UI Bridge  (Swing walk → tab-name ↔ editor-text; graceful degrade)
   ├─ Auth + host guard   (API key; host-aware DNS-rebinding protection)
   └─ Tool layer          (full Montoya surface, tiered)
```

### 5.1 Subsystems

- **Contract layer** — non-nullable/required fields agree between JSON Schema and runtime decoding;
  lenient number parsing (`10.0` → `10`); `ignoreUnknownKeys`; input validation before Montoya calls.
- **Exchange Shadow Store** — `http.registerHttpHandler` records every exchange (proxy/repeater/intruder/
  scanner/mcp) with a stable `exchange_id`, source tag, request/response fingerprints, timestamp, and tab
  name when resolvable. Ring buffer + temp-file backfill; bodies on demand.
- **Repeater UI Bridge** — `swingUtils().suiteFrame()` walk → Repeater message editors → tab name + current
  request + current response (works for unsent edits). Fallback: shadow-store "last exchange per tab name".
- **Transport/Auth** — Streamable HTTP + SSE + stdio; API key (random UUID, printed to Output tab), required
  on non-loopback binds; host-aware DNS-rebinding guard (fixes WSL/VPN).
- **Tiering** — Tier 0 read/utility always on; Tier 1 active testing behind target approval; Tier 2 dangerous
  (config import, shutdown, shell, scope mutations, BChecks) behind an explicit "enable dangerous tools" toggle.

## 6. Tool catalog (tiered)

**Tier 0 — read/utility (always on)**
`get_proxy_http_history` (fields/excerpts/by_id/newest), websocket history, organizer read, `get_sitemap`,
`search_http_messages`, `inspect_http_message`, codecs (url/base64/hex/html/gzip/crypto/json/string/number/
ranking), `get_status`, `get_audit_trail`, `list_extensions`, `get_cookie_jar`, `get_project_info`,
`get_command_line_args`.

**Tier 1 — active (with target approval)**
`list_repeater_tabs`, `read_repeater_tab_request`, `read_repeater_tab_response`, `send_repeater_request`,
`create_repeater_tab` (1.1/2), `create_intruder_attack` (inline payloads), `get_intruder_attack_status`,
`get_intruder_attack_results`, `send_http1/2_request` (RequestExecutionEngine), `send_http_requests_batch`,
`start_crawl`/`start_audit`/`start_crawl_and_audit`, `get_scan_status`, `stop_scan`, `get_scan_issues`,
`generate_report`, `load_bcheck`, `is_in_scope`/`add_to_scope`/`remove_from_scope`,
`generate_collaborator_payload`/`poll_collaborator_interactions`/`restore_collaborator_client`, `set_cookie`,
`register_session_handling_action`, `get/set_active_editor_contents`, `send_to_comparer`, `send_to_decoder`,
proxy intercept + live handlers.

**Tier 2 — powerful (opt-in)**
`export/import_project_options`, `export/import_user_options` (credential-filtered), `task_engine_state`,
`shutdown_burp`, `import_bambda`, `create_websocket`, `shell_utils`, `open_settings_window`.

## 7. Install & release

1. **BApp Store** (primary) — keep `BappManifest.bmf` pattern, submit to PortSwigger (approval not guaranteed/immediate).
2. **GitHub Releases** (day-one fallback) — CI tags → `burpsuite-complete-mcp-all.jar` attached.
3. **One-click client installers** in the extension MCP tab (Claude Desktop/Code, OpenCode, Copilot CLI, Codex, VS Code, Cline) + `curl|sh` one-liner.
4. Docs: tool reference, tiering/security guide, "authorized testing only" notice.

## 8. Roadmap

| Phase | Deliverable | Acceptance criteria |
|-------|------------|---------------------|
| P0 | Scaffold + contract fixes | Schema tests green; jar builds; release attaches |
| P1 | Shadow Store + Repeater bridge | PoC reads a human's live tab incl. unsent edits |
| P2 | Rich traffic reads | Token usage configurable; response no longer dropped |
| P3 | Intruder + Scanner control | Agent runs scan + fuzz end-to-end |
| P4 | Transport + auth + installers | Works in OpenCode/VS Code/WSL remote |
| P5 | Hardening + release | 10k-item stress passes; release + BApp submitted |

## 9. Risks

| Risk | Mitigation |
|------|-----------|
| Swing bridge breaks on Burp updates | Isolated adapter + shadow-store fallback + per-version tests |
| Shadow store memory blowup | Ring buffer + temp-file + bodies-on-demand |
| AI drives live attacks | Approval dialogs + auto-approve targets + tiering; Tier 2 default-deny |
| BApp approval delay | GitHub Releases + installers shipped first |
| GPLv3 copyleft | Accepted (fork); contributors must agree |