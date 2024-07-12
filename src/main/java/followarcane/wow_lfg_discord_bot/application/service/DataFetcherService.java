package followarcane.wow_lfg_discord_bot.application.service;

import followarcane.wow_lfg_discord_bot.domain.model.Message;
import followarcane.wow_lfg_discord_bot.domain.repository.MessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class DataFetcherService {

    private final DiscordBotService discordBotService;
    private final MessageRepository messageRepository;

    private final RestTemplate restTemplate = new RestTemplate();
    private final String dataCrawlerUrl = "http://your-data-crawler-service-url/data";

    @Autowired
    public DataFetcherService(DiscordBotService discordBotService, MessageRepository messageRepository) {
        this.discordBotService = discordBotService;
        this.messageRepository = messageRepository;
    }

    @Scheduled(fixedRate = 30000)
    public void fetchData() {
        String data = restTemplate.getForObject(dataCrawlerUrl, String.class);
        if (data != null && !data.isEmpty()) {
            discordBotService.sendMessageToChannel("your-discord-channel-id", data);

            Message message = new Message();
            message.setMessageChannelId("your-discord-channel-id");
            message.setMessageContent(data);
            message.setTimestamp(System.currentTimeMillis());

            messageRepository.save(message);
        }
    }
}