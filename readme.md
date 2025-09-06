# PROMPT UNTUK AI CODEX

**Goal:** Buat **client-side Fabric mod** Minecraft **1.21.4** (Java) yang dikontrol lewat **Discord (JDA 5.x)**. Proyek harus bisa diimpor di **IntelliJ IDEA**. Fitur utama:

1. **Perintah Minecraft `/ss`** (di game, client command): ambil **screenshot gameplay saat ini** dan **upload** ke channel Discord yang dikonfigurasi.
2. **Perintah Discord `/ballgag`** (slash command di Discord): toggle **gag**; saat aktif, **semua chat outgoing** dari pemain diubah menjadi onomatope seperti “Mnnph!” (ganti setiap kata jadi huruf acak m/n/h dengan kapitalisasi awal). Jalankan lagi untuk **ungag**.
3. **Perintah Discord `/blindfold`**: toggle **overlay hitam** yang **menutupi 3D world saja** (HUD, inventory, chat, crosshair, dan GUI penting tetap terlihat). Jalankan lagi untuk **unblindfold**.
4. **Perintah Minecraft `/leash <playerName>`**: pemain lokal **mengikuti** target player (client-side follow menggunakan input movement: maju, belok, lompat sederhana).
5. **Perintah Minecraft `/unleash`**: hentikan follow.

> Catatan: Semua kontrol berlaku untuk **pemain lokal** (client). Tidak perlu permission OP. Bot Discord berjalan **di proses yang sama** (embedded) agar sederhana.

---

## Persyaratan Proyek & Tech

* **Java 21**, **Gradle** dengan **Fabric Loom** (versi cocok untuk 1.21.4).
* **Dependencies:** Fabric API, JDA 5.x, Gson (untuk config), SLF4J simple (atau matikan logging JDA).
* **Shading:** Shade JDA ke dalam mod jar (shadowJar) agar tidak bentrok dependensi.
* **Package**: `com.example.discordcontrol` (boleh disesuaikan).
* **Run configs**: Gradle tasks `runClient`.

---

## Struktur File yang Diminta

```
build.gradle
settings.gradle
gradle.properties
README.md

src/main/resources/
  fabric.mod.json
  discordcontrol.mixins.json
  icon.png
  META-INF/services/... (jika perlu)

src/main/java/com/example/discordcontrol/
  DiscordControlMod.java                 // entrypoint Fabric (ClientModInitializer)
  config/Config.java                     // POJO config
  config/ConfigManager.java              // load/save JSON di .minecraft/config/discord_control.json
  discord/DiscordManager.java            // init JDA, register slash cmd, kirim file, dsb
  discord/SlashHandlers.java             // handler /ballgag & /blindfold
  features/ScreenshotUtil.java           // ambil screenshot & simpan sementara
  features/BlindfoldRenderer.java        // render overlay hitam (world only)
  features/FollowController.java         // logika follow target
  features/GagFilter.java                // ubah teks chat -> "Mnnph!" style
  mixin/ChatSendMixin.java               // intercept outgoing chat bila gag aktif
  commands/ClientCommands.java           // registrasi /ss, /leash, /unleash
```

---

## Konfigurasi (wajib ada)

Buat file: **`config/discord_control.json`** otomatis saat pertama run.

```json
{
  "discordToken": "PUT_YOUR_BOT_TOKEN_HERE",
  "channelId": "123456789012345678",
  "guildId": "123456789012345678",
  "controlRoleId": "",           // optional: jika diisi, hanya role ini yg boleh pakai slash
  "screenshotQuality": 100,      // 0-100
  "followMaxSpeed": 0.8,         // skala 0..1 dari sprint
  "followStopDistance": 2.0      // meter
}
```

* Jika `guildId` diisi, daftarkan **GUILD commands** agar instant (bukan global).
* Jika `controlRoleId` diisi, batasi hak pakai slash command.

---

## Implementasi: Detail & Acceptance Criteria

### A. Entry & Lifecycle

