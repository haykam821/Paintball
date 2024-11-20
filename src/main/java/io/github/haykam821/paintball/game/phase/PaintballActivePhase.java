package io.github.haykam821.paintball.game.phase;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.github.haykam821.paintball.Main;
import io.github.haykam821.paintball.game.PaintballConfig;
import io.github.haykam821.paintball.game.StainRemovalConfig;
import io.github.haykam821.paintball.game.event.LaunchPaintballEvent;
import io.github.haykam821.paintball.game.map.BlockStaining;
import io.github.haykam821.paintball.game.map.PaintballMap;
import io.github.haykam821.paintball.game.player.EliminationResult;
import io.github.haykam821.paintball.game.player.PlayerEntry;
import io.github.haykam821.paintball.game.player.WinManager;
import io.github.haykam821.paintball.game.player.armor.ArmorSet;
import io.github.haykam821.paintball.game.player.armor.StainedArmorHelper;
import io.github.haykam821.paintball.game.player.team.TeamEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.item.DyeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameCloseReason;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeam;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeamConfig;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeamKey;
import xyz.nucleoid.plasmid.api.game.common.team.TeamChat;
import xyz.nucleoid.plasmid.api.game.common.team.TeamManager;
import xyz.nucleoid.plasmid.api.game.common.team.TeamSelectionLobby;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptor;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptorResult;
import xyz.nucleoid.plasmid.api.game.player.JoinOffer;
import xyz.nucleoid.plasmid.api.game.player.PlayerSet;
import xyz.nucleoid.plasmid.api.game.rule.GameRuleType;
import xyz.nucleoid.plasmid.api.util.BlockTraversal;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.item.ItemUseEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;
import xyz.nucleoid.stimuli.event.player.PlayerSpectateEntityEvent;
import xyz.nucleoid.stimuli.event.projectile.ProjectileHitEvent;

public class PaintballActivePhase implements ProjectileHitEvent.Block, ProjectileHitEvent.Entity, GameActivityEvents.Enable, GameActivityEvents.Tick, LaunchPaintballEvent, GamePlayerEvents.Accept, ItemUseEvent, PlayerDeathEvent, PlayerSpectateEntityEvent, GamePlayerEvents.Remove {
	private final ServerWorld world;
	private final GameSpace gameSpace;
	private final PaintballMap map;
	private final PaintballConfig config;
	private final Set<PlayerEntry> players;
	private final Map<GameTeamKey, TeamEntry> teams = new HashMap<>();
	private final TeamManager teamManager;
	private final WinManager winManager = new WinManager(this);
	private final ArmorSet stainedArmorSet;
	private boolean singleplayer;
	private int ticksUntilClose = -1;

	public PaintballActivePhase(GameSpace gameSpace, ServerWorld world, PaintballMap map, TeamManager teamManager, GlobalWidgets widgets, PaintballConfig config) {
		this.world = world;
		this.gameSpace = gameSpace;
		this.map = map;
		this.config = config;

		PlayerSet participants = this.gameSpace.getPlayers().participants();

		this.players = new HashSet<>(participants.size());
		this.teamManager = teamManager;

		for (ServerPlayerEntity player : participants) {
			GameTeamKey teamKey = this.teamManager.teamFor(player);
			
			TeamEntry teamEntry = this.teams.get(teamKey);
			if (teamEntry == null) {
				teamEntry = new TeamEntry(this, teamKey, this.teamManager.getTeamConfig(teamKey));
				this.teams.put(teamKey, teamEntry);
			}
			
			this.players.add(new PlayerEntry(this, player, teamEntry));
		}

		this.stainedArmorSet = StainedArmorHelper.createArmorSet(this.teams.values());
	}

	private static void setRules(GameActivity activity) {
		activity.deny(GameRuleType.BLOCK_DROPS);
		activity.deny(GameRuleType.BREAK_BLOCKS);
		activity.deny(GameRuleType.CRAFTING);
		activity.deny(GameRuleType.FALL_DAMAGE);
		activity.deny(GameRuleType.FLUID_FLOW);
		activity.deny(GameRuleType.HUNGER);
		activity.deny(GameRuleType.MODIFY_ARMOR);
		activity.deny(GameRuleType.MODIFY_INVENTORY);
		activity.deny(GameRuleType.PLACE_BLOCKS);
		activity.deny(GameRuleType.PORTALS);
		activity.deny(Main.PROJECTILE_BARRIER_COLLISION);
		activity.deny(GameRuleType.PVP);
		activity.deny(GameRuleType.SWAP_OFFHAND);
		activity.deny(GameRuleType.THROW_ITEMS);
		activity.deny(GameRuleType.USE_BLOCKS);
		activity.deny(GameRuleType.USE_ENTITIES);
	}

