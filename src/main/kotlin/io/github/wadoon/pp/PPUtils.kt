/* This file is part of kotlin-prettyprinting.
 * kotlin-prettyprinting is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only
 */
@file:Suppress("unused")

package io.github.wadoon.pp

import io.github.wadoon.pp.PPUtils.break0
import io.github.wadoon.pp.PPUtils.break1
import io.github.wadoon.pp.PPUtils.breakableSpace
import io.github.wadoon.pp.PPUtils.concat
import io.github.wadoon.pp.PPUtils.fancysubstring
import io.github.wadoon.pp.PPUtils.hardline
import io.github.wadoon.pp.PPUtils.jump
import io.github.wadoon.pp.PPUtils.lines
import io.github.wadoon.pp.PPUtils.multilineTextblock
import io.github.wadoon.pp.PPUtils.plus
import io.github.wadoon.pp.PPUtils.separateMap
import io.github.wadoon.pp.PPUtils.softbreak
import io.github.wadoon.pp.PPUtils.split
import io.github.wadoon.pp.PPUtils.surround
import io.github.wadoon.pp.PPUtils.words
import java.util.*

object PPUtils {
    /**
     * The empty document
     * */
    @JvmField
    val empty = Document.Empty

    /**
     *  Creates a character document. Invalid for newlines.
     */
    @JvmStatic
    fun char(c: Char) = Document.Char(c).also { require(c != '\n') }

    /**
     *
     */
    @JvmField
    val space = Document.Blank(1)

    /**
     * Creates a simple string as a document. The length requirement is determined by its content.
     */
    @JvmStatic
    fun string(s: String) = Document.String(s)

    /**
     * Creates a string which printed length might be different from the text length.
     *
     * This string can be cut out of a larger string to save memory.
     *
     * @param s the string holding the content
     * @param ofs the offset inside [s]
     * @param len the length that should be truncated from [s]
     * @param apparentLength the printed length used in the requirements
     */
    @JvmStatic
    fun fancysubstring(s: String, ofs: Int, len: Int, apparentLength: Int) = if (len == 0) {
        empty
    } else {
        Document.FancyString(s, ofs, len, apparentLength)
    }

    /**
     * Represents a substring from [s], which offset [ofs] and length [len].
     * @see fancysubstring
     */
    @JvmStatic
    fun substring(s: String, ofs: Int, len: Int) = fancysubstring(s, ofs, len, len)

    /**
     * Represents a text which internal length is different from the printed text.
     * Useful for example for HTML and other markup languages.
     * @param s the internal string
     * @param apparentLength the printed length
     * @see fancysubstring
     */
    @JvmStatic
    fun fancystring(s: String, apparentLength: Int) = fancysubstring(s, 0, s.length, apparentLength)

    /** A hardline unavoidable linebreak */
    @JvmField
    val hardline = Document.HardLine

    /** [n] number of spaces */
    @JvmStatic
    fun blank(n: Int) = if (n == 0) empty else Document.Blank(n)

    /** Avoid nesting [Document.IfFlat] in the left-hand side of [Document.IfFlat], as this
     * is redundant.*/
    @JvmStatic
    fun ifflat(doc1: Document, doc2: Document): Document = when (doc1) {
        is Document.IfFlat -> ifflat(doc1.doc1, doc2)
        else -> Document.IfFlat(doc1, doc2)
    }

    /** Adds [i] spaces if necessary, else hardline break.*/
    @JvmStatic
    private fun internalBreak(i: Int) = Document.IfFlat(blank(i), hardline)

    /**
     * Represents and optional line break ([hardline]).
     * @see softbreak
     */
    @JvmField
    val break0 = internalBreak(0)

    /**
     * Represents and optional line break ([hardline]).
     * @see break0
     */
    @JvmField
    val softbreak = break0

    /**
     * Represents a single whitespace or a [hardline].
     * @see breakableSpace
     */
    @JvmField
    val break1 = internalBreak(1)

    /**
     * Represents a single whitespace or a [hardline].
     * @see break1
     */
    @JvmField
    val breakableSpace = break1

    /** Numbers of spaces if fits on line or a hardline */
    @JvmStatic
    fun breakOrSpaces(spaces: Int) = when (spaces) {
        0 -> break0
        1 -> break1
        else -> internalBreak(spaces)
    }

    /**
     * Concatenates two documents together
     *
     * @see Document.plus
     * @See concat
     */
    @JvmStatic
    fun cat(x: Document, y: Document) = if (x is Document.Empty) {
        y
    } else {
        if (y is Document.Empty) {
            x
        } else {
            Document.Cat(x.requirement() + y.requirement(), x, y)
        }
    }

