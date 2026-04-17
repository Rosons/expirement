-- =============================================================================
-- 智能客服 · 课程预约域：课程、校区、校区-课程关联、预约单
-- 新库一次性在本脚本建齐最终结构（不另拆后续迁移改列）。
--
-- 可重复执行（幂等）：
--   - DDL：CREATE TABLE / CREATE INDEX 均使用 IF NOT EXISTS
--   - 种子数据：INSERT … SELECT … WHERE NOT EXISTS（按 course_code、campus_code、
--     以及 campus_course 的 campus_id+course_id 且 deleted=0 判重）；预约单无种子数据
--   - COMMENT ON … 可多次执行，效果为覆盖为同一说明
--
-- 约束策略：
--   - 不创建物理外键（FOREIGN KEY），表间关联由后端维护
--   - 仅保留主键、业务唯一索引（partial unique + deleted=0）与查询索引，便于与逻辑删除共存
--
-- 字段约定：deleted 逻辑删除 0/1；时间戳 DEFAULT NOW()
--
-- 学历等级（0～9 一位数，课程与学员须用同一套刻度；数值越大代表报名门槛越高）：
--   0 不限；1 高中及以上；2 大专及以上；3 本科及以上；4 硕士及以上；5～9 预留
-- 课程表 min_education_level 表示「报名最低学历等级」。学员 user_education_level 与课程同标尺时，
-- 可报名条件示例：WHERE min_education_level <= :userEducationLevel
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 课程表
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS edu_course (
    id                   BIGSERIAL PRIMARY KEY,
    course_code          VARCHAR(64)  NOT NULL,
    name                 VARCHAR(256) NOT NULL,
    description          TEXT,
    min_education_level  INTEGER      NOT NULL DEFAULT 0,
    price_cent           BIGINT       NOT NULL DEFAULT 0,
    duration_hours       INTEGER,
    category             VARCHAR(64),
    interest_tags        JSONB        NOT NULL DEFAULT '[]'::JSONB,
    status               SMALLINT     NOT NULL DEFAULT 1,
    created_at           TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP    NOT NULL DEFAULT NOW(),
    deleted              SMALLINT     NOT NULL DEFAULT 0
);

COMMENT ON TABLE edu_course IS '培训课程：学历门槛、价格、分类等';
COMMENT ON COLUMN edu_course.id IS '主键，自增';
COMMENT ON COLUMN edu_course.course_code IS '业务唯一编码，用于导入与关联';
COMMENT ON COLUMN edu_course.name IS '课程名称';
COMMENT ON COLUMN edu_course.description IS '课程描述（面向学员的介绍文案，可含大纲要点）';
COMMENT ON COLUMN edu_course.min_education_level IS '报名最低学历等级：0～9 一位数，与学员学历同标尺（0 不限，1 高中，2 大专，3 本科，4 硕士，5～9 预留）。';
COMMENT ON COLUMN edu_course.price_cent IS '单价，单位分，避免浮点误差';
COMMENT ON COLUMN edu_course.duration_hours IS '建议总课时（小时），可空';
COMMENT ON COLUMN edu_course.category IS '业务分类编码，如 AI_DATA、BACKEND、FRONTEND';
COMMENT ON COLUMN edu_course.interest_tags IS '兴趣/方向标签 JSON 数组';
COMMENT ON COLUMN edu_course.status IS '1 上架，0 下架';
COMMENT ON COLUMN edu_course.created_at IS '创建时间';
COMMENT ON COLUMN edu_course.updated_at IS '最后更新时间';
COMMENT ON COLUMN edu_course.deleted IS '逻辑删除：0 未删除，1 已删除';

CREATE UNIQUE INDEX IF NOT EXISTS uq_edu_course_code_active
    ON edu_course (course_code)
    WHERE deleted = 0;

CREATE INDEX IF NOT EXISTS idx_edu_course_category_status
    ON edu_course (category, status)
    WHERE deleted = 0;

CREATE INDEX IF NOT EXISTS idx_edu_course_min_education_level
    ON edu_course (min_education_level)
    WHERE deleted = 0;

