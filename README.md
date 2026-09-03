# MiraStaff

MiraStaff provides the core staff utility toolkit for the Mira Paper server suite. It includes staff mode, vanish, player freezing, live inventory inspection, staff teleporting and private staff chat while leaving flight authority to MiraFly.

## Download

[**Download MiraStaff v0.1.0**](https://github.com/FiveSOCE/Mira-Staff/releases/download/v0.1.0/MiraStaff-0.1.0.jar)

## Requirements / Dependencies

- Paper 1.21.11
- Java 21

## How MiraStaff Works

Staff members can toggle a dedicated staff mode and use moderation utilities without each feature being split into separate plugins. Vanished staff can be hidden from normal players, frozen players are prevented from moving normally and staff are alerted if a frozen player disconnects. Inventory inspection provides live access to a player's inventory, while staff teleport and staff chat support moderation coordination.

MiraStaff exposes a public Bukkit ServicesManager API for integration with other Mira systems. It deliberately does not own Bukkit flight state; MiraFly remains the suite's flight authority.

## Commands

| Command | Permission | What it does |
| --- | --- | --- |
| `/staff` | `mirastaff.use` | Toggles staff mode. |
| `/vanish` | `mirastaff.vanish` | Toggles staff vanish. |
| `/freeze <player>` | `mirastaff.freeze` | Freezes or unfreezes the selected player. |
| `/inspect <player>` | `mirastaff.inspect` | Opens a live inventory inspection view for the selected player. |
| `/stafftp <player>` | `mirastaff.teleport` | Teleports the staff member to the selected player. |
| `/staffchat <message>` | `mirastaff.chat` | Sends a private staff-chat message. |

Alias: `/sc` for `/staffchat`.

## Permissions

| Permission | Default | What it does |
| --- | --- | --- |
| `mirastaff.use` | OP | Allows staff mode. |
| `mirastaff.vanish` | OP | Allows vanish. |
| `mirastaff.freeze` | OP | Allows freezing/unfreezing players. |
| `mirastaff.inspect` | OP | Allows live inventory inspection. |
| `mirastaff.chat` | OP | Allows staff chat. |
| `mirastaff.teleport` | OP | Allows staff teleport. |
