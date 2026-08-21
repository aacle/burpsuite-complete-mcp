# Burp Suite Complete MCP

> **Full read/write access to Burp Suite — including live Repeater & Intruder state — for AI agents via the Model Context Protocol (MCP).**

A GPLv3 fork of [`portswigger/mcp-server`](https://github.com/portswigger/mcp-server), upgraded and extended to close the gaps in the official server.

## What the official server can't do (and this one can)

The Montoya API exposes **no read access** to Repeater or Intruder — the official MCP server can only
*send* to these tools, never read what a human tester is doing in them. **Burp Suite Complete MCP**
adds a shadow store + UI bridge so your AI agent can:

- **Read** a tester's live Repeater tabs (request *and* response), then mutate and resend from that tab
- **Stage Intruder attacks** with payload positions preserved (fixes the "payload stubs ignored" bug), then read results
- **Run crawls and active audits**, poll scan status, and stop scans
- **Fetch proxy history by request id**, with configurable truncation that no longer drops responses

## Tools

- `list_repeater_tabs` · `read_repeater_tab_request` · `read_repeater_tab_response` · `send_repeater_request`
- `get_intruder_attack_results` · `create_intruder_attack`
- `start_crawl` · `start_audit` · `get_scan_status` · `stop_scan` (Professional)
- `send_http1_request` · `send_http2_request` · `create_repeater_tab(_http2)` · `send_to_intruder`
- `get_proxy_http_history(_regex/_by_id/_summary)` · `get_proxy_websocket_history(_regex)` · `get_organizer_items(_regex)`
- `is_in_scope` · `add_to_scope` · `remove_from_scope` · `get_cookies` · `set_cookie`
- `set_history_item_notes` · `set_history_item_highlight` · `send_to_organizer_by_id` · `send_to_comparer_by_id` · `get_sitemap`
- `get_scanner_issues` · `generate_collaborator_payload` · `get_collaborator_interactions` (Professional)
- `output_project/user_options` · `set_project/user_options` · `set_task_execution_engine_state` · `set_proxy_intercept_state`
- `get/set_active_editor_contents` · `url_encode/decode` · `base64_encode/decode` · `generate_random_string`

See [`SPEC.md`](SPEC.md) for the full design and tool catalog, and [`docs/BUGFIXES.md`](docs/BUGFIXES.md) for the upstream bugs fixed.

## Install (zero-friction)

**BApp Store (once approved):** *Extensions → BApp Store → search "Burp Suite Complete MCP" → Install*.

**GitHub Releases (available immediately):** download `burpsuite-complete-mcp-all.jar` from
[Releases](../../releases), then *Extensions → Add → Java → Select file*.

Then use the in-extension **MCP** tab to one-click configure your client (Claude Desktop, OpenCode,
Copilot CLI, Codex CLI), or point any MCP client at `http://127.0.0.1:9876` (SSE) or use the packaged stdio proxy.

The **MCP tab is a live control room**: a Dashboard (server status + stats), an Activity feed of every
AI action, a Traffic view of captured exchanges, a Scans monitor, and a searchable Tools catalog.

## Build from source

```bash
./gradlew embedProxyJar   # produces build/libs/burpsuite-complete-mcp-all.jar
```

Requirements: Java 21, `jar` on PATH.

## Authorized testing only

This is an offensive-security tool. Use it exclusively against systems you own or have explicit,
written permission to test.

## License

GPLv3 — see [`LICENSE`](LICENSE).