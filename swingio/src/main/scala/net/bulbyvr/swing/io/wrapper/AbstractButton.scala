package net.bulbyvr.swing.io
package wrapper
import cats.effect.syntax.all.*
import cats.effect.{Async, Ref, Resource}
import fs2.concurrent.Topic
import cats.effect.std.Dispatcher
import cats.syntax.all.*
import javax.swing.{AbstractButton as JAbstractButton, JButton, JToggleButton, JCheckBox, JRadioButton}
import java.awt.event.ActionListener
abstract class AbstractButton[F[_]](dispatcher: Dispatcher[F], topic: Topic[F, event.Event[F]])(using F: Async[F]) 
  extends Component[F](topic, dispatcher) 
  with WithChangableText[F] 
  with WithIcon[F] { outer =>
  override lazy val peer: JAbstractButton = new JAbstractButton with SuperMixin {}

  /**
   * Trigger a click on this button
   */
  def click: F[Unit] = F.delay { peer.doClick() }.evalOn(AwtEventDispatchEC)
  /**
   * Trigger a click with n clicks on this button
   * @param n number of clicks
   */
  def clickN(n: Int): F[Unit] = F.delay { peer.doClick(n) }.evalOn(AwtEventDispatchEC)

  lazy val a: ActionListener = _ => {
    // TODO: This is overly accepting
    dispatcher.unsafeRunAndForget(outer.topic.publish1(event.ButtonClicked(outer)))
  }

  override def onFirstSubscribe = 
    F.delay { peer.addActionListener(a) }.evalOn(AwtEventDispatchEC)
  override def onLastUnsubscribe =
    F.delay { peer.removeActionListener(a) }.evalOn(AwtEventDispatchEC)

  def text: Ref[F, String] =
    new WrappedRef(peer.getText, peer.setText)

  def selected: Ref[F, Boolean] =
    new WrappedRef(peer.isSelected, peer.setSelected)

  def icon: Ref[F, Option[Icon[F]]] =
    new FWrappedRef(F.delay { Option(peer.getIcon) }.flatMap(_.traverse(Icon[F](_))),
      it => F.delay {
        peer.setIcon(it.map(_.peer).orNull)
      }
      )
  def iconTextGap: Ref[F, Int] =
    new WrappedRef(peer.getIconTextGap, peer.setIconTextGap)
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

class ToggleButton[F[_]](dispatcher: Dispatcher[F], topic: Topic[F, event.Event[F]])(using F: Async[F])
  extends AbstractButton[F](dispatcher, topic) {
  override lazy val peer: JToggleButton = new JToggleButton("") with SuperMixin
}

class CheckBox[F[_]](dispatcher: Dispatcher[F], topic: Topic[F, event.Event[F]])(using F: Async[F])
  extends ToggleButton[F](dispatcher, topic) {
  override lazy val peer: JCheckBox = new JCheckBox("") with SuperMixin
}

object CheckBox {
  def apply[F[_]: Async] = {
    UIElement.buildApply[F, CheckBox[F]]((d, t) => new CheckBox(d, t))
  }
}
class RadioButton[F[_]](dispatcher: Dispatcher[F], topic: Topic[F, event.Event[F]])(using F: Async[F])
  extends ToggleButton[F](dispatcher, topic) {
  override lazy val peer: JRadioButton = new JRadioButton("") with SuperMixin
}

object RadioButton {
  def apply[F[_]: Async] =
    UIElement.buildApply[F, RadioButton[F]]((d, t) => new RadioButton(d, t))
}
