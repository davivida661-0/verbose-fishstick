#!/usr/bin/env node
/**
 * Gera STUBS minimos da API do Minecraft 1.8.9 (LWJGL 2, Mixin, log4j) em
 * tools/verify/stubs. NAO faz parte do mod e nunca entra no jar.
 *
 * Sao stubs de VERIFICACAO: contem apenas os membros que o client usa, para
 * rodar `javac` e pegar erro de tipo/assinatura no nosso codigo sem o jar do
 * Minecraft. O jogo de verdade continua sendo o jar da Mojang (docs/BUILD.md).
 *
 * Uso: node tools/verify/gen-stubs.js && sh tools/verify/verify.sh
 */
const fs = require('fs');
const path = require('path');

const OUT = path.join(__dirname, 'stubs');

function w(pkg, name, body) {
  const dir = path.join(OUT, pkg.split('.').join(path.sep));
  fs.mkdirSync(dir, { recursive: true });
  fs.writeFileSync(path.join(dir, name + '.java'), 'package ' + pkg + ';\n' + body, 'utf8');
}

const MC = 'net.minecraft.';

// ---------------------------------------------------------------- LWJGL 2
w('org.lwjgl.opengl', 'GL11', `
import java.nio.ByteBuffer;
public class GL11 {
  public static final int GL_TEXTURE_2D=3553, GL_BLEND=3042, GL_SRC_ALPHA=770, GL_ONE_MINUS_SRC_ALPHA=771;
  public static final int GL_LIGHTING=2899, GL_DEPTH_TEST=2929, GL_CULL_FACE=2884, GL_SCISSOR_TEST=3089;
  public static final int GL_QUADS=7, GL_TRIANGLES=4, GL_TRIANGLE_FAN=6, GL_LINE_LOOP=2, GL_LINES=1;
  public static final int GL_LINEAR=9729, GL_CLAMP=33071, GL_NEAREST=9728, GL_RGBA=6408, GL_UNSIGNED_BYTE=5121;
  public static final int GL_FOG_MODE=2897, GL_FOG_START=2915, GL_FOG_END=2916, GL_SMOOTH=7425, GL_ONE=1;
  public static final int GL_TEXTURE_MIN_FILTER=9729, GL_TEXTURE_MAG_FILTER=9728, GL_TEXTURE_WRAP_S=10497, GL_TEXTURE_WRAP_T=10498;
  public static final int GL_FRAMEBUFFER=36160, GL_COLOR_ATTACHMENT0=36064, GL_PROJECTION=5889, GL_MODELVIEW=5888;
  public static void glMatrixMode(int mode){}
  public static void glPushMatrix(){} public static void glPopMatrix(){}
  public static void glTranslatef(float a,float b,float c){} public static void glScalef(float a,float b,float c){}
  public static void glRotatef(float a,float b,float c,float d){}
  public static void glColor4f(float a,float b,float c,float d){} public static void glNormal3f(float a,float b,float c){}
  public static void glBegin(int mode){} public static void glEnd(){}
  public static void glVertex2f(float x,float y){} public static void glVertex3f(float x,float y,float z){}
  public static void glTexCoord2f(float u,float v){} public static void glLineWidth(float w){}
  public static void glDisable(int cap){} public static void glEnable(int cap){}
  public static void glBlendFunc(int src,int dst){} public static void glShadeModel(int mode){}
  public static void glScissor(int x,int y,int w,int h){} public static void glViewport(int x,int y,int w,int h){}
  public static void glFlush(){}
  public static int glGenTextures(){return 1;} public static void glBindTexture(int t,int id){}
  public static void glDeleteTextures(int id){}
  public static void glTexParameteri(int t,int pname,int param){}
  public static void glTexImage2D(int t,int l,int internal,int w,int h,int b,int f,int ty,ByteBuffer px){}
  public static void glCopyTexSubImage2D(int t,int l,int x,int y,int fx,int fy,int w,int h){}
  public static int glGenFramebuffers(){return 1;} public static void glBindFramebuffer(int target,int fbo){}
  public static void glDeleteFramebuffers(int fbo){}
  public static void glFramebufferTexture2D(int t,int att,int tt,int id,int l){}
  public static void glFogi(int pname,int param){} public static void glFogf(int pname,float param){}
}`);

