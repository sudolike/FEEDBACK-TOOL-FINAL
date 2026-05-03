# Demo Data Seed

This folder generates a fresh, self-consistent dataset for the Feedback-tool
demo so the app screens (My Courses, Survey, Insights, AI Assistant) all have
believable data to render.

## Prerequisites

1. The Docker stack is running and healthy:
   ```powershell
   cd deploy
   .\start.bat
   ```
   The script waits for `http://localhost:9091/actuator/health/live` to return
   200 before continuing.
2. Python 3.8+ on the host with the two libraries below:
   ```powershell
   python -m pip install requests pymysql
   ```
3. The course material directory exists at:
   ```
   C:\Users\lcw68\Documents\xwechat_files\wxid_2d6657mv3ww822_de7d\msg\file\2026-05\FeedbackTool_Test_Files
   ```
   If your path differs, edit `TEST_FILES_ROOT` in `catalog.py`.

## What the seed does

1. **Wipes** `sys_courses`, `sys_course_*`, `sys_questionnaire*`, `sys_qa_*`,
   `sys_chat_message`, `sys_assignment*`, `sys_kb_chunk`, `sys_file`,
   `sys_teacher_rating` and every `sys_user` row whose role is **not** `admin`.
   The pre-baked admin accounts (`admin01`, `admin02`) are preserved.
2. **Creates** 5 teachers + 30 students with Chinese display names.
3. **Creates** 10 HKU-themed courses, all already approved by `admin01`.
4. **Uploads** every file under `FeedbackTool_Test_Files\<course_code>\` via
   `POST /file/upload` so the file ends up on the backend container's
   `/data/files` volume **and** registers a row in `sys_course_resource`.
5. **Creates** 3 questionnaire templates (rating / single / multiple / text mix)
   and binds them to courses (the demo course gets all three).
6. **Generates** student answers for ~70% of enrolled students (≥95% for the
   demo course) with realistic rating distributions and short comments.
7. **Generates** course-feedback rows (rating + free-text comment) for ~60% of
   enrollments.
8. **POSTs** `/assistant/kb/rebuild` so the RAG knowledge base is regenerated
   from the new courses / resources / Q&A.

## Demo course

`COMP7506 — Smart Phone Apps Development` is the headline demo course. It owns
the full set of file formats (PNG / PDF / PPTX / MP4) and gets the highest
enrollment + the highest questionnaire response rate so the analytics screen,
the AI assistant and the file viewer all look great when you open it.

## Accounts you can log into

| Username   | Password                  | Role     |
|------------|---------------------------|----------|
| `admin01`  | `Admin@Cen2026!Feedback`  | admin    |
| `admin02`  | (existing pre-baked)      | admin    |
| `teacher`  | `teacher`                 | teacher  |
| `t_zhang`  | `Teacher@123`             | teacher  |
| `t_li`     | `Teacher@123`             | teacher  |
| `t_wang`   | `Teacher@123`             | teacher  |
| `t_chen`   | `Teacher@123`             | teacher  |
| `student`  | `student`                 | student  |
| `s002`-`s030` | `Student@123`          | student  |

## Run

```powershell
cd deploy\seed_demo
python seed.py
```

If you want a different randomness, set `SEED_RNG` (defaults to `20260502`):

```powershell
$env:SEED_RNG = "12345"
python seed.py
```

The script prints a short report at the end showing per-course enrollment,
resource, questionnaire-response and feedback counts so you can verify the
demo before opening the app.
