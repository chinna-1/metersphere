<template>
  <ms-container v-if="renderComponent" v-loading="loading">

    <ms-aside-container v-show="isAsideHidden">
      <test-case-node-tree
        :type="'edit'"
        :total='total'
        :show-operator="true"
        :public-total="publicTotal"
        :case-condition="condition"
        @handleExportCheck="handleExportCheck"
        @refreshTable="refresh"
        @setTreeNodes="setTreeNodes"
        @exportTestCase="exportTestCase"
        @saveAsEdit="editTestCase"
        @refreshAll="refreshAll"
        @enableTrash="enableTrash"
        @enablePublic="enablePublic"
        @toPublic="toPublic"
        @importRefresh="importRefresh"
        @importChangeConfirm="importChangeConfirm"
        @createCase="handleCaseSimpleCreate($event, 'add')"
        ref="nodeTree"
      />
    </ms-aside-container>

    <ms-aside-container v-if="showPublicNode">
      <test-case-public-node-tree
        :case-condition="condition"
        @nodeSelectEvent="publicNodeChange"
        ref="publicNodeTree"/>
    </ms-aside-container>

    <ms-aside-container v-if="showTrashNode">
      <test-case-trash-node-tree
        :case-condition="condition"
        @nodeSelectEvent="trashNodeChange"
        ref="trashNodeTree"/>
    </ms-aside-container>

    <ms-main-container>
      <el-tabs v-model="activeName" @tab-click="addTab" @tab-remove="closeConfirm">
        <el-tab-pane name="trash" v-if="trashEnable" :label="$t('commons.trash')" :closable="true">
          <ms-tab-button
            :isShowChangeButton="false">
            <template v-slot:version>
              <version-select v-xpack :project-id="projectId" @changeVersion="changeTrashVersion" margin-left="-10"/>
            </template>
            <test-case-list
              :checkRedirectID="checkRedirectID"
              :isRedirectEdit="isRedirectEdit"
              :tree-nodes="treeNodes"
              :trash-enable="true"
              :current-version="currentTrashVersion"
              :version-enable="versionEnable"
              @testCaseEdit="editTestCase"
              @testCaseCopy="copyTestCase"
              @refresh="refreshTrashNode"
              @refreshAll="refreshAll"
              @setCondition="setCondition"
              @search="refreshTreeByCaseFilter"
              ref="testCaseTrashList">
            </test-case-list>
          </ms-tab-button>
        </el-tab-pane>
        <el-tab-pane name="public" v-if="publicEnable" :label="$t('project.case_public')">
          <div style="height: 6px;"></div>
          <public-test-case-list
            :tree-nodes="treeNodes"
            :version-enable="versionEnable"
            @refreshTable="refresh"
            @testCaseEdit="editTestCase"
            @testCaseEditShow="editTestCaseShow"
            @testCaseCopy="copyTestCase"
            @refresh="refresh"
            @refreshAll="refreshAll"
            @refreshPublic="refreshPublic"
            @setCondition="setCondition"
            @search="refreshTreeByCaseFilter"
            ref="testCasePublicList">
          </public-test-case-list>
        </el-tab-pane>
        <el-tab-pane name="default" :label="$t('api_test.definition.case_title')">
          <ms-tab-button
            :active-dom="activeDom"
            @update:activeDom="updateActiveDom"
            :left-tip="$t('test_track.case.list')"
            :left-content="$t('test_track.case.list')"
            :right-tip="$t('test_track.case.minder')"
            :right-content="$t('test_track.case.minder')"
            :middle-button-enable="false">
            <template v-slot:version>
              <version-select v-xpack :project-id="projectId" @changeVersion="changeVersion"/>
            </template>
            <test-case-list
              v-if="activeDom === 'left'"
              :checkRedirectID="checkRedirectID"
              :isRedirectEdit="isRedirectEdit"
              :tree-nodes="treeNodes"
              :trash-enable="false"
              :public-enable="false"
              :current-version="currentVersion"
              :version-enable="versionEnable"
              @closeExport="closeExport"
              @refreshTable="refresh"
              @testCaseEdit="editTestCase"
              @testCaseCopy="copyTestCase"
              @getTrashList="getTrashList"
              @getPublicList="getPublicList"
              @refresh="refresh"
              @refreshAll="refreshAll"
              @setCondition="setCondition"
              @decrease="decrease"
              @search="refreshTreeByCaseFilter"
              ref="testCaseList">
            </test-case-list>
            <test-case-minder
              :current-version="currentVersion"
              :tree-nodes="treeNodes"
              :project-id="projectId"
              :condition="condition"
              :active-name="activeName"
              v-if="activeDom === 'right'"
              @refresh="minderSaveRefresh"
              ref="minder"/>
          </ms-tab-button>
        </el-tab-pane>
        <el-tab-pane
          :key="item.name"
          v-for="(item) in tabs"
          :label="item.label"
          :name="item.name"
          closable>
          <div class="ms-api-scenario-div" v-if="!item.isPublic">
            <test-case-edit
              :currentTestCaseInfo="item.testCaseInfo"
              :version-enable="versionEnable"
              @refresh="refreshAll"
              @caseEdit="handleCaseCreateOrEdit($event,'edit')"
              @caseCreate="handleCaseCreateOrEdit($event,'add')"
              @checkout="checkout($event, item)"
              :is-public="item.isPublic"
              :read-only="testCaseReadOnly"
              :tree-nodes="treeNodes"
              :select-node="selectNode"
              :select-condition="condition"
              :public-enable="item.isPublic"
              :case-type="type"
              @addTab="addTab"
              ref="testCaseEdit">
            </test-case-edit>
          </div>
          <div class="ms-api-scenario-div" v-if="item.isPublic">
            <test-case-edit-show
              :currentTestCaseInfo="item.testCaseInfo"
              :version-enable="versionEnable"
              @refresh="refreshAll"
              @caseEdit="handleCaseCreateOrEdit($event,'edit')"
              @caseCreate="handleCaseCreateOrEdit($event,'add')"
              :read-only="testCaseReadOnly"
              @checkout="checkoutPublic($event, item)"
              :tree-nodes="treeNodes"
              :select-node="selectNode"
              :select-condition="condition"
              :type="type"
              :public-enable="currentActiveName === 'default' ? false : true"
              @addTab="addTabShow"
              ref="testCaseEditShow">
            </test-case-edit-show>
          </div>
          <template v-slot:version>
            <version-select v-xpack :project-id="projectId" @changeVersion="changeVersion"/>
          </template>
        </el-tab-pane>
        <el-tab-pane name="add" v-if="hasPermission('PROJECT_TRACK_CASE:READ+CREATE')">
          <template v-slot:label>
            <el-dropdown @command="handleCommand" v-permission="['PROJECT_TRACK_CASE:READ+CREATE']">
              <el-button type="primary" plain icon="el-icon-plus" size="mini"/>
              <el-dropdown-menu slot="dropdown">
                <el-dropdown-item command="ADD" v-permission="['PROJECT_TRACK_CASE:READ+CREATE']">
                  {{ $t('test_track.case.create') }}
                </el-dropdown-item>
                <el-dropdown-item command="CLOSE_ALL">{{ $t('api_test.definition.request.close_all_label') }}
                </el-dropdown-item>
              </el-dropdown-menu>
            </el-dropdown>
          </template>
        </el-tab-pane>

      </el-tabs>

      <is-change-confirm
        @confirm="changeConfirm"
        ref="isChangeConfirm"/>
    </ms-main-container>

  </ms-container>

