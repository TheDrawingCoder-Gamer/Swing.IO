package net.bulbyvr.swing.io

import cats.effect.IO
import cats.effect.kernel.{Async, Ref}
import scala.swing

opaque type Swing[F[_]] = Async[F]

object Swing {
  implicit inline def forIO: Swing[IO] = IO.asyncForIO
  inline def forAsync[F[_]](using F: Async[F]): Swing[F] = F
}

trait SwingTag[F[_], E] private[io] (name: String)(using F: Async[F]) {
  protected def unsafeBuild: E
  private def build = F.delay(unsafeBuild)

}
opaque type UIElement[F[_]] <: Publisher[F] = swing.UIElement

opaque type Component[F[_]] <: UIElement[F] = swing.Component
object Component {
  extension[F[_]] (component: Component[F]) {
    def enabled(using Swing[F]): Ref[F, Boolean] = {
      new WrappedRef(() => component.enabled, component.enabled = _)
    } 
  }
}
opaque type Container[F[_]] <: UIElement[F] = swing.Container
opaque type ContainerWrapper[F[_]] <: Container[F] = swing.Container.Wrapper

opaque type SequentialContainer[F[_]] <: Container[F] = swing.SequentialContainer
opaque type SeqWrapper[F[_]] <: (SequentialContainer[F] & ContainerWrapper[F]) = swing.SequentialContainer.Wrapper
object SequentialContainer {
  extension[F[_]] (seqContainer: SequentialContainer[F]) {
    def children(using F: Swing[F]): Ref[F, Seq[Component[F]]] = {
      new WrappedRef(() => seqContainer.contents.toSeq, it => {
            seqContainer.contents.clear()
            // you're going to farewell
            seqContainer.contents ++= it
          }
          )
    }
    def append(comp: Component[F])(using F: Swing[F]): F[Unit] = F.delay {
      seqContainer.contents += comp
    }
    def prepend(comp: Component[F])(using F: Swing[F]): F[Unit] = F.delay {
      seqContainer.contents.prepend(comp)
    }
  }
}
opaque type Reactor[F[_]] = swing.Reactor
opaque type Publisher[F[_]] <: Reactor[F] = swing.Publisher

opaque type Panel[F[_]] <: (Component[F] & ContainerWrapper[F]) = swing.Panel
object Panel {
  extension[F[_]] (panel: Panel[F]) {
    def contents(using F: Swing[F]) = 
      F.delay(panel.contents)
  }
}

opaque type RootPanel[F[_]] <: Container[F] = swing.RootPanel
object RootPanel {
  extension[F[_]] (rootPanel: RootPanel[F]) {
    def child(using F: Swing[F]): Ref[F, Option[Component[F]]] = {
      new WrappedRef(() => rootPanel.contents.lift(0), it => {
        it match {
          case Some(value) => rootPanel.contents = value
          // TODO: warn
          case None => ()
        }
      })
    }
  }
}

opaque type Window[F[_]] <: (UIElement[F] & RootPanel[F]) = swing.Window
object Window {
  extension [F[_]](win: Window[F]) {
    def open(using F: Swing[F]): F[Unit] =
      F.delay { win.centerOnScreen(); win.open() }
    def close(using F: Swing[F]): F[Unit] =
      F.delay { win.close() }
  }
}


opaque type MenuBar[F[_]] <: (Component[F] & SeqWrapper[F]) = swing.MenuBar
object MenuBar {
  extension[F[_]] (menuBar: MenuBar[F]) {
    def exists(using F: Swing[F]): F[Boolean] = 
      F.delay { 
        menuBar match {
          case swing.MenuBar.NoMenuBar => false
          case _ => true 
        }
      }
  }
}
opaque type RichWindow[F[_]] <: Window[F] = swing.RichWindow
object RichWindow {
  extension [F[_]](richWindow: RichWindow[F]) {
    def menuBar(using F: Swing[F]): Ref[F, MenuBar[F]] = {
      new WrappedRef(() => richWindow.menuBar, richWindow.menuBar = _)
    }
  }
}

