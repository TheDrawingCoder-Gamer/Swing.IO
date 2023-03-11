package net.bulbyvr.swing.io
package wrapper

import cats.effect
import effect.syntax.all.*
import effect.{Async, Ref, Resource}
import effect.kernel.RefSink
import cats.syntax.all.*
import effect.std.Dispatcher
import fs2.concurrent.Topic

import javax.swing.JComboBox
import java.awt.event.{ItemListener, ItemEvent}

class ComboBox[F[_]: Async, A](dispatcher: Dispatcher[F], topic: Topic[F, event.Event[F]]) extends Component[F](topic, dispatcher) with WithRenderer[F, A] {
  override lazy val peer: JComboBox[A] = new JComboBox[A]() with SuperMixin
  
  def renderer: RefSink[F, ListView.Renderer[F, A]] = WrappedSink(it => peer.setRenderer(it.peer))

  def index: Ref[F, Int] = new WrappedRef(peer.getSelectedIndex, peer.setSelectedIndex)

  // i love casting : (
  def item: Ref[F, A] = new WrappedRef(() => peer.getSelectedItem.asInstanceOf[A], peer.setSelectedItem)

  def maxRowCount: Ref[F, Int] = new WrappedRef(peer.getMaximumRowCount, peer.setMaximumRowCount)

  def items: RefSink[F, Seq[A]] = WrappedSink { it => peer.removeAllItems(); it.foreach(peer.addItem) }

  private lazy val il: ItemListener = new ItemListener {
    def itemStateChanged(e: ItemEvent): Unit = {
      dispatcher.unsafeRunAndForget(topic.publish1(new event.SelectionChanged(ComboBox.this)))
    } 
  }

  override protected def onFirstSubscribe = 
    super.onFirstSubscribe *> Async[F].delay {peer.addItemListener(il)}.evalOn(AwtEventDispatchEC)
  override protected def onLastUnsubscribe =
    super.onLastUnsubscribe *> Async[F].delay { peer.removeItemListener(il) }.evalOn(AwtEventDispatchEC)

}

object ComboBox {
  def apply[F[_]: Async, A]: Resource[F, ComboBox[F, A]] =
    for {
      dispatcher <- Dispatcher.sequential[F]
      topic <- Topic[F, event.Event[F]].toResource
      res <- Async[F].delay { new ComboBox[F, A](dispatcher, topic) }.evalOn(AwtEventDispatchEC).toResource
    } yield res
}
