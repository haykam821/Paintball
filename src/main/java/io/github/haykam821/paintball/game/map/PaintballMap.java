package io.github.haykam821.paintball.game.map;

import java.util.Set;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import xyz.nucleoid.map_templates.MapTemplate;
import xyz.nucleoid.map_templates.TemplateRegion;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptor;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptorResult;
import xyz.nucleoid.plasmid.api.game.world.generator.TemplateChunkGenerator;

public class PaintballMap {
	private static final String WAITING_SPAWN_MARKER = "waiting_spawn";
	private static final String SPECTATOR_SPAWN_MARKER = "spectator_spawn";
	private static final String FACING_KEY = "Facing";

	private final MapTemplate template;

	public PaintballMap(MapTemplate template) {
		this.template = template;
	}

	public boolean isOutOfBounds(ServerPlayerEntity player) {
		return !this.template.getBounds().contains(player.getBlockPos());
	}

	public boolean teleportToWaitingSpawn(ServerPlayerEntity player) {
		return this.teleportToSpawn(player, WAITING_SPAWN_MARKER);
	}

	public JoinAcceptorResult.Teleport acceptWaitingSpawnOffer(JoinAcceptor acceptor, ServerWorld world) {
		return this.acceptJoins(acceptor, world, WAITING_SPAWN_MARKER);
	}

	public boolean teleportToSpectatorSpawn(ServerPlayerEntity player) {
		return this.teleportToSpawn(player, SPECTATOR_SPAWN_MARKER);
	}

	public JoinAcceptorResult.Teleport acceptSpectatorJoins(JoinAcceptor acceptor, ServerWorld world) {
		return this.acceptJoins(acceptor, world, SPECTATOR_SPAWN_MARKER);
	}

	private JoinAcceptorResult.Teleport acceptJoins(JoinAcceptor acceptor, ServerWorld world, String marker) {
		TemplateRegion region = this.template.getMetadata().getFirstRegion(marker);
		if (region == null) {
			return acceptor.teleport(world, Vec3d.ZERO);
		}

		return this.acceptJoins(acceptor, world, region);
	}

	private JoinAcceptorResult.Teleport acceptJoins(JoinAcceptor acceptor, ServerWorld world, TemplateRegion region) {
		Vec3d pos = region.getBounds().centerBottom();
		float facing = region.getData().getFloat(FACING_KEY);

		return acceptor.teleport(world, pos).thenRunForEach(player -> {
			player.setYaw(facing);
		});
	}

	public boolean teleportToSpawn(ServerPlayerEntity player, String marker) {
		TemplateRegion region = this.template.getMetadata().getFirstRegion(marker);
		if (region == null) return false;

		this.teleportToSpawn(player, region);
		return true;
	}

	private void teleportToSpawn(ServerPlayerEntity player, TemplateRegion region) {
		Vec3d pos = region.getBounds().centerBottom();
		float facing = region.getData().getFloat(FACING_KEY);

		player.teleport(player.getServerWorld(), pos.getX(), pos.getY(), pos.getZ(), Set.of(), facing, 0, true);
	}

	public ChunkGenerator createGenerator(MinecraftServer server) {
		return new TemplateChunkGenerator(server, this.template);
	}
}