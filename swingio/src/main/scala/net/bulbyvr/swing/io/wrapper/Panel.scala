package net.bulbyvr.swing.io
package wrapper

import cats.effect.{Async, Resource, Ref}
import cats.effect.syntax.all.*
import cats.effect.std.Dispatcher
import fs2.concurrent.Topic
import javax.swing.BoxLayout
import java.awt.FlowLayout
import cats.syntax.all.*
abstract class Panel[F[_]](topic: Topic[F, event.Event[F]], dispatcher: Dispatcher[F])(using F: Async[F]) extends Component[F](topic, dispatcher) with Container[F] {
  override lazy val peer: javax.swing.JPanel = new javax.swing.JPanel with SuperMixin
}

enum Orientation {
  case Vertical
  case Horizontal
  case LineAxis
  case PageAxis
  def asInt: Int =
    this match {
      case Vertical => 
        BoxLayout.Y_AXIS
      case Horizontal =>
        BoxLayout.X_AXIS
      case LineAxis =>
        BoxLayout.LINE_AXIS
      case PageAxis =>
        BoxLayout.PAGE_AXIS
    }
}
object Orientation {
  def ofInt(i: Int) = {
    i match {
      case BoxLayout.Y_AXIS => Orientation.Vertical
      case BoxLayout.X_AXIS => Orientation.Horizontal
      case BoxLayout.LINE_AXIS => Orientation.LineAxis
      case BoxLayout.PAGE_AXIS => Orientation.PageAxis
    }
  }
}
class BoxPanel[F[_]](orientation0: Orientation, topic: Topic[F, event.Event[F]], dispatcher: Dispatcher[F])(using F: Async[F])
  extends Panel[F](topic, dispatcher)
  with MutableContainer[F] {
  override lazy val peer: javax.swing.JPanel = {
    val p = new javax.swing.JPanel with SuperMixin
    val l = new BoxLayout(p, orientation0.asInt)
    p.setLayout(l)
    p
  }
  protected final def layout = peer.getLayout.asInstanceOf[BoxLayout]
  
  def orientation: Ref[F, Orientation] = {
    new WrappedRef(
      () => Orientation.ofInt(layout.getAxis),
      it => {
        val l = new BoxLayout(peer, it.asInt)
        peer.setLayout(l)
        ()
      }
      )
  }

}

object BoxPanel {
  def apply[F[_]](orientation: Orientation)(using F: Async[F]): Resource[F, BoxPanel[F]] = {
    for {
      topic <- Topic[F, event.Event[F]].toResource
      dispatcher <- Dispatcher.sequential[F]
      res <- F.delay { new BoxPanel(orientation, topic, dispatcher) }.toResource.flatTap(_.setup).evalOn(AwtEventDispatchEC)
    } yield res
  }
}
object FlowPanel {
  enum Alignment {
    import FlowLayout.*
    case Leading
    case Trailing
    case Left
    case Right
    case Center
    def id =
      this match {
        case Leading => LEADING
        case Trailing => TRAILING
        case Left => LEFT
        case Right => RIGHT
        case Center => CENTER
      }
  }
  object Alignment {
    def of(id: Int) = 
      id match {
        case FlowLayout.LEADING => Alignment.Leading
        case FlowLayout.TRAILING => Alignment.Trailing
        case FlowLayout.LEFT => Alignment.Left
        case FlowLayout.RIGHT => Alignment.Right
        case FlowLayout.CENTER => Alignment.Center
      }
  }
  def apply[F[_]](alignment: FlowPanel.Alignment)(using F: Async[F]): Resource[F, FlowPanel[F]] = {
    for {
      topic <- Topic[F, event.Event[F]].toResource
      dispatcher <- Dispatcher.sequential[F]
      res <- F.delay { new FlowPanel(alignment, topic, dispatcher) }.toResource.flatTap(_.setup).evalOn(AwtEventDispatchEC)
    } yield res
  }
}
class FlowPanel[F[_]](alignment0: FlowPanel.Alignment, topic: Topic[F, event.Event[F]], dispatcher: Dispatcher[F])
  (using F: Async[F]) 
  extends Panel[F](topic, dispatcher) 
  with MutableContainer[F] {
  override lazy val peer: javax.swing.JPanel =
    new javax.swing.JPanel(new FlowLayout(alignment0.id)) with SuperMixin
  private def layoutManager: FlowLayout = peer.getLayout.asInstanceOf[FlowLayout]


  def alignment: Ref[F, FlowPanel.Alignment] =
    new WrappedRef(
      () => FlowPanel.Alignment.of(layoutManager.getAlignment),
      it => layoutManager.setAlignment(it.id)
      )
  def vGap: Ref[F, Int] =
    new WrappedRef(
      layoutManager.getVgap,
      layoutManager.setVgap
      )
  def hGap: Ref[F, Int] =
    new WrappedRef(
      layoutManager.getHgap,
      layoutManager.setHgap
      )
}
