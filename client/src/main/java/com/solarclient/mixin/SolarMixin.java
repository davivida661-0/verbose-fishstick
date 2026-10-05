package com.solarclient.mixin;

import com.solarclient.SolarClient;
import com.solarclient.cosmetics.CosmeticManager;
import com.solarclient.mods.pvp.TabList;
import com.solarclient.mods.render.BossBar;
import com.solarclient.mods.render.ClearGlass;
import com.solarclient.mods.render.FullBright;
import com.solarclient.mods.render.NoHurtCam;
import com.solarclient.mods.render.OneSevenAnimations;
import com.solarclient.mods.render.ScoreboardMod;
import com.solarclient.network.CosmeticNetwork;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.renderer.BlockRenderLayer;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.player.AbstractClientPlayer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.C17PacketCustomPayload;
import net.minecraft.network.play.client.NetHandlerPlayClient;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * <h1>Todos os mixins do client</h1>
 *
 * <p>Esta e a unica classe que "toca" o Minecraft. Cada {@code @Mixin} aninhado
 * injeta em um ponto especifico e chama o client. Regra do projeto:</p>
 *
 * <ul>
 *     <li>o mixin <b>nao tem logica</b> - ele so avisa;
 *     <li>toda a decisao mora em um mod ou no {@link SolarClient};
 *     <li>quando o destino e cancelado, a modificacao e sempre
 *         {@code cancellable = true} e um {@code setCancelled(true)} explicito.</li>
 * </ul>
 *
 * <p>Os pontos de injecao estao listados nos comentarios de cada classe. A
 * lista completa esta em {@code src/main/resources/solar.mixins.json}.</p>
 */
public final class SolarMixin {

    private SolarMixin() {
    }

    // =====================================================================
    // Loop principal: tick, init e shutdown
    // =====================================================================
    @Mixin(Minecraft.class)
    public abstract static class MinecraftMixin {

        /** Roda uma vez por tick, no fim do {@code runTick} do jogo. */
        @Inject(method = "runTick", at = @At("TAIL"))
        private void solar$tick(CallbackInfo ci) {
            if (!SolarClient.isInitialized()) {
                // primeiro tick: o jogo ja tem janela, mundo e GameSettings
                SolarClient.get().init();
            }
            SolarClient.get().onTick();
        }

        /** Salva o config antes do processo morrer. */
        @Inject(method = "shutdown", at = @At("HEAD"))
        private void solar$shutdown(CallbackInfo ci) {
            if (SolarClient.isInitialized()) {
                SolarClient.get().shutdown();
            }
        }
    }

    // =====================================================================
    // HUD 2D
    // =====================================================================
    @Mixin(GuiIngame.class)
    public abstract static class GuiIngameMixin {

        /**
         * Inicio do render do HUD. Aqui o client desenha por cima de tudo:
         * poste o {@code Events.Render2D} e cancela as partes do vanilla que
         * foram substituidas (sidebar tipo 3, bossbar tipo 2).
         */
        @Inject(method = "renderGameOverlay", at = @At("HEAD"), cancellable = true)
        private void solar$renderHud(int type, float partialTicks, CallbackInfoReturnable<Integer> cir) {
            Minecraft mc = Minecraft.getMinecraft();
            if (type == 0) {
                int width = new net.minecraft.client.gui.ScaledResolution(mc).getScaledWidth();
                int height = new net.minecraft.client.gui.ScaledResolution(mc).getScaledHeight();
                SolarClient.get().onRender2D(new com.solarclient.event.Events.Render2D(
                        width, height, org.lwjgl.input.Mouse.getX()
                                * width / org.lwjgl.opengl.Display.getWidth(),
                        org.lwjgl.input.Mouse.getY()
                                * height / org.lwjgl.opengl.Display.getHeight(),
                        partialTicks));
            }

            // Sidebar: o Scoreboard Mod desenha a versao nova
            if (type == 3 && ScoreboardMod.get() != null && ScoreboardMod.get().isEnabled()) {
                ScoreboardMod.renderReplacement(new com.solarclient.event.Events.Render2D(
                        new net.minecraft.client.gui.ScaledResolution(mc).getScaledWidth(),
                        new net.minecraft.client.gui.ScaledResolution(mc).getScaledHeight(),
                        0, 0, partialTicks));
                cir.setReturnValue(0);
            }

            // Boss bar: mesma ideia
            if (type == 2 && BossBar.get() != null && BossBar.get().isEnabled()) {
                BossBar.renderReplacement(new com.solarclient.event.Events.Render2D(
                        new net.minecraft.client.gui.ScaledResolution(mc).getScaledWidth(),
                        new net.minecraft.client.gui.ScaledResolution(mc).getScaledHeight(),
                        0, 0, partialTicks));
                cir.setReturnValue(0);
            }
        }

