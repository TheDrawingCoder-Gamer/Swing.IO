# Core Concepts

Similar to **Calico**, **Swing.IO** takes its concepts from Cats Effect and FS2. This page lists some common idioms used in Swing.IO development.

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

TODO: Complete docs LATER™
