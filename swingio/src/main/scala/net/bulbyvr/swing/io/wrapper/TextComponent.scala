package net.bulbyvr.swing.io
package wrapper

import cats.effect.{Async, Ref}
import cats.effect.std.Dispatcher
import fs2.concurrent.Topic
import javax.swing.text.JTextComponent
// import javax.swing.event.{DocumentEvent, DocumentListener}
import java.awt.event.{InputMethodListener, InputMethodEvent}
import cats.effect.syntax.all.*
import cats.syntax.all.*

object TextComponent {
  trait HasColumns[F[_]](using F: Async[F]) extends TextComponent[F] {
    def columns: Ref[F, Int] 
  }
  trait HasRows[F[_]](using F: Async[F]) extends TextComponent[F] {
    def rows: Ref[F, Int]
  }
}
class TextComponent[F[_]](topic: Topic[F, event.Event[F]], dispatcher: Dispatcher[F])(using F: Async[F])
  extends Component[F](topic, dispatcher) 
  with WithChangableText[F] {
    override lazy val peer: JTextComponent = new JTextComponent with SuperMixin {}
    def text: Ref[F, String] =
      new WrappedRef(
          peer.getText,
          peer.setText
        )
    // TODO: Caret
    def editable: Ref[F, Boolean] =
      new WrappedRef(
          peer.isEditable,
          peer.setEditable
        )
    def cut: F[Unit] = F.delay { peer.cut() }.evalOn(AwtEventDispatchEC)
    def copy: F[Unit] = F.delay { peer.copy() }.evalOn(AwtEventDispatchEC)
    def paste: F[Unit] = F.delay { peer.paste() }.evalOn(AwtEventDispatchEC)

    def selected: F[String] = F.delay { peer.getSelectedText() }.evalOn(AwtEventDispatchEC)

    def selectAll: F[Unit] = F.delay { peer.selectAll() }.evalOn(AwtEventDispatchEC)
    /*
    private lazy val dl = new DocumentListener {
      def changedUpdate(e: DocumentEvent) = {
        dispatcher.unsafeRunAndForget(topic.publish1(new event.ValueChanged(TextComponent.this)))
      }
      def insertUpdate(e: DocumentEvent) = {
        dispatcher.unsafeRunAndForget(topic.publish1(new event.ValueChanged(TextComponent.this)))
      }
      def removeUpdate(e: DocumentEvent) = {
        dispatcher.unsafeRunAndForget(topic.publish1(new event.ValueChanged(TextComponent.this)))
      }
    }
    */
    private lazy val iml = new InputMethodListener {
      def inputMethodTextChanged(e: InputMethodEvent) = {
        dispatcher.unsafeRunAndForget(topic.publish1(new event.ValueChanged(TextComponent.this)))
      }
      def caretPositionChanged(e: InputMethodEvent) = {}
    }
    override def onFirstSubscribe =
      super.onFirstSubscribe *> F.delay { peer.addInputMethodListener(iml) }.evalOn(AwtEventDispatchEC)
    override def onLastUnsubscribe =
      super.onLastUnsubscribe *> F.delay { peer.removeInputMethodListener(iml) }.evalOn(AwtEventDispatchEC)
}
