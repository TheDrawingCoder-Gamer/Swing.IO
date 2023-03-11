package net.bulbyvr.swing.io
package wrapper

import cats.effect.{Async, Ref, Resource}
import cats.effect.std.Dispatcher
import fs2.concurrent.Topic
import javax.swing.JTextField
import cats.effect.syntax.all.*
import cats.syntax.all.*
class TextField[F[_]](dispatcher: Dispatcher[F], topic: Topic[F, event.Event[F]])(using F: Async[F]) 
  extends TextComponent[F](topic, dispatcher) 
  with TextComponent.HasColumns[F] {
  override lazy val peer: JTextField = new JTextField() with SuperMixin

  def columns: Ref[F, Int] =
    new WrappedRef(
      peer.getColumns,
      peer.setColumns
      )
  // TODO: More fun event listeners
}

object TextField {
  def apply[F[_]](using F: Async[F]): Resource[F, TextField[F]] =
    for {
      dispatcher <- Dispatcher.sequential[F]
      topic <- Topic[F, event.Event[F]].toResource
      res <- F.delay { new TextField[F](dispatcher, topic) }.toResource.flatTap(_.setup)
    } yield res
}
