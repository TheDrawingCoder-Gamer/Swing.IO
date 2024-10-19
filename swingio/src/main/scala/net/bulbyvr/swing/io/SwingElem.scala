package net.bulbyvr.swing.io

import cats.effect.kernel.Async
import cats.effect.kernel.{Resource, Ref, Deferred}
import cats.effect.syntax.all.*
import cats.syntax.all.*
import shapeless3.deriving.K0
import cats.effect.std.Dispatcher
import fs2.concurrent.Channel
import wrapper as swingio
sealed trait SwingElem[F[_], E, R] private[io] {
  def apply[M](modifier: M)(using M: Modifier[F, E, M]): Resource[F, R]
  def withSelf[M](mkModifier: E => M)(using
      M: Modifier[F, E, M]
  ): Resource[F, R]

}
final class NormalElem[F[_], E] private[io] (val constructor: Resource[F, E])(
    using F: Async[F]
) extends SwingElem[F, E, E] {
  def apply[M](modifier: M)(using M: Modifier[F, E, M]): Resource[F, E] =
    constructor.flatTap(M.modify(modifier, _))
  def withSelf[M](mkModifier: E => M)(using
      M: Modifier[F, E, M]
  ): Resource[F, E] =
    constructor.flatTap(e => M.modify(mkModifier(e), e))
}
final class MainElem[F[_]] private[io] (
    val fullConstructor: Resource[F, (swingio.MainFrame[F], Deferred[F, Unit])]
)(using F: Async[F])
    extends SwingElem[F, swingio.MainFrame[
      F
    ], (swingio.MainFrame[F], Deferred[F, Unit])] {
  override def apply[M](modifier: M)(using
      M: Modifier[F, swingio.MainFrame[F], M]
  ): Resource[F, (swingio.MainFrame[F], Deferred[F, Unit])] =
    fullConstructor.flatTap((l, _) => M.modify(modifier, l))
  override def withSelf[M](mkModifier: swingio.MainFrame[F] => M)(using
      M: Modifier[F, swingio.MainFrame[F], M]
  ): Resource[F, (swingio.MainFrame[F], Deferred[F, Unit])] =
    fullConstructor.flatTap((l, _) => M.modify(mkModifier(l), l))
}
private trait SwingElems[F[_]](using F: Async[F]) {
  lazy val label: NormalElem[F, swingio.Label[F]] = NormalElem(
    swingio.Label("")
  )
  lazy val window: MainElem[F] = MainElem(swingio.MainFrame[F])
  lazy val flow: NormalElem[F, swingio.FlowPanel[F]] = NormalElem(
    swingio.FlowPanel[F](swingio.FlowPanel.Alignment.Center)
  )
  lazy val box =
    NormalElem[F, swingio.BoxPanel[F]](
      swingio.BoxPanel[F](swingio.Orientation.Vertical)
    )
  lazy val button: NormalElem[F, swingio.Button[F]] = NormalElem(
    swingio.Button[F]
  )
  lazy val textField: NormalElem[F, swingio.TextField[F]] = NormalElem(
    swingio.TextField[F]
  )
  lazy val textArea: NormalElem[F, swingio.TextArea[F]] = NormalElem(
    swingio.TextArea[F]
  )
  lazy val checkbox: NormalElem[F, swingio.CheckBox[F]] = NormalElem(
    swingio.CheckBox[F]
  )
  lazy val radio: NormalElem[F, swingio.RadioButton[F]] = NormalElem(
    swingio.RadioButton[F]
  )
  lazy val slider: NormalElem[F, swingio.Slider[F]] = NormalElem(
    swingio.Slider[F]
  )
  lazy val scrollPane: NormalElem[F, swingio.ScrollPane[F]] = NormalElem(
      swingio.ScrollPane[F]
    )
  def comboBox[A]: NormalElem[F, swingio.ComboBox[F, A]] = NormalElem(
    swingio.ComboBox[F, A]
  )
  def listView[A]: NormalElem[F, swingio.ListView[F, A]] = NormalElem(
    swingio.ListView[F][A]
  )
  lazy val notebook =
    NormalElem[F, swingio.TabbedPane[F]](swingio.TabbedPane[F])
}
