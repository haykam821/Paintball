package io.github.haykam821.paintball.game.item;

import io.github.haykam821.paintball.Main;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public enum PaintballItems implements ItemConvertible {
	PAINTBALL_LAUNCHER("paintball_launcher", new PaintballLauncherItem(new Item.Settings())),
	STAIN_REMOVER("stain_remover", new StainRemoverItem(new Item.Settings()));

	private final Identifier id;
	private final Item item;

	private PaintballItems(String path, Item item) {
		this.id = new Identifier(Main.MOD_ID, path);
		this.item = item;
	}

	@Override
	public Item asItem() {
		return this.item;
	}

	public static void register() {
		for (PaintballItems item : PaintballItems.values()) {
			Registry.register(Registries.ITEM, item.id, item.item);
		}
	}
}