w('org.lwjgl.opengl', 'Display', `public class Display { public static int getWidth(){return 1280;} public static int getHeight(){return 720;} }`);
w('org.lwjgl.util.glu', 'GLU', `public class GLU { public static void gluPerspective(float fovy,float aspect,float zNear,float zFar){} }`);
w('org.lwjgl', 'BufferUtils', `import java.nio.ByteBuffer; public class BufferUtils { public static ByteBuffer createByteBuffer(int size){return null;} }`);

w('org.lwjgl.input', 'Keyboard', `
public class Keyboard {
  public static final int KEY_ESCAPE=1, KEY_F1=58, KEY_F2=59, KEY_F3=60, KEY_F4=61, KEY_F5=62, KEY_F6=63,
      KEY_R=19, KEY_V=47, KEY_B=48, KEY_N=49, KEY_M=50,
      KEY_H=36, KEY_G=35, KEY_F=33, KEY_C=46, KEY_P=25, KEY_S=39, KEY_T=54, KEY_O=24,
      KEY_J=36, KEY_L=38, KEY_X=45, KEY_Z=44, KEY_I=23, KEY_U=22, KEY_K=37, KEY_TAB=15;
  public static String getKeyName(int code){return "KEY";}
  public static boolean isCreated(){return true;}
  public static boolean isKeyDown(int code){return false;}
}`);
w('org.lwjgl.input', 'Mouse', `
public class Mouse {
  public static boolean isButtonDown(int b){return false;}
  public static int getX(){return 0;} public static int getY(){return 0;} public static int getDWheel(){return 0;}
}`);

// ---------------------------------------------------------------- log4j
w('org.apache.logging.log4j', 'Logger', `
public class Logger {
  public void info(String m){} public void warn(String m){} public void error(String m){}
  public void error(String m, Throwable t){} public void debug(String m){}
}`);
w('org.apache.logging.log4j', 'LogManager', `public class LogManager { public static Logger getLogger(String n){return new Logger();} }`);

// ---------------------------------------------------------------- Mixin
w('org.spongepowered.asm.mixin', 'Mixin', `
import java.lang.annotation.*;
@Retention(RetentionPolicy.RUNTIME) @Target(ElementType.TYPE)
public @interface Mixin { Class<?>[] value() default {}; }`);
w('org.spongepowered.asm.mixin.injection', 'At', `
import java.lang.annotation.*;
@Retention(RetentionPolicy.RUNTIME) @Target({}) public @interface At { String value(); }`);
w('org.spongepowered.asm.mixin.injection', 'Inject', `
import java.lang.annotation.*;
@Retention(RetentionPolicy.RUNTIME) @Target(ElementType.METHOD)
public @interface Inject { String[] method(); At[] at(); boolean cancellable() default false; }`);
w('org.spongepowered.asm.mixin.injection.callback', 'CallbackInfo', `
public class CallbackInfo { public void cancel(){} }`);
w('org.spongepowered.asm.mixin.injection.callback', 'CallbackInfoReturnable', `
public class CallbackInfoReturnable<T> { public void setReturnValue(T v){} public void cancel(){} }`);

