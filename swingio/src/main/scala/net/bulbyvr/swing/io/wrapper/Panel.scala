package net.bulbyvr.swing.io
package wrapper

import cats.effect.{Async, Resource}
import cats.effect.syntax.all.*
import cats.effect.std.Dispatcher
import fs2.concurrent.Topic
import javax.swing.BoxLayout
import cats.syntax.all.*
abstract class Panel[F[_]](topic: Topic[F, event.Event[F]], dispatcher: Dispatcher[F])(using F: Async[F]) extends Component[F](topic, dispatcher) with MutableContainer[F] {
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
class BoxPanel[F[_]](orientation: Orientation, topic: Topic[F, event.Event[F]], dispatcher: Dispatcher[F])(using F: Async[F])
  extends Panel[F](topic, dispatcher) {
  override lazy val peer: javax.swing.JPanel = {
    val p = new javax.swing.JPanel with SuperMixin
    val l = new BoxLayout(p, orientation.asInt)
    p.setLayout(l)
    p
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
