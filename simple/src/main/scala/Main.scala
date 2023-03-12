import net.bulbyvr.swing.io.{*, given}, all.{*, given}
import fs2.{text as _, *}
import fs2.concurrent.{Signal, SignallingRef}
import cats.effect.syntax.all.*
import cats.effect.IO
import net.bulbyvr.swing.io.wrapper.event.*
import cats.effect.Resource
import cats.syntax.all.*

case class ComboBoxItem(item: String)
object Main extends IOSwingApp {
  val daItems = 
    Seq(
      ComboBoxItem("hi1"),
      ComboBoxItem("hi2"),
      ComboBoxItem("hi3")
      )
  def render = 
    (SignallingRef[IO].of(false), SignallingRef[IO].of(""), SignallingRef[IO].of(daItems.head)).tupled.toResource.flatMap { (smth, txt, curItem) => 
      window(
      box(
        label(text <-- smth.map(_.toString())),
        flow(
          button(
            text := "hi",
            onBtnClick --> {
              _.evalMap(_ => smth.get).foreach(it => IO.println(it) *> smth.set(!it))
            }
            ),
          textField.withSelf { self =>
            (
              columns := 10,
              onValueChange --> {
                _.evalMap(_ => self.text.get).foreach(txt.set)
              }
              )
          }
        ),
        checkbox.withSelf {self => (
          text := "boolean",
          selected <-- smth,
          onBtnClick --> {
            _.evalMap(_ => self.selected.get).foreach(smth.set)
          }
          )
        },
        comboBox[ComboBoxItem].withSelf { self => (
          items := this.daItems,
          onSelectionChange --> {
            _.evalMap(_ => self.item.get).foreach(curItem.set)
          },
          renderer := { (it: ComboBoxItem) => {
            it.item.pure[IO]
          } }
        )}
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

