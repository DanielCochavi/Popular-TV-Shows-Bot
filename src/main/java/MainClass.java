import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainClass {

    public static void main(String[] args) throws InterruptedException {
        PopularTVShowsUpdater popularTVShowsUpdater = new PopularTVShowsUpdater(30);
        Thread popularTVShowsUpdaterThread = new Thread(popularTVShowsUpdater);
        popularTVShowsUpdaterThread.start();

        ApiContextInitializer.init();
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi();
        try {
            telegramBotsApi.registerBot(new PopularTVShowsBot(popularTVShowsUpdater));
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
