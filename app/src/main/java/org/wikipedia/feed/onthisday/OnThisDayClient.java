package org.wikipedia.feed.onthisday;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.retrofit.RetrofitFactory;
import org.wikipedia.feed.dataclient.FeedClient;
import org.wikipedia.feed.model.Card;
import org.wikipedia.settings.Prefs;
import org.wikipedia.util.DateUtil;
import org.wikipedia.util.log.L;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.GET;
import retrofit2.http.Path;

public class OnThisDayClient implements FeedClient {
    @Nullable private Call<OnThisDay> call;

    @Override
    public void request(@NonNull Context context, @NonNull WikiSite wiki, int age, @NonNull FeedClient.Callback cb) {
        Calendar today = DateUtil.getDefaultDateFor(age);
        String endpoint = String.format(Locale.ROOT, Prefs.getRestbaseUriFormat(), wiki.scheme(),
                wiki.authority());
        Retrofit retrofit = RetrofitFactory.newInstance(endpoint, wiki);
        OnThisDayClient.Service service = retrofit.create(Service.class);
        call = service.getSelectedEvent(today.get(Calendar.MONTH) + 1, today.get(Calendar.DATE));
        call.enqueue(new CallbackAdapter(cb, today, wiki, age));
    }

    public Call<OnThisDay> request(@NonNull WikiSite wiki, int month, int date) {
        String endpoint = String.format(Locale.ROOT, Prefs.getRestbaseUriFormat(), wiki.scheme(),
                wiki.authority());
        Retrofit retrofit = RetrofitFactory.newInstance(endpoint, wiki);
        OnThisDayClient.Service service = retrofit.create(Service.class);
        return service.getAllOtdEvents(month, date);
    }

    @Override
    public void cancel() {
        if (call == null) {
            return;
        }
        call.cancel();
        call = null;
    }

    static class CallbackAdapter implements retrofit2.Callback<OnThisDay> {
        @NonNull private final FeedClient.Callback cb;
        private Calendar today;
        private WikiSite wiki;
        private int age;

        CallbackAdapter(@NonNull FeedClient.Callback cb, Calendar today, WikiSite wiki, int age) {
            this.cb = cb;
            this.today = today;
            this.wiki = wiki;
            this.age = age;
        }

        @Override
        public void onResponse(@NonNull Call<OnThisDay> call,
                               @NonNull Response<OnThisDay> response) {
            List<Card> cards = new ArrayList<>();
            OnThisDay onThisDay = response.body();
            if (onThisDay == null || onThisDay.selectedEvents().size() <= 1) {
                cb.error(new RuntimeException("Incorrect response format."));
                return;
            }
            int randomIndex = new Random().nextInt(onThisDay.selectedEvents().size() - 1);
            OnThisDay.Event event = onThisDay.selectedEvents().get(randomIndex);
            OnThisDayCard card = new OnThisDayCard(onThisDay, event, onThisDay.selectedEvents().get(randomIndex + 1).year(), today, wiki, age);
            cards.add(card);
            cb.success(cards);
        }

        @Override
        public void onFailure(@NonNull Call<OnThisDay> call, @NonNull Throwable caught) {
            L.v(caught);
            cb.error(caught);
        }
    }

    @VisibleForTesting
    interface Service {
        @NonNull
        @GET("feed/onthisday/selected/{mm}/{dd}")
        Call<OnThisDay> getSelectedEvent(@Path("mm") int month,
                                         @Path("dd") int day);

        @NonNull
        @GET("feed/onthisday/events/{mm}/{dd}")
        Call<OnThisDay> getAllOtdEvents(@Path("mm") int month,
                                        @Path("dd") int day);
    }

    @VisibleForTesting
    @NonNull
    Call<OnThisDay> request(@NonNull Service service, int mon, int day) {
        return service.getSelectedEvent(mon, day);
    }
}
