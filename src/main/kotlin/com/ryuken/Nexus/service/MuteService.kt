package com.ryuken.Nexus.service

import com.ryuken.Nexus.database.repository.MuteRepository
import com.ryuken.Nexus.database.repository.UserRepository
import com.ryuken.Nexus.model.Mute
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class MuteService(
    private val muteRepository: MuteRepository,
    private val userRepository: UserRepository
) {

    @Transactional
    fun muteUser(muterUsername: String, mutedId: UUID) {
        val muter = userRepository.findByUsername(muterUsername)
            ?: throw IllegalArgumentException("User not found")
        val muted = userRepository.findById(mutedId)
            .orElseThrow { IllegalArgumentException("User to mute not found") }
        if (muter.id == muted.id) throw IllegalArgumentException("Cannot mute yourself")
        if (muteRepository.existsByMuterAndMuted(muter, muted)) return
        muteRepository.save(Mute(muter = muter, muted = muted))
    }

    @Transactional
    fun unmuteUser(muterUsername: String, mutedId: UUID) {
        val muter = userRepository.findByUsername(muterUsername)
            ?: throw IllegalArgumentException("User not found")
        val muted = userRepository.findById(mutedId)
            .orElseThrow { IllegalArgumentException("User not found") }
        val mute = muteRepository.findByMuterAndMuted(muter, muted)
            ?: throw IllegalArgumentException("Not muting this user")
        muteRepository.delete(mute)
    }
}

