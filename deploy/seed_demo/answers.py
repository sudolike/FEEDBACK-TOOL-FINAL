"""
Heuristics for generating realistic-looking student questionnaire answers and
course feedback. Kept separate so the main seed script stays small and so it is
easy to tweak the "personality" of the demo data without re-reading SQL.
"""

import json
import random
from datetime import datetime, timedelta
from typing import Dict, Any, List


# Pool of free-text answers grouped roughly by "tone". The seed script picks
# according to the rating the student would have given so positives match
# positives.
TEXT_BANK_POSITIVE = [
    "老师讲得非常清晰，每节课都能学到具体可用的方法。",
    "课程节奏把握得很好，配合大量实战，对工作帮助很大。",
    "助教非常负责，提交作业后通常 24 小时内就能收到详细反馈。",
    "案例选取贴近工业界，让我对所学内容有了直观的理解。",
    "PPT 排版清楚，配合录像反复看效果非常好。",
    "课堂氛围轻松，提问后老师都会停下来认真解答。",
    "项目设计有梯度，从最小可行版本到完整产品都覆盖到了。",
    "课后会布置一些小型挑战，让人兴趣保持得很高。",
]

TEXT_BANK_NEUTRAL = [
    "总体不错，但作业截止时间稍紧，希望能再宽松一些。",
    "理论部分讲得详细，但实战环节占比可以再多一点。",
    "希望增加一些工业界嘉宾分享，让课程更接地气。",
    "课程录像清晰度可以再提升，有时投影画面看不清楚。",
    "建议在 LMS 上提前公布每周的阅读材料，方便预习。",
]

TEXT_BANK_CRITICAL = [
    "课程内容偏难，前几次课基础铺垫可以再放慢一些。",
    "作业难度偏高，建议给一些参考思路或样例代码。",
    "讨论区互动较少，希望老师或助教能更主动回应。",
    "考核标准还可以再透明一些，期末分配比重略不清楚。",
]

SUMMARY_SHORT = [
    "整体收获很大",
    "推荐想深入数据分析的同学修读",
    "项目设计让我印象深刻",
    "工作中已经在用所学的知识",
    "比预期的更深入也更累",
    "希望下届保留这门课",
]


def make_rating_answer(question: Dict[str, Any], skew_high: bool) -> int:
    """Return a 1..5 integer with bias controlled by skew_high."""
    if skew_high:
        # 4-5 dominant, 3 sometimes, rare 2
        return random.choices([2, 3, 4, 5], weights=[1, 2, 5, 5])[0]
    return random.choices([1, 2, 3, 4, 5], weights=[1, 2, 4, 6, 4])[0]


def make_single_answer(question: Dict[str, Any]) -> str:
    options = question.get("options", []) or []
    if not options:
        return ""
    return random.choice(options)


def make_multiple_answer(question: Dict[str, Any]) -> List[str]:
    options = question.get("options", []) or []
    if not options:
        return []
    n = random.randint(1, max(1, min(3, len(options))))
    return random.sample(options, n)


def make_text_answer(rating_hint: int) -> str:
    """rating_hint roughly determines tone: 4-5 positive, 3 neutral, <=2 critical."""
    if rating_hint >= 4:
        return random.choice(TEXT_BANK_POSITIVE)
    if rating_hint == 3:
        return random.choice(TEXT_BANK_NEUTRAL + TEXT_BANK_POSITIVE)
    return random.choice(TEXT_BANK_CRITICAL + TEXT_BANK_NEUTRAL)


def build_answers_json(questions: List[Dict[str, Any]], student_id: int) -> str:
    """
    Build the JSON payload that gets stored in sys_questionnaire_responses.answers.

    The schema mirrors what the Android QuestionnaireFillViewModel.encodeAnswers
    produces:  { "<questionId>": value, ... }   where value can be int / str / List[str].
    """
    # Each student has a hidden personality bias so their answers are internally
    # consistent (e.g. someone who gives a 5 also writes positive comments).
    skew_high = (student_id % 5) != 0  # 4 of 5 students lean positive
    primary_rating = 0
    out: Dict[str, Any] = {}
    for q in questions:
        qid = q["id"]
        qtype = q["type"]
        if qtype == "rating":
            r = make_rating_answer(q, skew_high)
            if primary_rating == 0:
                primary_rating = r
            out[qid] = r
        elif qtype == "single":
            out[qid] = make_single_answer(q)
        elif qtype == "multiple":
            out[qid] = make_multiple_answer(q)
        elif qtype == "text":
            # Some students leave optional text empty; required text is always filled.
            if (not q.get("required", True)) and random.random() < 0.25:
                out[qid] = ""
            else:
                out[qid] = make_text_answer(primary_rating or 4)
        else:
            out[qid] = ""
    return json.dumps(out, ensure_ascii=False)


def make_course_feedback(student_id: int) -> Dict[str, Any]:
    """Generate one row for sys_course_feedback (rating + content)."""
    skew_high = (student_id % 6) != 0
    rating = random.choices([1, 2, 3, 4, 5],
                            weights=[1, 2, 4, 7, 6] if skew_high else [2, 3, 4, 4, 2])[0]
    if rating >= 4:
        content = random.choice(TEXT_BANK_POSITIVE)
    elif rating == 3:
        content = random.choice(TEXT_BANK_NEUTRAL)
    else:
        content = random.choice(TEXT_BANK_CRITICAL)
    return {"rating": rating, "content": content}


def random_recent_datetime(days_back: int = 30) -> datetime:
    """Random datetime within last `days_back` days, useful for spreading submitted_at."""
    delta_seconds = random.randint(0, days_back * 24 * 3600)
    return datetime.now() - timedelta(seconds=delta_seconds)
