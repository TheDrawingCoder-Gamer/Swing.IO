package net.bulbyvr.swing.io

import cats.effect.{IO, IOApp, ExitCode, Deferred}
import cats.effect.kernel.Resource
import cats.effect.unsafe.implicits.*
import cats.syntax.all.*
trait IOSwingApp extends IOApp {
  /**
   * The MainFrame to be rendered
   */
  def render: Resource[IO, (wrapper.MainFrame[IO], Deferred[IO, Unit])]

  def run(args: List[String]) = render.flatMap { case t @ (f, _) => Resource.make(f.open *> t.pure[IO])(_ => IO.unit) }.use { case (_, gate) => gate.get } *> ExitCode.Success.pure[IO] 
}

