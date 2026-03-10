package com.example.clinicalextraction.controller;

import com.example.clinicalextraction.dto.Group;
import com.example.clinicalextraction.dto.Relation;
import com.example.clinicalextraction.service.ExtractionService;
import com.example.clinicalextraction.service.step1.Step1ExtractionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

@RestController
public class ExtractionController {

    private final ExtractionService extractionService;

    private final Step1ExtractionService step1ExtractionService;

    public ExtractionController(ExtractionService extractionService, Step1ExtractionService step1ExtractionService) {
        this.extractionService = extractionService;
        this.step1ExtractionService = step1ExtractionService;
    }

    @GetMapping("/extract")
    public ResponseEntity<List<Group>> extractClinicalData(@RequestParam("file") MultipartFile file) throws IOException, SAXException {
        Path xml = Files.createTempFile("clinical-", ".xml");

        Files.copy(file.getInputStream(), xml, StandardCopyOption.REPLACE_EXISTING);

        List<Group> groups = extractionService.extractRelations(xml.toString());

        Files.deleteIfExists(xml);

        return ResponseEntity.ok(groups);
    }

    @GetMapping("/step1")
    public ResponseEntity<List<Group>> extractSingleGroup(@RequestParam("file") MultipartFile file) throws IOException, SAXException {
        Path xml = Files.createTempFile("clinical-", ".xml");

        Files.copy(file.getInputStream(), xml, StandardCopyOption.REPLACE_EXISTING);

        List<Group> groups = step1ExtractionService.getData(xml.toString());

        return ResponseEntity.ok(groups);

    }
}
