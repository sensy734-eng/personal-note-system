<template>
  <el-container class="edit-page">
    <el-header class="edit-header">
      <div class="header-left">
        <el-button icon="Back" @click="goBack">返回列表</el-button>
        <h3 class="page-title">{{ isEdit ? '编辑笔记' : '新建笔记' }}</h3>
        <span v-if="lastSavedTime" class="save-status">
          上次保存: {{ lastSavedTime }}
        </span>
      </div>
      <div class="header-right">
        <el-dropdown v-if="isEdit" @command="handleExport" style="margin-right: 15px;">
          <el-button type="info" plain icon="Download">
            导出笔记 <el-icon class="el-icon--right"><arrow-down /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="md">导出为 Markdown (.md)</el-dropdown-item>
              <el-dropdown-item command="txt">导出为 纯文本 (.txt)</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>

        <el-button type="primary" size="large" @click="handleSave(false)" :loading="saveLoading">
          <el-icon><Select /></el-icon> {{ isEdit ? '保存修改' : '立即发布' }}
        </el-button>
      </div>
    </el-header>

    <el-main class="edit-main">
      <el-card class="edit-card" shadow="never">
        <el-input
            v-model="noteForm.title"
            size="large"
            placeholder="标题..."
            class="title-input"
        >
          <template #prefix><el-icon><EditPen /></el-icon></template>
        </el-input>

        <div class="meta-section">
          <el-select v-model="noteForm.categoryId" placeholder="分类" style="width: 150px;">
            <el-option label="默认" :value="null" />
            <el-option v-for="cat in categoryList" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>

          <el-select
              v-model="noteForm.tags"
              multiple
              filterable
              allow-create
              default-first-option
              placeholder="标签"
              style="flex: 1; margin-left: 15px;"
          >
          </el-select>

          <el-button
              type="warning"
              plain
              style="margin-left: 15px;"
              @click="autoExtractTags"
              :loading="extracting"
          >
            <el-icon><MagicStick /></el-icon> 智能提取
          </el-button>
        </div>

        <NoteEditor
            v-model="noteForm.content"
            @textChange="handleTextChange"
            placeholder="开始你的创作..."
            style="margin-top: 20px;"
        />
      </el-card>
    </el-main>
  </el-container>
</template>

<script setup>
import { ref, reactive, onMounted, computed, watch, onBeforeUnmount } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { Download, Back, Select, EditPen, MagicStick, ArrowDown } from '@element-plus/icons-vue';
import request from '../utils/request';
import NoteEditor from '../components/NoteEditor.vue';

const route = useRoute();
const router = useRouter();
const isEdit = computed(() => !!route.params.id);
const saveLoading = ref(false);
const extracting = ref(false);
const categoryList = ref([]);
const lastSavedTime = ref('');
const autoSaveTimer = ref(null);

const noteForm = reactive({
  title: '',
  content: '',
  contentText: '',
  categoryId: null,
  tags: []
});

const handleTextChange = (text) => { noteForm.contentText = text };

// 🚀 修复：显式定义返回函数
const goBack = () => {
  router.push('/');
};

// 自动保存逻辑
watch(noteForm, (newVal) => {
  if (saveLoading.value) return;
  if (autoSaveTimer.value) clearTimeout(autoSaveTimer.value);
  autoSaveTimer.value = setTimeout(() => {
    if (noteForm.title.trim() || noteForm.contentText.trim()) {
      handleSave(true);
    }
  }, 3000);
}, { deep: true });

// 🚀 修复：导出拼接 token
const handleExport = (type) => {
  const token = localStorage.getItem('token');
  window.open(`http://localhost:8080/api/notes/${route.params.id}/export?type=${type}&token=${token}`);
};

const autoExtractTags = async () => {
  if (!noteForm.contentText || noteForm.contentText.length < 20) return ElMessage.warning('内容过少无法提取');
  extracting.value = true;
  try {
    const res = await request.post('/notes/extract-tags', { text: noteForm.contentText });
    noteForm.tags = Array.from(new Set([...noteForm.tags, ...res]));
    ElMessage.success('提取完成');
  } catch (e) { ElMessage.error('提取失败'); } finally { extracting.value = false; }
};

const handleSave = async (isAuto = false) => {
  if (!isAuto) saveLoading.value = true;

  try {
    const url = isEdit.value ? `/notes/${route.params.id}` : '/notes';
    const method = isEdit.value ? 'put' : 'post';
    const res = await request[method](url, noteForm);

    lastSavedTime.value = new Date().toLocaleTimeString();

    if (!isAuto) {
      ElMessage.success(isEdit.value ? '更新成功' : '新建成功');
      // 🚀 修复：保存成功后跳转回首页
      router.push('/');
    }

    if (!isEdit.value && (res.noteId || res.data?.noteId)) {
      const newId = res.noteId || res.data?.noteId;
      router.replace(`/note/edit/${newId}`);
    }
  } catch (e) {
    if (!isAuto) ElMessage.error('保存失败');
  } finally {
    if (!isAuto) saveLoading.value = false;
  }
};

onBeforeUnmount(() => {
  if (autoSaveTimer.value) clearTimeout(autoSaveTimer.value);
});

onMounted(() => {
  request.get('/categories').then(res => categoryList.value = res);
  if (isEdit.value) {
    request.get(`/notes/${route.params.id}`).then(res => {
      Object.assign(noteForm, res.note);
      noteForm.tags = res.tags || [];
      lastSavedTime.value = new Date(res.note.updated_at).toLocaleTimeString();
    });
  }
});
</script>

<style scoped>
.edit-page { height: 100vh; background-color: #f0f2f5; }
.edit-header { display: flex; justify-content: space-between; align-items: center; background-color: #fff; padding: 0 20px; border-bottom: 1px solid #ddd; }
.header-left { display: flex; align-items: center; gap: 15px; }
.page-title { margin-right: 15px; }
.save-status { font-size: 13px; color: #909399; font-weight: normal; font-style: italic; }
.edit-card { max-width: 1000px; margin: 0 auto; }
.meta-section { display: flex; margin: 15px 0; align-items: center; }
.title-input :deep(.el-input__inner) { font-size: 22px; font-weight: bold; border-bottom: 1px solid #eee; }
</style>