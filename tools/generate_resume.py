from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_JUSTIFY, TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib.units import mm
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.cidfonts import UnicodeCIDFont
from reportlab.platypus import (
    Image,
    KeepTogether,
    Paragraph,
    SimpleDocTemplate,
    Spacer,
    Table,
    TableStyle,
)


OUTPUT = "/Users/macbbokair/Downloads/王鹏程_Java后端开发_简历_修改版.pdf"
PHOTO = "/Users/macbbokair/Downloads/resume_img_p1_3_720x720.png"


pdfmetrics.registerFont(UnicodeCIDFont("STSong-Light"))


def style(size=10.5, leading=None, bold=False, align=TA_LEFT, color=colors.HexColor("#333333")):
    return ParagraphStyle(
        name=f"s-{size}-{leading}-{bold}-{align}",
        fontName="STSong-Light",
        fontSize=size,
        leading=leading or size + 5,
        alignment=align,
        textColor=color,
        wordWrap="CJK",
        splitLongWords=True,
        spaceAfter=0,
        spaceBefore=0,
    )


S = {
    "name": style(23, 28, True, TA_CENTER, colors.black),
    "center": style(10.5, 16, False, TA_CENTER),
    "section": style(17, 22, True, TA_LEFT, colors.black),
    "body": style(10.4, 16, False, TA_JUSTIFY),
    "body_small": style(10.0, 15, False, TA_JUSTIFY),
    "item_title": style(12.5, 18, True, TA_LEFT, colors.HexColor("#222222")),
    "muted": style(10.2, 16, False, TA_LEFT, colors.HexColor("#666666")),
    "skill": style(10.5, 18, False, TA_LEFT),
}


def p(text, key="body"):
    return Paragraph(text, S[key])


def section(title):
    line = Table([[p(title, "section")]], colWidths=[180 * mm])
    line.setStyle(
        TableStyle(
            [
                ("LINEBELOW", (0, 0), (-1, -1), 0.7, colors.HexColor("#C9C9C9")),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 7),
                ("TOPPADDING", (0, 0), (-1, -1), 8),
                ("LEFTPADDING", (0, 0), (-1, -1), 0),
                ("RIGHTPADDING", (0, 0), (-1, -1), 0),
            ]
        )
    )
    return line


def project_header(name, tech, date):
    table = Table(
        [[p(f"<b>{name}</b>", "item_title"), p(tech, "muted"), p(date, "muted")]],
        colWidths=[76 * mm, 34 * mm, 70 * mm],
    )
    table.setStyle(
        TableStyle(
            [
                ("VALIGN", (0, 0), (-1, -1), "TOP"),
                ("ALIGN", (2, 0), (2, 0), "RIGHT"),
                ("LEFTPADDING", (0, 0), (-1, -1), 0),
                ("RIGHTPADDING", (0, 0), (-1, -1), 0),
                ("TOPPADDING", (0, 0), (-1, -1), 5),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 4),
            ]
        )
    )
    return table


def bullets(items, small=False):
    return [p("- " + item, "body_small" if small else "body") for item in items]


