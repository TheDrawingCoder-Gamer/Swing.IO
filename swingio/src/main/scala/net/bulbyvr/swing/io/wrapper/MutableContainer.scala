package net.bulbyvr.swing.io
package wrapper

import cats.effect.{Async, Ref}
import cats.syntax.all.*
import cats.effect.syntax.all.*
/**
 * A container that makes sense for its contents to be mutable
 * Usually for components that are sequential, like the Box and Flow panel
 */
trait MutableContainer[F[_]](using F: Async[F]) extends Container[F] {
  override def peer: javax.swing.JComponent
  override def contents: Ref[F, Seq[Component[F]]] =
    new FWrappedRef(F.delay { peer.getComponents().toSeq }.evalOn(AwtEventDispatchEC)
      >>= (_.traverse(it => UIElement.cachedWrapper[F, Component[F]](it).map(_.get)).evalOn(AwtEventDispatchEC)),
      it => content.clear() *> it.traverse(content.addOne).void
      )
  object content {

    def addOne(c: Component[F]): F[Unit] =
      F.delay { peer.add(c.peer); () }.evalOn(AwtEventDispatchEC) *> validate
    def insert(n: Int, c: Component[F]): F[Unit] =
      F.delay { peer.add(c.peer, n); () }.evalOn(AwtEventDispatchEC) *> validate
    def apply(n: Int): F[Component[F]] = 
      F.delay { peer.getComponent(n) }.flatMap { comp => UIElement.cachedWrapper[F, Component[F]](comp).map(_.get) }
        .evalOn(AwtEventDispatchEC)
    def clear(): F[Unit] =
      F.delay { peer.removeAll() }.evalOn(AwtEventDispatchEC) *> validate
    def remove(n: Int): F[Component[F]] =
      F.delay { 
        val c = peer.getComponent(n)
        peer.remove(n)
        c
      }.flatMap { it => UIElement.cachedWrapper[F, Component[F]](it).map(_.get) }
        .evalOn(AwtEventDispatchEC) <* validate
    def length: F[Int] = F.delay { peer.getComponentCount }.evalOn(AwtEventDispatchEC)
   
    def update(n: Int, a: Component[F]): F[Unit] = {
      remove(n) *> insert(n, a)
    } 
  }
  
}
