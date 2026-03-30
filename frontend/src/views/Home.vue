<template>
  <el-container class="layout-container">
    <el-aside width="250px" class="aside">
      <div class="logo"><h3>📚 我的学习笔记</h3></div>
      <el-menu :default-active="currentFilter" class="menu" @select="handleMenuSelect">
        <el-menu-item index="statistics"><el-icon><DataLine /></el-icon><span>数据看板</span></el-menu-item>
        <el-menu-item index="all"><el-icon><Document /></el-icon><span>全部笔记</span></el-menu-item>
        <el-menu-item index="starred"><el-icon><Star /></el-icon><span>星标笔记</span></el-menu-item>
        <el-sub-menu index="categories">
          <template #title><el-icon><Folder /></el-icon><span>我的分类</span></template>
          <el-menu-item v-for="cate in categoryList" :key="cate.id" :index="'cat_' + cate.id">
            <el-icon><CollectionTag /></el-icon> {{ cate.name }}
          </el-menu-item>
          <el-menu-item index="manage_categories" style="color: #409eff;"><el-icon><Setting /></el-icon>管理分类...</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="tags">
          <template #title><el-icon><PriceTag /></el-icon><span>我的标签</span></template>
          <el-menu-item v-for="tag in tagList" :key="tag.id" :index="'tag_' + tag.name">
            <el-icon><Collection /></el-icon> {{ tag.name }}
          </el-menu-item>
        </el-sub-menu>
        <el-menu-item index="trash"><el-icon><Delete /></el-icon><span>回收站</span></el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-left" style="display: flex; gap: 15px; align-items: center;">
          <el-input v-model="searchQuery" placeholder="全文检索..." style="width: 200px" clearable @input="handleSearch">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-date-picker v-model="dateRange" type="daterange" size="default" value-format="YYYY-MM-DD" @change="handleSearch" style="width: 250px;" />
        </div>
        <div class="header-right" style="display: flex; align-items: center; gap: 15px;">
          <el-switch v-model="isDark" @change="toggleDarkMode" inline-prompt :active-icon="Moon" :inactive-icon="Sunny" style="margin-right: 10px;" />
          <el-button v-if="currentFilter.startsWith('cat_')" type="warning" plain icon="Box" @click="handleBatchExport">备份分类</el-button>
          <el-dropdown>
            <span class="el-dropdown-link" style="cursor: pointer; display: flex; align-items: center; gap: 8px;">
              <el-avatar :size="32" :src="userInfo.avatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'" />
              {{ userInfo.nickname || userInfo.username }}
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="profileDialogVisible = true">个人设置</el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-button type="primary" @click="createNewNote"><el-icon><Plus /></el-icon> 新建笔记</el-button>
        </div>
      </el-header>

      <el-main class="main">
        <DataStatistics v-if="currentFilter === 'statistics'" />
        <div v-else>
          <div v-if="currentFilter === 'all' && !searchQuery && recentNotesList.length > 0" class="recent-section">
            <div class="section-title">🕒 最近访问</div>
            <el-row :gutter="15">
              <el-col :span="6" v-for="recent in recentNotesList" :key="'recent-' + recent.id">
                <el-card class="recent-card" shadow="hover" @click="editNote(recent.id)">
                  <div class="recent-card-content">
                    <span class="recent-title">{{ recent.title }}</span>
                    <span class="recent-time">{{ formatRecentTime(recent.last_accessed_at) }}</span>
                  </div>
                </el-card>
              </el-col>
            </el-row>
            <el-divider />
          </div>
          <div v-if="loading" class="loading-state">数据加载中...</div>
          <el-row :gutter="20" v-else>
            <el-col :span="8" v-for="note in notesList" :key="note.id" style="margin-bottom: 20px;">
              <el-card class="note-card" shadow="hover">
                <template #header>
                  <div class="card-header">
                    <span class="note-title">{{ note.title }}</span>
                    <el-icon v-if="currentFilter !== 'trash'" :color="note.is_starred ? '#e6a23c' : '#c0c4cc'" @click.stop="handleToggleStar(note)">
                      <StarFilled v-if="note.is_starred" /><Star v-else />
                    </el-icon>
                  </div>
                </template>
                <div class="note-summary">{{ note.content_text ? note.content_text.substring(0, 50) + '...' : '暂无内容' }}</div>
                <div class="note-footer">
                  <span class="time">{{ formatDate(note.updated_at) }}</span>
                  <div class="actions">
                    <template v-if="currentFilter === 'trash'">
                      <el-button link type="success" @click="handleRestore(note.id)">恢复</el-button>
                      <el-button link type="danger" @click="handlePermanentDelete(note.id)">彻底删除</el-button>
                    </template>
                    <template v-else>
                      <el-button link type="primary" @click="editNote(note.id)">编辑</el-button>
                      <el-button link type="info" @click="handleSingleExport(note.id)">导出</el-button>
                      <el-button link type="danger" @click="handleSoftDelete(note.id)">删除</el-button>
                    </template>
                  </div>
                </div>
              </el-card>
            </el-col>
          </el-row>
          <el-empty v-if="!loading && notesList.length === 0" description="这里空空如也" />
        </div>
      </el-main>
    </el-container>

    <el-dialog v-model="categoryDialogVisible" title="分类管理" width="500px">
      <div style="margin-bottom: 20px; display: flex; gap: 10px;">
        <el-input v-model="newCategoryName" placeholder="输入新分类名称..." @keyup.enter="handleAddCategory" />
        <el-button type="primary" @click="handleAddCategory">添加分类</el-button>
      </div>
      <el-table :data="categoryList" style="width: 100%" max-height="300">
        <el-table-column prop="name" label="分类名称">
          <template #default="scope">
            <el-input v-if="editingCategoryId === scope.row.id" v-model="editingCategoryName" size="small" @blur="handleSaveCategory(scope.row)" />
            <span v-else>{{ scope.row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="scope">
            <el-button link type="primary" @click="startEditCategory(scope.row)">
              <el-icon><Edit /></el-icon>
            </el-button>
            <el-button link type="danger" @click="handleDeleteCategory(scope.row.id)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog v-model="profileDialogVisible" title="个人设置" width="450px">
      <el-form label-width="80px">
        <el-form-item label="我的头像">
          <el-upload class="avatar-uploader" action="http://localhost:8080/api/auth/avatar" :headers="uploadHeaders" :show-file-list="false" :on-success="handleAvatarSuccess">
            <img v-if="userInfo.avatar" :src="userInfo.avatar" class="avatar-preview" />
            <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
          </el-upload>
        </el-form-item>
        <el-form-item label="昵称"><el-input v-model="profileForm.nickname" /></el-form-item>
        <el-form-item label="个性签名"><el-input v-model="profileForm.signature" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="profileDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveProfile">保存修改</el-button>
      </template>
    </el-dialog>
  </el-container>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import request from '../utils/request';
import DataStatistics from '../components/DataStatistics.vue';
import {
  Document, Star, StarFilled, Delete, Search, Plus, Folder,
  CollectionTag, Setting, DataLine, Box, Sunny, Moon, PriceTag, Collection, Edit
} from '@element-plus/icons-vue';

const router = useRouter();
const notesList = ref([]);
const recentNotesList = ref([]);
const categoryList = ref([]);
const tagList = ref([]);
const loading = ref(false);
const searchQuery = ref('');
const dateRange = ref([]);
const currentFilter = ref('all');
const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || '{}'));

