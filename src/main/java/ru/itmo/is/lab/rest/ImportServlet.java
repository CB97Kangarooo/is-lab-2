package ru.itmo.is.lab.rest;

import ru.itmo.is.lab.exception.ImportInputException;
import ru.itmo.is.lab.rest.error.ErrorResponses;
import ru.itmo.is.lab.service.ImportFileReader;
import ru.itmo.is.lab.service.ImportWriteService;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@WebServlet("/api/imports")
@MultipartConfig(maxFileSize = 1_048_576, maxRequestSize = 1_114_112, fileSizeThreshold = 65_536)
public class ImportServlet extends HttpServlet {

    @Inject
    private ImportFileReader reader;
    @Inject
    private ImportWriteService writer;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            List<ImportFileReader.Entry> entries = readUpload(request);
            int count = writer.save(entries);
            write(response, 201, Json.createObjectBuilder().add("addedCount", count).build());
        } catch (ImportInputException e) {
            write(response, 400, Json.createObjectBuilder()
                    .add("message", e.getField() + ": " + e.getMessage())
                    .add("fieldErrors", Json.createObjectBuilder().add(e.getField(), e.getMessage()))
                    .build());
        } catch (RuntimeException e) {
            writeError(response, ErrorResponses.resolve(e));
        }
    }

    private List<ImportFileReader.Entry> readUpload(HttpServletRequest request) {
        try {
            Part file = request.getPart("file");
            if (file == null || file.getSubmittedFileName() == null || file.getSize() == 0) {
                throw new ImportInputException("file", "Выберите непустой JSON-файл");
            }
            try (InputStream source = file.getInputStream()) {
                return reader.read(source);
            }
        } catch (IllegalStateException e) {
            throw new ImportInputException("file", "Размер файла не должен превышать 1 МиБ");
        } catch (IOException | ServletException e) {
            throw new ImportInputException("file", "Не удалось прочитать загруженный файл");
        }
    }

    private static void writeError(HttpServletResponse response, ErrorResponses.ApiError error) throws IOException {
        JsonObjectBuilder fields = Json.createObjectBuilder();
        error.getBody().getFieldErrors().forEach(fields::add);
        write(response, error.getStatus(), Json.createObjectBuilder()
                .add("message", error.getBody().getMessage()).add("fieldErrors", fields).build());
    }

    private static void write(HttpServletResponse response, int status, JsonObject body) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(body.toString());
    }
}
