package net.bulbyvr.swing.io

import cats.effect.IO
import cats.effect.kernel.Async

object all extends SwingAll[IO]

sealed class SwingAll[F[_]](using A: Async[F])
  extends PropModifiers[F], 
  SwingElems[F],
  Props[F],
  Modifiers[F] {}