    /**
     * Increases the indentation level in [x] by [indent] after each [hardline].
     */
    @JvmStatic
    fun nest(indent: Int, x: Document) = Document.Nest(x.requirement(), indent, x)
        .also { require(indent >= 0) }

    /** Create a group around [x].*/
    @JvmStatic
    fun group(x: Document): Document {
        val req = x.requirement()
        /* Minor optimisation: an infinite Requirement dissolves a group. */
        return if (req.isInfinity) x else Document.Group(req, x)
    }

    /** Create a group around [this].*/
    @JvmStatic
    @JvmOverloads
    fun Document.grouped(g: Boolean = true) = if (g) group(this) else this

    /** */
    @JvmStatic
    fun align(x: Document) = Document.Align(x.requirement(), x)

    /** [this] */
    @JvmStatic
    fun Document.aligned() = Document.Align(requirement(), this)

    /** Adds a hook, which is called by the engine on printing. */
    @JvmStatic
    fun Document.range(hook: (PointRange) -> Unit) = Document.Range(requirement(), hook, this)

    /** This function expresses the following invariant: if we are in flattening
     * mode, then we must be within bounds, i.e. the width and ribbon width
     * constraints must be respected. */
    // @JvmStatic @JvmOverloads
    // fun ok(state: State, flatten: Boolean) = !flatten || state.column <= state.width && state.column <= state.lastIndent + state.ribbon

    /** Left paren */
    @JvmField
    val lparen = char('(')

    /** right par */
    @JvmField
    val rparen = char(')')

    @JvmField
    val langle = char('<')

    @JvmField
    val rangle = char('>')

    @JvmField
    val lbrace = char('{')

    @JvmField
    val rbrace = char('}')

    @JvmField
    val lbracket = char('[')

    @JvmField
    val rbracket = char(']')

    @JvmField
    val squote = char('\'')

    @JvmField
    val dquote = char('"')

    @JvmField
    val bquote = char('`')

    @JvmField
    val semi = char(';')

    @JvmField
    val colon = char(':')

    @JvmField
    val comma = char(',')

    @JvmField
    val dot = char('.')

    @JvmField
    val sharp = char('#')

    @JvmField
    val slash = char('/')

    @JvmField
    val backslash = char('\\')

    @JvmField
    val equals = char('=')

    @JvmField
    val qmark = char('?')

    @JvmField
    val tilde = char('~')

    @JvmField
    val at = char('@')

    @JvmField
    val percent = char('%')

    @JvmField
    val dollar = char('$')

    @JvmField
    val caret = char('^')

    @JvmField
    val ampersand = char('&')

    @JvmField
    val star = char('*')

    @JvmField
    val plus = char('+')

    @JvmField
    val minus = char('-')

    @JvmField
    val underscore = char('_')

    @JvmField
    val bang = char('!')

    @JvmField
    val bar = char('|')

    @JvmStatic
    fun twice(doc: Document) = cat(doc, doc)

