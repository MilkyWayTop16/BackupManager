# BackupManager

Ultra-lightweight Paper/Leaf server backup plugin streaming directly to Gofile.io without disk overflow or TPS degradation.

## Features

- **Asynchronous streaming**: Compresses and uploads backups entirely in background threads without blocking server ticks.
- **Smart filtering**: Automatically excludes bulky databases, worlds, core archives, and cached tiles.
- **Direct Gofile.io integration**: Uploads archives to Gofile.io with zero configuration and instant download link generation.
- **Zero footprint**: Automatically cleans up temporary ZIP archives upon completion.
- **Silent operation**: No broadcasts or player chat spam; feedback is returned strictly to the command executor.

## Commands

- `/backupmanager` or `/bm` — Trigger an asynchronous backup and upload.
  - Permission: `backupmanager.admin` (default: op)

## Compatibility

- Java 21+
- Paper / Leaf / Purpur 1.21.1+
