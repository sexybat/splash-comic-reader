package com.mythicalcreaturesoftware.splash.ui.viewmodel;

import com.mythicalcreaturesoftware.splash.service.FileService;
import com.mythicalcreaturesoftware.splash.service.impl.FileServiceImpl;
import com.mythicalcreaturesoftware.splash.utils.DefaultValues;
import com.mythicalcreaturesoftware.splash.utils.MathHelper;
import de.saxsys.mvvmfx.ViewModel;
import de.saxsys.mvvmfx.utils.commands.Action;
import de.saxsys.mvvmfx.utils.commands.Command;
import de.saxsys.mvvmfx.utils.commands.CompositeCommand;
import de.saxsys.mvvmfx.utils.commands.DelegateCommand;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.*;
import javafx.scene.image.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;

public class ComicReaderViewModel implements ViewModel {
    private static Logger logger = LoggerFactory.getLogger(ComicReaderViewModel.class);

    private Command readingDirectionCommand;
    private Command zoomInCommand;
    private Command zoomOutCommand;
    private Command openFileCompleteCommand;
    private Command loadPreviousPageCommand;
    private Command loadNextPageCommand;
    private Command previousPageCommand;
    private Command nextPageCommand;
    private Command loadSliderPageCommand;
    private Command updatePagesPerViewPageCommand;
    private Command applyDefaultScaleCommand;
    private Command updatePreviewImageCommand;

    private BooleanProperty zoomInButton;
    private BooleanProperty zoomOutButton;
    private BooleanProperty readingDirectionRightProperty;
    private BooleanProperty isTwoPagesProperty;
    private BooleanProperty enableAll;
    private BooleanProperty enableNextPage;

    private IntegerProperty currentPageProperty;
    private IntegerProperty currentPagePreviewProperty;
    private IntegerProperty totalPagesProperty;

    private DoubleProperty scaleLevelProperty;
    private DoubleProperty screenWidthProperty;
    private DoubleProperty screenHeightProperty;
    private DoubleProperty currentPageDefaultScaleLevelProperty;

    private StringProperty fileNameProperty;
    private StringProperty filePathProperty;

    private ObjectProperty<Image> leftImageProperty;
    private ObjectProperty<Image> rightImageProperty;
    private ObjectProperty<Image> previewImageProperty;
    private ObjectProperty<Dimension> leftImageDimensionProperty;
    private ObjectProperty<Dimension> rightImageDimensionProperty;

    private FileService fileService;

    public ComicReaderViewModel () {
        logger.info("Initializing comic reader view model");

        fileService = new FileServiceImpl();

        initializeDefaultProperties();
        initializeCommands();
    }

    private void initializeDefaultProperties () {
        logger.info("Initializing default properties");

        isTwoPagesProperty = new SimpleBooleanProperty(true);
        readingDirectionRightProperty = new SimpleBooleanProperty(true);
        enableAll = new SimpleBooleanProperty(false);
        enableNextPage = new SimpleBooleanProperty(false);

        zoomInButton = new SimpleBooleanProperty(true);
        zoomOutButton = new SimpleBooleanProperty(true);

        scaleLevelProperty = new SimpleDoubleProperty(1);
        screenWidthProperty = new SimpleDoubleProperty(1);
        screenHeightProperty = new SimpleDoubleProperty(1);
        currentPageDefaultScaleLevelProperty = new SimpleDoubleProperty(1);

        currentPageProperty = new SimpleIntegerProperty(1);
        currentPagePreviewProperty = new SimpleIntegerProperty(1);
        totalPagesProperty = new SimpleIntegerProperty(1);

        fileNameProperty = new SimpleStringProperty("");
        filePathProperty = new SimpleStringProperty("");

        leftImageProperty = new SimpleObjectProperty<>(new Image(DefaultValues.DEFAULT_IMAGE_PATH, true));
        rightImageProperty = new SimpleObjectProperty<>(new Image(DefaultValues.DEFAULT_IMAGE_PATH, true));
        previewImageProperty = new SimpleObjectProperty<>(new Image(DefaultValues.DEFAULT_IMAGE_PATH, true));

        leftImageDimensionProperty = new SimpleObjectProperty<>(new Dimension(1, 1));
        rightImageDimensionProperty = new SimpleObjectProperty<>(new Dimension(1, 1));
    }

