package net.bulbyvr.swing.io
package wrapper

import cats.effect.{Async, Ref}
import javax.swing.{Icon as JIcon, ImageIcon as JImageIcon}
import cats.effect.syntax.all.*
trait Icon[F[_]](using F: Async[F]) {
  def peer: JIcon
  def height: F[Int] = F.delay { peer.getIconHeight }.evalOn(AwtEventDispatchEC)
  def width: F[Int] = F.delay { peer.getIconWidth }.evalOn(AwtEventDispatchEC)
}
object Icon {
  def apply[F[_]: Async](daPeer: JIcon) =
    Async[F].delay { new Icon[F] { lazy val peer = daPeer } }
}
class ImageIcon[F[_]](image0: Image[F])(using F: Async[F]) extends Icon[F] {
  override lazy val peer: JImageIcon = new JImageIcon(image0.peer)
  def image: Ref[F, Image[F]] =
    new WrappedRef(() => new Image(peer.getImage), it => peer.setImage(it.peer))
  def description: Ref[F, String] =
    new WrappedRef(peer.getDescription, peer.setDescription)
}
trait WithIcon[F[_]: Async] {
  def icon: Ref[F, Option[Icon[F]]]
  def iconTextGap: Ref[F, Int]
}
