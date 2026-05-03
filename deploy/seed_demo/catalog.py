"""
Demo data catalog for the Feedback-tool seed script.

Defines the static "shape" of the demo: courses, teachers, students, questionnaire
templates, and the mapping between courses and their physical files on disk.
The actual SQL/HTTP work happens in seed.py — this file is purely declarative so it
is easy to tweak the demo without touching execution code.
"""

from pathlib import Path
from typing import List, Dict, Any

# Root of the test files supplied by the user (each subdir corresponds to a course code).
TEST_FILES_ROOT = Path(
    r"C:\Users\lcw68\Documents\xwechat_files\wxid_2d6657mv3ww822_de7d\msg\file\2026-05\FeedbackTool_Test_Files"
)

# ----------------------------------------------------------------------------- #
#  Teachers (existing teacher row id=2 is reused as one of the demo teachers).  #
# ----------------------------------------------------------------------------- #

TEACHERS: List[Dict[str, str]] = [
    # username / password / chinese name / email
    {"username": "t_zhang", "password": "Teacher@123", "nickname": "张文渊 教授",
     "email": "zhang.wy@hku.hk"},
    {"username": "t_li",    "password": "Teacher@123", "nickname": "李静怡 教授",
     "email": "li.jy@hku.hk"},
    {"username": "t_wang",  "password": "Teacher@123", "nickname": "王启明 教授",
     "email": "wang.qm@hku.hk"},
    {"username": "t_chen",  "password": "Teacher@123", "nickname": "陈嘉豪 教授",
     "email": "chen.jh@hku.hk"},
    # The fifth teacher reuses the existing demo `teacher` account so that the
    # tester can log in with the username they already know.
    {"username": "teacher", "password": "teacher",     "nickname": "孙慧雯 教授",
     "email": "sun.hw@hku.hk", "reuse_existing": True},
]


# ----------------------------------------------------------------------------- #
#  Students. The first one reuses the existing demo `student` account.          #
# ----------------------------------------------------------------------------- #

_STUDENT_NAMES = [
    "刘明轩", "王思琪", "李泽宇", "张诗涵", "赵子轩", "陈博文", "杨梓琳", "黄艺凡",
    "周一诺", "吴雨桐", "徐明哲", "孙若曦", "胡梓轩", "朱慧敏", "高泽楷", "林雨萱",
    "何俊杰", "罗诗韵", "梁子睿", "宋梓涵", "郑子墨", "唐雅婷", "韩浩然", "冯欣怡",
    "邓宇航", "曹梦琪", "彭嘉伟", "曾一帆", "肖悦欣", "谢致远",
]

STUDENTS: List[Dict[str, str]] = [
    {"username": "student", "password": "student",
     "nickname": _STUDENT_NAMES[0], "email": f"s001@hku.hk",
     "reuse_existing": True}
] + [
    {"username": f"s{idx:03d}",
     "password": "Student@123",
     "nickname": _STUDENT_NAMES[idx - 1],
     "email": f"s{idx:03d}@hku.hk"}
    for idx in range(2, len(_STUDENT_NAMES) + 1)
]


# ----------------------------------------------------------------------------- #
#  Courses. teacher_username will be resolved to teacher_id at runtime.         #
# ----------------------------------------------------------------------------- #

COURSES: List[Dict[str, Any]] = [
    {
        "code": "COMP7104", "name": "Foundations of Software Engineering",
        "teacher_username": "t_zhang",
        "description": "面向研究生的软件工程基础课程，覆盖需求分析、架构设计、敏捷开发、CI/CD 与软件质量保证；课程项目要求 4-5 人小组完成一个完整的应用。",
        "academic_year": "2026-2027", "semester": 1,
        "course_time": "Mon 14:30-17:20", "location": "CYC-405",
    },
    {
        "code": "COMP7404", "name": "Computational Intelligence and Machine Learning",
        "teacher_username": "t_zhang",
        "description": "深入讲解监督学习/无监督学习/强化学习的核心算法（SVM、决策树、聚类、神经网络），并辅以大量编程作业和 Kaggle 风格小项目。",
        "academic_year": "2026-2027", "semester": 1,
        "course_time": "Tue 09:30-12:20", "location": "CB-LG.07",
    },
    {
        "code": "COMP7409", "name": "Machine Learning in Trading and Finance",
        "teacher_username": "t_zhang",
        "description": "将机器学习应用于量化金融场景：因子挖掘、波动率建模、交易策略回测，使用真实美股 / A 股 / 期货数据。",
        "academic_year": "2026-2027", "semester": 2,
        "course_time": "Wed 18:30-21:20", "location": "MWT-T1",
    },
    {
        "code": "COMP7506", "name": "Smart Phone Apps Development",
        "teacher_username": "t_li",
        "description": "[Demo 重点课] 智能手机应用开发，覆盖 Android Jetpack Compose、iOS SwiftUI、跨端方案；含一个完整 capstone 项目，需提交 PPT、PDF 报告和演示视频。",
        "academic_year": "2026-2027", "semester": 1,
        "course_time": "Thu 14:30-17:20", "location": "CB-340",
    },
    {
        "code": "DASC7011", "name": "Statistical Foundations for Data Science",
        "teacher_username": "t_li",
        "description": "数据科学的统计学基础：概率分布、假设检验、回归分析、贝叶斯推断；对零基础研究生友好但作业量较大。",
        "academic_year": "2026-2027", "semester": 1,
        "course_time": "Fri 09:30-12:20", "location": "CYC-301",
    },
    {
        "code": "DASC7102", "name": "Statistical Machine Learning",
        "teacher_username": "t_wang",
        "description": "更深入的统计机器学习：核方法、稀疏建模、概率图模型、变分推断；强调数学推导和论文阅读。",
        "academic_year": "2026-2027", "semester": 2,
        "course_time": "Mon 09:30-12:20", "location": "CYC-405",
    },
    {
        "code": "STAT7008", "name": "Programming for Data Science",
        "teacher_username": "t_wang",
        "description": "Python / R 编程实战：NumPy、Pandas、Matplotlib、Scikit-learn 与 R tidyverse 全覆盖，含若干真实数据清洗作业。",
        "academic_year": "2026-2027", "semester": 1,
        "course_time": "Tue 14:30-17:20", "location": "MWT-T2",
    },
    {
        "code": "STAT8003", "name": "Probability and Statistics for Data Science",
        "teacher_username": "t_chen",
        "description": "数据科学家所必需的概率论与数理统计；从测度论入门到现代统计推断的桥梁课程。",
        "academic_year": "2026-2027", "semester": 1,
        "course_time": "Wed 14:30-17:20", "location": "CB-LG.07",
    },
    {
        "code": "STAT8017", "name": "Data Mining Techniques",
        "teacher_username": "t_chen",
        "description": "数据挖掘综合：特征工程、关联规则、聚类与异常检测、推荐系统；以小组项目作为期末考核。",
        "academic_year": "2026-2027", "semester": 2,
        "course_time": "Thu 09:30-12:20", "location": "MWT-T1",
    },
    {
        "code": "STAT8307", "name": "Statistical Learning",
        "teacher_username": "teacher",
        "description": "统计学习理论入门：偏差-方差权衡、正则化、模型选择、boosting / bagging；偏理论且强调推导。",
        "academic_year": "2026-2027", "semester": 1,
        "course_time": "Fri 14:30-17:20", "location": "CB-340",
    },
]

