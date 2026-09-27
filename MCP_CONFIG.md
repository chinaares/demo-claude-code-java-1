# MCP Configuration Instructions

This file explains how to configure Model Context Protocol (MCP) servers
for your project. It is generated from a template and tailored for each project.

---

## 1. Local Filesystem MCP

- **Purpose**: Allows Claude Code to navigate the project file tree efficiently
- **Root directory**: /Users/yangzb/ai-java/examples/demo-claude-code-java-1
- **Server command**:
```bash
npx @modelcontextprotocol/server-filesystem --path "/Users/yangzb/ai-java/examples/demo-claude-code-java-1"
```

## 2. GitHub MCP (optional)
- **Purpose**: Efficient issue management, pull request creation, and repository analysis
- **Repository**: 
- **Required**: GITHUB_TOKEN environment variable

```bash
export GITHUB_TOKEN=<your-token>
```
- **Add MCP server command**:
```
claude mcp add github --transport http ""
```
⚠️ If `GITHUB_TOKEN` is not set, GitHub MCP will not work.

## 3. Local Git MCP (removed)
- **Status**: ⚠️ Removed on 2026-09-28
- **Reason**: The package `@modelcontextprotocol/server-git` does not exist on npm
- **Alternative**: Claude Code has built-in git tools (git status, git log, etc.) that provide equivalent functionality

## Best Practices
1. Load skills once per session - avoids unnecessary token usage
2. Batch operations - process multiple files/issues together
3. Use MCP whenever possible - more efficient than shell commands
4. Keep this file updated - helps onboarding new contributors

**Notes**:

- Replace `<your-token>` with a valid GitHub Personal Access Token (PAT).
- For more info on creating a PAT and setting up MCP, see:
  - [GitHub Docs: Personal Access Tokens](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens)
  - [Model Context Protocol Docs](https://modelcontextprotocol.io/docs/getting-started/intro)
