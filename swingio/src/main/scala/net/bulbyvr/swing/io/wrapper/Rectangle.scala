package net.bulbyvr.swing.io.wrapper

import java.awt
case class Rectangle(x: Int, y: Int, w: Int, h: Int) {
  def asJavaRect =
    awt.Rectangle(x, y, w, h)
}

object Rectangle {
  def of(java: awt.Rectangle) =
    Rectangle(java.x, java.y, java.width, java.height)
}
