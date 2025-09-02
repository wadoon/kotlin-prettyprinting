@file:Suppress("unused")

package io.github.wadoon.pp

import io.github.wadoon.pp.PPUtil.break1
import io.github.wadoon.pp.PPUtil.breakableSpace
import io.github.wadoon.pp.PPUtil.lines
import io.github.wadoon.pp.PPUtil.multilineTextblock
import io.github.wadoon.pp.PPUtil.ok
import io.github.wadoon.pp.PPUtil.plus
import io.github.wadoon.pp.PPUtil.softbreak
import io.github.wadoon.pp.PPUtil.split
import io.github.wadoon.pp.PPUtil.words
import java.util.*


object PPUtil {
    /**
     * The empty document
     * */
    @JvmStatic
    val empty = Document.Empty

    /**
     *  Creates a character document. Invalid for newlines.
     */
    @JvmStatic
    fun char(c: Char) = Document.Char(c).also { require(c != '\n') }

    /**
     *
     */
    @JvmStatic
    val space = Document.Blank(1)

    /**
     *
     */
    @JvmStatic
    fun string(s: String) = Document.String(s)

    /**
     *
     */
    @JvmStatic
    fun fancysubstring(s: String, ofs: Int, len: Int, apparentLength: Int) =
        if (len == 0) empty
        else Document.FancyString(s, ofs, len, apparentLength)

    @JvmStatic
    fun substring(s: String, ofs: Int, len: Int) = fancysubstring(s, ofs, len, len)

    @JvmStatic
    fun fancystring(s: String, apparentLength: Int) = fancysubstring(s, 0, s.length, apparentLength)

    @JvmStatic
    val hardline = Document.HardLine

    @JvmStatic
    fun blank(n: Int) = if (n == 0) empty else Document.Blank(n)

    /** Avoid nesting [Document.IfFlat] in the left-hand side of [Document.IfFlat], as this
     * is redundant.*/
    @JvmStatic
    fun ifflat(doc1: Document, doc2: Document) =
        when (doc1) {
            is Document.IfFlat -> doc1.doc1
            else -> Document.IfFlat(doc1, doc2)
        }

    @JvmStatic
    fun internalBreak(i: Int) = Document.IfFlat(blank(i), hardline)

    /**
     * Optional hardline.
     * @see softbreak
     */
    @JvmStatic
    val break0 = internalBreak(0)

    /**
     *
     */
    @JvmStatic
    val softbreak = break0


    /**
     * Space or hardline.
     * @see breakableSpace
     */
    @JvmStatic
    val break1 = internalBreak(1)

    @JvmStatic
    val breakableSpace = break1


    @JvmStatic
    fun breakOrSpaces(spaces: Int) = when (spaces) {
        0 -> break0
        1 -> break1
        else -> internalBreak(spaces)
    }

    /**
     * Concatenate two Documents
     * @see Document.plus
     */
    @JvmStatic
    fun cat(x: Document, y: Document) =
        if (x is Document.Empty) y
        else
            if (y is Document.Empty) x
            else Document.Cat(x.requirement() + y.requirement(), x, y)

    @JvmStatic
    fun nest(i: Int, x: Document) =
        // assert (i >= 0);
        Document.Nest(x.requirement(), i, x)

    @JvmStatic
    fun group(x: Document): Document {
        val req = x.requirement()
        /* Minor optimisation: an infinite Requirement dissolves a group. */
        return if (req.isInfinity) x else Document.Group(req, x)
    }

    @JvmStatic
    fun align(x: Document) = Document.Align(x.requirement(), x)

    @JvmStatic
    fun range(hook: (PointRange) -> Unit, x: Document) = Document.Range(x.requirement(), hook, x)

    /** This function expresses the following invariant: if we are in flattening
     * mode, then we must be within bounds, i.e. the width and ribbon width
     * constraints must be respected. */
    @JvmStatic
    fun ok(state: State, flatten: Boolean) =
        !flatten || state.column <= state.width && state.column <= state.lastIndent + state.ribbon

    @JvmStatic
    val lparen = char('(')

    @JvmStatic
    val rparen = char(')')

    @JvmStatic
    val langle = char('<')

    @JvmStatic
    val rangle = char('>')

    @JvmStatic
    val lbrace = char('{')

    @JvmStatic
    val rbrace = char('}')

    @JvmStatic
    val lbracket = char('[')

    @JvmStatic
    val rbracket = char(']')

