package net.bulbyvr.swing.io

import cats.effect.kernel.Async
import cats.effect.kernel.Resource
import fs2.concurrent.Signal
import scala.swing 
import fs2.*
import cats.syntax.all.*
import cats.effect.syntax.all.*
import scala.reflect.TypeTest
sealed class SwingProp[F[_], -E <: UIElement[F], V] private[io](setter: (E, V) => F[Unit]) {
  import SwingProp.*
  def :=(v: V): ConstantModifier[F, E, V] =
    ConstantModifier(setter, v)
  def <--(vs: Signal[F, V]): SignalModifier[F, E, V] =
    SignalModifier(setter, vs)
  def <--(vs: Resource[F, Signal[F, V]]): SignalResourceModifier[F, E, V] =
    SignalResourceModifier(setter, vs)
  def <--(v: Resource[F, V]): ResourceModifier[F, E, V] =
    ResourceModifier(setter, v)
  // Option isn't real, it can't hurt you
  // inline def <--(vs: Signal[F, Option[V]]): OptionSignalModifier[F, E, V] =
  //  OptionSignalModifier(setter, vs)
  // inline def <--(vs: Resource[F, Signal[F, Option[V]]]): OptionSignalResourceModifier[F, E, V] =
  //  OptionSignalResourceModifier(setter, vs)
}

object SwingProp {

  final class ConstantModifier[F[_], -E <: UIElement[F], V] private[io] (
      private[io] val setter: (E, V) => F[Unit],
      private[io] val value: V
    )
  final class SignalModifier[F[_], -E <: UIElement[F], V] private[io] (
      private[io] val setter: (E, V) => F[Unit],
      private[io] val values: Signal[F, V]
    )
  final class SignalResourceModifier[F[_], -E <: UIElement[F], V] private[io] (
      private[io] val setter: (E, V) => F[Unit],
      private[io] val values: Resource[F, Signal[F, V]]
    )
  final class ResourceModifier[F[_], -E <: UIElement[F], V] private[io] (
      private[io] val setter: (E, V) => F[Unit],
      private[io] val value: Resource[F, V]
    )
  final class OptionSignalModifier[F[_], -E <: UIElement[F], V] private[io] (
      private[io] val setter: (E, V) => F[Unit],
      private[io] val values: Signal[F, Option[V]]
    )
  final class OptionSignalResourceModifier[F[_], -E <: UIElement[F], V] private[io] (
      private[io] val setter: (E, V) => F[Unit],
      private[io] val values: Resource[F, Signal[F, Option[V]]]
    )
}

private trait PropModifiers[F[_]](using F: Async[F]) {
  import SwingProp.*

  given forConstantProp[E <: UIElement[F], E2 >: E <: UIElement[F], V]: Modifier[F, E, ConstantModifier[F, E2, V]] =
    (m, n) => Resource.eval(m.setter(n, m.value))
  given forSignalProp[E <: UIElement[F], E2 >: E <: UIElement[F], V]: Modifier[F, E, SignalModifier[F, E2, V]] =
    Modifier.forSignal[F, E, SignalModifier[F, E2, V], V](_.values) { (m, n) => 
      it => m.setter(n, it)
    }
  given forSignalResource[E <: UIElement[F], E2 >: E <: UIElement[F], V]: Modifier[F, E, SignalResourceModifier[F, E2, V]] =
    Modifier.forSignalResource[F, E, SignalResourceModifier[F, E2, V], V](_.values) {
      (m, n) => it => m.setter(n, it)
    }
  given forResource[E <: UIElement[F], E2 >: E <: UIElement[F], V]: Modifier[F, E, ResourceModifier[F, E2, V]] =
    (m, n) => m.value.map(m.setter(n, _))

}

private trait Props[F[_]](using F: Async[F]) {
  def prop[Raw <: swing.UIElement, T <: UIElement[F], V](setter: (Raw, V) => Unit): SwingProp[F, T, V] =
    SwingProp[F, T, V]((e, v) => F.delay(setter(e.asInstanceOf[Raw], v)))
  lazy val lblText: SwingProp[F, Label[F], String] = 
    prop[swing.Label, Label[F], String]((e, v) => e.text = v)
  lazy val text: SwingProp[F, TextComponent[F], String] =
    prop[swing.TextComponent, TextComponent[F], String]((e, v) => e.text = v)
  lazy val title: SwingProp[F, Window[F], String] =
    prop[swing.Frame, Window[F], String](_.title = _)
  lazy val child: SwingProp[F, RootPanel[F], Component[F]] =
    prop[swing.RootPanel, RootPanel[F], Component[F]]((e, v) => e.contents = v.asInstanceOf[swing.Component])
}

sealed class EventProp[F[_], -C <: UIElement[F], A <: Event[F], REvent <: swing.event.Event] private[io] (getter: C => swing.Reactor) {
  import EventProp.*
  def -->(sink: Pipe[F, A, Nothing]): PipeModifier[F, C, A, REvent] =
    PipeModifier(getter, sink)


}

object EventProp {
  final class PipeModifier[F[_], -C <: UIElement[F], A <: Event[F], REvent <: swing.event.Event] private[io] (
    private[io] val getter: C => swing.Reactor,
    private[io] val sink: Pipe[F, A, Nothing]

    )
  private[io] def listener[F[_], Raw <: swing.event.Event, T <: Event[F]](target: swing.Reactor)(using F: Async[F], T: TypeTest[swing.event.Event, Raw]): Stream[F, T] =
    Stream.repeatEval {
      F.async[T] { cb => 
        F.delay {
          val fn: PartialFunction[swing.event.Event, Unit] = { 
            case e: Raw => 
              cb(Right(e.asInstanceOf[T]))
            
          }
          target.reactions += fn
          Some(F.delay(target.reactions -= fn))
        }
      }
    }
  // TODO: functor instance
}

private trait EventProps[F[_]](using F: Async[F]) {
  private def eventProp[Raw <: swing.UIElement, T <: UIElement[F], REvent <: swing.event.Event, AEvent <: Event[F]](getter: Raw => swing.Reactor)(using T: TypeTest[swing.event.Event, REvent]) =
    EventProp[F, T, AEvent, REvent](it => getter(it.asInstanceOf[Raw]))
  lazy val onClick: EventProp[F, Component[F], MouseClicked[F], swing.event.MouseClicked] = eventProp[swing.Component, Component[F], swing.event.MouseClicked, MouseClicked[F]](_.mouse.clicks)
}
private trait EventPropModifiers[F[_]](using F: Async[F]) {
  import EventProp.*
  given forPipeEventProp[E <: UIElement[F], Raw <: swing.event.Event, A <: Event[F], E2 >: E <: UIElement[F]](using T: TypeTest[swing.event.Event, Raw]): Modifier[F, E, PipeModifier[F, E2, A, Raw]] = 
    (m, t) => (F.cede *> listener[F, Raw, A](m.getter(t)).compile.drain).background.void
}