	public static void open(GameSpace gameSpace, ServerWorld world, PaintballMap map, TeamSelectionLobby teamSelection, PaintballConfig config) {
		gameSpace.setActivity(activity -> {
			TeamManager teamManager = TeamManager.addTo(activity);
			TeamChat.addTo(activity, teamManager);

			for (GameTeam team : config.getTeams()) {
				GameTeamConfig teamConfig = GameTeamConfig.builder(team.config())
					.setFriendlyFire(false)
					.setCollision(Team.CollisionRule.NEVER)
					.build();

				teamManager.addTeam(team.key(), teamConfig);
			}

			teamSelection.allocate(gameSpace.getPlayers().participants(), (teamKey, player) -> {
				teamManager.addPlayerTo(player, teamKey);
			});

			GlobalWidgets widgets = GlobalWidgets.addTo(activity);
			PaintballActivePhase phase = new PaintballActivePhase(gameSpace, world, map, teamManager, widgets, config);

			PaintballActivePhase.setRules(activity);

			// Listeners
			activity.listen(ProjectileHitEvent.BLOCK, phase);
			activity.listen(ProjectileHitEvent.ENTITY, phase);
			activity.listen(GameActivityEvents.ENABLE, phase);
			activity.listen(GameActivityEvents.TICK, phase);
			activity.listen(LaunchPaintballEvent.EVENT, phase);
			activity.listen(GamePlayerEvents.ACCEPT, phase);
			activity.listen(GamePlayerEvents.OFFER, JoinOffer::acceptSpectators);
			activity.listen(ItemUseEvent.EVENT, phase);
			activity.listen(PlayerDeathEvent.EVENT, phase);
			activity.listen(PlayerSpectateEntityEvent.EVENT, phase);
			activity.listen(GamePlayerEvents.REMOVE, phase);
		});
	}

	// Listeners
	@Override
	public EventResult onHitBlock(ProjectileEntity entity, BlockHitResult hitResult) {
		if (!this.tryApplyStainRemoval(hitResult.getPos(), entity) && this.config.getStainRadius() > 0) {
			if (entity.getOwner() instanceof ServerPlayerEntity) {
				PlayerEntry entry = this.getPlayerEntry((ServerPlayerEntity) entity.getOwner());
				if (entry != null) {
					DyeColor color = entry.getTeam().getConfig().blockDyeColor();
					BlockTraversal.create().accept(hitResult.getBlockPos(), (pos, fromPos, depth) -> {
						if (depth > this.config.getStainRadius()) {
							return BlockTraversal.Result.TERMINATE;
						}

						BlockStaining.setStainedState(this.world, pos, color);
						return BlockTraversal.Result.CONTINUE;
					});
				}
			}
		}

		return EventResult.DENY;
	}

	@Override
	public EventResult onHitEntity(ProjectileEntity entity, EntityHitResult hitResult) {
		if (!this.tryApplyStainRemoval(hitResult.getPos(), entity) && hitResult.getEntity() instanceof ServerPlayerEntity) {
			ServerPlayerEntity player = (ServerPlayerEntity) hitResult.getEntity();

			PlayerEntry entry = this.getPlayerEntry(player);
			if (entry != null) {
				// Ensure players are on different teams
				if (entity.getOwner() instanceof ServerPlayerEntity) {
					ServerPlayerEntity ownerPlayer = (ServerPlayerEntity) entity.getOwner();

					PlayerEntry ownerEntry = this.getPlayerEntry(ownerPlayer);
					if (ownerEntry != null) {
						if (this.config.shouldAllowFriendlyFire() || ownerEntry.getTeam() != entry.getTeam()) {
							entry.damage(ownerEntry);
						}
						return EventResult.DENY;
					}
				}

				entry.damage(null);
			}
		}

		return EventResult.DENY;
	}

	@Override
	public void onEnable() {
		this.singleplayer = this.players.size() == 1;

		for (PlayerEntry entry : this.players) {
			entry.spawn(false);
		}

		for (ServerPlayerEntity player : this.gameSpace.getPlayers().spectators()) {
			this.map.teleportToSpectatorSpawn(player);
			player.changeGameMode(GameMode.SPECTATOR);
		}
	}

