package io.github.haykam821.paintball.game.player.armor;

import io.github.haykam821.paintball.game.player.team.TeamEntry;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import xyz.nucleoid.plasmid.api.util.ItemStackBuilder;

public class ArmorSet {
	private static final String HELMET_KEY = "helmet";
	private static final String CHESTPLATE_KEY = "chestplate";
	private static final String LEGGINGS_KEY = "leggings";
	private static final String BOOTS_KEY = "boots";

	private final int color;

	public ArmorSet(int color) {
		this.color = color;
	}

	public ItemStack getHelmet(TeamEntry team) {
		return this.getArmorItem(Items.LEATHER_HELMET, team, HELMET_KEY);
	}

	public ItemStack getChestplate(TeamEntry team) {
		return this.getArmorItem(Items.LEATHER_CHESTPLATE, team, CHESTPLATE_KEY);
	}

	public ItemStack getLeggings(TeamEntry team) {
		return this.getArmorItem(Items.LEATHER_LEGGINGS, team, LEGGINGS_KEY);
	}

	public ItemStack getBoots(TeamEntry team) {
		return this.getArmorItem(Items.LEATHER_BOOTS, team, BOOTS_KEY);
	}

	private ItemStack getArmorItem(ItemConvertible item, TeamEntry team, String key) {
		return ItemStackBuilder.of(item)
			.setDyeColor(this.color)
			.setName(this.getArmorName(team, key))
			.setUnbreakable()
			.build();
	}

	private Text getArmorName(TeamEntry team, String key) {
		return Text.translatable("text.paintball.team_armor." + key, team.getName())
			.formatted(team.getConfig().chatFormatting());
	}

	@Override
	public String toString() {
		return "ArmorSet{color=" + this.color + "}";
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) return true;
		if (!(other instanceof ArmorSet)) return false;
		return this.color == ((ArmorSet) other).color;
	}

	@Override
	public int hashCode() {
		return this.color;
	}
}
