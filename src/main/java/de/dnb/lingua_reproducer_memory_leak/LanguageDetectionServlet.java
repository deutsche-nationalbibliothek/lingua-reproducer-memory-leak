package de.dnb.lingua_reproducer_memory_leak;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.github.pemistahl.lingua.api.Language;
import com.github.pemistahl.lingua.api.LanguageDetector;
import com.github.pemistahl.lingua.api.LanguageDetectorBuilder;

@WebServlet("/detectLanguage")
public class LanguageDetectionServlet extends HttpServlet {

    private static final long serialVersionUID = 2280806903904633128L;
    private static final Logger LOGGER = Logger.getLogger(LanguageDetectionServlet.class.getName());

    private static LanguageDetector detectorForAllLanguages;
    private static long millis;

    @Override
    public void init() throws ServletException {
        Instant before = Instant.now();
        LOGGER.info("Started building detector for all languages");
        detectorForAllLanguages = LanguageDetectorBuilder.fromAllLanguages().withPreloadedLanguageModels().build();
        Instant after = Instant.now();
        millis = Duration.between(before, after).toMillis();
        LOGGER.log(Level.INFO, "Finished building detector for all languages in {0}ms", millis);
    }

    @Override
    public void destroy() {
        super.destroy();
        // could do manual cleanup here if there was a method to call
        LOGGER.info("Destroying servlet");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.getWriter().write("LanguageDetector was initialized in " + millis + "ms");
        resp.getWriter().flush();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ServletInputStream inputStream = req.getInputStream();
        byte[] buffer = new byte[1024 * 1024 * 5];
        int readBytes = inputStream.readLine(buffer, 0, buffer.length);
        String msg = null;
        if (readBytes > 0) {
            byte[] stringBytes = new byte[readBytes];
            System.arraycopy(buffer, 0, stringBytes, 0, readBytes);
            String input = new String(stringBytes, StandardCharsets.UTF_8);
            Language detectedLanguage = detectorForAllLanguages.detectLanguageOf(input);
            msg = "Detected language " + detectedLanguage.toString() + System.lineSeparator();
            LOGGER.info(msg);
        } else {
            msg = "No language detected because no input was found" + System.lineSeparator();
        }
        PrintWriter writer = resp.getWriter();
        writer.write(msg);
        writer.flush();
    }

}
