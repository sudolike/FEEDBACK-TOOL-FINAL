# -*- coding: utf-8 -*-
"""
Demo data seed script for the Feedback-tool platform.

Wipes business data (keeps the two pre-baked admin accounts) and rebuilds a
self-consistent dataset that matches the catalog defined in catalog.py:
   - 5 teachers (4 new + the existing demo `teacher` account)
   - 30 students (29 new + the existing demo `student` account)
   - 10 HKU courses (all approved by admin01)
   - All physical files under FeedbackTool_Test_Files uploaded via /file/upload
     and registered in sys_course_resource for their corresponding course
   - 3 questionnaire templates bound to courses with realistic student answers
   - sys_course_feedback ratings + comments with believable distribution
   - sys_kb_chunk rebuilt at the end so the AI assistant has fresh content

Run:
    python deploy/seed_demo/seed.py
"""

from __future__ import annotations

import json
import os
import random
import sys
import time
from datetime import datetime, timedelta
from pathlib import Path
from typing import Dict, Any, List, Tuple

import pymysql
import requests

# Make catalog.py / answers.py importable when invoked from any cwd.
HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))

from catalog import (
    TEST_FILES_ROOT, TEACHERS, STUDENTS, COURSES, QUESTIONNAIRES,
    COURSE_QUESTIONNAIRE_BIND, DEMO_COURSE_CODE,
)
from answers import (
    build_answers_json, make_course_feedback, random_recent_datetime,
)

# --------------------------------------------------------------------------- #
#  Connection / endpoint configuration                                        #
# --------------------------------------------------------------------------- #

DB_CONF = {
    "host": os.getenv("DB_HOST_LOCAL", "127.0.0.1"),
    "port": int(os.getenv("DB_PORT_HOST", "3307")),
    "user": "root",
    "password": os.getenv("DB_PASSWORD", "feedback123"),
    "database": os.getenv("DB_NAME", "feedback_1"),
    "charset": "utf8mb4",
    # IMPORTANT: keep autocommit=True for the seed connection so that every
    # SELECT during step 4 (upload_course_files) starts a fresh read view.
    # Otherwise MySQL's REPEATABLE READ snapshot pins us to the state at the
    # first SELECT and we cannot see rows the backend INSERTs into sys_file
    # via /file/upload (a separate connection).
    "autocommit": True,
}

BACKEND_BASE = os.getenv("BACKEND_BASE_URL", "http://127.0.0.1:9091")
RNG_SEED = int(os.getenv("SEED_RNG", "20260502"))

random.seed(RNG_SEED)

# --------------------------------------------------------------------------- #
#  Logging helpers                                                            #
# --------------------------------------------------------------------------- #

def log(msg: str):
    print(f"[seed] {msg}", flush=True)


def log_step(label: str):
    print(f"\n=== {label} ===", flush=True)


# --------------------------------------------------------------------------- #
#  Step 1 — wipe business data, keep admin01/admin02                          #
# --------------------------------------------------------------------------- #

WIPE_SQL = [
    # children first to respect FK-like usage patterns even though the schema
    # has no explicit FOREIGN KEYs
    "DELETE FROM sys_kb_chunk",
    "DELETE FROM sys_chat_message",
    "DELETE FROM sys_qa_reply",
    "DELETE FROM sys_qa_post",
    "DELETE FROM sys_questionnaire_responses",
    "DELETE FROM sys_course_questionnaire",
    "DELETE FROM sys_questionnaires",
    "DELETE FROM sys_assignment_submission",
    "DELETE FROM sys_assignment",
    "DELETE FROM sys_course_resource",
    "DELETE FROM sys_course_feedback",
    "DELETE FROM sys_teacher_rating",
    "DELETE FROM sys_course_students",
    "DELETE FROM sys_courses",
    "DELETE FROM sys_file",
    # keep admin01 (id=101) and admin02 (id=102); we'll merge other accounts back in later
    "DELETE FROM sys_user WHERE role <> 'admin'",
]


def wipe_business_data(conn):
    log_step("step 1 / wipe existing business data")
    with conn.cursor() as c:
        c.execute("SET FOREIGN_KEY_CHECKS=0")
        for sql in WIPE_SQL:
            c.execute(sql)
            log(f"  {sql}  (rows affected: {c.rowcount})")
        c.execute("SET FOREIGN_KEY_CHECKS=1")
    conn.commit()


# --------------------------------------------------------------------------- #
#  Step 2 — create teachers + students                                        #
# --------------------------------------------------------------------------- #