</template>

<script>

import TestCaseEdit from './components/TestCaseEdit';
import TestCaseList from "./components/TestCaseList";
import SelectMenu from "../common/SelectMenu";
import MsContainer from "metersphere-frontend/src/components/MsContainer";
import MsAsideContainer from "metersphere-frontend/src/components/MsAsideContainer";
import MsMainContainer from "metersphere-frontend/src/components/MsMainContainer";
import {getCurrentProjectID, getCurrentWorkspaceId} from "metersphere-frontend/src/utils/token";
import {hasLicense, hasPermission} from "metersphere-frontend/src/utils/permission";
import {getUUID} from "metersphere-frontend/src/utils";
import TestCaseNodeTree from "../module/TestCaseNodeTree";
import MsTabButton from "metersphere-frontend/src/components/MsTabButton";
import TestCaseMinder from "../common/minder/TestCaseMinder";
import IsChangeConfirm from "metersphere-frontend/src/components/IsChangeConfirm";
import {openMinderConfirm} from "../common/minder/minderUtils";
import TestCaseEditShow from "./components/TestCaseEditShow";
import {PROJECT_ID} from "metersphere-frontend/src/utils/constants";
import MxVersionSelect from "metersphere-frontend/src/components/version/MxVersionSelect";
import {useStore} from "@/store";
import {testCaseNodePublicCount, testCaseNodeTrashCount} from "@/api/test-case-node";
import {getTestCase} from "@/api/testCase";
import {getProjectApplicationConfig} from "@/api/project-application";
import {versionEnableByProjectId} from "@/api/project";
import TestCasePublicNodeTree from "@/business/module/TestCasePublicNodeTree";
import TestCaseTrashNodeTree from "@/business/module/TestCaseTrashNodeTree";
import PublicTestCaseList from "@/business/case/components/public/PublicTestCaseList";

