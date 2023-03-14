package net.bulbyvr.swing.io
package wrapper

import cats.effect.Async
import cats.effect.kernel.RefSource

trait Container[F[_]](using F: Async[F]) extends UIElement[F] {
  /**
   * Contents of this container. Read only.
   */
  def contents: RefSource[F, Seq[Component[F]]]
}