-- ---------------------------------------------------------------------------
-- 校区表
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS edu_campus (
    id              BIGSERIAL PRIMARY KEY,
    campus_code     VARCHAR(64)  NOT NULL,
    name            VARCHAR(256) NOT NULL,
    province        VARCHAR(64),
    city            VARCHAR(64),
    district        VARCHAR(128),
    address_detail  TEXT,
    contact_phone   VARCHAR(32),
    status          SMALLINT     NOT NULL DEFAULT 1,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    deleted         SMALLINT     NOT NULL DEFAULT 0
);

COMMENT ON TABLE edu_campus IS '线下授课校区：地理与联系信息';
COMMENT ON COLUMN edu_campus.id IS '主键，自增';
COMMENT ON COLUMN edu_campus.campus_code IS '校区业务唯一编码';
COMMENT ON COLUMN edu_campus.name IS '校区展示名称（一般为地标/片区 + 学习中心）';
COMMENT ON COLUMN edu_campus.province IS '省级行政区名称';
COMMENT ON COLUMN edu_campus.city IS '地级行政区名称（地级市/直辖市）';
COMMENT ON COLUMN edu_campus.district IS '区县级或开发区名称';
COMMENT ON COLUMN edu_campus.address_detail IS '详细门牌、楼宇与楼层';
COMMENT ON COLUMN edu_campus.contact_phone IS '校区联系电话';
COMMENT ON COLUMN edu_campus.status IS '1 营业，0 停用';
COMMENT ON COLUMN edu_campus.created_at IS '创建时间';
COMMENT ON COLUMN edu_campus.updated_at IS '最后更新时间';
COMMENT ON COLUMN edu_campus.deleted IS '逻辑删除：0 未删除，1 已删除';

CREATE UNIQUE INDEX IF NOT EXISTS uq_edu_campus_code_active
    ON edu_campus (campus_code)
    WHERE deleted = 0;

CREATE INDEX IF NOT EXISTS idx_edu_campus_city
    ON edu_campus (city)
    WHERE deleted = 0;

-- ---------------------------------------------------------------------------
-- 校区与课程关联（非全量笛卡尔积：仅维护实际开设组合）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS edu_campus_course (
    id              BIGSERIAL PRIMARY KEY,
    campus_id       BIGINT       NOT NULL,
    course_id       BIGINT       NOT NULL,
    quota_per_month INTEGER,
    remark          VARCHAR(512),
    status          SMALLINT     NOT NULL DEFAULT 1,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    deleted         SMALLINT     NOT NULL DEFAULT 0
);

COMMENT ON TABLE edu_campus_course IS '校区开设课程；与 edu_campus、edu_course 逻辑关联，无物理外键';
COMMENT ON COLUMN edu_campus_course.id IS '主键，自增';
COMMENT ON COLUMN edu_campus_course.campus_id IS '校区主键，对应 edu_campus.id';
COMMENT ON COLUMN edu_campus_course.course_id IS '课程主键，对应 edu_course.id';
COMMENT ON COLUMN edu_campus_course.quota_per_month IS '可选：每月计划开班名额上限';
COMMENT ON COLUMN edu_campus_course.remark IS '可选：本校该课开班说明、排课策略等（与课程表 description 不同，侧重校区运营）';
COMMENT ON COLUMN edu_campus_course.status IS '1 有效，0 暂停开设';
COMMENT ON COLUMN edu_campus_course.created_at IS '创建时间';
COMMENT ON COLUMN edu_campus_course.updated_at IS '最后更新时间';
COMMENT ON COLUMN edu_campus_course.deleted IS '逻辑删除：0 未删除，1 已删除';

CREATE UNIQUE INDEX IF NOT EXISTS uq_edu_campus_course_active
    ON edu_campus_course (campus_id, course_id)
    WHERE deleted = 0;

CREATE INDEX IF NOT EXISTS idx_edu_campus_course_course
    ON edu_campus_course (course_id)
    WHERE deleted = 0;

-- ---------------------------------------------------------------------------
-- 预约单
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS edu_course_booking (
    id                 BIGSERIAL PRIMARY KEY,
    booking_no         VARCHAR(64)  NOT NULL,
    customer_name      VARCHAR(64)  NOT NULL,
    customer_phone     VARCHAR(32)  NOT NULL,
    course_id          BIGINT       NOT NULL,
    campus_id          BIGINT       NOT NULL,
    status             VARCHAR(32)  NOT NULL DEFAULT 'pending',
    remark             TEXT,
    created_at         TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP    NOT NULL DEFAULT NOW(),
    deleted            SMALLINT     NOT NULL DEFAULT 0
);

