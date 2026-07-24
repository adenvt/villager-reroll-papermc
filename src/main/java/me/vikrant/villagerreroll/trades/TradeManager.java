package me.vikrant.villagerreroll.trades;

import me.vikrant.villagerreroll.nms.PacketHelper;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class TradeManager {

    /**
     * Rerolls a villager's trades while preserving profession, brain, and POI state
     * so that leveling continues to work normally after reroll.
     *
     * <p>Root-cause notes:</p>
     * <ul>
     *   <li>Never call {@code setProfession(NONE)} — that wipes the brain's JOB_SITE memory.</li>
     *   <li>Reset villagerExperience + villagerLevel so XP thresholds re-sync from scratch.</li>
     *   <li>Use a temp villager (read after 1 tick) to get freshly-generated vanilla trades.</li>
     * </ul>
     *
     * <p><b>Paper 1.21.6 note:</b> cancelling the result-slot click causes Paper to close
     * the merchant GUI server-side before the 1-tick delay fires. We therefore attempt the
     * no-flicker packet path first; if the GUI was already closed we fall back to
     * {@code openMerchant()} (a minor flicker, but the reroll always works).</p>
     */
    public static void rerollTrades(JavaPlugin plugin, Player player, Villager villager) {

        // ── Reset XP and level BEFORE anything else ─────────────────────────────────────
        // Without this, the XP counter is out of sync with level thresholds after the
        // profession is re-acquired from the job block, silently breaking leveling.
        villager.setVillagerExperience(0);
        villager.setVillagerLevel(1);

        // ── Spawn a temp villager to get freshly-generated vanilla trades ────────────────
        // We do NOT call setProfession(NONE) on the REAL villager — that would erase
        // the brain's MemoryModuleType.JOB_SITE and break POI linking after reassign.
        Villager temp = (Villager) villager.getWorld()
                .spawnEntity(villager.getLocation(), EntityType.VILLAGER);

        // Match the real villager's properties so trades are appropriate
        temp.setProfession(villager.getProfession());
        temp.setVillagerType(villager.getVillagerType());
        temp.setVillagerLevel(1);
        temp.setVillagerExperience(0);

        // Prevent the temp villager from interacting with the world while we wait
        temp.setAI(false);
        temp.setSilent(true);
        temp.setInvisible(true);

        // ── Read trades after 1 tick ─────────────────────────────────────────────────────
        // Vanilla generates trades on the next server tick after entity creation +
        // profession set. Reading same-tick always returns an empty list.
        Bukkit.getScheduler().runTaskLater(plugin, () -> {

            List<MerchantRecipe> fresh = new ArrayList<>(temp.getRecipes());
            temp.remove(); // always clean up, even on early return

            if (fresh.isEmpty()) {
                // Fallback: keep whatever trades the real villager currently has
                // (minus any prior reroll trade) so the player always sees something.
                plugin.getLogger().warning("[VillagerReroll] Temp villager generated no trades " +
                        "for profession " + villager.getProfession() + " — using old trades as fallback.");
                fresh = new ArrayList<>();
                for (MerchantRecipe r : villager.getRecipes()) {
                    if (!RerollTrade.isRerollItem(r.getResult())) {
                        fresh.add(r);
                    }
                }
            }

            // Append the reroll trade so the player can reroll again
            fresh.add(RerollTrade.create());

            // Update the server-side villager so every future player sees the new trades
            villager.setRecipes(fresh);

            // ── Attempt no-flicker packet update ─────────────────────────────────────────
            // PacketHelper sends ClientboundMerchantOffersPacket to the OPEN merchant GUI.
            // If the GUI was already closed (Paper 1.21.6 closes it when setCancelled(true)
            // fires on the result slot), this returns false and we fall back to openMerchant().
            boolean sent = PacketHelper.sendMerchantOffers(player, villager);

            if (sent) {
                plugin.getLogger().info("[VillagerReroll] Packet sent — no-flicker update for " + player.getName());
                player.sendMessage("§aVillager trades rerolled!");
            } else {
                // GUI was closed (expected in Paper 1.21.6 due to setCancelled behaviour).
                // Re-open it the traditional way — minor flicker, but reroll always works.
                plugin.getLogger().info("[VillagerReroll] GUI was closed before packet could be sent — " +
                        "reopening for " + player.getName());
                player.openMerchant(villager, true);
                player.sendMessage("§aVillager trades rerolled!");
            }

        }, 1L);
    }
}
