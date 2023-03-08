package net.bulbyvr.swing.io
package wrapper

// stealing? what is that : )
import scala.collection.{Iterator, mutable}

/**
 * Default partial implementation for buffer adapters.
 */
abstract class BufferWrapper[A] extends mutable.Buffer[A] {
  type MoreElem[+B] = IterableOnce[B]

  override def clear(): Unit = for (_ <- 0 until length) remove(0)

  override def update(n: Int, a: A): Unit = {
    remove(n)
    insert(n, a)
  }

  override def iterator: Iterator[A] = Iterator.range(0, length).map(apply)

  override def prepend(elem: A): this.type = { insert(0, elem); this }

  override def insertAll(idx: Int, elems: MoreElem[A]): Unit = {
    var i = idx
    for (el <- elems.iterator) {
      insert(i, el)
      i += 1
    }
  }

  override def remove(idx: Int, count: Int): Unit = {
    require(count >= 0)
    var n = 0
    while (n < count) {
      remove(idx + n)
      n += 1
    }
  }

  override def patchInPlace(from: Int, patch: MoreElem[A], replaced: Int): this.type = {
    if (replaced > 0) {
      remove(from, replaced)
    }
    insertAll(from, patch)
    this
  }
}
