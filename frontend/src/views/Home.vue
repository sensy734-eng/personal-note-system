<template>
  <el-container class="layout-container">
    <el-aside width="250px" class="aside">
      <div class="logo"><h3>我的学习笔记</h3></div>
      <el-menu :default-active="currentFilter" class="menu" @select="handleMenuSelect">
        <el-menu-item index="statistics"><el-icon><DataLine /></el-icon><span>数据看板</span></el-menu-item>
        <el-menu-item v-if="isAdmin" index="admin"><el-icon><Setting /></el-icon><span>管理员面板</span></el-menu-item>
        <el-menu-item index="all"><el-icon><Document /></el-icon><span>全部笔记</span></el-menu-item>
        <el-menu-item index="starred"><el-icon><Star /></el-icon><span>星标笔记</span></el-menu-item>
        <el-sub-menu index="categories">
          <template #title><el-icon><Folder /></el-icon><span>我的分类</span></template>
          <el-menu-item v-for="cate in categoryList" :key="cate.id" :index="'cat_' + cate.id">
            <el-icon><CollectionTag /></el-icon>{{ cate.name }}
          </el-menu-item>
          <el-menu-item index="manage_categories" class="manage-entry"><el-icon><Setting /></el-icon>管理分类...</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="tags">
          <template #title><el-icon><PriceTag /></el-icon><span>我的标签</span></template>
          <el-menu-item v-for="tag in tagList" :key="tag.id" :index="'tag_' + tag.name">
            <el-icon><Collection /></el-icon>{{ tag.name }}
          </el-menu-item>
        </el-sub-menu>
        <el-menu-item index="trash"><el-icon><Delete /></el-icon><span>回收站</span></el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-input v-model="searchQuery" placeholder="全文检索..." clearable @input="handleSearch">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-date-picker v-model="dateRange" type="daterange" value-format="YYYY-MM-DD" @change="handleSearch" />
        </div>
        <div class="header-right">
          <el-switch v-model="isDark" @change="toggleDarkMode" inline-prompt :active-icon="Moon" :inactive-icon="Sunny" />
          <el-button v-if="currentFilter.startsWith('cat_')" type="warning" plain icon="Box" @click="handleBatchExport">备份分类</el-button>
          <el-dropdown>
            <span class="el-dropdown-link">
              <el-avatar :size="32" :src="userInfo.avatar || defaultAvatar" />
              {{ userInfo.nickname || userInfo.username }}
              <el-tag v-if="isAdmin" size="small" type="danger">管理员</el-tag>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="profileDialogVisible = true">个人设置</el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-button type="primary" @click="createNewNote"><el-icon><Plus /></el-icon>新建笔记</el-button>
        </div>
      </el-header>

      <el-main class="main">
        <DataStatistics v-if="currentFilter === 'statistics'" />

        <section v-else-if="currentFilter === 'admin'" class="admin-panel" v-loading="adminLoading">
          <el-row :gutter="16">
            <el-col :xs="24" :sm="12" :md="6">
              <el-card><div class="stat-title">用户总数</div><div class="stat-value">{{ adminOverview.totalUsers || 0 }}</div></el-card>
            </el-col>
            <el-col :xs="24" :sm="12" :md="6">
              <el-card><div class="stat-title">笔记总数</div><div class="stat-value">{{ adminOverview.totalNotes || 0 }}</div></el-card>
            </el-col>
            <el-col :xs="24" :sm="12" :md="6">
              <el-card><div class="stat-title">正常笔记</div><div class="stat-value">{{ adminOverview.activeNotes || 0 }}</div></el-card>
            </el-col>
            <el-col :xs="24" :sm="12" :md="6">
              <el-card><div class="stat-title">分类总数</div><div class="stat-value">{{ adminOverview.totalCategories || 0 }}</div></el-card>
            </el-col>
          </el-row>
          <el-card class="admin-users" shadow="never">
            <template #header>用户列表</template>
            <el-table :data="adminUsers" empty-text="暂无用户">
              <el-table-column prop="id" label="ID" width="80" />
              <el-table-column prop="username" label="用户名" />
              <el-table-column prop="email" label="邮箱" />
              <el-table-column prop="role" label="角色" width="110" />
              <el-table-column prop="nickname" label="昵称" />
            </el-table>
          </el-card>
        </section>

        <section v-else>
          <div v-if="currentFilter === 'all' && !searchQuery && recentNotesList.length > 0" class="recent-section">
            <div class="section-title">最近访问</div>
            <el-row :gutter="15">
              <el-col :xs="24" :sm="12" :md="6" v-for="recent in recentNotesList" :key="'recent-' + recent.id">
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

          <div v-loading="loading" class="notes-area">
            <el-row :gutter="20" v-if="notesList.length > 0">
              <el-col :xs="24" :sm="12" :lg="8" v-for="note in notesList" :key="note.id" class="note-col">
                <el-card class="note-card" shadow="hover">
                  <template #header>
                    <div class="card-header">
                      <span class="note-title">{{ note.title }}</span>
                      <el-icon v-if="currentFilter !== 'trash'" :color="note.is_starred ? '#e6a23c' : '#c0c4cc'" @click.stop="handleToggleStar(note)">
                        <StarFilled v-if="note.is_starred" /><Star v-else />
                      </el-icon>
                    </div>
                  </template>
                  <div class="note-summary">{{ note.content_text ? note.content_text.substring(0, 60) + '...' : '暂无内容' }}</div>
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
            <el-empty v-else-if="!loading" description="这里空空如也" />
          </div>

          <div class="pagination-row" v-if="totalNotes > pageSize">
            <el-pagination
              v-model:current-page="currentPage"
              v-model:page-size="pageSize"
              :page-sizes="[6, 9, 12, 18]"
              layout="total, sizes, prev, pager, next, jumper"
              :total="totalNotes"
              @current-change="fetchNotes"
              @size-change="handlePageSizeChange"
            />
          </div>
        </section>
      </el-main>
    </el-container>

    <el-dialog v-model="categoryDialogVisible" title="分类管理" width="500px">
      <div class="category-tools">
        <el-input v-model="newCategoryName" placeholder="输入新分类名称..." @keyup.enter="handleAddCategory" />
        <el-button type="primary" @click="handleAddCategory">添加分类</el-button>
      </div>
      <el-table :data="categoryList" max-height="300">
        <el-table-column prop="name" label="分类名称">
          <template #default="scope">
            <el-input v-if="editingCategoryId === scope.row.id" v-model="editingCategoryName" size="small" @blur="handleSaveCategory(scope.row)" />
            <span v-else>{{ scope.row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="scope">
            <el-button link type="primary" @click="startEditCategory(scope.row)"><el-icon><Edit /></el-icon></el-button>
            <el-button link type="danger" @click="handleDeleteCategory(scope.row.id)"><el-icon><Delete /></el-icon></el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog v-model="profileDialogVisible" title="个人设置" width="450px">
      <el-form label-width="80px">
        <el-form-item label="我的头像">
          <el-upload class="avatar-uploader" :action="avatarUploadUrl" :headers="uploadHeaders" :show-file-list="false" :on-success="handleAvatarSuccess">
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
import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import request, { buildApiUrl } from '../utils/request';
import DataStatistics from '../components/DataStatistics.vue';
import {
  Box, Collection, CollectionTag, DataLine, Delete, Document, Edit,
  Folder, Moon, Plus, PriceTag, Search, Setting, Star, StarFilled, Sunny
} from '@element-plus/icons-vue';

const router = useRouter();
const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png';

const notesList = ref([]);
const recentNotesList = ref([]);
const categoryList = ref([]);
const tagList = ref([]);
const loading = ref(false);
const searchQuery = ref('');
const dateRange = ref([]);
const currentFilter = ref('all');
const currentPage = ref(1);
const pageSize = ref(9);
const totalNotes = ref(0);
const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || '{}'));
const isAdmin = computed(() => userInfo.value.role === 'ADMIN');

