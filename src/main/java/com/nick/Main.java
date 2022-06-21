package com.nick;

import org.jetbrains.annotations.NotNull;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

public class Main {

    private static final String TOKEN = "5365898334:AAFqoFFfuIbUR06uD9RnkPjs7Tw-fGqCq4o";
    private static final ConcurrentHashMap<PomodoroBot.Timer, Long> userTimers = new ConcurrentHashMap();

    public static void main(String[] args) throws TelegramApiException {
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        PomodoroBot bot = new PomodoroBot();
        telegramBotsApi.registerBot(bot);
        // новый параллельный процесс: проверка таймера и сообщения от пользователя
        new Thread(() -> {
            try {
                bot.checkTimer();
            } catch (InterruptedException e) {
                System.out.println("Error!");
                // throw new RuntimeException(e);
            }
        }).run();
    }

    static class PomodoroBot extends TelegramLongPollingBot {

        enum TimerType {
            WORK,
            BREAK
        }

        static record Timer(Instant time, TimerType timerType) {

        }

//        public PomodoroBot() {
//            super();
//            try {
//                checkTimer();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//                // throw new RuntimeException(e);
//            }
//        }
        @Override
        public String getBotUsername() {return "Pomodoro Bot";}
        @Override
        public String getBotToken() {return TOKEN;}
        @Override
        public void onUpdateReceived(Update update) {
            if (update.hasMessage() && update.getMessage().hasText()) {
                Long chatId = update.getMessage().getChatId();
                if (update.getMessage().getText().equals("/start") || update.getMessage().getText().equals("/bot")) {
                    sendEcho("Pomodoro - make time effective\n" +
                            "Give me time work and break. " +
                            "For example: 1 1\n(in minutes)", chatId.toString());
                } else {
                    String[] args = update.getMessage().getText().split(" ");
                    if (args.length >= 1) {
                        Instant  workTime  = Instant.now().plus(Long.parseLong(args[0]), ChronoUnit.MINUTES);
                        userTimers.put(new Timer(workTime, TimerType.WORK), chatId);
                        sendEcho("Time for work!", chatId.toString());
                        if (args.length >= 2) {
                            Instant breakTime = workTime.plus(Long.parseLong(args[1]), ChronoUnit.MINUTES);
                            userTimers.put(new Timer(breakTime, TimerType.BREAK), chatId);
                            // sendEcho("Time for break!", chatId.toString());
                        }
                    }
                    // sendEcho("test server_time = " + Instant.now().toString(), update.getMessage().getChatId().toString());
                }
            }
        }

        private void checkTimer() throws InterruptedException {
            while (true) {
                // todo научиться понимать id пользователя sendEcho()
                // System.out.println("test server_time = " + Instant.now().toString());
                System.out.println("Колич. таймеров пользователя = " + userTimers.size());
                userTimers.forEach((timer, userId) -> {
                    System.out.printf("Check userId = %d, server_time = %s, user_time = %s\n",
                            userId, Instant.now().toString(), timer.time.toString());
                    if (Instant.now().isAfter(timer.time)) {
                        userTimers.remove(timer);
                        switch (timer.timerType) {
                            case WORK -> sendEcho("Time to break", userId.toString());
                            case BREAK -> sendEcho("Timer end", userId.toString());
                        }
                    }
                });
                Thread.sleep(1000);
            }
        }

        private void sendEcho(String text, String chatId) {
            SendMessage msg = new SendMessage();
            // пользователь чата
            msg.setChatId(chatId);
            msg.setProtectContent(true); // 1
            // msg.setProtectContent(true); // 2
            msg.setText(text);
            try {
                execute(msg);
            } catch (TelegramApiException e) {
                System.out.println("Error!");
                // throw new RuntimeException(e);
            }
        }
    }
    static class EchoBot extends TelegramLongPollingBot {
        @Override
        public String getBotUsername() {return "Echo Bot.";}
        @Override
        public String getBotToken() {return TOKEN;}
        int userCount =0;
        // обработка сообщений
        @Override
        public void onUpdateReceived(@NotNull Update update) {
            // int userCount =0;
            if (update.hasMessage() && update.getMessage().hasText()) {
                if (update.getMessage().getText().equals("/start") || update.getMessage().getText().equals("/bot")) {
                     System.out.println("Новый пользователь: " + ++userCount);
                    // приветствие

                    String st = "This is Echo-bot: " + Integer.toString(userCount) +"-th start";
                    sendEcho(st, update.getMessage().getChatId().toString());
                } else {
                    System.out.println("Обработка сообщений");
                    // sendEcho(update.getMessage().getText(), update.getMessage().getChatId().toString()); // 1
                    sendEcho(update.getMessage().getText().toUpperCase(), update.getMessage().getChatId().toString()); // 2
                }
            }
        }

        private void sendEcho(String text, String chatId) {
            SendMessage msg = new SendMessage();
            // пользователь чата
            msg.setChatId(chatId);
            msg.setProtectContent(true); // 1
            // msg.setProtectContent(true); // 2
            msg.setText(text);
            try {
                execute(msg);
            } catch (TelegramApiException e) {
                System.out.println("Error!");
                // throw new RuntimeException(e);
            }
        }
    }
}
