package net.bulbyvr.swing.io

import scala.concurrent.ExecutionContext
import javax.swing.SwingUtilities
object AwtEventDispatchEC extends ExecutionContext {
  def execute(r: Runnable): Unit = SwingUtilities.invokeLater(r)
  def reportFailure(t: Throwable): Unit = {
    println("oopsies")
    t.printStackTrace()
  }
}
