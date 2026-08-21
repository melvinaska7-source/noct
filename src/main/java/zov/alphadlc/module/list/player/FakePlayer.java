package zov.alphadlc.module.list.player;

import com.google.common.eventbus.Subscribe;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import zov.alphadlc.event.list.EventPacket;
import zov.alphadlc.event.list.EventPopTotem;
import zov.alphadlc.module.Module;
import zov.alphadlc.module.ModuleCategory;
import zov.alphadlc.module.ModuleInformation;

import java.util.UUID;

@ModuleInformation(moduleName = "Fake Player", moduleDesc = "Спавнит фейкового игрока", moduleCategory = ModuleCategory.MISC)
public class FakePlayer extends Module {
    private OtherClientPlayerEntity fakePlayer;

    // Локальное состояние HP: сервер о нашем fake не знает, поэтому считаем сами.
    private final float maxHealth = 20.0F;
    private float health = maxHealth;

    // Небольшое окно неуязвимости, чтобы KillAura не сносила HP каждый tick быстрее ваниллы.
    private static final long INVULN_MS = 500L;
    private long lastHitTime;

    @Override
    public void onEnable() {
        // Регистрируемся в EventBus всегда, чтобы enable/disable оставались симметричными
        // даже если мир ещё не загружен (иначе onDisable упадёт на unregister).
        super.onEnable();
        spawnFake();
    }

    private void spawnFake() {
        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null) return;
        if (fakePlayer != null) return;

        health = maxHealth;
        lastHitTime = 0L;

        GameProfile profile = new GameProfile(UUID.randomUUID(), mc.player.getName().getString());
        fakePlayer = new OtherClientPlayerEntity(mc.world, profile);
        fakePlayer.copyFrom(mc.player);
        fakePlayer.setPos(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        fakePlayer.setYaw(mc.player.getYaw());
        fakePlayer.setPitch(mc.player.getPitch());
        fakePlayer.setHealth(maxHealth);
        mc.world.addEntity(fakePlayer);
    }

    @Override
    public void onDisable() {
        removeFake();
        super.onDisable();
    }

    private void removeFake() {
        if (fakePlayer != null) {
            if (mc.world != null) {
                mc.world.removeEntity(fakePlayer.getId(), Entity.RemovalReason.DISCARDED);
            }
            fakePlayer = null;
        }
        health = maxHealth;
    }

    public boolean owns(Entity entity) {
        return entity != null && fakePlayer != null && entity == fakePlayer;
    }

    public void handleLocalAttack(PlayerEntity attacker) {
        if (attacker == null || fakePlayer == null || mc.world == null) return;

        long now = System.currentTimeMillis();
        if (now - lastHitTime < INVULN_MS) return;
        lastHitTime = now;

        float cooldown = attacker.getAttackCooldownProgress(0.5F);
        float base = (float) attacker.getAttributeValue(EntityAttributes.ATTACK_DAMAGE);
        // Ванильное масштабирование от заряда атаки: слабый удар при незаряженной атаке.
        float damage = base * (0.2F + cooldown * cooldown * 0.8F);

        boolean charged = cooldown > 0.9F;
        boolean crit = charged
                && attacker.fallDistance > 0.0F
                && !attacker.isOnGround()
                && !attacker.isClimbing()
                && !attacker.isTouchingWater()
                && !attacker.hasStatusEffect(StatusEffects.BLINDNESS)
                && !attacker.hasVehicle();
        if (crit) damage *= 1.5F;

        boolean enchanted = attacker.getMainHandStack().hasEnchantments();
        boolean sweeping = charged
                && !crit
                && attacker.isOnGround()
                && !attacker.isSprinting()
                && attacker.getMainHandStack().getItem() instanceof SwordItem;

        playAttackSound(attacker, charged, crit, sweeping, damage);
        playHurtReaction(attacker);

        if (crit) attacker.addCritParticles(fakePlayer);
        if (enchanted) attacker.addEnchantedHitParticles(fakePlayer);
        if (sweeping) spawnSweepParticle(attacker);

        if (health - damage <= 0.0F) {
            // Бесконечный тотем: не убиваем fake — тотем оставляет 1 HP, затем восстанавливаем до максимума.
            // Item не расходуется, поэтому повторные летальные удары снова активируют тотем.
            popTotem();
            health = maxHealth;
        } else {
            health -= damage;
        }
        fakePlayer.setHealth(Math.max(health, 1.0F));
    }

