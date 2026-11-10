CREATE TABLE users (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE teams (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    created_by BIGINT NOT NULL REFERENCES users(id),
    code_hash VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE team_users (
    team_id BIGINT NOT NULL REFERENCES teams(id),
    user_id BIGINT NOT NULL REFERENCES users(id),
    role VARCHAR(20) NOT NULL CHECK (role IN ('OWNER', 'MEMBER')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    
    PRIMARY KEY(team_id, user_id)
);

CREATE TABLE projects (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    team_id BIGINT NOT NULL REFERENCES teams(id),
    created_by BIGINT NOT NULL REFERENCES users(id),
    name VARCHAR(128) NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    
    UNIQUE(id, team_id)
);

CREATE TABLE project_users (
    user_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    team_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    
    PRIMARY KEY(user_id, project_id),
    FOREIGN KEY(team_id, user_id) REFERENCES team_users(team_id, user_id),
    FOREIGN KEY(project_id, team_id) REFERENCES projects(id, team_id)
);

CREATE TABLE tasks (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES projects(id),
    assigned_to BIGINT REFERENCES users(id),
    created_by BIGINT NOT NULL REFERENCES users(id),
    title VARCHAR(128) NOT NULL,
    description VARCHAR(255),
    status VARCHAR(20) NOT NULL CHECK (status IN ('TODO', 'IN_PROGRESS', 'DONE')),
    due_date DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    
    FOREIGN KEY(assigned_to, project_id) REFERENCES project_users(user_id, project_id)
);

-- チーム一覧取得
CREATE INDEX idx_team_users_user_id
    ON team_users(user_id);

-- チーム内プロジェクト一覧
CREATE INDEX idx_projects_team_id
    ON projects(team_id);

-- プロジェクトメンバー一覧
CREATE INDEX idx_project_users_project_id
    ON project_users(project_id);

-- チームメンバー削除時の検索
CREATE INDEX idx_project_users_team_id_user_id
    ON project_users(team_id, user_id);

-- プロジェクト内タスク一覧
CREATE INDEX idx_tasks_project_id
    ON tasks(project_id);

-- 担当タスク検索
CREATE INDEX idx_tasks_assigned_to_project_id
    ON tasks(assigned_to, project_id);