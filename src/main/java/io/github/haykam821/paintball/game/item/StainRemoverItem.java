package io.github.haykam821.paintball.game.item;

import eu.pb4.polymer.core.api.item.PolymerItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SplashPotionItem;
import net.minecraft.util.Identifier;
import xyz.nucleoid.packettweaker.PacketContext;

public class StainRemoverItem extends SplashPotionItem implements PolymerItem {
	public StainRemoverItem(Item.Settings settings) {
		super(settings);
	}

	@Override
	public Item getPolymerItem(ItemStack stack, PacketContext context) {
		return Items.SPLASH_POTION;
	}

	@Override
	public Identifier getPolymerItemModel(ItemStack stack, PacketContext context) {
		return null;
	}
}
