package net.bulbyvr.swing.io
package wrapper

import cats.effect.{Async, Ref}
import cats.effect.std.Dispatcher
import java.awt.Graphics
import javax.swing
import cats.effect.syntax.all.*
import cats.syntax.all.*
import java.awt.event.{MouseEvent as JMouseEvent, MouseListener}
import fs2.concurrent.Topic

abstract class Component[F[_]](topic: Topic[F, event.Event[F]], dispatcher: Dispatcher[F])(using F: Async[F]) extends UIElement[F] with WithTopic[F](topic) {
  override lazy val peer: swing.JComponent = new swing.JComponent with SuperMixin {}

  trait SuperMixin {}
  // TODO: Support custom painting
  // Doing that requires wrapping Graphics2D which is kinda a lot
  def name: Ref[F, String] =
    new WrappedRef(peer.getName, peer.setName)
  def xLayoutAlignment: Ref[F, Float] =
    new WrappedRef(peer.getAlignmentX, peer.setAlignmentX)
  def yLayoutAlignment: Ref[F, Float] =
    new WrappedRef(peer.getAlignmentY, peer.setAlignmentY)
  // TODO: Border wrapper
  def opaque: Ref[F, Boolean] =
    new WrappedRef(peer.isOpaque, peer.setOpaque)

  def enabled: Ref[F, Boolean] =
    new WrappedRef(peer.isEnabled, peer.setEnabled)

  def tooltip: Ref[F, String] =
    new WrappedRef(peer.getToolTipText, peer.setToolTipText)

  //input verifier todo
  
  def focusable: Ref[F, Boolean] =
    new WrappedRef(peer.isFocusable, peer.setFocusable)
  // TODO: Border

  def hasFocus: F[Boolean] =
    F.delay { peer.isFocusOwner }.evalOn(AwtEventDispatchEC)

  // TODO: Paint
  // Requires wrapping Graphics2D
  def requestFocus: F[Unit] =
    F.delay { peer.requestFocus() }.evalOn(AwtEventDispatchEC)
  def requestFocusInWindow: F[Boolean] =
    F.delay { peer.requestFocusInWindow() }.evalOn(AwtEventDispatchEC)
  def revalidate: F[Unit] =
    F.delay { peer.revalidate() }.evalOn(AwtEventDispatchEC)

  lazy val l: MouseListener = new MouseListener {
    def mouseEntered(e: JMouseEvent): Unit = ()
    def mouseExited(e: JMouseEvent): Unit = ()
    def mouseClicked(e: JMouseEvent): Unit = 
      dispatcher.unsafeRunAndForget(event.MouseClicked(e).flatMap(topic.publish1))
    // after proof of concept is done
    def mousePressed(e: JMouseEvent): Unit = ()
    def mouseReleased(e: JMouseEvent): Unit = ()
  }
  override def onFirstSubscribe: F[Unit] =
    F.delay { peer.addMouseListener(l) }.evalOn(AwtEventDispatchEC)
  override def onLastUnsubscribe: F[Unit] =
    F.delay { peer.removeMouseListener(l) }.evalOn(AwtEventDispatchEC)
}
