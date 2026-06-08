package com.example.notesystem.config;

import com.example.notesystem.entity.Category;
import com.example.notesystem.entity.Note;
import com.example.notesystem.entity.User;
import com.example.notesystem.repository.CategoryRepository;
import com.example.notesystem.repository.NoteRepository;
import com.example.notesystem.repository.UserRepository;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Component
@Profile("demo")
public class DemoDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final NoteRepository noteRepository;

    public DemoDataInitializer(UserRepository userRepository,
                               CategoryRepository categoryRepository,
                               NoteRepository noteRepository) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.noteRepository = noteRepository;
    }

    @Override
    public void run(String... args) {
        if (userRepository.findByUsername("admin").isPresent()) {
            return;
        }

        User admin = createUser("admin", "admin@example.com", "ADMIN", "系统管理员");
        User student = createUser("student", "student@example.com", "USER", "学习用户");
        userRepository.saveAll(List.of(admin, student));

        Category backend = createCategory(student.getId(), "Java后端");
        Category frontend = createCategory(student.getId(), "Vue前端");
        Category database = createCategory(student.getId(), "数据库");
        Category review = createCategory(student.getId(), "课程复习");
        categoryRepository.saveAll(List.of(backend, frontend, database, review));

        noteRepository.saveAll(List.of(
                createNote(student.getId(), backend.getId(), "Spring Boot REST接口设计", "控制器接收请求，Service处理业务逻辑。", 1, 1),
                createNote(student.getId(), backend.getId(), "JWT登录认证流程", "登录成功后生成Token，前端放入Authorization请求头。", 1, 1),
                createNote(student.getId(), frontend.getId(), "Vue路由守卫", "未登录用户访问业务页面时跳转登录页。", 0, 1),
                createNote(student.getId(), frontend.getId(), "Element Plus表单校验", "使用rules配置用户名、邮箱和密码校验。", 0, 1),
                createNote(student.getId(), database.getId(), "MySQL外键关系", "用户、分类、笔记和标签之间通过外键关联。", 0, 1),
                createNote(student.getId(), database.getId(), "多表关联查询", "笔记列表支持分类、标签、星标、日期组合筛选。", 1, 1),
                createNote(student.getId(), review.getId(), "全栈项目答辩要点", "演示登录、CRUD、分页、角色、统计看板。", 1, 1),
                createNote(student.getId(), review.getId(), "回收站测试笔记", "这是一条用于演示恢复和永久删除的笔记。", 0, 0)
        ));
    }

    private User createUser(String username, String email, String role, String nickname) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setRole(role);
        user.setNickname(nickname);
        user.setPassword(BCrypt.hashpw("password", BCrypt.gensalt()));
        return user;
    }

    private Category createCategory(Long userId, String name) {
        Category category = new Category();
        category.setUserId(userId);
        category.setName(name);
        category.setParentId(0L);
        return category;
    }

    private Note createNote(Long userId, Long categoryId, String title, String text, Integer starred, Integer status) {
        Note note = new Note();
        note.setUserId(userId);
        note.setCategoryId(categoryId);
        note.setTitle(title);
        note.setContent("<p>" + text + "</p>");
        note.setContentText(text);
        note.setIsStarred(starred);
        note.setStatus(status);
        note.setLastAccessedAt(new Date());
        return note;
    }
}
