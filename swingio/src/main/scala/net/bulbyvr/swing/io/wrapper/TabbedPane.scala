package net.bulbyvr.swing.io
package wrapper

import cats.effect.*
import cats.effect.syntax.all.*
import cats.effect.std.Dispatcher
import cats.syntax.all.*
import fs2.concurrent.Topic

import javax.swing.JTabbedPane

object TabbedPane {
  class Page[F[_]: Async](val name: String, val elem: Component[F])

  def apply[F[_]: Async]: Resource[F, TabbedPane[F]] = 
    for {
      topic <- Topic[F, event.Event[F]].toResource
      dispatcher <- Dispatcher.sequential[F]
      res <- Async[F].delay { new TabbedPane(topic, dispatcher) }.evalOn(AwtEventDispatchEC).toResource.flatTap(_.setup)
    } yield res
}
class TabbedPane[F[_]: Async](topic: Topic[F, event.Event[F]], dispatcher: Dispatcher[F]) extends Component[F](topic, dispatcher) {
  import TabbedPane.*
  override lazy val peer: JTabbedPane = new JTabbedPane() with SuperMixin

  def pages: Ref[F, Seq[Page[F]]] =
    new FWrappedRef(
        Async[F].delay { for { n <- 0 until peer.getTabCount } yield (peer.getTitleAt(n), peer.getTabComponentAt(n)) }.evalOn(AwtEventDispatchEC)
          .flatMap(_.toList.traverse((l, r) => (l.pure[F], UIElement.cachedWrapper[F, Component[F]](r)).tupled))
          .map(_.map((l, r) => new Page(l, r.get))),
        it => Async[F].delay { peer.removeAll() }.evalOn(AwtEventDispatchEC) *> it.traverse(page => Async[F].delay { peer.add(page.name, page.elem.peer) }).void
      )
  def addPage(page: Page[F]): F[Unit] =
    Async[F].delay { peer.add(page.name, page.elem.peer) }.evalOn(AwtEventDispatchEC).void


}