# Course code that should always have the full multi-format file set (PNG + PDF + PPTX + MP4)
# for the demo. The seed script will guarantee that every available format is present
# for this course.
DEMO_COURSE_CODE = "COMP7506"


# ----------------------------------------------------------------------------- #
#  Questionnaire templates. Each "questions" entry mirrors the editor format    #
#  used by the Android client: list of {id,type,title,required,options}.        #
# ----------------------------------------------------------------------------- #

import uuid


def _q(qtype: str, title: str, options: List[str] = None, required: bool = True) -> Dict[str, Any]:
    return {
        "id": str(uuid.uuid4()),
        "type": qtype,
        "title": title,
        "required": required,
        "options": options or [],
    }


QUESTIONNAIRES: List[Dict[str, Any]] = [
    {
        "title": "教学质量月度评估问卷",
        "description": "请基于近一个月的课堂体验匿名作答，所有回答仅用于教学改进。",
        "creator_username": "t_zhang",
        "questions": [
            _q("rating", "您对老师整体授课能力的满意度？（1=不满意，5=非常满意）"),
            _q("rating", "您对课程内容的难度是否合适？（1=过简单，5=过难，3=刚好）"),
            _q("single", "您每周用于本课程的课外学习时间大约是？",
               ["少于 2 小时", "2-5 小时", "5-10 小时", "10 小时以上"]),
            _q("multiple", "您认为本课程哪些方面做得较好？（可多选）",
               ["授课节奏", "案例丰富", "助教答疑", "课件质量", "课程项目设计"]),
            _q("text", "您对本课程后续教学有何具体建议？", required=False),
        ],
    },
    {
        "title": "课程内容深度反馈表",
        "description": "针对课程的内容设计、难度梯度、配套资料质量收集学生反馈。",
        "creator_username": "t_li",
        "questions": [
            _q("rating", "课程内容的实用性？"),
            _q("rating", "课件 / PPT 与课程录像的清晰度？"),
            _q("single", "课程作业难度对您是否合适？",
               ["远低于预期", "略低", "刚好", "略高", "远高于预期"]),
            _q("multiple", "您最希望未来增加哪些环节？",
               ["更多代码实战", "案例分析", "工业界嘉宾", "科研论文研读", "团队项目"]),
            _q("text", "请简要描述您觉得本课程最有收获的部分。", required=False),
            _q("text", "如果让您给老师一条改进建议，您会说什么？", required=False),
        ],
    },
    {
        "title": "期末综合评价问卷",
        "description": "课程结束前的综合反馈，包含整体满意度、对老师与助教的评价、未来选课建议。",
        "creator_username": "t_chen",
        "questions": [
            _q("rating", "您对本课程的整体满意度？"),
            _q("rating", "您是否会向同学推荐这门课程？（1=不推荐，5=强烈推荐）"),
            _q("single", "本学期投入到该课程的精力占您总精力的比例？",
               ["10% 以下", "10-20%", "20-30%", "30% 以上"]),
            _q("multiple", "您认为本课程最大的特色是？（可多选）",
               ["紧跟前沿", "讲解深入", "实践性强", "考核公平", "课件优秀", "课堂氛围好"]),
            _q("text", "请用一句话总结您对本课程的感受。", required=False),
        ],
    },
]


# ----------------------------------------------------------------------------- #
#  Mapping: each course gets bound to 1-2 questionnaire templates.              #
#  Indexes refer to QUESTIONNAIRES above.                                       #
# ----------------------------------------------------------------------------- #

COURSE_QUESTIONNAIRE_BIND: Dict[str, List[int]] = {
    "COMP7104": [0, 2],
    "COMP7404": [0, 1],
    "COMP7409": [0],
    "COMP7506": [0, 1, 2],   # demo course gets all three so the analysis screen has rich data
    "DASC7011": [1, 2],
    "DASC7102": [0, 1],
    "STAT7008": [1],
    "STAT8003": [0, 2],
    "STAT8017": [1, 2],
    "STAT8307": [0],
}
