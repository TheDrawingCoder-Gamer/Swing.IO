package net.bulbyvr.swing.io
package wrapper
package event

import cats.effect.Async

class ValueChanged[F[_]](val source: Component[F])(using F: Async[F]) extends ComponentEvent[F]
