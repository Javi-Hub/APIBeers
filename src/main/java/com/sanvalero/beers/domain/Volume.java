package com.sanvalero.beers.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Creado por @author: Javier
 * el 13/05/2021
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Volume {

    private int value;
    private String unit;

}
