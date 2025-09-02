import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import io.github.wadoon.pp.Engine.pretty
import io.github.wadoon.pp.PPUtil
import io.github.wadoon.pp.PPUtil.braces
import io.github.wadoon.pp.PPUtil.break0
import io.github.wadoon.pp.PPUtil.break1
import io.github.wadoon.pp.PPUtil.comma
import io.github.wadoon.pp.PPUtil.dot
import io.github.wadoon.pp.PPUtil.empty
import io.github.wadoon.pp.PPUtil.group
import io.github.wadoon.pp.PPUtil.langle
import io.github.wadoon.pp.PPUtil.lbrace
import io.github.wadoon.pp.PPUtil.lparen
import io.github.wadoon.pp.PPUtil.multilineTextblock
import io.github.wadoon.pp.PPUtil.nest
import io.github.wadoon.pp.PPUtil.plus
import io.github.wadoon.pp.PPUtil.rangle
import io.github.wadoon.pp.PPUtil.repeat
import io.github.wadoon.pp.PPUtil.rparen
import io.github.wadoon.pp.PPUtil.sharp
import io.github.wadoon.pp.PPUtil.string
import io.github.wadoon.pp.Requirement
import io.github.wadoon.pp.infinity
import kotlin.test.Test

/**
 *
 * @author Alexander Weigl
 * @version 1 (1/26/22)
 */
class PPrintTest {
    @Test
    fun testInfinity() {
        assertThat(infinity.isInfinity).isTrue()
        assertThat(Requirement(5).isInfinity).isFalse()

        assertThat((infinity + Requirement(5)).isInfinity).isTrue()
        assertThat((Requirement(5) + infinity).isInfinity).isTrue()
    }

    @Test
    fun testCat() {
        assertThat(pretty(string("a") + empty)).isEqualTo("a")
        assertThat(pretty(empty + string("a"))).isEqualTo("a")
        assertThat(pretty(empty + string("a") + empty)).isEqualTo("a")
        assertThat(pretty(empty + empty)).isEqualTo("")
    }

    @Test
    fun first() {
        val d =
            string("begin") + nest(
                4,
                break1 + string("stmt;")
                        + break1 + string("stmt;")
                        + break1 + string("stmt;")
            ) + break1 + string("end")

        println(pretty(d, 40))
    }

    @Test
    fun test_braces() {
        val doc = braces(
            string("abc")
        )
        print(doc)
        val actual = pretty(doc, 40)
        val expected = "{abc}"
        Truth.assertThat(actual).isEqualTo(expected)
    }


    @Test
    fun textBlock() {
        val d = multilineTextblock(
            """
            fdsafdsaf f dsaf dsaf dsaf dsa fsa fsadf
            f dsafdsaf dsaf dsaf dsaf dsaf dsa fd dddd
        """.trimIndent()
        )
        println(pretty(d, 20))
    }

    @Test
    fun repeat() {
        val d = string("xxx").repeat(25)
        assertThat(
            pretty(
                d,
                20
            )
        ).isEqualTo("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx")

        assertThat(sharp.repeat(0)).isEqualTo(empty)
        assertThat(sharp.repeat(1)).isEqualTo(sharp)

        assertThat(pretty(sharp.repeat(1)))
            .isEqualTo("#")

        val e = group((string("xxx") + break0).repeat(5))
        assertThat(pretty(e, 20)).isEqualTo("xxxxxxxxxxxxxxx")
    }

    @Test
    fun testBreaking() {
        val fiveAs = string("aaaaa")
        val twoTimesFive = group(fiveAs + break0 + fiveAs)
        assertThat(pretty(twoTimesFive, 10, rfrac = 1.0))
            .isEqualTo("aaaaaaaaaa")

        assertThat(pretty(twoTimesFive, 9))
            .isEqualTo("aaaaa\naaaaa")

        val twoTimesFiveSoft = group(fiveAs + break1 + fiveAs)
        assertThat(pretty(twoTimesFiveSoft, 12))
            .isEqualTo("aaaaa aaaaa")

        assertThat(pretty(twoTimesFiveSoft, 10))
            .isEqualTo("aaaaa\naaaaa")

    }

    @Test
    fun characters() {
        assertThat(pretty(comma)).isEqualTo(",")
        assertThat(pretty(lparen)).isEqualTo("(")
        assertThat(pretty(rparen)).isEqualTo(")")
        assertThat(pretty(lbrace)).isEqualTo("{")
        assertThat(pretty(rangle)).isEqualTo(">")
        assertThat(pretty(langle)).isEqualTo("<")
        assertThat(pretty(dot)).isEqualTo(".")
        assertThat(pretty(sharp)).isEqualTo("#")
        assertThat(pretty(PPUtil.equals)).isEqualTo("=")

        assertThat(pretty(PPUtil.at)).isEqualTo("@")
        assertThat(pretty(PPUtil.tilde)).isEqualTo("~")
        assertThat(pretty(PPUtil.qmark)).isEqualTo("?")
        assertThat(pretty(PPUtil.bang)).isEqualTo("!")
        assertThat(pretty(PPUtil.backslash)).isEqualTo("\\")
        assertThat(pretty(PPUtil.dollar)).isEqualTo("$")
        assertThat(pretty(PPUtil.caret)).isEqualTo("^")
        assertThat(pretty(PPUtil.ampersand)).isEqualTo("&")
        assertThat(pretty(PPUtil.star)).isEqualTo("*")
        assertThat(pretty(PPUtil.plus)).isEqualTo("+")
        assertThat(pretty(PPUtil.minus)).isEqualTo("-")
        assertThat(pretty(PPUtil.underscore)).isEqualTo("_")
        assertThat(pretty(PPUtil.bar)).isEqualTo("|")
    }

}