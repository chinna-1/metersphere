package io.metersphere.plan.controller;

import io.metersphere.plan.dto.TestPlanLoadCaseDTO;
import io.metersphere.plan.request.LoadCaseReportRequest;
import io.metersphere.plan.service.TestPlanLoadCaseService;
import io.metersphere.service.BaseShareInfoService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/share/test/plan/load/case")
public class ShareTestPlanLoadCaseController {

    @Resource
    BaseShareInfoService baseShareInfoService;
    @Resource
    TestPlanLoadCaseService testPlanLoadCaseService;

    @GetMapping("/list/failure/{shareId}/{planId}")
    public List<TestPlanLoadCaseDTO> getLoadFailureCases(@PathVariable String shareId, @PathVariable String planId) {
        baseShareInfoService.validate(shareId, planId);
        return testPlanLoadCaseService.getFailureCases(planId);
    }

    @GetMapping("/list/all/{shareId}/{planId}")
    public List<TestPlanLoadCaseDTO> getLoadAllCases(@PathVariable String shareId, @PathVariable String planId) {
        baseShareInfoService.validate(shareId, planId);
        return testPlanLoadCaseService.getAllCases(planId);
    }

    @PostMapping("/report/exist/{shareId}")
    public Boolean isExistReport(@PathVariable String shareId, @RequestBody LoadCaseReportRequest request) {
        baseShareInfoService.validateExpired(shareId);
        return testPlanLoadCaseService.isExistReport(request);
    }
}
