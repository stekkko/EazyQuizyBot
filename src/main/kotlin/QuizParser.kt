import model.Question
import model.Quiz
import java.io.File

class QuizParser {

    fun loadQuiz(id: Int): Quiz {
        val questionList: MutableList<Question> = mutableListOf()
        var question: Question? = null
        val inputStream = File("${BotConfig.FILE_PATH}$id").inputStream()
        inputStream.bufferedReader().forEachLine {
            val type = it.split(" ").first()
            val value = it.substring(type.length + 1)
            when (type) {
                "?q" -> {
                    question?.let { q -> questionList.add(q) }
                    question = Question(value)
                }
                "-" -> {
                    question?.addVariant(value, isRight = false)
                }
                "+" -> {
                    question?.addVariant(value, isRight = true)
                }
                "?url" -> {
                    question?.picture = value
                }
                else -> error("Fail")
            }
        }
        question?.let { q -> questionList.add(q) }
        inputStream.close()
        return Quiz(questionList, id)
    }

    fun parseQuizFromText(s: String): Quiz {
        val questionList: MutableList<Question> = mutableListOf()
        var question: Question? = null
        s.split("\n").forEach {
            val type = it.split(" ").first()
            val value = it.substring(type.length + 1)
            when (type) {
                "?q" -> {
                    question?.let { q -> questionList.add(q) }
                    question = Question(value)
                }
                "-" -> {
                    question?.addVariant(value, isRight = false)
                }
                "+" -> {
                    question?.addVariant(value, isRight = true)
                }
                "?url" -> {
                    question?.picture = value
                }
                else -> error("Fail")
            }
        }
        question?.let { q -> questionList.add(q) }
        return Quiz(questionList, -1)
    }
}