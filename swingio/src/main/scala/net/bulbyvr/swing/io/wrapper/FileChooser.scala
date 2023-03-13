package net.bulbyvr.swing.io
package wrapper

import javax.swing.JFileChooser
import cats.effect.*
import java.io.File
import cats.effect.syntax.all.*
import cats.syntax.all.*
object FileChooser {
  final class PartiallyAppliedApply[F[_]] private[io] (val dummy: Boolean = true) extends AnyVal {
    private def getFC(using Async[F]): F[JFileChooser] =
      Async[F].delay { JFileChooser() }.evalOn(AwtEventDispatchEC)
    private def showDialog(show: JFileChooser => Int)(using Async[F]): F[(Int, JFileChooser)] = 
      getFC.flatMap { fc => 
        Async[F].delay { show(fc) }.evalOn(AwtEventDispatchEC).map { res =>
          (res, fc)
        }
      }
    private def showOpenDialog(show: JFileChooser => Int)(using Async[F]): F[Option[File]] =
      showDialog(show).flatMap { (res, fc) =>
        if (res == JFileChooser.APPROVE_OPTION)
          Async[F].delay { Option(fc.getSelectedFile) }.evalOn(AwtEventDispatchEC)
        else
          None.pure[F]

      }
    def save(using Async[F]): F[Option[File]] = showOpenDialog(_.showSaveDialog(null))
    def open(using Async[F]): F[Option[File]] = showOpenDialog(_.showOpenDialog(null))

  }
  def apply[F[_]: Async]: PartiallyAppliedApply[F] = new PartiallyAppliedApply[F]
}