	@Override
	public void onTick() {
		// Decrease ticks until game end to zero
		if (this.isGameEnding()) {
			if (this.ticksUntilClose == 0) {
				this.gameSpace.close(GameCloseReason.FINISHED);
			}

			this.ticksUntilClose -= 1;
			return;
		}

		Iterator<PlayerEntry> iterator = this.players.iterator();
		while (iterator.hasNext()) {
			PlayerEntry entry = iterator.next();

			EliminationResult result = entry.tick();
			if (result != null && entry.eliminate(result, false)) {
				iterator.remove();
			}
		}

		// Attempt to determine a winner
		Text win = this.winManager.getWin();
		if (win != null) {
			this.sendMessage(win);
			this.ticksUntilClose = this.config.getTicksUntilClose().get(this.world.getRandom());
		}
	}

	@Override
	public ProjectileEntity onLaunchProjectile(World world, ServerPlayerEntity player) {
		PlayerEntry entry = this.getPlayerEntry(player);
		if (entry == null) return null;

		DyeColor color = entry.getTeam().getConfig().blockDyeColor();
		if (color == null) {
			color = DyeColor.WHITE;
		}

		Item item = DyeItem.byColor(color);
		if (item == null) {
			item = Items.WHITE_DYE;
		}
		
		ItemStack stack = new ItemStack(item);
		SnowballEntity projectile = new SnowballEntity(world, player, stack);

		return projectile;
	}

	@Override
	public JoinAcceptorResult onAcceptPlayers(JoinAcceptor acceptor) {
		return this.map.acceptSpectatorJoins(acceptor, this.world).thenRunForEach(player -> {
			player.changeGameMode(GameMode.SPECTATOR);
		});
	}
	
	@Override
	public ActionResult onUse(ServerPlayerEntity player, Hand hand) {
		ItemStack stack = player.getStackInHand(hand);
		PlayerEntry entry = this.getPlayerEntry(player);

		if (entry != null && this.isStainRemovalStack(stack)) {
			entry.decrementStainRemovers();
		}

		return ActionResult.PASS;
	}

	@Override
	public EventResult onDeath(ServerPlayerEntity player, DamageSource source) {
		return EventResult.DENY;
	}

	@Override
	public EventResult onSpectateEntity(ServerPlayerEntity player, Entity target) {
		return target instanceof ArmorStandEntity ? EventResult.DENY : EventResult.PASS;
	}

	@Override
	public void onRemovePlayer(ServerPlayerEntity player) {
		PlayerEntry entry = this.getPlayerEntry(player);
		if (entry != null) {
			entry.eliminateFromGameRemove(true);
		}
	}

	// Getters
	public PaintballMap getMap() {
		return this.map;
	}

	public PaintballConfig getConfig() {
		return this.config;
	}

	public Set<PlayerEntry> getPlayers() {
		return this.players;
	}

	public boolean isSingleplayer() {
		return this.singleplayer;
	}

	public ArmorSet getStainedArmorSet() {
		return this.stainedArmorSet;
	}

	// Utilities
	public boolean isGameEnding() {
		return this.ticksUntilClose >= 0;
	}

	public void sendMessage(Text message) {
		this.gameSpace.getPlayers().sendMessage(message);
	}

	private boolean isStainRemovalStack(ItemStack stack) {
		StainRemovalConfig config = this.getConfig().getStainRemoval();

		Optional<RegistryEntryList<Item>> items = config.getItems(this.world.getRegistryManager());
		if (items.isEmpty()) return false;

		return items.get().contains(stack.getRegistryEntry());
	}

	/**
	 * @return whether this projectile acted as a stain remover
	 */
	private boolean tryApplyStainRemoval(Vec3d pos, ProjectileEntity projectile) {
		StainRemovalConfig config = this.getConfig().getStainRemoval();

		if (!(projectile instanceof PotionEntity potion)) return false;
		if (!this.isStainRemovalStack(potion.getStack())) return false;

		if (!(projectile.getOwner() instanceof ServerPlayerEntity player)) return false;

		PlayerEntry owner = this.getPlayerEntry(player);
		if (owner == null) return false;

		float radius = config.radius().get(this.world.getRandom());

		for (PlayerEntry entry : this.players) {
			if (owner.getTeam() == entry.getTeam() && entry.getRecoveryPos().distanceTo(pos) <= radius) {
				entry.recover(config);
			}
		}

		return true;
	}

	private PlayerEntry getPlayerEntry(ServerPlayerEntity player) {
		for (PlayerEntry entry : this.players) {
			if (player == entry.getPlayer()) {
				return entry;
			}
		}
		return null;
	}
}