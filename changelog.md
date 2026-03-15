# InfiniteHomes 1.2

## GUI Update

We're excited to introduce a brand new graphical interface for managing your homes! 🎉

### ✨ New Features
- **GUI for /home**: Simply type `/home` (without arguments) to open a beautiful, paginated inventory showing all your homes. Each home is displayed with its custom icon.
- **Easy management**: Left‑click a home to teleport, right‑click to delete (with confirmation), or shift‑click to change its icon from a selection of 36 materials.
- **Create homes via GUI**: Click the "Create New Home" button and type the name in chat – no need to remember `/sethome <name>` if you prefer the GUI.
- **Admin mode**: New command `/homeadmin <player>` (requires `infinitehomes.admin`, default OP) lets you manage other players' homes. Perfect for server staff!

### 🔧 Technical Improvements
- Home icons are now saved permanently in `homes.yml`. Old homes automatically get a default red bed icon.
- Cooldown and limit checks are respected when using the GUI.
- All original commands (`/sethome`, `/delhome`, `/home <name>`, `/homes`) still work exactly as before – no breaking changes.

### 🐛 Bug Fixes
- Updated deprecated material names for Minecraft 1.21+ (e.g., `BED` → `RED_BED`, `SCUTE` → `TURTLE_SCUTE`).
- Fixed ItemMeta assignment in icon selection GUI.

Update now and enjoy a more visual way to manage your homes! If you encounter any issues, please report them on our GitHub.

## ✨ New Feature: Teleport Warmup

Admins can now set a warmup delay for home teleports! This adds a suspenseful wait before teleporting, with optional cancellation if the player moves or takes damage.

- **Commands:** `/htp <seconds> [true|false]` (alias `/htpc`) – set delay and whether to cancel on move/damage.
- **Configuration:** Settings are saved in `config.yml` as `teleport-delay` and `teleport-delay-cancel-on-move`.
- **Behavior:** When a player uses `/home` or clicks a home in the GUI, the teleport is delayed by the configured seconds. If cancellation is enabled, moving or taking damage will abort the teleport with a message.
- **Messages:** Fully translated into all supported languages (DE, EN, ES, FR, IT, NL, PT, RU).

This feature is perfect for survival servers wanting to add a bit of risk to teleportation!