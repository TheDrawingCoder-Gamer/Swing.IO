package net.bulbyvr.swing.io

import cats.effect.kernel.Async
import cats.effect.kernel.Resource
import fs2.concurrent.Signal
import scala.swing 
import fs2.*
import cats.syntax.all.*
import cats.effect.syntax.all.*
import scala.reflect.TypeTest
import scala.reflect.ClassTag
sealed class SwingProp[F[_], A] private[io] {
  import SwingProp.*
  def :=[E, V](v: V)(using S: Setter[F, E, A, V]): ConstantModifier[F, E, V] =
    ConstantModifier(S.set, v)
  def <--[E, V](vs: Signal[F, V])(using S: Setter[F, E, A, V]): SignalModifier[F, E, V] =
    SignalModifier(S.set, vs)
  def <--[E, V](vs: Resource[F, Signal[F, V]])(using S: Setter[F, E, A, V]): SignalResourceModifier[F, E, V] =
    SignalResourceModifier(S.set, vs)
  def <--[E, V](v: Resource[F, V])(using S: Setter[F, E, A, V]): ResourceModifier[F, E, V] =
    ResourceModifier(S.set, v)
  def -->[E, Ev, REv](listener: Pipe[F, Ev, Nothing])(using E: Emits[F, E, A, Ev, REv]): PipeModifier[F, Ev, REv] =
    PipeModifier(E.wrapper, listener)
  // Option isn't real, it can't hurt you
  // inline def <--(vs: Signal[F, Option[V]]): OptionSignalModifier[F, E, V] =
  //  OptionSignalModifier(setter, vs)
  // inline def <--(vs: Resource[F, Signal[F, Option[V]]]): OptionSignalResourceModifier[F, E, V] =
  //  OptionSignalResourceModifier(setter, vs)
}

object SwingProp {
  trait Setter[F[_], E, A, V] {
    def set(elem: E, value: V): F[Unit]
  }
  trait Emits[F[_], E, A, Ev, REv] {
    def wrapper(e: REv): Ev
  } 
  final class ConstantModifier[F[_], -E, V] private[io] (
      private[io] val setter: (E, V) => F[Unit],
      private[io] val value: V
    )
  final class SignalModifier[F[_], -E, V] private[io] (
      private[io] val setter: (E, V) => F[Unit],
      private[io] val values: Signal[F, V]
    )
  final class SignalResourceModifier[F[_], -E, V] private[io] (
      private[io] val setter: (E, V) => F[Unit],
      private[io] val values: Resource[F, Signal[F, V]]
    )
  final class ResourceModifier[F[_], -E, V] private[io] (
      private[io] val setter: (E, V) => F[Unit],
      private[io] val value: Resource[F, V]
    )
  final class OptionSignalModifier[F[_], -E, V] private[io] (
      private[io] val setter: (E, V) => F[Unit],
      private[io] val values: Signal[F, Option[V]]
    )
  final class OptionSignalResourceModifier[F[_], -E, V] private[io] (
      private[io] val setter: (E, V) => F[Unit],
      private[io] val values: Resource[F, Signal[F, Option[V]]]
    )
  final class PipeModifier[F[_], A, Raw] private[io] (
    private[io] val wrapper: Raw => A,
    private[io] val sink: Pipe[F, A, Nothing]

    )
  private[io] def listener[F[_], T <: Event[F], Raw <: swing.event.Event](target: Reactor[F], wrapper: Raw => T)
  (using F: Async[F], T: TypeTest[swing.event.Event, Raw]): Stream[F, T] =
    Stream.repeatEval {
      F.async[T] { cb => 
        F.delay {
          val fn: PartialFunction[swing.event.Event, Unit] = { 
            case e: Raw =>
              cb(Right(wrapper(e)))
          }

          target.reactions += fn
          Some(F.delay(target.reactions -= fn))
        }
      }
    }
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

  given forPipeEventProp[E <: UIElement[F] & Reactor[F], A <: Event[F], E2 >: E <: UIElement[F], Raw <: swing.event.Event]
    (using T: TypeTest[swing.event.Event, Raw])
  : Modifier[F, E, PipeModifier[F, A, Raw]] = 
    (m, t) => (F.cede *> listener[F, A, Raw](t, m.wrapper).through(m.sink).compile.drain).background.void

}

private trait Props[F[_]](using F: Swing[F], A: Async[F]) {
  import SwingProp.*
  def prop[A]: SwingProp[F, A] =
    SwingProp[F, A]
  given textBtn[E <: AbstractButton[F]]: Setter[F, E, "text", String] =
    (e, v) => e.text.set(v)
  given labelText[E <: Label[F]]: Setter[F, E, "text", String] =
    (e, v) => e.text.set(v)
  lazy val text: SwingProp[F, "text"] = prop["text"]
  given richWindowTitle[E <: RichWindow[F]]: Setter[F, E, "title", String] =
    (e, v) => e.title.set(v)
  lazy val title: SwingProp[F, "title"] = prop["title"]
  given rootPanelChild[E <: RootPanel[F]]: Setter[F, E, "child", Component[F]] =
    (e, v) => e.child.set(Some(v))
  lazy val child: SwingProp[F, "child"] =
    prop["child"]
  given btnClick[E <: Button[F]]: Emits[F, E, "onClick", ButtonClicked[F], swing.event.ButtonClicked] =
    it => ButtonClicked[F](it)
  given compClick[E <: Component[F]]: Emits[F, E, "onClick", MouseClicked[F], swing.event.MouseClicked] =
    MouseClicked.apply(_)
  lazy val onClick: SwingProp[F, "onClick"] =
    prop["onClick"]

}





