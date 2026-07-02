package com.android.purebilibili.core.util

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

private const val QUICK_RETURN_THRESHOLD_MS = 500L
private const val MIN_CARD_VISIBLE_FRACTION_FOR_CONTAINER_TRANSITION = 0.35f

internal fun shouldUseQuickReturnSharedTransitionPolicy(
    detailEnterUptimeMs: Long,
    detailReturnUptimeMs: Long,
    thresholdMs: Long = QUICK_RETURN_THRESHOLD_MS
): Boolean {
    if (detailEnterUptimeMs <= 0L || detailReturnUptimeMs < detailEnterUptimeMs) return false
    return detailReturnUptimeMs - detailEnterUptimeMs <= thresholdMs
}

/**
 *  卡片位置管理器
 * 
 * 用于记录点击卡片的位置，以便在返回动画时
 * 将缩放动画指向正确的卡片位置
 */
object CardPositionManager {
    
    /**
     * 最后点击的卡片边界（在 Root 坐标系中）
     */
    var lastClickedCardBounds: Rect? = null
        private set
    
    /**
     * 最后点击的卡片中心点（归一化坐标 0-1）
     */
    var lastClickedCardCenter: Offset? = null
        private set

    var lastClickedVideoSourceKey: String? = null
        private set

    var lastClickedVideoSourceCornerDp: Int? = null
        private set

    private var lastClickedCardVisibleFraction: Float = 1f
    
    /**
     *  是否是单列卡片（故事卡片）
     * 用于决定导航动画方向：单列用垂直滑动，双列用水平滑动
     */
    var isSingleColumnCard: Boolean = false
        private set
    
    /**
     *  [新增] 是否正在切换分类
     * 用于跳过首页卡片的入场动画，避免切换标签时出现收缩效果
     */
    @Volatile
    var isSwitchingCategory: Boolean = false
    
    /**
     *  [新增] 屏幕密度，用于计算 dp 到 px
     */
    var lastScreenDensity: Float = 3f
        private set

    /**
     * 记录卡片位置
     * @param bounds 卡片在 Root 坐标系中的边界
     * @param screenWidth 屏幕宽度
     * @param screenHeight 屏幕高度
     * @param isSingleColumn 是否是单列卡片（故事卡片）
     * @param density 屏幕密度（可选）
     * @param bottomBarHeightDp 底部导航栏高度（dp），用于裁剪可见区域
     */
    fun recordCardPosition(
        bounds: Rect, 
        screenWidth: Float, 
        screenHeight: Float,
        isSingleColumn: Boolean = false,
        density: Float = 3f,
        bottomBarHeightDp: Float = 80f  //  底部导航栏默认高度
    ) {
        lastClickedVideoSourceKey = null
        lastClickedVideoSourceCornerDp = null
        lastClickedCardBounds = bounds
        lastScreenDensity = density
        isSingleColumnCard = isSingleColumn
        //  [修复] 计算可见区域的底边界（屏幕高度减去底部导航栏）
        val bottomBarHeightPx = bottomBarHeightDp * density
        val visibleBottomPx = screenHeight - bottomBarHeightPx
        
        //  [修复] 计算卡片可见部分的中心点
        // 如果卡片底部被导航栏遮挡，使用可见部分的中心
        val visibleTop = bounds.top
        val visibleBottom = bounds.bottom.coerceAtMost(visibleBottomPx)
        val visibleCenterY = if (visibleBottom > visibleTop) {
            (visibleTop + visibleBottom) / 2
        } else {
            bounds.center.y  // 完全不可见时使用原始中心
        }
        
        // 计算归一化的中心点坐标 (0-1 范围)
        lastClickedCardCenter = Offset(
            x = bounds.center.x / screenWidth,
            y = visibleCenterY / screenHeight  //  使用可见部分的中心 Y
        )
        lastClickedCardVisibleFraction = resolveCardVisibleFraction(
            bounds = bounds,
            viewport = Rect(
                left = 0f,
                top = 0f,
                right = screenWidth,
                bottom = visibleBottomPx.coerceAtLeast(0f)
            )
        )
    }

    fun recordVideoCardPosition(
        bvid: String,
        sourceRoute: String?,
        bounds: Rect,
        screenWidth: Float,
        screenHeight: Float,
        isSingleColumn: Boolean = false,
        density: Float = 3f,
        bottomBarHeightDp: Float = 80f,
        sourceCornerDp: Int? = null
    ) {
        recordCardPosition(
            bounds = bounds,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            isSingleColumn = isSingleColumn,
            density = density,
            bottomBarHeightDp = bottomBarHeightDp
        )
        val normalizedBvid = bvid.trim()
        val normalizedRoute = sourceRoute?.substringBefore("?")?.takeIf { it.isNotBlank() }
        lastClickedVideoSourceKey = if (normalizedBvid.isNotEmpty() && normalizedRoute != null) {
            "$normalizedRoute:$normalizedBvid"
        } else {
            null
        }
        lastClickedVideoSourceCornerDp = sourceCornerDp?.coerceAtLeast(0)
    }
    
    /**
     * 清除记录的位置
     */
    fun clear() {
        lastClickedCardBounds = null
        lastClickedCardCenter = null
        lastClickedVideoSourceKey = null
        lastClickedVideoSourceCornerDp = null
        lastClickedCardVisibleFraction = 1f
    }
    
    /**
     *  卡片水平位置枚举
     */
    enum class CardHorizontalPosition {
        LEFT,   // 左边两个 (0% - 40%)
        MIDDLE, // 中间一个 (40% - 60%)
        RIGHT   // 右边两个 (60% - 100%)
    }

    /**
     *  获取卡片的水平位置区域（针对 5 列布局优化）
     */
    val cardHorizontalPosition: CardHorizontalPosition
        get() {
            val centerX = lastClickedCardCenter?.x ?: 0.5f
            return when {
                centerX < 0.4f -> CardHorizontalPosition.LEFT
                centerX > 0.6f -> CardHorizontalPosition.RIGHT
                else -> CardHorizontalPosition.MIDDLE
            }
        }

    /**
     *  判断最后点击的卡片是否在屏幕左侧
     * 用于小窗入场动画方向
     */
    val isCardOnLeft: Boolean
        get() = (lastClickedCardCenter?.x ?: 0.5f) < 0.5f
    
    /**
     * 判断最近点击卡片是否足够可见。
     * 这里用点击时的可见面积比例，避免固定 header 高度误杀首页上方可见卡片。
     */
    val isCardFullyVisible: Boolean
        get() {
            val bounds = lastClickedCardBounds ?: return true
            if (bounds.width <= 0f || bounds.height <= 0f) return false
            return lastClickedCardVisibleFraction >= MIN_CARD_VISIBLE_FRACTION_FOR_CONTAINER_TRANSITION
        }
}

internal fun resolveCardVisibleFraction(
    bounds: Rect,
    viewport: Rect
): Float {
    val area = bounds.width * bounds.height
    if (area <= 0f) return 0f
    val visibleLeft = maxOf(bounds.left, viewport.left)
    val visibleTop = maxOf(bounds.top, viewport.top)
    val visibleRight = minOf(bounds.right, viewport.right)
    val visibleBottom = minOf(bounds.bottom, viewport.bottom)
    val visibleWidth = (visibleRight - visibleLeft).coerceAtLeast(0f)
    val visibleHeight = (visibleBottom - visibleTop).coerceAtLeast(0f)
    return ((visibleWidth * visibleHeight) / area).coerceIn(0f, 1f)
}