// --- Mixin 0.8.5 (espelha a API real, verificada contra o jar oficial) ---
w('org.spongepowered.asm.launch', 'MixinBootstrap', `public abstract class MixinBootstrap { public static void init(){} }`);
w('org.spongepowered.asm.mixin', 'MixinEnvironment', `
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
public final class MixinEnvironment {
  public enum Phase { PREINIT, INIT, DEFAULT, POSTINIT }
  private static final MixinEnvironment CURRENT = new MixinEnvironment();
  public static MixinEnvironment getCurrentEnvironment(){return CURRENT;}
  public Object getActiveTransformer(){return null;}
  public void setActiveTransformer(IMixinTransformer t){}
  public IContainerHandle getPrimaryContainer(){return null;}
}`);
w('org.spongepowered.asm.mixin', 'Mixins', `
public final class Mixins {
  public static void addConfiguration(String name){}
  public static int getUnvisitedCount(){return 0;}
}`);
w('org.spongepowered.asm.mixin.transformer', 'IMixinTransformer', `
import org.spongepowered.asm.mixin.MixinEnvironment;
public interface IMixinTransformer { byte[] transformClass(MixinEnvironment env, String name, byte[] input); }`);

// --- servico do Mixin (necessario para rodar SEM Forge) ---
// ClassNode e do asm-tree: o verify.sh compila so com gson, entao o stub
// tambem precisa ser local (a assinatura real de transformClass referencia
// ClassNode, por isso o asm precisa estar no classpath de compilacao).
w('org.objectweb.asm.tree', 'ClassNode', `public class ClassNode {}`);
w('org.objectweb.asm', 'ClassReader', `
import org.objectweb.asm.tree.ClassNode;
public class ClassReader {
  public ClassReader(byte[] b){}
  public void accept(ClassNode node, int flags){}
}`);
w('org.spongepowered.asm.launch.platform.container', 'IContainerHandle', `
import java.util.Collection;
public interface IContainerHandle {
  String getAttribute(String name);
  Collection<IContainerHandle> getNestedContainers();
}`);
w('org.spongepowered.asm.launch.platform.container', 'ContainerHandleVirtual', `
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
public class ContainerHandleVirtual implements IContainerHandle {
  private final String name;
  private final List<IContainerHandle> nested = new ArrayList<IContainerHandle>();
  public ContainerHandleVirtual(String name){ this.name = name; }
  public String getName(){ return name; }
  public ContainerHandleVirtual add(IContainerHandle c){ nested.add(c); return this; }
  public String getAttribute(String n){ return null; }
  public Collection<IContainerHandle> getNestedContainers(){ return nested; }
}`);
w('org.spongepowered.asm.service', 'IMixinInternal', `public interface IMixinInternal {}`);
w('org.spongepowered.asm.service', 'IClassProvider', `
import java.net.URL;
public interface IClassProvider {
  URL[] getClassPath();
  Class<?> findClass(String name) throws ClassNotFoundException;
  Class<?> findClass(String name, boolean initialize) throws ClassNotFoundException;
  Class<?> findAgentClass(String name, boolean remap) throws ClassNotFoundException;
}`);
w('org.spongepowered.asm.service', 'IClassBytecodeProvider', `
import org.objectweb.asm.tree.ClassNode;
public interface IClassBytecodeProvider {
  ClassNode getClassNode(String name) throws ClassNotFoundException, java.io.IOException;
  ClassNode getClassNode(String name, boolean remap) throws ClassNotFoundException, java.io.IOException;
}`);
w('org.spongepowered.asm.service', 'ITransformer', `public interface ITransformer {}`);
w('org.spongepowered.asm.service', 'ITransformerProvider', `
import java.util.Collection;
public interface ITransformerProvider {
  Collection<ITransformer> getTransformers();
  Collection<ITransformer> getDelegatedTransformers();
  void addTransformerExclusion(String target);
}`);
w('org.spongepowered.asm.service', 'IClassTracker', `
public interface IClassTracker {
  void registerInvalidClass(String className);
  boolean isClassLoaded(String className);
  String getClassRestrictions(String className);
}`);
w('org.spongepowered.asm.service', 'IMixinAuditTrail', `
public interface IMixinAuditTrail {
  void onApply(String targetClassName, String mixinClassName);
  void onPostProcess(String className);
  void onGenerate(String className, String sourceClass);
}`);
w('org.spongepowered.asm.service', 'IPropertyKey', `public interface IPropertyKey {}`);
w('org.spongepowered.asm.service', 'IGlobalPropertyService', `
public interface IGlobalPropertyService {
  IPropertyKey resolveKey(String name);
  <T> T getProperty(IPropertyKey key);
  <T> T getProperty(IPropertyKey key, T defaultValue);
  void setProperty(IPropertyKey key, Object value);
  String getPropertyString(IPropertyKey key, String defaultValue);
}`);
w('org.spongepowered.asm.service', 'IMixinService', `
import java.io.InputStream;
import java.util.Collection;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.mixin.MixinEnvironment;
public interface IMixinService {
  String getName();
  boolean isValid();
  MixinEnvironment.Phase getInitialPhase();
  void offer(IMixinInternal internal);
  void init();
  void beginPhase();
  void checkEnv(Object obj);
  Object getReEntranceLock();
  IClassProvider getClassProvider();
  IClassBytecodeProvider getBytecodeProvider();
  ITransformerProvider getTransformerProvider();
  IClassTracker getClassTracker();
  IMixinAuditTrail getAuditTrail();
  Collection<String> getPlatformAgents();
  IContainerHandle getPrimaryContainer();
  Collection<IContainerHandle> getMixinContainers();
  InputStream getResourceAsStream(String name);
  String getSideName();
  Object getLogger(String name);
}`);
w('org.spongepowered.asm.service', 'MixinServiceAbstract', `
import java.io.InputStream;
import java.util.Collection;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.mixin.MixinEnvironment;
public abstract class MixinServiceAbstract implements IMixinService {
  public void prepare(){}
  public MixinEnvironment.Phase getInitialPhase(){ return MixinEnvironment.Phase.PREINIT; }
  public void offer(IMixinInternal internal){}
  public void init(){}
  public void beginPhase(){}
  public void checkEnv(Object obj){}
  public Object getReEntranceLock(){ return null; }
  public Collection<IContainerHandle> getMixinContainers(){ return java.util.Collections.emptyList(); }
  public String getSideName(){ return "UNKNOWN"; }
  public Object getLogger(String name){ return null; }
}`);