const store = useStore();
export default {
  name: "TestCase",
  components: {
    PublicTestCaseList,
    TestCaseTrashNodeTree,
    TestCasePublicNodeTree,
    IsChangeConfirm,
    TestCaseMinder,
    MsTabButton,
    TestCaseNodeTree,
    MsMainContainer,
    MsAsideContainer, MsContainer, TestCaseList, TestCaseEdit, SelectMenu, TestCaseEditShow,
    'VersionSelect': MxVersionSelect,
  },
  comments: {},
  data() {
    return {
      result: {},
      projects: [],
      treeNodes: [],
      testCaseReadOnly: true,
      trashEnable: false,
      publicEnable: false,
      showPublic: false,
      condition: {},
      activeName: 'default',
      currentActiveName: '',
      tabs: [],
      renderComponent: true,
      loading: false,
      type: '',
      activeDom: 'left',
      tmpActiveDom: null,
      total: 0,
      publicTotal: 0,
      tmpPath: null,
      currentVersion: null,
      currentTrashVersion: null,
      versionEnable: false,
      isAsideHidden: true,
      ignoreTreeNodes: false,
      hasRefreshDefault: true
    };
  },
  created() {
    let projectId = this.$route.query.projectId;
    if (projectId) {
      this.ignoreTreeNodes = true;
      if (projectId !== getCurrentProjectID() && projectId !== 'all') {
        sessionStorage.setItem(PROJECT_ID, projectId);
      }
    }
  },
  mounted() {
    this.getProject();
    this.init(this.$route);
    this.checkVersionEnable();
  },
  beforeRouteLeave(to, from, next) {
    if (store.isTestCaseMinderChanged) {
      this.$refs.isChangeConfirm.open();
      this.tmpPath = to.path;
    } else {
      next();
    }
  },
  watch: {
    redirectID() {
      this.renderComponent = false;
      this.$nextTick(() => {
        // 在 DOM 中添加 my-component 组件
        this.renderComponent = true;
      });
    },
    '$route'(to) {
      this.init(to);
    },
    activeName(newVal, oldVal) {
      this.isAsideHidden = this.activeName === 'default';
      if (oldVal !== 'default' && newVal === 'default' && this.$refs.minder) {
        this.$refs.minder.refresh();
      }
      if (oldVal === 'trash' && newVal === 'default') {
        this.condition.filters.status = [];
        // 在回收站恢复后，切到列表页面刷新
        if (!this.hasRefreshDefault) {
          this.refreshAll();
          this.hasRefreshDefault = true;
        } else {
          this.refresh();
        }
      } else if (newVal === 'default') {
        this.refresh();
      }
    },
    activeDom(newVal, oldVal) {
      this.$nextTick(() => {
        if (oldVal !== 'left' && newVal === 'left' && this.$refs.testCaseList) {
          this.$refs.testCaseList.getTemplateField();
        }
      });
    },
    trashEnable() {
      if (this.trashEnable) {
        this.activeName = 'trash';
        this.publicEnable = false;
        this.$nextTick(() => {
          this.$refs.trashNodeTree.list();
        });
      }
    },
    publicEnable() {
      if (this.publicEnable) {
        this.activeName = 'public';
        this.$nextTick(() => {
          this.$refs.publicNodeTree.list();
        });
        this.trashEnable = false;
      }
    },
    '$store.state.temWorkspaceId'() {
      if (this.$store.state.temWorkspaceId) {
        this.$refs.isChangeConfirm.open(null, this.$store.state.temWorkspaceId);
      }
    }
  },
  computed: {
    checkRedirectID: function () {
      let redirectIDParam = this.$route.params.redirectID;
      this.changeRedirectParam(redirectIDParam);
      return redirectIDParam;
    },
    isRedirectEdit: function () {
      return this.$route.params.dataSelectRange;
    },
    showPublicNode() {
      return this.activeName === 'public';
    },
    showTrashNode() {
      return this.activeName === 'trash';
    },
    projectId() {
      return getCurrentProjectID();
    },
    selectNodeIds() {
      return store.testCaseSelectNodeIds;
    },
    selectNode() {
      return store.testCaseSelectNode;
    },
    moduleOptions() {
      return store.testCaseModuleOptions;
    }
  },
  methods: {
    hasPermission,
    handleCommand(e) {
      switch (e) {
        case "ADD":
          this.addTab({name: 'add'});
          break;
        case "CLOSE_ALL":
          this.handleTabClose();
          break;
        default:
          this.addTab({name: 'add'});
          break;
      }
    },
    getTrashList() {
      testCaseNodeTrashCount(this.projectId)
        .then(response => {
          this.total = response.data;
        });
    },
    getPublicList() {
      testCaseNodePublicCount(getCurrentWorkspaceId())
        .then(response => {
          this.publicTotal = response.data;
        });
    },
    setCurTabId(tab, ref) {
      this.$nextTick(() => {
        if (this.$refs && this.$refs[ref]) {
          let index = tab.index ? Number.parseInt(tab.index) : this.tabs.length;
          let cutEditTab = this.$refs[ref][index - 1];
          let curTabId = cutEditTab ? cutEditTab.tabId : null;
          useStore().curTabId = curTabId;
        }
      });
    },
    updateActiveDom(activeDom) {
      openMinderConfirm(this, activeDom);
    },
    importChangeConfirm(isSave) {
      store.isTestCaseMinderChanged = false;
      if (isSave) {
        this.$refs.minder.save(() => {
          this.$refs.nodeTree.handleImport();
        });
      } else {
        this.$refs.nodeTree.handleImport();
      }
    },
    changeConfirm(isSave, temWorkspaceId) {
      if (isSave) {
        this.$refs.minder.save(() => {
          // 保存成功之后再切换tab
          this.activeDom = this.tmpActiveDom;
          this.tmpActiveDom = null;
        });
      } else {
        this.activeDom = this.tmpActiveDom;
        this.tmpActiveDom = null;
      }
      store.isTestCaseMinderChanged = false;
      this.$nextTick(() => {
        if (this.tmpPath) {
          this.$router.push({
            path: this.tmpPath
          });
          this.tmpPath = null;
        }
      });

      if (temWorkspaceId) {
        // 如果是切换工作空间提示的保存，则保存完后跳转到对应的工作空间
        this.$EventBus.$emit('changeWs', temWorkspaceId);
      }
    },
    changeRedirectParam(redirectIDParam) {
      this.redirectID = redirectIDParam;
      if (redirectIDParam != null) {
        if (this.redirectFlag === "none") {
          this.activeName = "default";
          this.redirectFlag = "redirected";
        }
      } else {
        this.redirectFlag = "none";
      }
    },
    addTab(tab) {
      if (!this.projectId) {
        this.$warning(this.$t('commons.check_project_tip'));
        return;
      }
      this.showPublic = false
      if (tab.name === 'add') {
        let label = this.$t('test_track.case.create');
        let name = getUUID().substring(0, 8);
        this.activeName = name;
        this.currentActiveName = 'default'
        this.type = 'add';
        this.tabs.push({label: label, name: name, testCaseInfo: {testCaseModuleId: "", id: getUUID()}});
      }
      if (tab.name === 'edit' || tab.name === 'show') {
        let label = this.$t('test_track.case.create');
        let name = getUUID().substring(0, 8);
        if (this.activeName === 'public') {
          this.currentActiveName = 'public'
        } else {
          this.currentActiveName = 'default'
        }
        this.activeName = name;
        label = tab.testCaseInfo.name;
        this.tabs.push({label: label, name: name, testCaseInfo: tab.testCaseInfo, isPublic: tab.isPublic});
      }

      if (tab.name === 'public') {
        this.publicEnable = false;
        this.$nextTick(() => {
          this.publicEnable = true;
        })
      } else if (tab.name === 'trash') {
        this.trashEnable = false;
        this.$nextTick(() => {
          this.trashEnable = true;
        })
      }

      this.setCurTabId(tab, 'testCaseEdit');
    },
    addTabShow(tab) {
      if (!this.projectId) {
        this.$warning(this.$t('commons.check_project_tip'));
        return;
      }
      if (tab.name === 'show') {
        this.showPublic = true
        let label = this.$t('test_track.case.create');
        let name = getUUID().substring(0, 8);
        this.activeName = name;
        this.currentActiveName = 'public'
        label = tab.testCaseInfo.name;
        this.tabs.push({label: label, name: name, testCaseInfo: tab.testCaseInfo});
      }
      this.setCurTabId(this, tab, 'testCaseEditShow');
    },
    handleTabClose() {
      let message = "";
      this.tabs.forEach(t => {
        if (t && store.testCaseMap.has(t.testCaseInfo.id) && store.testCaseMap.get(t.testCaseInfo.id) > 1) {
          message += t.testCaseInfo.name + "，";
        }
        if (t.label === this.$t('test_track.case.create')) {
          message += this.$t('test_track.case.create') + "，";
        }
        if (t.testCaseInfo.isCopy) {
          message += t.testCaseInfo.name + "，";
        }
      })
      if (message !== "") {
        this.$alert(this.$t('commons.track') + " [ " + message.substr(0, message.length - 1) + " ] " + this.$t('commons.confirm_info'), '', {
          confirmButtonText: this.$t('commons.confirm'),
          cancelButtonText: this.$t('commons.cancel'),
          callback: (action) => {
            if (action === 'confirm') {
              store.testCaseMap.clear();
              this.tabs = [];
              this.activeName = "default";
              this.refresh();
            }
          }
        });
      } else {
        this.tabs = [];
        this.activeName = "default";
        this.refresh();
      }
    },
    closeConfirm(targetName) {
      if (targetName === 'trash') {
        this.activeName = 'default';
        this.trashEnable = false;
      } else {
        this.closeTabWithSave(targetName);
      }
    },
    closeTabWithSave(targetName) {
      let t = this.tabs.filter(tab => tab.name === targetName);
      let message = "";
      if (t && store.testCaseMap.has(t[0].testCaseInfo.id) && store.testCaseMap.get(t[0].testCaseInfo.id) > 0) {
        message += t[0].testCaseInfo.name;
      }
      if (t[0].label === this.$t('test_track.case.create')) {
        message += this.$t('test_track.case.create');
      }
      if (t[0].testCaseInfo.isCopy) {
        message += t[0].testCaseInfo.name;
      }
      if (message !== "") {
        this.$alert(this.$t('commons.track') + " [ " + message + " ] " + this.$t('commons.confirm_info'), '', {
          confirmButtonText: this.$t('commons.confirm'),
          cancelButtonText: this.$t('commons.cancel'),
          callback: (action) => {
            if (action === 'confirm') {
              store.testCaseMap.delete(t[0].testCaseInfo.id);
              this.removeTab(targetName);
            }
          }
        });
      } else {
        store.testCaseMap.delete(t[0].testCaseInfo.id);
        this.removeTab(targetName);
      }
    },
    removeTab(targetName) {
      this.tabs = this.tabs.filter(tab => tab.name !== targetName);
      if (this.tabs.length > 0) {
        this.activeName = this.tabs[this.tabs.length - 1].name;
      } else {
        this.activeName = "default";
      }
    },
    handleExportCheck() {
      if (this.$refs.testCaseList.checkSelected()) {
        this.$refs.nodeTree.openExport();
      }
    },
    exportTestCase(type, param) {
      if (this.activeDom !== 'left') {
        this.$warning(this.$t('test_track.case.export.export_tip'));
        return;
      }
      this.$refs.testCaseList.exportTestCase(type, param);
    },
    closeExport() {
      this.$refs.nodeTree.closeExport();
    },
    init(route) {
      let path = route.path;
      if (path.indexOf("/track/case/edit") >= 0 || path.indexOf("/track/case/create") >= 0) {
        this.testCaseReadOnly = false;
        let caseId = this.$route.query.caseId;
        if (!this.projectId) {
          this.$warning(this.$t('commons.check_project_tip'));
          return;
        }
        if (caseId) {
          getTestCase(caseId)
            .then(response => {
              let testCase = response.data;
              this.editTestCase(testCase);
            });
        } else {
          this.addTab({name: 'add'});
        }
        this.$router.push('/track/case/all');
      }
    },
    publicNodeChange(node, nodeIds, pNodes) {
      if (this.$refs.testCasePublicList) {
        this.$refs.testCasePublicList.initTableData(nodeIds);
      }
    },
    trashNodeChange(node, nodeIds, pNodes) {
      if (this.$refs.testCaseTrashList) {
        this.$refs.testCaseTrashList.initTableData(nodeIds);
      }
    },
    increase(id) {
      this.$refs.nodeTree.increase(id);
    },
    decrease(id) {
      this.$refs.nodeTree.decrease(id);
    },
    editTestCase(testCase, isPublic) {
      const index = this.tabs.find(p => p.testCaseInfo && p.testCaseInfo.id === testCase.id);
      if (!index) {
        this.type = "edit";
        this.testCaseReadOnly = false;
        let hasEditPermission = hasPermission('PROJECT_TRACK_CASE:READ+EDIT');
        this.$set(testCase, 'rowClickHasPermission', hasEditPermission);
        this.addTab({name: isPublic ? 'show' : 'edit', testCaseInfo: testCase, isPublic});
      } else {
        this.activeName = index.name;
      }
    },
    editTestCaseShow(testCase) {
      this.editTestCase(testCase, true);
    },
    handleCaseCreateOrEdit(data, type) {
      if (this.$refs.minder) {
        this.$refs.minder.addCase(data, type);
      }
    },
    handleCaseSimpleCreate(data, type) {
      if ('default-module' === data.nodeId) {
        for (let i = 0; i < this.moduleOptions.length; i++) {
          let item = this.moduleOptions[i];
          if (item.path.indexOf('未规划用例') > -1) {
            data.nodeId = item.id;
            break;
          }
        }
      }
      this.handleCaseCreateOrEdit(data, type);
      if (this.$refs.minder) {
        this.$refs.minder.refresh();
      }
    },
    copyTestCase(testCase) {
      this.type = "copy";
      this.testCaseReadOnly = false;
      testCase.isCopy = true;
      this.addTab({name: 'edit', testCaseInfo: testCase});
    },
    refresh(data) {
      if (this.selectNodeIds && this.selectNodeIds.length > 0) {
        store.testCaseSelectNode = {};
        store.testCaseSelectNodeIds = [];
      }
      this.refreshAll(data);
    },
    refreshTrashNode() {
      this.$refs.trashNodeTree.list();
      this.hasRefreshDefault = false;
    },
    refreshTreeByCaseFilter() {
      if (this.publicEnable) {
        this.$refs.publicNodeTree.list();
      } else if (this.trashEnable) {
        this.$refs.trashNodeTree.list();
      } else {
        this.$refs.nodeTree.list();
      }
    },
    setTable(data) {
      if (data) {
        for (let index in this.tabs) {
          let tab = this.tabs[index];
          if (tab.name === this.activeName) {
            tab.label = data.name;
            break;
          }
        }
      }
    },
    refreshAll(data) {
      if (this.$refs.testCaseList) {
        this.$refs.testCaseList.initTableData();
      }
      this.$refs.nodeTree.list();
      this.setTable(data);
    },
    importRefresh() {
      this.refreshAll();
      if (this.$refs.testCaseEdit && this.$refs.testCaseEdit.length > 0) {
        setTimeout(() => {
          this.$info(this.$t('test_track.case.import.import_refresh_tips'));
        }, 3000)
      }
    },
    minderSaveRefresh() {
      if (this.$refs.testCaseList) {
        this.$refs.testCaseList.initTableData();
      }
      this.$refs.nodeTree.list();
    },
    refreshPublic() {
      if (this.$refs.testCasePublicList) {
        this.$refs.testCasePublicList.initTableData([]);
      }
      this.$refs.publicNodeTree.list();
    },
    setTreeNodes(data) {
      this.treeNodes = data;
    },
    setCondition(data) {
      this.condition = data;
    },
    getProject() {
      getProjectApplicationConfig('CASE_CUSTOM_NUM')
        .then(result => {
          let data = result.data;
          if (data) {
            store.currentProjectIsCustomNum = data.caseCustomNum;
          }
        });
    },
    enableTrash(data) {
      this.trashEnable = !data;
      this.$nextTick(() => {
        this.trashEnable = data;
      })
    },
    enablePublic(data) {
      this.publicEnable = !data;
      this.$nextTick(() => {
        this.publicEnable = data;
      })
    },
    toPublic(data) {
      if (data === 'public') {
        this.activeName = "public"
      } else {
        this.activeName = "trash"
      }
    },
    changeVersion(currentVersion) {
      this.currentVersion = currentVersion || null;
    },
    changeTrashVersion(currentVersion) {
      this.currentTrashVersion = currentVersion || null;
    },
    checkout(testCase, item) {
      Object.assign(item.testCaseInfo, testCase)
      //子组件先变更 copy 状态，再执行初始化操作
      for (let i = 0; i < this.$refs.testCaseEdit.length; i++) {
        this.$refs.testCaseEdit[i].initEdit(item.testCaseInfo, () => {
          this.$nextTick(() => {
            let vh = this.$refs.testCaseEdit[i].$refs.versionHistory;
            vh.getVersionOptionList(vh.handleVersionOptions);
            vh.show = false;
            vh.loading = false;
          });
        });
      }
    },
    checkoutPublic(testCase, item) {
      Object.assign(item.testCaseInfo, testCase)
      //子组件先变更 copy 状态，再执行初始化操作
      this.$refs.testCaseEditShow[0].initEdit(item.testCaseInfo, () => {
        this.$nextTick(() => {
          let vh = this.$refs.testCaseEditShow[0].$refs.versionHistory;
          vh.getVersionOptionList(vh.handleVersionOptions);
          vh.show = false;
          vh.loading = false;
        });
      });
    },
    checkVersionEnable() {
      if (!this.projectId) {
        return;
      }
      if (hasLicense()) {
        versionEnableByProjectId(this.projectId)
          .then(response => {
            this.versionEnable = response.data;
          });
      }
    },
  }
};
</script>

<style scoped>

.el-main {
  padding: 5px 10px;
}

:deep(.el-tabs__header) {
  margin: 0 0 0px;
  /*width: calc(100% - 90px);*/
}

:deep(.el-table__empty-block) {
  width: 100%;
  min-width: 100%;
  max-width: 100%;
  padding-right: 100%;
}

.version-select {
  padding-left: 10px;
}

</style>
