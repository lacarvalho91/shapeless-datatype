package shapeless.datatype.avro

import org.apache.avro.Schema
import org.apache.avro.specific.SpecificRecord
import shapeless._

import scala.collection.JavaConverters._
import scala.language.experimental.macros
import scala.reflect.macros.whitebox

@macrocompat.bundle
class AvroLabelledMacros(val c: whitebox.Context)
  extends SingletonTypeUtils with CaseClassMacros with AvroMacros {
  import c.universe._

  def mkDefaultSymbolicLabellingImpl[T](implicit tTag: WeakTypeTag[T]): Tree = {
    val tTpe = weakTypeOf[T]
    val labels: List[String] = schemaOf(tTpe).getFields.asScala.map(_.name()).toList

    val labelTpes = labels.map(SingletonSymbolType(_))
    val labelValues = labels.map(mkSingletonSymbol)

    val labelsTpe = mkHListTpe(labelTpes)
    val labelsValue =
      labelValues.foldRight(q"_root_.shapeless.HNil": Tree) {
        case (elem, acc) => q"_root_.shapeless.::($elem, $acc)"
      }

    q"""
      new _root_.shapeless.DefaultSymbolicLabelling[$tTpe] {
        type Out = $labelsTpe
        def apply(): $labelsTpe = $labelsValue
      } : _root_.shapeless.DefaultSymbolicLabelling.Aux[$tTpe, $labelsTpe]
    """
  }
}

@macrocompat.bundle
class AvroGenericMacros(val c: whitebox.Context) extends AvroMacros {
  import c.universe._
  import internal.constantType
  import Flag._

  def materializeAvro[T: WeakTypeTag, R: WeakTypeTag]: Tree = {
    val tpe = weakTypeOf[T]

    q"""
       new Generic[$tpe] {
         type Repr = _root_.shapeless.HNil
         def to(p: $tpe): Repr = HNil
         def from(p: Repr): $tpe = null
       }.asInstanceOf[_root_.shapeless.Generic.Aux[$tpe, _root_.shapeless.HNil]]
     """
  }
}

@macrocompat.bundle
trait AvroMacros {
  val c: whitebox.Context

  import c.universe._

  def schemaOf(tTpe: Type): Schema =
    Class.forName(tTpe.typeSymbol.fullName)
      .getMethod("getClassSchema")
      .invoke(null)
      .asInstanceOf[Schema]

  def isAvro(tpe: Type): Boolean = tpe =:= typeOf[SpecificRecord]
}