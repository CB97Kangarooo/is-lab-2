package ru.itmo.is.lab.rest;

import ru.itmo.is.lab.auth.SessionUser;
import ru.itmo.is.lab.exception.ImportInputException;
import ru.itmo.is.lab.rest.error.ErrorResponses;
import ru.itmo.is.lab.service.ImportFileReader;
import ru.itmo.is.lab.service.ImportHistoryService;
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
    @Inject
    private ImportHistoryService history;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            int offset = QueryParams.intValue("offset", request.getParameter("offset"), 0);
            if (offset < 0) {
                throw new ImportInputException("offset", "Ожидается неотрицательное число");
            }
            write(response, 200, history.page(SessionUser.current(request), offset));
        } catch (ImportInputException e) {
            writeInputError(response, e, null);
        } catch (RuntimeException e) {
            writeError(response, ErrorResponses.resolve(e), null);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Long id = null;
        int count;
        try {
            id = history.start(SessionUser.current(request));
            List<ImportFileReader.Entry> entries = readUpload(request);
            count = writer.save(id, entries);
        } catch (ImportInputException e) {
            history.fail(id);
            writeInputError(response, e, id);
            return;
        } catch (RuntimeException e) {
            if (id != null) {
                history.fail(id);
            }
            writeError(response, ErrorResponses.resolve(e), id);
            return;
        }
        write(response, 201, Json.createObjectBuilder().add("id", id).add("addedCount", count).build());
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

    private static void writeInputError(HttpServletResponse response, ImportInputException error, Long id)
            throws IOException {
        JsonObjectBuilder body = Json.createObjectBuilder()
                .add("message", error.getField() + ": " + error.getMessage())
                .add("fieldErrors", Json.createObjectBuilder().add(error.getField(), error.getMessage()));
        write(response, 400, withOperationId(body, id));
    }

    private static void writeError(HttpServletResponse response, ErrorResponses.ApiError error, Long id)
            throws IOException {
        JsonObjectBuilder fields = Json.createObjectBuilder();
        error.getBody().getFieldErrors().forEach(fields::add);
        JsonObjectBuilder body = Json.createObjectBuilder()
                .add("message", error.getBody().getMessage()).add("fieldErrors", fields);
        write(response, error.getStatus(), withOperationId(body, id));
    }

    private static JsonObject withOperationId(JsonObjectBuilder body, Long id) {
        if (id != null) {
            body.add("id", id);
        }
        return body.build();
    }

    private static void write(HttpServletResponse response, int status, JsonObject body) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(body.toString());
    }
}
