package com.sanvalero.beers.controller;

import com.sanvalero.beers.domain.Beer;
import com.sanvalero.beers.service.BeerService;
import com.sanvalero.beers.util.AlertUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import rx.Observable;
import rx.schedulers.Schedulers; // OJO IMPORTAR RX

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Creado por @author: Javier
 * el 13/05/2021
 */
public class AppController implements Initializable {

    public TableView<Beer> tvData;
    public ListView lvData;
    public ProgressIndicator piProgress;
    public TextField tfFecha, tfMin, tfMax, tfSearch, tfPage;
    public WebView wvImage;
    public Label lbName, lbSlogan, lbDate, lbDegree, lbValue, lbUnit;
    public TextArea taDescription;
    public ComboBox<String> cbCombo, cbComboYear;

    private BeerService service;
    private ObservableList<Beer> listBeers = FXCollections.observableArrayList();
    private ObservableList<Beer> listBitterBeers = FXCollections.observableArrayList();
    private List<Beer> list;
    private WebEngine engine;

    private int count = 1;
    private int page = 1;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        service = new BeerService();
        list = new ArrayList<>();
        /** Rellenar combo con opciones Todas las cervezas o ordenar por grados*/
        ObservableList<String> itemsCombo = FXCollections.observableArrayList("Cervezas", "Grados");
        cbCombo.setItems(itemsCombo);
        cbCombo.setValue("Cervezas");