        /** Fim do render do HUD: a Tab List precisa cobrir a tab original. */
        @Inject(method = "renderGameOverlay", at = @At("TAIL"))
        private void solar$overlays(int type, float partialTicks, CallbackInfoReturnable<Integer> cir) {
            if (type != 0) {
                return;
            }
            Minecraft mc = Minecraft.getMinecraft();
            int width = new net.minecraft.client.gui.ScaledResolution(mc).getScaledWidth();
            int height = new net.minecraft.client.gui.ScaledResolution(mc).getScaledHeight();
            TabList.renderOverlay(mc, width, height, partialTicks);
        }
    }

    // =====================================================================
    // Camada 3D
    // =====================================================================
    @Mixin(EntityRenderer.class)
    public abstract static class EntityRendererMixin {

        /** Fim do desenho do mundo: e aqui que o client desenha em 3D. */
        @Inject(method = "render", at = @At("TAIL"))
        private void solar$render3D(float partialTicks, long nanoTime, CallbackInfo ci) {
            SolarClient.get().onRender3D(new com.solarclient.event.Events.Render3D(partialTicks));
        }

        /** No Hurt Cam: cancela o tremor devolvendo 0. */
        @Inject(method = "hurtCameraEffect", at = @At("HEAD"), cancellable = true)
        private void solar$noHurtCam(float partialTicks, CallbackInfoReturnable<Float> cir) {
            if (NoHurtCam.isActive()) {
                cir.setReturnValue(0.0f);
            }
        }

        /** FullBright: joga a nevoa para longe antes do jogo configurar o fog. */
        @Inject(method = "setupFog", at = @At("HEAD"))
        private void solar$fullBright(int startCoords, float partialTicks, CallbackInfo ci) {
            if (!FullBright.isActive()) {
                return;
            }
            if (FullBright.get().getMode() == 1) {
                GL11.glFogi(GL11.GL_FOG_MODE, GL11.GL_LINEAR);
                GL11.glFogf(GL11.GL_FOG_START, 1000f);
                GL11.glFogf(GL11.GL_FOG_END, 1000f);
            } else if (FullBright.get().getMode() == 2) {
                GlStateManager.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            } else {
                GL11.glFogi(GL11.GL_FOG_MODE, GL11.GL_LINEAR);
                GL11.glFogf(GL11.GL_FOG_END, 1000f);
            }
        }
    }

    // =====================================================================
    // Jogadores: cosmeticos, cape e animacoes 1.7
    // =====================================================================
    @Mixin(RenderPlayer.class)
    public abstract static class RenderPlayerMixin {

        /**
         * Depois que o jogador foi desenhado: e aqui que as capes/wings
         * animadas do CosmeticManager entram por cima.
         */
        @Inject(method = "render", at = @At("TAIL"))
        private void solar$cosmetics(EntityPlayer player, float limbSwing, float limbSwingAmount,
                                     float partialTicks, float rotationYaw, float rotationPitch,
                                     float scale, CallbackInfo ci) {
            CosmeticManager.get().renderFor(player, partialTicks);
        }

        /** Old sneak 1.7: desloca o corpo inteiro ao agachar. */
        @Inject(method = "render", at = @At("HEAD"))
        private void solar$oldSneak(EntityPlayer player, float limbSwing, float limbSwingAmount,
                                    float partialTicks, float rotationYaw, float rotationPitch,
                                    float scale, CallbackInfo ci) {
            if (player != null && player.isSneaking() && OneSevenAnimations.get() != null
                    && OneSevenAnimations.get().oldSneak()) {
                // no 1.7 o agachamento "levanta" o corpo; aqui aplicamos o mesmo
                // deslocamento antes do modelo ser desenhado
                GL11.glPushMatrix();
                GL11.glTranslatef(0f, 0.12f, 0f);
            }
        }

        /** Fecha o push_matrix do old sneak. */
        @Inject(method = "render", at = @At("TAIL"))
        private void solar$oldSneakEnd(EntityPlayer player, float limbSwing, float limbSwingAmount,
                                       float partialTicks, float rotationYaw, float rotationPitch,
                                       float scale, CallbackInfo ci) {
            if (player != null && player.isSneaking() && OneSevenAnimations.get() != null
                    && OneSevenAnimations.get().oldSneak()) {
                GL11.glPopMatrix();
            }
        }
    }

    @Mixin(AbstractClientPlayer.class)
    public abstract static class AbstractClientPlayerMixin {

        /**
         * Esconde a capa original do Minecraft quando o jogador tem uma capa
         * animada do client (evita duas capas sobrepostas).
         */
        @Inject(method = "getLocationCape", at = @At("HEAD"), cancellable = true)
        private void solar$hideVanillaCape(CallbackInfoReturnable<ResourceLocation> cir) {
            AbstractClientPlayer self = (AbstractClientPlayer) (Object) this;
            if (CosmeticManager.get().hasCustomCape(self)) {
                cir.setReturnValue(null);
            }
        }
    }

    @Mixin(net.minecraft.client.model.ModelPlayer.class)
    public abstract static class ModelPlayerMixin {

