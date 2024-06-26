package io.github.haykam821.paintball.game.player;

import io.github.haykam821.paintball.game.StainRemovalConfig;
import io.github.haykam821.paintball.game.item.PaintballItems;
import io.github.haykam821.paintball.game.phase.PaintballActivePhase;
import io.github.haykam821.paintball.game.player.armor.ArmorSet;
import io.github.haykam821.paintball.game.player.team.TeamEntry;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.util.ItemStackBuilder;

public class PlayerEntry {
	private static final ItemStack HAND_STACK = ItemStackBuilder.of(PaintballItems.PAINTBALL_LAUNCHER)
		.setUnbreakable()
		.build();

	private static final int RAIN_PARTICLE_COUNT = 50;

	private final PaintballActivePhase phase;
	private final ServerPlayerEntity player;
	private final TeamEntry team;
	private int damage = 0;
	private PlayerEntry lastDamager;

	public PlayerEntry(PaintballActivePhase phase, ServerPlayerEntity player, TeamEntry team) {
		this.phase = phase;
		this.player = player;
		this.team = team;
	}

	// Getters
	public ServerPlayerEntity getPlayer() {
		return this.player;
	}

	public TeamEntry getTeam() {
		return this.team;
	}

	// Utilities
	public void spawn(boolean spectator) {
		// State
		this.player.changeGameMode(spectator ? GameMode.SPECTATOR : GameMode.SURVIVAL);
		this.player.setAir(this.player.getMaxAir());
		this.player.setFireTicks(0);
		this.player.fallDistance = 0;
		this.player.clearStatusEffects();

		// Position
		if (spectator) {
			this.phase.getMap().teleportToSpectatorSpawn(this.player);
		} else {
			this.team.teleportToSpawn(this.player);
		}

		// Inventory
		this.player.getInventory().clear();
		this.player.setExperienceLevel(0);
		this.player.setExperiencePoints(0);

		if (!spectator) {
			this.player.giveItemStack(HAND_STACK.copy());
			this.phase.getConfig().getStainRemoval().give(this.player);
			this.applyDamageRepresentation(0);
		}
	}

	private float getDamageProgress() {
		int maxDamage = this.phase.getConfig().getMaxDamage();
		if (maxDamage == 0) {
			return 1;
		}

		return this.damage / (float) maxDamage;
	}

	private ArmorSet getArmorSetForDamageProgress(float damageProgress, float minStainedDamage) {
		return damageProgress >= minStainedDamage ? this.phase.getStainedArmorSet() : this.team.getArmorSet();
	}

	private void applyArmor(float damageProgress) {
		this.player.equipStack(EquipmentSlot.HEAD, (this.getArmorSetForDamageProgress(damageProgress, 1)).getHelmet(this.team));
		this.player.equipStack(EquipmentSlot.CHEST, (this.getArmorSetForDamageProgress(damageProgress, 0.5f)).getChestplate(this.team));
		this.player.equipStack(EquipmentSlot.LEGS, (this.getArmorSetForDamageProgress(damageProgress, 0.75f)).getLeggings(this.team));
		this.player.equipStack(EquipmentSlot.FEET, (this.getArmorSetForDamageProgress(damageProgress, 0.25f)).getBoots(this.team));
	}

	private void applyHealth(float damageProgress) {
		float maxHealth = this.player.getMaxHealth();

		float health = (maxHealth + 1) - (damageProgress * maxHealth);
		if (health < 1) {
			health = 1;
		}

		this.player.setHealth(health);
	}

	/**
	 * Applies damage representation in the form of health and armor.
	 */
	private void applyDamageRepresentation(float damageProgress) {
		this.applyArmor(damageProgress);
		this.applyHealth(damageProgress);

		this.player.currentScreenHandler.sendContentUpdates();
		this.player.playerScreenHandler.onContentChanged(this.player.getInventory());
	}

	/**
	 * Damages the player by one unit and applies their damage representation accordingly.
	 */
	public void damage(PlayerEntry damager) {
		this.damage += 1;

		if (damager != null) {
			this.lastDamager = damager;
		}

		float pitch = (this.player.getRandom().nextFloat() * 0.3f) + 1.2f;
		this.player.playSound(SoundEvents.ENTITY_PLAYER_SPLASH_HIGH_SPEED, SoundCategory.PLAYERS, 1, pitch);

		this.applyDamageRepresentation(this.getDamageProgress());
	}

	/**
	 * Removes damage from the player based on stain removal configuration and applies their damage representation accordingly.
	 */
	public void recover(StainRemovalConfig config) {
		int oldDamage = this.damage;
		this.damage = config.modifyDamage(this.player, this.damage);

		// Apply heart particles
		Random random = this.player.getRandom();

		if (this.damage != oldDamage) {
			for (int index = 0; index < 3; index++) {
				double x = this.player.getParticleX(1);
				double y = this.player.getRandomBodyY() + 0.5;
				double z = this.player.getParticleZ(1);

				double deltaX = random.nextGaussian() * 0.02;
				double deltaY = random.nextGaussian() * 0.02;
				double deltaZ = random.nextGaussian() * 0.02;

				this.player.getServerWorld().spawnParticles(ParticleTypes.HEART, x, y, z, 1, deltaX, deltaY, deltaZ, 1);
			}
		}

		// Apply rain particles
		double x = this.player.getX();
		double y = this.player.getBodyY(0.5);
		double z = this.player.getZ();

		double deltaX = this.player.getWidth() / 2;
		double deltaY = this.player.getHeight() / 4;
		double deltaZ = this.player.getWidth() / 2;

		this.player.getServerWorld().spawnParticles(ParticleTypes.RAIN, x, y, z, RAIN_PARTICLE_COUNT, deltaX, deltaY, deltaZ, 1);

		this.applyDamageRepresentation(this.getDamageProgress());
	}

	/**
	 * Ticks the player.
	 * @return whether the player should be eliminated
	 */
	public Text tick() {
		if (this.phase.getMap().isOutOfBounds(this.player)) {
			return this.getOutOfBoundsEliminationMessage();
		}

		if (this.getDamageProgress() > 1) {
			return this.lastDamager == null ? this.getGenericEliminationMessage() : this.getDamageEliminationMessage();
		}
		return null;
	}

	public void eliminate(Text message, boolean remove) {
		if (this.phase.isGameEnding()) return;

		this.phase.sendMessage(message);

		if (remove) {
			this.phase.getPlayers().remove(this);
		}
		this.spawn(true);
	}

	public void eliminate(boolean remove) {
		this.eliminate(this.getGenericEliminationMessage(), remove);
	}

	private Text getOutOfBoundsEliminationMessage() {
		return Text.translatable("text.paintball.eliminated.out_of_bounds", this.player.getDisplayName()).formatted(Formatting.RED);
	}

	private Text getDamageEliminationMessage() {
		return Text.translatable("text.paintball.eliminated.by", this.player.getDisplayName(), this.lastDamager.getPlayer().getDisplayName()).formatted(Formatting.RED);
	}

	private Text getGenericEliminationMessage() {
		return Text.translatable("text.paintball.eliminated", this.player.getDisplayName()).formatted(Formatting.RED);
	}

	@Override
	public String toString() {
		return "PlayerEntry{player=" + this.player + ", team=" + this.team + ", damage=" + this.damage + "}";
	}
}