* `DiscordControlMod` implements `ClientModInitializer`.
* Pada init:

  * Load config (buat default jika belum ada).
  * Start **DiscordManager** di thread terpisah.
  * Register client commands `/ss`, `/leash`, `/unleash`.
  * Daftarkan renderer overlay (BlindfoldRenderer) ke event render dunia yang tepat.
  * Inisialisasi **FollowController** (tick handler client).

### B. Discord Bot (JDA)

* `DiscordManager`:

  * Start JDA dengan token dari config.
  * Jika `guildId` tersedia: `Guild#updateCommands()` untuk register dua slash:

    * `/ballgag` (tanpa argumen) → toggle gag (on/off).
    * `/blindfold` (tanpa argumen) → toggle blindfold (on/off).
  * Cek role jika `controlRoleId` diisi; jika tidak berhak → reply ephemeral “not allowed”.
  * Kirim embed status saat toggle (menunjukkan state terbaru).
  * **Kirim file**: method `sendImageToChannel(Path file, String caption)`.

### C. Screenshot (`/ss` di Minecraft)

* Register client command `/ss` via Fabric client command callback (Brigadier).
* `ScreenshotUtil.takeAndSend(Consumer<Path>)`:

  * Ambil ukuran dari window saat ini.
  * Gunakan API screenshot client (Framebuffer → PNG) **setelah frame selesai** (pastikan dijalankan di main client thread).
  * Simpan ke folder sementara (mis. `.minecraft/discord-control-cache/ss-<timestamp>.png`).
  * Panggil `DiscordManager.sendImageToChannel(file, "Screenshot from <playerName> @ <xyz>")`.
  * Tampilkan feedback di chat lokal: “Uploading screenshot… done/failed”.

### D. Gag Filter (Discord `/ballgag`)

* State boolean: `GagFilter.isGagged`.
* **Intercept outgoing chat**:

  * Gunakan Fabric event **ClientSendMessageEvents.ALLOW\_CHAT** jika tersedia untuk MC 1.21.4.

    * Jika tidak tersedia, gunakan **Mixin** ke metode pengiriman chat client (mis. `ClientPacketListener.sendChatMessage` / nama Yarn terbaru) untuk **memodifikasi isi** (bukan hanya cancel).
  * Transformasi teks:

    * Per kata, ganti huruf alfabet menjadi pattern acak **m/n/h** (misal 70% n, 20% m, 10% h), pertahankan tanda baca.
    * Huruf pertama kapital jika kata asli kapital.
    * Contoh: “tolong aku cepat!” → “Mnnph mnn nnnnh!”
* Toggle lewat `SlashHandlers.onBallgag()`: ubah state + kirim reply “Gag ON/OFF”.

### E. Blindfold Overlay (Discord `/blindfold`)

* State boolean: `BlindfoldRenderer.isBlindfolded`.
* Registrasi ke event **WorldRenderEvents.END** (Fabric API).

  * Di callback ini, **gambar quad hitam** fullscreen dengan alpha=1 **SEBELUM GUI/HUD** digambar, sehingga world tertutup tapi HUD/inventory/chat **tetap terlihat**.
  * Gunakan `RenderSystem.disableTexture`, `BufferBuilder` (atau `DrawContext`) dengan ortho projection sesuai ukuran layar saat ini.
* Toggle lewat `SlashHandlers.onBlindfold()`.

### F. Follow (`/leash <playerName>` & `/unleash`)

* `FollowController`:

  * State: `active`, `targetName`.
  * Setiap client tick:

    * Cari entity target dari `MinecraftClient.getInstance().world.getPlayers()` yang `getGameProfile().getName().equalsIgnoreCase(targetName)`.
    * Jika tidak ada → tampilkan chat lokal “Target not found”.
    * Jika ada:

      * Hitung vektor dari posisi kita ke target.
      * Tekan/tahan **KeyBinding** (`forward`, `left`, `right`, `jump`, `sneak` bila perlu) untuk bergerak mendekat.
      * Berhenti saat jarak < `followStopDistance`.
      * Batasi kecepatan via tidak menahan `sprint` selalu; gunakan `followMaxSpeed`.
      * **Edge basic**: jika beda ketinggian signifikan, lakukan lompatan sesekali.
  * **/leash <playerName>**: set `active=true`, `targetName=...`; munculkan feedback di chat.
  * **/unleash**: set `active=false`, lepaskan semua KeyBinding ke false, feedback “Unleashed”.

