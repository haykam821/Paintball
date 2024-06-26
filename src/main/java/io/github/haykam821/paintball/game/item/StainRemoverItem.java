package io.github.haykam821.paintball.game.item;

import eu.pb4.polymer.core.api.item.PolymerItem;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SplashPotionItem;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.server.network.ServerPlayerEntity;

public class StainRemoverItem extends SplashPotionItem implements PolymerItem {
	public StainRemoverItem(Item.Settings settings) {
		super(settings);
	}

	// Prevent using the custom translation key format used by potions
	@Override
	public String getTranslationKey(ItemStack stack) {
		return this.getTranslationKey();
	}

	@Override
	public Item getPolymerItem(ItemStack stack, ServerPlayerEntity player) {
		return Items.SPLASH_POTION;
	}

	@Override
	public ItemStack getPolymerItemStack(ItemStack stack, TooltipContext context, ServerPlayerEntity player) {
		ItemStack displayStack = PolymerItem.super.getPolymerItemStack(stack, context, player);
		PotionUtil.setPotion(displayStack, Potions.WATER);

		return displayStack;
	}
}
