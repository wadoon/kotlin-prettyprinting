package io.github.wadoon.pp

import io.github.wadoon.pp.Engine.pretty
import java.io.PrintWriter
import java.io.StringWriter
import java.util.*

/** The pretty rendering engine.
 *
The renderer is supposed to behave exactly like Daan Leijen's, although its
implementation is quite radically different, and simpler. Our documents are
constructed eagerly, as opposed to lazily. This means that we pay a large
space overhead, but in return, we get the ability of computing information
bottom-up, as described above, which allows to render documents without
backtracking or buffering.

The [State] record is never copied; it is just threaded through. In
addition to it, the parameters [indent] and [flatten] influence the
manner in which the document is rendered.

The code is written in tail-recursive style, so as to avoid running out of
stack space if the document is very deep. Each [KCons] cell in a
continuation represents a pending call to [pretty]. Each [KRange] cell
represents a pending call to a user-provided range hook.
 */
object Engine {
    sealed class Continuation
    data object KNil : Continuation()
    data class KCons(val indent: Int, val flatten: Boolean, val doc: Document, val cont: Continuation) : Continuation()
    data class KRange(val hook: (PointRange) -> Unit, val start: Point, val cont: Continuation) : Continuation()

    fun proceed(output: PrintWriter, state: State, x: Continuation) {
        when (x) {
            is KNil -> Unit
            is KCons -> pretty(output, state, x.indent, x.flatten, x.doc, x.cont)
            is KRange -> {
                var y = x
                while (y is KRange) {
                    val finish = Point(state.line, state.column)
                    y.hook(PointRange(y.start, finish))
                    y = y.cont
                }
                if (y is KCons) {
                    pretty(output, state, y.indent, y.flatten, y.doc, y.cont)
                }
            }
        }
    }

    /**
     *
     */
    fun prettyQ(output: PrintWriter, state: State, indent: Int, flatten: Boolean, doc: Document) {
        val queue = ArrayDeque<Continuation>(1024)
        queue.add(KCons(indent, flatten, doc, KNil))

        fun proceed(x: Continuation) {
            queue.push(x)
        }

        fun handle(indent: Int, flatten: Boolean, doc: Document, cont: Continuation) =
            when (doc) {
                is Document.Empty -> {}
                is Document.Char -> {
                    output.print(doc.char)
                    state.column += 1
                    proceed(cont)
                }

                is Document.String -> {
                    output.print(doc.s.take(doc.s.length))
                    state.column += doc.s.length
                    /* assert (ok state flatten); */
                    proceed(cont)
                }

                is Document.FancyString -> {
                    output.print(doc.s.substring(doc.ofs, doc.len))
                    state.column += doc.apparentLength
                    /* assert (ok state flatten); */
                    proceed(cont)
                }

                is Document.Blank -> {
                    output.print(" ".repeat(doc.len))
                    state.column += doc.len
                    /* assert (ok state flatten); */
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
                    state.line += 1
                    state.column = indent
                    state.lastIndent = indent
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
                            indent, flatten, doc.doc1,
                            KCons(indent, flatten, doc.doc2, cont)
                        )
                    )

                is Document.Nest ->
                    proceed(KCons(indent + doc.j, flatten, doc.doc, cont))

                is Document.Group -> {
                    /* If we already are in flattening mode, stay in flattening mode; we
                     * are committed to it. If we are not already in flattening mode, we
                     * have a choice of entering flattening mode. We enter this mode only
                     * if we know that this group fits on this line without violating the
                     * width or ribbon width constraints. Thus, we never backtrack. */
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
                    proceed(KCons(state.column, flatten, doc.doc, cont))

                is Document.Range -> {
                    val start = Point(state.line, state.column)
                    proceed(KCons(state.column, flatten, doc.doc, KRange(doc.fn, start, cont)))
                }

                is Document.Custom -> {
                    /* Invoke the document's custom rendering function. */
                    doc.doc.pretty(output, state, indent, flatten)
                    /* Sanity check. */
                    // assert(ok state flatten);
                    /* __continue. */
                    proceed(cont)
                }
            }

        while (queue.isNotEmpty()) {
            when (val x = queue.pop()) {
                is KNil -> return
                is KCons -> {
                    handle(x.indent, x.flatten, x.doc, x.cont)
                    // queue.addLast(x.cont)
                }

                is KRange -> {
                    val finish = Point(state.line, state.column)
                    x.hook(PointRange(x.start, finish))
                    proceed(x.cont)
                }
            }
        }
    }

