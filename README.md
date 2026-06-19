
<p align="center">
  <img src="https://img.shields.io/badge/Status-ACTIVE-brightgreen?style=for-the-badge" alt="Status">
  <img src="https://img.shields.io/badge/Paper-1.21.4-blue?style=for-the-badge" alt="Paper">
  <img src="https://img.shields.io/badge/Java-21-orange?style=for-the-badge" alt="Java">
  <img src="https://img.shields.io/badge/Depende-Vault-ff69b4?style=for-the-badge" alt="Vault">
</p>

<h1 align="center">GL-EcoAudit</h1>

<p align="center">
  <strong>Economy transaction audit plugin for Paper 1.21.4 servers.</strong><br>
  Automatically records all <code>/pay</code> transactions between players and provides a GUI menu to browse sent and received history.
</p>

---

## Features

- **Automatic recording** – Captures every `/pay` transaction and stores it in the database.
- **Interactive menu** – In-game GUI to browse sent and received transactions.
- **Multi-database support** – SQLite (default), H2, MySQL, PostgreSQL, and MongoDB.
- **Configurable limits** – Control max transactions per player and behavior when limit is reached.
- **Auto-purge** – Automatically removes old records based on a configurable expiration.
- **Fully configurable** – Messages, menus, and database settings via YAML.
- **Vault compatible** – Works with any Vault-compatible economy provider.

---

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/transactions` (`/trans`, `/tx`) | Opens the transactions menu to browse your sent and received payments. | `golden.ecoaudit.transactions` |
| `/ecoaudit` (`/ea`) | Administrative command for the plugin. | `golden.ecoaudit.admin` |

### `/ecoaudit` Subcommands

```
/ecoaudit reload               – Reloads all plugin configuration.
/ecoaudit purge [days]         – Purges transaction records older than the specified days (default: 90).
```

---

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `golden.ecoaudit.transactions` | Allows using `/transactions` and viewing own transaction history. | **OP** |
| `golden.ecoaudit.admin` | Allows using `/ecoaudit` and its subcommands (reload, purge). | **OP** |

---

## Installation

1. Download `GL-EcoAudit.jar` from the releases section.
2. Place the JAR in your server `plugins/` folder.

   > **Required dependency:**  
   > Download and place the following plugin in the same `plugins/` folder for GL-EcoAudit to work:  
   > https://github.com/7Str1kes/Nexus/releases/tag/Release

3. Make sure **Vault** is installed along with an economy plugin (e.g. EssentialsX, CMI, etc.).
4. Restart the server or reload plugins.
5. Done! The plugin will generate its configuration files automatically.

---

## Configuration

### `config.yml`

```yaml
SERVER_NAME: "Survival"              # Server identifier stored in transaction records

TRANSACTIONS:
  MAX_ENTRIES_PER_PAGE: 45           # Items per page in the menu
  MAX_PER_PLAYER: 100                # Max stored transactions per player
  LIMIT_BEHAVIOR: DELETE_OLDEST      # DELETE_OLDEST | STOP_RECORDING
  PURGE_AFTER_DAYS: 0                # 0 = disabled
```

### `database.yml`

Set your database type: `SQLITE`, `H2`, `MYSQL`, `POSTGRESQL`, or `MONGO`.

### `language.yml`

Customize all plugin messages with hex color code support (`&#ff0000`).

### Menus (`menus/`)

Customize titles, materials, slots, lore, and glow effects for the transaction menus.

---

## Support

If you encounter any bugs, have suggestions, or need help, join our official Discord:

<p align="center">
  <a href="https://discord.gg/kCe9zg3mKB">
    <img src="https://img.shields.io/badge/Discord-GL--Studio-5865F2?style=for-the-badge&logo=discord&logoColor=white" alt="Discord">
  </a>
</p>

---

<p align="center">
  Developed by <strong>GL-Studio</strong>
</p>
