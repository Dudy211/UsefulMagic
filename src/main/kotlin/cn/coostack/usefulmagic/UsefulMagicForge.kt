package cn.coostack.usefulmagic

import cn.coostack.usefulmagic.command.UsefulMagicCommands
import cn.coostack.usefulmagic.platform.network.UmNetwork
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraftforge.event.entity.EntityAttributeCreationEvent
import net.minecraftforge.event.RegisterCommandsEvent
import net.minecraftforge.event.TickEvent
import net.minecraftforge.event.entity.player.PlayerEvent
import net.minecraftforge.event.server.ServerStartedEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import thedarkcolour.kotlinforforge.forge.DIST
import thedarkcolour.kotlinforforge.forge.MOD_BUS

/**
 * UsefulMagic 的 Forge 1.20.1 主入口（服务端 + 客户端共用）。
 *
 * 关键：用 KFF 的 `MOD_BUS`，**不能**用 `FMLJavaModLoadingContext.get().modEventBus`
 * —— 后者内部有 `(FMLJavaModLoadingContext) ModLoadingContext.get().extension()` 强转，
 * 而 KFF 注册的是 `KotlinModLoadingContext`，会抛 ClassCastException。
 * （这是 CooParticlesAPI 移植时踩过的坑）
 *
 * `@Mod` 注解需要 mods.toml 里 `modLoader="kotlinforforge"`。
 */
@net.minecraftforge.fml.common.Mod(UsefulMagic.MOD_ID)
object UsefulMagicForge {

    init {
        UsefulMagic.logger.info("UsefulMagic Forge 入口初始化 (dist={})", DIST)


        runCatching { UsefulMagic.init() }
            .onFailure { UsefulMagic.logger.error("UsefulMagic.init 失败", it) }


        runCatching { registerAll() }
            .onFailure { UsefulMagic.logger.error("注册表初始化失败", it) }


        runCatching { UmNetwork.register() }
            .onFailure { UsefulMagic.logger.error("网络通道注册失败", it) }


        if (DIST.isClient) {
            runCatching { UsefulMagicForgeClient.setupClient() }
                .onFailure { UsefulMagic.logger.error("客户端初始化失败", it) }
        }


        MOD_BUS.addListener { event: EntityAttributeCreationEvent ->
            runCatching { cn.coostack.usefulmagic.entity.UsefulMagicEntityAttributes.register(event) }
                .onFailure { UsefulMagic.logger.error("实体属性注册失败", it) }
        }




        runCatching { cn.coostack.usefulmagic.listener.server.UmForgePlayerListener.register() }
            .onFailure { UsefulMagic.logger.error("玩家事件监听注册失败", it) }


        runCatching {
            cn.coostack.usefulmagic.listener.server.UmForgeServerListener.register()

            cn.coostack.usefulmagic.platform.FuelHelper.fuels[
                cn.coostack.usefulmagic.items.UsefulMagicItems.WOODEN_WAND
            ] = 200
        }.onFailure { UsefulMagic.logger.error("燃料监听注册失败", it) }


        thedarkcolour.kotlinforforge.forge.FORGE_BUS.addListener(::onServerStarted)
        thedarkcolour.kotlinforforge.forge.FORGE_BUS.addListener(::onServerTick)
        thedarkcolour.kotlinforforge.forge.FORGE_BUS.addListener(::onClientTick)
        thedarkcolour.kotlinforforge.forge.FORGE_BUS.addListener(::onRegisterCommands)
        thedarkcolour.kotlinforforge.forge.FORGE_BUS.addListener(::onPlayerLoggedOut)
    }

