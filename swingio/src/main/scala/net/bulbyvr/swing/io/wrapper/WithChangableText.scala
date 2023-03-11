package net.bulbyvr.swing.io
package wrapper
import cats.effect.{Async, Ref}

trait WithChangableText[F[_]](using F: Async[F]) {
  def text: Ref[F, String]
}
