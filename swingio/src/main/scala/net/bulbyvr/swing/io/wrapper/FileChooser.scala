package net.bulbyvr.swing.io
package wrapper

import javax.swing.SwingUtilities
import cats.effect.*
import java.io.File
import cats.effect.syntax.all.*
import cats.syntax.all.*
import java.awt.FileDialog
object FileChooser {
  final class PartiallyAppliedApply[F[_]] private[io] (val dummy: Boolean = true) extends AnyVal {
    private def getFC(frame: java.awt.Frame)(using Async[F]): F[FileDialog] =
      Async[F].delay { FileDialog(frame) }.evalOn(AwtEventDispatchEC)
    private def showDialog(frame: java.awt.Frame, show: FileDialog => Unit)(using Async[F]): F[FileDialog] = 
      getFC(frame) flatMap { fc => 
        Async[F].delay { show(fc) }.evalOn(AwtEventDispatchEC).map { res =>
          fc
        }
      }
    private def showOpenDialog(comp: UIElement[F], show: FileDialog => Unit)(using Async[F]): F[Option[File]] = {
      Async[F].delay {  SwingUtilities.windowForComponent(comp.peer).asInstanceOf[java.awt.Frame] }.flatMap { frame => 
        showDialog(frame, show).flatMap { fc =>
          Async[F].delay { Option(fc.getFiles).flatMap(it => it.lift(0) ) }.evalOn(AwtEventDispatchEC)

        }
      }
    }
    def save(component: UIElement[F])(using Async[F]): F[Option[File]] = showOpenDialog(component, it => { it.setMode(FileDialog.SAVE); it.setVisible(true) })
    def open(component: UIElement[F])(using Async[F]): F[Option[File]] = showOpenDialog(component, it => { it.setMode(FileDialog.LOAD); it.setVisible(true) })

  }
  def apply[F[_]: Async]: PartiallyAppliedApply[F] = new PartiallyAppliedApply[F]
}
