package net.bulbyvr.swing.io
package wrapper

import java.awt.Adjustable as JAdjustable
import cats.effect.{Async, Ref}
import cats.effect.kernel.RefSource

enum AdjustTo {
  import JAdjustable.*
  case Vertical
  case Horizontal
  case NoAdjust

  def id: Int = this match {
    case Vertical => 
      VERTICAL
    case Horizontal =>
      HORIZONTAL
    case NoAdjust =>
      NO_ORIENTATION
  }
}
object AdjustTo {
  def of(id: Int) = id match {
    case JAdjustable.VERTICAL =>
      AdjustTo.Vertical
    case JAdjustable.HORIZONTAL =>
      AdjustTo.Horizontal
    case JAdjustable.NO_ORIENTATION =>
      AdjustTo.NoAdjust
    case _ => ???
  }
}

trait Adjusted[F[_]: Async] {
  def adjustTo: RefSource[F, AdjustTo]
}
trait Adjustable[F[_]: Async] extends Adjusted[F] {
  override def adjustTo: Ref[F, AdjustTo]
}
