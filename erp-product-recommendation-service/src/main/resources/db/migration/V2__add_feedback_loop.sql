-- v1.9.0 Feedback 闭环 - 真实反馈数据持久化
-- 兼容 H2 (开发) / PostgreSQL (生产)

CREATE TABLE IF NOT EXISTS recommend_feedback (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    result_id BIGINT NOT NULL,
    candidate_id BIGINT NOT NULL,
    sku VARCHAR(64),
    category VARCHAR(64),
    platform VARCHAR(32),

    -- 采纳信号: 卖家接受了哪条推荐
    -- ADOPTED  接受并上架
    -- REJECTED 拒绝
    -- IGNORED  未表态
    feedback_type VARCHAR(32) NOT NULL,

    -- 卖家主观打分 1~5
    seller_rating INT,

    -- 真实 outcome (上架后若干天回填)
    actual_30d_sales INT,
    actual_30d_profit DECIMAL(14,2),
    actual_conversion_rate DOUBLE,

    -- 计算字段（写时缓存，避免重复聚合）
    -- accuracy_score = -1 ~ 1
    --   >0 表示推荐方向正确（采纳后表现好）
    --   <0 表示推荐方向错误
    --   =0 表示中性（未采纳/无 outcome）
    accuracy_score DOUBLE DEFAULT 0.0,

    feedback_note VARCHAR(512),
    feedback_by VARCHAR(64),

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_recommend_feedback_result ON recommend_feedback(result_id);
CREATE INDEX IF NOT EXISTS idx_recommend_feedback_candidate ON recommend_feedback(candidate_id);
CREATE INDEX IF NOT EXISTS idx_recommend_feedback_type ON recommend_feedback(feedback_type);
CREATE INDEX IF NOT EXISTS idx_recommend_feedback_category ON recommend_feedback(category, platform);
CREATE INDEX IF NOT EXISTS idx_recommend_feedback_created ON recommend_feedback(created_at DESC);

-- 权重自适应快照表 - 记录每次权重调优后的状态
CREATE TABLE IF NOT EXISTS recommend_weight_snapshot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    w_demand DOUBLE NOT NULL,
    w_trend DOUBLE NOT NULL,
    w_profit DOUBLE NOT NULL,
    w_competition DOUBLE NOT NULL,
    w_quality DOUBLE NOT NULL,
    sample_count INT NOT NULL,
    avg_accuracy DOUBLE,
    adopt_rate DOUBLE,
    trigger_reason VARCHAR(256),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_weight_snapshot_created ON recommend_weight_snapshot(created_at DESC);

-- 示例种子反馈数据 (开发测试使用)
INSERT INTO recommend_feedback
  (result_id, candidate_id, sku, category, platform, feedback_type, seller_rating,
   actual_30d_sales, actual_30d_profit, actual_conversion_rate, accuracy_score,
   feedback_note, feedback_by, created_at, updated_at)
VALUES
  -- 5 条历史反馈模拟"过去 30 天数据"
  (1, 1, 'AMZ-USB-001', 'Electronics', 'amazon', 'ADOPTED', 5,
   3500, 9800.00, 0.18, 0.85,
   'USB-C 充电器大卖，趋势判断准确', 'seller-001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (2, 2, 'AMZ-LED-002', 'Home', 'amazon', 'ADOPTED', 4,
   1900, 3200.00, 0.12, 0.65,
   'LED 灯带还行，季节性对得上', 'seller-001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (3, 3, 'AMZ-PET-003', 'Pet', 'amazon', 'REJECTED', 2,
   0, 0.00, 0.00, -0.40,
   '宠物喂食器竞争太激烈，没敢上', 'seller-002', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (4, 4, 'AMZ-FIT-004', 'Sports', 'amazon', 'IGNORED', 1,
   0, 0.00, 0.00, 0.00,
   '瑜伽垫评分不准，毛利率虚高', 'seller-002', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (5, 5, 'AMZ-CAM-005', 'Baby', 'amazon', 'ADOPTED', 5,
   2400, 18500.00, 0.22, 0.95,
   '宝宝监视器爆款，强烈推荐', 'seller-001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 初始权重快照 (基线)
INSERT INTO recommend_weight_snapshot
  (w_demand, w_trend, w_profit, w_competition, w_quality,
   sample_count, avg_accuracy, adopt_rate, trigger_reason, created_at)
VALUES
  (0.30, 0.20, 0.20, 0.15, 0.15, 0, 0.0, 0.0,
   'INITIAL_BASELINE', CURRENT_TIMESTAMP);