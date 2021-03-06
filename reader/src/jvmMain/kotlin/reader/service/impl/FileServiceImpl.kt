package reader.service.impl

import org.apache.commons.io.FilenameUtils
import reader.filereader.FileReaderType
import reader.service.FileService

/**
 * Service class to serve as interface between the graphical part and the reader
 */
actual object FileServiceImpl : FileService() {

    /**
     * Returns the name of the file on the given [path]
     */
    override fun getFilenameFromPath(path: String): String {
        return FilenameUtils.getName(path)
    }

    /**
     * Returns the file type from the give [path]
     */
    override fun getFileReaderTypeFromPath(path: String): FileReaderType? {
        val extension = FilenameUtils.getExtension(path).toLowerCase()
        var type: FileReaderType? = null

        when (extension) {
            "cbr" -> type = FileReaderType.CBR
            "cbz" -> type = FileReaderType.CBZ
        }

        return type;
    }
}