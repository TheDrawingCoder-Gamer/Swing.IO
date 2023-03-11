package net.bulbyvr.swing.io
package wrapper

import cats.effect.Async
import cats.effect.Ref
import cats.effect.std.Dispatcher
import javax.swing
import java.awt
import java.util as ju
import cats.effect.syntax.all.*
import cats.Id
import cats.syntax.all.*
import cats.Traverse
import cats.effect.Resource
import fs2.concurrent.Topic

import scala.ref.WeakReference
trait UIElement[F[_]](using F: Async[F]) extends WithTopic[F] {
  def peer: awt.Component

  def background: Ref[F, awt.Color] =
    new WrappedRef(peer.getBackground, peer.setBackground)
  def bounds: F[Rectangle] =
    F.delay { Rectangle.of(peer.getBounds()) }.evalOn(AwtEventDispatchEC)
  def componentOrientation: Ref[F, awt.ComponentOrientation] =
    new WrappedRef(peer.getComponentOrientation, peer.setComponentOrientation)
  def cursor: Ref[F, awt.Cursor] =
    new WrappedRef(peer.getCursor, peer.setCursor)
  def displayable: F[Boolean] =
    F.delay { peer.isDisplayable() }.evalOn(AwtEventDispatchEC)
  def font: Ref[F, awt.Font] =
    new WrappedRef(peer.getFont, peer.setFont)
  def foreground: Ref[F, awt.Color] =
    new WrappedRef(peer.getForeground, peer.setForeground)
  def ignoreRepaint: Ref[F, Boolean] =
    new WrappedRef(peer.getIgnoreRepaint, peer.setIgnoreRepaint)
  def locale: F[ju.Locale] =
    F.delay { peer.getLocale() }.evalOn(AwtEventDispatchEC)
  def location: F[Vec2i] =
    F.delay { Vec2i.of(peer.getLocation()) }.evalOn(AwtEventDispatchEC)
  def locationOnScreen: F[Vec2i] =
    F.delay { Vec2i.of(peer.getLocationOnScreen()) }.evalOn(AwtEventDispatchEC)
  def maximumSize: Ref[F, Dimension] =
    new WrappedRef(() => Dimension.of(peer.getMaximumSize()), it => peer.setMaximumSize(it.asAwt))
  def minimumSize: Ref[F, Dimension] =
    new WrappedRef(() => Dimension.of(peer.getMinimumSize()), it => peer.setMinimumSize(it.asAwt))
  def preferredSize: Ref[F, Dimension] =
    new WrappedRef(() =>  Dimension.of(peer.getPreferredSize()), it => peer.setPreferredSize(it.asAwt))
  def repaint(): F[Unit] =
    F.delay { peer.repaint() }.evalOn(AwtEventDispatchEC)
  def repaint(rect: Rectangle): F[Unit] =
    F.delay { peer.repaint(rect.x, rect.y, rect.w, rect.h) }.evalOn(AwtEventDispatchEC)
  def showing: F[Boolean] =
    F.delay { peer.isShowing() }.evalOn(AwtEventDispatchEC)
  def size: F[Dimension] =
    F.delay { Dimension.of(peer.getSize())}.evalOn(AwtEventDispatchEC)
  // NO TOOLKIT : (
  def validate: F[Unit] =
    F.delay { peer.validate() }.evalOn(AwtEventDispatchEC)
  def visible: Ref[F, Boolean] =
    new WrappedRef(peer.isVisible, peer.setVisible)
  override def setup: Resource[F, Unit] =
    for {
      _ <- super.setup
      _ <- UIElement.cache[F](this).toResource
    } yield ()
  // TODO
  override protected def onFirstSubscribe: F[Unit] = F.unit
  override protected def onLastUnsubscribe: F[Unit] = F.unit
}

object UIElement  {
  private val ClientKey = "swing.io.swingWrapper"
  private[this] val wrapperCache = new ju.WeakHashMap[awt.Component, WeakReference[UIElement[Id]]]
  private def cache[F[_]](e: UIElement[F])(using F: Async[F]) = F.delay {
    e.peer match {
    case p: swing.JComponent => p.putClientProperty(ClientKey, e)
    case _ => wrapperCache.put(e.peer, new WeakReference(e.asInstanceOf[UIElement[Id]]))
    }
  }
  private[wrapper] def cachedWrapper[F[_], C<:UIElement[F]](c: awt.Component)(using F: Async[F]): F[Option[C]] = F.delay {
    val w = c match {
      case c: swing.JComponent => c.getClientProperty(ClientKey)
      case _ => wrapperCache.get(c)
    }

    try { Option(w.asInstanceOf[C]) } catch { case _: Exception => None }

  }
  def wrap[F[_]](c: awt.Component)(using F: Async[F], T: Traverse[F]): Resource[F, UIElement[F]] = 
    cachedWrapper[F, UIElement[F]](c).sequence.map(_.toResource).getOrElse(apply(c))
  def apply[F[_]](daPeer: awt.Component)(using F: Async[F]): Resource[F, UIElement[F]] = {
    Topic[F, event.Event[F]].toResource
      .flatMap(topic => F.delay { new UIElement[F] with WithTopic[F](topic) { def peer = daPeer } }
      .toResource.flatTap(_.setup)
      .evalOn(AwtEventDispatchEC))
  }
  inline def buildApply[F[_], E <: UIElement[F]](build: (Dispatcher[F], Topic[F, event.Event[F]]) => E)(using F: Async[F]): Resource[F, E] = {
    for {
      topic <- Topic[F, event.Event[F]].toResource
      dispatcher <- Dispatcher.sequential[F]
      res <- F.delay { build(dispatcher, topic) }.toResource.flatTap(_.setup)
    } yield res
  }
}