# role_id mapping (matches sys_role rows we already saw):
#   admin -> 1, teacher -> 3, student -> 2
ROLE_ID = {"admin": 1, "teacher": 3, "student": 2}


def insert_users(conn) -> Tuple[Dict[str, int], Dict[str, int]]:
    """Insert teachers + students. Returns (teacher_username -> id, student_username -> id)."""
    log_step("step 2 / create teachers and students")

    teacher_ids: Dict[str, int] = {}
    student_ids: Dict[str, int] = {}

    with conn.cursor() as c:
        for t in TEACHERS:
            c.execute(
                "INSERT INTO sys_user (username, password, nickname, email, role, role_id, status)"
                " VALUES (%s, %s, %s, %s, 'teacher', %s, 1)",
                (t["username"], t["password"], t["nickname"], t["email"], ROLE_ID["teacher"]),
            )
            teacher_ids[t["username"]] = c.lastrowid
            log(f"  teacher {t['username']:<10} id={c.lastrowid}  {t['nickname']}")

        for s in STUDENTS:
            c.execute(
                "INSERT INTO sys_user (username, password, nickname, email, role, role_id, status)"
                " VALUES (%s, %s, %s, %s, 'student', %s, 1)",
                (s["username"], s["password"], s["nickname"], s["email"], ROLE_ID["student"]),
            )
            student_ids[s["username"]] = c.lastrowid
        log(f"  inserted {len(student_ids)} students")
    conn.commit()
    return teacher_ids, student_ids


# --------------------------------------------------------------------------- #
#  Step 3 — create approved courses                                           #
# --------------------------------------------------------------------------- #

def insert_courses(conn, teacher_ids: Dict[str, int]) -> Dict[str, int]:
    """Insert all 10 courses as already-approved (so students can see them)."""
    log_step("step 3 / create approved courses")
    course_ids: Dict[str, int] = {}
    admin_id = 101  # admin01
    now = datetime.now()
    with conn.cursor() as c:
        for course in COURSES:
            tid = teacher_ids.get(course["teacher_username"])
            if tid is None:
                raise RuntimeError(f"teacher not found: {course['teacher_username']}")
            c.execute(
                """
                INSERT INTO sys_courses
                  (name, code, teacher_id, description, academic_year, semester,
                   course_time, location, status, reviewed_by, reviewed_at, created_at, updated_at)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, 'approved', %s, %s, %s, %s)
                """,
                (
                    course["name"], course["code"], tid, course["description"],
                    course["academic_year"], course["semester"],
                    course["course_time"], course["location"],
                    admin_id, now, now, now,
                ),
            )
            course_ids[course["code"]] = c.lastrowid
            log(f"  {course['code']:<10} id={c.lastrowid}  teacher={course['teacher_username']}")
    conn.commit()
    return course_ids


# --------------------------------------------------------------------------- #
#  Step 4 — upload all course resource files via /file/upload                 #
# --------------------------------------------------------------------------- #

# Map suffix in the file name to the platform's "category" enum.
def _category_for(filename: str) -> str:
    n = filename.upper()
    if "VEDIO" in n or n.endswith(".MP4") or n.endswith(".MOV"):
        return "recording"
    if n.endswith(".PDF") or n.endswith(".PPT") or n.endswith(".PPTX"):
        return "lecture"
    if n.endswith(".PNG") or n.endswith(".JPG") or n.endswith(".JPEG"):
        return "lecture"
    return "other"


