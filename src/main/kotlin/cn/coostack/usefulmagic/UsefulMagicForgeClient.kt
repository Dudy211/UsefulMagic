package cn.coostack.usefulmagic

import cn.coostack.usefulmagic.blocks.UsefulMagicBlocks
import cn.coostack.usefulmagic.blocks.entity.AltarBlockCoreEntityRenderer
import cn.coostack.usefulmagic.blocks.entity.AltarBlockEntityRenderer
import cn.coostack.usefulmagic.blocks.entity.MagicCoreBlockEntityRenderer
import cn.coostack.usefulmagic.blocks.entity.UsefulMagicBlockEntities
import cn.coostack.usefulmagic.blocks.entity.formation.renderer.CrystalEntityRenderer
import cn.coostack.usefulmagic.client.tooltip.LoadedMagicClientTooltip
import cn.coostack.usefulmagic.client.tooltip.SpellBagClientTooltip
import cn.coostack.usefulmagic.entity.MagicBookEntityModel
import cn.coostack.usefulmagic.entity.UsefulMagicEntityLayers
import cn.coostack.usefulmagic.entity.UsefulMagicEntityTypes
import cn.coostack.usefulmagic.entity.custom.renderer.*
import cn.coostack.usefulmagic.items.UsefulMagicDataComponentTypes.LARGE_REVIVE_USE_COUNT
import cn.coostack.usefulmagic.compat.mc1201.getComponent
import cn.coostack.usefulmagic.items.UsefulMagicItems
import cn.coostack.usefulmagic.items.consumer.LargeManaRevive
import cn.coostack.usefulmagic.items.prop.SpellBagItem
import cn.coostack.usefulmagic.items.prop.SpellBagTooltip
import cn.coostack.usefulmagic.items.weapon.wands.LoadedMagicTooltip
import cn.coostack.usefulmagic.listener.client.UsefulMagicHud
import cn.coostack.usefulmagic.particles.particle.UsefulMagicParticleTypes
import cn.coostack.usefulmagic.particles.particle.WaveParticleProvider
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.KeyMapping
import net.minecraft.client.renderer.ItemBlockRenderTypes
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers
import net.minecraft.client.renderer.item.ItemProperties
import net.minecraft.client.resources.model.ModelResourceLocation
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraftforge.client.event.EntityRenderersEvent
import net.minecraftforge.client.event.ModelEvent
import net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent
import net.minecraftforge.client.event.RegisterKeyMappingsEvent
import net.minecraftforge.client.event.RegisterParticleProvidersEvent
import org.lwjgl.glfw.GLFW

/**
 * UsefulMagic 的 Forge 1.20.1 客户端初始化。
 *
 * 1.20.1 的 `@Mod` 没有 dist 参数，客户端分流由主类用 KFF 的 `DIST` 判断后调用本类。
 * 本类由 [UsefulMagicForge] 在客户端环境调用，再往 MOD_BUS 上挂各注册事件。
 */
object UsefulMagicForgeClient {

    /** 由主类在客户端环境调用 */
    fun setupClient() {
        UsefulMagic.logger.info("UsefulMagic Forge 客户端初始化")


        val bus = thedarkcolour.kotlinforforge.forge.MOD_BUS
        bus.addListener(::onKeyMappings)
        bus.addListener(::onRegisterLayerDefinitions)
        bus.addListener(::onRegisterRenderers)
        bus.addListener(::onRegisterParticleProviders)
        bus.addListener(::onRegisterTooltipFactories)
        bus.addListener(::onRegisterAdditionalModels)
        bus.addListener(::onClientSetup)





        runCatching { UsefulMagicHud.register() }
            .onFailure { UsefulMagic.logger.error("HUD 监听注册失败", it) }
        UsefulMagic.logger.info("HUD 渲染监听已注册")

        runCatching { UsefulMagicClient.init() }
            .onFailure { UsefulMagic.logger.error("UsefulMagicClient.init 失败", it) }
    }



    private fun onClientSetup(event: net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent) {
        event.enqueueWork {
            runCatching {
                loadModelPredicate()
                handleBlockLayer()
            }.onFailure { UsefulMagic.logger.error("客户端 setup 失败", it) }
        }
    }

    private fun onKeyMappings(event: RegisterKeyMappingsEvent) {
        val binding = KeyMapping(
            "key.friend_ui.open",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_J,
            UsefulMagicClient.KEY_CATEGORY
        )
        event.register(binding)
        UsefulMagicClient.loadKeyBindings(binding)
        UsefulMagic.logger.info("按键绑定注册完成")
    }

    /**
     * 实体模型层（`ModelLayerLocation`）注册。
     *
     * ⚠️ **1.20.1 Forge 必须走这个事件**：`EntityRendererProvider.Context.bakeLayer(layer)`
     * 只是去 `EntityModelSet` 里查表，**没有任何「注册」语义** —— 如果 layer 没在
     * `EntityRenderersEvent.RegisterLayerDefinitions` 里登记过，就会抛：
     *
     * ```
     * IllegalArgumentException: Failed to create model for usefulmagic:magic_book_entity
     *   Caused by: No model for layer usefulmagic:magic_book_entity#main
     * ```
     *
     * 注意 **不要**挂到 `ModelEvent.RegisterAdditional` 上 —— 那个事件是给
     * **额外的 block/item 模型** 用的，与实体 layer 无关（之前这里就是挂错了，
     * 于是 `MagicBookEntityRenderer` 的 `bakeLayer` 永远失败）。
     */
    private fun onRegisterLayerDefinitions(event: EntityRenderersEvent.RegisterLayerDefinitions) {
        event.registerLayerDefinition(UsefulMagicEntityLayers.MAGIC_BOOK_ENTITY_LAYER) {
            MagicBookEntityModel.createBodyLayer()
        }
        UsefulMagic.logger.info("实体模型层注册完成")
    }