const categoryDialogVisible = ref(false);
const newCategoryName = ref('');
const editingCategoryId = ref(null);
const editingCategoryName = ref('');

const profileDialogVisible = ref(false);
const profileForm = reactive({ nickname: userInfo.value.nickname || '', signature: userInfo.value.signature || '' });
const uploadHeaders = computed(() => ({ Authorization: `Bearer ${localStorage.getItem('token')}` }));
const avatarUploadUrl = computed(() => buildApiUrl('/auth/avatar'));

const adminLoading = ref(false);
const adminOverview = ref({});
const adminUsers = ref([]);

const fetchNotes = async () => {
  loading.value = true;
  try {
    const params = { status: currentFilter.value === 'trash' ? 0 : 1, page: currentPage.value, size: pageSize.value };
    if (searchQuery.value) params.keyword = searchQuery.value;
    if (currentFilter.value === 'starred') params.isStarred = 1;
    if (currentFilter.value.startsWith('cat_')) params.categoryId = currentFilter.value.split('_')[1];
    if (currentFilter.value.startsWith('tag_')) params.tagName = currentFilter.value.split('_')[1];
    if (dateRange.value?.length === 2) {
      params.startDate = dateRange.value[0];
      params.endDate = dateRange.value[1];
    }
    const res = await request.get('/notes', { params });
    notesList.value = res.records || res.data || [];
    totalNotes.value = Number(res.total || notesList.value.length);
  } finally {
    loading.value = false;
  }
};