COMMENT ON TABLE edu_course_booking IS '用户课程预约单；与课程、校区逻辑关联，无物理外键';
COMMENT ON COLUMN edu_course_booking.id IS '主键，自增';
COMMENT ON COLUMN edu_course_booking.booking_no IS '对外展示预约单号，业务唯一（未删除记录内）';
COMMENT ON COLUMN edu_course_booking.customer_name IS '预约人姓名';
COMMENT ON COLUMN edu_course_booking.customer_phone IS '预约人手机号';
COMMENT ON COLUMN edu_course_booking.course_id IS '预约课程主键，对应 edu_course.id';
COMMENT ON COLUMN edu_course_booking.campus_id IS '到访校区主键，对应 edu_campus.id';
COMMENT ON COLUMN edu_course_booking.status IS 'pending 待确认；confirmed 已确认；cancelled 已取消；completed 已完成';
COMMENT ON COLUMN edu_course_booking.remark IS '备注（意向到访时间、其它约定等均可写在此）';
COMMENT ON COLUMN edu_course_booking.created_at IS '创建时间';
COMMENT ON COLUMN edu_course_booking.updated_at IS '最后更新时间';
COMMENT ON COLUMN edu_course_booking.deleted IS '逻辑删除：0 未删除，1 已删除';

CREATE INDEX IF NOT EXISTS idx_edu_booking_phone_created
    ON edu_course_booking (customer_phone, created_at DESC)
    WHERE deleted = 0;

CREATE INDEX IF NOT EXISTS idx_edu_booking_course_campus
    ON edu_course_booking (course_id, campus_id)
    WHERE deleted = 0;

-- =============================================================================
-- 种子数据：课程、校区、校区-课程关联（幂等判重）；不含预约单
-- =============================================================================

INSERT INTO edu_course (course_code, name, description, min_education_level, price_cent, duration_hours, category, interest_tags, status)
SELECT 'CRS-AI-101', 'Python 与数据分析入门',
       '面向零基础与转行人群，Python 语法、数据处理与可视化，为后续 AI 学习打基础。',
       1, 298000, 40, 'AI_DATA', '["Python","数据分析","入门"]'::JSONB, 1
WHERE NOT EXISTS (SELECT 1 FROM edu_course WHERE course_code = 'CRS-AI-101' AND deleted = 0);

INSERT INTO edu_course (course_code, name, description, min_education_level, price_cent, duration_hours, category, interest_tags, status)
SELECT 'CRS-JAVA-201', 'Java 企业级后端开发',
       'Spring Boot、MyBatis、REST、常见中间件与工程实践，适合希望从事后端开发的学习者。',
       2, 458000, 64, 'BACKEND', '["Java","Spring","后端"]'::JSONB, 1
WHERE NOT EXISTS (SELECT 1 FROM edu_course WHERE course_code = 'CRS-JAVA-201' AND deleted = 0);

INSERT INTO edu_course (course_code, name, description, min_education_level, price_cent, duration_hours, category, interest_tags, status)
SELECT 'CRS-LLM-301', '大模型应用与 Prompt 工程',
       '主流大模型调用、RAG 入门、Prompt 设计与安全，适合已有编程基础的学员。',
       3, 598000, 48, 'AI_LLM', '["大模型","RAG","Prompt"]'::JSONB, 1
WHERE NOT EXISTS (SELECT 1 FROM edu_course WHERE course_code = 'CRS-LLM-301' AND deleted = 0);

INSERT INTO edu_course (course_code, name, description, min_education_level, price_cent, duration_hours, category, interest_tags, status)
SELECT 'CRS-FE-401', '前端 Vue3 + TypeScript 全栈基础',
       'Vue3、TypeScript、工程化与接口联调，可与后端课程组合学习。',
       1, 368000, 56, 'FRONTEND', '["Vue","TypeScript","前端"]'::JSONB, 1
