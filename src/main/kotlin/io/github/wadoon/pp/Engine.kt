/* This file is part of kotlin-prettyprinting.
 * kotlin-prettyprinting is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only
 */
package io.github.wadoon.pp

import io.github.wadoon.pp.Engine.pretty
import java.io.*
import java.util.*

/** The pretty rendering engine.
 *
 * The renderer is supposed to behave exactly like Daan Leijen's,
 * although its implementation is quite radically different, and simpler.
 * Our documents are constructed eagerly, as opposed to lazily. This means
 * that we pay a large space overhead, but in return, we get the ability
 * of computing information bottom-up, as described above, which allows
 * to render documents without backtracking or buffering.
 *
 * The [State] record is never copied; it is just threaded through. In
 * addition to it, the parameters `indent` and [flatten] influence the
 * manner in which the document is rendered.
 *
 * The code is written in tail-recursive style, so as to avoid running out of
 * stack space if the document is very deep. Each [KCons] cell in a
 * continuation represents a pending call to [pretty]. Each [KRange] cell
 * represents a pending call to a user-provided range hook.
 */
object Engine {
    private sealed class Continuation
    private data object KNil : Continuation()
    private data class KCons(val indent: Int, val flatten: Boolean, val doc: Document, val cont: Continuation) : Continuation()
    private data class KRange(val hook: (PointRange) -> Unit, val start: Point, val cont: Continuation) : Continuation()

    private fun proceed(output: StatefulPrintWriter, x: Continuation) {
        when (x) {
            is KNil -> Unit

            is KCons -> pretty(output, x.indent, x.flatten, x.doc, x.cont)

            is KRange -> {
                var y = x
                while (y is KRange) {
                    val state = output.state
                    val finish = Point(state.line, state.column)
                    y.hook(PointRange(y.start, finish))
                    y = y.cont
                }
                if (y is KCons) {
                    pretty(output, y.indent, y.flatten, y.doc, y.cont)
                }
            }
        }
    }

    @JvmStatic
    fun prettyQ(doc: Document, width: Int = 80, rfrac: Double = 1.0, indent: Int = 0, flatten: Boolean = false): String =
        StringWriter().let {
            prettyQ(doc, StatefulPrintWriter(it, State(width, rfrac)), indent, flatten)
            it.toString()
        }

    /**
     *
     */
    @JvmStatic
    @JvmOverloads
    fun prettyQ(doc: Document, output: StatefulPrintWriter, indent: Int = 0, flatten: Boolean = false) {
        val queue = ArrayDeque<Continuation>(1024)
        queue.add(KCons(indent, flatten, doc, KNil))

        fun proceed(x: Continuation) {
            queue.push(x)
        }

        fun handle(indent: Int, flatten: Boolean, doc: Document, cont: Continuation) = when (doc) {
            is Document.Empty -> {}

            is Document.Char -> {
                output.print(doc.char)
                proceed(cont)
            }

            is Document.String -> {
                output.print(doc.s.take(doc.s.length))
                proceed(cont)
            }

            is Document.FancyString -> {
                output.print(doc.s.substring(doc.ofs, doc.len))
                proceed(cont)
            }

            is Document.Blank -> {
                output.print(" ".repeat(doc.len))
                proceed(cont)
            }

            is Document.HardLine -> {
                /* We cannot be in flattening mode, because a hard line has an [infinity]
               Requirement, and we attempt to render a group in flattening mode only
               if this group's Requirement is met. */
                require(!flatten)
                /* Emit a hardline. */
                output.print("\n")
                output.print(" ".repeat(indent))
                proceed(cont)
            }

            is Document.IfFlat -> {
                /* Pick an appropriate sub-document, based on the current flattening mode. */
                proceed(KCons(indent, flatten, if (flatten) doc.doc1 else doc.doc2, cont))
            }

            is Document.Cat ->
                /* Push the second document onto the continuation. */
                proceed(
                    KCons(
                        indent,
                        flatten,
                        doc.doc1,
                        KCons(indent, flatten, doc.doc2, cont),
                    ),
                )

            is Document.Nest ->
                proceed(KCons(indent + doc.j, flatten, doc.doc, cont))

            is Document.Group -> {
                /* If we already are in flattening mode, stay in flattening mode; we
                 * are committed to it. If we are not already in flattening mode, we
                 * have a choice of entering flattening mode. We enter this mode only
                 * if we know that this group fits on this line without violating the
                 * width or ribbon width constraints. Thus, we never backtrack. */
                val state = output.state
                val column = Requirement(state.column) + doc.req
                val flatten2 = flatten || (column <= state.width && column <= state.lastIndent + state.ribbon)
                proceed(KCons(indent, flatten2, doc.doc, cont))
            }

            is Document.Align ->
                /* The effect of this combinator is to set [indent] to [state.column].
            Usually [indent] is equal to [state.last_indent], hence setting it
            to [state.column] increases it. However, if [nest] has been used
            since the current line began, then this could cause [indent] to
            decrease. */
                /* assert (state.column > state.last_indent); */
                proceed(KCons(output.state.column, flatten, doc.doc, cont))

            is Document.Range -> {
                val start = Point(output.state.line, output.state.column)
                proceed(KCons(output.state.column, flatten, doc.doc, KRange(doc.hook, start, cont)))
            }

            is Document.Custom -> {
                doc.doc.pretty(output, indent, flatten)
                proceed(cont)
            }
        }

        while (queue.isNotEmpty()) {
            when (val x = queue.pop()) {
                is KNil -> return

                is KCons -> handle(x.indent, x.flatten, x.doc, x.cont)

                is KRange -> {
                    val finish = Point(output.state.line, output.state.column)
                    x.hook(PointRange(x.start, finish))
                    proceed(x.cont)
                }
            }
        }
    }

