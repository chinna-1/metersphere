package io.metersphere.api.dto.datacount.response;

import io.metersphere.api.dto.datacount.ApiDataCountResult;
import io.metersphere.commons.constants.RequestTypeConstants;
import io.metersphere.commons.enums.ApiReportStatus;
import io.metersphere.commons.enums.ApiTestDataStatus;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * 接口数据统计返回
 */
@Getter
@Setter
public class ApiDataCountDTO {

    /**
     * 接口统计
     */
    private long allApiDataCountNumber = 0;
    /**
     * http接口统计
     */
    private long httpApiDataCountNumber = 0;
    /**
     * rpc接口统计
     */
    private long rpcApiDataCountNumber = 0;
    /**
     * tcp接口统计
     */
    private long tcpApiDataCountNumber = 0;
    /**
     * sql接口统计
     */
    private long sqlApiDataCountNumber = 0;

    private String httpCountStr = "";
    private String rpcCountStr = "";
    private String tcpCountStr = "";
    private String sqlCountStr = "";

    /**
     * 本周新增数量
     */
    private long thisWeekAddedCount = 0;
    /**
     * 本周执行数量
     */
    private long thisWeekExecutedCount = 0;
    /**
     * 历史总执行数量
     */
    private long executedCount = 0;

    /**
     * 进行中
     */
    private long runningCount = 0;
    /**
     * 未开始
     */
    private long notStartedCount = 0;
    /**
     * 已完成
     */
    private long finishedCount = 0;
    /**
     * 未覆盖
     */
    private long uncoverageCount = 0;
    /**
     * 已覆盖
     */
    private long coverageCount = 0;
    /**
     * 未执行
     */
    private long unexecuteCount = 0;
    /**
     * 执行失败
     */
    private long executionFailedCount = 0;
    /**
     * 执行通过
     */
    private long executionPassCount = 0;
    /**
     * 执行误报
     */
    private long fakeErrorCount = 0;
    /**
     * 失败
     */
    private long failedCount = 0;
    /**
     * 成功
     */
    private long successCount = 0;

    /**
     * 完成率
     */
    private String completionRage = " 0%";
    /**
     * 覆盖率
     */
    private String coverageRage = " 0%";
    /**
     * 通过率
     */
    private String passRage = " 0%";
    /**
     * 成功率
     */
    private String successRage = " 0%";

    /**
     * 接口覆盖率
     */
    private String interfaceCoverage = " 0%";

    /**
     * 对Protocal视角对查询结果进行统计
     *
     * @param countResultList 查询参数
     */
    public void countProtocal(List<ApiDataCountResult> countResultList) {
        for (ApiDataCountResult countResult :
                countResultList) {
            switch (countResult.getGroupField().toUpperCase()) {
                case RequestTypeConstants.DUBBO:
                    this.rpcApiDataCountNumber += countResult.getCountNumber();
                    break;
                case RequestTypeConstants.HTTP:
                    this.httpApiDataCountNumber += countResult.getCountNumber();
                    break;
                case RequestTypeConstants.SQL:
                    this.sqlApiDataCountNumber += countResult.getCountNumber();
                    break;
                case RequestTypeConstants.TCP:
                    this.tcpApiDataCountNumber += countResult.getCountNumber();
                    break;
                default:
                    break;
            }
            allApiDataCountNumber += countResult.getCountNumber();
        }
    }


    /**
     * 对Status视角对查询结果进行统计
     *
     * @param countResultList 查询参数
     */
    public void countStatus(List<ApiDataCountResult> countResultList) {
        for (ApiDataCountResult countResult :
                countResultList) {
            if (StringUtils.equalsIgnoreCase(countResult.getGroupField(), ApiTestDataStatus.UNDERWAY.getValue())) {
                //运行中
                this.runningCount += countResult.getCountNumber();
            } else if (StringUtils.equalsIgnoreCase(countResult.getGroupField(), ApiTestDataStatus.COMPLETED.getValue())) {
                //已完成
                this.finishedCount += countResult.getCountNumber();
            } else if (StringUtils.equalsIgnoreCase(countResult.getGroupField(), ApiTestDataStatus.PREPARE.getValue())) {
                this.notStartedCount += countResult.getCountNumber();
            }
        }
    }

    public void countApiCoverage(List<ApiDataCountResult> countResultList) {

        for (ApiDataCountResult countResult : countResultList) {
            if (StringUtils.equalsIgnoreCase(countResult.getGroupField(), "coverage")) {
                this.coverageCount += countResult.getCountNumber();
            } else if (StringUtils.equalsIgnoreCase(countResult.getGroupField(), "uncoverage")) {
                this.uncoverageCount += countResult.getCountNumber();
            }
        }
    }

    public void countRunResult(List<ApiDataCountResult> countResultByRunResult) {
        for (ApiDataCountResult countResult : countResultByRunResult) {
            if (StringUtils.equalsAnyIgnoreCase(countResult.getGroupField(), "notRun", ApiReportStatus.PENDING.name()) || StringUtils.isEmpty(countResult.getGroupField())) {
                this.unexecuteCount += countResult.getCountNumber();
            } else if (ApiReportStatus.ERROR.name().equals(countResult.getGroupField())) {
                this.executionFailedCount += countResult.getCountNumber();
            } else if (StringUtils.equalsAnyIgnoreCase(countResult.getGroupField(), ApiReportStatus.FAKE_ERROR.name())) {
                this.fakeErrorCount += countResult.getCountNumber();
            } else {
                this.executionPassCount += countResult.getCountNumber();
            }
        }
    }

    public void countScheduleExecute(List<ApiDataCountResult> allExecuteResult) {
        for (ApiDataCountResult countResult : allExecuteResult) {
            if (StringUtils.equalsIgnoreCase(countResult.getGroupField(), ApiReportStatus.SUCCESS.name())) {
                this.successCount += countResult.getCountNumber();
                this.executedCount += countResult.getCountNumber();
            } else if (StringUtils.equalsAnyIgnoreCase(countResult.getGroupField(), ApiReportStatus.ERROR.name())) {
                this.failedCount += countResult.getCountNumber();
                this.executedCount += countResult.getCountNumber();
            } else if (StringUtils.equalsAnyIgnoreCase(countResult.getGroupField(), ApiReportStatus.FAKE_ERROR.name())) {
                this.fakeErrorCount += countResult.getCountNumber();
                this.executedCount += countResult.getCountNumber();
            }
        }
    }

    public long getAllExecutedResultCount() {
        return this.unexecuteCount + this.executionFailedCount + this.fakeErrorCount + this.executionPassCount;
    }
}
