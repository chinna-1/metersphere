<template>
  <div>
    <ms-module-minder
      v-loading="result.loading"
      minder-key="PLAN_CASE"
      :tree-nodes="treeNodes"
      :data-map="dataMap"
      :tags="tags"
      :tag-enable="true"
      :disabled="disableMinder"
      :select-node="selectNode"
      :distinct-tags="[...tags, this.$t('test_track.plan.plan_status_prepare')]"
      :ignore-num="true"
      @afterMount="handleAfterMount"
      @save="save"
      ref="minder"
    />

    <IssueRelateList
      :plan-case-id="getCurId()"
      :case-id="getCurCaseId()"
      @refresh="refreshRelateIssue"
      ref="issueRelate"/>

    <test-plan-issue-edit
      :is-minder="true"
      :plan-id="planId"
      :plan-case-id="getCurId()"
      :case-id="getCurCaseId()"
      @refresh="refreshIssue"
      ref="issueEdit"/>

  </div>
</template>

<script>
import {setPriorityView} from "vue-minder-editor-plus/src/script/tool/utils";

const {getCurrentWorkspaceId} = require("metersphere-frontend/src/utils");
const {getIssuesListById} = require("@/api/issue");
import {
  handleExpandToLevel, listenBeforeExecCommand, listenNodeSelected, loadSelectNodes,
  tagBatch, getSelectedNodeData, handleIssueAdd, handleIssueBatch, listenDblclick, handleMinderIssueDelete
} from "@/business/common/minder/minderUtils";
import {getPlanCasesForMinder} from "@/api/testCase";
import IssueRelateList from "@/business/case/components/IssueRelateList";
import TestPlanIssueEdit from "@/business/case/components/TestPlanIssueEdit";
import {addIssueHotBox} from "./minderUtils";
import MsModuleMinder from "@/business/common/minder/MsModuleMinder";
import {useStore} from "@/store";
import {testPlanCaseMinderEdit} from "@/api/remote/plan/test-plan-case";

export default {
  name: "TestPlanMinder",
  components: {MsModuleMinder, TestPlanIssueEdit, IssueRelateList},
  data() {
    return {
      dataMap: new Map(),
      result: {loading: false},
      tags: [this.$t('test_track.plan_view.pass'), this.$t('test_track.plan_view.failure'), this.$t('test_track.plan_view.blocking'), this.$t('test_track.plan_view.skip')],
    }
  },
  props: {
    treeNodes: {
      type: Array,
      default() {
        return []
      }
    },
    selectNodeIds: {
      type: Array
    },
    planId: {
      type: String
    },
    planStatus: {
      type: String
    },
    projectId: String,
    condition: Object
  },
  computed: {
    selectNode() {
      return useStore().testPlanViewSelectNode;
    },
    workspaceId() {
      return getCurrentWorkspaceId();
    },
    disableMinder() {
      if (this.planStatus === 'Archived') {
        return true
      } else {
        return false
      }
    }
  },
  mounted() {
    this.setIsChange(false);
    if (this.selectNode && this.selectNode.data) {
      if (this.$refs.minder) {
        let importJson = this.$refs.minder.getImportJsonBySelectNode(this.selectNode.data);
        this.$refs.minder.setJsonImport(importJson);
      }
    }
  },
  watch: {
    selectNode() {
      if (this.$refs.minder) {
        this.$refs.minder.handleNodeSelect(this.selectNode);
      }
    },
    treeNodes() {
      this.$refs.minder.initData();
    }
  },
  methods: {
    handleAfterMount: function () {
      listenNodeSelected(() => {
        loadSelectNodes(this.getParam(), getPlanCasesForMinder, this.setParamCallback);
      });
      listenBeforeExecCommand((even) => {
        if (even.commandName === 'expandtolevel') {
          let level = Number.parseInt(even.commandArgs);
          handleExpandToLevel(level, even.minder.getRoot(), this.getParam(), getPlanCasesForMinder, this.setParamCallback);
        }

        if (handleMinderIssueDelete(even.commandName, true)) return; // 删除缺陷不算有编辑脑图信息

        if (even.commandName.toLocaleLowerCase() === 'resource') {
          // 设置完标签后，优先级显示有问题，重新设置下
          setTimeout(() => setPriorityView(true, 'P'), 100);
          this.setIsChange(true);
        }
      });

      listenDblclick(() => {
        let data = getSelectedNodeData();
        if (data.type === 'issue') {
          getIssuesListById(data.id, this.projectId, this.workspaceId, (data) => {
            data.customFields = JSON.parse(data.customFields);
            this.$refs.issueEdit.open(data);
          });
        }
      });

      tagBatch([...this.tags, this.$t('test_track.plan.plan_status_prepare')], {
        param: this.getParam(),
        getCaseFuc: getPlanCasesForMinder,
        setParamCallback: this.setParamCallback
      });

      addIssueHotBox(this);
    },
    getParam() {
      return {
        request: {
          planId: this.planId,
          orders: this.condition.orders
        },
        result: this.result,
        isDisable: true
      }
    },
    setParamCallback(data, item) {
      if (item.status === 'Pass') {
        data.resource.push(this.$t('test_track.plan_view.pass'));
      } else if (item.status === 'Failure') {
        data.resource.push(this.$t('test_track.plan_view.failure'));
      } else if (item.status === 'Blocking') {
        data.resource.push(this.$t('test_track.plan_view.blocking'));
      } else if (item.status === 'Skip') {
        data.resource.push(this.$t('test_track.plan_view.skip'));
      } else {
        data.resource.push(this.$t('test_track.plan.plan_status_prepare'));
      }
    },
    save(data) {
      let saveCases = [];
      this.buildSaveCase(data.root, saveCases);
      this.result.loading = true;
      testPlanCaseMinderEdit(saveCases)
        .then(() => {
          this.result.loading = false;
          this.$success(this.$t('commons.save_success'));
          this.setIsChange(false);
        });
    },
    buildSaveCase(root, saveCases) {
      let data = root.data;
      if (data.resource && data.resource.indexOf(this.$t('api_test.definition.request.case')) > -1) {
        this._buildSaveCase(root, saveCases, parent);
      } else {
        if (root.children) {
          root.children.forEach((childNode) => {
            this.buildSaveCase(childNode, saveCases, root.data);
          })
        }
      }
    },
    _buildSaveCase(node, saveCases) {
      let data = node.data;
      if (!data.changed) {
        return;
      }
      let testCase = {
        id: data.id,
      };
      if (data.resource.length > 1) {
        if (data.resource.indexOf(this.$t('test_track.plan_view.failure')) > -1) {
          testCase.status = 'Failure';
        } else if (data.resource.indexOf(this.$t('test_track.plan_view.pass')) > -1) {
          testCase.status = 'Pass';
        } else if (data.resource.indexOf(this.$t('test_track.plan_view.blocking')) > -1) {
          testCase.status = 'Blocking';
        } else if (data.resource.indexOf(this.$t('test_track.plan_view.skip')) > -1) {
          testCase.status = 'Skip';
        }
      }
      saveCases.push(testCase);
    },
    getCurCaseId() {
      return getSelectedNodeData().caseId;
    },
    getCurId() {
      return getSelectedNodeData().id;
    },
    refreshIssue(issue) {
      handleIssueAdd(issue);
    },
    refreshRelateIssue(issues) {
      handleIssueBatch(issues);
      this.$success('关联成功');
    },
    setIsChange(isChanged) {
      useStore().isTestCaseMinderChanged = isChanged;
    },
  }
}
</script>

<style scoped>

</style>
