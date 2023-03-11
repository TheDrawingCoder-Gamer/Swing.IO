package net.bulbyvr.swing.io
package wrapper
package event

import java.awt
import cats.effect.{Async, Resource}
import cats.effect.syntax.all.*
import javax.swing
import swing.JComponent
import cats.syntax.all.*

sealed abstract class MouseEvent[F[_]](using F: Async[F]) extends InputEvent[F] {
  override def peer: awt.event.MouseEvent
  def point: Vec2i
}

sealed abstract class MouseButtonEvent[F[_]](using F: Async[F]) extends MouseEvent[F] {
  def clicks: Int
  def triggersPopup: Boolean
}

case class MouseClicked[F[_]] private (source: Component[F], point: Vec2i, modifiers: Int, 
                                        clicks: Int, triggersPopup: Boolean)(val peer: awt.event.MouseEvent)(using F: Async[F])
  extends MouseButtonEvent[F]

object MouseClicked {
  def apply[F[_]](e: awt.event.MouseEvent)(using F: Async[F]): F[MouseClicked[F]] = 
    for {
      wrap <- UIElement.cachedWrapper[F, Component[F]](e.getSource.asInstanceOf[JComponent])
      point <- F.delay { e.getPoint }.evalOn(AwtEventDispatchEC)
      modifiers <- F.delay { e.getModifiersEx}.evalOn(AwtEventDispatchEC)
      clickCount <- F.delay { e.getClickCount}.evalOn(AwtEventDispatchEC)
      isPopupTrigger <- F.delay { e.isPopupTrigger }.evalOn(AwtEventDispatchEC)
    } yield MouseClicked[F](wrap.get, Vec2i.of(point), modifiers, clickCount, isPopupTrigger)(e)
}
