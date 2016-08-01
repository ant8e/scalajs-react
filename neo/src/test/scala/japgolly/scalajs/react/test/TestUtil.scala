package japgolly.scalajs.react.test

import org.scalajs.dom.{document, html}
import scala.io.AnsiColor._
import scala.scalajs.js
import scalaz.{Equal, Maybe}
import scalaz.syntax.equal._
import utest.CompileError
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.PackageBase._

object TestUtil extends TestUtil

trait TestUtil
  extends scalaz.std.StringInstances
     with scalaz.std.StreamInstances
     with scalaz.std.VectorInstances
     with scalaz.std.SetInstances
     with scalaz.std.TupleInstances
     with scalaz.std.OptionInstances
     with scalaz.std.AnyValInstances
     with scalaz.std.ListInstances {

  implicit val equalNull: Equal[Null] = Equal.equalA
  implicit val equalKey : Equal[Key ] = Equal.equalA

  // TODO erm... not really. Only allow in raw testing
  implicit val equalReactNodeList: Equal[raw.ReactNodeList] = Equal.equalA
  implicit val equalRawKey       : Equal[raw.Key          ] = Equal.equalA
  implicit val equalRawRef       : Equal[raw.Ref          ] = Equal.equalA
  implicit val equalState        : Equal[raw.State        ] = Equal.equalA

  def assertEq[A: Equal](actual: A, expect: A): Unit =
    assertEq(null, actual, expect)

  def assertEq[A: Equal](name: => String, actual: A, expect: A): Unit =
    if (actual ≠ expect) {
      println()
      Option(name).foreach(n => println(s">>>>>>> $n"))

      val toString: Any => String = {
        case s: Stream[_] => s.force.toString() // SI-9266
        case a            => a.toString
      }

      var as = toString(actual)
      var es = toString(expect)
      var pre = "["
      var post = "]"
      if ((as + es) contains "\n") {
        pre = "↙[\n"
      }
      println(s"expect: $pre$BOLD$BLUE$es$RESET$post")
      println(s"actual: $pre$BOLD$RED$as$RESET$post")
      println()
      assert(false)
    }

  def fail(msg: String, clearStackTrace: Boolean = true): Nothing = {
    val t = new AssertionError(msg)
    if (clearStackTrace)
      t.setStackTrace(Array.empty)
    throw t
  }

  implicit class AnyTestExt[A](a: A) {

    def some: Option[A] = Some(a)
    def none: Option[A] = None

    def jsdef: js.UndefOr[A] = a
    def undef: js.UndefOr[A] = js.undefined

    def just    : Maybe[A] = Maybe.just(a)
    def maybeNot: Maybe[A] = Maybe.empty

//    def matchesBy[B <: A : ClassTag](f: B => Boolean) = v match {
//      case b: B => f(b)
//      case _ => false
//    }
  }

  def none[A]: Option[A] = None

  final type TopNode = org.scalajs.dom.Element

  def assertOuterHTML(node: TopNode, expect: String): Unit =
    assertOuterHTML(null, node, expect)

  def assertOuterHTML(name: => String, node: TopNode, expect: String): Unit =
    assertEq(name, scrubReactHtml(node.outerHTML), expect)

  private val reactRubbish = """\s+data-react\S*?\s*?=\s*?".*?"|<!--(?:.|[\r\n])*?-->""".r

  def scrubReactHtml(html: String): String =
    reactRubbish.replaceAllIn(html, "")

  def newBodyContainer(): html.Element = {
    val cont = document.createElement("div")
    document.body.appendChild(cont)
    cont.asInstanceOf[html.Element]
  }

  def withBodyContainer[A](f: html.Element => A): A = {
    val cont = newBodyContainer()
    try
      f(cont)
    finally {
      ReactDOM unmountComponentAtNode cont // Doesn't matter if no component mounted here
      document.body.removeChild(cont)
    }
  }

  def assertRender(comp: ReactElement, expected: String): Unit = {
    val rendered: String = ReactDOMServer.renderToStaticMarkup(comp)
    assertEq(rendered, expected)
  }

  def assertContains(value: String, search: String, expect: Boolean = true): Unit =
    if (value.contains(search) != expect) {
      println(s"\nValue: $value\nSearch: $search\nExpect: $expect\n")
      assert(false)
    }

  def assertTypeMismatch(e: CompileError): Unit =
    assertContains(e.msg, "type mismatch")
}