    @JvmStatic
    val squote = char('\'')

    @JvmStatic
    val dquote = char('"')

    @JvmStatic
    val bquote = char('`')

    @JvmStatic
    val semi = char(';')

    @JvmStatic
    val colon = char(':')

    @JvmStatic
    val comma = char(',')

    @JvmStatic
    val dot = char('.')

    @JvmStatic
    val sharp = char('#')

    @JvmStatic
    val slash = char('/')

    @JvmStatic
    val backslash = char('\\')

    @JvmStatic
    val equals = char('=')

    @JvmStatic
    val qmark = char('?')

    @JvmStatic
    val tilde = char('~')

    @JvmStatic
    val at = char('@')

    @JvmStatic
    val percent = char('%')

    @JvmStatic
    val dollar = char('$')

    @JvmStatic
    val caret = char('^')

    @JvmStatic
    val ampersand = char('&')

    @JvmStatic
    val star = char('*')

    @JvmStatic
    val plus = char('+')

    @JvmStatic
    val minus = char('-')

    @JvmStatic
    val underscore = char('_')

    @JvmStatic
    val bang = char('!')

    @JvmStatic
    val bar = char('|')

    @JvmStatic
    fun twice(doc: Document) = cat(doc, doc)

    @JvmStatic
    fun Document.repeat(n: Int) =
        when (n) {
            0 -> empty
            1 -> this
            else -> (1..n).map { this }.fold(empty, ::cat)
        }

    @JvmStatic
    fun precede(l: Document, x: Document) = cat(l, x)

    @JvmStatic
    fun precede(l: String, x: Document) = cat(string(l), x)

    @JvmStatic
    fun terminate(r: Document, x: Document) = cat(x, r)

    @JvmStatic
    fun enclose(l: Document, x: Document, r: Document) = cat(cat(l, x), r)

    @JvmStatic
    fun squotes(x: Document) = enclose(squote, x, squote)

    @JvmStatic
    fun dquotes(x: Document) = enclose(dquote, x, dquote)

    @JvmStatic
    fun bquotes(x: Document) = enclose(bquote, x, bquote)

    @JvmStatic
    fun braces(x: Document) = enclose(lbrace, x, rbrace)

    @JvmStatic
    fun parens(x: Document) = enclose(lparen, x, rparen)

    @JvmStatic
    fun angles(x: Document) = enclose(langle, x, rangle)

    @JvmStatic
    fun brackets(x: Document) = enclose(lbracket, x, rbracket)

    /** A variant of [fold] that keeps track of the element index. */
    fun <A, B> List<A>.foldli(accu: B, f: (Int, B, A) -> B): B = foldIndexed(accu, f)

    /* Working with lists of documents. */

    /** We take advantage of the fact that [^^] operates in constant
     * time, regardless of the size of its arguments. The document
     * that is constructed is essentially a reversed list (i.e., a
     * tree that is biased towards the left). This is not a problem;
     * when pretty-printing this document, the engine will descend
     * along the left branch, pushing the nodes onto its stack as
     * it goes down, effectively reversing the list again.
     * */
    @JvmStatic
    fun concat(docs: List<Document>) = docs.fold(empty, ::cat)

    /**
     *
     */
    @JvmStatic
    fun <T : Document> List<T>.joinToDocument(sep: Document): Document =
        foldli(empty) { i, accu: Document, doc: Document ->
            if (i == 0) doc else cat(cat(accu, sep), doc)
        }

    /**
     *
     */
    @JvmStatic
    fun <T> List<T>.concatMap(f: (T) -> Document) = map(f).reduce(::cat)

    @JvmStatic
    fun <T> List<T>.joinToDocument(sep: Document, f: (T) -> Document) = separateMap(sep, f)

    fun <T> List<T>.separateMap(sep: Document, f: (T) -> Document) =
        foldli(empty) { i, accu: Document, x: T ->
            if (i == 0) f(x) else cat(cat(accu, sep), f(x))
        }

    fun <T : Document> List<T>.separate2(sep: Document, lastSep: Document) =
        foldli(empty) { i, accu: Document, doc: Document ->
            if (i == 0) doc
            else cat(accu, cat(if (i < this.size - 1) sep else lastSep, doc))
        }

    @JvmStatic
    fun <T> optional(x: Optional<T>, f: (T) -> Document) = x.map(f).orElse(empty)

///** This variant of [String.index_from] returns an option. */
//fun index_from(s: String, i: Int, c: Char) = s.indexOf(c, i)

