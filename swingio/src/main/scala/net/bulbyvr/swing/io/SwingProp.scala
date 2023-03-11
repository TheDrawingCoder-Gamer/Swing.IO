package net.bulbyvr.swing.io

import cats.effect.kernel.Async
import cats.effect.kernel.Resource
import fs2.concurrent.Signal
import fs2.*
import cats.syntax.all.*
import cats.effect.syntax.all.*
import scala.reflect.TypeTest
import scala.reflect.ClassTag
import java.awt.Window
import wrapper as swingio
sealed class SwingProp[F[_], A] private[io] {
  import SwingProp.*
  def :=[V](v: V): ConstantModifier[F, A, V] =
    ConstantModifier(v)
  def <--[V](vs: Signal[F, V]): SignalModifier[F, A, V] =
    SignalModifier(vs)
  def <--[V](vs: Resource[F, Signal[F, V]]): SignalResourceModifier[F, A, V] =
    SignalResourceModifier(vs)
  // def <--[V](v: Resource[F, V]): ResourceModifier[F, A, V] =
  //  ResourceModifier(v)
  def -->[Ev](listener: Pipe[F, Ev, Nothing]): PipeModifier[F, A, Ev] =
    PipeModifier(listener)
  // more specific listener that always infers correctly :sob:
  // Only really works with listen
  def ~~>(listener: Pipe[F, swingio.event.Event[F], Nothing]): PipeModifier[F, A, swingio.event.Event[F]] =
    PipeModifier(listener)
  // Option isn't real, it can't hurt you
  // inline def <--(vs: Signal[F, Option[V]]): OptionSignalModifier[F, E, V] =
  //  OptionSignalModifier(setter, vs)
  // inline def <--(vs: Resource[F, Signal[F, Option[V]]]): OptionSignalResourceModifier[F, E, V] =
  //  OptionSignalResourceModifier(setter, vs)
}

object SwingProp {
  trait Setter[F[_], E, A, V] {
    def set(elem: E, value: V): Resource[F, Unit]
  }
  trait Emits[F[_], E, A, Ev] {} 
  final class ConstantModifier[F[_], A, V] private[io] (
      private[io] val value: V
    )
  final class SignalModifier[F[_], A, V] private[io] (
      private[io] val values: Signal[F, V]
    )
  final class SignalResourceModifier[F[_], A, V] private[io] (
      private[io] val values: Resource[F, Signal[F, V]]
    )
  final class ResourceModifier[F[_], A, V] private[io] (
      private[io] val value: Resource[F, V]
    )
  final class OptionSignalModifier[F[_], A, V] private[io] (
      private[io] val values: Signal[F, Option[V]]
    )
  final class OptionSignalResourceModifier[F[_], A, V] private[io] (
      private[io] val values: Resource[F, Signal[F, Option[V]]]
    )
  final class PipeModifier[F[_], A, Ev] private[io] (
    private[io] val sink: Pipe[F, Ev, Nothing]

    )
  private[io] def listener[F[_], T <: swingio.event.Event[F]](target: swingio.WithTopic[F])
  (using F: Async[F], T: TypeTest[swingio.event.Event[F], T]): Stream[F, T] =
    target.topic.subscribe(5).collect { case e: T => e }
}

private trait PropModifiers[F[_]](using F: Async[F]) {
  import SwingProp.*
  given forConstantProp[A, E, V](using S: Setter[F, E, A, V]): Modifier[F, E, ConstantModifier[F, A, V]] =
    (m, n) => S.set(n, m.value)
  given forSignalProp[E, A, V](using S: Setter[F, E, A, V]): Modifier[F, E, SignalModifier[F, A, V]] =
    Modifier.forSignal[F, E, SignalModifier[F, A, V], V](_.values) { (m, n) => 
      it => S.set(n, it)
    }
  given forSignalResource[E, A, V](using S: Setter[F, E, A, V]): Modifier[F, E, SignalResourceModifier[F, A, V]] =
    Modifier.forSignalResource[F, E, SignalResourceModifier[F, A, V], V](_.values) {
      (m, n) => it => S.set(n, it)
    }
  given forResource[E, A, V](using S: Setter[F, E, A, V]): Modifier[F, E, ResourceModifier[F, A, V]] =
    (m, n) => m.value.map(S.set(n, _))

