package com.example.advisor.judgement;

import java.util.List;

public record CsvImportResponse(
        List<StudentLedgerResult> ledgers
) {
}

