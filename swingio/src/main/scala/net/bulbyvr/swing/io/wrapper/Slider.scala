package net.bulbyvr.swing.io
package wrapper

import cats.effect.syntax.all.*
import cats.syntax.all.*
import cats.effect.{Async, Ref}
import cats.effect.std.Dispatcher
import fs2.concurrent.Topic

import javax.swing.{JSlider, JLabel}
import javax.swing.event.{ChangeListener, ChangeEvent}

class Slider[F[_]: Async](topic: Topic[F, event.Event[F]], dispatcher: Dispatcher[F]) extends Component[F](topic, dispatcher) with Adjustable[F] {
  override lazy val peer: JSlider = new JSlider with SuperMixin
  def min: Ref[F, Int] = 
    new WrappedRef(peer.getMinimum, peer.setMinimum)
  def max: Ref[F, Int] =
    new WrappedRef(peer.getMaximum, peer.setMaximum)
  def value: Ref[F, Int] =
    new WrappedRef(peer.getValue, peer.setValue)
  def extent: Ref[F, Int] =
    new WrappedRef(peer.getExtent, peer.setExtent)

  def paintLabels: Ref[F, Boolean] =
    new WrappedRef(peer.getPaintLabels, peer.setPaintLabels)
  def paintTicks: Ref[F, Boolean] =
    new WrappedRef(peer.getPaintTicks, peer.setPaintTicks)
  def paintTrack: Ref[F, Boolean] =
    new WrappedRef(peer.getPaintTrack, peer.setPaintTrack)

  def snapToTicks: Ref[F, Boolean] =
    new WrappedRef(peer.getSnapToTicks, peer.setSnapToTicks)

  def minorTickSpacing: Ref[F, Int] =
    new WrappedRef(peer.getMinorTickSpacing, peer.setMinorTickSpacing)
  def majorTickSpacing: Ref[F, Int] =
    new WrappedRef(peer.getMajorTickSpacing, peer.setMajorTickSpacing)

  def adjusting: F[Boolean] = Async[F].delay { peer.getValueIsAdjusting() }.evalOn(AwtEventDispatchEC)

  def adjustTo: Ref[F, AdjustTo] =
    new WrappedRef(() => AdjustTo.of(peer.getOrientation()), it => peer.setOrientation(it.id))

  private lazy val cl = new ChangeListener {
    def stateChanged(e: ChangeEvent): Unit = 
      dispatcher.unsafeRunAndForget(topic.publish1(event.ValueChanged[F](Slider.this)))
  }

  override protected def onFirstSubscribe: F[Unit] =
    super.onFirstSubscribe *> Async[F].delay { peer.addChangeListener(cl) }.evalOn(AwtEventDispatchEC)
  override protected def onLastUnsubscribe: F[Unit] =
    super.onLastUnsubscribe *> Async[F].delay { peer.removeChangeListener(cl) }.evalOn(AwtEventDispatchEC)

}
