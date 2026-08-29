/* This file is part of kotlin-prettyprinting.
 * kotlin-prettyprinting is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only
 */
@file:Suppress("unused")
@file:JvmName("PPUtils")

package io.github.wadoon.pp

import java.io.PrintWriter
import java.util.*

/**
 * The empty document that prints nothing and has zero width requirement.
 * Acts as the identity element for document concatenation.
 */
@JvmField
val empty = Document.Empty

/**
 * Creates a character document from a single character.
 *
 * @param c the character to convert to a document
 * @return a document representing the character
 */
fun char(c: Char) = when (c) {
    '\n' -> hardline
    ' ' -> space
    else -> Document.Char(c)
}

/** One whitespace ` `. */
@JvmField
val space = Document.Blank(1)

/**
 * A document that adds a space only if needed (i.e., if the previous character is not whitespace).
 * Prevents duplicate spaces when concatenating documents.
 */
@JvmField
val spaceIfNeeded: Document = Document.Custom(object : CustomDocument {
    override val requirement: Requirement
        get() = space.requirement()

    override fun pretty(o: StatefulPrintWriter, indent: Int, flatten: Boolean) {
        if (o.state.column != 0 && !Character.isWhitespace(o.state.lastChar)) {
            Engine.pretty(o, indent, flatten, space)
        }
    }

    override fun compact(o: PrintWriter) {
        Engine.compact(o, space)
    }
})

/**
 * A document that clears the current line and starts a fresh line.
 * Only inserts a [hardline] if not already at the beginning of a line.
 * Useful for ensuring proper line separation without creating blank lines.
 */
@JvmField
val clearline: Document = Document.Custom(object : CustomDocument {
    override val requirement: Requirement
        get() = hardline.requirement()

    override fun pretty(o: StatefulPrintWriter, indent: Int, flatten: Boolean) {
        if (!o.state.freshLine) {
            Engine.pretty(o, indent, flatten, hardline)
        }
    }

    override fun compact(o: PrintWriter) {
        Engine.compact(o, hardline)
    }
})

/**
 * Creates a simple string as a document. The length requirement is determined by its content.
 *
 * @param s the string to convert to a document
 * @return a document representing the string
 */
fun string(s: String) = Document.String(s)

/**
 * Creates a string whose printed length might be different from the text length.
 *
 * This string can be cut out of a larger string to save memory.
 *
 * @param s the string holding the content
 * @param ofs the offset inside [s]
 * @param len the length that should be truncated from [s]
 * @param apparentLength the printed length used in the requirements
 * @return a document representing the substring with custom apparent length
 */
fun fancysubstring(s: String, ofs: Int, len: Int, apparentLength: Int) = if (len == 0) {
    empty
} else {
    Document.FancyString(s, ofs, len, apparentLength)
}

/**
 * Represents a substring from [s], with offset [ofs] and length [len].
 *
 * @param s the source string
 * @param ofs the starting offset
 * @param len the length of the substring
 * @return a document representing the substring
 * @see fancysubstring
 */
fun substring(s: String, ofs: Int, len: Int) = fancysubstring(s, ofs, len, len)

/**
 * Represents a text whose internal length is different from the printed text.
 * Useful for example for HTML and other markup languages where formatting tags
 * don't contribute to visible width.
 *
 * @param s the internal string
 * @param apparentLength the printed length (may differ from s.length)
 * @return a document with custom apparent length
 * @see fancysubstring
 */
fun fancystring(s: String, apparentLength: Int) = fancysubstring(s, 0, s.length, apparentLength)

/** A hardline unavoidable linebreak */
@JvmField
val hardline = Document.HardLine

/**
 * Creates a document with [n] number of spaces.
 *
 * @param n the number of spaces (must be >= 0)
 * @return a blank document with [n] spaces, or [empty] if [n] is 0
 */
fun blank(n: Int) = if (n == 0) empty else Document.Blank(n)

