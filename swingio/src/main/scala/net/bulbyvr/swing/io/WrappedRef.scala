package net.bulbyvr.swing.io
import cats.data.State
import cats.effect.kernel.Ref
import cats.effect.kernel.Async
import cats.syntax.all.*
import cats.effect.syntax.all.*

private final class WrappedRef[F[_], A](
    unsafeGet: () => A,
    unsafeSet: A => Unit
)(implicit F: Async[F])
    extends Ref[F, A] {

  def get: F[A] = F.delay(unsafeGet()).evalOn(AwtEventDispatchEC)

  def set(a: A): F[Unit] = F.delay(unsafeSet(a)).evalOn(AwtEventDispatchEC)

  def access: F[(A, A => F[Boolean])] = F.delay {
    val snapshot = unsafeGet()
    val setter = (a: A) =>
      F.delay {
        if (unsafeGet() == snapshot) {
          unsafeSet(a)
          true
        } else false
      }.evalOn(AwtEventDispatchEC)
    (snapshot, setter)
  }.evalOn(AwtEventDispatchEC)

  def tryUpdate(f: A => A): F[Boolean] =
    update(f).as(true)

  def tryModify[B](f: A => (A, B)): F[Option[B]] =
    modify(f).map(Some(_))

  def update(f: A => A): F[Unit] =
    F.delay(unsafeSet(f(unsafeGet()))).evalOn(AwtEventDispatchEC)

  def modify[B](f: A => (A, B)): F[B] =
    F.delay {
      val (a, b) = f(unsafeGet())
      unsafeSet(a)
      b
    }.evalOn(AwtEventDispatchEC)

  def tryModifyState[B](state: State[A, B]): F[Option[B]] =
    tryModify(state.run(_).value)

  def modifyState[B](state: State[A, B]): F[B] =
    modify(state.run(_).value)

}

final class FWrappedRef[F[_], A](
    getter: F[A],
    setter: A => F[Unit]
  )(using F: Async[F])
    extends Ref[F, A] {
  def get: F[A] = getter.evalOn(AwtEventDispatchEC)
  def set(a: A): F[Unit] = setter(a).evalOn(AwtEventDispatchEC)

  def access: F[(A, A => F[Boolean])] = 
    for {
      snapshot <- get
      setter = (a: A) => {
        for {
          now <- get
          res <-
            if (now == snapshot) {
              set(a) *> true.pure[F]
            } else {
              false.pure[F]
            }
        } yield res

      }
    } yield (snapshot, setter)
  def update(f: A => A): F[Unit] =
    get.map(f).flatMap(set)
  def modify[B](f: A => (A, B)): F[B] =
    // I LOVE FUTURE SOURCE
    // I have complained about this before : )
    for {
      (a, b) <- get.map(f)
      _ <- set(a)
    } yield b
  def tryUpdate(f: A => A): F[Boolean] =
    update(f).as(true)
  def tryModify[B](f: A => (A, B)): F[Option[B]] =
    modify(f).map(Some(_))
  def tryModifyState[B](state: State[A, B]): F[Option[B]] =
    tryModify(state.run(_).value)
  def modifyState[B](state: State[A, B]): F[B] =
    modify(state.run(_).value)
}
