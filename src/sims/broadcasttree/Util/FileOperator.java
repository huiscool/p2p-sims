package sims.broadcasttree.Util;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class FileOperator {
    private static final String filePath = "/Users/jiahaoliu/Desktop/proj/PeerSim/src/jiahaoliu/example/broadcasttree/results/result.txt";

    private static FileOutputStream fos;

    static {
        try {
            fos = new FileOutputStream(filePath, false);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void writeToFile(String str) {
        try {
            fos.write(str.getBytes());
            fos.write("\r\n".getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<String> getLines() {
        try {
            return Files.readAllLines(Paths.get(filePath));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public static void closeFiles() {
        try {
            if (fos != null)
                fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) throws IOException {
        writeToFile("hello");
        writeToFile("world");
    }

}
