package net.bulbyvr.swing.io
package wrapper

import cats.effect.{Async, Ref}
import cats.effect.kernel.RefSource
import cats.effect.syntax.all.*
import cats.syntax.all.*
trait SingletonContainer[F[_]: Async] extends Container[F] {
  override def contents: RefSource[F, Seq[Component[F]]] =
    new RefSource[F, Seq[Component[F]]] {
      def get: F[Seq[Component[F]]] =
        child.get.map(_.toSeq)
    }
    
  def child: Ref[F, Option[Component[F]]]

}
