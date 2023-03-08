package net.bulbyvr.swing.io
package wrapper
import cats.effect.syntax.all.*
import cats.effect.{Async, Ref, Resource}
import fs2.concurrent.Topic
import cats.effect.std.Dispatcher
import cats.syntax.all.*
import javax.swing.{AbstractButton as JAbstractButton, JButton}
import java.awt.event.ActionListener
abstract class AbstractButton[F[_]](dispatcher: Dispatcher[F], topic: Topic[F, event.Event[F]])(using F: Async[F]) 
  extends Component[F](topic, dispatcher) 
  with WithChangableText[F] { outer =>
  override lazy val peer: JAbstractButton = new JAbstractButton with SuperMixin {}

  def click: F[Unit] = F.delay { peer.doClick() }.evalOn(AwtEventDispatchEC)
  def clickN(n: Int): F[Unit] = F.delay { peer.doClick(n) }.evalOn(AwtEventDispatchEC)

  lazy val a: ActionListener = _ => {
    dispatcher.unsafeRunAndForget(outer.topic.publish1(event.ButtonClicked(outer)))
  }

  override def onFirstSubscribe = 
    F.delay { peer.addActionListener(a) }.evalOn(AwtEventDispatchEC)
  override def onLastUnsubscribe =
    F.delay { peer.removeActionListener(a) }.evalOn(AwtEventDispatchEC)

  def text: Ref[F, String] =
    new WrappedRef(peer.getText, peer.setText)
}

class Button[F[_]](dispatcher: Dispatcher[F], topic: Topic[F, event.Event[F]])(using F: Async[F]) 
  extends AbstractButton[F](dispatcher, topic) {
  override lazy val peer: JButton = new JButton("") with SuperMixin


}

object Button {
  def apply[F[_]](using F: Async[F]): Resource[F, Button[F]] =
    for {
      topic <- Topic[F, event.Event[F]].toResource
      dispatcher <- Dispatcher.sequential[F]
      res <- F.delay { new Button[F](dispatcher, topic) }.toResource.flatTap(_.setup)
    } yield res
}
