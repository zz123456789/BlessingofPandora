# Blessing of Pandora

This page describes **version 1.5.1** for Minecraft 1.20.1 and Forge. Install the same mod version on the client and server.

Blessing of Pandora is a **Forge 1.20.1** addon for [Curse of Pandora](https://modrinth.com/mod/curse-of-pandora). It lets you reverse the seven reality curses into seven ideal blessings, turning their limitations into gifts instead of simply stacking more combat buffs.

## How It Works

When a reversal condition is met, the worn curse charm is transformed in place through special NBT. There is no separate recipe, replacement item, or crafting step. The transformed item keeps the Curse of Pandora item and model, gains a golden name and an enchanted glint, and is bound to the player who completed the reversal.

Transformed blessings can be removed in Survival mode. If another player tries to wear a blessing that is bound to someone else, its name and tooltip become scrambled and its effects do not apply.

## The Seven Blessings

| Blessing | Effect | Reality Index |
| --- | --- | --- |
| Ideal First Blessing: Freedom | +1 Necklace curio slot | +1 |
| Ideal Second Blessing: Infinity | +1 Bracelet curio slot | +1 |
| Ideal Third Blessing: Spirit | Luck +10 | +1 |
| Ideal Fourth Blessing: Strength | Strong +0.5 | +1 |
| Ideal Fifth Blessing: Fearlessness | No curio slot | +2 |
| Ideal Sixth Blessing: Requiem | +2 Charm curio slots | +1 |
| Ideal Seventh Blessing: Liberation | +1 Hands curio slot | +1 |

The **Strong** attribute is a new percentage-based max health attribute registered by this mod. Its default value is 0. Wearing the Ideal Fourth Blessing: Strength grants 0.5 Strong, which increases max health by 50%.

## Curse Reversals

Each reality curse has its own reversal condition. The worn curse item transforms directly into the corresponding blessing. The following are the default conditions; server configuration can change them.

- **Cage -> Freedom:** Wear the First Reality Curse: Cage and cause a Wither kill with an empty main hand while under the Strength effect.
- **Limitation -> Infinity:** Wear the Second Reality Curse: Limitation and fall from Y >= 666 to Y <= -60 in the Overworld.
- **Flesh -> Spirit:** Wear the Third Reality Curse: Flesh and get struck by lightning while under Invisibility.
- **Weakness -> Strength:** Wear the Fourth Reality Curse: Weakness and kill a Weak mob with 325 or more max health.
- **Fear -> Fearlessness:** Wear the Fifth Reality Curse: Fear, hold a Sculk Shrieker, and kill a Warden.
- **Broken Soul -> Requiem:** Wear the Sixth Reality Curse: Broken Soul, gain Soul Sculpting by maintaining at least three other beneficial effects and five harmful effects, then absorb 2,000,000 experience points.
- **Spellbound -> Liberation:** Wear the Seventh Reality Curse: Spellbound. It has a 0.0001% chance to transform every game tick, plus an extra 0.0005% per curse enchantment on the item.

With JEI installed, each reversal can be viewed as a dedicated guide page.

In 1.5.1, Broken Soul's absorbed experience is saved per player and survives restarts. Losing Soul Sculpting pauses absorption; removing the corresponding worn item resets progress according to the existing removal rule. Absorption uses the player's current level and experience bar, never takes more than the remaining requirement, and is limited to 50,000 points per game tick.

Spellbound counts each distinct positive-level curse enchantment once, combining stored enchantments with Forge-provided enchantments. This supports vanilla curses, Pandora's Curse, Celestial Enchantments, and other mods using the standard curse flag.

## Reversal HUD

Sneak-right-click a Pandora necklace or Pandora bracelet to open the global reversal HUD. The default `P` key also opens it while wearing either Pandora holder.

The global HUD lists all seven blessings and shows the current reversal conditions for each corresponding curse. Each blessing has its own allowed/blocked button, controlling whether that curse is allowed to reverse automatically.

Sneak-right-clicking any Reality Curse item opens a dedicated HUD for that curse's reversal allowed/blocked permission. Pandora necklaces, Pandora bracelets, and all seven curse items show a pale gray "Sneak + Right-click to view reversal methods" tooltip line.

The HUD and JEI pages both read configurable reversal values from `config/blessingofpandora.toml`. In multiplayer, 1.5.1 synchronizes the server's settings on login and configuration reload, so the displayed conditions follow the server. The HUD adapts to the window size, wraps text, and supports vertical scrolling on smaller windows.

For the Seventh Reality Curse, the HUD and JEI page also show the current number of recognized curse enchantments on the worn item and the actual conversion chance calculated from that count in real time.

Configurable values are highlighted in bold yellow, while live Seventh Curse enchantment counts and actual conversion chances are highlighted in bold aqua.

The Sixth Reality Curse HUD and JEI descriptions also show the current absorbed experience points, updated from the server as Broken Soul drains experience.

The Pandora's Blessing Transformation Debug Stick remains available for testing, but its crafting recipe has been removed. Creative players or operators holding it can open a separate direct transformation HUD. The server validates access, and reverting a blessing requires it to belong to the player or be unbound.

## Soul Sculpting

Soul Sculpting is automatically granted while at least **three other beneficial effects and five harmful effects** are active. It is removed when either requirement is no longer met. No potion bottles or brewing recipes are added.

While Soul Sculpting is active, the affected entity gains **20% maximum health** and takes **20% extra damage**. Its acquisition condition is shown in the relevant item, HUD, and JEI descriptions.

## Pandora's Curse and Grindstone Transfer

Pandora's Curse is a treasure curse enchantment. It can appear on loot-chest equipment where the loot's enchantment rules allow it.

In 1.5.1, place equipment carrying a stored Pandora's Curse in either grindstone input and a regular book in the other. Taking the result gives you a Pandora's Curse enchanted book and consumes one regular book. The equipment remains in its input slot, with all stored enchantments and its repair penalty removed; its damage, name, and other data are preserved.

Canceling before taking the result leaves the inputs unchanged. This transfer awards no extra experience and does not extract dynamic-only enchantments into an unlimited supply of books.

## Pandora Bracelets

The two special bracelets use Curse of Pandora's own `pandora:pandora_bracelet` item. This mod only writes special NBT to them.

- The first time a player wears all seven reality curses at once, they receive a bracelet with the **Prologue** chapter NBT.
- Each time a player transforms one worn curse into a blessing, the **Cursed to Blessed** attribute increases by 1.
- **Cursed to Blessed** starts at 0 and is capped at 7.
- The first time the attribute reaches 7, the player receives a bracelet with the **Finale** chapter NBT. After that, the value no longer changes and the check no longer repeats.

Both bracelets record the player who received them. Only that player can equip them.

In 1.5.1, Cursed to Blessed progress survives death and relogging. Players who already received the Finale but lost their count in an older version recover a count of seven without receiving a duplicate bracelet.

## Commands

The following commands require operator permissions and are disabled by default. Set `enableCommands = true` in `config/blessingofpandora.toml` and restart before they become available.

Transform the matching curse worn by the player:

```mcfunction
/blessingofpandora 1
/blessingofpandora 2
/blessingofpandora 3
/blessingofpandora 4
/blessingofpandora 5
/blessingofpandora 6
/blessingofpandora 7
```

The numbers correspond to Freedom, Infinity, Spirit, Strength, Fearlessness, Requiem, and Liberation. Transformed blessings belong to the current player.

Give blessing items:

```mcfunction
/blessingofpandora give freedom
/blessingofpandora give freedom @p
/blessingofpandora giveall
/blessingofpandora giveall @p
/giveblessing freedom
/giveblessing freedom @p
```

Available blessing names: `freedom`, `infinity`, `spirit`, `strength`, `fearlessness`, `requiem`, and `liberation`.

## Touhou Little Maid Support

With [Touhou Little Maid](https://modrinth.com/mod/touhou-little-maid) installed, maids can wear blessing items inside a Pandora necklace through Curios.

- If the maid's owner UUID matches the blessing owner UUID, the blessing provides its curio slot, attributes, and Reality Index to the maid.
- If the owner does not match, or the maid has no owner, those effects do not trigger.

Unbound blessing items given through commands can be used by any player or maid.

## Compatibility

- Minecraft: 1.20.1
- Loader: Forge
- Required: Curse of Pandora 2.4.26 or later
- Required: Curios API 5.4.5 or later
- Optional: JEI for reversal guide pages
- Optional: Touhou Little Maid for maid support
- Optional: Iron's Spells 'n Spellbooks, Goety, and Apothic Attributes are declared as optional compatibility dependencies

The mod is designed for both client and dedicated server installation. It also includes a defensive mixin for the Pandora API resize crash that can occur when removing one of multiple Pandora holders, such as a necklace and two bracelets.

## Source

- Source: [GitHub](https://github.com/zz123456789/BlessingofPandora)
- Issues: [GitHub Issues](https://github.com/zz123456789/BlessingofPandora/issues)
- License: MIT
