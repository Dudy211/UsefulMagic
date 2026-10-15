package cn.coostack.usefulmagic.entity

import cn.coostack.usefulmagic.entity.custom.book.MagicBookEntity
import cn.coostack.usefulmagic.entity.custom.dragon.MagicDragonEntity
import cn.coostack.usefulmagic.entity.custom.dragon.eye.MagicEyeEntity
import cn.coostack.usefulmagic.entity.custom.dragon.eye.MagicHeartEntity
import cn.coostack.usefulmagic.entity.custom.dragon.eye.MagicSubEyeEntity
import net.minecraftforge.event.entity.EntityAttributeCreationEvent

/**
 * 实体默认属性注册。
 *
 * 上游用 NeoForge 的 `EntityAttributeCreationEvent`，Forge 1.20.1 同名同形。
 */
object UsefulMagicEntityAttributes {

    fun register(event: EntityAttributeCreationEvent) {
        event.put(
            UsefulMagicEntityTypes.MAGIC_BOOK_ENTITY_TYPE.get(),
            MagicBookEntity.createDefaultMobAttributes().build()
        )
        event.put(
            UsefulMagicEntityTypes.MAGIC_DRAGON_ENTITY_TYPE.get(),
            MagicDragonEntity.createDefaultMobAttributes().build()
        )
        event.put(
            UsefulMagicEntityTypes.MAGIC_EYE_ENTITY_TYPE.get(),
            MagicEyeEntity.createDefaultMobAttributes().build()
        )
        event.put(
            UsefulMagicEntityTypes.MAGIC_SUB_EYE_ENTITY_TYPE.get(),
            MagicSubEyeEntity.createDefaultMobAttributes().build()
        )
        event.put(
            UsefulMagicEntityTypes.MAGIC_HEART_ENTITY_TYPE.get(),
            MagicHeartEntity.createDefaultMobAttributes().build()
        )
        cn.coostack.usefulmagic.UsefulMagic.logger.info("实体默认属性注册完成")
    }
}