    private void playAttackSound(PlayerEntity attacker, boolean charged, boolean crit, boolean sweeping, float damage) {
        SoundEvent sound;
        if (sweeping) sound = SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP;
        else if (crit) sound = SoundEvents.ENTITY_PLAYER_ATTACK_CRIT;
        else if (charged) sound = SoundEvents.ENTITY_PLAYER_ATTACK_STRONG;
        else if (damage <= 0.0F) sound = SoundEvents.ENTITY_PLAYER_ATTACK_NODAMAGE;
        else sound = SoundEvents.ENTITY_PLAYER_ATTACK_WEAK;

        mc.world.playSound((PlayerEntity) null, attacker.getX(), attacker.getY(), attacker.getZ(),
                sound, attacker.getSoundCategory(), 1.0F, 1.0F);
    }

    private void playHurtReaction(PlayerEntity attacker) {
        // В 1.21.4 нет EntityStatuses.DAMAGE (статус 2 удалён), поэтому воспроизводим
        // hurt-анимацию вручную: красный флеш + наклон от удара + hurt-звук.
        double dx = fakePlayer.getX() - attacker.getX();
        double dz = fakePlayer.getZ() - attacker.getZ();
        float hitYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - attacker.getYaw());

        fakePlayer.hurtTime = fakePlayer.maxHurtTime = 10;
        fakePlayer.animateDamage(hitYaw);

        mc.world.playSound((PlayerEntity) null, fakePlayer.getX(), fakePlayer.getY(), fakePlayer.getZ(),
                SoundEvents.ENTITY_PLAYER_HURT, fakePlayer.getSoundCategory(), 1.0F, 1.0F);
    }

    private void spawnSweepParticle(PlayerEntity attacker) {
        double rad = Math.toRadians(attacker.getYaw());
        double dx = -Math.sin(rad);
        double dz = Math.cos(rad);
        mc.world.addParticle(ParticleTypes.SWEEP_ATTACK,
                fakePlayer.getX() + dx, fakePlayer.getBodyY(0.5), fakePlayer.getZ() + dz,
                dx, 0.0, dz);
    }

    private void popTotem() {
        // Эмулируем vanilla-подобный totem pop у другого игрока: партиклы TOTEM_OF_UNDYING + звук.
        for (int i = 0; i < 40; i++) {
            mc.world.addParticle(ParticleTypes.TOTEM_OF_UNDYING,
                    fakePlayer.getX(), fakePlayer.getBodyY(0.5), fakePlayer.getZ(),
                    (mc.world.random.nextDouble() - 0.5) * 2.0,
                    mc.world.random.nextDouble() * 0.5 + 0.5,
                    (mc.world.random.nextDouble() - 0.5) * 2.0);
        }
        mc.world.playSound((PlayerEntity) null, fakePlayer.getX(), fakePlayer.getY(), fakePlayer.getZ(),
                SoundEvents.ITEM_TOTEM_USE, fakePlayer.getSoundCategory(), 1.0F, 1.0F);

        new EventPopTotem(fakePlayer).post();
    }

    @Subscribe
    private void onPacket(EventPacket e) {
        if (e.getPacket() instanceof DisconnectS2CPacket
                || e.getPacket() instanceof GameJoinS2CPacket
                || e.getPacket() instanceof PlayerRespawnS2CPacket) {
            setEnabled(false);
        }
    }
}
