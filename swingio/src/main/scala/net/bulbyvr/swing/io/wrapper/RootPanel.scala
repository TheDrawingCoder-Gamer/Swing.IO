package net.bulbyvr.swing.io
package wrapper

import cats.effect.{Async, Ref}
import cats.syntax.all.*
import cats.effect.syntax.all.*
trait RootPanel[F[_]](using F: Async[F]) extends SingletonContainer[F] {
  def peer: java.awt.Component with javax.swing.RootPaneContainer

  def child: Ref[F, Option[Component[F]]] =
    new FWrappedRef(
        {
          for {
            count <- F.delay { peer.getContentPane.getComponentCount }.evalOn(AwtEventDispatchEC)
            res <- 
              if (count == 0) 
                F.pure(None)
              else 
                F.delay { peer.getContentPane.getComponent(0).asInstanceOf[javax.swing.JComponent] }.evalOn(AwtEventDispatchEC)
                  >>= UIElement.cachedWrapper[F, Component[F]]
          } yield res
        },
        setChild 
      )
  protected def setChild(comp: Option[Component[F]]): F[Unit] = F.delay {
    if (peer.getContentPane.getComponentCount > 0) {
      val old = peer.getContentPane.getComponent(0)
      peer.getContentPane.remove(old)
    }
    comp.foreach(i => peer.getContentPane.add(i.peer))
  }.evalOn(AwtEventDispatchEC) <* validate
}