// ---------------------------------------------------------------- Minecraft
w(MC + 'util', 'IChatComponent', `public interface IChatComponent { String getUnformattedText(); String getFormattedText(); }`);
w(MC + 'util', 'ChatComponentText', `
import net.minecraft.util.IChatComponent;
public class ChatComponentText implements IChatComponent {
  public ChatComponentText(String text){}
  public String getUnformattedText(){return "";} public String getFormattedText(){return "";}
}`);
w(MC + 'util', 'ResourceLocation', `public class ResourceLocation { public ResourceLocation(String s){} }`);
w(MC + 'util', 'SoundEvent', `public class SoundEvent {}`);
w(MC + 'util.math', 'BlockPos', `public class BlockPos { public BlockPos(int x,int y,int z){} }`);

w(MC + 'entity', 'Entity', `
import java.util.UUID;
import net.minecraft.util.IChatComponent;
public class Entity {
  public double posX,posY,posZ,lastTickPosX,lastTickPosY,lastTickPosZ;
  public float rotationYaw,rotationPitch;
  public boolean isDead;
  public boolean isInvisible(){return false;}
  public double getDistanceToEntity(Entity e){return 0;}
  public IChatComponent getDisplayName(){return null;}
  public String getName(){return "";}
  public UUID getUniqueID(){return null;}
  public int getEntityId(){return 0;}
}`);
w(MC + 'entity', 'EntityLivingBase', `
import java.util.Collection;
import net.minecraft.entity.Entity;
import net.minecraft.potion.PotionEffect;
public class EntityLivingBase extends Entity {
  public int swingItem;
  public int hurtTime;
  public float getHealth(){return 20;} public float getMaxHealth(){return 20;}
  public float getEyeHeight(){return 1.62f;}
  public Collection<PotionEffect> getActivePotionEffects(){return null;}
  public boolean isSneaking(){return false;} public boolean isSprinting(){return false;}
  public void setSprinting(boolean v){}
}`);
w(MC + 'entity.player', 'EntityPlayer', `
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IChatComponent;
public class EntityPlayer extends EntityLivingBase {
  public EntityPlayerInventory inventory;
  public ItemStack getHeldItem(){return null;}
  public int getTotalArmorValue(){return 0;}
  public String getClientBrand(){return "vanilla";}
  public IChatComponent getDisplayName(){return null;}
  public void sendChatMessage(String m){}
}`);
w(MC + 'entity.player', 'AbstractClientPlayer', `
import net.minecraft.util.ResourceLocation;
public class AbstractClientPlayer extends EntityPlayer { public ResourceLocation getLocationCape(){return null;} }`);
w(MC + 'entity.player', 'EntityPlayerInventory', `public class EntityPlayerInventory { public net.minecraft.item.ItemStack[] armorInventory = new net.minecraft.item.ItemStack[4]; }`);

