package reader.utils

import mu.KotlinLogging
import org.apache.commons.io.FilenameUtils
import reader.exception.InsufficientDataException
import reader.model.Dimension
import reader.tasks.ImageResizeTask
import java.io.IOException
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.Executors
import javax.imageio.ImageIO
import javax.imageio.ImageReader
import javax.imageio.stream.ImageInputStream

private val logger = KotlinLogging.logger {}

/**
 * Generate the previews images on the temp folder. Receives the [Map] with the paths of the files and the [String] with
 * the path of the temp folder and returns a new map with the paths of the previews
 */
actual fun generatePreviewImages(pages: Map<Int, String>?, tempFolderPath: String): MutableMap<Int, String> {
    val startTime = System.currentTimeMillis()
    logger.info("Constructing preview images")

    if (pages == null || pages.isEmpty()) {
        throw InsufficientDataException("Insufficient data to generate previews")
    }

    val previews: MutableMap<Int, String> = mutableMapOf()

    val cleanedPath = URLDecoder.decode(cleanPath(tempFolderPath), StandardCharsets.UTF_8.toString())
    val folderPath: Path = Paths.get(cleanedPath)
    val previewFolderPath = generatePreviewPath(folderPath)
    if (previewFolderPath != null) {
        generatePreviewImagesSimultaneously(previewFolderPath, pages, previews)
    }

    val stopTime = System.currentTimeMillis()
    val elapsedTime = stopTime - startTime
    logger.info("Constructed preview images in {} milliseconds", elapsedTime)

    return previews
}

/**
 * Execute several process to simultaneously reduce the size of the images and store that on the temp folder
 */
private fun generatePreviewImagesSimultaneously (previewFolderPath: Path, pages: Map<Int, String>, previews: MutableMap<Int, String>) {
    val executor = Executors.newFixedThreadPool(50)

    for (index: Int  in pages.keys) {
        val previewPath = pages[index]?.let { generatePreviewImagePath(it, previewFolderPath) }
        if (previewPath != null) {
            previews[index] = previewPath
        }

        val task = ImageResizeTask(pages[index], previewPath, 10.0)
        executor.execute(task)
    }
}

/**
 * Generate the url for the preview image based on the path of the original image.
 * [path] is the path of the image ad [folderPath] is the path of the preview images folder inside the temp folder
 */
private fun generatePreviewImagePath(path: String, folderPath: Path?): String {
    if (path.isEmpty()) {
        return ""
    }
    if (folderPath == null) {
        return ""
    }

    var result = ""
    val imagePath = Paths.get(path)

    try {
        var extension = FilenameUtils.getExtension(imagePath.fileName.toString())
        if (extension.isNotEmpty()) {
            extension = ".$extension"
        }

        val baseName = URLDecoder.decode(FilenameUtils.getBaseName(imagePath.fileName.toString()), StandardCharsets.UTF_8.toString())

        val previewImagePath = Files.createTempFile(folderPath, baseName, extension)
        previewImagePath.toFile().deleteOnExit()

        result = previewImagePath.toUri().toURL().toString()
    } catch (e: IOException) {
        logger.error(e) { e.message }
    }

    return result
}

/**
 * Creates a preview folder inside the temp folder for the images. [path] is the url of the temp folder
 */
private fun generatePreviewPath(path: Path?): Path? {
    if (path == null) {
        return null
    }

    var previewFolderPath: Path? = null
    try {
        previewFolderPath = Files.createTempDirectory(path, "preview")
        previewFolderPath.toFile().deleteOnExit()
    } catch (e: IOException) {
        logger.error(e) { e.message }
    }

    return previewFolderPath
}

/**
 * Returns the [Dimension] of the file located on the given [path]
 */
actual fun getPageSize(path: String): Dimension {
    val dimension = Dimension()
    val cleanedPath = URLDecoder.decode(cleanPath(path), StandardCharsets.UTF_8.toString())
    val file: Path = Paths.get(cleanedPath)

    try {
        val iis: ImageInputStream = ImageIO.createImageInputStream(file.toFile())
        val readers: Iterator<ImageReader> = ImageIO.getImageReaders(iis)

        if (readers.hasNext()) {

            val reader: ImageReader = readers.next()
            reader.setInput(iis, true)
            val width = reader.getWidth(reader.minIndex)
            val height = reader.getHeight(reader.minIndex)

            dimension.height = height
            dimension.width = width
        }
    } catch (e: IOException) {
        logger.error(e) { e.message }
    }

    return dimension
}

/**
 * Remove the leading "file:" from the indicated path
 * @param path path to clean
 * @return the cleaned path
 */
fun cleanPath(path: String): String {
    return path.replace("file:/", "/")
}