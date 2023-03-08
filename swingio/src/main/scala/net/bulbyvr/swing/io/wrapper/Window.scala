package net.bulbyvr.swing.io
package wrapper

import java.awt.Window as AWTWindow
import java.awt.event.{WindowEvent, WindowListener}
import cats.effect.Async
import cats.effect.std.Dispatcher
import cats.syntax.all.*
import cats.effect.syntax.all.*
import fs2.concurrent.Topic
abstract class Window[F[_]](topic: Topic[F, event.Event[F]], dispatcher: Dispatcher[F])(using F: Async[F]) 
  extends UIElement[F]
  with RootPanel[F]
  with WithTopic[F](topic) { outer =>
  def peer: AWTWindow with InterfaceMixin

  protected trait InterfaceMixin extends javax.swing.RootPaneContainer 

  protected trait SuperMixin extends AWTWindow {
    override protected def processWindowEvent(e: WindowEvent): Unit = {
      super.processWindowEvent(e)
      if (e.getID == WindowEvent.WINDOW_CLOSING)
        dispatcher.unsafeRunAndForget(closeOperation)
    }    
  }

  def closeOperation: F[Unit] = F.unit
  // Set child doesn't require an evalOn add
  override def setChild(c: Option[Component[F]]): F[Unit] = {
    super.setChild(c) *> this.pack.void
  }

  def dispose: F[Unit] = F.delay { peer.dispose() }.evalOn(AwtEventDispatchEC)

  def setLocationRelativeTo(c: UIElement[F]): F[Unit] = F.delay { peer.setLocationRelativeTo(c.peer) }.evalOn(AwtEventDispatchEC)
  def centerOnScreen: F[Unit] = F.delay { peer.setLocationRelativeTo(null) }.evalOn(AwtEventDispatchEC)

  def pack: F[this.type] = F.delay { peer.pack(); this }.evalOn(AwtEventDispatchEC)
  
  def open: F[Unit] = F.delay { peer.setVisible(true) }.evalOn(AwtEventDispatchEC)
  def close: F[Unit] = F.delay { peer.setVisible(false) }.evalOn(AwtEventDispatchEC)

  def owner: F[Option[Window[F]]] = F.delay {peer.getOwner}.evalOn(AwtEventDispatchEC) >>= UIElement.cachedWrapper[F, Window[F]]

  // todo: event listener

}
