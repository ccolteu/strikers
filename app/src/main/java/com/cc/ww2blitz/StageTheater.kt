package com.cc.ww2blitz

import android.content.res.AssetManager
import android.graphics.Bitmap

/** Live floor / canopy / briefing / theater skins for the bound [StageDef]. */
class StageTheater {
  var def: StageDef = StageCatalog.get(1)
    private set
  var floor: Bitmap? = null
    private set
  var mid: Bitmap? = null
    private set
  var high: Bitmap? = null
    private set
  var canopy: Bitmap? = null
    private set
  var floorAlt: Bitmap? = null
    private set
  var briefing: Bitmap? = null
    private set
  var skinTank: Bitmap? = null
    private set
  var skinDestroyer: Bitmap? = null
    private set
  var skinWagon: Bitmap? = null
    private set
  var skinHelicopter: Bitmap? = null
    private set
  var activeFloor: Bitmap? = null
  var floorSwapped = false
  private var loadedWidth = -1
  private var loadedId = -1

  fun load(assets: AssetManager, next: StageDef, width: Int, restartPlayback: Boolean = false) {
    if (width <= 0) return
    if (loadedId == next.id && loadedWidth == width && floor != null && floor?.isRecycled == false) {
      def = next
      if (restartPlayback || next.theaterKind != StageTheaterKind.ASCENT) {
        floorSwapped = false
        activeFloor = floor
      } else if (!floorSwapped) {
        activeFloor = floor
      }
      return
    }
    recycle()
    def = next
    loadedId = next.id
    loadedWidth = width
    floor = StageBitmaps.decode(assets, next.floorPath(), keyed = false, widthLock = width)
    val overlayKeyed = next.keyedOverlayLayers
    mid = StageBitmaps.tryDecode(assets, next.midPath(), keyed = overlayKeyed, widthLock = width)
    high = StageBitmaps.tryDecode(assets, next.highPath(), keyed = overlayKeyed, widthLock = width)
    canopy = StageBitmaps.tryDecode(assets, next.canopyPath(), keyed = true, widthLock = width)
    floorAlt = StageBitmaps.tryDecode(assets, next.floorAltPath(), keyed = false, widthLock = width)
    briefing = StageBitmaps.tryDecode(assets, next.briefingPath(), keyed = false, widthLock = 0)
    skinTank = StageBitmaps.tryDecode(assets, next.skinTankPath(), keyed = true, widthLock = 0)
    skinDestroyer = StageBitmaps.tryDecode(assets, next.skinDestroyerPath(), keyed = true, widthLock = 0)
    skinWagon = StageBitmaps.tryDecode(assets, next.skinWagonPath(), keyed = true, widthLock = 0)
    skinHelicopter = StageBitmaps.tryDecode(assets, next.skinHelicopterPath(), keyed = true, widthLock = 0)
    floorSwapped = false
    activeFloor = floor
  }

  fun swapToFloorAlt() {
    val alt = floorAlt
    if (alt == null || alt.isRecycled) return
    floorSwapped = true
    activeFloor = alt
  }

  fun recycle() {
    StageBitmaps.recycle(floor)
    StageBitmaps.recycle(mid)
    StageBitmaps.recycle(high)
    StageBitmaps.recycle(canopy)
    StageBitmaps.recycle(floorAlt)
    StageBitmaps.recycle(briefing)
    StageBitmaps.recycle(skinTank)
    StageBitmaps.recycle(skinDestroyer)
    StageBitmaps.recycle(skinWagon)
    StageBitmaps.recycle(skinHelicopter)
    floor = null
    mid = null
    high = null
    canopy = null
    floorAlt = null
    briefing = null
    skinTank = null
    skinDestroyer = null
    skinWagon = null
    skinHelicopter = null
    activeFloor = null
    floorSwapped = false
    loadedWidth = -1
    loadedId = -1
  }
}