const fetchCategories = async () => {
  categoryList.value = await request.get('/categories');
};

const fetchAdminData = async () => {
  if (!isAdmin.value) return;
  adminLoading.value = true;
  try {
    const [overview, users] = await Promise.all([
      request.get('/admin/overview'),
      request.get('/admin/users')
    ]);
    adminOverview.value = overview;
    adminUsers.value = users;
  } finally {
    adminLoading.value = false;
  }
};

const handleSearch = () => {
  currentPage.value = 1;
  fetchNotes();
};

const handlePageSizeChange = () => {
  currentPage.value = 1;
  fetchNotes();
};

const handleAddCategory = async () => {
  if (!newCategoryName.value.trim()) return ElMessage.warning('名称不能为空');
  await request.post('/categories', { name: newCategoryName.value.trim() });
  newCategoryName.value = '';
  ElMessage.success('添加成功');
  await fetchCategories();
};

const startEditCategory = (row) => {
  editingCategoryId.value = row.id;
  editingCategoryName.value = row.name;
};

const handleSaveCategory = async (row) => {
  if (!editingCategoryName.value.trim()) {
    editingCategoryId.value = null;
    return;
  }
  await request.put(`/categories/${row.id}`, { name: editingCategoryName.value.trim() });
  editingCategoryId.value = null;
  await fetchCategories();
};

const handleDeleteCategory = (id) => {
  ElMessageBox.confirm('删除分类后，原分类下的笔记将变为无分类状态，确认吗？', '提示').then(async () => {
    await request.delete(`/categories/${id}`);
    ElMessage.success('删除成功');
    await fetchCategories();
    await fetchNotes();
  }).catch(() => {});
};

const handlePermanentDelete = (id) => {
  ElMessageBox.confirm('彻底删除后将无法恢复，确认继续吗？', '严重警告', { type: 'warning' }).then(async () => {
    await request.delete(`/notes/${id}/permanent`);
    ElMessage.success('已永久删除');
    await fetchNotes();
  }).catch(() => {});
};

const handleMenuSelect = (index) => {
  if (index === 'manage_categories') {
    categoryDialogVisible.value = true;
    return;
  }
  currentFilter.value = index;
  currentPage.value = 1;
  if (index === 'admin') fetchAdminData();
  else if (index !== 'statistics') fetchNotes();
};

const handleSingleExport = (id) => {
  window.open(`${buildApiUrl(`/notes/${id}/export`)}?type=md&token=${localStorage.getItem('token')}`);
};

const handleBatchExport = () => {
  const categoryId = currentFilter.value.split('_')[1];
  window.open(`${buildApiUrl('/notes/export/category')}?categoryId=${categoryId}&type=md&token=${localStorage.getItem('token')}`);
};

const saveProfile = async () => {
  const res = await request.put('/auth/profile', profileForm);
  userInfo.value = { ...userInfo.value, ...res.user };
  localStorage.setItem('userInfo', JSON.stringify(userInfo.value));
  profileDialogVisible.value = false;
  ElMessage.success('更新成功');
};

