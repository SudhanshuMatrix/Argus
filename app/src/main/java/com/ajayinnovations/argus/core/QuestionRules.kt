package com.ajayinnovations.argus.core

/**
 * Gemma reads, code decides. Combines the model's yes/no answers with RedFlags, which is more
 * reliable for links and phone numbers. Checked top to bottom; the first rule that fires wins.
 *
 *   money   = Q1                       code     = Q2
 *   link    = RedFlags link            contact  = Q3 or RedFlags phone number
 *   action  = money or link or contact
 *
 *   SCAM    code                                   ASKS_FOR_CODE
 *   SCAM    Q5 account problem and action          ACCOUNT_PROBLEM
 *   SCAM    Q6 prize/job/loan and action           TOO_GOOD_OFFER
 *   SCAM    Q4 claims official and (money, code or link)   FAKE_OFFICIAL
 *   CAREFUL money or link                          ASKS_FOR_MONEY_OR_LINK
 *   CAREFUL Q7 pressure and action                 PRESSURE
 *   NONE    otherwise (asks for nothing)
 */
object QuestionRules {

    data class Decision(val level: Level, val reason: ReasonCode?)

    fun decide(answers: Answers, redFlags: List<String>): Decision {
        val money = answers.asksMoney
        val code = answers.asksCode
        val link = "link" in redFlags
        val contact = answers.asksClickOrCall || "phone number" in redFlags
        val action = money || link || contact

        return when {
            code -> Decision(Level.SCAM, ReasonCode.ASKS_FOR_CODE)
            answers.accountProblem && action -> Decision(Level.SCAM, ReasonCode.ACCOUNT_PROBLEM)
            answers.promisesGain && action -> Decision(Level.SCAM, ReasonCode.TOO_GOOD_OFFER)
            answers.claimsOfficial && (money || code || link) -> Decision(Level.SCAM, ReasonCode.FAKE_OFFICIAL)
            money || link -> Decision(Level.CAREFUL, ReasonCode.ASKS_FOR_MONEY_OR_LINK)
            answers.pressure && action -> Decision(Level.CAREFUL, ReasonCode.PRESSURE)
            else -> Decision(Level.NONE, null)
        }
    }
}
