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
    mkModify: (M, E) => V => F[Unit])(using F: Async[F]): Modifier[F, E, M] = (m, e) => 
    signal(m).getAndDiscreteUpdates.flatMap { (head, tail) => 
      val modify = mkModify(m, e)
      Resource.eval(modify(head)) *>
        (F.cede *> tail.foreach(modify(_)).compile.drain).background.void
    }
  private[io] def forSignalResource[F[_], E, M, V](signal: M => Resource[F, Signal[F, V]])(
    mkModify: (M, E) => V => F[Unit])(using F: Async[F]): Modifier[F, E, M] = (m, e) => {
      signal(m).flatMap { sig => 
        sig.getAndDiscreteUpdates.flatMap { (head, tail) => 
          val modify = mkModify(m, e)
          Resource.eval(modify(head)) *>
            (F.cede *> tail.foreach(modify(_)).compile.drain).background.void
        }
      }
    }
}

private trait Modifiers[F[_]](using F: Async[F]) {
  private val _forString: Modifier[F, swing.SequentialContainer, String] = (s, e) => {
    Resource.eval {
      F.delay {
        e.contents += swing.Label(s)
        ()
      }
    }
  }
  inline given forString[E <: SequentialContainer[F]]: Modifier[F, E, String] = 
    _forString.asInstanceOf[Modifier[F, E, String]]
  private val _forRootString: Modifier[F, swing.RootPanel, String] = (s, e) => {
    Resource.eval {
      F.delay { e.contents = swing.Label(s)}
    }
  }
  inline given forRootString[E <: RootPanel[F]]: Modifier[F, E, String] =
    _forRootString.asInstanceOf[Modifier[F, E, String]]
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
  
  inline given forComponent[E <: SequentialContainer[F], C <: Component[F]]: Modifier[F, E, C] =
    _forComponent.asInstanceOf[Modifier[F, E, C]]
  private val _forRootComponent: Modifier[F, swing.RootPanel, swing.Component] = (a, e) => {
    Resource.eval {
      F.delay {
        e.contents = a
        ()
      }
    }
  }
  
  inline given forRootComponent[E <: RootPanel[F], C <: Component[F]]: Modifier[F, E, C] =
    _forRootComponent.asInstanceOf[Modifier[F, E, C]]

  private val _forComponentResource: Modifier[F, swing.SequentialContainer, Resource[F, swing.Component]] = 
    Modifier.forResource(using _forComponent)

  inline given forComponentResource[E <: SequentialContainer[F], C <: Component[F]]: Modifier[F, E, Resource[F, C]] =
    _forComponentResource.asInstanceOf[Modifier[F, E, Resource[F, C]]]

  private val _forRootComponentResource: Modifier[F, swing.RootPanel, Resource[F, swing.Component]] = 
    Modifier.forResource(using _forRootComponent)

  inline given forRootComponentResource[E <: RootPanel[F], C <: Component[F]]: Modifier[F, E, Resource[F, C]] =
    _forRootComponentResource.asInstanceOf[Modifier[F, E, Resource[F, C]]]
  private val _forComponentSignal: Modifier[F, swing.SequentialContainer, Signal[F, Resource[F, swing.Component]]] = 
    (cs, e) => 
      cs.getAndDiscreteUpdates.flatMap { (head, tail) => 
        SwingHotswap(head).flatMap { (hs, c2) => 
          F.delay(e.contents += c2).toResource *>
          (F.cede *> tail
            .foreach(hs.swap(_)((c2, c3) => F.delay {
              val idx = e.contents.indexOf(c2)
              if (idx != -1) {
                e.contents(idx) = c3
              } 
              ()
            }))
            .compile
            .drain
            ).background
        }.void
      }
  inline given forComponentSignal[E <: SequentialContainer[F], C <: Component[F]]: Modifier[F, E, Signal[F, Resource[F, C]]] =
    _forComponentSignal.asInstanceOf[Modifier[F, E, Signal[F, Resource[F, C]]]]

  // TODO: forComponentOptionSignal
}
