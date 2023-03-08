package net.bulbyvr.swing.io
package wrapper
package event

import cats.effect.Async

class ActionEvent[F[_]](val source: Component[F])(using F: Async[F]) extends ComponentEvent[F]

case class ButtonClicked[F[_]](override val source: AbstractButton[F])(using F: Async[F]) extends ActionEvent[F](source)
