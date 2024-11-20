package io.github.haykam821.paintball.game.player.team;

import io.github.haykam821.paintball.game.phase.PaintballActivePhase;
import io.github.haykam821.paintball.game.player.armor.ArmorSet;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeamConfig;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeamKey;

public class TeamEntry {
	private final PaintballActivePhase phase;
	private final GameTeamKey key;
	private final GameTeamConfig config;
	private final ArmorSet armorSet;

	public TeamEntry(PaintballActivePhase phase, GameTeamKey key, GameTeamConfig config) {
		this.phase = phase;

		this.key = key;
		this.config = config;

		this.armorSet = new ArmorSet(config.dyeColor().getRgb());
	}

	// Getters
	public GameTeamConfig getConfig() {
		return this.config;
	}

	public ArmorSet getArmorSet() {
		return this.armorSet;
	}

	// Utilities
	public Text getName() {
		return this.config.name();
	}

	public boolean teleportToSpawn(ServerPlayerEntity player) {
		return this.phase.getMap().teleportToSpawn(player, this.key.id() + "_spawn");
	}

	@Override
	public String toString() {
		return "TeamEntry{phase=" + this.phase + ", key=" + this.key.id() + ", config=" + this.config + ", armorSet=" + this.armorSet + "}";
	}
}
