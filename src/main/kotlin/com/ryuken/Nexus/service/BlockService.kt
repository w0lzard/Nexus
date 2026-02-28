package com.ryuken.Nexus.service

import com.ryuken.Nexus.database.repository.BlockRepository
import com.ryuken.Nexus.database.repository.UserRepository
import com.ryuken.Nexus.model.Block
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class BlockService(
    private val blockRepository: BlockRepository,
    private val userRepository: UserRepository
) {

    @Transactional
    fun blockUser(blockerUsername: String, blockedId: UUID) {
        val blocker = userRepository.findByUsername(blockerUsername)
            ?: throw IllegalArgumentException("User not found")
        val blocked = userRepository.findById(blockedId)
            .orElseThrow { IllegalArgumentException("User to block not found") }
        if (blocker.id == blocked.id) throw IllegalArgumentException("Cannot block yourself")
        if (blockRepository.existsByBlockerAndBlocked(blocker, blocked)) return
        blockRepository.save(Block(blocker = blocker, blocked = blocked))
    }

    @Transactional
    fun unblockUser(blockerUsername: String, blockedId: UUID) {
        val blocker = userRepository.findByUsername(blockerUsername)
            ?: throw IllegalArgumentException("User not found")
        val blocked = userRepository.findById(blockedId)
            .orElseThrow { IllegalArgumentException("User not found") }
        val block = blockRepository.findByBlockerAndBlocked(blocker, blocked)
            ?: throw IllegalArgumentException("Not blocking this user")
        blockRepository.delete(block)
    }

    fun isBlocked(blockerUsername: String, blockedId: UUID): Boolean {
        val blocker = userRepository.findByUsername(blockerUsername) ?: return false
        val blocked = userRepository.findById(blockedId).orElse(null) ?: return false
        return blockRepository.existsByBlockerAndBlocked(blocker, blocked)
    }
}

