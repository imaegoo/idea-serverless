package com.serverless.middle.utils;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.zip.GZIPOutputStream;

public class TarUtils {

    /**
     * compress file to .tar.gz
     *
     * @param sourceFolder
     * @param tarGzPath
     * @throws IOException
     */
    public static void compressTar(String sourceFolder, String tarGzPath) throws IOException {
        createTarFile(sourceFolder, tarGzPath);
    }

    private static void createTarFile(String sourceFolder, String tarGzPath) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(tarGzPath);
             GZIPOutputStream gos = new GZIPOutputStream(new BufferedOutputStream(fos));
             TarArchiveOutputStream tarOs = new TarArchiveOutputStream(gos);) {
            // 若不设置此模式，当文件名超过 100 个字节时会抛出异常，异常大致如下：
            // is too long ( > 100 bytes)
            tarOs.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
            addFilesToTarGZ(sourceFolder, "", tarOs);
        } catch (IOException e) {
            throw e;
        }
    }

    public static void addFilesToTarGZ(String filePath, String parent, TarArchiveOutputStream tarArchive) throws IOException {
        File file = new File(filePath);
        // Create entry name relative to parent file path
        String entryName = parent + file.getName();
        // 添加 tar ArchiveEntry
        tarArchive.putArchiveEntry(new TarArchiveEntry(file, entryName));
        if (file.isFile()) {
            FileInputStream fis = new FileInputStream(file);
            BufferedInputStream bis = new BufferedInputStream(fis);
            // 写入文件
            IOUtils.copy(bis, tarArchive);
            tarArchive.closeArchiveEntry();
            bis.close();
        } else if (file.isDirectory()) {
            // 因为是个文件夹，无需写入内容，关闭即可
            tarArchive.closeArchiveEntry();
            // 读取文件夹下所有文件
            for (File f : file.listFiles()) {
                // 递归
                addFilesToTarGZ(f.getAbsolutePath(), entryName + File.separator, tarArchive);
            }
        }
    }

    public static void unCompressTar(String sourceTarGz, String target) throws IOException {
        File sourceTarGzFile = new File(sourceTarGz);
        File targetDir = new File(target);
        // decompressing *.tar.gz files to tar
        try (GzipCompressorInputStream gis = new GzipCompressorInputStream(Files.newInputStream(sourceTarGzFile.toPath()));
             TarArchiveInputStream tai = new TarArchiveInputStream(gis)) {
            TarArchiveEntry entry;
            // 将 tar 文件解压到 targetDir 目录下
            // 将 tar.gz文件解压成tar包,然后读取tar包里的文件元组，复制文件到指定目录
            while ((entry = tai.getNextTarEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                File targetFile = new File(targetDir, entry.getName());
                File parent = targetFile.getParentFile();
                if (!parent.exists()) {
                    parent.mkdirs();
                }
                // 将文件写出到解压的目录
                IOUtils.copy(tai, Files.newOutputStream(targetFile.toPath()));
            }
        } catch (IOException e) {
            throw e;
        }
    }
}