/**
 * Creates an IfFlat document, avoiding nesting [Document.IfFlat] in the left-hand side
 * of [Document.IfFlat], as this is redundant.
 *
 * When the parent group is flattened, [doc1] is printed. Otherwise, [doc2] is printed.
 *
 * @param doc1 the document to print when flattened (must not be an IfFlat itself)
 * @param doc2 the document to print when broken (multi-line mode)
 * @return an IfFlat document
 */
fun ifflat(doc1: Document, doc2: Document): Document = if (doc1 is Document.IfFlat) {
    ifflat(doc1.doc1, doc2)
} else {
    Document.IfFlat(doc1, doc2)
}

/** Adds [i] spaces if necessary, else hardline break.*/
private fun internalBreak(i: Int) = Document.IfFlat(blank(i), hardline)

/**
 * Represents an optional line break ([hardline]) with zero spaces. Marks positions for newlines.
 *
 * If the content fits on one line, nothing is printed. Otherwise, a hard line break is inserted.
 *
 * @see softbreak
 */
@JvmField
val break0 = internalBreak(0)

/**
 * Represents an optional line break ([hardline]).
 *
 * Alias for [break0]. Use [softbreak] for more readable code.
 *
 * @see break0
 */
@JvmField
val softbreak = break0

/**
 * Represents a single whitespace or a [hardline].
 *
 * If the content fits on one line, a single space is printed. Otherwise, a hard line break is inserted.
 *
 * @see breakableSpace
 */
@JvmField
val break1 = internalBreak(1)

/**
 * Represents a single whitespace or a [hardline].
 *
 * Alias for [break1]. Use [breakableSpace] for more readable code.
 *
 * @see break1
 */
@JvmField
val breakableSpace = break1

/**
 * Creates a document that prints [spaces] number of spaces if it fits on the current line,
 * or a [hardline] otherwise.
 *
 * @param spaces the number of spaces to print when on a single line
 * @return a document representing the conditional break with spaces
 */
fun breakOrSpaces(spaces: Int) = when (spaces) {
    0 -> break0
    1 -> break1
    else -> internalBreak(spaces)
}

/**
 * Concatenates two documents together.
 *
 * This is the primary way to combine documents. The resulting document prints [x] followed by [y].
 * Empty documents are optimized away for efficiency.
 *
 * @param x the first document
 * @param y the second document
 * @return a document representing the concatenation of [x] and [y]
 * @see Document.plus
 * @see concat
 */
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
 *
 * This affects all hardline breaks within [x], adding [indent] additional spaces
 * at the beginning of each line after a line break.
 *
 * @param indent the number of additional spaces to add after each line break (must be >= 0)
 * @param x the document to nest
 * @return a document with increased indentation
 * @throws IllegalArgumentException if [indent] is negative
 */
fun nest(indent: Int, x: Document) = Document.Nest(x.requirement(), indent, x)
    .also { require(indent >= 0) }

/**
 * Creates a group around [x], allowing the pretty-printer to choose between
 * a flat (single-line) or broken (multi-line) layout based on available space.
 *
 * Groups are the fundamental mechanism for enabling flexible formatting.
 *
 * @param x the document to group
 * @return a grouped document that can be flattened or broken across lines
 */
fun group(x: Document): Document {
    val req = x.requirement()
    /* Minor optimisation: an infinite Requirement dissolves a group. */
    return if (req.isInfinity) x else Document.Group(req, x)
}

/**
 * Extension function to create a group around this document.
 *
 * @param g if true (default), wraps this document in a [group]; otherwise returns it unchanged
 * @return a grouped document or this document unchanged
 */
@JvmOverloads
fun Document.grouped(g: Boolean = true) = if (g) group(this) else this

/**
 * Aligns [x] to the current column position.
 *
 * When encountered, [align] sets the indentation level to the current column,
 * so subsequent nested content aligns with the current position rather than
 * the left margin.
 *
 * @param x the document to align
 * @return an aligned document
 */
fun align(x: Document) = Document.Align(x.requirement(), x)

/**
 * Extension function to align this document to the current column position.
 *
 * @return an aligned version of this document
 * @see align
 */
fun Document.aligned() = Document.Align(requirement(), this)

