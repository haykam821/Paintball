package io.github.haykam821.paintball.game.player;

import org.joml.Matrix4f;

import com.mojang.authlib.GameProfile;

import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.VirtualEntityUtils;
import eu.pb4.polymer.virtualentity.api.attachment.EntityAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import io.github.haykam821.paintball.mixin.ArmorStandEntityAccessor;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.EulerAngle;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class RevivalMarker {
	private static final Vec3d HEAD_DISPLAY_OFFSET = new Vec3d(0, 1.975, 0);

	private final ArmorStandEntity bodyEntity;
	private final HolderAttachment attachment;

	private RevivalMarker(ArmorStandEntity bodyEntity, HolderAttachment attachment) {
		this.bodyEntity = bodyEntity;
		this.attachment = attachment;
	}

	public Vec3d getPos() {
		return this.bodyEntity.getPos();
	}

	public float getPitch() {
		return this.bodyEntity.getPitch();
	}

	public float getYaw() {
		return this.bodyEntity.getYaw();
	}

	public void destroy() {
		this.bodyEntity.discard();

		this.attachment.holder().destroy();
		this.attachment.destroy();
	}

	private static ArmorStandEntity createArmorStand(PlayerEntry entry) {
		ServerPlayerEntity player = entry.getPlayer();
		ServerWorld world = player.getServerWorld();

		ArmorStandEntity entity = new ArmorStandEntity(EntityType.ARMOR_STAND, world);
		ArmorStandEntityAccessor accessor = (ArmorStandEntityAccessor) (Object) entity;

		// Position
		entity.setPosition(entry.getRecoveryPos());

		entity.setYaw(player.getYaw());
		entity.setPitch(player.getPitch());

		// Appearance
		entity.setHeadRotation(new EulerAngle(player.getPitch(), 0, 0));

		entity.setShowArms(true);
		entity.setHideBasePlate(true);

		// Behavior
		accessor.paintball$setDisabledSlots(Integer.MAX_VALUE);
		entity.setInvulnerable(true);

		return entity;
	}

	private static ItemStack createHeadStack(ServerPlayerEntity player) {
		GameProfile profile = player.getGameProfile();

		ItemStack stack = new ItemStack(Items.PLAYER_HEAD);
		stack.set(DataComponentTypes.PROFILE, new ProfileComponent(profile));

		return stack;
	}

	public static RevivalMarker spawnFromPlayer(PlayerEntry entry) {
		ServerPlayerEntity player = entry.getPlayer();
		ServerWorld world = player.getServerWorld();

		// Body entity
		ArmorStandEntity bodyEntity = createArmorStand(entry);

		entry.applyArmor(entry.getDamageProgress(), bodyEntity);

		ItemStack mainHandStack = player.getStackInHand(Hand.MAIN_HAND);
		ItemStack offHandStack = player.getStackInHand(Hand.OFF_HAND);

		if (player.getMainArm() == Arm.RIGHT) {
			bodyEntity.setStackInHand(Hand.MAIN_HAND, mainHandStack);
			bodyEntity.setStackInHand(Hand.OFF_HAND, offHandStack);
		} else {
			bodyEntity.setStackInHand(Hand.MAIN_HAND, offHandStack);
			bodyEntity.setStackInHand(Hand.OFF_HAND, mainHandStack);
		}

		bodyEntity.setCustomName(player.getDisplayName());
		bodyEntity.setCustomNameVisible(true);

		// Head element
		ItemDisplayElement headElement = new ItemDisplayElement(createHeadStack(player));

		Matrix4f transformation = new Matrix4f()
			.translate(0, -0.55f, 0)
			.rotateY(MathHelper.PI)
			.rotateY(-player.getYaw() * MathHelper.RADIANS_PER_DEGREE)
			.rotateX(-player.getPitch() * MathHelper.RADIANS_PER_DEGREE)
			.translate(0, 0.5f, 0);

		headElement.setTransformation(transformation);

		headElement.setOffset(HEAD_DISPLAY_OFFSET);
		headElement.setSendPositionUpdates(false);

		headElement.setDisplayWidth(1);
		headElement.setDisplayHeight(1);
		headElement.setViewRange(4);

		headElement.setInvisible(true);

		// Element holder
		ElementHolder holder = new ElementHolder();
		holder.addElement(headElement);

		world.spawnEntity(bodyEntity);
		VirtualEntityUtils.addVirtualPassenger(bodyEntity, headElement.getEntityId());

		HolderAttachment attachment = EntityAttachment.ofTicking(holder, bodyEntity);

		return new RevivalMarker(bodyEntity, attachment);
	}
}
