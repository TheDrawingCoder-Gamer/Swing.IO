package net.bulbyvr.swing.io.helpers

class ComboModel[A] extends javax.swing.DefaultComboBoxModel[A] {
  def +=(elem: A) =  addElement(elem)
  def ++=(elems: TraversableOnce[A]) =  elems.foreach(addElement) 
}

