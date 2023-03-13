# Common Gotchas

**Swing.IO** has a fundementally different base from **Calico**. While it tries to be similar, some **Calico**
patterns are unsupported.

```scala
textField.withSelf { self =>
  (
    text <-- txtRef,
    onValueChange --> {
      _.foreach(_ => self.value.get.flatMap(txtRef.set))
    }
  )
}
```
This pattern causes an infinite loop that consumes all memory in the app. 

## Look And Feel
When setting the Look and Feel of an app, Resources can't be prematurely initialized. 

```scala
for {
  comp <- resourceComponent
  win <- window(
    lookAndFeel := ???,
    // Don't do this, comp will be styled with the default look and feel
    comp
  )
} yield win
```

Instead, obtain the resource directly. **Swing.IO** supports composing resources together, and it handles the 
lifecycles itself. 

```scala
for {
  comp = resourceComponent
  win <- window(
    lookAndFeel := ???,
    comp
  )
} yield win
```

Look and feel can't be updated, as the components style won't update.

## Circumventing DSL
If you have to access a feature that isn't implemented in the DSL yet, you can use flatTaps.
```scala
checkbox(icon := ???).flatTap(_.iconTextGap.set(20).toResource)
```
## Adding custom components
You can technically make your own component by extending a wrapper class. However, it requires wrapping a peer. WrappedRef is exposed to allow for wrapping.
See source code for examples of wrapping.
