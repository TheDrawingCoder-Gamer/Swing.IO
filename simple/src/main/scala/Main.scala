import net.bulbyvr.swing.io.*, all.{*, given}
import fs2.*
import fs2.concurrent.{Signal, SignallingRef}
import cats.effect.syntax.all.*
import cats.effect.IO
object Main extends IOSwingApp {
  def render = 
    SignallingRef[IO].of(false).toResource.flatMap { smth => 
      window(
      title := "Hello World!",
      box(
        "Hi!",
        button(
          onClick --> {
            _.evalMap(_ => smth.get).foreach(it => IO.println(it) *> smth.set(!it))
          } 
          )
        ))
    }
}

