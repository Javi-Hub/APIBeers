package com.sanvalero.beers.domain;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

/**
 * Creado por @author: Javier
 * el 13/05/2021
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Beer {

    private int id;
    private String name;
    @SerializedName(value = "tagline")
    private String tagLine;
    @SerializedName(value = "first_brewed")
    private String firstBrewed;
    private String description;
    @SerializedName(value = "ibu")
    private float bitter;
    @SerializedName(value = "image_url")
    private String imageUrl;
    @SerializedName(value = "abv")
    private float degree;

    private Volume volume;

}
