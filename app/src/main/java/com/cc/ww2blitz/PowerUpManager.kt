package com.cc.ww2blitz

object ItemType {
  const val WEAPON_UP = PowerUpItem.ITEM_TYPE_POWERUP
  const val BOMB_STOCK = PowerUpItem.ITEM_TYPE_BOMB
  const val SHIELD_RECOVERY = PowerUpItem.ITEM_TYPE_SHIELD
}

class PowerUpManager private constructor() {

  val items = PowerUpItem()

  fun spawnGuaranteedDrop(x: Float, y: Float, itemType: Int) {
    items.spawnSway(x, y, itemType)
  }

  fun spawnRightFlankDrop(x: Float, y: Float, playerWeaponPower: Int, bombStock: Int) {
    if (playerWeaponPower < 3) {
      spawnGuaranteedDrop(x, y, ItemType.WEAPON_UP)
    } else if (bombStock < 3) {
      spawnGuaranteedDrop(x, y, ItemType.BOMB_STOCK)
    } else {
      spawnGuaranteedDrop(x, y, ItemType.SHIELD_RECOVERY)
    }
  }

  fun spawnBulletCancelDrop(x: Float, y: Float) {
    if (!items.spawnStationaryMedal(x, y, ScoreManager.BULLET_CANCEL_POINTS)) {
      ScoreManager.instance.addBulletCancelBonus(x, y)
    }
  }

  companion object {
    val instance = PowerUpManager()
  }
}
