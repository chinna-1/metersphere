<template>
  <el-table border
            v-loading="result"
            highlight-current-row
            @row-click="handleRowClick"
            :data="tableData"
            class="adjust-table table-content"
            height="300px">
    <el-table-column prop="index"
                     width="60"
                     :label="$t('api_test.home_page.running_task_list.table_coloum.index')"
                     show-overflow-tooltip/>
    <el-table-column
      prop="swaggerUrl"
      :label="$t('swaggerUrl')"
      min-width="170" show-overflow-tooltip>
    </el-table-column>
    <el-table-column prop="modulePath" :label="$t('导入模块')"
                     min-width="100"
                     show-overflow-tooltip/>
    <el-table-column prop="rule" label="同步规则"
                     min-width="140"
                     show-overflow-tooltip/>
    <el-table-column width="100" :label="$t('api_test.home_page.running_task_list.table_coloum.task_status')">
      <template v-slot:default="scope">
        <div>
          <el-switch
            v-model="scope.row.enable"
            class="captcha-img"
            @click.native="closeTaskConfirm(scope.row)"
          ></el-switch>
        </div>
      </template>
    </el-table-column>
    <el-table-column width="170" :label="$t('api_test.home_page.running_task_list.table_coloum.next_execution_time')">
      <template v-slot:default="scope">
        <span>{{ scope.row.nextExecutionTime | datetimeFormat }}</span>
      </template>
    </el-table-column>
    <el-table-column width="100" label="操作">
      <template v-slot:default="scope">
        <el-button
          type="danger"
          icon="el-icon-delete"
          size="mini"
          @click.native.prevent="deleteRowTask(scope.row)"
        ></el-button>
      </template>
    </el-table-column>
  </el-table>
</template>

<script>
import {definitionSwitch, delDefinitionSchedule, scheduleTask} from "@/api/definition";
import {operationConfirm} from "metersphere-frontend/src/utils";
import {getCurrentProjectID} from "metersphere-frontend/src/utils/token";

export default {
  name: "SwaggerTaskList",
  data() {
    return {
      tableData: [],
      result: false
    };
  },
  props: {
    param: Object
  },
  methods: {
    search() {
      let projectId = getCurrentProjectID();
      this.result = scheduleTask(projectId).then(response => {
        this.tableData = response.data;
        response.data.forEach(item => {
          if (item.taskId === this.param.taskID) {
            this.handleRowClick(item);
          }
        });
      });
    },
    handleRowClick(row) {
      this.$emit('rowClick', row);
    },
    closeTaskConfirm(row) {
      let message = this.$t('api_test.home_page.running_task_list.confirm.close_title');
      if (row.enable) {
        message = this.$t('api_test.home_page.running_task_list.confirm.open_title');
      }
      row.enable = !row.enable;
      operationConfirm(message, () => {
        row.enable = !row.enable
        this.updateTask(row);
      });
    },
    updateTask(taskRow) {
      let schedule = {
        resourceId: taskRow.id,
        id: taskRow.taskId,
        enable: taskRow.enable,
        value: taskRow.rule
      }
      this.result = definitionSwitch(schedule).then(response => {
        this.search();
      });
    },
    deleteRowTask(row) {
      this.result = delDefinitionSchedule(row).then(response => {
        this.search();
        this.$emit('clear');
      });
    }

  },
  mounted() {
    this.search()
  },
}
</script>


<style scoped>

</style>