        /**
         * Blockhit 1.7: depois de o vanilla calcular as rotacoes dos bracos,
         * sobrescrevemos com o vaivem do 1.7.
         */
        @Inject(method = "setRotationAngles", at = @At("TAIL"))
        private void solar$blockhit(float limbSwing, float limbSwingAmount, float partialTicks,
                                   float rotationPitch, float rotationYawHead, float scale,
                                   EntityPlayer player, CallbackInfo ci) {
            if (player == null) {
                return;
            }
            OneSevenAnimations animations = OneSevenAnimations.get();
            if (animations == null) {
                return;
            }
            float angle = OneSevenAnimations.blockhitAngle(player);
            if (angle == 0f) {
                return;
            }
            net.minecraft.client.model.ModelPlayer self =
                    (net.minecraft.client.model.ModelPlayer) (Object) this;
            self.rightArm.rotationX = -angle;
            self.rightArm.rotationZ = 0f;
            if (player.getHeldItem() == null || player.getHeldItem().isEmpty()) {
                self.leftArm.rotationX = angle;
            }
        }
    }

    // =====================================================================
    // Rede: sincronia de cosmeticos e chat
    // =====================================================================
    @Mixin(NetHandlerPlayClient.class)
    public abstract static class NetHandlerPlayClientMixin {

        /** Payload customizado: e o canal "solar|cosmetics". */
        @Inject(method = "handleCustomPayload", at = @At("HEAD"), cancellable = true)
        private void solar$payload(C17PacketCustomPayload packet, CallbackInfo ci) {
            if (CosmeticNetwork.isSolarChannel(packet)) {
                CosmeticNetwork.get().handle(packet);
                ci.cancel();
            }
        }

        /**
         * Chat: entrega o componente ao client. Se o Nick Hider trocar o texto,
         * o original e cancelado e o novo e impresso aqui.
         */
        @Inject(method = "handleChatMessage", at = @At("HEAD"), cancellable = true)
        private void solar$chat(IChatComponent message, CallbackInfo ci) {
            if (message == null) {
                return;
            }
            IChatComponent result = SolarClient.get().onChatIncoming(message);
            if (result == null) {
                ci.cancel();
                return;
            }
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.ingameGUI != null && mc.ingameGUI.getChat() != null) {
                mc.ingameGUI.getChat().addChatMessage(result);
            }
            ci.cancel();
        }
    }

    // =====================================================================
    // Menu principal: logo e botao do client
    // =====================================================================
    @Mixin(net.minecraft.client.gui.GuiMainMenu.class)
    public abstract static class GuiMainMenuMixin {

        /**
         * Desenha por cima do menu principal: logo "SOLAR CLIENT" e um botao
         * que abre o ClickGUI (o clique em si e tratado pelo atalho R+S ou
         * pelo {@code SolarMixin} do GuiScreen - aqui so e o visual).
         */
        @Inject(method = "drawScreen", at = @At("TAIL"))
        private void solar$brand(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
            int width = Minecraft.getMinecraft().displayWidth();
            int height = Minecraft.getMinecraft().displayHeight();

            com.solarclient.ui.Blur.apply(6);
            com.solarclient.ui.RenderUtils.begin2D();
            // escurece a foto de fundo para o logo respirar
            com.solarclient.ui.RenderUtils.gradient(0, 0, width, height,
                    com.solarclient.ui.Colors.argb(120, 6, 7, 12),
                    com.solarclient.ui.Colors.argb(40, 6, 7, 12), true);
            com.solarclient.gui.GuiWidgets.brand(width, height / 2f - 40f);
            com.solarclient.ui.SolarFont.get("Sora", 12, false).drawCentered(
                    "R + S  para abrir o menu", width / 2f, height / 2f - 16f,
                    com.solarclient.ui.Colors.argb(150, 255, 255, 255));
            com.solarclient.ui.RenderUtils.end2D();
        }
    }

    // =====================================================================
    // Blocks: Clear Glass
    // =====================================================================
    @Mixin(BlockRenderLayer.class)
    public abstract static class BlockRenderLayerMixin {

        /**
         * Clear Glass: manda vidro/eterio/barreira de agua para a camada
         * "cutout" (sem transparencia e sem ordenacao).
         */
        @Inject(method = "canRender", at = @At("HEAD"), cancellable = true)
        private void solar$clearGlass(int renderLayer, IBlockState state,
                                      CallbackInfoReturnable<Boolean> cir) {
            if (!ClearGlass.isActive() || state == null) {
                return;
            }
            String name = state.getBlock().getUnlocalizedName();
            int mask = ClearGlass.get().getBlockMask();
            if (name.contains("glass") && (mask & 1) != 0) {
                cir.setReturnValue(true);
            } else if (name.contains("water") && (mask & 4) != 0) {
                cir.setReturnValue(true);
            }
        }
    }

    // =====================================================================
    // Cliente customizado: solta a conexao ao trocar de conta
    // =====================================================================
    @Mixin(RenderGlobal.class)
    public abstract static class RenderGlobalMixin {

        /** Limpa o cache de cosmeticos remotos quando o mundo e descarregado. */
        @Inject(method = "clearEntities", at = @At("HEAD"))
        private void solar$clearCosmetics(CallbackInfo ci) {
            CosmeticManager.get().clearRemote();
        }
    }
}
