package net.bulbyvr.swing.io
package wrapper

import cats.effect.{Async, Ref, Resource}
import cats.effect.std.Dispatcher
import fs2.concurrent.Topic
import javax.swing.JTextArea
import cats.effect.syntax.all.*
import cats.syntax.all.*

class TextArea[F[_]](
    dispatcher: Dispatcher[F],
    topic: Topic[F, event.Event[F]]
)(using F: Async[F])
    extends TextComponent[F](topic, dispatcher)
    with TextComponent.HasColumns[F]
    with TextComponent.HasRows[F] {
  override lazy val peer: JTextArea = new JTextArea() with SuperMixin

  def columns: Ref[F, Int] =
    new WrappedRef(
      peer.getColumns,
      peer.setColumns
    )

  def lineWrap: Ref[F, Boolean] =
    new WrappedRef(
      peer.getLineWrap,
      peer.setLineWrap
    )

  def rows: Ref[F, Int] =
    new WrappedRef(
      peer.getRows,
      peer.setRows
    )
}

object TextArea {
  def apply[F[_]](using F: Async[F]): Resource[F, TextArea[F]] =
    for {
      dispatcher <- Dispatcher.sequential[F]
      topic <- Topic[F, event.Event[F]].toResource
      res <- F
        .delay { new TextArea[F](dispatcher, topic) }
        .toResource
        .flatTap(_.setup)
    } yield res
}
