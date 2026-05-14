# Task: Backend 개발 체크리스트 (채널 삭제·수정)

> **대상 버전**: v2.1 (fix)
> **작성일**: 2026-05-13
> **연관 Plan**: `docs/plans/v2/plan_fix.md`

---

## 변경 없음

백엔드에는 채널 수정·삭제 API가 이미 구현되어 있습니다.

- `PUT /api/v1/channels/{id}` — 채널 수정 (완료)
- `DELETE /api/v1/channels/{id}` — 채널 삭제 soft delete (완료)
- `ChannelResponse`에 `target_identifier` 필드 반환 중 (완료)

프론트엔드 작업만 필요합니다. `docs/tasks/v2/task_frontend.md` 참조.
