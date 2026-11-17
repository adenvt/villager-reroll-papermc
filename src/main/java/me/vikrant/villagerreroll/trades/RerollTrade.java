package me.vikrant.villagerreroll.trades;

import org.bukkit.Material;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class RerollTrade {

    public static MerchantRecipe create() {

        ItemStack result = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = result.getItemMeta();
        meta.setDisplayName("§a🔁 Reroll Trades");
        meta.setLore(List.of(
                "§7Generate fresh vanilla trades",
                "§8Does not consume emerald"
        ));
        result.setItemMeta(meta);

        MerchantRecipe recipe = new MerchantRecipe(result, 9999);

        // ✅ REQUIRED ingredient
        recipe.addIngredient(new ItemStack(Material.EMERALD, 1));

        recipe.setExperienceReward(false);
        recipe.setVillagerExperience(0);
        recipe.setPriceMultiplier(0f);

        return recipe;
    }

    /**
     * True when this villager has never been traded with, i.e. vanilla would still
     * regenerate its offers on workstation break/replace.
     */
    public static boolean canReroll(Villager villager) {
        if (villager.getProfession() == Villager.Profession.NONE) return false;

        // Villager XP is only awarded by trading.
        if (villager.getVillagerExperience() > 0) return false;

        // Trade uses are reset by restocking, so this only catches a villager that
        // traded and had its XP zeroed before its first restock.
        for (MerchantRecipe recipe : villager.getRecipes()) {
            if (isRerollItem(recipe.getResult())) continue;
            if (recipe.getUses() > 0) return false;
        }

        return true;
    }

    public static boolean isRerollItem(ItemStack item) {
        return item != null
                && item.getType() == Material.NETHER_STAR
                && item.hasItemMeta()
                && item.getItemMeta().getDisplayName().contains("Reroll");
    }
}
