/* This file is part of kotlin-prettyprinting.
 * kotlin-prettyprinting is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only
 */
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import io.github.wadoon.pp.*
import io.github.wadoon.pp.Engine.pretty
import io.github.wadoon.pp.Engine.prettyQ
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
        val d = tokenize("int i = function") +
            align(
                listOf(1, 2, 3).surroundSeparateMap(opening = lparen, closing = rparen, indent = 1) {
                    string("argument_$it")
                },
            ) + semi
        assertPretty(
            d,
            """
                int i = function(
                                 argument_1,
                                 argument_2,
                                 argument_3
                                 );
            """.trimIndent(),
            width = 15,
        )
    }

    @Test
    fun testRange() {
        assertPretty(
            rejoin("a b c d").range { println(it) },
            "a b c d",
        )
    }

    @Test
    fun testHang() {
        val h = hang(4, string("hanged"))
        val line = nest(
            4,
            (words("AAA BBB CCC DDD") + h).joinToDocument(hardline),
        )

        assertPretty(
            words("AAA BBB CCC DDD").joinToDocument(hardline),
            """
            AAA
            BBB
            CCC
            DDD
            """.trimIndent(),
        )

        assertPretty(
            line,
            """
            AAA
                BBB
                CCC
                DDD
                hanged
            """.trimIndent(),
        )
    }

    @Test
    fun `join document`() {
        assertPretty(hardline).isEqualTo("\n")

        val byhand = string("AAA") + hardline + string("BBB")
        assertPretty(
            byhand,
            """
                AAA
                BBB
            """.trimIndent(),
        )

        assertThat(words("AAA BBB").joinToDocument(hardline))
            .isEqualTo(byhand)
    }

    @Test
    fun repeat() {
        assertThat(sharp.repeat(0)).isEqualTo(empty)
        assertThat(sharp.repeat(1)).isEqualTo(sharp)
        assertPretty(sharp.repeat(1), "#")

        val d = string("x").repeat(11)
        assertPretty(d, "xxxxxxxxxxx", 11)

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
        } + semi
        println(pretty(d))

        val fc = rejoin("func") + listOf(1, 2, 3).surroundSeparateMap(opening = lparen, closing = rparen, indent = 2) {
            string("a_$it")
        }

        val e = rejoin("bool i = next") + listOf(fc, fc, fc).surroundSeparateMap(opening = lparen, closing = rparen, indent = 2) {
            it.grouped()
        } + semi
        println(pretty(e))
    }

    @Test
    fun testJump() {
        val d = tokenize("int i =") + jump(tokenize("2;"), 20, 30)
        assertPretty(d, "int i =                              2;", width = 40)
        assertPretty(d, "int i =\n                    2;", width = 20)

        assertThat(jump(d)).isEqualTo(d.jumped())
    }

    private fun assertPretty(d: Document, expected: String, width: Int = 40) {
        assertThat(pretty(d, width = width)).isEqualTo(expected)
        assertThat(prettyQ(d, width = width)).isEqualTo(expected)
    }

    private fun assertPretty(d: Document, width: Int = 40) = assertThat(pretty(d, width = width))

    @Test
    fun testConcatMap() {
        assertPretty(concat(words("a b c d")), "abcd")
    }
}
