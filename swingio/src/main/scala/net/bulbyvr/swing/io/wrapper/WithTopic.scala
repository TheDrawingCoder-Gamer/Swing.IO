package net.bulbyvr.swing.io
package wrapper

import cats.effect.{Async, Resource}
import fs2.concurrent.Topic
import wrapper.event.Event
import cats.effect.syntax.all.*
import cats.syntax.all.*
import fs2.Stream
trait WithTopic[F[_]](val topic: Topic[F, Event[F]])(using F: Async[F]) {

  protected def onFirstSubscribe: F[Unit]
  protected def onLastUnsubscribe: F[Unit]

  private[io] def setup: Resource[F, Unit] = {
    topic.subscribers.sliding(2).evalMap { chunk =>
      val l = chunk(0)
      val r = chunk(1)
      if (l > 1 && r == 1) 
        onLastUnsubscribe
      else if (l == 0 && r > 0) 
        onFirstSubscribe
      else
        F.unit
    }.compile.drain.background.void
  }
}

