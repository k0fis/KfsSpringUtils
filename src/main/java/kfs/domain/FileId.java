package kfs.domain;

import javax.persistence.Embeddable;

/**
 *
 * @author pavedrim
 */
@Embeddable
public class FileId {
    
    private String fileName;
    private Long fileIndex;

    /**
     * @return the fileName
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * @param fileName the fileName to set
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * @return the fileIndex
     */
    public Long getFileIndex() {
        return fileIndex;
    }

    /**
     * @param fileIndex the fileIndex to set
     */
    public void setFileIndex(Long fileIndex) {
        this.fileIndex = fileIndex;
    }
}
