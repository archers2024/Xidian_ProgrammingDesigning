# Git 备份与恢复

本文件配合 [项目管理基础标准](项目管理基础标准.md) 和根目录 [AGENTS.md](../../AGENTS.md) 使用。用户已授权本项目自动建立 Git 检查点，并同步到指定远程仓库。

远程仓库：[archers2024/Xidian_ProgrammingDesigning](https://github.com/archers2024/Xidian_ProgrammingDesigning)。

## 1 自动备份方式

- 项目正式历史使用 `main`；日常开发分支由实际开发流程决定。
- AI 自动检查点统一使用 `backup/ai-checkpoints`，不把未完成内容直接提交到当前开发分支。
- AI 开始修改前保存 `before`，完成修改和验证后保存 `after`。普通问答和只读操作不备份。
- 脚本需要 PowerShell 7 与 Git，自动依据脚本位置寻找项目根目录，命令在根目录执行。
- 脚本使用独立 Git 索引记录工作区内容，保留用户当前分支和原暂存内容，不使用 `git stash` 隐藏工作。
- 检查点作者标记为 `Project Backup`，不修改用户全局 Git 身份。
- 无内容变化时复用已有检查点，不制造重复提交；`after` 代表任务完成时的快照，是否通过验证仍须查看任务记录。

```powershell
# AI 修改前
pwsh -NoProfile -File ./scripts/git-checkpoint.ps1 -Phase before -Message "修改活动列表" -Push

# AI 完成验证后
pwsh -NoProfile -File ./scripts/git-checkpoint.ps1 -Phase after -Message "活动列表修改完成，已验证" -Push

# 你自己保存后端代码后，也可主动创建同样的检查点
pwsh -NoProfile -File ./scripts/git-checkpoint.ps1 -Phase manual -Message "后端报名事务阶段备份" -Push
```

自动执行范围是 AI 参与的修改任务；关闭 AI 后不会后台定时监控你手动编辑的文件。要保护两次 AI 任务之间的手动改动，在重要阶段执行 `manual`，或以后另行配置定时任务。

## 2 判断备份是否成功

脚本成功保存本地检查点后输出 `LOCAL_OK` 和提交哈希。带 `-Push` 且远程同步成功后，再输出 `REMOTE_OK`。

只看到 `LOCAL_OK` 不能视为 GitHub 已同步。远程失败时保留本地检查点、输出 `REMOTE_FAILED` 并以退出码2结束；本地备份错误则以非零状态失败。网络或认证修复后，重跑相同命令即可再次尝试推送。

远程推送不用 force。如果其他电脑已更新同一备份分支造成拒绝，应先获取和比较两侧历史，再按实际情况整合；不能强制覆盖远程检查点。

```powershell
git status --short --branch
git log backup/ai-checkpoints --oneline -n 15
git ls-remote origin refs/heads/backup/ai-checkpoints
```

本地最新检查点哈希与最后一条远程查询返回的哈希一致，才表明该检查点已同步。

## 3 恢复误删或误改的单个文件

先确认要恢复的文件、期望版本，以及是否存在用户有意修改的内容。AI 只能自动修复自己本次造成的问题，不把用户的新修改当成错误回滚。

```powershell
# 第一步：保护当前状态，包含错误状态，便于撤销恢复操作
pwsh -NoProfile -File ./scripts/git-checkpoint.ps1 -Phase before -Message "恢复文件前保留当前状态" -Push

# 第二步：查看检查点，选择错误发生前的正确提交
git log backup/ai-checkpoints --oneline -n 15

# 第三步：先检查选定提交的内容与差异
git show '<正确提交哈希>:frontend/src/views/admin/ActivityList.vue'
git diff '<正确提交哈希>' -- frontend/src/views/admin/ActivityList.vue

# 第四步：只恢复确认过的文件到工作区
git restore --source='<正确提交哈希>' --worktree -- frontend/src/views/admin/ActivityList.vue

# 第五步：检查恢复结果，再执行与该文件有关的验证
git diff -- frontend/src/views/admin/ActivityList.vue
git status --short
```

示例路径要替换成真实文件；`<正确提交哈希>` 是必须替换的占位符。先使用 `git show` 确认该版本含有目标文件。最新检查点也可能已经记录了错误，不要直接把“最新”当作“正确”。

恢复只改工作区，不自动修改暂存区。若原先存在已暂存的编辑或删除，需要检查 `git diff --cached`；不要自动用 `--staged` 覆盖用户暂存内容。恢复完成后执行 `after` 检查点。

## 4 从 GitHub 获取历史后恢复

```powershell
# 获取远程历史，不自动合并或覆盖工作区
git fetch origin

# 查找远程保存的检查点
git log origin/backup/ai-checkpoints --oneline -n 15

# 本地已备份当前状态后，按确认的版本恢复指定路径
git restore --source='<远程正确提交哈希>' --worktree -- frontend/src/views/admin/ActivityList.vue
```

不使用 `git pull` 代替上述步骤。pull 会更新并尝试合并分支，不能保证找回目标文件，还可能把远程的删除继续应用到本地。

若整个本地项目或 `.git` 已丢失，在新的、不存在的目录中克隆备份分支：

```powershell
git clone --branch backup/ai-checkpoints https://github.com/archers2024/Xidian_ProgrammingDesigning.git '<新的恢复目录>'
```

恢复目录不得覆盖现有工作目录。先检查快照内容，确认版本后再安排后续开发。

## 5 Git 能恢复的范围

- 可恢复：进入过提交或检查点的源码、文档、SQL、配置模板、已纳入快照的新文件。
- 不可保证恢复：从未保存进任何提交或检查点就被删除的文件。
- 不通过本脚本备份：被忽略的 `runtime/`、上传文件、数据库内容、私有配置、依赖和构建目录。
- 数据库与上传文件应另外做配套备份；不能将源码检查点说成整个运行系统的数据备份。

禁止用 `git reset --hard`、`git clean -fd`、强制推送或整仓覆盖来修复单个文件，也不自动删除旧备份分支或改写已有检查点。