    private fun onRegisterRenderers(event: EntityRenderersEvent.RegisterRenderers) {
        val t = UsefulMagicEntityTypes
        event.registerEntityRenderer(t.MAGIC_DRAGON_ENTITY_TYPE.get(), ::MagicDragonRenderer)
        event.registerEntityRenderer(t.MAGIC_EYE_ENTITY_TYPE.get(), ::MagicEyeEntityRenderer)
        event.registerEntityRenderer(t.MAGIC_SUB_EYE_ENTITY_TYPE.get(), ::MagicSubEyeEntityRenderer)
        event.registerEntityRenderer(t.MAGIC_HEART_ENTITY_TYPE.get(), ::MagicHeartEntityRenderer)
        event.registerEntityRenderer(t.MAGIC_BOOK_ENTITY_TYPE.get(), ::MagicBookEntityRenderer)

        val be = UsefulMagicBlockEntities
        event.registerBlockEntityRenderer(be.ALTAR_BLOCK.get()) { AltarBlockEntityRenderer() }
        event.registerBlockEntityRenderer(be.ALTAR_BLOCK_CORE.get()) { AltarBlockCoreEntityRenderer() }
        event.registerBlockEntityRenderer(be.MAGIC_CORE.get()) { MagicCoreBlockEntityRenderer() }
        event.registerBlockEntityRenderer(be.DEFEND_CRYSTAL.get()) { CrystalEntityRenderer() }
        event.registerBlockEntityRenderer(be.SWORD_ATTACK_CRYSTAL.get()) { CrystalEntityRenderer() }
        event.registerBlockEntityRenderer(be.RECOVER_CRYSTAL.get()) { CrystalEntityRenderer() }
        event.registerBlockEntityRenderer(be.ENERGY_CRYSTAL.get()) { CrystalEntityRenderer() }

        UsefulMagic.logger.info("实体 / 方块实体渲染器注册完成")
    }

    private fun onRegisterParticleProviders(event: RegisterParticleProvidersEvent) {
        event.registerSpriteSet(
            UsefulMagicParticleTypes.WAVE_PARTICLE.get(),
            ::WaveParticleProvider
        )
        UsefulMagic.logger.info("粒子 provider 注册完成")
    }

    private fun onRegisterTooltipFactories(event: RegisterClientTooltipComponentFactoriesEvent) {
        event.register(LoadedMagicTooltip::class.java) { LoadedMagicClientTooltip(it.magicStack) }
        event.register(SpellBagTooltip::class.java) { SpellBagClientTooltip(it) }
        UsefulMagic.logger.info("tooltip 组件工厂注册完成")
    }

    /**
     * 额外的 block/item 模型。
     *
     * ⚠️ 这里**不是**注册实体模型层的地方（实体 layer 走
     * [onRegisterLayerDefinitions] / `EntityRenderersEvent.RegisterLayerDefinitions`）。
     */
    private fun onRegisterAdditionalModels(event: ModelEvent.RegisterAdditional) {

    }



    private fun loadModelPredicate() {
        ItemProperties.register(
            UsefulMagicItems.LARGE_MANA_REVIVE.getItem(),
            ResourceLocation.withDefaultNamespace("use_count")
        ) { stack: ItemStack, _: Level?, _: LivingEntity?, _: Int ->
            val count = stack.getComponent(LARGE_REVIVE_USE_COUNT.get()) ?: 0
            if (count <= 0) 0f
            else 1f - (count.toFloat() / LargeManaRevive.MAX_USAGE)
        }
        ItemProperties.register(
            UsefulMagicItems.SPELL_BAG.getItem(),
            ResourceLocation.withDefaultNamespace("open")
        ) { stack: ItemStack, _: Level?, _: LivingEntity?, _: Int ->
            if (SpellBagItem.hasMagicContents(stack)) 0f else 1f
        }
    }

    private fun handleBlockLayer() {
        val cutoutBlocks = listOf(
            UsefulMagicBlocks.DEFEND_CRYSTAL_BLOCK.get(),
            UsefulMagicBlocks.SWORD_ATTACK_CRYSTAL_BLOCK.get(),
            UsefulMagicBlocks.RECOVER_CRYSTAL_BLOCK.get(),
            UsefulMagicBlocks.ENERGY_CRYSTAL_BLOCK.get(),
            UsefulMagicBlocks.FORMATION_CORE_BLOCK.get(),
            UsefulMagicBlocks.ALTAR_BLOCK.get(),
        )
        cutoutBlocks.forEach { ItemBlockRenderTypes.setRenderLayer(it, RenderType.cutout()) }
    }
}
