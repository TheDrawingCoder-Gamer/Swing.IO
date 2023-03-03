package net.bulbyvr.swing.io

import scala.swing.event as sevent
import scala.concurrent.duration.*
import cats.effect.kernel.Sync
import java.awt.Point
// TODO: Why are we here? just to suffer?
trait EventWrapper[Ev] {
  type RawEv
  def wrap(ev: RawEv): Ev
}
trait Event[F[_]] private[io] {}

trait UIEvent[F[_]] private[io] extends Event[F] {}

trait ComponentEvent[F[_]] private[io] extends UIEvent[F] {}

trait InputEvent[F[_]] private[io] extends ComponentEvent[F] {
  def modifiers: F[sevent.Key.Modifiers]
  def consume: F[Unit]
  def consumed: F[Boolean]
  def when: F[FiniteDuration]
}
private trait InputEventImpl[F[_]](using F: Sync[F]) extends InputEvent[F] {
  def event: sevent.InputEvent

  def modifiers = F.delay { event.modifiers }
  def consume = F.delay { event.consume() }
  def consumed = F.delay { event.consumed }
  def when = F.delay { event.`when`.millis }
}

abstract class MouseEvent[F[_]] private[io] extends InputEvent[F] {
  def point: F[Point]
}


private trait MouseEventImpl[F[_]](using F: Sync[F]) extends MouseEvent[F] with InputEventImpl[F] {
  def event: sevent.MouseEvent

  def point = F.delay(event.point)
}

abstract class MouseButtonEvent[F[_]] private[io] extends MouseEvent[F] {
  def clicks: F[Int]
  // triggersPopup
}

private trait MouseButtonEventImpl[F[_]](using F: Sync[F]) extends MouseButtonEvent[F] with MouseEventImpl[F] {
  def event: sevent.MouseButtonEvent

  def clicks = F.delay(event.clicks)
}
final class MouseClicked[F[_]](val event: sevent.MouseClicked)(using F: Sync[F]) extends MouseButtonEvent[F] with MouseButtonEventImpl[F] {}
final class MousePressed[F[_]](val event: sevent.MousePressed)(using F: Sync[F]) extends MouseButtonEvent[F] with MouseButtonEventImpl[F] {}
final class MouseReleased[F[_]](val event: sevent.MouseReleased)(using F: Sync[F]) extends MouseButtonEvent[F] with MouseButtonEventImpl[F] {}

trait ActionEvent[F[_]] private[io] extends ComponentEvent[F] {}

final class ButtonClicked[F[_]](val event: sevent.ButtonClicked) extends ActionEvent[F]

final class ValueChanged[F[_]](val event: sevent.ValueChanged) extends ComponentEvent[F] {}
trait EventWrappers[F[_]](using F: Sync[F]) {
  given EventWrapper[MouseClicked[F]] = new EventWrapper {
    type RawEv = sevent.MouseClicked
    def wrap(it: RawEv) = new MouseClicked(it)
  }
  given EventWrapper[MousePressed[F]] = new EventWrapper {
    type RawEv = sevent.MousePressed
    def wrap(it: RawEv) = new MousePressed(it)
  }
  given EventWrapper[MouseReleased[F]] = new EventWrapper {
    type RawEv = sevent.MouseReleased
    def wrap(it: RawEv) = new MouseReleased(it)
  }
  given EventWrapper[ButtonClicked[F]] = new EventWrapper[ButtonClicked[F]] {
    type RawEv = sevent.ButtonClicked
    def wrap(it: RawEv) = new ButtonClicked[F](it)
  }
  given EventWrapper[ValueChanged[F]] = new EventWrapper {
    type RawEv = sevent.ValueChanged
    def wrap(it: RawEv) = new ValueChanged(it)
  }
}
