package cn.coostack.usefulmagic.states

import cn.coostack.usefulmagic.UsefulMagic
import cn.coostack.usefulmagic.profile.MagicPlayerData
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.Level
import net.minecraft.world.level.saveddata.SavedData
import java.util.UUID

/**
 * 1.20.1 移植改动：
 *  - `SavedData.save(nbt, RegistryAccess)` → 1.20.1 的 `save(nbt)`（1 参）
 *  - `SavedData.Factory<T>` 不存在 → 用 `computeIfAbsent(Function, Supplier, name)`
 */
class ManaServerState : SavedData() {

    val magicPlayerData = HashMap<UUID, MagicPlayerData>()

    fun getDataFromServer(uuid: UUID): MagicPlayerData {
        return magicPlayerData.getOrPut(uuid) { MagicPlayerData(uuid) }
    }

    fun sendToggle() {
        setDirty()
    }

    /** 1.20.1 的存盘（1 参） */
    override fun save(nbt: CompoundTag): CompoundTag {
        val playersNBT = CompoundTag()
        magicPlayerData.forEach {
            val key = it.key.toString()
            val value = it.value
            val dataNbt = CompoundTag()
            val friends = CompoundTag()
            value.friends.forEachIndexed { index, f ->
                friends.putUUID("$index", f)
            }
            dataNbt.put("friends", friends)
            val friendlySettings = CompoundTag()
            friendlySettings.putBoolean("hostile", value.treatHostileAsFriend)
            friendlySettings.putBoolean("neutral", value.treatNeutralAsFriend)
            friendlySettings.putBoolean("non_friend_player", value.treatNonFriendPlayerAsFriend)
            friendlySettings.putBoolean("friend_player", value.treatFriendPlayerAsFriend)
            friendlySettings.putBoolean("animal", value.treatAnimalAsFriend)
            friendlySettings.putBoolean("friendly_mob", value.treatFriendlyMobAsFriend)
            dataNbt.put("friendly_settings", friendlySettings)
            playersNBT.put(key, dataNbt)
        }
        nbt.put("players", playersNBT)
        return nbt
    }

    companion object {
        /** 从 NBT 载入 */
        @JvmStatic
        fun load(nbt: CompoundTag): ManaServerState {
            val loader = ManaServerState()
            val players = nbt.getCompound("players")
            players.allKeys.forEach { key ->
                val uuid = UUID.fromString(key)
                val value = players.getCompound(key)
                val data = MagicPlayerData(uuid)
                data.apply {
                    val friendsNBT = value.getCompound("friends")
                    friendsNBT.allKeys.forEach { index ->
                        val fu = friendsNBT.getUUID(index)
                        data.addFriend(fu)
                    }
                    if (value.contains("friendly_settings")) {
                        val settings = value.getCompound("friendly_settings")
                        updateFriendlySettings(
                            settings.getBoolean("hostile"),
                            if (settings.contains("neutral")) settings.getBoolean("neutral") else false,
                            settings.getBoolean("non_friend_player"),
                            if (settings.contains("friend_player")) settings.getBoolean("friend_player") else true,
                            settings.getBoolean("animal"),
                            if (settings.contains("friendly_mob")) settings.getBoolean("friendly_mob") else false,
                        )
                    }
                }
                loader.magicPlayerData[uuid] = data
            }
            return loader
        }

        @JvmStatic
        fun getFromState(server: MinecraftServer): ManaServerState {
            val pm = server.getLevel(Level.OVERWORLD)!!.dataStorage

            val state = pm.computeIfAbsent(
                { tag -> load(tag) },
                { ManaServerState() },
                UsefulMagic.MOD_ID,
            )
            state.setDirty()
            return state
        }
    }
}
