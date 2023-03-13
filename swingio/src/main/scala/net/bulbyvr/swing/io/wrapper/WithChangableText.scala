package net.bulbyvr.swing.io
package wrapper
import cats.effect.{Async, Ref}

trait WithChangableText[F[_]](using F: Async[F]) {
  /**
   * Text stored in this instance
   *
   * What this data represents depends on the class. A text field 
   * would represent the text inside the field, a label would be the
   * label text, etc.
   */
  def text: Ref[F, String]
}