w(MC + 'item', 'Item', `public class Item { public String getUnlocalizedName(){return "";} }`);
w(MC + 'item', 'ItemStack', `
import net.minecraft.util.IChatComponent;
public class ItemStack {
  public int stackSize;
  public boolean isEmpty(){return true;}
  public Item getItem(){return null;}
  public int getItemDamage(){return 0;}
  public int getMaxDamage(){return 0;}
  public boolean isItemStackDamageable(){return false;}
  public IChatComponent getDisplayName(){return null;}
  public String getUnlocalizedName(){return "";}
}`);
w(MC + 'potion', 'Potion', `public class Potion { public String getName(){return "";} }`);
w(MC + 'potion', 'PotionEffect', `
import net.minecraft.potion.Potion;
public class PotionEffect { public Potion getPotion(){return null;} public int getDuration(){return 0;} public int getAmplifier(){return 0;} }`);

w(MC + 'scoreboard', 'Scoreboard', `
import java.util.Collection;
public class Scoreboard {
  public ScoreObjective getObjectiveInDisplaySlot(int slot){return null;}
  public Collection<Score> getScores(ScoreObjective o){return null;}
  public Collection<ScoreTeam> getTeams(){return null;}
}`);
w(MC + 'scoreboard', 'ScoreObjective', `import net.minecraft.util.IChatComponent; public class ScoreObjective { public IChatComponent getDisplayName(){return null;} }`);
w(MC + 'scoreboard', 'Score', `public class Score { public String getScoreName(){return "";} public int getScorePoints(){return 0;} }`);
w(MC + 'scoreboard', 'ScoreTeam', `
import java.util.Collection;
import net.minecraft.util.IChatComponent;
public class ScoreTeam {
  public IChatComponent getDisplayName(){return null;}
  public IChatComponent getPrefix(){return null;}
  public Collection<String> getMembership(){return null;}
}`);

w(MC + 'block.state', 'IBlockState', `import net.minecraft.block.Block; public interface IBlockState { Block getBlock(); }`);
w(MC + 'block', 'Block', `public class Block { public String getUnlocalizedName(){return "";} }`);

w(MC + 'init', 'SoundEvents', `import net.minecraft.util.SoundEvent; public class SoundEvents { public static final SoundEvent BLOCK_NOTE_PLING = new SoundEvent(); }`);

w(MC + 'client', 'Session', `public class Session { public String getUsername(){return "";} public String getPlayerID(){return "";} }`);