    private void initializeCommands () {
        logger.info("Initializing commands");

        readingDirectionCommand = new DelegateCommand(() -> new Action() {
            @Override
            protected void action() throws Exception {
                changeReadingDirection();
            }
        }, false);

        zoomInCommand = new DelegateCommand(() -> new Action() {
            @Override
            protected void action() throws Exception {
                zoomIn();
            }
        }, zoomInButton, false);

        zoomOutCommand = new DelegateCommand(() -> new Action() {
            @Override
            protected void action() throws Exception {
                zoomOut();
            }
        }, zoomOutButton, false);

        applyDefaultScaleCommand = new DelegateCommand(() -> new Action() {
            @Override
            protected void action() throws Exception {
                applyDefaultScale();
            }
        }, false);

        Command pagePerViewCommand = new DelegateCommand(() -> new Action() {
            @Override
            protected void action() throws Exception {
                setPagesPerView();
            }
        }, false);

        Command updateCurrentFromUiCommand = new DelegateCommand(() -> new Action() {
            @Override
            protected void action() throws Exception {
                updateCurrentFromUi();
            }
        }, false);

        Command loadImagesCommand = new DelegateCommand(() -> new Action() {
            @Override
            protected void action() throws Exception {
                loadImages();
            }
        }, false);

        Command openFileCommand = new DelegateCommand(() -> new Action() {
            @Override
            protected void action() throws Exception {
                openFile();
            }
        }, false);

        Command totalPageCommand = new DelegateCommand(() -> new Action() {
            @Override
            protected void action() throws Exception {
               updateTotalPages();
            }
        }, false);

        Command currentPageCommand = new DelegateCommand(() -> new Action() {
            @Override
            protected void action() throws Exception {
                updateCurrentPage();
            }
        }, false);

        previousPageCommand = new DelegateCommand(() -> new Action() {
            @Override
            protected void action() throws Exception {
                previousPage();
            }
        }, createEnablePreviousPageButtonProperty(), false);

        nextPageCommand = new DelegateCommand(() -> new Action() {
            @Override
            protected void action() throws Exception {
                nextPage();
            }
        }, createEnableNextPageButtonProperty(), false);

        updatePreviewImageCommand = new DelegateCommand(() -> new Action() {
            @Override
            protected void action() throws Exception {
                updatePreviewImage();
            }
        }, false);

        openFileCompleteCommand = new CompositeCommand(openFileCommand, loadImagesCommand, totalPageCommand, currentPageCommand);

        loadNextPageCommand = new CompositeCommand(nextPageCommand, loadImagesCommand);

        loadPreviousPageCommand = new CompositeCommand(previousPageCommand, loadImagesCommand);

        loadSliderPageCommand = new CompositeCommand(updateCurrentFromUiCommand, loadImagesCommand);

        updatePagesPerViewPageCommand = new CompositeCommand(pagePerViewCommand, loadImagesCommand);
    }

    private BooleanProperty createEnableNextPageButtonProperty () {
        logger.debug("Creating EnableNextPageButtonProperty");

        BooleanProperty enableNextPageButton = new SimpleBooleanProperty();

        BooleanBinding nextPageBinding = Bindings.when(enableNextPage).then(true).otherwise(false);
        enableNextPageButton.bind(nextPageBinding);

        return enableNextPageButton;
    }

    private BooleanProperty createEnablePreviousPageButtonProperty () {
        logger.debug("creating EnablePreviousPageButtonProperty");

        BooleanProperty enablePreviousPageButton = new SimpleBooleanProperty();

        BooleanBinding previousPageBinding = Bindings.when(currentPageProperty.lessThanOrEqualTo(1)).then(false).otherwise(true);
        enablePreviousPageButton.bind(previousPageBinding);

        return enablePreviousPageButton;
    }

    public StringProperty getFileNameProperty(){
        return fileNameProperty;
    }

    public StringProperty getFilePathProperty(){
        return filePathProperty;
    }

    public ObjectProperty<Image> getLeftImageProperty(){
        return leftImageProperty;
    }

    public ObjectProperty<Image> getRightImageProperty(){
        return rightImageProperty;
    }

    public ObjectProperty<Image> getPreviewImageProperty() {
        return previewImageProperty;
    }

    public ObjectProperty<Dimension> getLeftImageDimensionProperty(){
        return leftImageDimensionProperty;
    }

    public ObjectProperty<Dimension> getRightImageDimensionProperty(){
        return rightImageDimensionProperty;
    }

    public BooleanProperty getIsTwoPagesProperty(){
        return isTwoPagesProperty;
    }

    public BooleanProperty getReadingDirectionRightProperty(){
        return readingDirectionRightProperty;
    }

    public BooleanProperty getEnableAll() {
        return enableAll;
    }

    public DoubleProperty getScaleLevelProperty(){
        return scaleLevelProperty;
    }

    public DoubleProperty getScreenWidthProperty(){
        return screenWidthProperty;
    }

    public DoubleProperty getScreenHeightProperty(){
        return screenHeightProperty;
    }

    public DoubleProperty getCurrentPageDefaultScaleLevelProperty(){
        return currentPageDefaultScaleLevelProperty;
    }

    public IntegerProperty getCurrentPageProperty(){
        return currentPageProperty;
    }

    public IntegerProperty getCurrentPagePreviewProperty(){
        return currentPagePreviewProperty;
    }

    public IntegerProperty getTotalPagesProperty(){
        return totalPagesProperty;
    }

    public Command getLoadPreviousPageCommand() {
        return loadPreviousPageCommand;
    }