    /**
     *
     */
    @JvmStatic
    private tailrec fun pretty(output: StatefulPrintWriter, indent: Int, flatten: Boolean, doc: Document, cont: Continuation) {
        val state = output.state
        when (doc) {
            is Document.Empty -> proceed(output, cont)

            is Document.Char -> {
                output.print(doc.char)
                proceed(output, cont)
            }

            is Document.String -> {
                val s = doc.s.take(doc.s.length)
                output.print(s)
                proceed(output, cont)
            }

            is Document.FancyString -> {
                output.print(doc.s.substring(doc.ofs, doc.len))
                proceed(output, cont)
            }

            is Document.Blank -> {
                output.print(" ".repeat(doc.len))
                proceed(output, cont)
            }

            is Document.HardLine -> {
                /* We cannot be in flattening mode, because a hard line has an [infinity]
               Requirement, and we attempt to render a group in flattening mode only
               if this group's Requirement is met. */
                require(!flatten)
                /* Emit a hardline. */
                output.print("\n")
                output.print(" ".repeat(indent))
                proceed(output, cont)
            }

            is Document.IfFlat -> {
                // Pick an appropriate sub-document, based on the current flattening mode.
                pretty(output, indent, flatten, if (flatten) doc.doc1 else doc.doc2, cont)
            }

            is Document.Cat ->
                /* Push the second document onto the continuation. */
                pretty(
                    output,
                    indent,
                    flatten,
                    doc.doc1,
                    KCons(indent, flatten, doc.doc2, cont),
                )

            is Document.Nest ->
                pretty(output, indent + doc.j, flatten, doc.doc, cont)

            is Document.Group -> {
                /* If we already are in flattening mode, stay in flattening mode; we
                 * are committed to it. If we are not already in flattening mode, we
                 * have a choice of entering flattening mode. We enter this mode only
                 * if we know that this group fits on this line without violating the
                 * width or ribbon width constraints. Thus, we never backtrack. */
                val column = Requirement(state.column) + doc.req
                val flatten2 = flatten || ((column <= state.width) && (column <= (state.lastIndent + state.ribbon)))
                pretty(output, indent, flatten2, doc.doc, cont)
            }

            is Document.Align ->
                /* The effect of this combinator is to set [indent] to [state.column].
                 * Usually [indent] is equal to [state.last_indent], hence setting it
                 * to [state.column] increases it. However, if [nest] has been used
                 * since the current line began, then this could cause [indent] to
                 * decrease. */
                pretty(output, output.state.column, flatten, doc.doc, cont)

            is Document.Range -> {
                val start = Point(state.line, state.column)
                pretty(output, indent, flatten, doc.doc, KRange(doc.hook, start, cont))
            }

            is Document.Custom -> {
                val cd = doc.doc
                cd.pretty(output, indent, flatten)
                proceed(output, cont)
            }
        }
    }

