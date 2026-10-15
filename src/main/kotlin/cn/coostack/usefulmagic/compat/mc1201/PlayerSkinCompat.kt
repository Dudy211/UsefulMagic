package cn.coostack.usefulmagic.compat.mc1201

import com.mojang.authlib.GameProfile
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.DefaultPlayerSkin
import net.minecraft.resources.ResourceLocation

/**
 * # 1.20.1 兼容层：PlayerSkin
 *
 * 1.20.5+ 引入 `PlayerSkin`（record：纹理 + 模型类型 + 斗篷），
 * 由 `SkinManager.get(profile)` 异步返回。
 *
 * 1.20.1 没有这个类，等价信息：
 *   - 皮肤纹理：`DefaultPlayerSkin.getDefaultSkin(uuid)` 或 `SkinManager` 的回调
 *   - 模型类型：`GameProfile` 的 "model" 属性（"slim" / "default"）
 *
 * 这里提供同形 API。
 */
class PlayerSkinCompat(
    val texture: ResourceLocation,
    val model: Model = Model.WIDE,
) {
    /** 1.20.5+ 的 `PlayerSkin.Model` */
    enum class Model {
        WIDE, SLIM;

        /** 1.20.1: 模型类型就是字符串 */
        fun id(): String = if (this == SLIM) "slim" else "default"
    }

    /** 1.20.5+ 的 `skin.model()` */
    fun model(): Model = model

    /** 1.20.5+ 的 `skin.texture()` */
    fun texture(): ResourceLocation = texture

    companion object {
        /** 从 GameProfile 构造（1.20.1 无异步皮肤，用默认皮肤兜底） */
        @JvmStatic
        fun fromProfile(profile: GameProfile?): PlayerSkinCompat {
            val uuid = profile?.id ?: java.util.UUID(0, 0)
            val defaultTex = DefaultPlayerSkin.getDefaultSkin(uuid)
            val modelStr = profile?.properties?.get("textures")
                ?.firstOrNull()?.value
                ?.let { _ -> profile?.properties?.get("model")?.firstOrNull()?.value }
            val model = if (modelStr == "slim") Model.SLIM else Model.WIDE
            return PlayerSkinCompat(defaultTex, model)
        }

        /** 从 UUID 构造 */
        @JvmStatic
        fun fromUuid(uuid: java.util.UUID): PlayerSkinCompat =
            PlayerSkinCompat(DefaultPlayerSkin.getDefaultSkin(uuid), Model.WIDE)

        /** 当前玩家的皮肤 */
        @JvmStatic
        fun ofLocalPlayer(): PlayerSkinCompat? {
            val p = Minecraft.getInstance().player ?: return null
            return fromProfile(p.gameProfile)
        }
    }
}

/** typealias 让调用点零改动 */
typealias PlayerSkin = PlayerSkinCompat