  given forPipeEventProp[E <: swingio.UIElement[F], A, Ev <: swingio.event.Event[F]](using E: Emits[F, E, A, Ev], T: TypeTest[swingio.event.Event[F], Ev])
  : Modifier[F, E, PipeModifier[F, A, Ev]] = 
    (m, t) => (F.cede *> listener[F, Ev](t).through(m.sink).compile.drain).background.void

}

private trait Props[F[_]](using A: Async[F]) extends LowPriorityProps[F] {
  import SwingProp.*
  def prop[A]: SwingProp[F, A] =
    SwingProp[F, A]
  //given textBtn[E <: AbstractButton[F]]: Setter[F, E, "text", String] =
  //  (e, v) => e.text.set(v)
  given textMost[E <: swingio.WithChangableText[F]]: Setter[F, E, "text", String] =
    (e, v) => e.text.set(v).toResource
  //given txtCompText[E <: TextComponent[F]]: Setter[F, E, "text", String] =
  //  (e, v) => e.text.set(v)
  lazy val text: SwingProp[F, "text"] = prop["text"]
  given richWindowTitle[E <: swingio.RichWindow[F]]: Setter[F, E, "title", String] =
    (e, v) => e.title.set(v).toResource
  lazy val title: SwingProp[F, "title"] = prop["title"]
  given rootPanelChild[E <: swingio.RootPanel[F], C <: swingio.Component[F]]: Setter[F, E, "child", C] =
    (e, v) => e.child.set(Some(v)).toResource
  lazy val child: SwingProp[F, "child"] =
    prop["child"]

  given btnClick[E <: swingio.AbstractButton[F]]: Emits[F, E, "onClick", swingio.event.ButtonClicked[F]] = new Emits {}
  lazy val onClick: SwingProp[F, "onClick"] =
    prop["onClick"]

  export swingio.Orientation
  given orientationProp[E <: swingio.BoxPanel[F]]: Setter[F, E, "orientation", swingio.Orientation] =
    (e, v) => e.orientation.set(v).toResource
  lazy val orientation: SwingProp[F, "orientation"] =
    prop["orientation"]

  // Listen allows one to listen to ALL possible events
  given listenGiven[E <: swingio.UIElement[F]]: Emits[F, E, "listen", swingio.event.Event[F]] = new Emits {}

  lazy val listen: SwingProp[F, "listen"] =
    prop["listen"]

  def listening(listenTo: Pipe[F, swingio.event.Event[F], Nothing]): PipeModifier[F, "listen", swingio.event.Event[F]] =
    PipeModifier(listenTo)

  export swingio.FlowPanel.Alignment as FlowAlignment
  given alignmentProp[E <: swingio.FlowPanel[F]]: Setter[F, E, "alignment", swingio.FlowPanel.Alignment] =
    (e, v) => e.alignment.set(v).toResource
  lazy val alignment = prop["alignment"]

  given selectedProp[E <: swingio.AbstractButton[F]]: Setter[F, E, "selected", Boolean] =
    (e, v) => e.selected.set(v).toResource
  lazy val selected = prop["selected"]

  given colsProp[E <: swingio.TextComponent.HasColumns[F]]: Setter[F, E, "columns", Int] =
    (e, v) => e.columns.set(v).toResource

  lazy val columns = prop["columns"]

  given rowsProp[E <: swingio.TextComponent.HasRows[F]]: Setter[F, E, "rows", Int] =
    (e, v) => e.rows.set(v).toResource

  lazy val rows = prop["rows"]

  given rendererProp[A, E <: swingio.WithRenderer[F, A]]: Setter[F, E, "renderer", A => F[String]] =
    (e, v) => swingio.ListView.Renderer.text[F, A] {
      (s: Boolean, f: Boolean, a: A, i: Int) => v(a)
    }.evalMap(e.renderer.set)
  lazy val renderer = prop["renderer"]

  given itemForComboBox[A, E <: swingio.ComboBox[F, A]]: Setter[F, E, "items", Seq[A]] =
    (e, v) => e.items.set(v).toResource
  lazy val items = prop["items"]
}

private trait LowPriorityProps[F[_]] (using F: Async[F]) {
  import SwingProp.*
  given compClick[E <: swingio.Component[F]]: Emits[F, E, "onClick", swingio.event.MouseClicked[F]] = new Emits {}

}



