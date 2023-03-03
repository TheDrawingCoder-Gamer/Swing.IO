import net.bulbyvr.swing.io.{*, given}, all.{*, given}
import fs2.{text as _, *}
import fs2.concurrent.{Signal, SignallingRef}
import cats.effect.syntax.all.*
import cats.effect.IO
object Main extends IOSwingApp {
  def render = 
    SignallingRef[IO].of(false).product(SignallingRef[IO].of("")).toResource.flatMap { (smth, txt) => 
      window(
      title := "Hello World!",
      box(
        "Hi!",
        button(
          text := "hi", 
          onClick.-->[ButtonClicked[IO]] {
            _.evalMap(_ => smth.get).foreach(it => IO.println(it) *> smth.set(!it))
          } 
          ),
        textField.withSelf { self =>
          (
            text <-- txt,
            onValueChanged.-->[ValueChanged[IO]] {
              _.evalMap(_ => self.text.get).foreach(txt.set)
            }
            )
        }
       )
      )
    }
}