/**
 * Wraps [this] document with a hook that is called after the document is printed.
 *
 * The hook receives a [PointRange] indicating the start and end positions
 * where the document was rendered in the output. This is useful for tracking
 * source code locations, building syntax highlighting information, or other
 * post-processing tasks.
 *
 * @param hook a function that receives the point range occupied by this document
 * @return a document with an attached range hook
 */
fun Document.range(hook: (PointRange) -> Unit) = Document.Range(requirement(), hook, this)

/** Left paren */
@JvmField
val lparen = char('(')

/** Right paren */
@JvmField
val rparen = char(')')

/** Left angle bracket */
@JvmField
val langle = char('<')

/** Right angle bracket */
@JvmField
val rangle = char('>')

/** Left curly brace */
@JvmField
val lbrace = char('{')

/** Right curly brace */
@JvmField
val rbrace = char('}')

/** Left square bracket */
@JvmField
val lbracket = char('[')

/** Right square bracket */
@JvmField
val rbracket = char(']')

/** Single quote */
@JvmField
val squote = char('\'')

/** Double quote */
@JvmField
val dquote = char('"')

/** Backtick */
@JvmField
val bquote = char('`')

/** Semicolon */
@JvmField
val semi = char(';')

/** Colon */
@JvmField
val colon = char(':')

/** Comma */
@JvmField
val comma = char(',')

/** Dot / period */
@JvmField
val dot = char('.')

/** Hash / sharp */
@JvmField
val sharp = char('#')

/** Forward slash */
@JvmField
val slash = char('/')

/** Backslash */
@JvmField
val backslash = char('\\')

/** Equals sign */
@JvmField
val equals = char('=')

/** Question mark */
@JvmField
val qmark = char('?')

/** Tilde */
@JvmField
val tilde = char('~')

/** At sign */
@JvmField
val at = char('@')

/** Percent sign */
@JvmField
val percent = char('%')

/** Dollar sign */
@JvmField
val dollar = char('$')

/** Caret */
@JvmField
val caret = char('^')

/** Ampersand */
@JvmField
val ampersand = char('&')

/** Asterisk / star */
@JvmField
val star = char('*')

/** Plus sign */
@JvmField
val plus = char('+')

/** Minus sign / hyphen */
@JvmField
val minus = char('-')

/** Underscore */
@JvmField
val underscore = char('_')

/** Exclamation mark / bang */
@JvmField
val bang = char('!')

/** Vertical bar / pipe character */
@JvmField
val bar = char('|')

/**
 * Concatenates [doc] with itself.
 *
 * @param doc the document to duplicate
 * @return a document representing [doc] concatenated with itself
 */
fun twice(doc: Document) = cat(doc, doc)

/**
 * Repeats this document [n] times by concatenating it with itself.
 *
 * @param n the number of repetitions (must be >= 0)
 * @return a document with this document repeated [n] times, or [empty] if [n] is 0
 */
fun Document.repeat(n: Int) = when (n) {
    0 -> empty
    1 -> this
    else -> (1..n).map { this }.fold(empty, ::cat)
}

/**
 * Prepends document [l] before document [x].
 *
 * @param l the document to prepend
 * @param x the document to precede
 * @return a document with [l] followed by [x]
 */
fun precede(l: Document, x: Document) = cat(l, x)

/**
 * Prepends string [l] before document [x].
 *
 * @param l the string to prepend
 * @param x the document to precede
 * @return a document with [l] converted to a document and followed by [x]
 */
fun precede(l: String, x: Document) = cat(string(l), x)

/**
 * Appends document [r] after document [x].
 *
 * @param r the document to append
 * @param x the document to terminate
 * @return a document with [x] followed by [r]
 */
fun terminate(r: Document, x: Document) = cat(x, r)

/**
 * Encloses document [x] between [l] (left) and [r] (right) documents.
 *
 * @param l the opening document
 * @param x the content document
 * @param r the closing document
 * @return a document with [l], [x], and [r] concatenated in order
 */
fun enclose(l: Document, x: Document, r: Document) = cat(cat(l, x), r)

