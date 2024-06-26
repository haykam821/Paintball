package io.github.haykam821.paintball.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.SharedConstants;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.intprovider.ConstantIntProvider;
import net.minecraft.util.math.intprovider.IntProvider;
import xyz.nucleoid.plasmid.game.common.config.PlayerConfig;
import xyz.nucleoid.plasmid.game.common.team.GameTeamList;

public class PaintballConfig {
	public static final Codec<PaintballConfig> CODEC = RecordCodecBuilder.create(instance -> {
		return instance.group(
			Identifier.CODEC.fieldOf("map").forGetter(PaintballConfig::getMap),
			PlayerConfig.CODEC.fieldOf("players").forGetter(PaintballConfig::getPlayerConfig),
			GameTeamList.CODEC.fieldOf("teams").forGetter(PaintballConfig::getTeams),
			StainRemovalConfig.CODEC.optionalFieldOf("stain_removal", StainRemovalConfig.DEFAULT).forGetter(PaintballConfig::getStainRemoval),
			IntProvider.NON_NEGATIVE_CODEC.optionalFieldOf("ticks_until_close", ConstantIntProvider.create(SharedConstants.TICKS_PER_SECOND * 5)).forGetter(PaintballConfig::getTicksUntilClose),
			Codec.INT.optionalFieldOf("max_damage", 4).forGetter(PaintballConfig::getMaxDamage),
			Codec.BOOL.optionalFieldOf("allow_friendly_fire", false).forGetter(PaintballConfig::shouldAllowFriendlyFire),
			Codec.INT.optionalFieldOf("stain_radius", 2).forGetter(PaintballConfig::getStainRadius)
		).apply(instance, PaintballConfig::new);
	});

	private final Identifier map;
	private final PlayerConfig playerConfig;
	private final GameTeamList teams;
	private final StainRemovalConfig stainRemoval;
	private final IntProvider ticksUntilClose;
	private final int maxDamage;
	private final boolean allowFriendlyFire;
	private final int stainRadius;

	public PaintballConfig(Identifier map, PlayerConfig playerConfig, GameTeamList teams, StainRemovalConfig stainRemoval, IntProvider ticksUntilClose, int maxDamage, boolean allowFriendlyFire, int stainRadius) {
		this.map = map;
		this.playerConfig = playerConfig;
		this.teams = teams;
		this.stainRemoval = stainRemoval;
		this.ticksUntilClose = ticksUntilClose;
		this.maxDamage = maxDamage;
		this.allowFriendlyFire = allowFriendlyFire;
		this.stainRadius = stainRadius;
	}

	public Identifier getMap() {
		return this.map;
	}

	public PlayerConfig getPlayerConfig() {
		return this.playerConfig;
	}

	public GameTeamList getTeams() {
		return this.teams;
	}

	public StainRemovalConfig getStainRemoval() {
		return this.stainRemoval;
	}

	public IntProvider getTicksUntilClose() {
		return this.ticksUntilClose;
	}

	public int getMaxDamage() {
		return this.maxDamage;
	}

	public boolean shouldAllowFriendlyFire() {
		return this.allowFriendlyFire;
	}

	public int getStainRadius() {
		return this.stainRadius;
	}
}