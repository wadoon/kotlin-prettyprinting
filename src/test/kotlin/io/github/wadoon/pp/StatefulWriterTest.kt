/* This file is part of kotlin-prettyprinting.
 * kotlin-prettyprinting is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only
 */
package io.github.wadoon.pp.io.github.wadoon.pp

import io.github.wadoon.pp.State
import io.github.wadoon.pp.StatefulWriter
import org.junit.Assert
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.StringWriter
import java.io.Writer

/**
 *
 * @author Alexander Weigl
 * @version 1 (29.08.26)
 */
class StatefulWriterTest {
    @Test
    fun test1() {
        val state = State()
        val s = StatefulWriter(NullWriter, state)
        s.write("abc\nabc")

        assertEquals(3, state.column)
        assertEquals(1, state.line)
        assertEquals(0, state.lastIndent)
        assertEquals(false, state.freshLine)
        assertEquals('c', state.lastChar)

        s.write("\n")
        assertEquals(0, state.column)
        assertEquals(2, state.line)
        assertEquals(0, state.lastIndent)
        assertEquals(true, state.freshLine)
        assertEquals('\n', state.lastChar)

        s.write("    ")
        assertEquals(4, state.column)
        assertEquals(2, state.line)
        assertEquals(4, state.lastIndent)
        assertEquals(true, state.freshLine)
        assertEquals(' ', state.lastChar)

        s.write("    test  ")
        assertEquals(14, state.column)
        assertEquals(2, state.line)
        assertEquals(8, state.lastIndent)
        assertEquals(false, state.freshLine)
        assertEquals(' ', state.lastChar)

        s.write("\n\t\t")
        assertEquals(2, state.column)
        assertEquals(3, state.line)
        assertEquals(2, state.lastIndent)
        assertEquals(true, state.freshLine)
        assertEquals('\t', state.lastChar)
    }
}

object NullWriter : Writer() {
    override fun write(cbuf: CharArray, off: Int, len: Int) {}

    override fun flush() {}

    override fun close() {}
}
