package com.mtur.je.compression;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.xerial.snappy.Snappy;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

@Slf4j
public class FileAppender {

    public byte[] generate(int maxBytes, String dictName){
        List<String> dict = readDictionary(dictName);

        StringBuffer sb = new StringBuffer(maxBytes);
        Random rnd = new Random(System.currentTimeMillis());
        int writtenBytes = 0;

        while (writtenBytes < maxBytes) {
            int idx = rnd.nextInt(dict.size());
            String word = dict.get(idx);

            if (word.length() < (maxBytes - writtenBytes + 1)) {
                sb.append(word).append(" ");
                writtenBytes += word.length() + 1;
            } else {
                int reducedSize = Math.min(word.length(), maxBytes - writtenBytes);
                sb.append(word, 0, reducedSize - 1);
                writtenBytes += reducedSize;
            }
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public List<String> readDictionary(String dictName) {
        int bufferSize = 8192;
        int expectedLinesCount = 194433;
        List<String> dict = new ArrayList<>(expectedLinesCount);

        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try (InputStream is = cl.getResourceAsStream(dictName);
             InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
             BufferedReader br = new BufferedReader(isr, bufferSize)) {
            String line;
            int count = 0;
            while ((line = br.readLine()) != null) {
                count++;
                dict.add(line);
            }

            log.info("Loaded {} lines with words.", count);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return dict;

    }

    private void persistPlain(byte[] data) {
        int bufferSize = 8192;

        StopWatch sw = StopWatch.createStarted();

        String fileName = "/Users/mturetskiy/tmp/data.out";
        File file = new File(fileName);
        try (FileOutputStream fos = new FileOutputStream(file);
             BufferedOutputStream bos = new BufferedOutputStream(fos)) {

            int pos = 0;
            int writtenSize = 0;

            while (writtenSize < data.length) {
                int actualSize = Math.min(bufferSize, data.length - writtenSize);
                byte[] buffer = Arrays.copyOfRange(data, pos, pos + actualSize);
                bos.write(buffer);
                writtenSize += actualSize;
                pos += actualSize;
            }

            bos.flush();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            sw.stop();
            log.info("[Plain] Writing time: {} ms", sw.getTime(TimeUnit.MILLISECONDS));
        }
    }

    private void persistGzip(byte[] data) {
        int bufferSize = 8192;

        StopWatch sw = StopWatch.createStarted();

        String fileName = "/Users/mturetskiy/tmp/data.out.gz";
        File file = new File(fileName);
        try (FileOutputStream fos = new FileOutputStream(file);
             GZIPOutputStream gzos = new GZIPOutputStream(fos, bufferSize, false)) {
            gzos.write(data);
            gzos.flush();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            sw.stop();
            log.info("[Gzip] Writing time: {} ms", sw.getTime(TimeUnit.MILLISECONDS));
        }
    }

    private void persistSnappy(byte[] data) {
        int bufferSize = 8192;

        StopWatch sw = StopWatch.createStarted();

        String fileName = "/Users/mturetskiy/tmp/data.out.snappy.gz";
        File file = new File(fileName);
        try (FileOutputStream fos = new FileOutputStream(file);
             BufferedOutputStream bos = new BufferedOutputStream(fos, bufferSize)) {
            byte[] compressed = Snappy.compress(data);
            bos.write(compressed);
            bos.flush();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            sw.stop();
            log.info("[Snappy] Writing time: {} ms", sw.getTime(TimeUnit.MILLISECONDS));
        }
    }

    public static void main(String[] args) {
        FileAppender appender = new FileAppender();
        log.info("Generating data ...");
        byte[] data = appender.generate(1024 * 1024 * 1024, "log_words.txt");
        log.info("Generation is done. Prepared: {} mb. Writing to plan file.", data.length / 1024 / 1024);
        appender.persistPlain(data);
        appender.persistGzip(data);
        appender.persistSnappy(data);
        log.info("Done");
    }
}
