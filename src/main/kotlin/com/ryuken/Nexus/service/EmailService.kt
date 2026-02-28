package com.ryuken.Nexus.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

@Service
class EmailService(
    private val mailSender: JavaMailSender,
    @Value("\${app.mail.from}") private val fromAddress: String
) {
    private val logger = LoggerFactory.getLogger(EmailService::class.java)

    @Async
    fun sendPasswordResetEmail(toEmail: String, resetLink: String) {
        try {
            val message = SimpleMailMessage()
            message.from = fromAddress
            message.setTo(toEmail)
            message.subject = "Reset your Nexus password"
            message.text = "Click the link to reset your password:\n\n$resetLink\n\nThis link expires in 1 hour."
            mailSender.send(message)
        } catch (ex: Exception) {
            logger.error("Failed to send password reset email to $toEmail", ex)
        }
    }

    @Async
    fun sendVerificationEmail(toEmail: String, verificationLink: String) {
        try {
            val message = SimpleMailMessage()
            message.from = fromAddress
            message.setTo(toEmail)
            message.subject = "Verify your Nexus account"
            message.text = "Click the link to verify your account:\n\n$verificationLink"
            mailSender.send(message)
        } catch (ex: Exception) {
            logger.error("Failed to send verification email to $toEmail", ex)
        }
    }
}

