package io.github.haykam821.paintball.game.player.armor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import io.github.haykam821.paintball.game.player.team.TeamEntry;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.item.DyeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.DyeColor;

public final class StainedArmorHelper {
	private static final int DEFAULT_STAINED_COLOR = 0xAD5398;

	public static ArmorSet createArmorSet(Collection<TeamEntry> teams) {
		return new ArmorSet(StainedArmorHelper.getColor(teams));
	}

	private static int getColor(Collection<TeamEntry> teams) {
		if (teams.size() < 2) {
			return DEFAULT_STAINED_COLOR;
		}

		List<DyeItem> dyes = new ArrayList<>(teams.size());

		for (TeamEntry team : teams) {
			DyeColor color = team.getConfig().blockDyeColor();
			if (color == null) continue;

			DyeItem dye = DyeItem.byColor(color);
			if (dye == null) continue;

			dyes.add(dye);
		}

		return StainedArmorHelper.getColorFromDyes(dyes);
	}

	private static int getColorFromDyes(List<DyeItem> dyes) {
		ItemStack stack = DyedColorComponent.setColor(new ItemStack(Items.LEATHER_BOOTS), dyes);
		return DyedColorComponent.getColor(stack, DyedColorComponent.DEFAULT_COLOR);
	}
}
