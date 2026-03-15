# InfiniteHomes

**The simplest, most feature-rich home plugin for Minecraft servers. Set unlimited homes, manage them with a beautiful GUI, and enjoy multilingual support – all with zero configuration required!**

InfiniteHomes is designed for server owners who want a reliable home plugin that **just works**. No complicated config files to tweak, no permissions to set up (unless you want admin features). Install it and your players can instantly start setting homes with `/sethome <name>`. But don't let the simplicity fool you – under the hood, InfiniteHomes is packed with features to satisfy both casual players and advanced server admins.

---

## ✨ Features

- **Unlimited homes by default** – no arbitrary limits. But if you need them, you can set a global home limit in the config or via command.
- **Named homes** – players can set multiple homes with custom names (e.g., `/sethome base`, `/sethome farm`).
- **Beautiful GUI** – browse all your homes in a paginated inventory, complete with icons!
    - Left-click to teleport.
    - Right-click to delete (with confirmation).
    - Shift-click to change the icon (choose from dozens of materials).
    - Click the emerald to create a new home – just type the name in chat.
- **Admin GUI** – manage other players’ homes with `/homeadmin <player>`.
- **Teleport warmup (configurable)** – add a delay before teleporting, and optionally cancel if the player moves or takes damage. Perfect for PvP or survival servers.
- **Cooldown (configurable)** – prevent spam by setting a cooldown between `/home` uses.
- **Multilingual** – automatically displays messages in the player’s client language. Supported: English, German, Spanish, French, Italian, Dutch, Portuguese, Russian. (Easily add your own translations!)
- **Tab completion** – quickly find your homes when typing `/home` or `/delhome`.
- **Simple commands** – intuitive and easy to remember.
- **Lightweight & performant** – no database needed; homes are stored in a simple `homes.yml` file.

---

## 📋 Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/sethome <name>` | Set a home at your current location. | none |
| `/delhome <name>` (or `/deletehome`) | Delete a home. | none |
| `/home <name>` (or `/h`) | Teleport to a home. If no name is given, opens the GUI. | none |
| `/homes` (or `/listhomes`) | List all your homes in chat. | none |
| `/homeadmin <player>` | Open the GUI to manage another player’s homes. | `infinitehomes.admin` |
| `/homecount <number>` | Set the global home limit (-1 for unlimited). | OP |
| `/homecooldown <seconds>` | Set the cooldown between home teleports (-1 to disable). | OP |
| `/htp <seconds> [true\|false]` | Set teleport warmup delay and whether to cancel on move/damage. | OP |

---

## 🔧 Configuration (Optional)

All settings are stored in `config.yml` with sensible defaults:

```yaml
# Home limit (-1 = unlimited)
max-homes: -1

# Cooldown in seconds (-1 = disabled)
home-cooldown: -1

# Teleport warmup delay (-1 = disabled)
teleport-delay: -1

# Cancel teleport if player moves or takes damage?
teleport-delay-cancel-on-move: true
```

You can change these at any time – no server restart required (just use the admin commands).

---

## 🌍 Languages

InfiniteHomes speaks your players’ language! It automatically detects the client’s locale and displays all messages accordingly.  
Currently supported:

- 🇬🇧 English
- 🇩🇪 German
- 🇪🇸 Spanish
- 🇫🇷 French
- 🇮🇹 Italian
- 🇳🇱 Dutch
- 🇵🇹 Portuguese
- 🇷🇺 Russian

Want another language? Simply copy `texts_en.yml` from the plugin folder, rename it to `texts_[lang].yml`, and translate the strings. No coding required!

---

## 🛡️ Permissions

- `infinitehomes.admin` – grants access to `/homeadmin`, `/homecount`, `/homecooldown`, and `/htp`. Default: OP.

---

## 📦 Installation

1. Download the latest `InfiniteHomes.jar`.
2. Place it in your server’s `plugins/` folder.
3. Restart your server (or use a plugin manager).
4. That’s it! Players can start using `/sethome` immediately.

---

## YouTube Tutorial (German)

We have an official **German YouTube Tutorial**! Here it is:

<iframe width="560" height="315" src="https://www.youtube-nocookie.com/embed/i_CeLUQJIl8?si=4LHodJb8deVdvV14" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" referrerpolicy="strict-origin-when-cross-origin" allowfullscreen></iframe>

*Thank you minemeeer for this tutorial :)*

---

## 💬 Support

- **Issues & suggestions:** [GitHub Issues](https://github.com/deutschich/InfiniteHomes/issues)
- **Source code:** [GitHub](https://github.com/deutschich/InfiniteHomes)

---

## 📌 Summary

**The simplest and most feature-rich home plugin for Minecraft servers – unlimited homes, GUI, multilingual, and just works out of the box.**