### G. Keamanan & Stabilitas

* Semua aksi Discord harus **thread-safe**: gunakan `MinecraftClient.getInstance().execute(..)` untuk ubah state yang menyentuh game.
* Jangan crash bila Discord gagal login: log error & tampilkan toast/ chat lokal.
* Rate-limit upload screenshot (mis. minimal 3 detik antar `/ss`).
* Config autoload + command `/discordreload` (opsional) untuk reload config tanpa restart.

### H. README (singkat)

* Cara setup:

  1. Buat bot di Discord, ambil **token**, invite ke server (punya `applications.commands` dan izin mengirim pesan/file).
  2. Isi `discord_control.json` (token, channelId, guildId).
  3. Import proyek di IntelliJ → Gradle `runClient`.
  4. Di Discord: gunakan `/ballgag` & `/blindfold`. Di Minecraft: `/ss`, `/leash <player>`, `/unleash`.

---

## Contoh Potongan Kode (arah implementasi)

**Registrasi client commands:**

```java
public final class ClientCommands {
  public static void register() {
    ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
      dispatcher.register(literal("ss").executes(ctx -> { ScreenshotUtil.takeAndSend(); return 1; }));
      dispatcher.register(literal("leash")
        .then(argument("player", StringArgumentType.word())
        .executes(ctx -> { FollowController.start(StringArgumentType.getString(ctx, "player")); return 1; })));
      dispatcher.register(literal("unleash").executes(ctx -> { FollowController.stop(); return 1; }));
    });
  }
}
```

**Gag transform sederhana:**

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

**Overlay blindfold (WorldRenderEvents.END):**

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

**Screenshot & upload (ringkas):**

```java
public final class ScreenshotUtil {
  public static void takeAndSend() {
    MinecraftClient mc = MinecraftClient.getInstance();
    mc.execute(() -> {
      Path out = ScreenShotHelper.saveScreenshot(mc.runDirectory, "discord-ss"); // gunakan util setara di versi 1.21.4
      if (out != null) DiscordManager.sendImageToChannel(out, "Screenshot by " + mc.getSession().getUsername());
    });
  }
}
```

**Mixin intercept chat (fallback bila event tidak ada):**

* Buat `discordcontrol.mixins.json` dan Mixin ke method pengiriman chat client untuk memodifikasi pesan **jika `GagFilter.gagged`**.

---

## Kriteria Selesai (Checklist)

* [ ] Proyek Gradle Fabric 1.21.4 buildable & runnable di IntelliJ.
* [ ] Config JSON dibuat otomatis; membaca token & channelId.
* [ ] Bot Discord online; slash `/ballgag` & `/blindfold` bekerja (toggle & reply).
* [ ] `/ss` di game mengirim PNG ke channel yang di-set.
* [ ] Chat outgoing berubah jika gag aktif (contoh “Hello!” → “Mnnph!”).
* [ ] Overlay hitam menutup dunia 3D; HUD/GUI tetap terlihat.
* [ ] `/leash <player>` membuat pemain mengikuti target; `/unleash` menghentikan.
* [ ] Tidak crash saat Discord gagal; ada log & pesan error yang ramah.
* [ ] Rate-limit screenshot.

---

## Catatan Tambahan

* Nama paket, modid, ikon bebas asal konsisten.
* Pastikan **commands Discord** didaftarkan sebagai **Guild Commands** (gunakan `guildId`) agar langsung tersedia.
* Jangan commit token ke repository; masukkan di config lokal.

**Output yang diharapkan:** seluruh struktur proyek + kode sesuai spesifikasi di atas.
