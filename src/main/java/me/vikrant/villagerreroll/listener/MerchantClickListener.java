package me.vikrant.villagerreroll.listener;

import me.vikrant.villagerreroll.trades.RerollTrade;
import me.vikrant.villagerreroll.trades.TradeManager;
import me.vikrant.villagerreroll.util.Constants;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.plugin.java.JavaPlugin;

public class MerchantClickListener implements Listener {

    private final JavaPlugin plugin;

    public MerchantClickListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMerchantClick(InventoryClickEvent event) {

        // Must be a merchant (villager) inventory
        if (!(event.getInventory() instanceof MerchantInventory merchant))
            return;

        // Result slot is always raw slot 2
        if (event.getRawSlot() != 2)
            return;

        if (!(event.getWhoClicked() instanceof Player player))
            return;

        // ── Robust reroll detection ────────────────────────────────────────────────────
        // In Paper 1.21.6, getSelectedRecipe() can return null even when a valid trade
        // is displayed (server-side slot sync lag). We check the result slot item
        // directly instead — it's always populated when the player can see the result.
        ItemStack resultItem = merchant.getItem(2);
        if (!RerollTrade.isRerollItem(resultItem)) {
            // Fallback: try getSelectedRecipe() in case the item slot is temporarily null
            var recipe = merchant.getSelectedRecipe();
            if (recipe == null || !RerollTrade.isRerollItem(recipe.getResult()))
                return;
        }

        // Cancel the click so no item is consumed/given
        event.setCancelled(true);

        if (!(merchant.getMerchant() instanceof Villager villager))
            return;

        if (plugin.getConfig().getBoolean(Constants.ONLY_FRESH_VILLAGERS, true)
                && !RerollTrade.canReroll(villager))
            return;

        plugin.getLogger().info("[VillagerReroll] Reroll triggered by " + player.getName());

        // Delegate reroll (handles XP reset, level reset, temp villager, UI update)
        TradeManager.rerollTrades(plugin, player, villager);
    }
}
