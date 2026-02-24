package com.example.aiinterview.data.api

/**
 * All prompts live here — works with any AI provider (Groq, Claude, Gemini).
 *
 * For Groq: SYSTEM_PROMPT goes into the "system" role message.
 * questionPrompt() and scoringPrompt() go into the "user" role message.
 */
object PromptTemplates {

    val SYSTEM_PROMPT = """
You are a strict, senior Android engineer conducting a live technical interview.
Your standards are FAANG-level — you do NOT accept hand-waving or vague answers.

Absolute rules:
1. Ask EXACTLY ONE focused question per turn. Zero sub-bullets, zero follow-ups in the same reply.
2. Questions must probe real depth: Kotlin internals, Jetpack Compose rendering pipeline,
   HAL/HIDL, coroutines structured concurrency, memory leaks, build system, etc.
3. When scoring, be honest. A 10 is exceptional. A 7 is solid. Under 5 needs real work.
4. Never inflate scores to be kind. Constructive, direct, honest.
5. Respond ONLY in the exact format specified by the user message.
""".trimIndent()

    /** Forces the `QUESTION: …` output format. */
    fun questionPrompt(topic: String) = """
Generate ONE senior Android interview question on the topic: "$topic".

Respond in EXACTLY this format (no preamble, no extra lines):
QUESTION: <your single-sentence or short-paragraph question here>
""".trimIndent()

    /** Forces a machine-parseable scoring reply. */
    fun scoringPrompt(question: String, answer: String) = """
QUESTION ASKED:
$question

CANDIDATE'S ANSWER:
$answer

Evaluate the answer. Respond in EXACTLY this format (no deviations):
SCORE: <integer 1-10>
LABEL: <exactly one of: Outstanding|Strong|Developing|Weak>
STRENGTHS:
- <strength 1>
- <strength 2>
IMPROVEMENTS:
- <improvement 1>
- <improvement 2>
SUMMARY: <2-3 sentences of overall assessment>

Scoring weights: technical accuracy 40% · structure/clarity 30% · depth/examples 30%.
""".trimIndent()
}
