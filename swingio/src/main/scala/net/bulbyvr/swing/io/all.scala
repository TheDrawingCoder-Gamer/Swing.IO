package net.bulbyvr.swing.io

import cats.effect.IO
import cats.effect.kernel.Async

object all extends SwingAll[IO]

sealed class SwingAll[F[_]](using F: Async[F])
  extends PropModifiers[F], 
  EventPropModifiers[F],
  SwingElems[F],
  Props[F],
  Modifiers[F],
  EventProps[F] {
    given Swing[F] = Swing.forAsync
  }