    /** Publish a version of [pretty] that does not take an explicit continuation.
     * This function may be used by authors of custom documents. We do not expose
     * the internal [pretty] -- the one that takes a continuation -- because we
     * wish to simplify the user's life. The price to pay is that calls that go
     * through a custom document cannot be tail calls.
     */
    @JvmStatic
    fun pretty(output: StatefulPrintWriter, indent: Int, flatten: Boolean, doc: Document) = pretty(output, indent, flatten, doc, KNil)

    @JvmStatic
    @JvmOverloads
    fun pretty(doc: Document, width: Int = 80, rfrac: Double = 1.0, indent: Int = 0, flatten: Boolean = false): String =
        pretty(doc, State(width, rfrac), indent, flatten)

    @JvmStatic
    @JvmOverloads
    fun pretty(doc: Document, state: State, indent: Int = 0, flatten: Boolean = false): String {
        val sw = StringWriter()
        val output = StatefulPrintWriter(sw, state)
        pretty(output, indent, flatten, doc, KNil)
        return sw.toString()
    }

    private fun proceedCompact(output: PrintWriter, cont: List<Document>) {
        if (cont.isEmpty()) return
        compact(output, cont.first(), cont.subList(1, cont.lastIndex))
    }

    @JvmStatic
    @JvmOverloads
    tailrec fun compact(output: PrintWriter, doc: Document, cont: List<Document> = listOf()) {
        when (doc) {
            is Document.Empty -> proceedCompact(output, cont)

            is Document.Char -> {
                output.print(doc.char)
                proceedCompact(output, cont)
            }

            is Document.String -> {
                val len = doc.s.length
                output.print(doc.s.take(len))
                proceedCompact(output, cont)
            }

            is Document.FancyString -> {
                output.print(doc.s.substring(doc.ofs, doc.len))
                proceedCompact(output, cont)
            }

            is Document.Blank -> {
                output.print(" ".repeat(doc.len))
                proceedCompact(output, cont)
            }

            is Document.HardLine -> {
                output.print('\n')
                proceedCompact(output, cont)
            }

            is Document.Cat ->
                proceedCompact(output, listOf(doc.doc1, doc.doc2) + cont)

            is Document.IfFlat -> compact(output, doc.doc1, cont)

            is Document.Nest -> compact(output, doc.doc, cont)

            is Document.Group -> compact(output, doc.doc, cont)

            is Document.Align -> compact(output, doc.doc, cont)

            is Document.Range -> compact(output, doc.doc, cont)

            is Document.Custom -> {
                doc.doc.compact(output)
                proceedCompact(output, cont)
            }
        }
    }
}

/**
 * Tracking the current
 */
class StatefulWriter(val out: Writer, val state: State) : Writer() {
    override fun close() {
        out.close()
    }

    override fun flush() {
        out.flush()
    }

    override fun write(cbuf: CharArray, off: Int, len: Int) {
        for (i in off until off + len) {
            val ch = cbuf[i]
            state.lastChar = ch
            if (ch == '\n') {
                state.line += 1
                state.column = 0
                state.freshLine = true
                state.lastIndent = 0
            } else if (Character.isWhitespace(ch)) {
                state.column += 1
                if (state.freshLine) {
                    state.lastIndent += 1
                }
            } else {
                state.column += 1
                state.freshLine = false
            }
        }
        out.write(cbuf, off, len)
    }
}

class StatefulPrintWriter(out: Writer, val state: State) : PrintWriter(StatefulWriter(out, state))
