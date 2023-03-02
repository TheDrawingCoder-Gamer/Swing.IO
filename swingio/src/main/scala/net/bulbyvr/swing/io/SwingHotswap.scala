package net.bulbyvr.swing.io

import cats.effect.kernel.Concurrent
import cats.effect.kernel.Resource
import cats.effect.syntax.all.*
import cats.syntax.all.*


private abstract class SwingHotswap[F[_], A] {
  def swap(next: Resource[F, A])(render: (A, A) => F[Unit]): F[Unit]
}

private object SwingHotswap {
  def apply[F[_], A](init: Resource[F, A])(
      using F: Concurrent[F]
    ): Resource[F, (SwingHotswap[F, A], A)] = 
      Resource.make(init.allocated.flatMap(F.ref(_)))(_.get.flatMap(_._2)).evalMap { active => 
        val hs = new SwingHotswap[F, A] {
          def swap(next: Resource[F, A])(render: (A, A) => F[Unit]) = F.uncancelable { poll =>
            for {
              nextAllocated <- poll(next.allocated)
              (oldA, oldFinalizer) <- active.getAndSet(nextAllocated)
              newA = nextAllocated._1
              _ <- render(oldA, newA) *> F.cede *> oldFinalizer
            } yield ()
          }
        }
        active.get.map(_._1).tupleLeft(hs)
      }
}
