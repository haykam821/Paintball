package io.github.haykam821.paintball;

import io.github.haykam821.paintball.game.PaintballConfig;
import io.github.haykam821.paintball.game.item.PaintballItems;
import io.github.haykam821.paintball.game.phase.PaintballWaitingPhase;
import net.fabricmc.api.ModInitializer;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import xyz.nucleoid.plasmid.api.game.GameType;
import xyz.nucleoid.plasmid.api.game.rule.GameRuleType;

public class Main implements ModInitializer {
	public static final String MOD_ID = "paintball";

	private static final Identifier PAINTBALL_ID = Main.identifier("paintball");
	public static final GameType<PaintballConfig> PAINTBALL_TYPE = GameType.register(PAINTBALL_ID, PaintballConfig.CODEC, PaintballWaitingPhase::open);

	public static final GameRuleType PROJECTILE_BARRIER_COLLISION = GameRuleType.create();

	private static final Identifier STAINABLE_TERRACOTTA_ID = Main.identifier("stainable_terracotta");
	public static final TagKey<Block> STAINABLE_TERRACOTTA = TagKey.of(RegistryKeys.BLOCK, STAINABLE_TERRACOTTA_ID);

	private static final Identifier STAIN_REMOVERS_ID = Main.identifier("stain_removers");
	public static final TagKey<Item> STAIN_REMOVERS = TagKey.of(RegistryKeys.ITEM, STAIN_REMOVERS_ID);

	@Override
	public void onInitialize() {
		PaintballItems.register();
	}

	public static Identifier identifier(String path) {
		return Identifier.of(MOD_ID, path);
	}
}