WHERE NOT EXISTS (SELECT 1 FROM edu_course WHERE course_code = 'CRS-FE-401' AND deleted = 0);

INSERT INTO edu_course (course_code, name, description, min_education_level, price_cent, duration_hours, category, interest_tags, status)
SELECT 'CRS-GO-501', 'Go 语言与云原生微服务',
       'Go 语法、gin、gRPC、容器与 K8s 入门，面向云原生与微服务方向。',
       2, 528000, 60, 'CLOUD', '["Go","微服务","云原生"]'::JSONB, 1
WHERE NOT EXISTS (SELECT 1 FROM edu_course WHERE course_code = 'CRS-GO-501' AND deleted = 0);

INSERT INTO edu_course (course_code, name, description, min_education_level, price_cent, duration_hours, category, interest_tags, status)
SELECT 'CRS-QA-601', '软件测试与质量保障',
       '功能测试、接口自动化、持续集成与质量度量，学历要求宽松。',
       0, 228000, 36, 'QUALITY', '["测试","自动化","质量"]'::JSONB, 1
WHERE NOT EXISTS (SELECT 1 FROM edu_course WHERE course_code = 'CRS-QA-601' AND deleted = 0);

INSERT INTO edu_course (course_code, name, description, min_education_level, price_cent, duration_hours, category, interest_tags, status)
SELECT 'CRS-ML-701', '机器学习基础与实践',
       '经典监督学习、特征工程与模型评估，结合 scikit-learn 与基础深度学习概念，衔接大模型与数据岗位。',
       2, 458000, 48, 'AI_ML', '["机器学习","scikit-learn","建模"]'::JSONB, 1
WHERE NOT EXISTS (SELECT 1 FROM edu_course WHERE course_code = 'CRS-ML-701' AND deleted = 0);

INSERT INTO edu_course (course_code, name, description, min_education_level, price_cent, duration_hours, category, interest_tags, status)
SELECT 'CRS-OPS-701', 'DevOps 与持续交付实践',
       'Git 分支策略、CI/CD（Jenkins/GitLab）、镜像构建与发布流水线，面向研发与运维协同。',
       1, 328000, 40, 'DEVOPS', '["DevOps","CI/CD","Jenkins"]'::JSONB, 1
WHERE NOT EXISTS (SELECT 1 FROM edu_course WHERE course_code = 'CRS-OPS-701' AND deleted = 0);

-- 校区：city 为地级市；name 为具体地标/商圈 + 学习中心（不含重复城市前缀）
INSERT INTO edu_campus (campus_code, name, province, city, district, address_detail, contact_phone, status)
SELECT 'BJ-ZGC', '中关村大街学习中心', '北京市', '北京市', '海淀区', '中关村大街 27 号中关村大厦 10 层', '010-8888-0101', 1
WHERE NOT EXISTS (SELECT 1 FROM edu_campus WHERE campus_code = 'BJ-ZGC' AND deleted = 0);

INSERT INTO edu_campus (campus_code, name, province, city, district, address_detail, contact_phone, status)
SELECT 'SH-ZJ', '张江高科学习中心', '上海市', '上海市', '浦东新区', '科苑路 88 号德国中心 2 号楼 6 层', '021-8888-0202', 1
WHERE NOT EXISTS (SELECT 1 FROM edu_campus WHERE campus_code = 'SH-ZJ' AND deleted = 0);

INSERT INTO edu_campus (campus_code, name, province, city, district, address_detail, contact_phone, status)
SELECT 'GZ-ZJC', '珠江新城学习中心', '广东省', '广州市', '天河区', '花城大道 85 号高德置地春广场 4 层', '020-8888-0303', 1
WHERE NOT EXISTS (SELECT 1 FROM edu_campus WHERE campus_code = 'GZ-ZJC' AND deleted = 0);

INSERT INTO edu_campus (campus_code, name, province, city, district, address_detail, contact_phone, status)
SELECT 'SZ-NSKJ', '科技园学习中心', '广东省', '深圳市', '南山区', '科苑路 15 号科兴科学园 B 栋 3 单元', '0755-8888-0404', 1
WHERE NOT EXISTS (SELECT 1 FROM edu_campus WHERE campus_code = 'SZ-NSKJ' AND deleted = 0);

