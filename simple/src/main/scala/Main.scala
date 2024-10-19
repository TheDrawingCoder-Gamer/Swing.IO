import net.bulbyvr.swing.io.{*, given}, all.{*, given}
import fs2.{text as _, *}
import fs2.concurrent.{Signal, SignallingRef}
import cats.effect.syntax.all.*
import cats.effect.IO
import net.bulbyvr.swing.io.wrapper.*
import net.bulbyvr.swing.io.wrapper.event.*
import cats.effect.Resource
import cats.syntax.all.*
import javax.swing.UIManager
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
      notebook(
        "Test" -> box(
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
                },
                text <-- txt
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
          flow(
            comboBox[ComboBoxItem].withSelf { self => (
              items := this.daItems,
              onSelectionChange --> {
                _.evalMap(_ => self.item.get).foreach(it => IO.println(it) *> curItem.set(it))
              },
              item <-- curItem,
              renderer := { (it: ComboBoxItem) => {
                it.item.pure[IO]
              } }
            )}
          ),
          button.withSelf { self =>( 
            text := "Select file",
            onBtnClick --> {
              _.evalMap(_ => FileChooser[IO].open(self)).foreach(_ => IO.unit)
            }
          )}
        
        ),
        "Sample 2" -> box(
          flow(label(text := "banana"), textField(columns := 10)),
          
          )
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

