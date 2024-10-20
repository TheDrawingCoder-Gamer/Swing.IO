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
import java.awt.event.{ActionListener, ActionEvent}

class ComboBox[F[_]: Async, A](dispatcher: Dispatcher[F], topic: Topic[F, event.Event[F]], private val updating: Ref[F, Boolean]) extends Component[F](topic, dispatcher) with WithRenderer[F, A] {
  override lazy val peer: JComboBox[A] = new JComboBox[A]() with SuperMixin

  /**
   * Renderer
   *
   * Uses [[ListView.Renderer]] as java uses ListView under the hood.
   */
  def renderer: RefSink[F, ListView.Renderer[F, A]] = WrappedSink(it => peer.setRenderer(it.peer))

  /**
   * The current selected index
   */
  def index: Ref[F, Int] = new FWrappedRef[F, Int](Async[F].delay { peer.getSelectedIndex }, it => updating.set(true) *> Async[F].delay { peer.setSelectedIndex(it) })

  // i love casting : (
  /**
   * Current selected item.
   * WARNING: Setting will not work if the item isn't in the dropdown
   */
  def item: Ref[F, A] = new FWrappedRef[F, A](Async[F].delay { peer.getSelectedItem.asInstanceOf[A] }, it => updating.set(true) *> Async[F].delay { peer.setSelectedItem(it) })

  /**
   * Max row count that can be displayed without scrolling
   */
  def maxRowCount: Ref[F, Int] = new WrappedRef(peer.getMaximumRowCount, peer.setMaximumRowCount)

  /**
   * Items in box. Write only access.
   */
  def items: RefSink[F, Seq[A]] = WrappedSink { it => peer.removeAllItems(); it.foreach(peer.addItem) }

  private lazy val il: ActionListener = new ActionListener {
    def actionPerformed(e: ActionEvent): Unit = {
      dispatcher.unsafeRunAndForget {
        for {
          updated <- updating.getAndSet(false)
          _ <- Async[F].whenA(!updated)(topic.publish1(event.SelectionChanged(ComboBox.this)))
        } yield ()
      }
    } 
  }

  override protected def onFirstSubscribe = 
    super.onFirstSubscribe *> Async[F].delay {peer.addActionListener(il)}.evalOn(AwtEventDispatchEC)
  override protected def onLastUnsubscribe =
    super.onLastUnsubscribe *> Async[F].delay { peer.removeActionListener(il) }.evalOn(AwtEventDispatchEC)

}

object ComboBox {
  def apply[F[_]: Async, A]: Resource[F, ComboBox[F, A]] =
    for {
      dispatcher <- Dispatcher.sequential[F]
      topic <- Topic[F, event.Event[F]].toResource
      updating <- Ref[F].of(false).toResource
      // Setup wasn't here before :broken_heart:
      res <- Async[F].delay { new ComboBox[F, A](dispatcher, topic, updating) }.toResource.flatTap(_.setup).evalOn(AwtEventDispatchEC)
    } yield res
}