w(MC + 'client.gui', 'FontRenderer', `
public class FontRenderer {
  public int getStringWidth(String s){return 0;}
  public void drawString(String s,float x,float y,int c,boolean shadow){}
  public void drawStringWithShadow(String s,float x,float y,int c){}
}`);
w(MC + 'client.gui', 'ScaledResolution', `
import net.minecraft.client.Minecraft;
public class ScaledResolution {
  public ScaledResolution(Minecraft mc){}
  public int getScaledWidth(){return 854;} public int getScaledHeight(){return 480;}
}`);
w(MC + 'client.gui', 'GuiScreen', `
import java.io.IOException;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
public class GuiScreen {
  public Minecraft mc; public int width, height; public FontRenderer fontRenderer;
  public List<net.minecraft.client.gui.GuiButton> buttonList;
  public void initGui(){}
  public void drawScreen(int mouseX,int mouseY,float partial){}
  public void keyTyped(char c,int k) throws IOException {}
  public void handleMouseInput(){}
  public void updateScreen(){}
  public void mouseClicked(int x,int y,int b){}
  public void mouseReleased(int x,int y,int b){}
  public boolean isCtrlKeyDown(){return false;}
  public boolean doesGuiPauseGame(){return true;}
}`);
w(MC + 'client.gui', 'GuiButton', `public class GuiButton {}`);
w(MC + 'client.gui', 'GuiNewChat', `import net.minecraft.util.IChatComponent; public class GuiNewChat { public void addChatMessage(IChatComponent c){} }`);
w(MC + 'client.gui', 'GuiMainMenu', `
import net.minecraft.client.gui.GuiScreen;
public class GuiMainMenu extends GuiScreen {
  public void drawScreen(int mouseX,int mouseY,float partial){}
}`);
w(MC + 'client.gui', 'GuiIngame', `
import net.minecraft.client.gui.GuiScreen;
public class GuiIngame extends GuiScreen {
  public int renderGameOverlay(int type,float partialTicks){return 0;}
  public GuiNewChat getChat(){return null;}
}`);

w(MC + 'client.entity', 'EntityPlayerSP', `
import net.minecraft.entity.player.EntityPlayer;
public class EntityPlayerSP extends EntityPlayer {}`);

w(MC + 'client.multiplayer', 'WorldClient', `
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
public class WorldClient {
  public List<Entity> loadedEntityList;
  public Scoreboard getScoreboard(){return null;}
  public long getWorldTime(){return 0;} public long getTotalWorldTime(){return 0;}
  public String getBiomeName(BlockPos pos){return "";}
  public void playSound(EntityPlayer p,SoundEvent s,float v,float pitch){}
}`);

w(MC + 'client.network', 'NetworkPlayerInfo', `
import com.mojang.authlib.GameProfile;
public class NetworkPlayerInfo { public GameProfile getGameProfile(){return null;} public int getLatency(){return 0;} }`);
w('com.mojang.authlib', 'GameProfile', `public class GameProfile { public String getName(){return "";} public String getId(){return "";} }`);

w(MC + 'client.settings', 'KeyBinding', `
public class KeyBinding {
  public int keyCode; public String displayName;
  public KeyBinding(String description,int keyCode,String category){this.keyCode=keyCode;this.displayName=description;}
  public boolean isPressed(){return false;} public boolean isKeyDown(){return false;}
  public static void setKeyBindState(int key,boolean pressed){}
}`);
w(MC + 'client.settings', 'GameSettings', `
public class GameSettings {
  public KeyBinding keyBindForward,keyBindLeft,keyBindBack,keyBindRight,keyBindJump,
      keyBindSneak,keyBindSprint,keyBindAttack,keyBindUseItem,keyBindPlayerList;
  public int guiScale, thirdPersonView;
}`);

w(MC + 'client.particle', 'EffectRenderer', `public class EffectRenderer {}`);
w(MC + 'client.resources', 'ResourcePack', `public class ResourcePack {}`);
w(MC + 'client.resources', 'ResourcePackManager', `public class ResourcePackManager {}`);
w(MC + 'client.renderer.texture', 'TextureManager', `public class TextureManager {}`);
w(MC + 'client.renderer.texture', 'TextureMap', `import java.util.Map; public class TextureMap { public Map<java.lang.Object,java.lang.Object> map; }`);

