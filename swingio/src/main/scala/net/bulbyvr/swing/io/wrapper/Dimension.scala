package net.bulbyvr.swing.io.wrapper

import java.awt

case class Dimension(w: Int, h: Int) {
  def asAwt: awt.Dimension =
    new awt.Dimension(w, h)
}

object Dimension {
  def of(dim: awt.Dimension) =
    new Dimension(dim.width, dim.height)
}
