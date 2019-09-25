CREATE INDEX idx_usergroup_user ON usergroup(user_id);
CREATE INDEX idx_usergroup_group ON usergroup(group_id);
CREATE INDEX idx_usergroup_both ON usergroup(user_id, group_id);

CREATE INDEX idx_userpermission_user ON userpermission(user_id);
CREATE INDEX idx_userpermission_policy ON userpermission(policy_id);
CREATE INDEX idx_userpermission_both ON userpermission(user_id, policy_id);

CREATE INDEX idx_grouppermission_group ON grouppermission(group_id);
CREATE INDEX idx_grouppermission_policy ON grouppermission(policy_id);
CREATE INDEX idx_grouppermission_both ON grouppermission(group_id, policy_id);

CREATE INDEX idx_token_owner ON token(owner);
CREATE INDEX idx_tokenscope ON tokenscope(token_id, policy_id, access_level);
CREATE INDEX idx_tokenscope_policy ON tokenscope(policy_id);
