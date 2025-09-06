# AI CODEX PROMPT

**Goal:** Create a **client-side Fabric mod** for Minecraft **1.21.4** (Java) controlled via **Discord (JDA 5.x)**. The project must be importable into **IntelliJ IDEA**. Primary features:

1. **Minecraft command `/ss`** (in-game, client command): take a **screenshot of the current gameplay** and **upload** it to a configured Discord channel.
2. **Discord command `/ballgag`** (Discord slash command): toggle **gag**; when active, **all outgoing chat** from the player is transformed into onomatopoeia such as "Mnnph!" (replace every word with random m/n/h letters with initial capitalization). Run again to **ungag**.
3. **Discord command `/blindfold`**: toggle a **black overlay** that **covers only the 3D world** (HUD, inventory, chat, crosshair and important GUI remain visible). Run again to **unblindfold**.
4. **Minecraft command `/leash <playerName>`**: the local player **follows** the target player (client-side follow using basic movement input: forward, turn, simple jump).
5. **Minecraft command `/unleash`**: stop following.

> Note: All controls apply to the **local player** (client). No OP permission required. The Discord bot runs **in the same process** (embedded) for simplicity.

---

## Project Requirements & Tech

* **Java 21**, **Gradle** with **Fabric Loom** (version matching 1.21.4).
* **Dependencies:** Fabric API, JDA 5.x, Gson (for config), SLF4J simple (or disable JDA logging).
* **Shading:** Shade JDA into the mod jar (shadowJar) to avoid dependency conflicts.
* **Package**: `com.example.discordcontrol` (modifiable if consistent).
* **Run configs**: Gradle task `runClient`.

---

## Requested File Structure

```
build.gradle
settings.gradle
gradle.properties
README.md

src/main/resources/
  fabric.mod.json
  discordcontrol.mixins.json
  META-INF/services/... (if needed)

src/main/java/com/example/discordcontrol/
  DiscordControlMod.java                 // Fabric entrypoint (ClientModInitializer)
  config/Config.java                     // Config POJO
  config/ConfigManager.java              // load/save JSON at .minecraft/config/discord_control.json
  discord/DiscordManager.java            // init JDA, register slash cmd, send file, etc
  discord/SlashHandlers.java             // handlers for /ballgag & /blindfold
  features/ScreenshotUtil.java           // take screenshot & store temporarily
  features/BlindfoldRenderer.java        // render black overlay (world only)
  features/FollowController.java         // follow target logic
  features/GagFilter.java                // convert chat text -> "Mnnph!" style
  mixin/ChatSendMixin.java               // intercept outgoing chat when gag active
  commands/ClientCommands.java           // register /ss, /leash, /unleash
```

---

## Configuration (required)

Create file **`config/discord_control.json`** automatically on first run.

```json
{
  "discordToken": "PUT_YOUR_BOT_TOKEN_HERE",
  "channelId": "123456789012345678",
  "guildId": "123456789012345678",
  "controlRoleId": "",           // optional: if filled, only this role can use slash commands
  "screenshotQuality": 100,      // 0-100
  "followMaxSpeed": 0.8,         // scale 0..1 of sprint
  "followStopDistance": 2.0      // meters
}
```

* If `guildId` is set, register **GUILD commands** for instant availability (not global).
* If `controlRoleId` is set, restrict slash command usage to that role.

---

## Implementation: Details & Acceptance Criteria

### A. Entry & Lifecycle

* `DiscordControlMod` implements `ClientModInitializer`.
* On init:
  * Load config (create default if missing).
  * Start **DiscordManager** in a separate thread.
  * Register client commands `/ss`, `/leash`, `/unleash`.
  * Register overlay renderer (BlindfoldRenderer) to appropriate world render event.
  * Initialize **FollowController** (client tick handler).

### B. Discord Bot (JDA)

* `DiscordManager`:
  * Start JDA using token from config.
  * If `guildId` present: `Guild#updateCommands()` to register two slash commands:
    * `/ballgag` (no arguments) → toggle gag (on/off).
    * `/blindfold` (no arguments) → toggle blindfold (on/off).
  * Check role if `controlRoleId` is set; if unauthorized → reply ephemeral "not allowed".
  * Send embed status when toggling (show latest state).
  * **Send file**: method `sendImageToChannel(Path file, String caption)`.

### C. Screenshot (`/ss` in Minecraft)

* Register client command `/ss` via Fabric client command callback (Brigadier).
* `ScreenshotUtil.takeAndSend(Consumer<Path>)`:
  * Take dimensions from current window.
  * Use client screenshot API (Framebuffer → PNG) **after the frame finishes** (ensure running on main client thread).
  * Save to a temporary folder (e.g. `.minecraft/discord-control-cache/ss-<timestamp>.png`).
  * Call `DiscordManager.sendImageToChannel(file, "Screenshot from <playerName> @ <xyz>")`.
  * Show feedback in local chat: “Uploading screenshot… done/failed”.
  * Rate-limit invocations (e.g. min 3 seconds between `/ss`).

### D. Gag Filter (Discord `/ballgag`)

