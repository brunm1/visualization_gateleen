package ch.bfh.ti.gapa.cli.printer;

import ch.bfh.ti.gapa.cli.exception.CommandLineException;
import ch.bfh.ti.gapa.cli.exception.CommandLineExceptionType;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.Charset;

/**
 * Verifies the output of the {@link InfoPrinterImpl} when
 * dummy input is provided.
 */
class InfoPrinterImplTest {
    private static InfoPrinter infoPrinter;
    private static PrintWriter pw;
    private static ByteArrayOutputStream out;

    @BeforeAll
    static void beforeAll() {
        //dummy options
        Options options = new Options();
        Option option = Option.builder("o")
                .desc("optionDesc")
                .longOpt("option")
                .build();
        options.addOption(option);

        //in-memory writer
        out = new ByteArrayOutputStream();
        pw = new PrintWriter(out);

        //mocked properties input stream
        String propertiesFile = "application.version=<version>";
        InputStream appProperties = new ByteArrayInputStream(propertiesFile.getBytes(Charset.forName("utf8")));

        //create with mocked dependencies
        infoPrinter = new InfoPrinterImpl(
                options,
                options,
                pw,
                "appDesc", "command", "appName", new CommandLineExceptionType[]{
                new CommandLineExceptionType() {
                    @Override
                    public int getCode() {
                        return 1;
                    }

                    @Override
                    public String getDesc() {
                        return "desc";
                    }
                }
        }, appProperties);
    }

    @BeforeEach
    void beforeEach() {
        out.reset();
    }

    @Test
    void printHelp() throws UnsupportedEncodingException {
        //print
        infoPrinter.printHelp();
        pw.flush();

        //verify
        Assertions.assertEquals("appDesc" + System.lineSeparator() + System.lineSeparator() +
                "usage: command [-o]" + System.lineSeparator() +
                " -o,--option   optionDesc" + System.lineSeparator() +
                System.lineSeparator() +
                "or" + System.lineSeparator() +
                        System.lineSeparator() +
                "usage: command [-o]" + System.lineSeparator() +
                " -o,--option   optionDesc" + System.lineSeparator() +
                        System.lineSeparator() +
                "Possible exit codes:" + System.lineSeparator() +
                "0 - Normal termination." + System.lineSeparator() +
                "1 - desc" + System.lineSeparator(), out.toString("utf8"));
    }

    @Test
    void printVersion() throws IOException {
        infoPrinter.printVersion();
        pw.flush();

        Assertions.assertEquals("appName - <version>" + System.lineSeparator(), out.toString("utf8"));
    }

    @Test
    void printCommandLineException() throws UnsupportedEncodingException {
        infoPrinter.printCommandLineException(new CommandLineException(new CommandLineExceptionType() {
            @Override
            public int getCode() {
                return 1;
            }

            @Override
            public String getDesc() {
                return "desc";
            }
        }, new Throwable("message")));
        pw.flush();

        Assertions.assertEquals("desc. Cause: message" + System.lineSeparator(), out.toString("utf8"));
    }
}