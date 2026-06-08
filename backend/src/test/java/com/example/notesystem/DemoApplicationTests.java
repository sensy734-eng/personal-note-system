package com.example.notesystem;

import com.example.notesystem.entity.Note;
import com.example.notesystem.repository.NoteRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.text.SimpleDateFormat;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("demo")
class DemoApplicationTests {

    @Autowired
    private NoteRepository noteRepository;

    @Test
    void contextLoads() {
    }

    @Test
    void dateRangeIncludesEntireEndDate() throws Exception {
        Note note = new Note();
        note.setUserId(9001L);
        note.setTitle("date range note");
        note.setContent("content");
        note.setContentText("date range content");
        note.setStatus(1);
        note.setUpdatedAt(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2026-06-08 16:30:00"));
        noteRepository.saveAndFlush(note);

        Date startDate = new SimpleDateFormat("yyyy-MM-dd").parse("2026-06-08");
        Date endDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").parse("2026-06-08 23:59:59.999");

        assertThat(noteRepository.findPageByFilters(
                9001L,
                1,
                "date range",
                null,
                null,
                startDate,
                endDate,
                null,
                PageRequest.of(0, 9)).getContent())
                .extracting(Note::getId)
                .contains(note.getId());
    }
}
