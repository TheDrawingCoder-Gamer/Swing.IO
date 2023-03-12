# Core Concepts

Similar to **Calico**, **Swing.IO** takes its concepts from Cats Effect and FS2. This page lists some common idioms used in Swing.IO development.

Note: Some of these docs were copied from [Calico](https://www.armanbilge.com/calico/concepts.html), as these are very similar projects with a similar basis.

## Components and resource management

The most important idea of **Swing.IO** is that all components are represented as a `Resource[IO, Component[IO]]`.

```scala
import cats.effect.*
import net.bulbyvr.swing.io.wrapper.*

val component: Resource[IO, Component[IO]] = ???

// Or generally
def component[F[_]: Async]: Resource[F, Component[F]] = ???
```

The `Resource` handles management completely by itself. When it finalizes, it will clean up event listeners and cancel fibers.

Because a `Resource[IO, Component[IO]]` can be used multiple times, it can behave as a builder. 

One caveat with this is the main window - to handle closing correctly, it's expressed as a `Resource[IO, (MainFrame[IO], Deferred[F, Unit])]`. 
The `IOSwingApp` surrounds the lock and the main window takes the lock as an argument, which is completed when the app closes.

**Swing.IO** provides an idiomatic DSL for describing swing components.

```scala
import net.bulbyvr.swing.io.all.{*, given}
import cats.effect.*
import net.bulbyvr.swing.io.wrapper.*

val component: Resource[IO, Component[IO]] = box("Hello, checkbox:", checkbox())
```
In this example, a checkbox that does nothing composes together to create a `Resource`

## Signals

`Signal`s are time varying values. You can always obtain the current value and subscribe to stream of update events that notify when it is updated. 
This is ideal for use in UI Components: they can always render immediately with the current value, and re-render only when there are updates.

`Signal` is a monad, enabling them to be transformed with pure functions and composed with each other. 
Using transformation and composition, you can derive a `Signal` that contains precisely the data you are interested in.

```scala
import cats.syntax.all.*

case class S3Weapon(name: String)
enum Games {
	case Splatoon1
	case Splatoon2
	case Splatoon3
}
val signals = (
  SignallingRef[IO].of(S3Weapon("52 Gal")),
  SignallingRef[IO].of(Games.Splatoon3),
)

val weapons: Seq[S3Weapon] = Seq(???)

val app = signals.flatMap { (weaponSig, gameSig) => window(
  	 flow(
	    comboBox[S3Weapon].withSelf { self => (
	      items := weapons,
	      renderer := { (it: S3Weapon) => it.name },
	      onSelectionChange --> {
		_.foreach(_ => self.value.get.flatMap(weaponSig.set))
	      }
	    )},
	    comboBox[Games].withSelf { self => (
	      items := Seq(Games.Splatoon1, Games.Splatoon2, Games.Splatoon3),
	      onSelectionChange --> {
		_.foreach(_ => self.value.get.flatMap(gameSig.set))
	      }
	    )}
	 )
  ) 
  }
```

There are various ways to obtain a `Signal`.

- Create a `SignallingRef` with an initial value.
```scala
SignallingRef[IO].of("initial value")
```

- Derive a `Signal` from a `Stream`, by “holding” its latest value.
```scala
def stringStream: Stream[IO, String] = ???
stringStream.holdResource("initial value")
stringStream.holdOptionResource // use None for the intitial value
```

## Glitch-free rendering

Swing forces all access to swing components to be on the Event Dispatch Thread. This ensures that rendering is glitch-free. 
**Swing.IO** automatically handles this by evaluating all access to swing components on the EDT, and it lets Cats Effect
handle anything that doesn't touch swing components. This means you get all the benefits of multithreading your Swing app
without the hassle of having to deal with the EDT.
