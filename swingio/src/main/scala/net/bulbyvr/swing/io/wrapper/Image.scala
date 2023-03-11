package net.bulbyvr.swing.io
package wrapper
import cats.effect.Async
// TODO: Better wrapper
class Image[F[_]: Async](private[io] val peer: java.awt.Image)
