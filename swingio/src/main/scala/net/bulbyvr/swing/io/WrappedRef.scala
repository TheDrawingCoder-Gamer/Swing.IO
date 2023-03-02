package net.bulbyvr.swing.io
import cats.data.State
import cats.effect.kernel.Ref
import cats.effect.kernel.Sync
import cats.syntax.all.*


private final class WrappedRef[F[_], A](
    unsafeGet: () => A,
    unsafeSet: A => Unit
)(implicit F: Sync[F])
    extends Ref[F, A] {

  def get: F[A] = F.delay(unsafeGet())

  def set(a: A): F[Unit] = F.delay(unsafeSet(a))

  def access: F[(A, A => F[Boolean])] = F.delay {
    val snapshot = unsafeGet()
    val setter = (a: A) =>
      F.delay {
        if (unsafeGet() == snapshot) {
          unsafeSet(a)
          true
        } else false
      }
    (snapshot, setter)
  }

  def tryUpdate(f: A => A): F[Boolean] =
    update(f).as(true)

  def tryModify[B](f: A => (A, B)): F[Option[B]] =
    modify(f).map(Some(_))

  def update(f: A => A): F[Unit] =
    F.delay(unsafeSet(f(unsafeGet())))

  def modify[B](f: A => (A, B)): F[B] =
    F.delay {
      val (a, b) = f(unsafeGet())
      unsafeSet(a)
      b
    }

  def tryModifyState[B](state: State[A, B]): F[Option[B]] =
    tryModify(state.run(_).value)

  def modifyState[B](state: State[A, B]): F[B] =
    modify(state.run(_).value)

}
