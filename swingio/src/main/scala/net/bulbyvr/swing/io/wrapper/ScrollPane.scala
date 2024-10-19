package net.bulbyvr.swing.io
package wrapper

import cats.effect.*
import cats.effect.syntax.all.*
import fs2.concurrent.Topic
import cats.effect.std.Dispatcher
import javax.swing.{JScrollPane, JComponent}
import cats.syntax.all.*
class ScrollPane[F[_]: Async](topic: Topic[F, event.Event[F]], dispatcher: Dispatcher[F]) extends Component(topic, dispatcher) with SingletonContainer[F] {
  override lazy val peer: JScrollPane = new JScrollPane with SuperMixin

  override def child: Ref[F, Option[Component[F]]] =
    // TODO: probably gets an NPE
    new FWrappedRef(Async[F].delay { Option(peer.getViewport).flatMap(it => Option(it.getView).map(_.asInstanceOf[JComponent])) }
      .flatMap(it => it.traverse(i => UIElement.cachedWrapper[F, Component[F]](i)).map(_.flatten))
      , it => Async[F].delay { Option(peer.getViewport) }.flatMap(viewport => Async[F].delay { viewport.foreach(_.setView(it.map(_.peer).orNull))  })
      )
}

object ScrollPane {
  def apply[F[_]](using F: Async[F]): Resource[F, ScrollPane[F]] = {
    for {
      topic <- Topic[F, event.Event[F]].toResource
      dispatcher <- Dispatcher.sequential[F]
      res <- F.delay { new ScrollPane[F](topic, dispatcher) }.toResource.flatTap(_.setup).evalOn(AwtEventDispatchEC)
    } yield res
  }
}