def upload_course_files(conn, course_ids: Dict[str, int], teacher_ids: Dict[str, int]):
    log_step("step 4 / upload course resource files via /file/upload")
    if not TEST_FILES_ROOT.exists():
        raise RuntimeError(f"test files root not found: {TEST_FILES_ROOT}")

    inserted = 0
    with conn.cursor() as c:
        for code, cid in course_ids.items():
            course_dir = TEST_FILES_ROOT / code
            if not course_dir.is_dir():
                log(f"  WARN  {code}: directory missing, skipped")
                continue
            files = sorted(course_dir.iterdir())
            if not files:
                log(f"  WARN  {code}: empty directory, skipped")
                continue

            # Resolve uploader (the teacher who owns the course)
            teacher_username = next(
                (cc["teacher_username"] for cc in COURSES if cc["code"] == code), None
            )
            uploader_id = teacher_ids[teacher_username]

            log(f"  {code}: {len(files)} file(s)")
            for f in files:
                # 1. POST /file/upload  ——  multipart upload, get back relative URL
                with open(f, "rb") as fh:
                    resp = requests.post(
                        f"{BACKEND_BASE}/file/upload",
                        files={"file": (f.name, fh)},
                        timeout=180,
                    )
                if resp.status_code != 200:
                    raise RuntimeError(f"upload failed for {f.name}: HTTP {resp.status_code}: {resp.text[:200]}")
                url = resp.text.strip().strip('"')

                # 2. The backend already inserted into sys_file. Look up the row to
                #    get the file_id (we need it to register sys_course_resource).
                c.execute("SELECT id, name, type, size FROM sys_file WHERE url = %s ORDER BY id DESC LIMIT 1", (url,))
                row = c.fetchone()
                if row is None:
                    raise RuntimeError(f"sys_file row not found for url={url}")
                file_id, file_name, file_type, file_size_kb = row
                file_size_bytes = (file_size_kb or 0) * 1024  # backend stored size in KB

                # 3. Register a sys_course_resource pointing at the file.
                title = f.stem.replace(f"{code}-", f"{code} ")  # e.g. "COMP7506 PDF"
                category = _category_for(f.name)
                c.execute(
                    """
                    INSERT INTO sys_course_resource
                      (course_id, uploader_id, uploader_role, title, description,
                       file_id, file_name, file_url, file_type, file_size,
                       category, download_count, created_at, updated_at, is_deleted)
                    VALUES (%s, %s, 'teacher', %s, %s, %s, %s, %s, %s, %s, %s, %s, NOW(), NOW(), 0)
                    """,
                    (
                        cid, uploader_id, title,
                        f"Demo material for {code}",
                        file_id, file_name, url, file_type, file_size_bytes,
                        category, random.randint(0, 30),
                    ),
                )
                inserted += 1
                log(f"    -> {f.name:<28} url={url}  category={category}")
    conn.commit()
    log(f"  total course resources inserted: {inserted}")


# --------------------------------------------------------------------------- #
#  Step 5 — questionnaires + bindings                                          #
# --------------------------------------------------------------------------- #

def insert_questionnaires(conn, teacher_ids: Dict[str, int]) -> List[int]:
    log_step("step 5 / create questionnaire templates")
    q_ids: List[int] = []
    with conn.cursor() as c:
        for q in QUESTIONNAIRES:
            creator = teacher_ids[q["creator_username"]]
            c.execute(
                "INSERT INTO sys_questionnaires (title, description, created_by, questions, created_at, updated_at)"
                " VALUES (%s, %s, %s, %s, NOW(), NOW())",
                (q["title"], q["description"], creator,
                 json.dumps(q["questions"], ensure_ascii=False)),
            )
            q_ids.append(c.lastrowid)
            log(f"  questionnaire #{c.lastrowid}: {q['title']}  ({len(q['questions'])} questions)")
    conn.commit()
    return q_ids


def bind_questionnaires_to_courses(conn, course_ids: Dict[str, int], q_ids: List[int]):
    log_step("step 5b / bind questionnaires to courses (status=1 published)")
    bound = 0
    with conn.cursor() as c:
        for code, q_indexes in COURSE_QUESTIONNAIRE_BIND.items():
            cid = course_ids.get(code)
            if cid is None:
                continue
            for qi in q_indexes:
                qid = q_ids[qi]
                c.execute(
                    "INSERT INTO sys_course_questionnaire (course_id, questionnaire_id, status, created_at)"
                    " VALUES (%s, %s, 1, NOW())",
                    (cid, qid),
                )
                bound += 1
    conn.commit()
    log(f"  total course-questionnaire bindings: {bound}")


# --------------------------------------------------------------------------- #
#  Step 6 — enrollments + answers + course feedback                           #
# --------------------------------------------------------------------------- #

# Each student takes 5 courses; demo course (COMP7506) gets boosted enrollment so
# the analysis screen always has plenty of data.
COURSES_PER_STUDENT = 5
MIN_STUDENTS_PER_COURSE = 5

def assign_enrollments(course_ids: Dict[str, int], student_ids: Dict[str, int]) -> Dict[int, List[int]]:
    """
    Returns {course_id: [student_id, ...]} satisfying:
      * every student is enrolled in exactly COURSES_PER_STUDENT courses
      * every course has at least MIN_STUDENTS_PER_COURSE students
    Demo course gets the highest weight so it has rich data.
    """
    course_id_list = list(course_ids.values())
    demo_cid = course_ids[DEMO_COURSE_CODE]

    enrollments: Dict[int, List[int]] = {cid: [] for cid in course_id_list}
    for sid in student_ids.values():
        # Always include the demo course for every student so it has 30 enrollments.
        picked = {demo_cid}
        # Random fill the remaining 4 from other courses, weighted slightly so the
        # distribution is not perfectly uniform.
        remaining = [c for c in course_id_list if c != demo_cid]
        random.shuffle(remaining)
        for cid in remaining:
            if len(picked) >= COURSES_PER_STUDENT:
                break
            picked.add(cid)
        for cid in picked:
            enrollments[cid].append(sid)

    # Sanity: ensure every course has >= MIN_STUDENTS_PER_COURSE
    for cid, students in enrollments.items():
        if len(students) < MIN_STUDENTS_PER_COURSE:
            # Should not happen with current ratio (30 students × 5 = 150, 10 courses → avg 15)
            extra_needed = MIN_STUDENTS_PER_COURSE - len(students)
            pool = [s for s in student_ids.values() if s not in students]
            students.extend(random.sample(pool, extra_needed))
    return enrollments


