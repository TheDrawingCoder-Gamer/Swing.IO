package net.bulbyvr.swing.io
package wrapper

import cats.effect.{Async, Ref, Resource}
import cats.effect.std.Dispatcher
import cats.effect.syntax.all.*
import cats.syntax.all.*
import javax.swing.JLabel
import fs2.concurrent.Topic
class Label[F[_]](text0: String, topic: Topic[F, event.Event[F]], dispatcher: Dispatcher[F])(using F: Async[F]) 
  extends Component[F](topic, dispatcher)
  with WithChangableText[F](using F) {
  override lazy val peer: JLabel =
    new JLabel(text0)
  def text: Ref[F, String] =
    new WrappedRef(peer.getText, peer.setText)
}

object Label {
  def apply[F[_]](text: String)(using F: Async[F]): Resource[F, Label[F]] =
    for {
      dispatcher <- Dispatcher.sequential[F]
      topic <- Topic[F, event.Event[F]].toResource
      res <- F.delay { new Label(text, topic, dispatcher) }.toResource.flatTap(_.setup).evalOn(AwtEventDispatchEC)
    } yield res
}
