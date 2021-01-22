/*
 * Copyright 2016 LINE Corporation
 *
 * LINE Corporation licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.example.bot.spring;

import static java.util.Collections.singletonList;

import java.io.*;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.google.common.base.CharMatcher;
import com.linecorp.bot.model.message.flex.component.Text;
import com.linecorp.bot.model.message.flex.container.Carousel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.google.common.io.ByteStreams;

import com.linecorp.bot.client.LineBlobClient;
import com.linecorp.bot.client.LineMessagingClient;
import com.linecorp.bot.client.MessageContentResponse;
import com.linecorp.bot.model.ReplyMessage;
import com.linecorp.bot.model.action.DatetimePickerAction;
import com.linecorp.bot.model.action.MessageAction;
import com.linecorp.bot.model.action.PostbackAction;
import com.linecorp.bot.model.action.URIAction;
import com.linecorp.bot.model.event.BeaconEvent;
import com.linecorp.bot.model.event.Event;
import com.linecorp.bot.model.event.FollowEvent;
import com.linecorp.bot.model.event.JoinEvent;
import com.linecorp.bot.model.event.MemberJoinedEvent;
import com.linecorp.bot.model.event.MemberLeftEvent;
import com.linecorp.bot.model.event.MessageEvent;
import com.linecorp.bot.model.event.PostbackEvent;
import com.linecorp.bot.model.event.UnfollowEvent;
import com.linecorp.bot.model.event.UnknownEvent;
import com.linecorp.bot.model.event.UnsendEvent;
import com.linecorp.bot.model.event.VideoPlayCompleteEvent;
import com.linecorp.bot.model.event.message.AudioMessageContent;
import com.linecorp.bot.model.event.message.ContentProvider;
import com.linecorp.bot.model.event.message.FileMessageContent;
import com.linecorp.bot.model.event.message.ImageMessageContent;
import com.linecorp.bot.model.event.message.LocationMessageContent;
import com.linecorp.bot.model.event.message.StickerMessageContent;
import com.linecorp.bot.model.event.message.TextMessageContent;
import com.linecorp.bot.model.event.message.VideoMessageContent;
import com.linecorp.bot.model.event.source.GroupSource;
import com.linecorp.bot.model.event.source.RoomSource;
import com.linecorp.bot.model.event.source.Source;
import com.linecorp.bot.model.group.GroupMemberCountResponse;
import com.linecorp.bot.model.group.GroupSummaryResponse;
import com.linecorp.bot.model.message.AudioMessage;
import com.linecorp.bot.model.message.ImageMessage;
import com.linecorp.bot.model.message.ImagemapMessage;
import com.linecorp.bot.model.message.LocationMessage;
import com.linecorp.bot.model.message.Message;
import com.linecorp.bot.model.message.StickerMessage;
import com.linecorp.bot.model.message.TemplateMessage;
import com.linecorp.bot.model.message.TextMessage;
import com.linecorp.bot.model.message.VideoMessage;
import com.linecorp.bot.model.message.imagemap.ImagemapArea;
import com.linecorp.bot.model.message.imagemap.ImagemapBaseSize;
import com.linecorp.bot.model.message.imagemap.ImagemapExternalLink;
import com.linecorp.bot.model.message.imagemap.ImagemapVideo;
import com.linecorp.bot.model.message.imagemap.MessageImagemapAction;
import com.linecorp.bot.model.message.imagemap.URIImagemapAction;
import com.linecorp.bot.model.message.sender.Sender;
import com.linecorp.bot.model.message.template.ButtonsTemplate;
import com.linecorp.bot.model.message.template.CarouselColumn;
import com.linecorp.bot.model.message.template.CarouselTemplate;
import com.linecorp.bot.model.message.template.ConfirmTemplate;
import com.linecorp.bot.model.message.template.ImageCarouselColumn;
import com.linecorp.bot.model.message.template.ImageCarouselTemplate;
import com.linecorp.bot.model.response.BotApiResponse;
import com.linecorp.bot.model.room.RoomMemberCountResponse;
import com.linecorp.bot.spring.boot.annotation.EventMapping;
import com.linecorp.bot.spring.boot.annotation.LineMessageHandler;

import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@LineMessageHandler
public class KitchenSinkController {
    @Autowired
    private LineMessagingClient lineMessagingClient;

    @Autowired
    private LineBlobClient lineBlobClient;

    @EventMapping
    public void handleTextMessageEvent(MessageEvent<TextMessageContent> event) throws Exception {
        TextMessageContent message = event.getMessage();
        String userId = event.getSource().getUserId();
        String usrMode = UserAIMode.getAIMode().checkAImap(userId);
        if(usrMode == null){
            handleTextContent(event.getReplyToken(), event, message);
        }
        else if (usrMode.equals("ChatBot")){
            handleTextContentChatBot(event.getReplyToken(), event, message);
        }
        else if (usrMode.equals("TranslationBot")){
            handleTextContentTranslationBot(event.getReplyToken(), event, message);
        }

    }



    @EventMapping
    public void handleStickerMessageEvent(MessageEvent<StickerMessageContent> event) {
        handleSticker(event.getReplyToken(), event.getMessage());
    }

    @EventMapping
    public void handleLocationMessageEvent(MessageEvent<LocationMessageContent> event) {
        LocationMessageContent locationMessage = event.getMessage();
        reply(event.getReplyToken(), new LocationMessage(
                locationMessage.getTitle(),
                locationMessage.getAddress(),
                locationMessage.getLatitude(),
                locationMessage.getLongitude()
        ));
    }

    @EventMapping
    public void handleImageMessageEvent(MessageEvent<ImageMessageContent> event) throws IOException {
        // You need to install ImageMagick
        handleHeavyContent(
                event.getReplyToken(),
                event.getMessage().getId(),
                responseBody -> {
                    final ContentProvider provider = event.getMessage().getContentProvider();
                    final DownloadedContent jpg;
                    final DownloadedContent previewImg;
                    if (provider.isExternal()) {
                        jpg = new DownloadedContent(null, provider.getOriginalContentUrl());
                        previewImg = new DownloadedContent(null, provider.getPreviewImageUrl());
                    } else {
                        jpg = saveContent("jpg", responseBody);
                        previewImg = createTempFile("jpg");
                        system(
                                "convert",
                                "-resize", "240x",
                                jpg.path.toString(),
                                previewImg.path.toString());
                    }
                    reply(event.getReplyToken(),
                          new ImageMessage(jpg.getUri(), previewImg.getUri()));
                });
    }

    @EventMapping
    public void handleAudioMessageEvent(MessageEvent<AudioMessageContent> event) throws IOException {
        handleHeavyContent(
                event.getReplyToken(),
                event.getMessage().getId(),
                responseBody -> {
                    final ContentProvider provider = event.getMessage().getContentProvider();
                    final DownloadedContent mp4;
                    if (provider.isExternal()) {
                        mp4 = new DownloadedContent(null, provider.getOriginalContentUrl());
                    } else {
                        mp4 = saveContent("mp4", responseBody);
                    }
                    reply(event.getReplyToken(), new AudioMessage(mp4.getUri(), 100));
                });
    }

    @EventMapping
    public void handleVideoMessageEvent(MessageEvent<VideoMessageContent> event) throws IOException {
        log.info("Got video message: duration={}ms", event.getMessage().getDuration());

        // You need to install ffmpeg and ImageMagick.
        handleHeavyContent(
                event.getReplyToken(),
                event.getMessage().getId(),
                responseBody -> {
                    final ContentProvider provider = event.getMessage().getContentProvider();
                    final DownloadedContent mp4;
                    final DownloadedContent previewImg;
                    if (provider.isExternal()) {
                        mp4 = new DownloadedContent(null, provider.getOriginalContentUrl());
                        previewImg = new DownloadedContent(null, provider.getPreviewImageUrl());
                    } else {
                        mp4 = saveContent("mp4", responseBody);
                        previewImg = createTempFile("jpg");
                        system("convert",
                               mp4.path + "[0]",
                               previewImg.path.toString());
                    }
                    String trackingId = UUID.randomUUID().toString();
                    log.info("Sending video message with trackingId={}", trackingId);
                    reply(event.getReplyToken(),
                          VideoMessage.builder()
                                      .originalContentUrl(mp4.getUri())
                                      .previewImageUrl(previewImg.uri)
                                      .trackingId(trackingId)
                                      .build());
                });
    }

    @EventMapping
    public void handleVideoPlayCompleteEvent(VideoPlayCompleteEvent event) throws IOException {
        log.info("Got video play complete: tracking id={}", event.getVideoPlayComplete().getTrackingId());
        this.replyText(event.getReplyToken(),
                       "You played " + event.getVideoPlayComplete().getTrackingId());
    }

    @EventMapping
    public void handleFileMessageEvent(MessageEvent<FileMessageContent> event) {
        this.reply(event.getReplyToken(),
                   new TextMessage(String.format("Received '%s'(%d bytes)",
                                                 event.getMessage().getFileName(),
                                                 event.getMessage().getFileSize())));
    }

    @EventMapping
    public void handleUnfollowEvent(UnfollowEvent event) {
        log.info("unfollowed this bot: {}", event);
    }

    @EventMapping
    public void handleUnknownEvent(UnknownEvent event) {
        log.info("Got an unknown event!!!!! : {}", event);
    }

    @EventMapping
    public void handleFollowEvent(FollowEvent event) {
        String replyToken = event.getReplyToken();
        this.replyText(replyToken, "您好!\n歡迎您加入Jim Wang的聊天!\n\n如果想了解使用說明請點及下方url或點選右下方使用教學哦!" +
                "\nhttps://hackmd.io/@GDxABCY6RJydRN7UbLYgHw/SkxqoASkO");
    }

    @EventMapping
    public void handleJoinEvent(JoinEvent event) {
        String replyToken = event.getReplyToken();
        this.replyText(replyToken, "Joined " + event.getSource());
    }

    @EventMapping
    public void handlePostbackEvent(PostbackEvent event) throws Exception {
        String replyToken = event.getReplyToken();
        handlePostbackContent(replyToken, event, event.getPostbackContent().getData());
        //this.replyText(replyToken,
        //               "Got postback data " + event.getPostbackContent().getData() + ", param " + event
        //                       .getPostbackContent().getParams().toString());
    }

    @EventMapping
    public void handleBeaconEvent(BeaconEvent event) {
        String replyToken = event.getReplyToken();
        this.replyText(replyToken, "Got beacon message " + event.getBeacon().getHwid());
    }

    @EventMapping
    public void handleMemberJoined(MemberJoinedEvent event) {
        String replyToken = event.getReplyToken();
        this.replyText(replyToken, "Got memberJoined message " + event.getJoined().getMembers()
                                                                      .stream().map(Source::getUserId)
                                                                      .collect(Collectors.joining(",")));
    }

    @EventMapping
    public void handleMemberLeft(MemberLeftEvent event) {
        log.info("Got memberLeft message: {}", event.getLeft().getMembers()
                                                    .stream().map(Source::getUserId)
                                                    .collect(Collectors.joining(",")));
    }

    @EventMapping
    public void handleMemberLeft(UnsendEvent event) {
        log.info("Got unsend event: {}", event);
    }

    @EventMapping
    public void handleOtherEvent(Event event) {
        log.info("Received message(Ignored): {}", event);
    }

    private void reply(@NonNull String replyToken, @NonNull Message message) {
        reply(replyToken, singletonList(message));
    }

    private void reply(@NonNull String replyToken, @NonNull List<Message> messages) {
        reply(replyToken, messages, false);
    }

    private void reply(@NonNull String replyToken,
                       @NonNull List<Message> messages,
                       boolean notificationDisabled) {
        try {
            BotApiResponse apiResponse = lineMessagingClient
                    .replyMessage(new ReplyMessage(replyToken, messages, notificationDisabled))
                    .get();
            log.info("Sent messages: {}", apiResponse);
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private void replyText(@NonNull String replyToken, @NonNull String message) {
        if (replyToken.isEmpty()) {
            throw new IllegalArgumentException("replyToken must not be empty");
        }
        if (message.length() > 1000) {
            message = message.substring(0, 1000 - 2) + "……";
        }
        this.reply(replyToken, new TextMessage(message));
    }

    private void handleHeavyContent(String replyToken, String messageId,
                                    Consumer<MessageContentResponse> messageConsumer) {
        final MessageContentResponse response;
        try {
            response = lineBlobClient.getMessageContent(messageId)
                                     .get();
        } catch (InterruptedException | ExecutionException e) {
            reply(replyToken, new TextMessage("Cannot get image: " + e.getMessage()));
            throw new RuntimeException(e);
        }
        messageConsumer.accept(response);
    }

    private void handleSticker(String replyToken, StickerMessageContent content) {
        reply(replyToken, new StickerMessage(
                content.getPackageId(), content.getStickerId())
        );
    }

    private void handlePostbackContent(String replyToken, Event event, String data) throws Exception {
        PostbackData postbackData = new PostbackData(data.split("=|&"));
        switch(postbackData.getMapping("action")) {
            case "AI": {
                String userId = event.getSource().getUserId();
                lineMessagingClient.linkRichMenuIdToUser(userId, "richmenu-4a1a432c2ef37d07d0d51b9bf641cfba");
                //this.replyText(replyToken, "change rich menu !!");

                break;
            }
            case "back": {
                String userId = event.getSource().getUserId();
                lineMessagingClient.linkRichMenuIdToUser(userId, "richmenu-167632482efa68b9c149b9c3feb4a9eb");
                //this.replyText(replyToken, "change rich menu !!");
                break;
            }
            case "chat": {
                String userId = event.getSource().getUserId();
                UserAIMode.getAIMode().addAIMap(userId, "ChatBot");
                this.replyText(replyToken, "已切換至 AI 聊天模式!!目前僅支援英文聊天哦~\n\n若要回到一般模式，請打\"quit\"!");
                break;
            }
            case "translation": {
                String userId = event.getSource().getUserId();
                UserAIMode.getAIMode().addAIMap(userId, "TranslationBot");
                this.replyText(replyToken, "已切換至 AI 翻譯模式!!目前僅支援英翻中哦~\n\n若要回到一般模式，請打\"quit\"!");
                break;
            }
        }

        return;
    }


    private void handleTextContent(String replyToken, Event event, TextMessageContent content)
            throws Exception {
        final String text = content.getText();

        log.info("Got text message from replyToken:{}: text:{} emojis:{}", replyToken, text,
                 content.getEmojis());
        switch (text) {
            case "聊天機器人": {
                ConfirmTemplate confirmTemplate = new ConfirmTemplate(
                        "確定要切換成聊天機器人嗎?",
                        Arrays.asList(
                                new PostbackAction("確定", "action=chat"),
                                new URIAction("使用說明", URI.create("https://hackmd.io/@GDxABCY6RJydRN7UbLYgHw/SkxqoASkO"), null)
                        )
                );
                TemplateMessage templateMessage = new TemplateMessage("確定要切換成聊天機器人嗎?", confirmTemplate);
                this.reply(replyToken, templateMessage);
                break;
            }

            case "翻譯機器人": {
                ConfirmTemplate confirmTemplate = new ConfirmTemplate(
                        "確定要切換成翻譯機器人嗎?",
                        Arrays.asList(
                                new PostbackAction("確定", "action=translation"),
                                new URIAction("使用說明", URI.create("https://hackmd.io/@GDxABCY6RJydRN7UbLYgHw/SkxqoASkO"), null)
                        )
                );
                TemplateMessage templateMessage = new TemplateMessage("確定要切換成翻譯機器人嗎?", confirmTemplate);
                this.reply(replyToken, templateMessage);
                break;
            }

            case "關於我": {
                URI imageUrl = createUri("/static/buttons/aboutme.png");
                ButtonsTemplate buttonsTemplate = new ButtonsTemplate(
                        imageUrl,
                        "關於Jim Wang",
                        "你想知道些什麼?",
                        Arrays.asList(
                                new MessageAction("個人簡歷",
                                                   "個人簡歷"),
                                new MessageAction("興趣與專長",
                                                   "興趣與專長"),
                                new MessageAction("人格特質",
                                                  "人格特質")
                        ));
                TemplateMessage templateMessage = new TemplateMessage("關於我", buttonsTemplate);
                this.reply(replyToken, templateMessage);
                break;
            }

            case "個人簡歷": {
                URI imageUrl = createUri("/static/buttons/resume.png");
                String resumeString = "$您好! 我叫做王俊翔，也可以叫我Jim，很高興能認識你!\n\n" +
                        "我目前22歲，就讀於台大資工系四年級，即將進入台大網媒所繼續深耕~\n\n" +
                        "此外，我目前也在一家新創公司實習以增加自己的實務經驗!\n\n" +
                        "如果想知道更多，歡迎點選下方選單~";

                TextMessage resume = TextMessage.builder()
                        .text(resumeString)
                        .emojis(
                                Arrays.asList(
                                    TextMessage.Emoji.builder().index(0).productId("5ac1bfd5040ab15980c9b435").emojiId("011").build()
                                )
                        )
                        .build();

                ButtonsTemplate buttonsTemplate = new ButtonsTemplate(
                        imageUrl,
                        "Jim Wang 的個人簡歷",
                        "你想知道些什麼?",
                        Arrays.asList(
                                new MessageAction("學校專題",
                                        "學校專題"),
                                new MessageAction("工作經驗",
                                        "工作經驗")
                        ));
                TemplateMessage templateMessage = new TemplateMessage("個人簡歷", buttonsTemplate);


                this.reply(replyToken, Arrays.asList(
                        resume,
                        templateMessage
                ));
                break;
            }

            case "學校專題": {
                URI imageUrl = createUri("/static/buttons/resume.png");
                String resumeString = "這四年中，我曾經參與過許多的專題，研究內容廣泛，包含影像、資安、機器學習及Android App。\n\n" +
                        "目前較專注於機器學習方面，未來研究所也會往這方向持續努力!\n\n" +
                        "現在正準備開始音樂方面的深度學習專題，目標將人聲從音樂中分離出來~";

                TextMessage resume = TextMessage.builder()
                        .text(resumeString)
                        .build();

                ButtonsTemplate buttonsTemplate = new ButtonsTemplate(
                        imageUrl,
                        "Jim Wang 的個人簡歷",
                        "你想知道些什麼?",
                        Arrays.asList(
                                new MessageAction("學校專題",
                                        "學校專題"),
                                new MessageAction("工作經驗",
                                        "工作經驗")
                        ));
                TemplateMessage templateMessage = new TemplateMessage("個人簡歷", buttonsTemplate);

                this.reply(replyToken, Arrays.asList(
                        resume,
                        templateMessage
                ));
                break;
            }

            case "工作經驗": {
                URI imageUrl = createUri("/static/buttons/resume.png");
                String resumeString = "從去年開始，我就在一家新創公司實習，主要的工作內容為做UI設計。\n\n" +
                        "運用到的技術有前端的 html, css, js, jQuery 以及前後端的整合 (Flask 做為後端)，" +
                        "也運用過 python 的 UI 介面 PySide2。\n\n" +
                        "在那裡的自由度很高，不過所有事基本上比較少人可以教你，因此也增加了我解決問題及自我學習的能力!";

                TextMessage resume = TextMessage.builder()
                        .text(resumeString)
                        .build();

                ButtonsTemplate buttonsTemplate = new ButtonsTemplate(
                        imageUrl,
                        "Jim Wang 的個人簡歷",
                        "你想知道些什麼?",
                        Arrays.asList(
                                new MessageAction("學校專題",
                                        "學校專題"),
                                new MessageAction("工作經驗",
                                        "工作經驗")
                        ));
                TemplateMessage templateMessage = new TemplateMessage("個人簡歷", buttonsTemplate);

                this.reply(replyToken, Arrays.asList(
                        resume,
                        templateMessage
                ));
                break;
            }

            case "興趣與專長": {
                List<URI> carouselUrl = new ArrayList<>();
                List<String> textString = new ArrayList<>();


                carouselUrl.add(createUri("/static/hobbies/sing.jpg"));
                carouselUrl.add(createUri("/static/hobbies/basketball.jpg"));
                carouselUrl.add(createUri("/static/hobbies/snow.jpg"));
                carouselUrl.add(createUri("/static/hobbies/rf.jpg"));

                textString.add("音樂對我來說就像是生活的良藥，尤其是唱歌。" +
                        "我也喜歡在舞台上展現自我，用歌聲造福大家。");
                textString.add("籃球是我最喜歡的運動之一，也曾是系上籃球隊的一員。我很享受在場上熱血奮鬥、汗水淋漓的感覺。");
                textString.add("說走就走的旅行總是充滿著有趣的過程及挑戰，去看看世界上的不同角落是我嚮往的。");
                textString.add("除了籃球外，網球也是我很喜歡的一項運動，" +
                        "我希望持續精進技術、找到更多熱愛網球的人。");

                CarouselTemplate carouselTemplate = new CarouselTemplate(
                        Arrays.asList(
                                new CarouselColumn(carouselUrl.get(0), "唱歌", textString.get(0), Arrays.asList(
                                        new MessageAction("人格特質",
                                                "人格特質")
                                )),
                                new CarouselColumn(carouselUrl.get(1), "籃球", textString.get(1), Arrays.asList(
                                        new MessageAction("人格特質",
                                                "人格特質")
                                )),
                                new CarouselColumn(carouselUrl.get(2), "冒險", textString.get(2), Arrays.asList(
                                        new MessageAction("人格特質",
                                                "人格特質")
                                )),
                                new CarouselColumn(carouselUrl.get(3), "網球", textString.get(3), Arrays.asList(
                                        new MessageAction("人格特質",
                                                "人格特質")
                                ))
                        )
                );
                TemplateMessage templateMessage = new TemplateMessage("興趣與專長", carouselTemplate);
                this.reply(replyToken, templateMessage);
                break;
            }

            case "人格特質": {
                this.reply(replyToken, ImagemapMessage
                        .builder()
                        .baseUrl(createUri("/static/put_map/5"))
                        .altText("人格特質")
                        .baseSize(new ImagemapBaseSize(1040, 1040))
                        .actions(Arrays.asList(
                                MessageImagemapAction.builder()
                                        .text("外向")
                                        .area(new ImagemapArea(0, 145, 260, 447))
                                        .build(),
                                MessageImagemapAction.builder()
                                        .text("好奇")
                                        .area(new ImagemapArea(780, 145, 260, 447))
                                        .build(),
                                MessageImagemapAction.builder()
                                        .text("健談")
                                        .area(new ImagemapArea(0, 592, 260, 447))
                                        .build(),
                                MessageImagemapAction.builder()
                                        .text("樂觀")
                                        .area(new ImagemapArea(780, 592, 260, 447))
                                        .build()
                        ))
                        .build());
                break;
            }

            case "外向": {
                this.reply(replyToken, ImagemapMessage
                        .builder()
                        .baseUrl(createUri("/static/put_map/1"))
                        .altText("人格特質-外向")
                        .baseSize(new ImagemapBaseSize(1040, 1040))
                        .actions(Arrays.asList(
                                MessageImagemapAction.builder()
                                        .text("好奇")
                                        .area(new ImagemapArea(780, 145, 260, 447))
                                        .build(),
                                MessageImagemapAction.builder()
                                        .text("健談")
                                        .area(new ImagemapArea(0, 592, 260, 447))
                                        .build(),
                                MessageImagemapAction.builder()
                                        .text("樂觀")
                                        .area(new ImagemapArea(780, 592, 260, 447))
                                        .build()
                        ))
                        .build());
                break;
            }

            case "好奇": {
                this.reply(replyToken, ImagemapMessage
                        .builder()
                        .baseUrl(createUri("/static/put_map/2"))
                        .altText("人格特質-好奇")
                        .baseSize(new ImagemapBaseSize(1040, 1040))
                        .actions(Arrays.asList(
                                MessageImagemapAction.builder()
                                        .text("外向")
                                        .area(new ImagemapArea(0, 145, 260, 447))
                                        .build(),
                                MessageImagemapAction.builder()
                                        .text("健談")
                                        .area(new ImagemapArea(0, 592, 260, 447))
                                        .build(),
                                MessageImagemapAction.builder()
                                        .text("樂觀")
                                        .area(new ImagemapArea(780, 592, 260, 447))
                                        .build()
                        ))
                        .build());
                break;
            }

            case "健談": {
                this.reply(replyToken, ImagemapMessage
                        .builder()
                        .baseUrl(createUri("/static/put_map/3"))
                        .altText("人格特質-健談")
                        .baseSize(new ImagemapBaseSize(1040, 1040))
                        .actions(Arrays.asList(
                                MessageImagemapAction.builder()
                                        .text("外向")
                                        .area(new ImagemapArea(0, 145, 260, 447))
                                        .build(),
                                MessageImagemapAction.builder()
                                        .text("好奇")
                                        .area(new ImagemapArea(780, 145, 260, 447))
                                        .build(),
                                MessageImagemapAction.builder()
                                        .text("樂觀")
                                        .area(new ImagemapArea(780, 592, 260, 447))
                                        .build()
                        ))
                        .build());
                break;
            }

            case "樂觀": {
                this.reply(replyToken, ImagemapMessage
                        .builder()
                        .baseUrl(createUri("/static/put_map/4"))
                        .altText("人格特質-樂觀")
                        .baseSize(new ImagemapBaseSize(1040, 1040))
                        .actions(Arrays.asList(
                                MessageImagemapAction.builder()
                                        .text("外向")
                                        .area(new ImagemapArea(0, 145, 260, 447))
                                        .build(),
                                MessageImagemapAction.builder()
                                        .text("好奇")
                                        .area(new ImagemapArea(780, 145, 260, 447))
                                        .build(),
                                MessageImagemapAction.builder()
                                        .text("健談")
                                        .area(new ImagemapArea(0, 592, 260, 447))
                                        .build()
                        ))
                        .build());
                break;
            }

            case "網站": {
                URI githubUrl = createUri("/static/icon/github_icon.png");
                URI fbUrl = createUri("/static/icon/fb.png");
                ImageCarouselTemplate imageCarouselTemplate = new ImageCarouselTemplate(
                        Arrays.asList(
                                new ImageCarouselColumn(githubUrl,
                                                        new URIAction("My github",
                                                                      URI.create("https://github.com/jimwanggg"), null)
                                ),
                                new ImageCarouselColumn(fbUrl,
                                                        new URIAction("My Facebook",
                                                                URI.create("https://www.facebook.com/wan.j.xiong.1/"), null)
                                )
                        ));
                TemplateMessage templateMessage = new TemplateMessage("ImageCarousel alt text",
                                                                      imageCarouselTemplate);
                this.reply(replyToken, templateMessage);
                break;
            }

            default:
                log.info("Returns sorry message");
                this.reply(replyToken, new DefaultFlexMessageCreator(text).get());
                break;
        }
    }
    private String doConnectionAI(String inputUrl, String inputMsg) throws Exception{
        HttpURLConnection con = null;
        DataOutputStream os = null;
        String replyMsg = "";
        URL url = new URL(inputUrl);
        String[] inputData = {"{\"text\":\"" + inputMsg + "\"}"};
        for(String input: inputData){
            byte[] postData = input.getBytes(StandardCharsets.UTF_8);
            con = (HttpURLConnection) url.openConnection();
            con.setDoOutput(true);
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty( "charset", "utf-8");
            con.setRequestProperty("Content-Length", Integer.toString(input.length()));
            os = new DataOutputStream(con.getOutputStream());
            os.write(postData);
            os.flush();

            if (con.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + con.getResponseCode());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(
                    (con.getInputStream())));

            String output;
            //replyMsg += "Output from Server .... \n";
            while ((output = br.readLine()) != null) {
                replyMsg += output;
            }
            con.disconnect();
        }
        return replyMsg;
    }
    private void handleTextContentTranslationBot(String replyToken, MessageEvent<TextMessageContent> event, TextMessageContent content) throws Exception {
        final String text = content.getText();

        log.info("Got text message from replyToken:{}: text:{} emojis:{}", replyToken, text,
                content.getEmojis());
        String replyMsg = "";
        String replaceText = text.replace('\'', 'a');
        if (replaceText.equals("quit")) {
            UserAIMode.getAIMode().removeAIMap(event.getSource().getUserId());
            replyMsg = "已退出翻譯模式!!";
        }
        else if (CharMatcher.ascii().matchesAllOf(replaceText)){
            replyMsg = doConnectionAI("http://140.112.30.57:9000/translation/", text);
            //log(con.getOutputStream().toString());
            //this.replyText(replyToken, replyMsg);
        }
        else{
            replyMsg = "目前只能輸入英文哦!\n\n如果要退出請打\"quit\"!";
        }
        this.reply(replyToken,
                TextMessage.builder()
                        .text(replyMsg)
                        .sender(Sender.builder()
                                .name("翻譯機器人")
                                .iconUrl(createUri("/static/icon/translationbot.png"))
                                .build())
                        .build());
    }

    private void handleTextContentChatBot(String replyToken, MessageEvent<TextMessageContent> event, TextMessageContent content) throws Exception {
        final String text = content.getText();

        log.info("Got text message from replyToken:{}: text:{} emojis:{}", replyToken, text,
                content.getEmojis());
        String replyMsg = "";
        String replaceText = text.replace('\'', 'a');
        if (replaceText.equals("quit")) {
            UserAIMode.getAIMode().removeAIMap(event.getSource().getUserId());
            replyMsg = "已退出聊天模式!!";
        }
        else if (CharMatcher.ascii().matchesAllOf(replaceText)){
            replyMsg = doConnectionAI("http://140.112.30.57:9000/chat/", text);
            //log(con.getOutputStream().toString());
            //this.replyText(replyToken, replyMsg);
        }
        else{
            replyMsg = "目前只能輸入英文哦!\n\n如果要退出請打\"quit\"!";
        }
        this.reply(replyToken,
                TextMessage.builder()
                        .text(replyMsg)
                        .sender(Sender.builder()
                                .name("聊天機器人")
                                .iconUrl(createUri("/static/icon/chatbot.jpg"))
                                .build())
                        .build());
    }

    private static URI createUri(String path) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                                          .scheme("https")
                                          .path(path).build()
                                          .toUri();
    }

    private void system(String... args) {
        ProcessBuilder processBuilder = new ProcessBuilder(args);
        try {
            Process start = processBuilder.start();
            int i = start.waitFor();
            log.info("result: {} =>  {}", Arrays.toString(args), i);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            log.info("Interrupted", e);
            Thread.currentThread().interrupt();
        }
    }

    private static DownloadedContent saveContent(String ext, MessageContentResponse responseBody) {
        log.info("Got content-type: {}", responseBody);

        DownloadedContent tempFile = createTempFile(ext);
        try (OutputStream outputStream = Files.newOutputStream(tempFile.path)) {
            ByteStreams.copy(responseBody.getStream(), outputStream);
            log.info("Saved {}: {}", ext, tempFile);
            return tempFile;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static DownloadedContent createTempFile(String ext) {
        String fileName = LocalDateTime.now().toString() + '-' + UUID.randomUUID() + '.' + ext;
        Path tempFile = KitchenSinkApplication.downloadedContentDir.resolve(fileName);
        tempFile.toFile().deleteOnExit();
        return new DownloadedContent(
                tempFile,
                createUri("/downloaded/" + tempFile.getFileName()));
    }

    @Value
    private static class DownloadedContent {
        Path path;
        URI uri;
    }
}
