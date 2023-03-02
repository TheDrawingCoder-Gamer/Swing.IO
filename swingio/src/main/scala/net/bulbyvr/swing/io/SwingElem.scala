package net.bulbyvr.swing.io

import cats.effect.kernel.Async
import cats.effect.kernel.Resource
import cats.effect.syntax.all.*
import cats.syntax.all.*
import scala.swing
final class SwingElem[F[_], E <: UIElement[F]] private[io] (constructor: () => E)(using F: Async[F]) {
  private def build = F.delay(constructor())
  def apply[M](modifier: M)(using M: Modifier[F, E, M]): Resource[F, E] =
    build.toResource.flatTap(M.modify(modifier, _))
  def withSelf[M](mkModifier: E => M)(using M: Modifier[F, E, M]): Resource[F, E] =
    build.toResource.flatTap(e => M.modify(mkModifier(e), e))
}

private trait SwingElems[F[_]](using Async[F]) {
  private[io] def swingElem[Raw <: swing.UIElement, T <: UIElement[F]](cons: () => Raw): SwingElem[F, T] =
    SwingElem(() => cons().asInstanceOf[T])
  lazy val label: SwingElem[F, Label[F]] = swingElem[swing.Label, Label[F]](() => swing.Label())
  lazy val window: SwingElem[F, MainFrame[F]] = swingElem[swing.MainFrame, MainFrame[F]](() => new swing.MainFrame {})
  lazy val flow: SwingElem[F, FlowPanel[F]] = swingElem[swing.FlowPanel, FlowPanel[F]](() => new swing.FlowPanel())
  lazy val box: SwingElem[F, BoxPanel[F]] = swingElem[swing.BoxPanel, BoxPanel[F]](() => new swing.BoxPanel(swing.Orientation.Vertical))
  lazy val button: SwingElem[F, Button[F]] = swingElem[swing.Button, Button[F]](() => new swing.Button())
}
