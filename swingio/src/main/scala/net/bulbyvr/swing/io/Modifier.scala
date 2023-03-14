package net.bulbyvr.swing.io

import cats.effect.kernel.Async
import cats.effect.kernel.Resource
import shapeless3.deriving.K0
import cats.Id
import cats.syntax.all.*
import cats.Foldable
import fs2.concurrent.Signal
import cats.effect.syntax.all.*
import cats.Contravariant
import wrapper as swingio
import fs2.Stream
trait Modifier[F[_], E, A] { outer => 
  def modify(a: A, e: E): Resource[F, Unit]
  
  inline final def contramap[B](inline f: B => A): Modifier[F, E, B] =
    (b: B, e: E) => outer.modify(f(b), e)
}

object Modifier {
  private val _forUnit: Modifier[Id, Any, Unit] =
    (_, _) => Resource.unit
  inline given forUnit[F[_], E]: Modifier[F, E, Unit] = 
    _forUnit.asInstanceOf[Modifier[F, E, Unit]]
  given forTuple[F[_], E, M <: Tuple](
      using inst: K0.ProductInstances[[X] =>> Modifier[F, E, X], M]
    ): Modifier[F, E, M] = (m, e) => 
      inst.foldLeft(m)(Resource.unit[F]) {
        [a] => (r: Resource[F, Unit], m: Modifier[F, E, a], a: a) => r *> m.modify(a, e)
      }
  given forList[F[_], E, A](using M: Modifier[F, E, A]): Modifier[F, E, List[A]] =
    (as, e) => as.foldMapM(M.modify(_, e)).void
  given forResource[F[_], E, A](using M: Modifier[F, E, A]): Modifier[F, E, Resource[F, A]] = 
    (a, e) => a.flatMap(M.modify(_, e))
  private val _contravariant: Contravariant[[X] =>> Modifier[Id, Any, X]] = new {
    def contramap[A, B](fa: Modifier[Id, Any, A])(f: B => A) = 
      fa.contramap(f)
  }
  inline given contravariant[F[_], E]: Contravariant[[X] =>> Modifier[F, E, X]] = 
    _contravariant.asInstanceOf[Contravariant[[X] =>> Modifier[F, E, X]]]

  private[io] def forSignal[F[_], E, M, V](signal: M => Signal[F, V])(
    mkModify: (M, E) => V => Resource[F, Unit])(using F: Async[F]): Modifier[F, E, M] = (m, e) => 
    signal(m).getAndDiscreteUpdates.flatMap { (head, tail) => 
      val modify = mkModify(m, e)
      modify(head) *>
        (F.cede *> tail.flatMap(it => Stream.resource[F, Unit](modify(it))).compile.drain).background.void
    }
  private[io] def forSignalResource[F[_], E, M, V](signal: M => Resource[F, Signal[F, V]])(
    mkModify: (M, E) => V => Resource[F, Unit])(using F: Async[F]): Modifier[F, E, M] = (m, e) => {
      signal(m).flatMap { sig => 
        sig.getAndDiscreteUpdates.flatMap { (head, tail) => 
          val modify = mkModify(m, e)
          modify(head) *>
            (F.cede *> tail.flatMap(it => Stream.resource(modify(it))).compile.drain).background.void
        }
      }
    }
}

private trait Modifiers[F[_]](using F: Async[F]) {
  given forString[E <: swingio.MutableContainer[F]]: Modifier[F, E, String] = 
    (a, e) => {
      swingio.Label[F](a).flatMap(l => e.content.addOne(l).toResource)
    }
  given forRootString[E <: swingio.SingletonContainer[F]]: Modifier[F, E, String] =
    (a, e) => {
      swingio.Label[F](a).flatMap(l => e.child.set(Some(l)).toResource) 
    }
    /*
  private val _forStringSignal: Modifier[F, swing.SequentialContainer, Signal[F, String]] = (s, e) => {
    s.getAndDiscreteUpdates.flatMap { (head, tail) => 
      Resource
        .eval(F.delay { 
          val label = swing.Label(head) 
          e.contents += label 
          label
        })
        .flatMap { n => 
          (F.cede *> tail.foreach(t => F.delay(n.text = t)).compile.drain).background
        }
        .void
    }
  }
  inline given forStringSignal[E <: SequentialContainer[F]]: Modifier[F, E, Signal[F, String]] = 
    _forStringSignal.asInstanceOf[Modifier[F, E, Signal[F, String]]]

  private val _forStringOptionSignal: Modifier[F, swing.SequentialContainer, Signal[F, Option[String]]] =
    _forStringSignal.contramap(_.map(_.getOrElse("")))

  inline given forStringOptionSignal[E <: SequentialContainer[F]]: Modifier[F, E, Signal[F, Option[String]]] = 
    _forStringOptionSignal.asInstanceOf[Modifier[F, E, Signal[F, Option[String]]]]

  private val _forComponent: Modifier[F, swing.SequentialContainer, swing.Component] = (a, e) => {
    Resource.eval {
      F.delay {
        e.contents += a
        ()
      }
    }
  }
  */ 
  given forComponent[E <: swingio.MutableContainer[F], C <: swingio.Component[F]]: Modifier[F, E, C] = { (a, e) =>
    e.content.addOne(a).toResource
  }
  
  given forRootComponent[E <: swingio.SingletonContainer[F], C <: swingio.Component[F]]: Modifier[F, E, C] = { (a, e) =>
    e.child.set(Some(a)).toResource
  }


  given forComponentResource[E <: swingio.MutableContainer[F], C <: swingio.Component[F]]: Modifier[F, E, Resource[F, C]] = 
    Modifier.forResource


  given forRootComponentResource[E <: swingio.SingletonContainer[F], C <: swingio.Component[F]]: Modifier[F, E, Resource[F, C]] =
    Modifier.forResource
  given forComponentSignal[E <: swingio.MutableContainer[F], C <: swingio.Component[F]]: Modifier[F, E, Signal[F, Resource[F, C]]] = { (cs, e) =>
    cs.getAndDiscreteUpdates.flatMap { (head, tail) =>
      SwingHotswap(head).flatMap { (hs, c2) =>
        e.content.addOne(c2).toResource *>
        (F.cede *> tail
          .foreach(hs.swap(_)((c2, c3) => {
            for {
              contents <- e.contents.get
              idx = contents.indexOf(c2)
              _ <- 
                if (idx != -1)
                  e.content(idx) = c3
                else
                  F.unit
            } yield ()
          }))
          .compile
          .drain
          ).background
      }.void
    } 
  }
  // TODO: forComponentOptionSignal
}