w(MC + 'client.renderer', 'GlStateManager', `public class GlStateManager { public static void glBlendFunc(int s,int d){} public static void color(float r,float g,float b,float a){} }`);
w(MC + 'client.renderer', 'BlockRenderLayer', `import net.minecraft.block.state.IBlockState; public class BlockRenderLayer { public static boolean canRender(int layer,IBlockState state){return false;} }`);
w(MC + 'client.renderer', 'RenderGlobal', `public class RenderGlobal { public void clearEntities(){} }`);
w(MC + 'client.renderer', 'EntityRenderer', `
public class EntityRenderer {
  public void render(float partialTicks,long nanoTime){}
  public float hurtCameraEffect(float partialTicks){return 0f;}
  public void setupFog(int startCoords,float partialTicks){}
}`);
w(MC + 'client.renderer.entity', 'RenderManager', `
import net.minecraft.entity.Entity;
public class RenderManager {
  public double renderPosX,renderPosY,renderPosZ;
  public void updateCameraPosition(float f){}
  public void cacheActiveRenderInfo(Entity e,boolean b1,boolean b2,int view,boolean b3){}
  public void renderEntity(Entity e,double x,double y,double z,float yaw,float pitch,int flags){}
}`);
w(MC + 'client.renderer.entity', 'RenderPlayer', `
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.player.EntityPlayer;
public class RenderPlayer extends RenderManager {
  public void render(EntityPlayer player,float limbSwing,float limbSwingAmount,float partialTicks,
                     float rotationYaw,float rotationPitch,float scale){}
}`);

w(MC + 'client.model', 'ModelRenderer', `public class ModelRenderer { public float rotationX,rotationY,rotationZ; }`);
w(MC + 'client.model', 'ModelPlayer', `
import net.minecraft.entity.player.EntityPlayer;
public class ModelPlayer {
  public ModelRenderer rightArm = new ModelRenderer();
  public ModelRenderer leftArm = new ModelRenderer();
  public void setRotationAngles(float a,float b,float c,float d,float e,float f,EntityPlayer p){}
}`);

w(MC + 'network.play.client', 'C17PacketCustomPayload', `
public class C17PacketCustomPayload {
  public C17PacketCustomPayload(String channel,byte[] data){}
  public String getChannelName(){return "";} public byte[] getBuffer(){return new byte[0];}
}`);
w(MC + 'network.play.client', 'NetHandlerPlayClient', `
import java.util.Collection;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.IChatComponent;
public class NetHandlerPlayClient {
  public NetworkPlayerInfo getPlayerInfo(String name){return null;}
  public Collection<NetworkPlayerInfo> getPlayerInfo(){return null;}
  public void addToSendQueue(Object packet){}
  public void handleCustomPayload(C17PacketCustomPayload packet){}
  public void handleChatMessage(IChatComponent message){}
}`);

w(MC + 'client', 'Minecraft', `
import java.util.List;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.resources.ResourcePackManager;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.network.play.client.NetHandlerPlayClient;
public class Minecraft {
  public EntityPlayerSP thePlayer; public WorldClient theWorld;
  public FontRenderer fontRenderer; public GameSettings gameSettings;
  public GuiIngame ingameGUI; public GuiScreen currentScreen;
  public static Minecraft getMinecraft(){return null;}
  public EntityPlayerSP getRenderViewEntity(){return thePlayer;}
  public int getDisplayWidth(){return 854;} public int getDisplayHeight(){return 480;}
  public int displayWidth(){return 854;} public int displayHeight(){return 480;}
  public NetHandlerPlayClient getNetHandler(){return null;}
  public Session getSession(){return null;}
  public EffectRenderer getEffectRenderer(){return null;}
  public TextureManager getTextureManager(){return null;}
  public ResourcePackManager getResourcePackManager(){return null;}
  public void displayGuiScreen(GuiScreen screen){}
  public void runTick(){} public void shutdown(){}
}`);

console.log('stubs gerados em ' + OUT);