    /**
     * 把 UM 的注册表条目注册到 CP 的 ForgeRegister（它监听 Forge 的 RegisterEvent）。
     *
     * UM 的 `CommonDeferredBlock` 等是各不相同的懒加载持有器，
     * 而 CP 的 `CooRegistry.register` 只接受统一的 `CommonDeferredRegistry<T>`，
     * 所以这里在边界包一层。
     */
    private fun registerAll() {
        val reg = cn.coostack.cooparticlesapi.platform.CooParticlesServices.COO_REGISTRY

        reg.init(MOD_BUS)

        fun <T : Any> reg(registry: net.minecraft.core.Registry<T>, id: net.minecraft.resources.ResourceLocation, v: () -> T) {
            reg.register(
                cn.coostack.cooparticlesapi.platform.registry.CommonDeferredRegistry(registry, id) { v() }
            )
        }

        cn.coostack.usefulmagic.blocks.UsefulMagicBlocks.blocks.forEach {
            reg(BuiltInRegistries.BLOCK, it.id) { it.get() }
        }
        cn.coostack.usefulmagic.blocks.UsefulMagicBlocks.blockItems.forEach {
            reg(BuiltInRegistries.ITEM, it.id) { it.getItem() }
        }
        cn.coostack.usefulmagic.items.UsefulMagicItems.items.forEach {
            reg(BuiltInRegistries.ITEM, it.id) { it.getItem() }
        }
        cn.coostack.usefulmagic.blocks.entity.UsefulMagicBlockEntities.blockEntityTypes.forEach {
            reg(BuiltInRegistries.BLOCK_ENTITY_TYPE, it.id) { it.get() }
        }
        cn.coostack.usefulmagic.entity.UsefulMagicEntityTypes.entityTypes.forEach {
            reg(BuiltInRegistries.ENTITY_TYPE, it.id) { it.get() }
        }
        cn.coostack.usefulmagic.particles.particle.UsefulMagicParticleTypes.particleTypes.forEach {
            reg(BuiltInRegistries.PARTICLE_TYPE, it.id) { it.get() }
        }
        cn.coostack.usefulmagic.effects.UsefulMagicEffects.mobEffects.forEach {
            reg(BuiltInRegistries.MOB_EFFECT, it.id) { it.get() }
        }
        cn.coostack.usefulmagic.sounds.UsefulMagicSoundEvents.soundEvents.forEach {
            reg(BuiltInRegistries.SOUND_EVENT, it.id) { it.get() }
        }
        cn.coostack.usefulmagic.recipe.UsefulMagicRecipeTypes.recipeTypes.forEach {
            reg(BuiltInRegistries.RECIPE_TYPE, it.id) { it.get() }
        }
        cn.coostack.usefulmagic.recipe.UsefulMagicRecipeTypes.recipeSerializer.forEach {
            reg(BuiltInRegistries.RECIPE_SERIALIZER, it.id) { it.get() }
        }
        run {
            val g = cn.coostack.usefulmagic.items.UsefulMagicItemGroups.usefulMagicMainGroup
            reg(BuiltInRegistries.CREATIVE_MODE_TAB, g.id) { g.get() }
        }

        UsefulMagic.logger.info("UsefulMagic 注册表已挂到 Forge RegisterEvent")
    }



    private fun onServerStarted(event: ServerStartedEvent) {
        runCatching { UsefulMagic.setupServer(event.server) }
            .onFailure { UsefulMagic.logger.error("setupServer 失败", it) }
    }

    private fun onServerTick(event: TickEvent.ServerTickEvent) {
        if (event.phase != TickEvent.Phase.END) return
        runCatching { UsefulMagic.tickServer() }
            .onFailure { UsefulMagic.logger.error("tickServer 失败", it) }
    }

    private fun onClientTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.END) return
        runCatching { UsefulMagicClient.tickClient() }
            .onFailure { UsefulMagic.logger.error("tickClient 失败", it) }
    }

    private fun onRegisterCommands(event: RegisterCommandsEvent) {
        runCatching { UsefulMagicCommands.initCommand(event.dispatcher) }
            .onFailure { UsefulMagic.logger.error("命令注册失败", it) }
    }

    private fun onPlayerLoggedOut(event: PlayerEvent.PlayerLoggedOutEvent) {
        val sp = event.entity as? net.minecraft.server.level.ServerPlayer ?: return
        runCatching {
            cn.coostack.usefulmagic.items.weapon.MagicAxe.playerSkills.remove(sp.uuid)
        }.onFailure { UsefulMagic.logger.warn("清理玩家技能失败: {}", it.message) }
    }
}
