package net.bulbyvr.swing.io
package wrapper

import javax.swing.UIManager
object LookAndFeel {
  def defaultLookAndFeel: String = {
    val os = sys.props("os.name")
    if (os.contains("nux") || os.contains("nix")) {
      UIManager.getInstalledLookAndFeels().find(_.getName == "GTK+").map(_.getClassName).getOrElse(UIManager.getCrossPlatformLookAndFeelClassName())
    } else {
      UIManager.getSystemLookAndFeelClassName()
    }
  }
}
