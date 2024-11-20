package io.github.haykam821.paintball.game.item;

import java.util.function.Function;

import eu.pb4.sgui.api.GuiHelpers;
import io.github.haykam821.paintball.Main;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.potion.Potions;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

public enum PaintballItems implements ItemConvertible {
	PAINTBALL_LAUNCHER("paintball_launcher", settings -> {
		settings.maxCount(1);
		return new PaintballLauncherItem(settings);
	}),

	STAIN_REMOVER("stain_remover", settings -> {
		settings.component(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Potions.WATER));

		// Prevent using the custom translation key format used by potions
		Identifier id = Main.identifier("stain_remover");
		String translationKey = Util.createTranslationKey("item", id);

		Text customName = Text.translatable(translationKey).styled(GuiHelpers.STYLE_CLEARER);
		settings.component(DataComponentTypes.CUSTOM_NAME, customName);

		return new StainRemoverItem(settings);
	});

	private final RegistryKey<Item> key;
	private final Item item;

	private PaintballItems(String path, Function<Item.Settings, Item> factory) {
		Identifier id = Main.identifier(path);
		this.key = RegistryKey.of(RegistryKeys.ITEM, id);

		this.item = factory.apply(new Item.Settings().registryKey(key));
	}

	@Override
	public Item asItem() {
		return this.item;
	}

	public static void register() {
		for (PaintballItems item : PaintballItems.values()) {
			Registry.register(Registries.ITEM, item.key, item.item);
		}
	}
}
