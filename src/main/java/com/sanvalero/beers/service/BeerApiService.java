package com.sanvalero.beers.service;

import com.sanvalero.beers.domain.Beer;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;
import rx.Observable;

import java.util.List;

/**
 * Creado por @author: Javier
 * el 13/05/2021
 */
public interface BeerApiService {

    @GET("beers")
    Observable<List<Beer>> getBeers();

    @GET("beers/")
    Observable<List<Beer>> getBeersPage(@Query("page") int page);
}
