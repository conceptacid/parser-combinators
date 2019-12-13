package parser.idl

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import parser.core.Failure
import parser.core.State
import parser.core.Success
import parser.core.UnexpectedToken
import parser.idl.Identifier
import parser.idl.pIdentifier
import parser.json.jNull


class IdlParsersTest {

    @Test
    fun `Identifier Parser`() {
        assertThat(pIdentifier().run(State("Abcd")))
                .isEqualTo(Success(Identifier("Abcd"), State(input = "Abcd", col = 4, pos = 4)))

        assertThat(pIdentifier().run(State("abcd")))
                .isEqualTo(Failure(UnexpectedToken(label = "type-identifier", char = 'a', line = 0, col = 0)))

    }
}