INSERT INTO edu_campus (campus_code, name, province, city, district, address_detail, contact_phone, status)
SELECT 'HZ-XH', '文三路学习中心', '浙江省', '杭州市', '西湖区', '文三路 259 号昌地火炬大厦 1 幢', '0571-8888-0505', 1
WHERE NOT EXISTS (SELECT 1 FROM edu_campus WHERE campus_code = 'HZ-XH' AND deleted = 0);

INSERT INTO edu_campus (campus_code, name, province, city, district, address_detail, contact_phone, status)
SELECT 'CD-TF', '天府大道学习中心', '四川省', '成都市', '高新区', '天府大道北段 966 号天府国际金融中心 11 号楼', '028-8888-0606', 1
WHERE NOT EXISTS (SELECT 1 FROM edu_campus WHERE campus_code = 'CD-TF' AND deleted = 0);

INSERT INTO edu_campus (campus_code, name, province, city, district, address_detail, contact_phone, status)
SELECT 'NJ-XJK', '新街口学习中心', '江苏省', '南京市', '鼓楼区', '中山北路 2 号紫峰大厦副楼 5 层', '025-8888-0707', 1
WHERE NOT EXISTS (SELECT 1 FROM edu_campus WHERE campus_code = 'NJ-XJK' AND deleted = 0);

INSERT INTO edu_campus (campus_code, name, province, city, district, address_detail, contact_phone, status)
SELECT 'WH-GG', '光谷广场学习中心', '湖北省', '武汉市', '东湖新技术开发区', '珞喻路 766 号世界城广场写字楼 T1 8 层', '027-8888-0808', 1
WHERE NOT EXISTS (SELECT 1 FROM edu_campus WHERE campus_code = 'WH-GG' AND deleted = 0);

INSERT INTO edu_campus (campus_code, name, province, city, district, address_detail, contact_phone, status)
SELECT 'XA-GX', '高新路学习中心', '陕西省', '西安市', '雁塔区', '高新路 52 号高科大厦 9 层', '029-8888-0909', 1
WHERE NOT EXISTS (SELECT 1 FROM edu_campus WHERE campus_code = 'XA-GX' AND deleted = 0);

INSERT INTO edu_campus (campus_code, name, province, city, district, address_detail, contact_phone, status)
SELECT 'SU-JBH', '金鸡湖学习中心', '江苏省', '苏州市', '工业园区', '苏州大道东 278 号领汇广场 2 幢 7 层', '0512-8888-1010', 1
WHERE NOT EXISTS (SELECT 1 FROM edu_campus WHERE campus_code = 'SU-JBH' AND deleted = 0);

-- 校区 × 课程：非全量开设；一线城市组合各异
INSERT INTO edu_campus_course (campus_id, course_id, quota_per_month, status)
SELECT c.id, co.id, 36, 1
FROM edu_campus c, edu_course co
WHERE c.campus_code = 'BJ-ZGC' AND co.course_code IN ('CRS-AI-101', 'CRS-LLM-301', 'CRS-JAVA-201', 'CRS-FE-401', 'CRS-ML-701', 'CRS-OPS-701')
  AND c.deleted = 0 AND co.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM edu_campus_course ecc
      WHERE ecc.campus_id = c.id AND ecc.course_id = co.id AND ecc.deleted = 0);

INSERT INTO edu_campus_course (campus_id, course_id, quota_per_month, status)
SELECT c.id, co.id, 40, 1
FROM edu_campus c, edu_course co
WHERE c.campus_code = 'SH-ZJ' AND co.course_code IN ('CRS-JAVA-201', 'CRS-GO-501', 'CRS-FE-401', 'CRS-QA-601', 'CRS-ML-701', 'CRS-OPS-701')
  AND c.deleted = 0 AND co.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM edu_campus_course ecc
      WHERE ecc.campus_id = c.id AND ecc.course_id = co.id AND ecc.deleted = 0);

INSERT INTO edu_campus_course (campus_id, course_id, quota_per_month, status)
SELECT c.id, co.id, 32, 1
FROM edu_campus c, edu_course co
WHERE c.campus_code = 'GZ-ZJC' AND co.course_code IN ('CRS-AI-101', 'CRS-JAVA-201', 'CRS-FE-401', 'CRS-QA-601', 'CRS-ML-701', 'CRS-OPS-701')
  AND c.deleted = 0 AND co.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM edu_campus_course ecc
      WHERE ecc.campus_id = c.id AND ecc.course_id = co.id AND ecc.deleted = 0);

