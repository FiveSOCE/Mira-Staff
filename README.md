# MiraStaff

MiraStaff provides the staff utility toolkit for the Mira Paper server suite. It includes staff mode, vanish, player freezing, live inventory inspection, staff teleporting, state diagnostics and private staff chat while leaving flight authority to MiraFly.

## Download

[**Download MiraStaff v0.1.1**](https://github.com/FiveSOCE/Mira-Staff/releases/download/v0.1.1/MiraStaff-0.1.1.jar)

[View All Releases](https://github.com/FiveSOCE/Mira-Staff/releases)

## Requirements / Dependencies

- Paper 1.21.11
- Java 21
- MiraCore 0.2.0 or newer
- MiraPunishments optional integration
- MiraReports optional integration
- MiraFly remains the flight authority

## How MiraStaff Works

Staff members can toggle staff mode and use moderation utilities without duplicating punishment/report systems. Vanished staff are hidden from players without `mirastaff.vanish.see`. Frozen-player state persists to `plugins/MiraStaff/state.yml`, so a restart cannot silently clear an active staff freeze. Frozen disconnects are broadcast to staff and recorded through MiraCore audit history.

v0.1.1 registers `StaffApi` through Bukkit and MiraCore, audits staff-mode, vanish, freeze, inventory inspection, staff teleports and staff chat actions, and emits `StaffStateChangeEvent` for staff-mode/vanish/freeze state changes. Staff teleports use Paper's async teleport API, and inspection avoids pointless self-inspection.

Staff mode intentionally does not own flight state. MiraFly remains the single flight authority for Mira-managed flight.

## Commands

| Command | Permission | What it does |
| --- | --- | --- |
| `/staff` | `mirastaff.use` | Toggles staff mode. |
| `/vanish` | `mirastaff.vanish` | Toggles staff vanish. |
| `/freeze <player>` | `mirastaff.freeze` | Freezes or unfreezes the selected online player. |
| `/inspect <player>` | `mirastaff.inspect` | Opens the selected player's live inventory. |
| `/stafftp <player>` | `mirastaff.teleport` | Teleports the staff member to the selected player. |
| `/staffchat <message>` | `mirastaff.chat` | Sends a private staff-chat message. |
| `/staffstatus <player>` | `mirastaff.use` | Shows staff-mode, vanish and freeze state. |

Alias: `/sc` for `/staffchat`.

## Permissions

| Permission | Default | What it does |
| --- | --- | --- |
| `mirastaff.use` | OP | Allows staff mode and state inspection. |
| `mirastaff.vanish` | OP | Allows vanish. |
| `mirastaff.vanish.see` | OP | Allows seeing vanished MiraStaff users. |
| `mirastaff.freeze` | OP | Allows freezing/unfreezing players. |
| `mirastaff.inspect` | OP | Allows live inventory inspection. |
| `mirastaff.chat` | OP | Allows staff chat. |
| `mirastaff.teleport` | OP | Allows staff teleport. |

## API / Integration

`StaffApi` is registered through Bukkit ServicesManager and MiraCore. It exposes staff-mode, vanish and freeze queries, programmatic freeze control, and the current persistent frozen-player set.

`StaffStateChangeEvent` provides a typed event for staff-mode, vanish and freeze transitions.

## Persistence

Persistent frozen-player state is stored in `plugins/MiraStaff/state.yml`. Staff mode and vanish are session states and intentionally clear when staff disconnect/restart.

## Building

```bash
gradle clean build
```

The output JAR is created in `build/libs/`.
