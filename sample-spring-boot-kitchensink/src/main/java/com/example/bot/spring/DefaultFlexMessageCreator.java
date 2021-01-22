package com.example.bot.spring;

import static java.util.Arrays.asList;

import com.linecorp.bot.model.action.MessageAction;
import com.linecorp.bot.model.message.FlexMessage;

import java.net.URI;
import java.util.function.Supplier;

import com.linecorp.bot.model.action.URIAction;
import com.linecorp.bot.model.message.flex.component.Box;
import com.linecorp.bot.model.message.flex.component.Button;
import com.linecorp.bot.model.message.flex.component.Button.ButtonHeight;
import com.linecorp.bot.model.message.flex.component.Button.ButtonStyle;
import com.linecorp.bot.model.message.flex.component.Icon;
import com.linecorp.bot.model.message.flex.component.Separator;
import com.linecorp.bot.model.message.flex.component.Spacer;
import com.linecorp.bot.model.message.flex.component.Text;
import com.linecorp.bot.model.message.flex.container.Bubble;
import com.linecorp.bot.model.message.flex.unit.FlexFontSize;
import com.linecorp.bot.model.message.flex.unit.FlexLayout;
import com.linecorp.bot.model.message.flex.unit.FlexMarginSize;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

public class DefaultFlexMessageCreator implements Supplier<FlexMessage> {
    private String userText;

    public DefaultFlexMessageCreator(String userText) {
        this.userText = userText;
    }

    @Override
    public FlexMessage get() {
        final Box bodyBlock = createBodyBlock();

        final Box footerBlock = createFooterBlock();
        final Bubble bubble =
                Bubble.builder()
                        .body(bodyBlock)
                        .footer(footerBlock)
                        .build();

        return new FlexMessage("不好意思! 目前並沒有支援** " + userText + " **這個指令呦!如果要了解詳細使用說明，請點擊下方使用教學~", bubble);
    }

    private Box createFooterBlock() {
        final Spacer spacer = Spacer.builder().size(FlexMarginSize.SM).build();
        final Button userAction = Button
                .builder()
                .style(ButtonStyle.PRIMARY)
                .height(ButtonHeight.SMALL)
                .action(new URIAction("使用說明", URI.create("https://hackmd.io/@GDxABCY6RJydRN7UbLYgHw/SkxqoASkO"), null))
                .build();
        final Separator separator = Separator.builder().build();
        final Button IntroductionAction =
                Button.builder()
                        .style(ButtonStyle.SECONDARY)
                        .height(ButtonHeight.SMALL)
                        .action(new MessageAction("關於我", "關於我"))
                        .build();

        return Box.builder()
                .layout(FlexLayout.HORIZONTAL)
                .spacing(FlexMarginSize.SM)
                .contents(asList(spacer, userAction, separator, IntroductionAction))
                .build();
    }

    private Box createBodyBlock() {

        final Box review = createMainBox();
        final Box secondLine = createSecondMainBox();
        final Separator separator = Separator.builder().build();

        return Box.builder()
                .layout(FlexLayout.VERTICAL)
                .contents(asList(review, secondLine, separator))
                .build();
    }

    private Box createSecondMainBox() {
        final Text sorryText =
                Text.builder()
                        .text("目前並沒有支援 ** " + userText + " ** 這個指令呦!如果要了解詳細使用說明，請點擊下方使用教學~")
                        .wrap(true)
                        .size(FlexFontSize.SM)
                        .color("#666666")
                        .margin(FlexMarginSize.MD)
                        .flex(0)
                        .build();

        final Spacer spacer = Spacer.builder().size(FlexMarginSize.SM).build();
        return Box.builder()
                .layout(FlexLayout.VERTICAL)
                .margin(FlexMarginSize.SM)
                .contents(asList(sorryText, spacer))
                .build();
    }

    private Box createMainBox() {
        final Icon sorryIcon =
                Icon.builder().size(FlexFontSize.SM).url(createUri("/static/icon/sorry.png")).build();
        final Text sorryText =
                Text.builder()
                        .text("不好意思!")
                        .size(FlexFontSize.SM)
                        .color("#666666")
                        .margin(FlexMarginSize.MD)
                        .flex(0)
                        .build();

        return Box.builder()
                .layout(FlexLayout.BASELINE)
                .margin(FlexMarginSize.MD)
                .contents(asList(sorryIcon, sorryIcon, sorryText))
                .build();
    }

    private static URI createUri(String path) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .scheme("https")
                .path(path).build()
                .toUri();
    }
}