        loadColumnsTable();
        loadComplete();
    }

    @FXML
    public void orderBy(){
        String selection = cbCombo.getValue();
        switch (selection){
            case "Cervezas":
                loadComplete();
            case "Grados":
                CompletableFuture.runAsync(() -> {
                    orderByDegrees();
                    timeProgressIndicator();
                }).whenComplete((string, throwable) -> piProgress.setVisible(false));
        }
    }

    @FXML
    public void filterMaxMin(){

        if (tfMin.getText().isBlank() || tfMax.getText().isBlank()){
            AlertUtils.showError("Rellene todos los campos");
            return;
        }
            float min = Float.parseFloat(tfMin.getText());
            float max = Float.parseFloat(tfMax.getText());
            lvData.setItems(list.stream()
                    .filter(beer -> beer.getBitter() < max && beer.getBitter() > min)
                    .collect(Collectors.toCollection(FXCollections::observableArrayList)));

        /*listBitterBeers.clear();
        lvData.setItems(listBitterBeers);
        service.getBeers()
                .flatMap(Observable::from)
                .filter(beer -> beer.getBitter() < max && beer.getBitter() > min)
                .doOnCompleted(() -> System.out.println("Filter by bitter"))
                .doOnError(throwable -> System.out.println(throwable.getMessage()))
                .subscribe(beer -> listBitterBeers.add(beer));*/
    }

    @FXML
    public void export(){
        List<Beer> list = tvData.getItems();
        FileChooser fileChooser = new FileChooser();
        File file = fileChooser.showSaveDialog(null);
        export(file, list);
    }

    @FXML
    public void searchText(){
        String text = tfSearch.getText();
        searchBeers(text);
    }

    @FXML
    public void searchYear(){
        String year = cbComboYear.getValue();
        tvData.setItems(list.stream()
                                .filter(beer -> beer.getFirstBrewed().substring(3,7).equals(year))
                                .collect(Collectors.toCollection(FXCollections::observableArrayList)));
    }

    @FXML
    public void zip(){
        List<Beer> list = tvData.getItems();
        FileChooser fileChooser = new FileChooser();
        File file = fileChooser.showSaveDialog(null);
        CompletableFuture.supplyAsync(() -> export(file, list))
                .thenAccept(this::zipFile)
                .whenComplete((string, throwable) -> System.out.println(throwable.getMessage()));
    }

    @FXML
    public void getDetailTable(Event event){
        Beer beer = tvData.getSelectionModel().getSelectedItem();
        engine = wvImage.getEngine();
        engine.load(beer.getImageUrl());
        lbName.setText(beer.getName());
        lbSlogan.setText(beer.getTagLine());
        lbDate.setText(beer.getFirstBrewed());
        lbDegree.setText(String.valueOf(beer.getDegree()));
        lbValue.setText(String.valueOf(beer.getVolume().getValue()));
        lbUnit.setText(beer.getVolume().getUnit());
        taDescription.setWrapText(true);
        taDescription.setMaxSize(300, 300);
        taDescription.setText(beer.getDescription());
    }

    @FXML
    public void page(Event event){
        page = Integer.parseInt(tfPage.getText());
        loadCompletePage(page);
        count = page;
        System.out.println(count);
    }

    @FXML
    public void previous(Event event){
        count--;
        System.out.println(count);
        loadCompletePage(count);
        tfPage.setText(String.valueOf(count));
    }

    @FXML
    public void next(Event event){
        count++;
        System.out.println(count);
        loadCompletePage(count);
        tfPage.setText(String.valueOf(count));
    }

    /**
     * *******************************************************************************************
     * *******************************************************************************************/

    public void loadDataBeers(){
        listBeers.clear();
        tvData.setItems(listBeers);
        service.getBeers()
                .flatMap(Observable::from)
                .doOnCompleted(() -> {
                    System.out.println("Loading Data");
                    cbComboYear.setItems(list.stream()
                                    .map(beer -> beer.getFirstBrewed().substring(3, 7))
                                    .sorted()
                                    .distinct()
                                    .collect(Collectors.toCollection(FXCollections::observableArrayList)));
                })
                .doOnError(throwable -> System.out.println(throwable.getMessage()))
                .subscribeOn(Schedulers.from((Executors.newCachedThreadPool())))
                .subscribe(beer -> {
                    listBeers.add(beer);
                    list.add(beer);
                });
    }

    public void loadDataBeersPage(int page){
        listBeers.clear();
        tvData.setItems(listBeers);
        service.getBeersPage(page)
                .flatMap(Observable::from)
                .doOnCompleted(() -> System.out.println("Loading Data Page"))
                .doOnError(throwable -> System.out.println(throwable.getMessage()))
                .subscribe(beer -> listBeers.add(beer));
    }

    public void orderByDegrees(){
        tvData.setItems(list.stream()
                        .sorted(Comparator.comparing(Beer::getDegree).reversed())
                        .collect(Collectors.toCollection(FXCollections::observableArrayList)));
        /*listBeers.clear();
        service.getBeers()
                .flatMap(Observable::from)
                .doOnCompleted(() -> System.out.println("Order by degrees"))
                .doOnError(throwable -> System.out.println(throwable.getMessage()))
                .subscribe(beer -> listBeers.add(beer));
        tvData.setItems(listBeers.stream()
                        .sorted(Comparator.comparing(Beer::getDegree).reversed())
                        .collect(Collectors.toCollection(FXCollections::observableArrayList)));*/

    }

    public void loadColumnsTable(){
        Field[] fields = Beer.class.getDeclaredFields();
        for (Field field : fields){
            if (field.getName().equals("id") || field.getName().equals("description") || field.getName().equals("imageUrl")
                    || field.getName().equals("volume")){ continue; }

            TableColumn<Beer, String> column = new TableColumn<>(field.getName());
            column.setCellValueFactory(new PropertyValueFactory<>(field.getName()));
            tvData.getColumns().add(column);
        }
        tvData.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tvData.setFixedCellSize(TableView.USE_COMPUTED_SIZE);
    }

    public void timeProgressIndicator(){
        piProgress.setVisible(true);
        piProgress.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        try {
            Thread.sleep(3000);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public void loadComplete(){
        piProgress.setVisible(true);
        CompletableFuture.runAsync(() -> {
            piProgress.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
            try{
                Thread.sleep(2000);
            } catch (Exception e){
                e.printStackTrace();
            }
            loadDataBeers();
        }).whenComplete((string, throwable) -> piProgress.setVisible(false));
    }

    public void loadCompletePage(int page){
        piProgress.setVisible(true);
        CompletableFuture.runAsync(() -> {
            piProgress.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
            try{
                Thread.sleep(2000);
            } catch (Exception e){
                e.printStackTrace();
            }
            loadDataBeersPage(page);
        }).whenComplete((string, throwable) -> piProgress.setVisible(false));
    }

    public void searchBeers(String text){
        /*tvData.setItems(list.stream()
                        .filter(beer -> beer.getName().equalsIgnoreCase(text))
                        .collect(Collectors.toCollection(FXCollections::observableArrayList)));*/

        listBeers.clear();

        service.getBeers()
                .flatMap(Observable::from)
                .doOnCompleted(() -> System.out.println("Search beers"))
                .doOnError(throwable -> System.out.println(throwable.getMessage()))
                .subscribe(beer -> listBeers.add(beer));

        tvData.setItems(listBeers.stream()
                .filter(beer -> beer.getName().equalsIgnoreCase(text))
                .collect(Collectors.toCollection(FXCollections::observableArrayList)));
    }

    public String export(File file, List<Beer> list){
        try {
            FileWriter fileWriter = new FileWriter(file + ".csv");
            CSVPrinter printer = CSVFormat.TDF.withHeader("Country;", "Code;", "Capital;", "Continent;", "Area;", "Population;", "Demonym;").print(fileWriter);

            for (Beer beer : list) {
                printer.printRecord(beer.getName(), ";", beer.getTagLine(), ";", beer.getFirstBrewed(),
                        ";", beer.getDescription(), ";", beer.getBitter(), ";", beer.getImageUrl(), ";", beer.getDegree());
            }
            printer.close();
        } catch (Exception e){
            e.printStackTrace();
        }
        return file.getAbsolutePath();
    }

    public void zipFile(String file) {
        try {
            FileOutputStream fos = new FileOutputStream(file + ".zip");
            ZipOutputStream zipOut = new ZipOutputStream(fos);
            File fileToZip = new File(file + ".csv");
            FileInputStream fis = new FileInputStream(fileToZip);
            ZipEntry zipEntry = new ZipEntry(fileToZip.getName());
            zipOut.putNextEntry(zipEntry);
            byte [] bytes = new byte[1024];
            int length;
            while ((length = fis.read(bytes)) >= 0){
                zipOut.write(bytes, 0, length);
            }
            zipOut.close();
            fis.close();
            fos.close();
            Files.delete(fileToZip.toPath());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