def insert_enrollments(conn, enrollments: Dict[int, List[int]]):
    log_step("step 6a / insert sys_course_students (approved)")
    total = 0
    with conn.cursor() as c:
        for cid, sids in enrollments.items():
            for sid in sids:
                c.execute(
                    """
                    INSERT INTO sys_course_students
                      (course_id, student_id, status, source, apply_message, reviewed_at, created_at)
                    VALUES (%s, %s, 'approved', %s, %s, NOW(), NOW())
                    """,
                    (
                        cid, sid,
                        random.choice(["student_apply", "teacher_invite"]),
                        random.choice([None, "希望学习这门课的核心内容", "对该方向感兴趣"]),
                    ),
                )
                total += 1
    conn.commit()
    log(f"  enrollments inserted: {total}")


def insert_questionnaire_responses(conn, course_ids: Dict[str, int],
                                   enrollments: Dict[int, List[int]],
                                   q_ids: List[int]):
    """
    For every course-questionnaire binding, ~70% of the students enrolled in
    that course submit answers. Demo course gets nearly 100% submission so the
    analytics page lights up nicely.
    """
    log_step("step 6b / generate student questionnaire answers")
    cid_to_demo = {course_ids[DEMO_COURSE_CODE]}
    inserted = 0
    with conn.cursor() as c:
        for code, q_indexes in COURSE_QUESTIONNAIRE_BIND.items():
            cid = course_ids[code]
            student_ids = enrollments.get(cid, [])
            for qi in q_indexes:
                qid = q_ids[qi]
                qdef = QUESTIONNAIRES[qi]
                rate = 0.95 if cid in cid_to_demo else 0.7
                for sid in student_ids:
                    if random.random() > rate:
                        continue
                    answers_json = build_answers_json(qdef["questions"], sid)
                    submitted_at = random_recent_datetime(days_back=30)
                    c.execute(
                        """
                        INSERT INTO sys_questionnaire_responses
                          (course_id, questionnaire_id, student_id, answers, submitted_at)
                        VALUES (%s, %s, %s, %s, %s)
                        """,
                        (cid, qid, sid, answers_json, submitted_at),
                    )
                    inserted += 1
    conn.commit()
    log(f"  questionnaire responses inserted: {inserted}")


def insert_course_feedback(conn, course_ids: Dict[str, int],
                           enrollments: Dict[int, List[int]]):
    """About 60% of enrolled students leave a written course evaluation."""
    log_step("step 6c / insert course feedback (rating + content)")
    inserted = 0
    demo_cid = course_ids[DEMO_COURSE_CODE]
    with conn.cursor() as c:
        for cid, sids in enrollments.items():
            rate = 0.9 if cid == demo_cid else 0.6
            for sid in sids:
                if random.random() > rate:
                    continue
                fb = make_course_feedback(sid)
                created_at = random_recent_datetime(days_back=45)
                c.execute(
                    """
                    INSERT INTO sys_course_feedback
                      (course_id, student_id, content, rating, created_at, updated_at)
                    VALUES (%s, %s, %s, %s, %s, %s)
                    """,
                    (cid, sid, fb["content"], fb["rating"], created_at, created_at),
                )
                inserted += 1
    conn.commit()
    log(f"  course feedback inserted: {inserted}")


# --------------------------------------------------------------------------- #
#  Step 7 — rebuild the RAG knowledge base                                    #
# --------------------------------------------------------------------------- #