    /**
     *
     */
    private tailrec fun pretty(
        output: PrintWriter, state: State, indent: Int, flatten: Boolean, doc: Document,
        cont: Continuation
    ) {
        when (doc) {
            is Document.Empty -> proceed(output, state, cont)
            is Document.Char
                -> {
                output.print(doc.char)
                state.column += 1
                /* assert (ok state flatten); */
                proceed(output, state, cont)
            }

            is Document.String -> {
                output.print(doc.s.take(doc.s.length))
                state.column += doc.s.length
                /* assert (ok state flatten); */
                proceed(output, state, cont)
            }

            is Document.FancyString -> {
                output.print(doc.s.substring(doc.ofs, doc.len))
                state.column += doc.apparentLength
                /* assert (ok state flatten); */
                proceed(output, state, cont)
            }

            is Document.Blank -> {
                output.print(" ".repeat(doc.len))
                state.column += doc.len
                /* assert (ok state flatten); */
                proceed(output, state, cont)
            }

            is Document.HardLine -> {
                /* We cannot be in flattening mode, because a hard line has an [infinity]
               Requirement, and we attempt to render a group in flattening mode only
               if this group's Requirement is met. */
                require(!flatten)
                /* Emit a hardline. */
                output.print("\n")
                output.print(" ".repeat(indent))
                state.line += 1
                state.column = indent
                state.lastIndent = indent
                proceed(output, state, cont)
            }

            is Document.IfFlat -> {
                // Pick an appropriate sub-document, based on the current flattening mode.
                pretty(output, state, indent, flatten, if (flatten) doc.doc1 else doc.doc2, cont)
            }

            is Document.Cat ->
                /* Push the second document onto the continuation. */
                pretty(
                    output, state, indent, flatten, doc.doc1,
                    KCons(indent, flatten, doc.doc2, cont)
                )

            is Document.Nest ->
                pretty(output, state, indent + doc.j, flatten, doc.doc, cont)

            is Document.Group -> {
                /* If we already are in flattening mode, stay in flattening mode; we
                 * are committed to it. If we are not already in flattening mode, we
                 * have a choice of entering flattening mode. We enter this mode only
                 * if we know that this group fits on this line without violating the
                 * width or ribbon width constraints. Thus, we never backtrack. */
                val column = Requirement(state.column) + doc.req
                val flatten2 = flatten || column <= state.width && column <= state.lastIndent + state.ribbon
                pretty(output, state, indent, flatten2, doc.doc, cont)
            }

            is Document.Align ->
                /* The effect of this combinator is to set [indent] to [state.column].
            Usually [indent] is equal to [state.last_indent], hence setting it
            to [state.column] increases it. However, if [nest] has been used
            since the current line began, then this could cause [indent] to
            decrease. */
                /* assert (state.column > state.last_indent); */
                pretty(output, state, state.column, flatten, doc.doc, cont)

            is Document.Range -> {
                val start = Point(state.line, state.column)
                pretty(output, state, indent, flatten, doc.doc, KRange(doc.fn, start, cont))
            }
            //        is Custom c
            //      -> {
            /* Invoke the document's custom rendering function. */
            //        c#pretty output state indent flatten;
            /* Sanity check. */
            //      assert(ok state flatten);
            /* __continue. */
            //    __continue(output, state, cont)
            // }
            is Document.Custom -> TODO()
        }
    }

    /** Publish a version of [pretty] that does not take an explicit continuation.
     * This function may be used by authors of custom documents. We do not expose
     * the internal [pretty] -- the one that takes a continuation -- because we
     * wish to simplify the user's life. The price to pay is that calls that go
     * through a custom document cannot be tail calls.
     */
    fun pretty(output: PrintWriter, state: State, indent: Int, flatten: Boolean, doc: Document) =
        pretty(output, state, indent, flatten, doc, KNil)

    fun pretty(doc: Document, width: Int = 80, rfrac: Double = 0.2, indent: Int = 0, flatten: Boolean = false): String =
        pretty(doc, State(width, rfrac), indent, flatten)

    fun pretty(doc: Document, state: State, indent: Int = 0, flatten: Boolean = false): String {
        val sw = StringWriter()
        val output = PrintWriter(sw)
        pretty(output, state, indent, flatten, doc, KNil)
        return sw.toString()
    }

    fun proceedCompact(output: PrintWriter, cont: List<Document>) {
        if (cont.isEmpty()) return
        compact(output, cont.first(), cont.subList(1, cont.lastIndex))
    }

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
            // is document.Custom ->
            //    /* Invoke the document's custom rendering function. */
            //    c#compact output;
            // continue output cont
            is Document.Custom -> TODO()
        }
    }
}