    public Command getLoadNextPageCommand() {
        return loadNextPageCommand;
    }

    public Command getReadingDirectionCommand() {
        return readingDirectionCommand;
    }

    public Command getUpdatePagesPerViewPageCommand() {
        return updatePagesPerViewPageCommand;
    }

    public Command getZoomInCommand() {
        return zoomInCommand;
    }

    public Command getZoomOutCommand() {
        return zoomOutCommand;
    }

    public Command getApplyDefaultScaleCommand() {
        return applyDefaultScaleCommand;
    }

    public Command getOpenFileCompleteCommand() {
        return openFileCompleteCommand;
    }

    public Command getPreviousPageCommand() {
        return previousPageCommand;
    }

    public Command getNextPageCommand() {
        return nextPageCommand;
    }

    public Command getLoadSliderPageCommand() {
        return loadSliderPageCommand;
    }

    public Command getUpdatePreviewImageCommand() {
        return updatePreviewImageCommand;
    }

    private void previousPage() {
        logger.debug("Previous Page");

        fileService.updatePreviousPage(isTwoPagesProperty.getValue());
        currentPageProperty.setValue(fileService.getCurrentPageNumber());
    }

    private void nextPage() {
        logger.debug("Next Page");

        fileService.updateNextPage(isTwoPagesProperty.getValue());
        currentPageProperty.setValue(fileService.getCurrentPageNumber());
    }

    private void changeReadingDirection() {
        logger.debug("Change Reading Direction");

        readingDirectionRightProperty.setValue(!readingDirectionRightProperty.getValue());
    }

    private void setPagesPerView() {
        logger.debug("Set pages per view");

        leftImageProperty.set(new Image(DefaultValues.DEFAULT_IMAGE_PATH, true));
        rightImageProperty.set(new Image(DefaultValues.DEFAULT_IMAGE_PATH, true));

        isTwoPagesProperty.setValue(!isTwoPagesProperty.getValue());
    }

    private void zoomIn() {
        logger.debug("Zooming in image");

        if (scaleLevelProperty.getValue() <= DefaultValues.MAXIMUM_SCALE_LEVEL) {
            scaleLevelProperty.setValue(scaleLevelProperty.getValue() + DefaultValues.SCALE_DELTA);
        }
    }

    private void zoomOut() {
        logger.debug("Zooming out image");

        if (scaleLevelProperty.getValue() >= DefaultValues.MINIMUM_SCALE_LEVEL) {
            scaleLevelProperty.setValue(scaleLevelProperty.getValue() - DefaultValues.SCALE_DELTA);
        }
    }

    private void applyDefaultScale() {
        logger.debug("Applying default scale");

        scaleLevelProperty.set(currentPageDefaultScaleLevelProperty.getValue());
    }

    private void openFile() {
        logger.debug("Opening file");

        fileNameProperty.setValue(fileService.loadFile(filePathProperty.getValue()));
        enableAll.setValue(true);
    }

    private void loadImages () {
        logger.debug("Loading images");

        double maxHeight;

        if (isTwoPagesProperty.getValue()) {
            Dimension leftImageDimension = fileService.getCurrentVersoSize();
            Dimension rightImageDimension = fileService.getCurrentRectoSize();

            maxHeight = Math.max(leftImageDimension.height, rightImageDimension.height);

            leftImageProperty.setValue(new Image(fileService.getCurrentVerso(), true));
            rightImageProperty.setValue(new Image(fileService.getCurrentRecto(), true));

            leftImageDimensionProperty.setValue(leftImageDimension);
            rightImageDimensionProperty.setValue(rightImageDimension);
        } else {
            Dimension imageDimension = fileService.getCurrentPageSize();

            maxHeight = imageDimension.height;

            leftImageProperty.setValue(new Image(fileService.getCurrentPage(), true));
            leftImageDimensionProperty.setValue(imageDimension);
        }

        double defaultScaleLevel = (MathHelper.percentageOf(maxHeight, getScreenHeightProperty().getValue()))/100;
        currentPageDefaultScaleLevelProperty.setValue(defaultScaleLevel);
        if (scaleLevelProperty.get() == 1) {
            scaleLevelProperty.setValue(defaultScaleLevel);
        }

        enableNextPage.setValue(fileService.canChangeToNextPage(isTwoPagesProperty.getValue()));
    }

    private void updateTotalPages () {
        logger.debug("Updating total page");

        totalPagesProperty.setValue(fileService.getTotalPages());
    }

    private void updateCurrentPage() {
        logger.debug("Updating current page");

        currentPageProperty.setValue(fileService.getCurrentPageNumber());
    }

    private void updateCurrentFromUi() {
        logger.debug("Updating current page");

        fileService.setCurrentPage(currentPageProperty.getValue());
    }

    private void updatePreviewImage() {
        logger.debug("Updating preview image");

        String path = fileService.getCurrentPageByPageNumber(currentPagePreviewProperty.getValue());
        previewImageProperty.set(new Image(path, true));
    }
}