/**
 * Encloses [x] in single quotes `'`.
 *
 * @param x the document to quote
 * @return a document with [x] surrounded by single quotes
 */
fun squotes(x: Document) = enclose(squote, x, squote)

/**
 * Encloses [x] in double quotes `"`.
 *
 * @param x the document to quote
 * @return a document with [x] surrounded by double quotes
 */
fun dquotes(x: Document) = enclose(dquote, x, dquote)

/**
 * Encloses [x] in backticks `` ` ``.
 *
 * @param x the document to quote
 * @return a document with [x] surrounded by backticks
 */
fun bquotes(x: Document) = enclose(bquote, x, bquote)

/**
 * Encloses [x] in curly braces `{}`.
 *
 * @param x the document to enclose
 * @return a document with [x] surrounded by curly braces
 */
fun braces(x: Document) = enclose(lbrace, x, rbrace)

/**
 * Encloses [x] in parentheses `()`.
 *
 * @param x the document to enclose
 * @return a document with [x] surrounded by parentheses
 */
fun parens(x: Document) = enclose(lparen, x, rparen)

/**
 * Encloses [x] in angle brackets `<>`.
 *
 * @param x the document to enclose
 * @return a document with [x] surrounded by angle brackets
 */
fun angles(x: Document) = enclose(langle, x, rangle)

/**
 * Encloses [x] in square brackets `[]`.
 *
 * @param x the document to enclose
 * @return a document with [x] surrounded by square brackets
 */
fun brackets(x: Document) = enclose(lbracket, x, rbracket)

/**
 * A variant of [fold] that keeps track of the element index.
 *
 * @param accu the initial accumulator value
 * @param f a function that takes the index, current accumulator, and element, returning a new accumulator
 * @return the final accumulated value
 */
fun <A, B> Iterable<A>.foldli(accu: B, f: (Int, B, A) -> B): B = foldIndexed(accu, f)

/* Working with lists of documents. */

/**
 * Concatenates a list of documents into a single document.
 *
 * We take advantage of the fact that [cat] operates in constant time, regardless of the size of its arguments.
 * The document that is constructed is essentially a reversed list (i.e., a tree that is biased towards the left).
 * This is not a problem; when pretty-printing this document, the engine will descend along the left branch,
 * pushing the nodes onto its stack as it goes down, effectively reversing the list again.
 *
 * @param docs the list of documents to concatenate
 * @return a single document representing the concatenation of all input documents
 */
fun concat(docs: List<Document>) = docs.fold(empty, ::cat)

/**
 * Concatenates vararg documents into a single document.
 *
 * @param docs the array of documents to concatenate
 * @return a single document representing the concatenation of all input documents
 * @see concat
 */
fun concat(vararg docs: Document) = concat(docs.toList())

/**
 * Joins a list of documents into a single document with [sep] as separator between elements.
 *
 * @param sep the separator document to insert between each element
 * @return a document representing all documents joined with separators
 */
fun <T : Document> List<T>.joinToDocument(sep: Document): Document = foldli(empty) { i, accu: Document, doc: Document ->
    if (i == 0) doc else cat(cat(accu, sep), doc)
}

/**
 * Maps each element of the list to a document and concatenates all results.
 *
 * @param f a function that transforms each element to a document
 * @return a document representing the concatenation of all mapped documents
 */
fun <T> List<T>.concatMap(f: (T) -> Document) = map(f).reduce(::cat)

/**
 * Joins a list of elements into a document, mapping each element using [f] and separating with [sep].
 *
 * @param sep the separator document (defaults to [empty])
 * @param f a function that transforms each element to a document
 * @return a document representing all mapped elements joined with separators
 * @see separateMap
 */
fun <T> List<T>.joinToDocument(sep: Document = empty, f: (T) -> Document) = separateMap(sep, f)

/**
 * Maps each element of the collection to a document and joins them with [sep] as separator.
 *
 * @param sep the separator document (defaults to [empty])
 * @param f a function that transforms each element to a document
 * @return a document representing all mapped elements joined with separators
 */
