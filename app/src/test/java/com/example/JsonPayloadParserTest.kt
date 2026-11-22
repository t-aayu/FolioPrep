package com.example

import com.example.domain.JsonPayloadParser
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonPayloadParserTest {

    @Test
    fun `extractJsonObject repairs markdown escaped brackets and preserves double escaped newlines`() {
        val rawAiClipboard = """
            { "title": "Great Madras Famine (1876–78)", "tags": \["History", "Modern Indian History", "Colonial India"\], "mcqs": \[ { "q": "With reference to the Great Madras Famine of 1876–78, consider the following statements:\\n\\n1. It is described as having wiped out approximately one-third of the population in the affected region.\\n2. Landless Dalit labourers constituted a majority of those who died, according to the passage.\\n3. The affected population had substantial savings and food reserves to withstand the famine.\\n4. The historical event is the subject of Jeyamohan's novel White Elephant, translated from Tamil.", "opts": \[ "Only two of the statements given above are correct.", "Only three of the statements given above are correct.", "Only one of the statements given above are correct.", "All four of the statements given above are correct." \], "ans": 0, "exp": "Statements 1 and 2 are correct." } \] }
        """.trimIndent()

        val sanitized = JsonPayloadParser.extractJsonObject(rawAiClipboard)
        assertNotNull(sanitized)
        val json = sanitized!!
        assertFalse("Should not contain escaped opening brackets", json.contains("\\["))
        assertFalse("Should not contain escaped closing brackets", json.contains("\\]"))
        assertTrue(json.contains("\"tags\": [\"History\", \"Modern Indian History\", \"Colonial India\"]"))
        assertTrue(json.contains("\"mcqs\": [ {"))
        assertTrue(json.contains("\"opts\": [ \"Only two"))
    }

    @Test
    fun `extractJsonObject strips conversational filler, fences, trailing commas, and escaped braces`() {
        val raw = """
            Here is your UPSC CSE quiz set:
            ```json
            \{
              "title": "Forest Conservation \& Ecology",
              "tags": \["Environment", "GS\-III"\],
              "subjective": \[
                \{
                  "q": "Critically examine India's forest governance framework.",
                  "marks": 15,
                  "points": \["Point 1", "Point 2", \],
                \},
              \]
            \}
            ```
            Hope this helps!
        """.trimIndent()

        val sanitized = JsonPayloadParser.extractJsonObject(raw)
        assertNotNull(sanitized)
        val json = sanitized!!
        assertTrue(json.startsWith("{"))
        assertTrue(json.endsWith("}"))
        assertFalse(json.contains("\\{"))
        assertFalse(json.contains("\\}"))
        assertFalse(json.contains("\\["))
        assertFalse(json.contains("\\]"))
        assertFalse(json.contains("\\&"))
        assertFalse(json.contains("\\-"))
        assertFalse(json.contains(", ]"))
        assertFalse(Regex(",\\s*[}\\]]").containsMatchIn(json))
        assertTrue("Pretty-printed indentation/newlines should remain intact", json.contains("\n  \"title\":"))
    }

    @Test
    fun `extractJsonObject preserves pretty-printed JSON and escapes raw newlines inside strings`() {
        val rawPretty = """
            |```json
            |{
            |	"title": "Constitutional Amendments",
            |	"tags": [
            |		"Polity"
            |	],
            |	"mcqs": [
            |		{
            |			"q": "Consider the following statements:
            |1. Statement one.
            |2. Statement two.
            |
            |How many of the statements given above are correct?",
            |			"opts": [
            |				"Only one",
            |				"Only two",
            |				"All of the above",
            |				"None"
            |			],
            |			"ans": 0,
            |			"exp": "Statement 1 is correct.\nStatement 2 is incorrect."
            |		}
            |	]
            |}
            |```
        """.trimMargin()

        val sanitized = JsonPayloadParser.extractJsonObject(rawPretty)
        assertNotNull(sanitized)
        val json = sanitized!!
        assertTrue("Outer JSON tabs and newlines should remain readable", json.contains("\n\t\"title\":"))
        assertTrue("Raw newlines inside string values should be escaped as \\n", json.contains("statements:\\n1. Statement one.\\n2. Statement two.\\n\\nHow many"))
        assertTrue("Escaped \\n in explanation should be preserved", json.contains("Statement 1 is correct.\\nStatement 2 is incorrect."))
    }

    @Test
    fun `extractJsonObject preserves difficulty field in MCQ objects`() {
        val raw = """
            ```json
            {
              "title": "Parliamentary Privileges",
              "tags": ["Polity"],
              "mcqs": [
                {
                  "q": "Consider Article 105 of the Constitution.",
                  "opts": ["A", "B", "C", "D"],
                  "ans": 1,
                  "difficulty": "Hard",
                  "exp": "Detailed reasoning."
                }
              ]
            }
            ```
        """.trimIndent()
        val sanitized = JsonPayloadParser.extractJsonObject(raw)
        assertNotNull(sanitized)
        assertTrue(sanitized!!.contains("\"difficulty\": \"Hard\""))
    }
}
