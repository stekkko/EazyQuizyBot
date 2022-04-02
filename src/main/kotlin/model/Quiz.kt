package model


class Quiz(private val questionList: List<Question>, private val quizId: Int) {

    var score = 0
    val isOver get() = next >= questionList.size
    private var next = 0

    fun nextQuestion(): Question {
        return if (next < questionList.size) {
            questionList[next++]
        } else {
            Question("Quiz $quizId | Ваш результат: $score/${questionList.size} | ${100 * score / questionList.size}%")
        }
    }
}