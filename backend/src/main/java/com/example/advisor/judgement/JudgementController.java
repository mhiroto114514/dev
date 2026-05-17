package com.example.advisor.judgement;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/judgements")
public class JudgementController {

    private final JudgementService judgementService;
    private final CsvImportService csvImportService;

    public JudgementController(JudgementService judgementService, CsvImportService csvImportService) {
        this.judgementService = judgementService;
        this.csvImportService = csvImportService;
    }

    @PostMapping
    public JudgementResponse judge(@Valid @RequestBody JudgementRequest request) {
        return judgementService.judge(request);
    }

    @PostMapping("/csv-import")
    @ResponseStatus(HttpStatus.OK)
    public CsvImportResponse csvImport(@RequestParam("file") MultipartFile file) {
        return csvImportService.importAndJudge(file);
    }
}
