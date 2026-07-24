package me.vikrant.villagerreroll.nms;

import net.minecraft.network.protocol.game.ClientboundMerchantOffersPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MerchantMenu;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;

/**
 * Low-level helper that sends {@code ClientboundMerchantOffersPacket} directly
 * to the player, bypassing the normal {@code openMerchant()} path.
 *
 * <p>Why we need this:
 * {@code player.openMerchant()} sends {@code ClientboundOpenWindowPacket} first,
 * which causes the client to close the current window and reset the cursor to
 * centre-screen — the "flicker" effect. By sending only the offers packet
 * to the already-open container we avoid that round-trip entirely.</p>
 *
 * <p><b>Important — classloader restriction in Paper 26.1.2:</b>
 * {@code net.minecraft.world.entity.npc.AbstractVillager} (and its subclasses)
 * are blocked by Paper's plugin classloader. We therefore read the
 * {@code MerchantOffers} from the already-open {@code MerchantMenu} instead of
 * going through the NMS villager handle. After {@code villager.setRecipes()} is
 * called, the menu's internal merchant reference (which IS the NMS villager) will
 * return the updated offer list via {@code menu.getOffers()}.</p>
 */
public final class PacketHelper {

    private PacketHelper() {}

    /**
     * Refreshes the merchant trade list for a player who currently has a
     * merchant GUI open, without closing or reopening the window.
     *
     * <p><strong>Call {@code villager.setRecipes()} before this method.</strong>
     * The {@code MerchantMenu} delegates {@code getOffers()} to its internally-held
     * NMS merchant reference (the villager), so the updated recipes are read
     * automatically.</p>
     *
     * <p>Must be called on the main server thread.</p>
     *
     * @param player   The player whose open merchant GUI will be updated.
     * @param villager The Bukkit villager — used only for level/XP bar values.
     * @return {@code true} if the packet was dispatched;
     *         {@code false} if the player had no merchant window open.
     */
    public static boolean sendMerchantOffers(Player player, Villager villager) {

        // Get the NMS ServerPlayer from the Bukkit Player.
        // CraftPlayer / ServerPlayer are in net.minecraft.server.level.* which is
        // accessible from plugins in Paper 26.1.2.
        ServerPlayer sp = ((CraftPlayer) player).getHandle();

        // Verify the player actually has a merchant window open right now.
        if (!(sp.containerMenu instanceof MerchantMenu menu)) {
            return false;
        }

        // ── Key fix: read offers from MerchantMenu, not the NMS villager handle ─────
        // MerchantMenu.getOffers() is in net.minecraft.world.inventory.* (accessible).
        // It delegates to the merchant's getOffers() internally, which now returns
        // the updated list because we already called villager.setRecipes() above.
        // This avoids net.minecraft.world.entity.npc.AbstractVillager entirely,
        // which is restricted by Paper 26.1.2's plugin classloader.
        var offers = menu.getOffers();

        // Build the packet.
        // containerId must match the client's current window ID exactly.
        // showProgress=true  → renders the villager level/XP bar
        // canRestock=false   → hides the vanilla "Restock" button
        ClientboundMerchantOffersPacket packet = new ClientboundMerchantOffersPacket(
                menu.containerId,
                offers,
                villager.getVillagerLevel(),
                villager.getVillagerExperience(),
                true,   // showProgress
                false   // canRestock
        );

        sp.connection.send(packet);
        return true;
    }
}