def build():
    doc = SimpleDocTemplate(
        OUTPUT,
        pagesize=A4,
        rightMargin=24 * mm,
        leftMargin=24 * mm,
        topMargin=20 * mm,
        bottomMargin=18 * mm,
    )

    story = []

    header = Table(
        [
            [
                "",
                [p("<b>王鹏程</b>", "name"), Spacer(1, 6), p("18846077132 | 2642048508@qq.com", "center"), p("求职意向：Java 后端开发", "center")],
                Image(PHOTO, width=25 * mm, height=32 * mm),
            ]
        ],
        colWidths=[38 * mm, 104 * mm, 38 * mm],
    )
    header.setStyle(
        TableStyle(
            [
                ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
                ("ALIGN", (2, 0), (2, 0), "RIGHT"),
                ("LEFTPADDING", (0, 0), (-1, -1), 0),
                ("RIGHTPADDING", (0, 0), (-1, -1), 0),
            ]
        )
    )
    story += [header, Spacer(1, 12)]

    story += [
        section("自我评价"),
        Spacer(1, 9),
        p(
            "具备扎实的 Java 后端开发基础，熟悉 Spring Boot、Spring Security、MyBatis-Plus、MySQL、Redis 等技术栈，"
            "能够完成从需求分析、数据库设计、接口开发到联调测试的完整后端开发流程。正在开发企业级 AI Coding 协作平台，"
            "重点实践 JWT 认证、RBAC 权限、模块化单体架构、统一响应与异常处理、Flyway 数据库迁移及项目级权限隔离；"
            "同时具备大模型 API 接入、Prompt 设计和结果结构化解析经验。熟悉 Vibe Coding 开发方式，能够结合 AI 编程工具"
            "完成需求拆解、代码生成、调试迭代和文档沉淀。学习能力强，关注工程规范、系统可扩展性和后端服务稳定性。",
            "body",
        ),
        Spacer(1, 18),
        section("项目经历"),
    ]

    story += [
        project_header("智能养老健康管理平台", "Java", "2026.01-2026.04"),
        p(
            "项目简介：面向养老机构的健康管理与运营系统，提供老人档案管理、护理计划制定、合同管理及体检报告智能分析功能，"
            "通过接入大模型实现报告自动解析与健康评估，提高护理决策效率与服务质量。",
            "body_small",
        ),
        p("技术栈：SpringBoot、RuoYi、MyBatis-Plus、MySQL、Redis、阿里云OSS、Vue3、LLM", "body_small"),
        p("个人职责&技术亮点：", "body_small"),
        *bullets(
            [
                "基于 RuoYi 框架完成后台管理系统开发，负责老人信息、护理计划、合同管理等核心模块，实现多表关联查询与业务流程处理。",
                "接入大模型 API，实现体检报告文本解析、异常指标识别、风险评估及健康建议生成，并设计 Prompt 模板与结构化解析逻辑，提升模型输出稳定性。",
                "构建 Redis 缓存体系，对体检报告及分析结果进行缓存，通过报告 MD5 设计缓存 Key，避免重复计算，接口响应时间由 800ms 降低至 200ms 以内，命中率达 90%+。",
                "集成阿里云 OSS 实现报告文件上传与访问，提升文件管理能力并降低服务器存储压力。",
            ],
            small=True,
        ),
        Spacer(1, 8),
    ]

    story += [
        project_header("AI Coding 团队协作平台", "Java", "2026.05-至今"),
        p(
            "项目简介：面向研发团队的企业级 AI Coding 协作平台，围绕“项目管理、成员权限、AI Agent、任务执行、"
            "知识库与 GitHub 集成”构建研发协作闭环。当前阶段聚焦后端基础架构、认证鉴权、权限模型、接口规范和核心数据库设计。",
            "body_small",
        ),
        p(
            "技术栈：Java 17、Spring Boot 3、Spring Security、JWT、MyBatis-Plus、MySQL、Flyway、Maven、"
            "RESTful API、RBAC、模块化单体架构",
            "body_small",
        ),
        p("个人职责&技术亮点：", "body_small"),
        *bullets(
            [
                "负责项目需求分析与系统架构设计，完成用户权限、项目协作、仓库接入、AI 任务、Chat、Agent、RAG 知识库、审计等模块拆分，明确模块边界和演进路径。",
                "搭建 Spring Boot 3 后端基础工程，封装统一响应结构、全局异常处理、错误码、分页对象和 Trace ID 过滤器，提升接口规范性与问题定位效率。",
                "基于 Spring Security + JWT 实现登录认证、Token 签发与刷新、认证过滤器和登录用户上下文，为后续项目级权限控制提供基础能力。",
                "在开发过程中使用 Vibe Coding 方式辅助完成模块设计、接口实现、问题排查和文档整理，并结合人工 Review 保证代码风格、业务边界和安全逻辑可控。",
                "设计 RBAC 权限模型与核心数据表，使用 Flyway 管理用户、角色、权限、用户角色、角色权限、GitHub 账号等表结构和初始化数据，保证数据库变更可追踪。",
                "抽象用户、角色、权限、GitHub 账号等领域实体与 MyBatis-Plus Mapper，完成认证模块应用服务和登录、刷新、当前用户查询等接口开发。",
                "输出需求、系统架构、数据库设计、API 设计、模块拆分和开发规范文档，为后续前端联调、Agent 调度、GitHub 集成和知识库模块开发提供依据。",
            ],
            small=True,
        ),
        Spacer(1, 12),
        section("教育经历"),
        Spacer(1, 6),
    ]

    edu = Table(
        [[p("<b>哈尔滨理工大学</b>", "item_title"), p("本科", "body"), p("计算机科学与技术专业", "body"), p("2023-2027", "muted")]],
        colWidths=[38 * mm, 24 * mm, 78 * mm, 40 * mm],
    )
    edu.setStyle(
        TableStyle(
            [
                ("LEFTPADDING", (0, 0), (-1, -1), 0),
                ("RIGHTPADDING", (0, 0), (-1, -1), 0),
                ("ALIGN", (3, 0), (3, 0), "RIGHT"),
            ]
        )
    )
    story.append(edu)

    story += [
        Spacer(1, 24),
        section("专业技能"),
        Spacer(1, 10),
        p("Java 基础：熟悉 Java 基础语法、集合、多线程、IO、异常处理，了解 JVM 基础、类加载机制和常见性能排查思路。", "skill"),
        p("后端开发：熟悉 Spring Boot、Spring MVC、MyBatis-Plus，能够完成 RESTful API 设计、参数校验、统一响应、全局异常处理和分层开发。", "skill"),
        p("认证与权限：熟悉 Spring Security、JWT、RBAC 权限模型，具备登录认证、Token 刷新、接口鉴权和用户上下文封装实践经验。", "skill"),
        p("数据库与缓存：熟悉 MySQL 表结构设计、索引、事务、SQL 优化和 Flyway 迁移管理；熟悉 Redis 缓存、缓存穿透/击穿/雪崩、Lua 与分布式锁思路。", "skill"),
        p("中间件与工程化：了解 RabbitMQ 异步处理、削峰填谷应用场景；熟悉 Maven、Git、Linux、Nginx、Docker 基础使用。", "skill"),
        p("前端与联调：了解 Vue3、Axios、Element Plus，能够配合前端完成接口联调和基础页面问题定位。", "skill"),
        p("AI 应用能力：具备大模型 API 接入、Prompt 设计、结构化输出解析经验，熟悉 Vibe Coding 工作流，能够使用 AI 编程工具辅助完成需求拆解、代码生成、调试优化、测试补充和技术文档整理；了解 AI Agent、RAG、工具调用、项目级上下文构建等 AI Coding 平台核心能力。", "skill"),
    ]

    doc.build(story)
    print(OUTPUT)


if __name__ == "__main__":
    build()