def rebuild_knowledge_base():
    log_step("step 7 / rebuild RAG knowledge base via /assistant/kb/rebuild")
    # Endpoint is admin-only in production but our JwtInterceptor only blocks
    # missing/invalid tokens, so we log in as admin01 first.
    login = requests.post(
        f"{BACKEND_BASE}/login",
        json={"username": "admin01", "password": "Admin@Cen2026!Feedback", "role": "admin"},
        timeout=15,
    )
    login.raise_for_status()
    token = login.json()["data"]["token"]
    log(f"  logged in as admin01, token prefix={token[:24]}…")

    resp = requests.post(
        f"{BACKEND_BASE}/assistant/kb/rebuild",
        headers={"Authorization": token},
        timeout=180,
    )
    if resp.status_code != 200:
        log(f"  WARN: kb rebuild HTTP {resp.status_code}: {resp.text[:200]}")
        return
    log(f"  rebuild ok: {resp.json()}")


# --------------------------------------------------------------------------- #
#  Step 8 — final report                                                      #
# --------------------------------------------------------------------------- #

REPORT_QUERIES = [
    ("teachers",        "SELECT COUNT(*) FROM sys_user WHERE role='teacher'"),
    ("students",        "SELECT COUNT(*) FROM sys_user WHERE role='student'"),
    ("admins",          "SELECT COUNT(*) FROM sys_user WHERE role='admin'"),
    ("courses",         "SELECT COUNT(*) FROM sys_courses"),
    ("course_resource", "SELECT COUNT(*) FROM sys_course_resource"),
    ("sys_file",        "SELECT COUNT(*) FROM sys_file"),
    ("course_students", "SELECT COUNT(*) FROM sys_course_students"),
    ("questionnaires",  "SELECT COUNT(*) FROM sys_questionnaires"),
    ("q_responses",     "SELECT COUNT(*) FROM sys_questionnaire_responses"),
    ("course_feedback", "SELECT COUNT(*) FROM sys_course_feedback"),
    ("kb_chunk",        "SELECT COUNT(*) FROM sys_kb_chunk"),
]


def print_report(conn):
    log_step("step 8 / final summary")
    with conn.cursor() as c:
        for label, sql in REPORT_QUERIES:
            c.execute(sql)
            cnt = c.fetchone()[0]
            log(f"  {label:<18} {cnt}")

    with conn.cursor() as c:
        c.execute(
            """
            SELECT c.code, c.name, u.nickname,
                   (SELECT COUNT(*) FROM sys_course_students s WHERE s.course_id = c.id AND s.status='approved') AS students,
                   (SELECT COUNT(*) FROM sys_course_resource r WHERE r.course_id = c.id) AS resources,
                   (SELECT COUNT(*) FROM sys_questionnaire_responses qr WHERE qr.course_id = c.id) AS responses,
                   (SELECT COUNT(*) FROM sys_course_feedback f WHERE f.course_id = c.id) AS feedbacks
            FROM sys_courses c
            JOIN sys_user u ON u.id = c.teacher_id
            ORDER BY c.id
            """
        )
        log("\n  per-course breakdown:")
        log(f"    {'CODE':<10}{'TEACHER':<14}{'#STU':>6}{'#RES':>6}{'#RESP':>7}{'#FB':>6}  NAME")
        for row in c.fetchall():
            code, name, nick, stu, res, resp, fb = row
            log(f"    {code:<10}{nick[:12]:<14}{stu:>6}{res:>6}{resp:>7}{fb:>6}  {name}")


# --------------------------------------------------------------------------- #
#  Main                                                                       #
# --------------------------------------------------------------------------- #

def main():
    log(f"backend  : {BACKEND_BASE}")
    log(f"db host  : {DB_CONF['host']}:{DB_CONF['port']}/{DB_CONF['database']}")
    log(f"test src : {TEST_FILES_ROOT}")
    log(f"rng seed : {RNG_SEED}")

    # Sanity check the backend is up.
    try:
        h = requests.get(f"{BACKEND_BASE}/actuator/health/live", timeout=5)
        log(f"backend health: HTTP {h.status_code}")
    except Exception as exc:
        log(f"backend NOT reachable: {exc}")
        sys.exit(1)

    conn = pymysql.connect(**DB_CONF)
    try:
        wipe_business_data(conn)
        teacher_ids, student_ids = insert_users(conn)
        course_ids = insert_courses(conn, teacher_ids)
        upload_course_files(conn, course_ids, teacher_ids)
        q_ids = insert_questionnaires(conn, teacher_ids)
        bind_questionnaires_to_courses(conn, course_ids, q_ids)
        enrollments = assign_enrollments(course_ids, student_ids)
        insert_enrollments(conn, enrollments)
        insert_questionnaire_responses(conn, course_ids, enrollments, q_ids)
        insert_course_feedback(conn, course_ids, enrollments)
        rebuild_knowledge_base()
        print_report(conn)
        log("\nDONE. Demo data ready.")
    finally:
        conn.close()


if __name__ == "__main__":
    main()
