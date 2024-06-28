package io.github.haykam821.paintball.game.item;

import eu.pb4.polymer.core.api.item.PolymerItem;
import io.github.haykam821.paintball.game.event.LaunchPaintballEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.item.DyeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.CooldownUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import xyz.nucleoid.stimuli.EventInvokers;
import xyz.nucleoid.stimuli.Stimuli;

public class PaintballLauncherItem extends Item implements PolymerItem {
	private static final int DYE_COLORS = DyeColor.values().length;
	private static final int COOLDOWN = 10;

	public PaintballLauncherItem(Item.Settings settings) {
		super(settings);
	}

	private void playSound(SoundEvent sound, World world, ServerPlayerEntity player) {
		float pitch = (world.getRandom().nextFloat() * 0.3f) + 1.7f;
		world.playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundCategory.PLAYERS, 0.5f, pitch);
	}

	private SoundEvent getFailSound(World world, ServerPlayerEntity player) {
		return SoundEvents.BLOCK_DISPENSER_FAIL;
	}

	private SoundEvent getLaunchSound(World world, ServerPlayerEntity player) {
		return SoundEvents.ENTITY_SNOWBALL_THROW;
	}

	private ProjectileEntity createDefaultProjectile(World world, ServerPlayerEntity player) {
		SnowballEntity projectile = new SnowballEntity(world, player);

		DyeColor color = DyeColor.byId(world.getRandom().nextInt(DYE_COLORS));
		if (color == null) {
			color = DyeColor.WHITE;
		}

		Item item = DyeItem.byColor(color);
		if (item == null) {
			item = Items.WHITE_DYE;
		}

		projectile.setItem(new ItemStack(item));
		return projectile;
	}

	private Entity createProjectile(World world, ServerPlayerEntity player) {
		ProjectileEntity projectile;

		try (EventInvokers invokers = Stimuli.select().forEntity(player)) {
			projectile = invokers.get(LaunchPaintballEvent.EVENT).onLaunchProjectile(world, player);
		}

		if (projectile == null) {
			projectile = this.createDefaultProjectile(world, player);
		}
		projectile.setVelocity(player, player.getPitch(), player.getYaw(), 0, 1.5f, 1);

		return projectile;
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		if (world.isClient()) {
			return TypedActionResult.success(stack);
		}

		ServerPlayerEntity serverPlayer = (ServerPlayerEntity) user;
		Entity projectile = this.createProjectile(world, serverPlayer);

		if (projectile == null) {
			this.playSound(this.getFailSound(world, serverPlayer), world, serverPlayer);
		} else {
			this.playSound(this.getLaunchSound(world, serverPlayer), world, serverPlayer);
			serverPlayer.incrementStat(Stats.USED.getOrCreateStat(this));

			setPolymerItemCooldown(serverPlayer, stack, COOLDOWN);

			world.spawnEntity(projectile);
		}

		return TypedActionResult.consume(stack);
	}

	@Override
	public Item getPolymerItem(ItemStack stack, ServerPlayerEntity player) {
		return Items.DIAMOND_HORSE_ARMOR;
	}

	private static void setPolymerItemCooldown(ServerPlayerEntity player, ItemStack stack, int duration) {
		player.getItemCooldownManager().set(stack.getItem(), COOLDOWN);

		if (stack.getItem() instanceof PolymerItem polymerItem) {
			Item item = polymerItem.getPolymerItem(stack, player);
			player.networkHandler.sendPacket(new CooldownUpdateS2CPacket(item, COOLDOWN));
		}
	}
}
