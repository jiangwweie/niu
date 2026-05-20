-- M20A: add wechat binding timestamp
-- wechat_openid UNIQUE index already exists in V1 (uk_user_wechat_openid)
ALTER TABLE sys_user ADD COLUMN wechat_bound_at DATETIME NULL;
