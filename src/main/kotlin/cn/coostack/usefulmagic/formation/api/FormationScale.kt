package cn.coostack.usefulmagic.formation.api

import net.minecraft.core.BlockPos

/**
 * 阵法规模
 * @param structure 不包括核心(0,0,0)的方块, 用于确认这些位置都是正确的阵法水晶
 */
enum class FormationScale(val structure: List<BlockPos>) {

    NONE(listOf()),


    SMALL(
        listOf(
            BlockPos(2, 0, 0),
            BlockPos(-2, 0, 0),
            BlockPos(0, 0, 2),
            BlockPos(0, 0, -2),

            BlockPos(2, 0, -2),
            BlockPos(-2, 0, -2),
            BlockPos(-2, 0, 2),
            BlockPos(2, 0, 2),
        )
    ),


    MID(
        listOf(
            *SMALL.structure.toTypedArray(),

            BlockPos(0, 0, 4),
            BlockPos(0, 0, -4),
            BlockPos(4, 0, 0),
            BlockPos(-4, 0, 0),

            BlockPos(-3, 0, 2),
            BlockPos(3, 0, 2),
            BlockPos(2, 0, 3),
            BlockPos(-2, 0, 3),
            BlockPos(-3, 0, -2),
            BlockPos(3, 0, -2),
            BlockPos(2, 0, -3),
            BlockPos(-2, 0, -3),
        )
    ),


    LARGE(
        listOf(
            *MID.structure.toTypedArray(),
            BlockPos(-4, 0, -4),
            BlockPos(-4, 0, 4),
            BlockPos(4, 0, -4),
            BlockPos(4, 0, 4),
            BlockPos(6, 0, 0),
            BlockPos(-6, 0, 0),
            BlockPos(0, 0, 6),
            BlockPos(0, 0, -6),
        )
    )
}