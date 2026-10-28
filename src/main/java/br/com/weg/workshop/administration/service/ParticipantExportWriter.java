package br.com.weg.workshop.administration.service;

import br.com.weg.workshop.administration.dto.ParticipantResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

final class ParticipantExportWriter {

    private static final List<String> HEADERS = List.of(
            "registrationId", "userId", "name", "email", "wegRegistration",
            "registrationStatus", "paymentStatus", "attendanceStatus", "registeredAt", "attendanceMarkedAt"
    );

    private ParticipantExportWriter() {
    }

    static byte[] csv(List<ParticipantResponse> participants) {
        StringBuilder output = new StringBuilder("\uFEFF");
        appendCsvRow(output, HEADERS);
        participants.stream().map(ParticipantExportWriter::values).forEach(row -> appendCsvRow(output, row));
        return output.toString().getBytes(StandardCharsets.UTF_8);
    }

    static byte[] xlsx(List<ParticipantResponse> participants) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
                entry(zip, "[Content_Types].xml", contentTypes());
                entry(zip, "_rels/.rels", rootRelationships());
                entry(zip, "xl/workbook.xml", workbook());
                entry(zip, "xl/_rels/workbook.xml.rels", workbookRelationships());
                entry(zip, "xl/worksheets/sheet1.xml", worksheet(participants));
            }
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not create participant export.", exception);
        }
    }

    private static List<String> values(ParticipantResponse participant) {
        return List.of(
                participant.registrationId().toString(),
                participant.userId().toString(),
                participant.name(),
                participant.email(),
                value(participant.wegRegistration()),
                participant.registrationStatus(),
                participant.paymentStatus(),
                value(participant.attendanceStatus()),
                participant.registeredAt().toString(),
                participant.attendanceMarkedAt() == null ? "" : participant.attendanceMarkedAt().toString()
        );
    }

    private static void appendCsvRow(StringBuilder output, List<String> values) {
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) output.append(',');
            output.append('"').append(values.get(index).replace("\"", "\"\"")).append('"');
        }
        output.append("\r\n");
    }

    private static String worksheet(List<ParticipantResponse> participants) {
        List<List<String>> rows = new ArrayList<>();
        rows.add(HEADERS);
        participants.stream().map(ParticipantExportWriter::values).forEach(rows::add);
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
                .append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>");
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            xml.append("<row r=\"").append(rowIndex + 1).append("\">");
            List<String> row = rows.get(rowIndex);
            for (int columnIndex = 0; columnIndex < row.size(); columnIndex++) {
                xml.append("<c r=\"").append(columnName(columnIndex)).append(rowIndex + 1)
                        .append("\" t=\"inlineStr\"><is><t xml:space=\"preserve\">")
                        .append(xml(row.get(columnIndex))).append("</t></is></c>");
            }
            xml.append("</row>");
        }
        return xml.append("</sheetData></worksheet>").toString();
    }

    private static String columnName(int index) {
        StringBuilder name = new StringBuilder();
        for (int value = index + 1; value > 0; value = (value - 1) / 26) {
            name.insert(0, (char) ('A' + (value - 1) % 26));
        }
        return name.toString();
    }

    private static String xml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }

    private static void entry(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static String contentTypes() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
                + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                + "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
                + "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
                + "</Types>";
    }

    private static String rootRelationships() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>"
                + "</Relationships>";
    }

    private static String workbook() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
                + "<sheets><sheet name=\"Participants\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>";
    }

    private static String workbookRelationships() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>"
                + "</Relationships>";
    }

    private static String value(String value) {
        return value == null ? "" : value;
    }
}
