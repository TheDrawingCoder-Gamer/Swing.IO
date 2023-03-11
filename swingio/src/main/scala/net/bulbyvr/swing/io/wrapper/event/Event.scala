package net.bulbyvr.swing.io
package wrapper
package event

import java.awt
import cats.effect.Async
import cats.effect.syntax.all.*
trait Event[F[_]]

trait UIEvent[F[_]] extends Event[F] {
  def source: UIElement[F]
}

trait ComponentEvent[F[_]] extends UIEvent[F] {
  def source: Component[F]
}

trait InputEvent[F[_]](using F: Async[F]) extends ComponentEvent[F] {
  def peer: awt.event.InputEvent
  def when: F[Long] = F.delay { peer.getWhen() }.evalOn(AwtEventDispatchEC)
  def consume: F[Unit] = F.delay { peer.consume() }.evalOn(AwtEventDispatchEC)
  def consumed: F[Boolean] = F.delay { peer.isConsumed() }.evalOn(AwtEventDispatchEC)
  def modifiers: Int
}

trait SelectionEvent[F[_]] extends Event[F]

trait ListSelectionEvent[F[_]] extends SelectionEvent[F] {
  def range: Range
}

case class SelectionChanged[F[_]](val source: Component[F]) extends ComponentEvent[F] with SelectionEvent[F]
