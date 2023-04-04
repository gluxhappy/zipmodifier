import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipModifier {

    //language=TEXT
    private static final String USAGE = "Usage: \n" +
            "  --in, input zip file, --in=a.zip\n" +
            "  --out, output zip file, --out=b.zip\n" +
            "  --add, add file to zip, --add=add.txt:/zip/entry/home/add.txt\n" +
            "  --rm, remove a file, --rm=/zip/entry/rm.txt\n" +
            "  -v, show verbose logs\n" +
            "  --help, show help.\n" +
            "  ";

    public static void main(String[] args) {
        try {
            execute(args);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(USAGE);
            System.exit(1);
        }
    }

    public static void execute(String[] args) throws IOException {

        File inZipFile = null;
        File outZipFile = null;
        Map<String, File> toAddFiles = new HashMap<>();
        Set<String> toRemoveFiles = new HashSet<>();

        BiFunction<String, String, String> parser = (arg, pattern) -> {
            if (arg.startsWith(pattern)) {
                return arg.substring(pattern.length()).trim();
            } else {
                return null;
            }
        };

        for (String arg : args) {
            String value;
            if ((value = parser.apply(arg, "--in=")) != null) {
                inZipFile = new File(value);
            } else if ((value = parser.apply(arg, "--out=")) != null) {
                outZipFile = new File(value);
            } else if ((value = parser.apply(arg, "--add=")) != null) {
                String[] add = value.split(":");
                toAddFiles.put(add[1], new File(add[0]));
            } else if ((value = parser.apply(arg, "--rm=")) != null) {
                toRemoveFiles.add(value);
            } else if ("--help".equals(arg)) {
                System.out.println(USAGE);
                return;
            } else {
                throw new IllegalArgumentException("Unknown command line arg " + arg);
            }
        }

        if (null == inZipFile || !inZipFile.exists()) {
            throw new IllegalArgumentException("Input file not exists. ");
        }
        if (null == outZipFile) {
            throw new IllegalArgumentException("Output file not set. ");
        }
        byte[] buffer = new byte[1024 * 4 * 128];
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(inZipFile))) {
            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outZipFile))) {
                ZipEntry entry;
                while (null != (entry = zis.getNextEntry())) {
                    if (toRemoveFiles.contains(entry.getName())) {
                        log("Removing " + entry.getName());
                    } else {
                        log("Adding " + entry.getName() + " size=" + entry.getSize() + " => cszie=" + entry.getCompressedSize() + " method=" + entry.getMethod());
                        if (entry.getMethod() == ZipEntry.STORED) {

                            zos.putNextEntry(entry);
                        } else {
                            ZipEntry newEntry = new ZipEntry(entry.getName());
                            newEntry.setTime(entry.getTime());
                            newEntry.setMethod(entry.getMethod());
                            newEntry.setExtra(entry.getExtra());
                            newEntry.setComment(entry.getComment());
                            zos.putNextEntry(entry);
                        }
                        copyLarge(zis, zos, buffer);
                        zos.closeEntry();
                    }
                }
                for (Map.Entry<String, File> stringFileEntry : toAddFiles.entrySet()) {
                    String name = stringFileEntry.getKey();
                    File input = stringFileEntry.getValue();
                    ZipEntry inputEntry = new ZipEntry(name);
                    zos.putNextEntry(inputEntry);
                    try (FileInputStream fis = new FileInputStream(input)) {
                        copyLarge(fis, zos, buffer);
                    }
                    zos.closeEntry();
                }
            }
        }
    }

    public static long copyLarge(InputStream input, OutputStream output, byte[] buffer) throws IOException {
        long count = 0L;
        int n;
        for (; -1 != (n = input.read(buffer)); count += n) {
            output.write(buffer, 0, n);
        }
        log("n=" + n + "; count=" + count);
        return count;
    }

    public static void log(String msg) {
        System.out.println(msg);
    }
}
