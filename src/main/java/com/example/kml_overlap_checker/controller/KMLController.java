package com.example.kml_overlap_checker.controller;

import com.example.kml_overlap_checker.service.KMLOverlapService;
// import io.swagger.annotations.Api;
// import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/kml")
// @Api(tags = "KML Overlap Checker")
public class KMLController {

    @Autowired
    private KMLOverlapService kmlOverlapService;

    @GetMapping("/check-partial-overlaps")
    // @ApiOperation("Check for partial overlaps between KML files in a folder")
    public List<String> checkOverlaps(@RequestParam String folderPath) {
        return kmlOverlapService.detectPartialOverlaps(folderPath);
    }

    @GetMapping("/overlap-report")
    public Map<String, Map<String, String>> getOverlapReport(@RequestParam String folderPath) {
        return kmlOverlapService.findGroupedOverlaps(folderPath);
    }
}
