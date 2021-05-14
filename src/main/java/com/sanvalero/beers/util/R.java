package com.sanvalero.beers.util;

import java.io.File;
import java.net.URL;

/**
 * Creado por @author: Javier
 * el 13/05/2021
 */
public class R {

    public static URL getUI (String name){
        return Thread.currentThread().getContextClassLoader().getResource("ui" + File.separator + name);
    }

}
