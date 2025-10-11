package io.github.wadoon.pp;

import com.google.common.truth.Truth;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static io.github.wadoon.pp.Engine.pretty;
import static io.github.wadoon.pp.JavaHelper.isInfinity;
import static io.github.wadoon.pp.JavaHelper.plusReq;
import static io.github.wadoon.pp.PPUtils.*;
import static io.github.wadoon.pp.PPrintTestKt.assertPretty;

/**
 *
 * @author Alexander Weigl
 * @version 1 (9/3/25)
 */
public class JavaTest {
    @Test
    void testInfinity() {
        var infinity = ModelKt.getInfinity();
        assertThat(isInfinity(plusReq(infinity, 5))).isTrue();
        assertThat(isInfinity(plusReq(5, infinity))).isTrue();
    }

    @Test
    void testCat() {
        assertPretty(cat(string("a"), empty), "a");
        assertPretty(cat(empty, string("a")));
        assertPretty(cat(cat(empty, string("a")), empty), "a");
        assertPretty(cat(empty, empty), "");
    }

    @Test
    void first() {
        var d = concat(string("begin"),
                nest(4, concat(break1, string("stmt;"),
                        break1, string("stmt;"),
                        break1, string("stmt;"))
                ), break1, string("end"));
        System.out.println(pretty(d, 40));
    }

    @Test
    void test_braces() {
        var doc = braces(string("abc"));
        var actual = pretty(doc, 40);
        var expected = "{abc}";
        Truth.assertThat(actual).isEqualTo(expected);
    }

    @Test
    void textBlock() {
        var d = multilineTextblock(
                """
                        fdsafdsaf f dsaf dsaf dsaf dsa fsa fsadf
                        f dsafdsaf dsaf dsaf dsaf dsaf dsa fd dddd
                        """
        );
        System.out.println(pretty(d, 20));
    }

    @Test
    void testBlanks() {
        assertPretty(blank(0)).isEqualTo("");
        assertPretty(blank(1)).isEqualTo(" ");
        assertPretty(blank(2)).isEqualTo("  ");
        assertPretty(blank(3)).isEqualTo("   ");
        assertPretty(blank(10), 5).isEqualTo("          ");
    }

    @Test
    void testHardline() {
        assertPretty(hardline).isEqualTo("\n");
    }

    @Test
    void testAlign() {
        var d = concat(tokenize("int i = function"),
                reducer(List.of(1, 2, 3)).opening(lparen).closing(rparen).indent(2)
                        .map((it) -> string("argument_%s".formatted(it))).build(), semi);

        assertPretty(d,
                """
                        int i = function(
                          argument_1,
                          argument_2,
                          argument_3
                          );""",
                15);
    }

    @Test
    void testRange() {
        assertPretty(
                range(rejoin("a b c d"), it -> {
                    System.out.println(it);
                    return null;
                }),
                "a b c d");
    }

    @Test
    void testHang() {
        var h = hang(4, string("hanged"));
        var line = nest(4, concat(joinToDocument(words("AAA BBB CCC DDD"), hardline), h));

        assertPretty(
                joinToDocument(words("AAA BBB CCC DDD"), hardline),
                "AAA\nBBB\nCCC\nDDD");

        assertPretty(line, "AAA\n    BBB\n    CCC\n    DDDhanged");
    }

    @Test
    void joinDocument() {
        assertPretty(hardline).isEqualTo("\n");

        var byhand = concat(string("AAA"), hardline, string("BBB"));
        assertPretty(byhand, "AAA\nBBB");

        assertThat(joinToDocument(words("AAA BBB"), hardline)).isEqualTo(byhand);
    }

    @Test
    void testRepeat() {
        assertThat(repeat(sharp, 0)).isEqualTo(empty);
        assertThat(repeat(sharp, 1)).isEqualTo(sharp);
        assertPretty(repeat(sharp, 1), "#");

        var d = repeat(string("x"), 11);
        assertPretty(d, "xxxxxxxxxxx", 11);

        var e = group(repeat(concat(string("xxx"), break0), 5));
        assertThat(pretty(e, 20)).isEqualTo("xxxxxxxxxxxxxxx");
    }

    @Test
    void testBreaking() {
        var fiveAs = string("aaaaa");
        var twoTimesFive = group(concat(fiveAs, break0, fiveAs));
        assertPretty(twoTimesFive, 10)
                .isEqualTo("aaaaaaaaaa");

        assertPretty(twoTimesFive, 9)
                .isEqualTo("aaaaa\naaaaa");

        var twoTimesFiveSoft = group(concat(fiveAs, break1, fiveAs));
        assertPretty(twoTimesFiveSoft, 12)
                .isEqualTo("aaaaa aaaaa");

        assertPretty(twoTimesFiveSoft, 10)
                .isEqualTo("aaaaa\naaaaa");
    }

    @Test
    void characters() {
        assertThat(pretty(comma)).isEqualTo(",");
        assertThat(pretty(lparen)).isEqualTo("(");
        assertThat(pretty(rparen)).isEqualTo(")");
        assertThat(pretty(lbrace)).isEqualTo("{");
        assertThat(pretty(rangle)).isEqualTo(">");
        assertThat(pretty(langle)).isEqualTo("<");
        assertThat(pretty(dot)).isEqualTo(".");
        assertThat(pretty(sharp)).isEqualTo("#");
        assertThat(pretty(equals)).isEqualTo("=");
        assertThat(pretty(at)).isEqualTo("@");
        assertThat(pretty(tilde)).isEqualTo("~");
        assertThat(pretty(qmark)).isEqualTo("?");
        assertThat(pretty(bang)).isEqualTo("!");
        assertThat(pretty(backslash)).isEqualTo("\\");
        assertThat(pretty(dollar)).isEqualTo("$");
        assertThat(pretty(caret)).isEqualTo("^");
        assertThat(pretty(ampersand)).isEqualTo("&");
        assertThat(pretty(star)).isEqualTo("*");
        assertThat(pretty(plus)).isEqualTo("+");
        assertThat(pretty(minus)).isEqualTo("-");
        assertThat(pretty(underscore)).isEqualTo("_");
        assertThat(pretty(bar)).isEqualTo("|");
    }

    @Test
    void funcCallLike() {
        var hlp = new PPHelper();

        var e = surroundSeparateMap(List.of(1, 2, 3), lparen, rparen, (it) -> string("argument_%s".formatted(it)));
        var d = concat(rejoin("int i = function"), e, semi);

        System.out.println(pretty(d));

        var fc = concat(rejoin("func"),
                hlp.withParens(List.of(1, 2, 3)).map((it) -> string("a_%s".formatted(it)))
                        .build());

        var q = concat(rejoin("bool i = next"),
                PPUtils.reducer(List.of(fc, fc, fc))
                        .map(PPUtils::grouped)
                        .opening(lparen).closing(rparen)
                        .indent(2)
                        .build(),
                semi);

        System.out.println(pretty(q));
    }

    @Test
    void testJump() {
        var d = concat(
                tokenize("int i ="), jump(tokenize("2;"), 20, 30));

        assertPretty(d, "int i =                              2;", 40);
        assertPretty(d, "int i =\n                    2;", 20);

        assertThat(jump(d)).isEqualTo(jumped(d));
    }

    @Test
    void testConcatMap() {
        assertPretty(concat(words("a b c d")), "abcd");
    }
}