// 🚀 修复：分类管理相关状态
const categoryDialogVisible = ref(false);
const newCategoryName = ref('');
const editingCategoryId = ref(null);
const editingCategoryName = ref('');

const profileDialogVisible = ref(false);
const profileForm = reactive({ nickname: userInfo.value.nickname || '', signature: userInfo.value.signature || '' });
const uploadHeaders = ref({ Authorization: `Bearer ${localStorage.getItem('token')}` });

const fetchNotes = async () => {
  loading.value = true;
  try {
    const params = { status: currentFilter.value === 'trash' ? 0 : 1 };
    if (searchQuery.value) params.keyword = searchQuery.value;
    if (currentFilter.value === 'starred') params.isStarred = 1;
    if (currentFilter.value.startsWith('cat_')) params.categoryId = currentFilter.value.split('_')[1];
    if (currentFilter.value.startsWith('tag_')) params.tagName = currentFilter.value.split('_')[1];
    if (dateRange.value?.length === 2) {
      params.startDate = dateRange.value[0];
      params.endDate = dateRange.value[1];
    }
    const res = await request.get('/notes', { params });
    notesList.value = res.data;
  } catch (e) { ElMessage.error('获取失败'); } finally { loading.value = false; }
};

// 🚀 修复：分类管理逻辑
const fetchCategories = async () => {
  try { categoryList.value = await request.get('/categories'); } catch (e) {}
};

const handleAddCategory = async () => {
  if (!newCategoryName.value.trim()) return ElMessage.warning('名称不能为空');
  try {
    await request.post('/categories', { name: newCategoryName.value });
    newCategoryName.value = '';
    ElMessage.success('添加成功');
    fetchCategories();
  } catch (e) {}
};

