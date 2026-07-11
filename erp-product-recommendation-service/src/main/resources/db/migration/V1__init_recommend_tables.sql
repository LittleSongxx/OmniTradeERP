-- 选品推荐服务 v1.9.0 schema (包含 Feedback 闭环表)
-- 兼容 H2 (开发) / PostgreSQL (生产)

CREATE TABLE IF NOT EXISTS recommend_candidate (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sku VARCHAR(64) NOT NULL,
    title VARCHAR(256),
    category VARCHAR(64),
    platform VARCHAR(32),
    cost_price DECIMAL(12,2),
    suggest_price DECIMAL(12,2),
    bsr_rank INT,
    monthly_searches BIGINT,
    last_30d_sales BIGINT,
    review_count INT,
    avg_rating DOUBLE,
    category_avg_price DECIMAL(12,2),
    gross_margin DOUBLE,
    trend_score DOUBLE,
    seasonality DOUBLE,
    competition_intensity DOUBLE,
    remark VARCHAR(512),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_recommend_candidate_platform ON recommend_candidate(platform);
CREATE INDEX IF NOT EXISTS idx_recommend_candidate_category ON recommend_candidate(category);
CREATE INDEX IF NOT EXISTS idx_recommend_candidate_sku ON recommend_candidate(sku);

CREATE TABLE IF NOT EXISTS recommend_result (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    candidate_id BIGINT NOT NULL,
    sku VARCHAR(64),
    score DOUBLE,
    recommend_level VARCHAR(32),
    rank_position INT,
    expected_monthly_profit DOUBLE,
    expected_monthly_sales INT,
    risk_score DOUBLE,
    triggered_rules VARCHAR(512),
    key_strengths VARCHAR(1024),
    key_risks VARCHAR(1024),
    adoption_status VARCHAR(32) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_recommend_result_score ON recommend_result(score DESC);
CREATE INDEX IF NOT EXISTS idx_recommend_result_level ON recommend_result(recommend_level);
CREATE INDEX IF NOT EXISTS idx_recommend_result_candidate ON recommend_result(candidate_id);

-- 示例种子数据 (开发环境使用)
INSERT INTO recommend_candidate
  (sku, title, category, platform, cost_price, suggest_price, bsr_rank, monthly_searches,
   last_30d_sales, review_count, avg_rating, category_avg_price, gross_margin,
   trend_score, seasonality, competition_intensity, remark, created_at, updated_at)
VALUES
  ('AMZ-USB-001', 'USB-C 100W 快速充电器', 'Electronics', 'amazon', 6.5, 24.9, 250,
   85000, 3200, 4500, 4.5, 22.0, 0.55, 0.65, 0.7, 0.45,
   '蓝海机会，趋势强劲', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('AMZ-LED-002', 'LED 灯带 RGB 5米', 'Home', 'amazon', 3.2, 15.9, 1200,
   42000, 1800, 1200, 4.2, 16.0, 0.40, 0.45, 0.6, 0.55,
   '中等竞争，季节性强', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('AMZ-PET-003', '宠物自动喂食器', 'Pet', 'amazon', 18.0, 49.9, 850,
   22000, 900, 320, 4.0, 52.0, 0.30, 0.30, 0.4, 0.65,
   '竞争激烈', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('AMZ-FIT-004', '瑜伽垫加厚防滑', 'Sports', 'amazon', 5.0, 19.9, 3200,
   15000, 600, 200, 3.8, 22.0, 0.20, -0.10, 0.3, 0.85,
   '红海低质', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('AMZ-CAM-005', '宝宝监视器 1080P', 'Baby', 'amazon', 22.0, 65.0, 480,
   56000, 2200, 2800, 4.6, 68.0, 0.45, 0.75, 0.8, 0.40,
   '黄金推荐品', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);