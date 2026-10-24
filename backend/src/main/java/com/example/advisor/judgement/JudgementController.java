package com.example.advisor.judgement;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/judgements")
public class JudgementController {

    private final CsvImportService csvImportService;

    public JudgementController(CsvImportService csvImportService) {
        this.csvImportService = csvImportService;
    }

    @PostMapping("/csv-import")
    public CsvImportResponse csvImport(@RequestParam("file") MultipartFile file) {
        return csvImportService.importAndJudge(file);
    }

    @PostMapping("/csv-preview")
    public CsvImportResponse csvPreview(@RequestParam("files") MultipartFile[] files) {
        return csvImportService.preview(files);
    }
}