* Boolean state: `GagFilter.gagged`.
* **Intercept outgoing chat**:
  * Example: "Hello there!" → "Mnnph nmm!"
  * Utilize mixin or chat send callback.

### E. Blindfold Overlay (Discord `/blindfold`)

* Black overlay covering only world rendering, not HUD.

### F. Follow Feature

* `/leash <player>` instructs local player to follow target by applying movement inputs each tick until within stop distance or `/unleash`.

### G. Error Handling

* All Discord actions must be **thread-safe**: use `MinecraftClient.getInstance().execute(..)` to change game state.
* Do not crash if Discord login fails: log error & display toast/local chat message.

### H. README (brief)

* Setup:
  1. Create a bot in Discord, obtain the **token**, and invite it to a server (with `applications.commands` and permissions to send messages/files).
  2. Fill `discord_control.json` (token, channelId, guildId).
  3. Import the project in IntelliJ → run Gradle `runClient`.
  4. In Discord: use `/ballgag` & `/blindfold`. In Minecraft: `/ss`, `/leash <player>`, `/unleash`.

---

## Code Snippet Examples (implementation hints)

**Registering client commands:**

```java
public final class ClientCommands {
  public static void register() {
    ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
      dispatcher.register(literal("ss").executes(ctx -> { ScreenshotUtil.takeAndSend(); return 1; }));
      dispatcher.register(literal("leash")
        .then(argument("player", StringArgumentType.word())
        .executes(ctx -> { FollowController.start(StringArgumentType.getString(ctx, "player")); return 1; }));
      dispatcher.register(literal("unleash").executes(ctx -> { FollowController.stop(); return 1; }));
    });
  }
}
```

**Simple gag transformation:**

```java
public final class GagFilter {
  private static final Random R = new Random();
  public static volatile boolean gagged = false;

  public static String gagify(String msg) {
    StringBuilder out = new StringBuilder();
    for (String token : msg.split("(?<=\\b)|(?=\\b)")) {
      if (token.matches("[A-Za-zÀ-ÖØ-öø-ÿ]+")) {
        char c = choose(); // m/n/h
        String repl = token.chars()
          .mapToObj(i -> String.valueOf(c))
          .collect(Collectors.joining());
        if (Character.isUpperCase(token.charAt(0))) repl = repl.substring(0,1).toUpperCase() + repl.substring(1);
        out.append(repl);
      } else {
        out.append(token);
      }
    }
    return out.toString();
  }
  private static char choose(){ int p=R.nextInt(10); return p<7?'n':(p<9?'m':'h'); }
}
```

**Blindfold overlay (WorldRenderEvents.END):**

```java
WorldRenderEvents.END.register(ctx -> {
  if (!BlindfoldRenderer.isBlindfolded) return;
  int w = ctx.gameRenderer().getClient().getWindow().getScaledWidth();
  int h = ctx.gameRenderer().getClient().getWindow().getScaledHeight();
  RenderSystem.disableDepthTest();
  Tesselator t = Tesselator.getInstance();
  BufferBuilder b = t.getBuilder();
  b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
  b.vertex(0, h, 0).color(0,0,0,255).endVertex();
  b.vertex(w, h, 0).color(0,0,0,255).endVertex();
  b.vertex(w, 0, 0).color(0,0,0,255).endVertex();
  b.vertex(0, 0, 0).color(0,0,0,255).endVertex();
  BufferUploader.drawWithShader(b.end());
  RenderSystem.enableDepthTest();
});
```

**Screenshot & upload (condensed):**

```java
public final class ScreenshotUtil {
  public static void takeAndSend() {
    MinecraftClient mc = MinecraftClient.getInstance();
    mc.execute(() -> {
      Path out = ScreenShotHelper.saveScreenshot(mc.runDirectory, "discord-ss"); // use equivalent util for 1.21.4
      if (out != null) DiscordManager.sendImageToChannel(out, "Screenshot by " + mc.getSession().getUsername());
    });
  }
}
```

**Mixin intercept chat (fallback when no event):**

* Create `discordcontrol.mixins.json` and a mixin into the client's chat sending method to modify the message **if `GagFilter.gagged`**.

---

## Completion Checklist

* [ ] Gradle Fabric 1.21.4 project buildable & runnable in IntelliJ.
* [ ] Config JSON created automatically; reads token & channelId.
* [ ] Discord bot online; slash commands `/ballgag` & `/blindfold` function (toggle & reply).
* [ ] `/ss` in game sends PNG to configured channel.
* [ ] Outgoing chat text altered when gag active (e.g., “Hello!” → “Mnnph!”).
* [ ] Black overlay covers 3D world; HUD/GUI remain visible.
* [ ] `/leash <player>` makes player follow target; `/unleash` stops.
* [ ] No crash when Discord fails; log & user-friendly error message.
* [ ] Screenshot rate-limited.

---

## Additional Notes

* Package name, mod ID, and icon are flexible as long as consistent.
* Register Discord commands as **Guild Commands** (using `guildId`) for immediate availability.
* Do not commit token to repository; keep it in local config.

**Expected output:** full project structure + code as specified above.