    /** [lines] chops the string [s] into a list of lines, which are turned into documents. */
    @JvmStatic
    fun lines(s: String) = s.split("\n").map { string(it) }

    /** [multilineTextblock] represents the given [s] text block using [break1] for `\n` */
    @JvmStatic
    fun multilineTextblock(s: String) = lines(s).joinToDocument(break1)

    /** [split] splits the string [s] at every occurrence of a character
     * that satisfies the predicate [ok]. The substrings thus obtained are
     * turned into documents, and a list of documents is returned. No information
     * is lost: the concatenation of the documents yields the original string.
     */
    @JvmStatic
    fun split(s: String, chars: (Char) -> Boolean): List<Document> {
        val d = arrayListOf<Document>()
        var lastIndex = 0
        s.toCharArray().forEachIndexed { idx, c ->
            if (chars(c)) {
                d.add(substring(s, lastIndex, idx))
                lastIndex = idx
            }
        }
        if (lastIndex != s.length - 1) {
            d.add(substring(s, lastIndex, s.length))
        }
        return d
    }

    /** [words] chops the string [s] into a list of words, which are turned
    into documents using whitespaces as a delimiter*/
    @JvmStatic
    fun words(s: String) = s.split("\\s").map { it.trim() }.map { ::string }

    @JvmStatic
    fun <T> List<T>.joinToDocument(sep: Document, docs: List<T>, f: (T) -> Document) = flowMap(sep, f)

    @JvmStatic
    fun <T> List<T>.flowMap(sep: Document, f: (T) -> Document) =
        foldli(empty) { i: Int, accu: Document, doc: T ->
            if (i == 0) f(doc)
            else cat(
                accu,
                //This idiom allows beginning a new line if [doc] does not fit on the current line.
                group(cat(sep, f(doc)))
            )
        }

    @JvmStatic
    fun List<Document>.flow(sep: Document) = flowMap(sep, { it })

    @JvmStatic
    fun url(s: String) = split(s) { it == '/' || it == '.' }.flow(breakOrSpaces(0))

    /* -------------------------------------------------------------------------- */
    /* Alignment and indentation. */

    @JvmStatic
    fun hang(i: Int, d: Document) = align(nest(i, d))

    @JvmStatic
    infix fun Document.slash(y: Document) = cat(cat(this, break1), y)

    @JvmStatic
    infix fun Document.`^^`(y: Document) = cat(this, y)

    /** Concatenation for two documents */
    operator fun Document.plus(y: Document) = cat(this, y)

    @JvmStatic
    fun prefix(n: Int, b: Int, x: Document, y: Document) = group(x `^^` nest(n, (breakOrSpaces(b) `^^` y)))

    @JvmStatic
    infix fun Document.prefixed(y: Document) = prefix(2, 1, this, y) // ^//^


    @JvmStatic
    fun jump(n: Int, b: Int, y: Document) = group(nest(n, breakOrSpaces(b) `^^` y))

    @JvmStatic
    fun `infix`(n: Int, b: Int, op: Document, x: Document, y: Document) = prefix(n, b, x `^^` blank(b) `^^` op, y)

    @JvmStatic
    fun surround(n: Int, b: Int, opening: Document, contents: Document, closing: Document) =
        group(opening `^^` nest(n, (breakOrSpaces(b) `^^` contents) `^^` breakOrSpaces(b) `^^` closing))

    @JvmStatic
    fun softSurround(n: Int, b: Int, opening: Document, contents: Document, closing: Document) =
        group(
            opening `^^` nest(n, group(breakOrSpaces(b) `^^` contents) `^^` group((breakOrSpaces(b) `^^` closing)))
        )

    val commaSpace = comma + break1

    @JvmStatic
    fun List<Document>.surroundSeparate(
        n: Int,
        b: Int,
        emptiness: Document = empty,
        opening: Document = empty,
        sep: Document = commaSpace,
        closing: Document = empty,
    ) =
        if (isEmpty()) emptiness
        else surround(n, b, opening, joinToDocument(sep), closing)

    @JvmStatic
    fun <T> List<T>.surroundSeparateMap(
        n: Int,
        b: Int,
        emptiness: Document = empty,
        opening: Document = empty,
        sep: Document = commaSpace,
        closing: Document = empty,
        f: (T) -> Document
    ) = if (isEmpty()) emptiness else surround(n, b, opening, separateMap(sep, f), closing)
}