const startEditCategory = (row) => {
  editingCategoryId.value = row.id;
  editingCategoryName.value = row.name;
};

const handleSaveCategory = async (row) => {
  if (!editingCategoryName.value.trim()) return editingCategoryId.value = null;
  try {
    await request.put(`/categories/${row.id}`, { name: editingCategoryName.value });
    editingCategoryId.value = null;
    fetchCategories();
  } catch (e) {}
};

const handleDeleteCategory = (id) => {
  ElMessageBox.confirm('删除分类后，原分类下的笔记将变为“无分类”状态，确认吗？', '提示').then(async () => {
    await request.delete(`/categories/${id}`);
    ElMessage.success('删除成功');
    fetchCategories();
  }).catch(() => {});
};

const handlePermanentDelete = (id) => {
  ElMessageBox.confirm('彻底删除后将无法恢复，确认继续吗？', '严重警告', { type: 'warning' }).then(async () => {
    await request.delete(`/notes/${id}/permanent`);
    ElMessage.success('已永久删除');
    fetchNotes();
  }).catch(() => {});
};

const handleMenuSelect = (i) => {
  // 🚀 修复：点击管理分类时打开弹窗
  if (i === 'manage_categories') {
    categoryDialogVisible.value = true;
    return;
  }
  currentFilter.value = i;
  if (i !== 'statistics') fetchNotes();
};

// ...其余导出、登录、样式切换逻辑保持不变
const handleSingleExport = (id) => window.open(`http://localhost:8080/api/notes/${id}/export?type=md&token=${localStorage.getItem('token')}`);
const handleBatchExport = () => window.open(`http://localhost:8080/api/notes/export/category?categoryId=${currentFilter.value.split('_')[1]}&type=md&token=${localStorage.getItem('token')}`);
const saveProfile = async () => {
  try {
    const res = await request.put('/auth/profile', profileForm);
    userInfo.value = { ...userInfo.value, ...res.user };
    localStorage.setItem('userInfo', JSON.stringify(userInfo.value));
    profileDialogVisible.value = false;
    ElMessage.success('更新成功');
  } catch (e) { ElMessage.error('保存失败'); }
};
const handleAvatarSuccess = (res) => { userInfo.value.avatar = res.url; localStorage.setItem('userInfo', JSON.stringify(userInfo.value)); ElMessage.success('头像上传成功'); };
const handleLogout = () => { localStorage.clear(); router.push('/login'); };
const isDark = ref(document.documentElement.classList.contains('dark'));
const toggleDarkMode = (val) => {
  if (val) { document.documentElement.classList.add('dark'); localStorage.setItem('theme-mode', 'dark'); }
  else { document.documentElement.classList.remove('dark'); localStorage.setItem('theme-mode', 'light'); }
};
const formatDate = (s) => s ? new Date(s).toLocaleDateString() : '未知';
const formatRecentTime = (s) => s ? `${new Date(s).getMonth()+1}月${new Date(s).getDate()}日` : '';
const createNewNote = () => router.push('/note/new');
const editNote = (id) => router.push(`/note/edit/${id}`);
const handleToggleStar = async (n) => { const res = await request.put(`/notes/${n.id}/star`); n.is_starred = res.is_starred; };
const handleSoftDelete = async (id) => { await request.delete(`/notes/${id}`); fetchNotes(); };
const handleRestore = async (id) => { await request.put(`/notes/${id}/restore`); fetchNotes(); };

onMounted(() => {
  fetchNotes();
  request.get('/notes/tags').then(res => tagList.value = res);
  fetchCategories();
  request.get('/notes/recent').then(res => recentNotesList.value = res);
});
</script>

<style scoped>
.layout-container { height: 100vh; }
.aside { background-color: #f8f9fa; border-right: 1px solid #e4e7ed; transition: 0.3s; }
.header { display: flex; justify-content: space-between; align-items: center; background-color: #fff; padding: 0 20px; border-bottom: 1px solid #e4e7ed; }
.main { background-color: #f0f2f5; padding: 20px; }
.note-card { height: 160px; }
.avatar-uploader { border: 1px dashed #d9d9d9; border-radius: 50%; width: 100px; height: 100px; overflow: hidden; margin: 0 auto; cursor: pointer; }
.avatar-preview { width: 100px; height: 100px; object-fit: cover; }
.recent-section { margin-bottom: 25px; }
.section-title { font-size: 15px; font-weight: bold; color: #606266; margin-bottom: 12px; }
.recent-card { cursor: pointer; border-left: 4px solid #409eff; }
html.dark .aside, html.dark .header { background-color: #1d1e1f; }
html.dark .main { background-color: #121212; }
</style>