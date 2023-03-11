package net.bulbyvr.swing.io.wrapper

case class Vec2i(x: Int, y: Int) {
  def asPoint: java.awt.Point =
    java.awt.Point(x, y)
}

object Vec2i {
  def of(swing: java.awt.Point) = 
    new Vec2i(swing.x, swing.y)
}
