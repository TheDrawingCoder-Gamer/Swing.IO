package net.bulbyvr.swing.io

import cats.effect.kernel.Async
import cats.effect.kernel.{Resource, Ref}
import cats.effect.syntax.all.*
import cats.syntax.all.*
import scala.swing
import shapeless3.deriving.K0
import cats.effect.std.Dispatcher
import fs2.concurrent.Channel
final class SwingElem[F[_], E] private[io] (constructor: F[E])(using F: Async[F]) {
  def apply[M](modifier: M)(using M: Modifier[F, E, M]): Resource[F, E] =
    constructor.toResource.flatTap(M.modify(modifier, _))
  def withSelf[M](mkModifier: E => M)(using M: Modifier[F, E, M]): Resource[F, E] =
    constructor.toResource.flatTap(e => M.modify(mkModifier(e), e))
}
// FIXME: I'm going to hell for this
private[io] class StinkyMainFrame extends swing.MainFrame {
  override def dispose(): Unit = {
    sys.exit(0)
  }
}
private trait SwingElems[F[_]](using F: Async[F]) {
  private[io] def swingElem[Raw, T](cons: F[Raw]): SwingElem[F, T] =
    // TODO: do this w/o casting
    SwingElem(cons.asInstanceOf[F[T]])
  lazy val label: SwingElem[F, Label[F]] = swingElem[swing.Label, Label[F]](F.delay { swing.Label()})
  lazy val window: SwingElem[F, MainFrame[F]] = swingElem[swing.MainFrame, MainFrame[F]](F.delay { new StinkyMainFrame {} })
  lazy val flow: SwingElem[F, FlowPanel[F]] = swingElem[swing.FlowPanel, FlowPanel[F]](F.delay(new swing.FlowPanel()))
  lazy val box: SwingElem[F, BoxPanel[F]] = swingElem[swing.BoxPanel, BoxPanel[F]](F.delay(new swing.BoxPanel(swing.Orientation.Vertical)))
  lazy val button: SwingElem[F, Button[F]] = swingElem[swing.Button, Button[F]](F.delay(new swing.Button()))
  lazy val textField: SwingElem[F, TextField[F]] = swingElem[swing.TextField, TextField[F]](F.delay(new swing.TextField()))
}
