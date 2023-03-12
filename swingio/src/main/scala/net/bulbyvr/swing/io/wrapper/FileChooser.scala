package net.bulbyvr.swing.io
package wrapper

import javax.swing.JFileChooser
import cats.effect.*
import java.io.File
import cats.effect.syntax.all.*
import cats.syntax.all.*
object FileChooser {
  private[io] class PartiallyAppliedApply[F[_]: Async] {
    private def getFC: F[JFileChooser] =
      Async[F].delay { JFileChooser() }.evalOn(AwtEventDispatchEC)
    private def showDialog(show: JFileChooser => Int): F[(Int, JFileChooser)] = 
      getFC.flatMap { fc => 
        Async[F].delay { show(fc) }.evalOn(AwtEventDispatchEC).map { res =>
          (res, fc)
        }
      }
    private def showOpenDialog(show: JFileChooser => Int): F[Option[File]] =
      showDialog(show).flatMap { (res, fc) =>
        if (res == JFileChooser.APPROVE_OPTION)
          Async[F].delay { Option(fc.getSelectedFile) }.evalOn(AwtEventDispatchEC)
        else
          None.pure[F]

      }
    def save: F[Option[File]] = showOpenDialog(_.showSaveDialog(null))
    def open: F[Option[File]] = showOpenDialog(_.showOpenDialog(null))

  }
  def apply[F[_]: Async]: PartiallyAppliedApply[F] = new PartiallyAppliedApply[F]
}