fun <T> Collection<T>.separateMap(sep: Document = empty, f: (T) -> Document) = foldli(empty) { i, accu: Document, x: T ->
    if (i == 0) {
        f(x)
    } else {
        cat(cat(accu, sep), f(x))
    }
}

/**
 * Joins a list of documents with [sep] between elements, but uses [lastSep] before the last element.
 *
 * This is useful for natural language lists like "a, b, and c" where you want different
 * separators for the last element.
 *
 * @param sep the separator for all elements except before the last
 * @param lastSep the separator before the last element
 * @return a document with custom separators
 */
fun <T : Document> List<T>.separate2(sep: Document = empty, lastSep: Document) = foldli(empty) { i, accu: Document, doc: Document ->
    if (i == 0) {
        doc
    } else {
        cat(accu, cat(if (i < this.size - 1) sep else lastSep, doc))
    }
}

/**
 * Converts an [Optional] to a document by applying [f] if present, or returns [empty] if absent.
 *
 * @param x the optional value
 * @param f a function to transform the value to a document if present
 * @return a document representing the optional value, or [empty] if not present
 */
fun <T> optional(x: Optional<T>, f: (T) -> Document): Document = x.map(f).orElse(empty) ?: empty

/**
 * Transforms a nullable receiver to a document using [f], or returns [empty] if null.
 *
 * @param f a function to transform the non-null receiver to a document
 * @return a document from [f] if this is non-null, otherwise [empty]
 */
fun <T> T?.orEmpty(f: T.() -> Document): Document = this?.f() ?: empty

/**
 * Returns this document if non-null, otherwise returns [empty].
 *
 * @return this document or [empty] if null
 */
fun <T> Document?.orEmpty(): Document = this ?: empty

/**
 * Chops the string [s] into a list of lines, which are turned into documents.
 *
 * @param s the string to split into lines
 * @return a list of documents, one per line
 */
fun lines(s: String) = s.split("\n").map { string(it) }

/**
 * Represents the given [s] text block by splitting on newlines and joining with [break1].
 *
 * Each line becomes a separate document, and lines are separated by optional breaks
 * that become actual line breaks when the text doesn't fit on one line.
 *
 * @param s the multi-line string to convert
 * @return a document representing the multi-line text with breakable spaces between lines
 */
fun multilineTextblock(s: String) = lines(s).joinToDocument(break1)

/**
 * Splits the string [s] at every occurrence of a character that satisfies the predicate [chars].
 * The substrings thus obtained are turned into documents, and a list of documents is returned.
 * No information is lost: the concatenation of the documents yields the original string.
 *
 * @param s the string to split
 * @param chars a predicate that returns true for delimiter characters
 * @return a list of documents representing the split substrings
 */
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

/**
 * Chops the string [s] into a list of words, which are turned into documents without any delimiters.
 *
 * @param s the string to split into words
 * @return a list of string documents representing non-whitespace words
 */
fun words(s: String): List<Document.String> = s.split("\\s+".toRegex()).map { it.trim() }.map(::string)

/**
 * Tokenizes string [s] using [delim] as delimiter characters.
 *
 * Each token becomes either a [string] document (for non-empty tokens),
 * a [blank] document (for whitespace), or [empty] (for empty tokens).
 *
 * @param s the string to tokenize
 * @param delim the delimiter characters (defaults to common programming language delimiters)
 * @return a document representing the tokenized string
 */
@JvmOverloads
fun tokenize(s: String, delim: String = " \t\n\r();[],!+-*/.,&%$§?"): Document {
    val seq = StringTokenizer(s, delim, true).asSequence().map { it as String }.toList()
    return seq.joinToDocument {
        when {
            it.isEmpty() -> empty
            it.isBlank() -> blank(it.length)
            else -> string(it)
        }
    }
}

/**
 * Converts string [s] into a document by splitting into words and joining with [breakableSpace].
 *
 * The resulting document can be formatted on a single line or broken across multiple lines
 * depending on available space.
 *
 * @param s the string to convert to a document
 * @param g if true (default), wraps the result in a [group] to allow flexible formatting
 * @return a document representing the string with breakable spaces between words
 */