INSERT INTO edu_campus_course (campus_id, course_id, quota_per_month, status)
SELECT c.id, co.id, 38, 1
FROM edu_campus c, edu_course co
WHERE c.campus_code = 'SZ-NSKJ' AND co.course_code IN ('CRS-LLM-301', 'CRS-GO-501', 'CRS-JAVA-201', 'CRS-FE-401', 'CRS-ML-701', 'CRS-OPS-701')
  AND c.deleted = 0 AND co.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM edu_campus_course ecc
      WHERE ecc.campus_id = c.id AND ecc.course_id = co.id AND ecc.deleted = 0);

INSERT INTO edu_campus_course (campus_id, course_id, quota_per_month, status)
SELECT c.id, co.id, 28, 1
FROM edu_campus c, edu_course co
WHERE c.campus_code = 'HZ-XH' AND co.course_code IN ('CRS-AI-101', 'CRS-LLM-301', 'CRS-FE-401', 'CRS-QA-601', 'CRS-ML-701')
  AND c.deleted = 0 AND co.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM edu_campus_course ecc
      WHERE ecc.campus_id = c.id AND ecc.course_id = co.id AND ecc.deleted = 0);

INSERT INTO edu_campus_course (campus_id, course_id, quota_per_month, status)
SELECT c.id, co.id, 30, 1
FROM edu_campus c, edu_course co
WHERE c.campus_code = 'CD-TF' AND co.course_code IN ('CRS-JAVA-201', 'CRS-LLM-301', 'CRS-GO-501', 'CRS-ML-701')
  AND c.deleted = 0 AND co.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM edu_campus_course ecc
      WHERE ecc.campus_id = c.id AND ecc.course_id = co.id AND ecc.deleted = 0);

INSERT INTO edu_campus_course (campus_id, course_id, quota_per_month, status)
SELECT c.id, co.id, 22, 1
FROM edu_campus c, edu_course co
WHERE c.campus_code = 'NJ-XJK' AND co.course_code IN ('CRS-JAVA-201', 'CRS-FE-401', 'CRS-QA-601', 'CRS-OPS-701')
  AND c.deleted = 0 AND co.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM edu_campus_course ecc
      WHERE ecc.campus_id = c.id AND ecc.course_id = co.id AND ecc.deleted = 0);

INSERT INTO edu_campus_course (campus_id, course_id, quota_per_month, status)
SELECT c.id, co.id, 26, 1
FROM edu_campus c, edu_course co
WHERE c.campus_code = 'WH-GG' AND co.course_code IN ('CRS-AI-101', 'CRS-JAVA-201', 'CRS-GO-501', 'CRS-ML-701')
  AND c.deleted = 0 AND co.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM edu_campus_course ecc
      WHERE ecc.campus_id = c.id AND ecc.course_id = co.id AND ecc.deleted = 0);

INSERT INTO edu_campus_course (campus_id, course_id, quota_per_month, status)
SELECT c.id, co.id, 24, 1
FROM edu_campus c, edu_course co
WHERE c.campus_code = 'XA-GX' AND co.course_code IN ('CRS-AI-101', 'CRS-FE-401', 'CRS-QA-601', 'CRS-OPS-701')
  AND c.deleted = 0 AND co.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM edu_campus_course ecc
      WHERE ecc.campus_id = c.id AND ecc.course_id = co.id AND ecc.deleted = 0);

INSERT INTO edu_campus_course (campus_id, course_id, quota_per_month, status)
SELECT c.id, co.id, 34, 1
FROM edu_campus c, edu_course co
WHERE c.campus_code = 'SU-JBH' AND co.course_code IN ('CRS-JAVA-201', 'CRS-FE-401', 'CRS-GO-501', 'CRS-QA-601', 'CRS-OPS-701')
  AND c.deleted = 0 AND co.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM edu_campus_course ecc
      WHERE ecc.campus_id = c.id AND ecc.course_id = co.id AND ecc.deleted = 0);
