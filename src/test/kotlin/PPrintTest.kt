/* This file is part of kotlin-prettyprinting.
 * kotlin-prettyprinting is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only
 */
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import io.github.wadoon.pp.*
import io.github.wadoon.pp.Engine.pretty
import io.github.wadoon.pp.PPUtils.align
import io.github.wadoon.pp.PPUtils.ampersand
import io.github.wadoon.pp.PPUtils.at
import io.github.wadoon.pp.PPUtils.backslash
import io.github.wadoon.pp.PPUtils.bang
import io.github.wadoon.pp.PPUtils.bar
import io.github.wadoon.pp.PPUtils.blank
import io.github.wadoon.pp.PPUtils.braces
import io.github.wadoon.pp.PPUtils.break0
import io.github.wadoon.pp.PPUtils.break1
import io.github.wadoon.pp.PPUtils.caret
import io.github.wadoon.pp.PPUtils.comma
import io.github.wadoon.pp.PPUtils.dollar
import io.github.wadoon.pp.PPUtils.dot
import io.github.wadoon.pp.PPUtils.empty
import io.github.wadoon.pp.PPUtils.equals
import io.github.wadoon.pp.PPUtils.group
import io.github.wadoon.pp.PPUtils.grouped
import io.github.wadoon.pp.PPUtils.hang
import io.github.wadoon.pp.PPUtils.hardline
import io.github.wadoon.pp.PPUtils.jump
import io.github.wadoon.pp.PPUtils.langle
import io.github.wadoon.pp.PPUtils.lbrace
import io.github.wadoon.pp.PPUtils.lparen
import io.github.wadoon.pp.PPUtils.minus
import io.github.wadoon.pp.PPUtils.multilineTextblock
import io.github.wadoon.pp.PPUtils.nest
import io.github.wadoon.pp.PPUtils.plus
import io.github.wadoon.pp.PPUtils.qmark
import io.github.wadoon.pp.PPUtils.range
import io.github.wadoon.pp.PPUtils.rangle
import io.github.wadoon.pp.PPUtils.rejoin
import io.github.wadoon.pp.PPUtils.repeat
import io.github.wadoon.pp.PPUtils.rparen
import io.github.wadoon.pp.PPUtils.sharp
import io.github.wadoon.pp.PPUtils.star
import io.github.wadoon.pp.PPUtils.string
import io.github.wadoon.pp.PPUtils.surroundSeparateMap
import io.github.wadoon.pp.PPUtils.tilde
import io.github.wadoon.pp.PPUtils.underscore
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
                break1 + string("stmt;") +
                    break1 + string("stmt;") +
                    break1 + string("stmt;"),
            ) + break1 + string("end")

        println(pretty(d, 40))
    }

    @Test
    fun test_braces() {
        val doc = braces(
            string("abc"),
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
            """.trimIndent(),
        )
        println(pretty(d, 20))
    }

    @Test
    fun testBlanks() {
        assertPretty(blank(0)).isEqualTo("")
        assertPretty(blank(1)).isEqualTo(" ")
        assertPretty(blank(2)).isEqualTo("  ")
        assertPretty(blank(3)).isEqualTo("   ")
        assertPretty(blank(10), 5).isEqualTo("""          """)
    }

    @Test
    fun testHardline() {
        assertPretty(hardline).isEqualTo("\n")
    }

    @Test
    fun testAlign() {
        val d = rejoin("int i = function") +
            align(
                listOf(1, 2, 3).surroundSeparateMap(opening = lparen, closing = rparen, indent = 1) {
                    string("argument_$it")
                },
            ) + PPUtils.semi
        assertPretty(d, 15)
            .isEqualTo(
                """
                int i = function(
                                 argument_1,
                                 argument_2,
                                 argument_3
                                 );
                """.trimIndent(),
            )
    }

    @Test
    fun `test range`() {
        assertPretty(rejoin("a b c d").range { println(it) })
            .isEqualTo("a b c d")
    }

    @Test
    fun `test hang`() {
        assertPretty(hang(20, string("abc")))
            .isEqualTo("")
    }

    @Test
    fun repeat() {
        val d = string("xxx").repeat(25)
        assertThat(
            pretty(
                d,
                20,
            ),
        ).isEqualTo("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx")

        assertPretty(sharp.repeat(0)).isEqualTo(empty)
        assertPretty(sharp.repeat(1)).isEqualTo(sharp)

        assertPretty(sharp.repeat(1)).isEqualTo("#")
        val e = group((string("xxx") + break0).repeat(5))
        assertThat(pretty(e, 20)).isEqualTo("xxxxxxxxxxxxxxx")
    }

    @Test
    fun testBreaking() {
        val fiveAs = string("aaaaa")
        val twoTimesFive = group(fiveAs + break0 + fiveAs)
        assertPretty(twoTimesFive, 10)
            .isEqualTo("aaaaaaaaaa")

        assertPretty(twoTimesFive, 9)
            .isEqualTo("aaaaa\naaaaa")

        val twoTimesFiveSoft = group(fiveAs + break1 + fiveAs)
        assertPretty(twoTimesFiveSoft, 12)
            .isEqualTo("aaaaa aaaaa")

        assertPretty(twoTimesFiveSoft, 10)
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
        assertThat(pretty(equals)).isEqualTo("=")
        assertThat(pretty(at)).isEqualTo("@")
        assertThat(pretty(tilde)).isEqualTo("~")
        assertThat(pretty(qmark)).isEqualTo("?")
        assertThat(pretty(bang)).isEqualTo("!")
        assertThat(pretty(backslash)).isEqualTo("\\")
        assertThat(pretty(dollar)).isEqualTo("$")
        assertThat(pretty(caret)).isEqualTo("^")
        assertThat(pretty(ampersand)).isEqualTo("&")
        assertThat(pretty(star)).isEqualTo("*")
        assertThat(pretty(plus)).isEqualTo("+")
        assertThat(pretty(minus)).isEqualTo("-")
        assertThat(pretty(underscore)).isEqualTo("_")
        assertThat(pretty(bar)).isEqualTo("|")
    }

    @Test
    fun funcCallLike() {
        val d = rejoin("int i = function") + listOf(1, 2, 3).surroundSeparateMap(opening = lparen, closing = rparen, indent = 2) {
            string("argument_$it")
        } + PPUtils.semi
        println(pretty(d))

        val fc = rejoin("func") + listOf(1, 2, 3).surroundSeparateMap(opening = lparen, closing = rparen, indent = 2) {
            string("a_$it")
        }

        val e = rejoin("bool i = next") + listOf(fc, fc, fc).surroundSeparateMap(opening = lparen, closing = rparen, indent = 2) {
            it.grouped()
        } + PPUtils.semi
        println(pretty(e))
    }

    @Test
    fun testJump() {
        val d = rejoin("int i =") + jump(rejoin("2 ;"), 20, 30)
        assertPretty(d, width = 40)
            .isEqualTo("int i =                              2 ;")
        assertPretty(d, width = 20)
            .isEqualTo("int i =\n                    2 ;")
    }

    private fun assertPretty(d: Document, width: Int = 40) = assertThat(pretty(d, width = width))
}
