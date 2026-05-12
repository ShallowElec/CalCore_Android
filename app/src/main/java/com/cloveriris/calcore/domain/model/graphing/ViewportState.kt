package com.cloveriris.calcore.domain.model.graphing

/**
 * 2D 视口状态（世界坐标系 → 屏幕坐标系的映射）
 *
 * @param centerX 视口中心的世界坐标 x
 * @param centerY 视口中心的世界坐标 y
 * @param scale 每单位世界坐标对应的屏幕像素数（pixels per unit）
 */
data class ViewportState(
    val centerX: Double = 0.0,
    val centerY: Double = 0.0,
    val scale: Double = 50.0
) {
    /**
     * 将世界坐标转换为屏幕坐标（相对于 Canvas 中心）
     */
    fun worldToScreen(worldX: Double, worldY: Double, canvasWidth: Float, canvasHeight: Float): Pair<Float, Float> {
        val screenCenterX = canvasWidth / 2f
        val screenCenterY = canvasHeight / 2f
        val screenX = screenCenterX + ((worldX - centerX) * scale).toFloat()
        val screenY = screenCenterY - ((worldY - centerY) * scale).toFloat() // Y轴向上为正
        return screenX to screenY
    }

    /**
     * 将屏幕坐标（相对于 Canvas 中心）转换为世界坐标
     */
    fun screenToWorld(screenX: Float, screenY: Float, canvasWidth: Float, canvasHeight: Float): Pair<Double, Double> {
        val screenCenterX = canvasWidth / 2f
        val screenCenterY = canvasHeight / 2f
        val worldX = centerX + (screenX - screenCenterX) / scale
        val worldY = centerY - (screenY - screenCenterY) / scale // Y轴向上为正
        return worldX to worldY
    }

    fun withScale(newScale: Double): ViewportState = copy(scale = newScale.coerceIn(MIN_SCALE, MAX_SCALE))
    fun withCenter(newCenterX: Double, newCenterY: Double): ViewportState = copy(centerX = newCenterX, centerY = newCenterY)

    companion object {
        const val MIN_SCALE = 1.0
        const val MAX_SCALE = 2000.0
    }
}
