package net.bulbyvr.swing.io
package wrapper

import cats.effect.Async

trait Container[F[_]](using F: Async[F]) extends UIElement[F] {
  /**
   * Contents of this container. Read only.
   */
  def contents: F[Seq[Component[F]]]
}

