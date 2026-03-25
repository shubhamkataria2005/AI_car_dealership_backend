package com.Shubham.carDealership.controller;

import com.Shubham.carDealership.dto.MessageRequest;
import com.Shubham.carDealership.model.Message;
import com.Shubham.carDealership.model.User;
import com.Shubham.carDealership.repository.MessageRepository;
import com.Shubham.carDealership.repository.UserRepository;
import com.Shubham.carDealership.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Controller
public class MessageController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MessageService messageService;

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload MessageRequest messageRequest) {
        // Save message to database
        Message message = new Message();
        message.setSenderId(messageRequest.getSenderId());
        message.setReceiverId(messageRequest.getReceiverId());
        message.setCarId(messageRequest.getCarId());
        message.setContent(messageRequest.getContent());
        message.setIsRead(false);
        message.setCreatedAt(LocalDateTime.now());

        Message savedMessage = messageRepository.save(message);

        // Get sender info
        User sender = userRepository.findById(messageRequest.getSenderId()).orElse(null);

        // Create response object
        Map<String, Object> response = new HashMap<>();
        response.put("id", savedMessage.getId());
        response.put("senderId", savedMessage.getSenderId());
        response.put("senderName", sender != null ? sender.getUsername() : "Unknown");
        response.put("receiverId", savedMessage.getReceiverId());
        response.put("carId", savedMessage.getCarId());
        response.put("content", savedMessage.getContent());
        response.put("createdAt", savedMessage.getCreatedAt());
        response.put("isRead", savedMessage.getIsRead());

        // Send to receiver's personal queue
        messagingTemplate.convertAndSendToUser(
                messageRequest.getReceiverId().toString(),
                "/queue/messages",
                response
        );

        // Also send back to sender for confirmation
        messagingTemplate.convertAndSendToUser(
                messageRequest.getSenderId().toString(),
                "/queue/messages",
                response
        );
    }
}