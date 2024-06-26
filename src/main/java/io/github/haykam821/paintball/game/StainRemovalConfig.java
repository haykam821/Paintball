package io.github.haykam821.paintball.game;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.haykam821.paintball.Main;
import io.github.haykam821.paintball.game.item.PaintballItems;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryCodecs;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.floatprovider.ConstantFloatProvider;
import net.minecraft.util.math.floatprovider.FloatProvider;
import net.minecraft.util.math.intprovider.ConstantIntProvider;
import net.minecraft.util.math.intprovider.IntProvider;
import net.minecraft.util.math.random.Random;

public record StainRemovalConfig(
	Optional<RegistryEntryList<Item>> items,
	IntProvider initialCount,
	FloatProvider radius,
	Optional<IntProvider> recoverAmount
) {
	public static final StainRemovalConfig DEFAULT = new StainRemovalConfig(
		Optional.empty(),
		ConstantIntProvider.create(3),
		ConstantFloatProvider.create(3.5f),
		Optional.empty()
	);

	public static final Codec<StainRemovalConfig> CODEC = RecordCodecBuilder.create(instance -> {
		return instance.group(
			RegistryCodecs.entryList(RegistryKeys.ITEM).optionalFieldOf("items").forGetter(StainRemovalConfig::items),
			IntProvider.NON_NEGATIVE_CODEC.optionalFieldOf("initial_count", DEFAULT.initialCount()).forGetter(StainRemovalConfig::initialCount),
			FloatProvider.createValidatedCodec(0, Float.MAX_VALUE).optionalFieldOf("radius", DEFAULT.radius()).forGetter(StainRemovalConfig::radius),
			IntProvider.NON_NEGATIVE_CODEC.optionalFieldOf("recover_amount").forGetter(StainRemovalConfig::recoverAmount)
		).apply(instance, StainRemovalConfig::new);
	});

	/**
	 * Gets the configured stain removers {@linkplain RegistryEntryList registry entry list}, or a default based on the {@code paintball:stain_removers} tag.
	 */
	public Optional<RegistryEntryList<Item>> getItems() {
		return this.items.or(() -> Registries.ITEM.getEntryList(Main.STAIN_REMOVERS));
	}

	/**
	 * Gives a player the initial item stacks for a stain remover.
	 */
	public void give(ServerPlayerEntity player, int count) {
		Random random = player.getRandom();

		this.getItems()
			.flatMap(items -> items.getRandom(random))
			.map(RegistryEntry::value)
			.ifPresent(item -> {
				ItemStack stack = new ItemStack(item, count);

				if (stack.isOf(PaintballItems.STAIN_REMOVER.asItem())) {
					PotionUtil.setPotion(stack, Potions.WATER);
				}

				player.giveItemStack(stack);
			});
	}

	/**
	 * Calculates the updated damage for a player affected by stain remover, based on their current damage.
	 * @return the amount of damage that the player should be left with
	 */
	public int modifyDamage(ServerPlayerEntity player, int damage) {
		// Empty optional indicates removing all damage
		if (this.recoverAmount().isEmpty()) {
			return 0;
		}

		Random random = player.getRandom();
		return Math.max(0, damage - this.recoverAmount().get().get(random));
	}
}