    @JvmStatic
    fun Document.repeat(n: Int) = when (n) {
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
    fun <T : Document> List<T>.joinToDocument(sep: Document): Document = foldli(empty) { i, accu: Document, doc: Document ->
        if (i == 0) doc else cat(cat(accu, sep), doc)
    }

    /**
     *
     */
    @JvmStatic
    fun <T> List<T>.concatMap(f: (T) -> Document) = map(f).reduce(::cat)

    @JvmStatic
    fun <T> List<T>.joinToDocument(sep: Document, f: (T) -> Document) = separateMap(sep, f)

    fun <T> List<T>.separateMap(sep: Document, f: (T) -> Document) = foldli(empty) { i, accu: Document, x: T ->
        if (i == 0) {
            f(x)
        } else {
            cat(cat(accu, sep), f(x))
        }
    }

    fun <T : Document> List<T>.separate2(sep: Document, lastSep: Document) = foldli(empty) { i, accu: Document, doc: Document ->
        if (i == 0) {
            doc
        } else {
            cat(accu, cat(if (i < this.size - 1) sep else lastSep, doc))
        }
    }

    /** */
    @JvmStatic
    fun <T> optional(x: Optional<T>, f: (T) -> Document): Document = x.map(f).orElse(empty)

    /** */
    @JvmStatic
    fun <T> T?.orEmpty(f: T.() -> Document): Document = this?.f() ?: empty

    /** */
    @JvmStatic
    fun <T> Document?.orEmpty(): Document = this ?: empty

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
    fun words(s: String): List<Document.String> = s.split("\\s+").map { it.trim() }.map(::string)

    @JvmStatic
    fun rejoin(s: String): Document = words(s).joinToDocument(breakableSpace)

    /** */
    @JvmStatic
    fun <T> List<T>.joinToDocument(sep: Document, docs: List<T>, f: (T) -> Document) = flowMap(sep, f)

    /** */
    @JvmStatic
    fun <T> List<T>.flowMap(sep: Document, f: (T) -> Document) = foldli(empty) { i: Int, accu: Document, doc: T ->
        if (i == 0) {
            f(doc)
        } else {
            cat(
                accu,
                // This idiom allows beginning a new line if [doc] does not fit on the current line.
                group(cat(sep, f(doc))),
            )
        }
    }

    /**
     *
     */
    @JvmStatic
    fun List<Document>.flow(sep: Document) = flowMap(sep) { it }

    /**
     *
     */
    @JvmStatic
    fun url(s: String) = split(s) { it == '/' || it == '.' }.flow(breakOrSpaces(0))

    /**
     *
     */
    @JvmStatic
    fun hang(i: Int, d: Document) = align(nest(i, d))

    /** Concatenates two documents with a [softbreak]. */
    @JvmStatic
    operator fun Document.div(y: Document) = this + break1 + y

    /** Concatenation for two documents */
    @JvmStatic
    operator fun Document.plus(y: Document) = cat(this, y)

    /** Concatenation for two documents */
    @JvmStatic
    operator fun String.plus(y: Document) = cat(string(this), y)

    /** Concatenation for two documents */
    @JvmStatic
    operator fun Document.plus(y: String) = cat(this, string(y))

    /** Creates a block of documents, which are either printed on-line or printed over several lines,
     * with indentation [intend].
     *
     * @param intend indentation for each line
     * @param space
     * @param x
     * @param y
     */
    @JvmStatic
    fun prefix(intend: Int, space: Int, x: Document, y: Document) = group(x + nest(intend, (breakOrSpaces(space) + y)))

    /**
     *
     */
    @JvmStatic
    infix fun Document.prefixed(y: Document) = prefix(2, 1, this, y)

    /**
     * Adds the given amount of [space]s in front of [x] if
     * it would fit into the remaining line. Otherwise, the text moves
     * to the next line and is intended by [indent].
     *
     * @param indent indentation for each line
     * @param space number of spaces before [x]
     */
    @JvmStatic
    fun jump(x: Document, indent: Int = 0, space: Int = 0) = group(
        nest(
            indent,
            breakOrSpaces(space) + x,
        ),
    )

    /** @see jump */
    @JvmStatic
    fun Document.jumped(indent: Int = 0, space: Int = 0) = jump(this, indent, space)

    /**
     *
     */
    @JvmStatic
    @JvmOverloads
    fun `infix`(n: Int, b: Int, op: Document, x: Document, y: Document) = prefix(n, b, x + blank(b) + op, y)

    /**
     * An [indent]ed document with [space]s before and after [this].
     */
    @JvmStatic
    @JvmOverloads
    fun Document.surround(opening: Document, closing: Document, indent: Int = 0, space: Int = 0) = group(
        opening +
            nest(
                indent,
                (breakOrSpaces(space) + this) +
                    breakOrSpaces(space) + closing,
            ),
    )

    /**
     *
     */
    @JvmStatic
    @JvmOverloads
    fun softSurround(contents: Document, opening: Document = lparen, closing: Document = rparen, indent: Int = 0, space: Int = 0) = group(
        opening + nest(indent, group(breakOrSpaces(space) + contents) + group((breakOrSpaces(space) + closing))),
    )

    val commaSpace = comma + break1

    /**
     *
     */
    @JvmStatic
    @JvmOverloads
    fun List<Document>.surroundSeparate(
        emptiness: Document = empty,
        opening: Document = empty,
        sep: Document = commaSpace,
        closing: Document = empty,
        group: Boolean = true,
        indent: Int = 0,
        space: Int = 0,
    ) = if (isEmpty()) {
        emptiness
    } else {
        joinToDocument(sep).surround(indent = indent, space = space, opening = opening, closing = closing)
    }

    /**
     *
     * @see surround
     * @see separateMap
     */
    @JvmStatic
    @JvmOverloads
    fun <T> List<T>.surroundSeparateMap(
        emptiness: Document = empty,
        opening: Document = empty,
        sep: Document = commaSpace,
        closing: Document = empty,
        group: Boolean = true,
        indent: Int = 0,
        space: Int = 0,
        f: (T) -> Document,
    ) = if (isEmpty()) {
        emptiness
    } else {
        separateMap(sep, f).grouped(group).surround(indent = indent, space = space, opening = opening, closing = closing)
    }
}
