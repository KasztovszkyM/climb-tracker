package bme.prompteng.android.climbtracker.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "chat_conversations")
data class ChatConversationEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "chat_messages",
    foreignKeys = [
        ForeignKey(
            entity = ChatConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ChatMessageEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val conversationId: String,
    val isUser: Boolean,
    val text: String?,
    val imagePath: String?,
    val isRouteBeta: Boolean,
    val holdsJson: String?,
    val imageAspectRatio: Float?,
    val betaDescription: String?,
    val timestamp: Long = System.currentTimeMillis()
)
