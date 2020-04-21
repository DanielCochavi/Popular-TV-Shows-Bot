import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class PopularTVShowsBot extends TelegramLongPollingBot {

    private final PopularTVShowsUpdater popularTVShowsUpdater;

    public PopularTVShowsBot(PopularTVShowsUpdater popularTVShowsUpdater) {
        this.popularTVShowsUpdater = popularTVShowsUpdater;
    }

    public void onUpdateReceived(Update update) {
        String command = update.getMessage().getText();
        command = command.substring(1);
        SendMessage msgToUser = new SendMessage();
        String replyToUser;
        List<String> popularTvShowsList = null;

        ConcurrentHashMap<String, List<String>> popularTVShowsMap = popularTVShowsUpdater.getPopularTVShowsByGenre();
        if(popularTVShowsMap.isEmpty()){
            // The TV Shows lists hasn't loaded yet,
            // or the updater didn't managed to build the lists.
            command = "empty_lists";
        }
        else{
            popularTvShowsList = popularTVShowsMap.get(command);
        }

        if (popularTvShowsList != null) {
            replyToUser = String.join("\n", popularTvShowsList);
        } else if ("help".equals(command)) {
            replyToUser = "Please try to type only one of the following genres:\n/action_adventure\n" +
                    "/animation\n/comedy\n/crime\n/documentary\n/drama\n/family\n/kids\n/mystery\n" +
                    "/news\n/reality\n/sci_fi_fantasy\n/soap\n/talk\n/war_politics\n/western";
        } else if ("empty_lists".equals(command)) {
            replyToUser = "Something is wrong!\n" +
                    "Please try again later.";
        } else {
            replyToUser = "There is no such command in my bot.\n" +
                    "For more help try using /help command.";
        }
        msgToUser.setText(replyToUser);
        msgToUser.setChatId(update.getMessage().getChatId());
        try {
            execute(msgToUser);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public String getBotUsername() {
        return "PopularTVShowsBot";
    }

    public String getBotToken() {
        return "1264115954:AAHbWWu8NnUHhu1cWvPXcNt0pTN315KkA-8";
    }
}
