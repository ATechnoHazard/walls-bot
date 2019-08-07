package me.msfjarvis.wallsbot

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.ivmg.telegram.Bot
import me.ivmg.telegram.bot
import me.ivmg.telegram.dispatch
import me.ivmg.telegram.dispatcher.command
import me.ivmg.telegram.entities.ChatAction
import me.ivmg.telegram.entities.ParseMode
import okhttp3.logging.HttpLoggingInterceptor
import org.dizitart.kno2.nitrite
import java.io.File
import java.text.DecimalFormat
import kotlin.coroutines.CoroutineContext
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.random.Random
import kotlin.system.exitProcess

class WallsBot : CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = Job() + Dispatchers.IO

    private val bot: Bot
    private var fileList: Array<File> = emptyArray()

    init {
        val props = AppProps()
        val db = nitrite {
            file = File(props.databaseFile)
            autoCommitBufferSize = 2048
            compress = true
            autoCompact = false
        }
        val repository = db.getRepository(props.botToken, CachedFile::class.java)
        fileList = requireNotNull(File(props.searchDir).listFiles())
        bot = bot {
            token = props.botToken
            timeout = 30
            logLevel = if (props.debug) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
            dispatch {
                command("pic") { bot, update, args ->
                    launch {
                        if (args.isEmpty()) {
                            update.message?.let { message ->
                                bot.sendChatAction(chatId = message.chat.id, action = ChatAction.TYPING)
                                bot.sendMessage(
                                        chatId = message.chat.id,
                                        text = "No arguments supplied!",
                                        replyToMessageId = message.messageId
                                )
                            }
                            return@launch
                        }
                        val fileList = fileList.filter { file ->
                            file.nameWithoutExtension.startsWith(args.joinToString("_"))
                        }
                        if (fileList.isEmpty()) {
                            update.message?.let { message ->
                                bot.sendChatAction(chatId = message.chat.id, action = ChatAction.TYPING)
                                bot.sendMessage(
                                        chatId = message.chat.id,
                                        text = "No files found for \'${args.joinToString(" ")}\'",
                                        replyToMessageId = message.messageId
                                )
                                return@launch
                            }
                        }
                        val randIdx = Random.nextInt(0, fileList.size)
                        val fileToSend = fileList[randIdx]
                        update.message?.let { message ->
                            bot.sendPictureSafe(
                                    repository,
                                    message.chat.id,
                                    props.baseUrl,
                                    fileToSend,
                                    message.messageId,
                                    genericCaption = props.genericCaption
                            )
                        }
                    }
                }

                if (props.ownerId != null) {
                    command("quit") { bot, update, _ ->
                        update.message?.let { message ->
                            val messageFrom: Long = message.from?.id ?: 0
                            if (messageFrom != props.ownerId) {
                                bot.sendChatAction(chatId = message.chat.id, action = ChatAction.TYPING)
                                bot.sendMessage(
                                        chatId = message.chat.id,
                                        text = "Unauthorized!",
                                        replyToMessageId = message.messageId
                                )
                                return@command
                            } else {
                                bot.sendMessage(
                                        chatId = message.chat.id,
                                        text = "Going down!",
                                        replyToMessageId = message.messageId
                                )
                            }
                            coroutineContext.cancelChildren()
                            db.commit()
                            db.close()
                            exitProcess(0)
                        }
                    }
                }

                command("random") { bot, update ->
                    launch {
                        val randomInt = Random.nextInt(0, fileList.size)
                        val fileToSend = fileList[randomInt]
                        update.message?.let { message ->
                            bot.sendPictureSafe(
                                    repository,
                                    message.chat.id,
                                    props.baseUrl,
                                    fileToSend,
                                    message.messageId,
                                    genericCaption = props.genericCaption
                            )
                        }
                    }
                }

                command("search") { bot, update, args ->
                    launch {
                        if (args.isEmpty()) {
                            update.message?.let { message ->
                                bot.sendChatAction(chatId = message.chat.id, action = ChatAction.TYPING)
                                bot.sendMessage(
                                        chatId = message.chat.id,
                                        text = "No arguments supplied!",
                                        replyToMessageId = message.messageId
                                )
                            }
                            return@launch
                        }
                        val foundFiles = HashSet<String>()
                        fileList.forEach { file ->
                            if (file.name.startsWith(args.joinToString("_"))) {
                                foundFiles.add("[${file.sanitizedName}](${props.baseUrl}/${file.name})")
                            }
                        }
                        update.message?.let { message ->
                            bot.sendChatAction(chatId = message.chat.id, action = ChatAction.TYPING)
                            bot.sendMessage(
                                    chatId = message.chat.id,
                                    replyToMessageId = message.messageId,
                                    text = foundFiles.joinToString("\n"),
                                    parseMode = ParseMode.MARKDOWN,
                                    disableWebPagePreview = true
                            )
                        }
                    }
                }

                if (props.ownerId != null) {
                    command("stats") { bot, update ->
                        launch {
                            update.message?.let { message ->
                                val messageFrom: Long = message.from?.id ?: 0
                                if (messageFrom != props.ownerId) {
                                    bot.sendChatAction(chatId = message.chat.id, action = ChatAction.TYPING)
                                    bot.sendMessage(
                                            chatId = message.chat.id,
                                            text = "Unauthorized!",
                                            replyToMessageId = message.messageId
                                    )
                                    return@launch
                                }
                            }
                            var diskSpace: Long = 0
                            for (file in fileList) {
                                diskSpace += file.length()
                            }
                            val units = arrayOf("B", "KB", "MB", "GB", "TB")
                            val digitGroups: Double = floor((log10(diskSpace.toDouble()) / log10(1024.0)))
                            val decimalFormat = DecimalFormat("#,##0.##")
                                    .format(diskSpace / 1024.0.pow(digitGroups)) + " " + units[digitGroups.toInt()]
                            update.message?.let { message ->
                                bot.sendChatAction(chatId = message.chat.id, action = ChatAction.TYPING)
                                bot.sendMessage(
                                        chatId = message.chat.id,
                                        text = "Total files : ${fileList.size} \nDisk space used : $decimalFormat",
                                        replyToMessageId = message.messageId
                                )
                            }
                        }
                    }

                    command("update") { bot, update, _ ->
                        runBlocking(coroutineContext) {
                            coroutineContext.cancelChildren()
                            fileList = requireNotNull(File(props.searchDir).listFiles())
                            update.message?.let { message ->
                                bot.sendChatAction(chatId = message.chat.id, action = ChatAction.TYPING)
                                bot.sendMessage(
                                        chatId = message.chat.id,
                                        text = "Updated files list!",
                                        replyToMessageId = message.messageId
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    fun startPolling() {
        bot.startPolling()
    }
}
