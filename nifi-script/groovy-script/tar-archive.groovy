// FIle Name : tar-archive.groovy
// Type: Groovy Script
// Description: This script creates a tar archive from the specified files and sends the archive file to the success relationship.
// Author: Seoktae Jeong
// Version: 1.0
// Date: 2024-03-30
// Input(Attributs) : filesToArchive, tarFilePath
//  - filesToArchive: The paths of the files to be archived. Multiple paths are separated by commas.
//  - tarFilePath: The path of the tar archive file to be created.
// Process: Create a tar archive from the specified files and send the archive file to the success relationship.
// Output(Attributs) : N/A
// Usage: This script is used in the ExecuteScript processor to create a tar archive from the specified files and send the archive file to the success relationship.

import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.nifi.processor.io.StreamCallback
import java.nio.file.Files
import java.nio.file.Paths
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

// ExecuteScript Processor에서 제공하는 FlowFile을 처리하는 로직
flowFile = session.get()
if (!flowFile) return

try {
    // FlowFile 속성에서 필요한 정보를 읽음
    def filesToArchivePath = flowFile.getAttribute('filesToArchive')
    def tarFilePath = flowFile.getAttribute('tarFilePath')

    // tar 파일 스트림을 생성
    FileOutputStream fos = new FileOutputStream(new File(tarFilePath))
    TarArchiveOutputStream tarOut = new TarArchiveOutputStream(fos)

    // 지정된 파일들을 tar 압축
    filesToArchivePath.split(',').each { filePath ->
        File file = new File(filePath.trim())
        if (file.exists()) {
            tarOut.putArchiveEntry(new TarArchiveEntry(file, file.name))
            FileInputStream fis = new FileInputStream(file)
            fis.transferTo(tarOut)
            fis.close()
            tarOut.closeArchiveEntry()
        } else {
            log.error("File not found: $filePath")
        }
    }
    tarOut.finish()
    tarOut.close()
    fos.close()

    // 성공적으로 처리된 FlowFile을 전송
    session.transfer(flowFile, REL_SUCCESS)
} catch (Exception e) {
    log.error("Error while creating tar archive: ${e.message}", e)
    session.transfer(flowFile, REL_FAILURE)
}