const handleAvatarSuccess = (res) => {
  userInfo.value.avatar = res.url;
  localStorage.setItem('userInfo', JSON.stringify(userInfo.value));
  ElMessage.success('头像上传成功');
};

const handleLogout = () => {
  localStorage.clear();
  router.push('/login');
};

const isDark = ref(document.documentElement.classList.contains('dark'));
const toggleDarkMode = (val) => {
  if (val) {
    document.documentElement.classList.add('dark');
    localStorage.setItem('theme-mode', 'dark');
  } else {
    document.documentElement.classList.remove('dark');
    localStorage.setItem('theme-mode', 'light');
  }
};

const formatDate = (value) => value ? new Date(value).toLocaleDateString() : '未知';
const formatRecentTime = (value) => value ? `${new Date(value).getMonth() + 1}月${new Date(value).getDate()}日` : '';
const createNewNote = () => router.push('/note/new');
const editNote = (id) => router.push(`/note/edit/${id}`);
const handleToggleStar = async (note) => {
  const res = await request.put(`/notes/${note.id}/star`);
  note.is_starred = res.is_starred;
};
const handleSoftDelete = async (id) => {
  await request.delete(`/notes/${id}`);
  await fetchNotes();
};
const handleRestore = async (id) => {
  await request.put(`/notes/${id}/restore`);
  await fetchNotes();
};

onMounted(async () => {
  await fetchNotes();
  tagList.value = await request.get('/notes/tags');
  await fetchCategories();
  recentNotesList.value = await request.get('/notes/recent');
});
</script>

<style scoped>
.layout-container { min-height: 100vh; }
.aside { background-color: #f8f9fa; border-right: 1px solid #e4e7ed; transition: 0.3s; }
.logo { padding: 12px 18px; }
.manage-entry { color: #409eff; }
.header { display: flex; justify-content: space-between; align-items: center; gap: 16px; background-color: #fff; padding: 0 20px; border-bottom: 1px solid #e4e7ed; }
.header-left, .header-right { display: flex; align-items: center; gap: 12px; }
.header-left .el-input { width: 220px; }
.main { background-color: #f0f2f5; padding: 20px; }
.el-dropdown-link { cursor: pointer; display: flex; align-items: center; gap: 8px; }
.notes-area { min-height: 260px; }
.note-col { margin-bottom: 20px; }
.note-card { min-height: 170px; }
.card-header, .note-footer, .recent-card-content { display: flex; justify-content: space-between; gap: 12px; align-items: center; }
.note-title, .recent-title { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-weight: 600; }
.note-summary { min-height: 54px; color: #606266; line-height: 1.6; }
.time, .recent-time { color: #909399; font-size: 13px; }
.avatar-uploader { border: 1px dashed #d9d9d9; border-radius: 50%; width: 100px; height: 100px; overflow: hidden; margin: 0 auto; cursor: pointer; display: flex; align-items: center; justify-content: center; }
.avatar-preview { width: 100px; height: 100px; object-fit: cover; }
.recent-section { margin-bottom: 25px; }
.section-title { font-size: 15px; font-weight: bold; color: #606266; margin-bottom: 12px; }
.recent-card { cursor: pointer; border-left: 4px solid #409eff; margin-bottom: 12px; }
.category-tools { margin-bottom: 20px; display: flex; gap: 10px; }
.pagination-row { display: flex; justify-content: center; margin-top: 8px; }
.admin-panel .el-col { margin-bottom: 16px; }
.admin-users { margin-top: 12px; }
.stat-title { color: #606266; margin-bottom: 8px; }
.stat-value { font-size: 26px; font-weight: bold; }
html.dark .aside, html.dark .header { background-color: #1d1e1f; }
html.dark .main { background-color: #121212; }

@media (max-width: 900px) {
  .layout-container { display: block; }
  .aside { width: 100% !important; border-right: none; border-bottom: 1px solid #e4e7ed; }
  .header { height: auto; flex-wrap: wrap; padding: 12px; }
  .header-left, .header-right { flex-wrap: wrap; width: 100%; }
  .header-left .el-input, .header-left .el-date-editor { width: 100% !important; }
}
</style>
