import net.bulbyvr.swing.io.{*, given}, all.{*, given}
import fs2.{text as _, *}
import fs2.concurrent.{Signal, SignallingRef}
import cats.effect.syntax.all.*
import cats.effect.IO
import net.bulbyvr.swing.io.wrapper.event.*
import cats.effect.Resource
import cats.syntax.all.*

object Main extends IOSwingApp {
  def render = 
    SignallingRef[IO].of(false).product(SignallingRef[IO].of("")).toResource.flatMap { (smth, txt) => 
      window(
      box(
        label(text <-- smth.map(_.toString())),
        button(
          text := "hi",
          listen ~~>  {
            _.collect { case ButtonClicked(_) => () }.evalMap(_ => smth.get).foreach(it => IO.println(it) *> smth.set(!it))
          }
          ),
        textField.withSelf { self =>
          (
            text <-- txt,
            listen ~~> {
              _.collect { case _: ValueChanged[IO] => () }.evalMap(_ => self.text.get).foreach(txt.set)
            }
            )
        }
      ),
    
      title := "Hello World!",
      /*
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
     */
      )
    }
}

