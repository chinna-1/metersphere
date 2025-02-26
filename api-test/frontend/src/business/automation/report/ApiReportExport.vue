<template>
  <ms-report-export-template :title="title" :report="report" :project-env-map="projectEnvMap"
                             :type="$t('report.api_test_report')">
    <ms-metric-chart :content="content" :is-export="true" :totalTime="totalTime" :report="report"/>
    <div class="scenario-result" v-for="(scenario, index) in content.scenarios" :key="index" :scenario="scenario">
      <div>
        <el-card>
          <template v-slot:header>
            {{ $t('api_report.scenario_name') }}：{{ scenario.name }}
          </template>
          <div class="ms-border clearfix" v-for="(request, index) in scenario.requestResults" :key="index"
               :request="request">

            <div class="request-top">
              <div>
                {{ getName(request.name) }}
              </div>
              <div class="url">
                {{ request.url }}
              </div>
            </div>
            <el-divider/>
            <div class="request-middle">
              <api-report-reqest-header-item :title="$t('api_test.request.method')">
                <span class="method"> {{ request.method }}</span>
              </api-report-reqest-header-item>

              <api-report-reqest-header-item :title="$t('api_report.response_time')">
                {{ request.responseResult.responseTime }} ms
              </api-report-reqest-header-item>

              <api-report-reqest-header-item :title="$t('api_report.latency')">
                {{ request.responseResult.latency }} ms
              </api-report-reqest-header-item>

              <api-report-reqest-header-item :title="$t('api_report.request_size')">
                {{ request.requestSize }} bytes
              </api-report-reqest-header-item>

              <api-report-reqest-header-item :title="$t('api_report.response_size')">
                {{ request.responseResult.responseSize }} bytes
              </api-report-reqest-header-item>

              <api-report-reqest-header-item :title="$t('api_report.error')">
                {{ request.error }}
              </api-report-reqest-header-item>

              <api-report-reqest-header-item :title="$t('api_report.assertions')">
                {{ request.passAssertions + " / " + request.totalAssertions }}
              </api-report-reqest-header-item>

              <api-report-reqest-header-item :title="$t('api_report.response_code')">
                {{ request.responseResult.responseCode }}
              </api-report-reqest-header-item>

              <api-report-reqest-header-item :title="$t('api_report.result')">
                <el-tag v-if="request.unexecute">{{
                    $t('api_test.home_page.detail_card.unexecute')
                  }}
                </el-tag>
                <el-tag v-else-if="!request.success && request.status && request.status==='PENDING'">{{
                    $t('api_test.home_page.detail_card.unexecute')
                  }}
                </el-tag>
                <el-tag v-else-if="request.errorCode" class="ms-test-error_code">
                  {{ $t('error_report_library.option.name') }}
                </el-tag>
                <el-tag size="mini" type="success" v-else-if="request.success">
                  {{ $t('api_report.success') }}
                </el-tag>
                <el-tag size="mini" type="danger" v-else>
                  {{ $t('api_report.fail') }}
                </el-tag>
              </api-report-reqest-header-item>
            </div>
          </div>
        </el-card>
      </div>
    </div>
  </ms-report-export-template>
</template>

<script>
import MsScenarioResult from "./components/ScenarioResult";
import MsRequestResultTail from "./components/RequestResultTail";
import ApiReportReqestHeaderItem from "./ApiReportReqestHeaderItem";
import MsMetricChart from "./components/MetricChart";
import MsReportTitle from "metersphere-frontend/src/components/report/MsReportTitle";
import MsReportExportTemplate from "metersphere-frontend/src/components/report/MsReportExportTemplate";
import MsAssertionResults from "../../../business/automation/report/components/AssertionResults"

export default {
  name: "MsApiReportExport",
  components: {
    MsReportExportTemplate,
    MsReportTitle, MsMetricChart, ApiReportReqestHeaderItem, MsRequestResultTail, MsScenarioResult, MsAssertionResults
  },
  props: {
    report: Object,
    content: Object,
    totalTime: Number,
    projectEnvMap: {},
    title: String
  },
  data() {
    return {}
  },
  methods: {
    getName(name) {
      if (name && name.indexOf("^@~@^") !== -1) {
        let arr = name.split("^@~@^");
        if (arr[arr.length - 1].indexOf("UUID=")) {
          return arr[arr.length - 1].split("UUID=")[0];
        }
        return arr[arr.length - 1];
      }
      return name;
    }
  }
}
</script>

<style scoped>

.scenario-result {
  margin-top: 20px;
  margin-bottom: 20px;
}

.method {
  color: #1E90FF;
  font-size: 14px;
  font-weight: 500;
}

.request-top, .request-bottom, .request-middle {
  margin-left: 20px;
}

.url {
  color: #409EFF;
  font-size: 14px;
  font-weight: bold;
  font-style: italic;
}

.el-card {
  border-style: none;
  padding: 10px 30px;
}

.request-top div {
  margin-top: 10px;
}

.ms-test-error_code {
  color: #F6972A;
  background-color: #FDF5EA;
  border-color: #FDF5EA;
}

</style>
