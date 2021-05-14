package com.sanvalero.beers.service;

import com.sanvalero.beers.domain.Beer;
import com.sanvalero.beers.util.R;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import rx.Observable;

import java.util.List;

import static com.sanvalero.beers.util.Constants.URL;

/**
 * Creado por @author: Javier
 * el 13/05/2021
 */
public class BeerService {

        private BeerApiService api;

        public BeerService(){

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                    .build();

            api = retrofit.create(BeerApiService.class);

        }

        public Observable<List<Beer>> getBeers(){
            return api.getBeers();
        }

        public Observable<List<Beer>> getBeersPage(int page){
            return api.getBeersPage(page);
        }


}
