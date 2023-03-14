package net.bulbyvr.swing.io
package wrapper

import cats.effect.*
import cats.effect.std.Dispatcher
import cats.effect.kernel.RefSource
import cats.effect.syntax.all.* 
import fs2.concurrent.Topic
import javax.swing.JSplitPane
import cats.syntax.all.*
class SplitPane[F[_]: Async](topic: Topic[F, event.Event[F]], dispatcher: Dispatcher[F]) extends Component[F](topic, dispatcher) with Container[F] {
  override lazy val peer: JSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT)

  def contents: RefSource[F, Seq[Component[F]]] =
    new RefSource {
      def get: F[Seq[Component[F]]] =
        for {
          top <- SplitPane.this.top.get
          bottom <- Async[F].delay { peer.getBottomComponent }.evalOn(AwtEventDispatchEC).flatMap(UIElement.cachedWrapper[F, Component[F]])
        } yield Seq(top, bottom.get)
    }
  def top: Ref[F, Component[F]] =
    new FWrappedRef(
      Async[F].delay { peer.getTopComponent }.evalOn(AwtEventDispatchEC).flatMap(UIElement.cachedWrapper[F, Component[F]](_).map(_.get)),
      it => Async[F].delay { peer.setTopComponent(it.peer) }
      )
  def bottom: Ref[F, Component[F]] =
    new FWrappedRef(
      Async[F].delay { peer.getBottomComponent }.evalOn(AwtEventDispatchEC).flatMap(UIElement.cachedWrapper[F, Component[F]](_).map(_.get)),
      it => Async[F].delay { peer.setBottomComponent(it.peer) }
      )
  def left: Ref[F, Component[F]] = top
  def right: Ref[F, Component[F]] = bottom
}