opaque type Frame[F[_]] <: RichWindow[F] = swing.Frame
opaque type MainFrame[F[_]] <: Frame[F] = swing.MainFrame

opaque type AbstractButton[F[_]] <: Component[F] = swing.AbstractButton

opaque type Button[F[_]] <: AbstractButton[F] = swing.Button

opaque type ToggleButton[F[_]] <: AbstractButton[F] = swing.ToggleButton

opaque type CheckBox[F[_]] <: ToggleButton[F] = swing.CheckBox

opaque type RadioButton[F[_]] <: ToggleButton[F] = swing.RadioButton

opaque type MenuItem[F[_]] <: AbstractButton[F] = swing.MenuItem

opaque type Menu[F[_]] <: MenuItem[F] = swing.Menu

opaque type RadioMenuItem[F[_]] <: MenuItem[F] = swing.RadioMenuItem

opaque type CheckMenuItem[F[_]] <: MenuItem[F] = swing.CheckMenuItem

opaque type ComboBox[F[_], A] <: Component[F] = helpers.MutableComboBox[A]
object ComboBox {
  extension[F[_], A] (comboBox: ComboBox[F, A]) {
    def clear(using F: Swing[F]): F[Unit] = F.delay { comboBox.items.removeAllElements() }
    def addAll(elems: TraversableOnce[A])(using F: Swing[F]): F[Unit] = F.delay { comboBox.items ++= elems }
    def ++=(elems: TraversableOnce[A])(using F: Swing[F]): F[Unit] = comboBox.addAll(elems)
    def add(elem: A)(using F: Swing[F]): F[Unit] = F.delay { comboBox.items += elem }
    def +=(elem: A)(using F: Swing[F]): F[Unit] = comboBox.add(elem)
    def value(using F: Swing[F]): Ref[F, A] = 
      new WrappedRef(() => comboBox.selection.item, comboBox.selection.item = _)
    def renderer(using F: Swing[F]): Ref[F, ItemRenderer[F, A]] = 
      new WrappedRef(() => comboBox.renderer, comboBox.renderer = _)

  }
}
opaque type ItemRenderer[F[_], -A] = swing.ListView.Renderer[A]
opaque type Label[F[_]] <: Component[F] = swing.Label


opaque type FlowPanel[F[_]] <: (Panel[F] & SeqWrapper[F]) = swing.FlowPanel

opaque type BoxPanel[F[_]] <: (Panel[F] & SeqWrapper[F]) = swing.BoxPanel

opaque type ListView[F[_], A] <: Component[F] = swing.ListView[A]

opaque type TextComponent[F[_]] <: Component[F] = swing.TextComponent


import swing.event as sevent
opaque type Event[F[_]] = sevent.Event

opaque type UIEvent[F[_]] <: Event[F] = sevent.UIEvent
object UIEvent {
  extension[F[_]] (event: UIEvent[F]) {
    def source(using F: Swing[F]): UIElement[F] = 
      event.source
  }
}

opaque type ComponentEvent[F[_]] <: UIEvent[F] = sevent.ComponentEvent
object ComponentEvent {
  extension[F[_]] (event: ComponentEvent[F]) {
    def source(using F: Swing[F]): Component[F] =
      event.source
  }
}

opaque type InputEvent[F[_]] <: ComponentEvent[F] = sevent.InputEvent
object InputEvent {
  extension[F[_]] (event: InputEvent[F]) {
    def modifiers: sevent.Key.Modifiers =
      event.modifiers
  }
}

opaque type MouseEvent[F[_]] <: InputEvent[F] = sevent.MouseEvent

opaque type MouseButtonEvent[F[_]] <: MouseEvent[F] = sevent.MouseButtonEvent

opaque type MouseClicked[F[_]] <: MouseButtonEvent[F] = sevent.MouseClicked

