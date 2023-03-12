package net.bulbyvr.swing.io
package wrapper

import cats.effect, effect.{Async, Ref, Resource}, effect.std.{Dispatcher, Hotswap}, effect.syntax.all.*
import cats.effect.kernel.RefSink
import cats.syntax.all.*
import fs2.concurrent.Topic
import javax.swing.{JList, AbstractListModel, ListCellRenderer, JComponent, JLabel}
import scala.collection.mutable as mut
object ListView {
  private[io] final class ApplyPartiallyApplied[F[_]](val dummy: Boolean = true) extends AnyVal {
    def apply[A](using F: Async[F]): Resource[F, ListView[F, A]] = {
      for {
        dispatcher <- Dispatcher.sequential[F]
        topic <- Topic[F, event.Event[F]].toResource
        res <- Async[F].delay { new ListView[F, A](dispatcher, topic) }.toResource.flatTap(_.setup)
      } yield res 
    }
    def wrap[A](c: JList[A])(using F: Async[F]): Resource[F, ListView[F, A]] =
      for {
        dispatcher <- Dispatcher.sequential[F]
        topic <- Topic[F, event.Event[F]].toResource
        res <- Async[F].delay { new ListView[F, A](dispatcher, topic) { override lazy val peer = c } }.toResource.flatTap(_.setup)
      } yield res
  }
  def apply[F[_]]: ApplyPartiallyApplied[F] = new ApplyPartiallyApplied[F]
  trait Renderer[F[_]: Async, -A] {
    def peer: ListCellRenderer[? >: A]
  }
  trait AbstractRenderer[F[_]: Async, -A](dispatcher: Dispatcher[F], hotswap: Hotswap[F, Component[F]]) extends Renderer[F, A] {
    override lazy val peer:  ListCellRenderer[? >: A] = new ListCellRenderer[A] {
      def getListCellRendererComponent(list: JList[? <: A], a: A, index: Int, isSelected: Boolean, focused: Boolean): JComponent = {
        val comp = dispatcher.unsafeRunSync[Component[F]](hotswap.swap(render(isSelected, focused, a, index)))
        comp.peer
      }
    } 
    protected def render(isSelected: Boolean, focused: Boolean, a: A, index: Int): Resource[F, Component[F]]
  }
  trait TextRenderer[F[_]: Async, -A](dispatcher: Dispatcher[F]) extends Renderer[F, A] {
    override lazy val peer: ListCellRenderer[? >: A] = new ListCellRenderer[A] {
      def getListCellRendererComponent(list: JList[? <: A], a: A, index: Int, isSelected: Boolean, focused: Boolean): JComponent = {
        JLabel(dispatcher.unsafeRunSync[String](render(isSelected, focused, a, index)))
      }
    }
    protected def render(isSelected: Boolean, focused: Boolean, a: A, index: Int): F[String]
  }
  object Renderer {
    def apply[F[_]: Async, A](fn: (Boolean, Boolean, A, Int) => Resource[F, Component[F]]): Resource[F, Renderer[F, A]] = {
      for {
        dispatcher <- Dispatcher.sequential[F]
        hotswap <- Hotswap.create[F, Component[F]]
        res <- Async[F].delay { new AbstractRenderer[F, A](dispatcher, hotswap) { 
          override protected def render(s: Boolean, f: Boolean, a: A, index: Int) = fn(s, f, a, index)
        } }.toResource
      } yield res
    }
    def wrap[F[_]: Async, A](renderer: ListCellRenderer[? >: A]): F[Renderer[F, A]] = {
      Async[F].delay {
        new Renderer[F, A] {
          override lazy val peer = renderer
        }
      } 
    }
    def text[F[_]: Async, A](fn: (Boolean, Boolean, A, Int) => F[String]): Resource[F, Renderer[F, A]] = {
      for {
        dispatcher <- Dispatcher.sequential[F]
        res <- Async[F].delay { new TextRenderer[F, A](dispatcher) {
          override protected def render(s: Boolean, f: Boolean, a: A, i: Int) = fn(s, f, a, i)
        }}.toResource
      } yield res
    }
  }
}
trait WithRenderer[F[_]: Async, A] {
  def renderer: RefSink[F, ListView.Renderer[F, A]]
}
class ListView[F[_]: Async, A](dispatcher: Dispatcher[F], topic: Topic[F, event.Event[F]]) extends Component[F](topic, dispatcher) with WithRenderer[F, A] {
  override lazy val peer: JList[A] = new JList[A]() with SuperMixin
  def ensureIndexIsVisible(idx: Int): F[Unit] =
    Async[F].delay { peer.ensureIndexIsVisible(idx) }.evalOn(AwtEventDispatchEC)
  def fixedCellHeight: Ref[F, Int] =
    new WrappedRef(
      peer.getFixedCellHeight,
      peer.setFixedCellHeight,
      )
  def fixedCellWidth: Ref[F, Int] =
    new WrappedRef(
      peer.getFixedCellWidth,
      peer.setFixedCellWidth
      )

  protected class ModelWrapper[B](val items: Seq[B]) extends AbstractListModel[B] {
    def getElementAt(n: Int): B = items(n)
    def getSize: Int = items.size
  }
  def listData: Ref[F, Seq[A]] =
    new WrappedRef(
      () => peer.getModel match {
        case model: ModelWrapper[A] => model.items
        case model => new Seq[A] { selfSeq => 
          def length: Int = model.getSize
          def iterator: Iterator[A] = new Iterator[A] {
            var idx = 0
            def next(): A = { idx += 1; apply(idx - 1) }
            def hasNext: Boolean = idx < selfSeq.length
          }
          def apply(n: Int): A = model.getElementAt(n)
        }
      }, 
      items => 
        peer.setModel(new ModelWrapper[A](items))
      )
  def visibleRowCount: Ref[F, Int] =
    new WrappedRef(peer.getVisibleRowCount, peer.setVisibleRowCount)

  def renderer: RefSink[F, ListView.Renderer[F, A]] =
    WrappedSink[F, ListView.Renderer[F, A]](it => peer.setCellRenderer(it.peer))
} 
