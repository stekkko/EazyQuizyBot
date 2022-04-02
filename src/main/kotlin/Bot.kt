import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.text
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.TelegramFile
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import com.github.kotlintelegrambot.logging.LogLevel
import com.github.kotlintelegrambot.network.fold
import model.Question
import model.Quiz
import model.Variant
import java.io.File
import kotlin.random.Random

class Bot {
    val bot = bot {

        token = BotConfig.BOT_API_KEY
        timeout = 30
        logLevel = LogLevel.All()

        var quiz = Quiz(emptyList(), 0)

        var titleId: Long? = null
        var photoId: Long? = null

        dispatch {
            command("start") {
                val sent = bot.sendMessage(
                    chatId = ChatId.fromId(message.chat.id),
                    text = "Welcome to quiz!\n" +
                            "/quiz <id> - to start quiz with specific id\n" +
                            "/add - to add new quiz with random id"
                )
                sent.fold({
                    titleId = it?.result?.messageId
                })
            }

            command("add") {
                val sent = bot.sendMessage(
                    chatId = ChatId.fromId(message.chat.id),
                    text = "Type your new quiz right here!\n" +
                            "?q <first question>\n" +
                            "?url <picture url> (optional)\n" +
                            "- <wrong answer>\n" +
                            "+ <right answer>\n" +
                            "?q <second question>\n" +
                            "..."
                )
                sent.fold({
                    titleId = it?.result?.messageId
                })
            }

            text {
                val parsedQuiz = QuizParser().parseQuizFromText(this.text)
                if (parsedQuiz.isOver) {
                    bot.sendMessage(
                        chatId = ChatId.fromId(message.chat.id),
                        text = "Wrong quiz format")
                } else {
                    val randomId = Random.nextInt(1000000)
                    val path = "${BotConfig.FILE_PATH}$randomId"
                    File(path).writeText(this.text)
                    bot.sendMessage(
                        chatId = ChatId.fromId(message.chat.id),
                        text = "Your quiz is ready id=${randomId}"
                    )
                }
            }

            command("quiz") {
                if (args.size != 1) {
                    bot.sendMessage(
                        chatId = ChatId.fromId(message.chat.id),
                        text = "You need to type /quiz <id>, for example /quiz 123456"
                    )
                }
                quiz = QuizParser().loadQuiz(args[0].toInt())
                val q = quiz.nextQuestion()
                q.picture?.let { url ->
                    bot.sendPhoto(chatId = ChatId.fromId(message.chat.id), TelegramFile.ByUrl(url))
                        .also { it.fold({ response -> photoId = response?.result?.messageId}) }
                }
                bot.sendMessage(
                    chatId = ChatId.fromId(message.chat.id),
                    text = q.question,
                    replyMarkup = createVariantButtons(q)
                ).also { it.fold({ response -> titleId = response?.result?.messageId}) }
            }

            callbackQuery("wrongAnswer") {
                val chatId = callbackQuery.message?.chat?.id ?: return@callbackQuery
                titleId?.let { bot.deleteMessage(ChatId.fromId(chatId), it) }
                photoId?.let { bot.deleteMessage(ChatId.fromId(chatId), it) }

                val q = quiz.nextQuestion()
                q.picture?.let { url ->
                    bot.sendPhoto(chatId = ChatId.fromId(chatId), photo = TelegramFile.ByUrl(url))
                        .also { it.fold({ response -> photoId = response?.result?.messageId}) }
                }
                bot.sendMessage(
                    chatId = ChatId.fromId(chatId),
                    text = q.question,
                    replyMarkup = createVariantButtons(q)
                ).also { it.fold({ response -> titleId = response?.result?.messageId}) }
            }

            callbackQuery("rightAnswer") {
                quiz.score++
                val chatId = callbackQuery.message?.chat?.id ?: return@callbackQuery
                titleId?.let { bot.deleteMessage(ChatId.fromId(chatId), it) }
                photoId?.let { bot.deleteMessage(ChatId.fromId(chatId), it) }

                val q = quiz.nextQuestion()
                q.picture?.let { url ->
                    bot.sendPhoto(chatId = ChatId.fromId(chatId), photo = TelegramFile.ByUrl(url))
                        .also { it.fold({ response -> photoId = response?.result?.messageId}) }
                }
                bot.sendMessage(
                    chatId = ChatId.fromId(chatId),
                    text = q.question,
                    replyMarkup = createVariantButtons(q)
                ).also { it.fold({ response -> titleId = response?.result?.messageId}) }
            }

            callbackQuery(
                callbackData = "showAlert",
                callbackAnswerText = "Ошибка",
                callbackAnswerShowAlert = true
            ) {
                val chatId = callbackQuery.message?.chat?.id ?: return@callbackQuery
                bot.sendMessage(ChatId.fromId(chatId), callbackQuery.data)
            }
        }
    }

    private fun createVariantButtons(q: Question) =
        InlineKeyboardMarkup.create(
            q.variants.map {
                when (it) {
                    is Variant.Wrong ->
                        InlineKeyboardButton.CallbackData(text = it.value, callbackData = "wrongAnswer")
                    is Variant.Right ->
                        InlineKeyboardButton.CallbackData(text = it.value, callbackData = "rightAnswer")
                }
            }.chunked(2)
        )
}