@JvmOverloads
fun rejoin(s: String, g: Boolean = true): Document = words(s).joinToDocument(breakableSpace).grouped(g)

/**
 * Joins elements of [docs] into a document using [sep] as separator, mapping each element with [f].
 *
 * @param sep the separator document
 * @param docs the list of elements to join
 * @param f a function to transform each element to a document
 * @return a document representing the joined elements
 * @deprecated Use [flowMap] instead
 */
@Deprecated("Use flowMap instead", ReplaceWith("flowMap(sep, f)"))
fun <T> Iterable<T>.joinToDocument(sep: Document, docs: List<T>, f: (T) -> Document) = flowMap(sep, f)

/**
 * Maps and joins elements with [sep] as separator, with special formatting behavior.
 *
 * Unlike [separateMap], this function wraps each element (after the first) in a [group],
 * allowing the pretty-printer to decide whether to keep it on the same line or break.
 * This creates a "flowing" layout where elements wrap naturally.
 *
 * @param sep the separator document
 * @param f a function to transform each element to a document
 * @return a document with flowing layout
 */
fun <T> Iterable<T>.flowMap(sep: Document, f: (T) -> Document) = foldli(empty) { i: Int, accu: Document, doc: T ->
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
 * Creates a flowing layout from a list of documents with [sep] as separator.
 *
 * Each document (after the first) is wrapped in a group, allowing flexible line breaking.
 *
 * @param sep the separator document
 * @return a document with flowing layout
 * @see flowMap
 */
fun List<Document>.flow(sep: Document) = flowMap(sep) { it }

/**
 * Creates a document representing a URL that can break at `/` or `.` characters.
 *
 * The URL is split at slashes and dots, with zero-space breaks between parts,
 * allowing long URLs to wrap naturally at appropriate breaking points.
 *
 * @param s the URL string
 * @return a document representing the URL with break points
 */
fun url(s: String) = split(s) { it == '/' || it == '.' }.flow(breakOrSpaces(0))

/**
 * Hangs document [d] with indentation [indent].
 *
 * Combines [align] and [nest] to create a hanging indent effect where the first
 * line starts at the current position and subsequent lines are indented.
 *
 * @param indent the indentation level for subsequent lines
 * @param d the document to hang
 * @return a document with hanging indentation
 */
fun hang(indent: Int, d: Document) = align(nest(indent, d))

/**
 * Concatenates two documents with a [softbreak] (breakable space) between them.
 *
 * This operator provides convenient syntax for creating documents that can
 * break across lines: `doc1 / doc2`
 *
 * @param y the document to append
 * @return a document with this, a breakable space, and [y]
 */
operator fun Document.div(y: Document) = this + break1 + y

/**
 * Concatenation operator for two documents.
 *
 * @param y the document to append
 * @return a document representing this concatenated with [y]
 * @see cat
 */
operator fun Document.plus(y: Document) = cat(this, y)

/**
 * Concatenation operator for string and document.
 *
 * @param y the document to append
 * @return a document with this string converted to a document and concatenated with [y]
 */
operator fun String.plus(y: Document) = cat(string(this), y)

/**
 * Concatenation operator for document and string.
 *
 * @param y the string to append
 * @return a document with this document concatenated with [y] converted to a document
 */
operator fun Document.plus(y: String) = cat(this, string(y))

/**
 * Creates a block of documents, which are either printed on one line or printed over several lines,
 * with indentation [intend].
 *
 * @param intend indentation for each line after the first
 * @param space the number of spaces before the second block
 * @param x the first document (typically a header or label)
 * @param y the second document (typically the content)
 * @return a document with [x] followed by [y] with appropriate indentation and spacing
 */
fun prefix(intend: Int, space: Int, x: Document, y: Document) = group(x + nest(intend, (breakOrSpaces(space) + y)))

/**
 * Infix operator for [prefix] with default values (indent=2, space=1).
 *
 * Creates a layout where [this] is followed by [y] with proper indentation.
 * Commonly used for function applications, type annotations, etc.
 *
 * @param y the document to append with indentation
 * @return a prefixed document
 * @see prefix
 */
infix fun Document.prefixed(y: Document) = prefix(2, 1, this, y)

/**
 * Adds the given amount of [space]s in front of [x] if it would fit into the remaining line.
 * Otherwise, the text moves to the next line and is indented by [indent].
 *
 * @param x the document to jump
 * @param indent indentation when broken to next line (default: 0)
 * @param space number of spaces before [x] when on same line (default: 0)
 * @return a document that conditionally jumps to a new line
 */
@JvmOverloads
fun jump(x: Document, indent: Int = 0, space: Int = 0) = group(
    nest(
        indent,
        breakOrSpaces(space) + x,
    ),
)

/**
 * Extension function version of [jump].
 *
 * @param indent indentation when broken to next line (default: 0)
 * @param space number of spaces before this document when on same line (default: 0)
 * @return a jumped version of this document
 * @see jump
 */
@JvmOverloads
fun Document.jumped(indent: Int = 0, space: Int = 0) = jump(this, indent, space)

/**
 * Creates an infix notation layout: `x op y` with proper spacing and indentation.
 *
 * @param n the nesting level
 * @param b the number of blank spaces around the operator
 * @param op the operator document
 * @param x the left operand
 * @param y the right operand
 * @return a document representing infix notation
 */
fun `infix`(n: Int, b: Int, op: Document, x: Document, y: Document) = prefix(n, b, x + blank(b) + op, y)

/**
 * Surrounds [this] document with [opening] and [closing] delimiters, with optional indentation and spacing.
 *
 * Wraps [this] document with [opening] and [closing] delimiters, with optional
 * indentation and spacing. The content can flow on the same line or break
 * across multiple lines with proper indentation.
 *
 * @param opening the opening delimiter document
 * @param closing the closing delimiter document
 * @param indent the indentation level for content (default: 0)
 * @param space the number of spaces around content (default: 0)
 * @return a surrounded document with delimiters
 */
fun Document.surround(opening: Document, closing: Document, indent: Int = 0, space: Int = 0) = group(
    opening +
        nest(
            indent,
            (breakOrSpaces(space) + this) +
                breakOrSpaces(space) + closing,
        ),
)

/**
 * Creates a soft surround layout with flexible line breaking.
 *
 * Similar to [surround] but with additional grouping for more flexible formatting.
 * The content and closing can independently decide to break to new lines.
 *
 * @param contents the content document to surround
 * @param opening the opening delimiter (default: `(`)
 * @param closing the closing delimiter (default: `)`)
 * @param indent the indentation level (default: 0)
 * @param space the number of spaces around content (default: 0)
 * @return a softly surrounded document
 */
@JvmOverloads
fun softSurround(contents: Document, opening: Document = lparen, closing: Document = rparen, indent: Int = 0, space: Int = 0) = group(
    opening + nest(indent, group(breakOrSpaces(space) + contents) + group((breakOrSpaces(space) + closing))),
)

/** A comma followed by a breakable space: `, ` */
val commaSpace = comma + break1

/**
 * Joins a list of documents with [sep] separator and surrounds them with [opening] and [closing].
 *
 * If the list is empty, returns [emptiness]. The result is wrapped in a group
 * for flexible formatting.
 *
 * @param emptiness the document to return if the list is empty (default: [empty])
 * @param opening the opening delimiter (default: [empty])
 * @param sep the separator between elements (default: [commaSpace])
 * @param closing the closing delimiter (default: [empty])
 * @param group if true (default), wraps the result in a group
 * @param indent the indentation level
 * @param space the number of spaces around content
 * @return a surrounded and separated document
 * @see surround
 * @see joinToDocument
 */
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
 * Maps a collection to documents, joins them with separator, and surrounds with delimiters.
 *
 * Combines [separateMap] and [surround] for common pattern of mapping, separating,
 * and enclosing a collection of elements.
 *
 * @param emptiness the document to return if the collection is empty (default: [empty])
 * @param opening the opening delimiter (default: [empty])
 * @param sep the separator between elements (default: [commaSpace])
 * @param closing the closing delimiter (default: [empty])
 * @param group if true (default), wraps the result in a group
 * @param indent the indentation level
 * @param space the number of spaces around content
 * @param f a function to transform each element to a document
 * @return a surrounded and separated document
 * @see surround
 * @see separateMap
 */
@JvmOverloads
fun <T> Collection<T>.surroundSeparateMap(
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

/**
 * Builder class for creating surrounded and separated document layouts fluently.
 *
 * This builder provides a fluent API for configuring all aspects of a surround-separate
 * layout, including mapping function, separators, delimiters, and formatting options.
 *
 * @param T the type of elements to transform
 * @see surroundSeparateMap
 */
open class SurroundSepMapBuilder<T> {
    var map: (T) -> Document = { empty }
    var list: Collection<T> = listOf()
    var emptiness: Document = empty
    var opening: Document = empty
    var sep: Document = commaSpace
    var closing: Document = empty
    var group: Boolean = true
    var indent: Int = 0
    var space: Int = 0

    /**
     * Builds the final document using the configured parameters.
     *
     * @return a document with the configured surround-separate layout
     */
    fun build() = list.surroundSeparateMap(
        opening = opening,
        sep = sep,
        closing = closing,
        group = group,
        indent = indent,
        space = space,
        f = map,
    )

    /**
     * Sets the mapping function to transform elements to documents.
     *
     * @param f a function that transforms an element of type [T] to a document
     * @return this builder for method chaining
     */
    fun map(f: (T) -> Document) = also { map = f }

    /**
     * Sets the collection of elements to transform.
     *
     * @param x the collection of elements
     * @return this builder for method chaining
     */
    fun list(x: Collection<T>) = also { list = x }

    /**
     * Sets the document to use when the collection is empty.
     *
     * @param x the emptiness document
     * @return this builder for method chaining
     */
    fun emptiness(x: Document) = also { emptiness = x }

    /**
     * Sets the opening delimiter document.
     *
     * @param x the opening delimiter
     * @return this builder for method chaining
     */
    fun opening(x: Document) = also { opening = x }

    /**
     * Sets the separator document between elements.
     *
     * @param x the separator document
     * @return this builder for method chaining
     */
    fun sep(x: Document) = also { sep = x }

    /**
     * Sets the closing delimiter document.
     *
     * @param x the closing delimiter
     * @return this builder for method chaining
     */
    fun closing(x: Document) = also { closing = x }

    /**
     * Sets whether to wrap the result in a group for flexible formatting.
     *
     * @param x true to group the result, false otherwise
     * @return this builder for method chaining
     */
    fun group(x: Boolean) = also { group = x }

    /**
     * Sets the indentation level for content.
     *
     * @param x the indentation level
     * @return this builder for method chaining
     */
    fun indent(x: Int) = also { indent = x }

    /**
     * Sets the number of spaces around content.
     *
     * @param x the number of spaces
     * @return this builder for method chaining
     */
    fun space(x: Int) = also { space = x }
}

/**
 * Creates a [SurroundSepMapBuilder] initialized with a sequence.
 *
 * This is a convenience function for starting the builder pattern with a collection.
 *
 * @param seq the collection of elements to transform
 * @return a builder initialized with the sequence
 */
fun <T> reducer(seq: Collection<T>) = SurroundSepMapBuilder<T>().list(seq)

/**
 * Helper class providing common pretty-printing patterns.
 *
 * Provides pre-configured builders for common layouts like parenthesized lists.
 */
class PPHelper {
    /** Default indentation level of 2 spaces */
    val indent = 2

    /**
     * Creates a builder for a parenthesized list with default settings.
     *
     * Configures opening/closing with parentheses, indentation of 2,
     * and comma-space separation.
     *
     * @param seq the collection of elements to format
     * @return a pre-configured builder for parenthesized lists
     */
    fun <T> withParens(seq: Collection<T>) = SurroundSepMapBuilder<T>().list(seq).opening(lparen).closing(rparen).indent(2)
        .sep(commaSpace)
}
