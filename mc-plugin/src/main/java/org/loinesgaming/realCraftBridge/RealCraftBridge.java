package org.loinesgaming.realCraftBridge;

import io.github.cdimascio.dotenv.Dotenv;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.bukkit.attribute.Attribute;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Statistic;
import java.lang.management.ManagementFactory;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.NamespacedKey;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.stream.Collectors;

public final class RealCraftBridge extends JavaPlugin {

    private HttpServer server;
    private String apiKey;
    private File apiKeyFile;
    private Dotenv dotenv;

    @Override
    public void onEnable() {
        dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .load();

        sendDiscordWebhook("[STATUS] Serveur allumé");

        apiKeyFile = new File(getDataFolder().getParentFile().getParentFile(), ".api_key");
        loadOrCreateApiKey();

        try {
            server = HttpServer.create(new InetSocketAddress(8090), 0);
            server.createContext("/command", new CommandHandler());
            server.setExecutor(null);
            Bukkit.getScheduler().runTaskAsynchronously(this, () -> server.start());
            getLogger().info("HTTP server started on port 8090");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
        sendDiscordWebhook("[STATUS] Serveur éteint");
        if (server != null) {
            server.stop(0);
        }
    }

    private void loadOrCreateApiKey() {
        try {
            if (!apiKeyFile.exists()) {
                String key = generateApiKey();
                Files.write(apiKeyFile.toPath(), key.getBytes());
            }
            apiKey = new String(Files.readAllBytes(apiKeyFile.toPath())).trim();
            getLogger().info("API key loaded from " + apiKeyFile.getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to load or create API key", e);
        }
    }

    private String generateApiKey() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private class CommandHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String providedKey = exchange.getRequestHeaders().getFirst("X-API-Key");
            if (providedKey == null || !providedKey.equals(apiKey)) {
                exchange.sendResponseHeaders(401, -1);
                return;
            }

            String request = new String(exchange.getRequestBody().readAllBytes()).trim();

            Bukkit.getScheduler().runTask(RealCraftBridge.this, () -> {
                try {
                    handleCommand(exchange, request);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }

        private void handleCommand(HttpExchange exchange, String request) throws IOException {
            String[] parts = request.split(" ", 2);
            String command = parts[0];
            String arg = parts.length > 1 ? parts[1] : "";

            JSONObject json = new JSONObject();

            switch (command) {
                case "print":
                    Bukkit.broadcastMessage("[Bridge] " + arg);
                    json.put("status", "Message broadcasted");
                    send(exchange, 200, json.toJSONString());
                    break;

                case "player-list":
                    json.put("players", Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .collect(Collectors.toList()));
                    send(exchange, 200, json.toJSONString());
                    break;

                case "time":
                    long time = Bukkit.getWorlds().get(0).getTime();
                    json.put("time", timeToFrench(time));
                    send(exchange, 200, json.toJSONString());
                    break;

                case "weather":
                    World world = Bukkit.getWorlds().get(0);
                    String weather = world.isThundering() ? "Orage"
                            : world.hasStorm() ? "Pluie"
                            : "Clair";
                    json.put("weather", weather);
                    send(exchange, 200, json.toJSONString());
                    break;

                case "get-exp":
                    Player xpPlayer = Bukkit.getPlayerExact(arg);
                    if (xpPlayer != null) {
                        json.put("exp", xpPlayer.getLevel());
                        send(exchange, 200, json.toJSONString());
                    } else {
                        send(exchange, 404, error("Player not found"));
                    }
                    break;

                case "get-health":
                    Player hpPlayer = Bukkit.getPlayerExact(arg);
                    if (hpPlayer != null) {
                        json.put("health", hpPlayer.getHealth());
                        send(exchange, 200, json.toJSONString());
                    } else {
                        send(exchange, 404, error("Player not found"));
                    }
                    break;

                case "stats":
                    Player statPlayer = Bukkit.getPlayerExact(arg);
                    if (statPlayer != null) {
                        json.put("blocks_walked", statPlayer.getStatistic(Statistic.WALK_ONE_CM) / 100);
                        json.put("diamonds_mined", statPlayer.getStatistic(Statistic.MINE_BLOCK, Material.DIAMOND_ORE));
                        json.put("deaths", statPlayer.getStatistic(Statistic.DEATHS));
                        send(exchange, 200, json.toJSONString());
                    } else {
                        send(exchange, 404, error("Player not found"));
                    }
                    break;

                case "first-join":
                case "last-join":
                case "ping":
                case "armor":
                case "food":
                case "online-since":
                case "advancements":
                    Player player = Bukkit.getPlayerExact(arg);
                    if (player != null) {
                        switch (command) {
                            case "first-join":
                                json.put("first_join", Bukkit.getOfflinePlayer(player.getUniqueId()).getFirstPlayed());
                                break;
                            case "last-join":
                                json.put("last_join", Bukkit.getOfflinePlayer(player.getUniqueId()).getLastPlayed());
                                break;
                            case "ping":
                                json.put("ping", player.getPing());
                                break;
                            case "armor":
                                json.put("armor", player.getAttribute(Attribute.ARMOR).getValue());
                                break;
                            case "food":
                                json.put("food_level", player.getFoodLevel());
                                break;
                            case "online-since":
                                long joinTime = System.currentTimeMillis() - player.getLastLogin();
                                json.put("online_since", joinTime / 1000);
                                break;
                            case "advancements":
                                List<String> completed = new ArrayList<>();
                                if (player != null) {
                                    Iterator<Advancement> it = Bukkit.advancementIterator();
                                    while (it.hasNext()) {
                                        Advancement adv = it.next();
                                        AdvancementProgress progress = player.getAdvancementProgress(adv);
                                        if (progress.isDone()) {
                                            NamespacedKey key = adv.getKey();
                                            completed.add(key.getNamespace() + ":" + key.getKey());
                                        }
                                    }
                                    json.put("completed_advancements", completed);
                                    send(exchange, 200, json.toJSONString());
                                } else {
                                    send(exchange, 404, error("Player not found"));
                                }
                                break;
                        }
                        send(exchange, 200, json.toJSONString());
                    } else {
                        send(exchange, 404, error("Player not found"));
                    }
                    break;

                case "world-time":
                    json.put("world_time", Bukkit.getWorlds().get(0).getTime());
                    send(exchange, 200, json.toJSONString());
                    break;

                case "uptime":
                    long uptimeMillis = ManagementFactory.getRuntimeMXBean().getUptime();
                    json.put("uptime_seconds", uptimeMillis / 1000);
                    send(exchange, 200, json.toJSONString());
                    break;

                case "memory":
                    Runtime runtime = Runtime.getRuntime();
                    json.put("free_mb", runtime.freeMemory() / 1024 / 1024);
                    json.put("used_mb", (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024);
                    json.put("max_mb", runtime.maxMemory() / 1024 / 1024);
                    send(exchange, 200, json.toJSONString());
                    break;

                default:
                    send(exchange, 400, error("Commande inconnue"));
            }
        }

        private void send(HttpExchange exchange, int code, String body) throws IOException {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(code, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }

        private String error(String message) {
            JSONObject error = new JSONObject();
            error.put("error", message);
            return error.toJSONString();
        }

        private String timeToFrench(long time) {
            time = time % 24000;
            if (time < 1000) return "Minuit";
            if (time < 6000) return "Matin";
            if (time < 12000) return "Midi";
            if (time < 13000) return "Après-midi";
            if (time < 18000) return "Soir";
            return "Nuit";
        }
    }
    private void sendDiscordWebhook(String message) {
        try {
            String webhookUrl = dotenv.get("DISCORD_WEBHOOK_URL");
            if (webhookUrl == null || webhookUrl.isEmpty()) return;

            URL url = new URL(webhookUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");

            String jsonPayload = "{\"content\": \"" + message.replace("\"", "\\\"") + "\"}";

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonPayload.getBytes());
                os.flush();
            }

            int responseCode = conn.getResponseCode();
            if (responseCode != 204 && responseCode != 200) {
                getLogger().warning("Discord webhook failed with code " + responseCode);
            }

            conn.disconnect();

        } catch (Exception e) {
            getLogger().warning("Error sending Discord webhook: " + e.getMessage());
        }
    }


}
