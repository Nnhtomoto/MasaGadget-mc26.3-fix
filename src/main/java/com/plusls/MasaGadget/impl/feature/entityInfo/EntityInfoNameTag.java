package com.plusls.MasaGadget.impl.feature.entityInfo;

//#if MC >= 26.1
import com.plusls.MasaGadget.game.Configs;
import com.plusls.MasaGadget.util.IrisUtil;
import com.plusls.MasaGadget.util.MiscUtil;
import com.plusls.MasaGadget.util.SyncUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class EntityInfoNameTag {
    private EntityInfoNameTag() {
    }

    public static boolean shouldUseNameTag() {
        return Configs.entityInfoShaderCompat.getBooleanValue() && IrisUtil.isShaderPackInUse();
    }

    public static void append(@NotNull Entity entity, @NotNull EntityRenderState renderState, float partialTick) {
        if (!EntityInfoNameTag.shouldUseNameTag()) {
            return;
        }

        Component info = EntityInfoNameTag.getInfo(entity);

        if (info == null) {
            return;
        }

        Vec3 attachment = entity.getAttachments().getNullable(EntityAttachment.NAME_TAG, 0, entity.getYRot(partialTick));

        if (attachment == null) {
            return;
        }

        MutableComponent nameTag = Component.empty();

        if (renderState.nameTag != null) {
            nameTag.append(renderState.nameTag.copy());
            nameTag.append(Component.literal("\n"));
        }

        nameTag.append(info);
        renderState.nameTag = nameTag;
        renderState.nameTagAttachment = attachment;
    }

    @Nullable
    private static Component getInfo(@NotNull Entity entity) {
        if (entity instanceof Villager) {
            if (!Configs.renderNextRestockTime.getBooleanValue() && !Configs.renderTradeEnchantedBook.getBooleanValue()) {
                return null;
            }

            Villager villager = MiscUtil.cast(SyncUtil.syncEntityDataFromIntegratedServer(entity));
            MutableComponent ret = Component.empty();
            boolean empty = true;

            if (Configs.renderNextRestockTime.getBooleanValue()) {
                Component info = VillagerNextRestockTimeInfo.getInfo(villager);

                if (info != null) {
                    ret.append(info);
                    empty = false;
                }
            }

            if (Configs.renderTradeEnchantedBook.getBooleanValue()) {
                List<Component> infoList = VillagerTradeEnchantedBookInfo.getInfo(villager);

                for (Component info : infoList) {
                    if (!empty) {
                        ret.append(Component.literal("\n"));
                    }

                    ret.append(info);
                    empty = false;
                }
            }

            return empty ? null : ret;
        }

        if (entity instanceof ZombieVillager && Configs.renderZombieVillagerConvertTime.getBooleanValue()) {
            ZombieVillager zombieVillager = MiscUtil.cast(SyncUtil.syncEntityDataFromIntegratedServer(entity));
            return ZombieVillagerConvertTimeInfo.getInfo(zombieVillager);
        }

        return null;
    }
}
//#endif
