# Burp Suite Complete MCP

> **Full read/write access to Burp Suite — including live Repeater & Intruder state — for AI agents via the Model Context Protocol (MCP).**

A GPLv3 fork of [`portswigger/mcp-server`](https://github.com/portswigger/mcp-server), upgraded and extended to close the gaps in the official server.

## What the official server can't do (and this one can)

The Montoya API exposes **no read access** to Repeater or Intruder — the official MCP server can only
*send* to these tools, never read what a human tester is doing in them. **Burp Suite Complete MCP**
adds a shadow store + UI bridge so your AI agent can:

- **Read** a tester's live Repeater tabs (request *and* response), then mutate and resend from that tab
- **Drive Intruder** with inline payload lists, and read attack status + results
- **Run crawls, audits and active scans** and poll their status/issues
- Read the **site map**, **scope**, **notes**, and **cookie jar**
- Use **Streamable HTTP / SSE / stdio** transport with optional API-key auth

See [`SPEC.md`](SPEC.md) for the full design and tool catalog, and [`docs/BUGFIXES.md`](docs/BUGFIXES.md) for the upstream bugs fixed.

## Install (zero-friction)

**BApp Store (once approved):** *Extensions → BApp Store → search "Burp Suite Complete MCP" → Install*.

**GitHub Releases (available immediately):** download `burpsuite-complete-mcp-all.jar` from
[Releases](../../releases), then *Extensions → Add → Java → Select file*.

Then use the in-extension **MCP** tab to one-click configure your client (Claude Desktop/Code, OpenCode,
Copilot CLI, Codex, VS Code, Cline), or point any MCP client at `http://127.0.0.1:9876` (SSE).

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