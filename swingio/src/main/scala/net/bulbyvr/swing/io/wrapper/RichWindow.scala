package net.bulbyvr.swing.io
package wrapper

import cats.effect.{Async, Ref, Resource, Deferred}
import cats.effect.std.Dispatcher
import java.awt.{Window as AWTWindow, Frame as AWTFrame}
import javax.swing.{JFrame, JMenuBar, WindowConstants, UIManager}
import cats.effect.syntax.all.*
import cats.syntax.all.*
import fs2.concurrent.Topic
sealed trait RichWindow[F[_]](using F: Async[F]) extends Window[F] {
  def peer: AWTWindow with InterfaceMixin2
  trait InterfaceMixin2 extends InterfaceMixin {
    def getJMenuBar: JMenuBar
    def setJMenuBar(b: JMenuBar): Unit
    def setUndecorated(b: Boolean): Unit
    def setTitle(s: String): Unit
    def getTitle: String
    def setResizable(b: Boolean): Unit
    def isResizable: Boolean
  }
  def title: Ref[F, String] =
    new WrappedRef(() => peer.getTitle, peer.setTitle)
  def resizable: Ref[F, Boolean] =
    new WrappedRef(() => peer.isResizable, peer.setResizable)

}

class Frame[F[_]](topic: Topic[F, event.Event[F]], dispatcher: Dispatcher[F])(using F: Async[F]) extends Window[F](topic, dispatcher) with RichWindow[F] {
  override lazy val peer: JFrame with InterfaceMixin2 = new JFrame() with InterfaceMixin2 with SuperMixin
}

class MainFrame[F[_]](gate: Deferred[F, Unit], topic: Topic[F, event.Event[F]], dispatcher: Dispatcher[F])(using F: Async[F]) extends Frame[F](topic, dispatcher) {
  override def closeOperation = gate.complete(()).void
}

object MainFrame {
  def apply[F[_]](using F: Async[F]): Resource[F, (MainFrame[F], Deferred[F, Unit])] =
    for {
      dispatcher <- Dispatcher.sequential[F]
      topic <- Topic[F, event.Event[F]].toResource
      gate <- Deferred[F, Unit].toResource
      res <- Resource.eval(F.delay { UIManager.setLookAndFeel(LookAndFeel.defaultLookAndFeel); new MainFrame(gate, topic, dispatcher)}).flatTap(_.setup).evalOn(AwtEventDispatchEC)
    } yield (res, gate)
}
