package com.plusls.MasaGadget.impl.feature.entityTrace;

import com.google.common.collect.Queues;
import com.plusls.MasaGadget.api.event.RenderEntityListener;
import com.plusls.MasaGadget.game.Configs;
import com.plusls.MasaGadget.util.MiscUtil;
import com.plusls.MasaGadget.util.RenderUtil;
import com.plusls.MasaGadget.util.SyncUtil;
import fi.dy.masa.malilib.util.data.Color4f;
import lombok.Getter;
import org.jetbrains.annotations.ApiStatus;
import top.hendrixshen.magiclib.MagicLib;
import top.hendrixshen.magiclib.api.event.minecraft.render.RenderLevelListener;
import top.hendrixshen.magiclib.api.render.context.LevelRenderContext;
import top.hendrixshen.magiclib.impl.render.context.RenderGlobal;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.Vec3;

import java.util.Queue;

public class EntityTraceRenderer implements RenderEntityListener, RenderLevelListener {
    @Getter
    private static final EntityTraceRenderer instance = new EntityTraceRenderer();
    private final Queue<Entity> queue = Queues.newConcurrentLinkedQueue();

    @ApiStatus.Internal
    public void init() {
        MagicLib.getInstance().getEventManager().register(RenderEntityListener.class, this);
        MagicLib.getInstance().getEventManager().register(RenderLevelListener.class, this);
    }

    @Override
    public void postRenderEntity(Entity entity) {
        if (entity instanceof Villager
                && (Configs.renderVillageHomeTracer.getBooleanValue()
                || Configs.renderVillageJobSiteTracer.getBooleanValue())) {
            this.queue.add(entity);
        }
    }

    @Override
    public void preRenderLevel(ClientLevel level, LevelRenderContext renderContext) {
        // NO-OP
    }

    @Override
    public void postRenderLevel(ClientLevel level, LevelRenderContext renderContext) {
        float partialTick = top.hendrixshen.magiclib.util.minecraft.render.RenderUtil.getPartialTick();

        for (Entity entity : this.queue) {
            if (entity instanceof Villager) {
                Villager villager = MiscUtil.cast(SyncUtil.syncEntityDataFromIntegratedServer(entity));

                if (Configs.renderVillageHomeTracer.getBooleanValue()) {
                    villager.getBrain().getMemory(MemoryModuleType.HOME).ifPresent(globalPos -> {
                        if (!isPoiTargetValid(level, globalPos)) {
                            return;
                        }

                        Vec3 eyeVec3 = entity.getEyePosition(partialTick);
                        Vec3 bedVec3 = new Vec3(globalPos.pos().getX() + 0.5, globalPos.pos().getY() + 0.5, globalPos.pos().getZ() + 0.5);
                        RenderGlobal.disableDepthTest();
                        RenderUtil.drawConnectLine(eyeVec3, bedVec3, 0.05,
                                new Color4f(1, 1, 1),
                                Color4f.fromColor(Configs.renderVillageHomeTracerColor.getColor(), 1.0F),
                                Configs.renderVillageHomeTracerColor.getColor());
                        RenderGlobal.enableDepthTest();
                    });
                }

                if (Configs.renderVillageJobSiteTracer.getBooleanValue()) {
                    villager.getBrain().getMemory(MemoryModuleType.JOB_SITE).ifPresent(globalPos -> {
                        if (!isPoiTargetValid(level, globalPos)) {
                            return;
                        }

                        Vec3 eyeVec3 = entity.getEyePosition(partialTick);
                        Vec3 jobVev3 = new Vec3(globalPos.pos().getX() + 0.5, globalPos.pos().getY() + 0.5, globalPos.pos().getZ() + 0.5);
                        RenderGlobal.disableDepthTest();
                        RenderUtil.drawConnectLine(eyeVec3, jobVev3, 0.05,
                                new Color4f(1, 1, 1),
                                Color4f.fromColor(Configs.renderVillageJobSiteTracerColor.getColor(), 1.0F),
                                Configs.renderVillageJobSiteTracerColor.getColor());
                        RenderGlobal.enableDepthTest();
                    });
                }
            }
        }

        this.queue.clear();
    }

    /**
     * Check whether the remembered point of interest still exists in the world.
     * Villagers keep their HOME / JOB_SITE memory even after the bed or workstation is destroyed,
     * which used to leave a tracer line pointing at the old position.
     */
    private static boolean isPoiTargetValid(ClientLevel level, GlobalPos globalPos) {
        //#if MC >= 12000
        if (globalPos == null) {
            return true;
        }

        net.minecraft.core.BlockPos blockPos = globalPos.pos();

        if (level != null && !globalPos.dimension().equals(level.dimension())) {
            return false;
        }

        Boolean clientCheck = null;
        if (level != null && level.hasChunkAt(blockPos)) {
            clientCheck = net.minecraft.world.entity.ai.village.poi.PoiTypes.hasPoi(level.getBlockState(blockPos));
        }

        Boolean serverCheck = null;
        net.minecraft.client.server.IntegratedServer server =
                net.minecraft.client.Minecraft.getInstance().getSingleplayerServer();
        if (server != null) {
            net.minecraft.server.level.ServerLevel serverLevel = server.getLevel(globalPos.dimension());
            if (serverLevel != null && serverLevel.hasChunkAt(blockPos)) {
                serverCheck = serverLevel.getPoiManager().getType(blockPos).isPresent();
            }
        }

        // Neither side knows the answer (chunk not loaded), keep the original behaviour.
        if (clientCheck == null && serverCheck == null) {
            return true;
        }

        if (Boolean.TRUE.equals(clientCheck) || Boolean.TRUE.equals(serverCheck)) {
            return true;
        }

        logSuppressed(level, globalPos, clientCheck, serverCheck);
        return false;
        //#else
        //$$ return true;
        //#endif
    }

    private static final java.util.Map<Long, Long> SUPPRESSED_LAST = new java.util.concurrent.ConcurrentHashMap<>();
    private static final org.slf4j.Logger TRACE_DEBUG = org.slf4j.LoggerFactory.getLogger("masa_gadget_trace");

    private static void logSuppressed(ClientLevel level, GlobalPos globalPos, Boolean clientCheck, Boolean serverCheck) {
        net.minecraft.core.BlockPos blockPos = globalPos.pos();
        long key = blockPos.asLong();
        long now = System.currentTimeMillis();
        Long prev = SUPPRESSED_LAST.get(key);
        if (prev != null && now - prev < 10000L) {
            return;
        }
        if (SUPPRESSED_LAST.size() > 512) {
            SUPPRESSED_LAST.clear();
        }
        SUPPRESSED_LAST.put(key, now);
        TRACE_DEBUG.info("MGTD|suppress pos={},{},{}|dim={}|client={}|server={}|state={}",
                blockPos.getX(), blockPos.getY(), blockPos.getZ(), globalPos.dimension().identifier(),
                clientCheck, serverCheck,
                level == null ? "?" : String.valueOf(level.getBlockState(blockPos)));
    }
}
