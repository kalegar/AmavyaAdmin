package me.sanjy33.amavyaadmin.motd;

import com.google.gson.*;
import me.sanjy33.amavyaadmin.AmavyaAdmin;
import me.sanjy33.amavyaadmin.SystemManager;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class MessageOfTheDay extends SystemManager {

    private final AmavyaAdmin plugin;
    private final List<TextComponent> componentsList = new ArrayList<>();

    public MessageOfTheDay(AmavyaAdmin plugin) {
        this.plugin = plugin;
    }

    public void sendMotd(Player player) {
        player.spigot().sendMessage(ChatMessageType.CHAT,componentsList.toArray(new BaseComponent[0]));
    }

    @Override
    public void save() {
        if (componentsList.isEmpty()) return;
        JsonArray array = new JsonArray();
        for (TextComponent component : componentsList) {
            JsonObject obj = new JsonObject();
            obj.addProperty("text", component.getText());
            if (component.getHoverEvent() != null) {
                Text text = (Text) component.getHoverEvent().getContents().get(0);
                obj.addProperty("hover",(String) text.getValue());
            }
            if (component.getClickEvent() != null) {
                obj.addProperty("command",component.getClickEvent().getValue());
            }
            array.add(obj);
        }
        Gson gson = new Gson();
        plugin.getConfig().set("messages.motd",gson.toJson(array));
    }

    @Override
    public void reload() {
        parseMotd(plugin.getConfig().getString("messages.motd"));
    }

    public void parseMotd(String json) {
        List<TextComponent> newComponents = new ArrayList<>();
        try {
            JsonArray components = new JsonParser().parse(json).getAsJsonArray();
            for (JsonElement elem : components) {
                if (!elem.isJsonObject()) continue;
                JsonObject obj = elem.getAsJsonObject();
                if (obj.has("text")) {
                    TextComponent tc = new TextComponent();
                    tc.setText(ChatColor.translateAlternateColorCodes('&', obj.get("text").getAsString()));
                    if (obj.has("command")) {
                        tc.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, obj.get("command").getAsString()));
                    }
                    if (obj.has("hover")) {
                        String hover = ChatColor.translateAlternateColorCodes('&', obj.get("hover").getAsString());
                        tc.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(hover)));
                    }
                    newComponents.add(tc);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        componentsList.clear();
        componentsList.addAll(newComponents);
    }
}
