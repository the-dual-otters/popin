// /js/admin/mission-management.js
class MissionManagement {
    constructor() {
        this.currentPage = 1;
        this.pageSize = 10;
        this.totalPages = 0;
        this.currentFilters = {};
        this.selectedSetId = null; // UUID

        // 팝업 id -> title 캐시
        this.popupTitleById = {};

        this.init();
    }

    async init() {
        this.checkAdminAuth();
        this.bindEvents();
        await this.ensurePopupMap();
        await this.ensurePopupMap();
        await this.populatePopupFilter();
        this.loadMissionSets();
    }

    // 관리자 권한 확인
    checkAdminAuth() {
        const token = localStorage.getItem('authToken');
        const userRole = localStorage.getItem('userRole');
        if (!token || userRole !== 'ADMIN') {
            alert('관리자만 접근할 수 있습니다.');
            window.location.href = '/templates/auth/login.html';
        }
    }

    // 인증 헤더
    getAuthHeaders() {
        return {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${localStorage.getItem('authToken')}`
        };
    }

    // 공통 응답 체크
    async assertOk(res, msgOnFail) {
        if (res.status === 401) {
            alert('인증이 필요합니다. 다시 로그인 해주세요.');
            window.location.href = '/templates/auth/login.html';
            throw new Error('Unauthorized');
        }
        if (!res.ok) {
            const text = await res.text().catch(() => '');
            throw new Error(`${msgOnFail} (${res.status}) ${text}`);
        }
    }

    // 이벤트 바인딩
    bindEvents() {
        // 검색
        document.getElementById('searchBtn').addEventListener('click', () => this.search());
        document.getElementById('resetBtn').addEventListener('click', () => this.resetFilters());
        document.getElementById('searchKeyword').addEventListener('keypress', e => {
            if (e.key === 'Enter') this.search();
        });

        // 상세 모달 닫기
        document.getElementById('modalCloseBtn').addEventListener('click', () => this.closeDetailModal());
        document.getElementById('closeModalBtn').addEventListener('click', () => this.closeDetailModal());

        // 미션셋 등록 모달
        document.getElementById('createMissionSetBtn').addEventListener('click', () => this.openCreateSetModal());
        document.getElementById('createSetCloseBtn').addEventListener('click', () => this.closeCreateSetModal());
        document.getElementById('createSetCancelBtn').addEventListener('click', () => this.closeCreateSetModal());
        document.getElementById('createSetConfirmBtn').addEventListener('click', () => this.createMissionSet());

        // 미션 추가 모달
        document.getElementById('addMissionBtn').addEventListener('click', () => this.openAddMissionModal());
        document.getElementById('addMissionCloseBtn').addEventListener('click', () => this.closeAddMissionModal());
        document.getElementById('addMissionCancelBtn').addEventListener('click', () => this.closeAddMissionModal());
        document.getElementById('addMissionConfirmBtn').addEventListener('click', () => this.addMission());

        // 미션셋 삭제
        document.getElementById('deleteSetBtn').addEventListener('click', () => this.deleteSet());
    }

    // ===== 팝업 타이틀 맵 =====
    async ensurePopupMap() {
        if (Object.keys(this.popupTitleById).length > 0) return;
        await this.refreshPopupMap();
    }

    async refreshPopupMap() {
        try {
            const url = `/api/admin/popups?page=0&size=1000`; // status 파라미터 넣지 않음
            const res = await fetch(url, {headers: this.getAuthHeaders()});
            await this.assertOk(res, '팝업 목록 로딩 실패');
            const page = await res.json();

            const list = (page && (page.content || page.popups || [])) || [];
            const map = {};
            list.forEach(p => {
                if (p && p.id != null) map[p.id] = p.title || '(무제 팝업)';
            });
            this.popupTitleById = map;
        } catch (e) {
            console.warn('팝업 타이틀 맵 로드 실패:', e);
            this.popupTitleById = {};
        }
    }

    // ===== 목록 =====
    async loadMissionSets() {
        try {
            this.showLoading();
            await this.ensurePopupMap();

            const params = {
                page: this.currentPage - 1,
                size: this.pageSize,
                ...this.currentFilters
            };

            const data = await apiService.getMissionSets(params);

            this.renderTable(data.content);
            this.renderPagination(data);
        } catch (e) {
            console.error(e);
            this.showError('미션셋 목록을 불러오는데 실패했습니다.');
        }
    }

    showLoading() {
        document.getElementById('tableContainer').innerHTML = '<div class="loading">데이터를 불러오는 중...</div>';
    }

    showError(msg) {
        document.getElementById('tableContainer').innerHTML = `<div class="no-data">${msg}</div>`;
    }

    // 테이블
    renderTable(items) {
        if (!items || items.length === 0) {
            document.getElementById('tableContainer').innerHTML = '<div class="no-data">등록된 미션셋이 없습니다.</div>';
            return;
        }
        const html = `
      <table class="popup-table">
        <thead>
          <tr>
            <th>미션셋ID</th>
            <th>팝업</th>
            <th>필요완료수</th>
            <th>미션수</th>
            <th>상태</th>
            <th>등록일</th>
            <th>액션</th>
          </tr>
        </thead>
        <tbody>
          ${items.map(s => this.renderRow(s)).join('')}
        </tbody>
      </table>`;
        document.getElementById('tableContainer').innerHTML = html;
    }

    renderRow(set) {
        const statusClass = (set.status || '').toLowerCase();
        const missionsCount = Array.isArray(set.missions) ? set.missions.length : (set.totalMissions || 0);
        const fullId = (set.id || set.missionSetId || '').toString();
        const popupId = set.popupId;
        const popupTitle = popupId != null ? (this.popupTitleById[popupId] || `#${popupId}`) : '-';

        return `
      <tr>
      <td class="mono small break" title="${fullId}">${fullId}</td>
        <td title="${popupId ?? ''}">${this.escapeHtml(popupTitle)}</td>
        <td>${set.requiredCount ?? 0}</td>
        <td>${missionsCount}</td>
        <td><span class="status-badge ${statusClass}">${set.status || '-'}</span></td>
        <td>${this.formatDate(set.createdAt)}</td>
        <td>
          <div class="action-buttons">
            <button class="button button-primary" onclick="missionManagement.viewDetail('${set.id || set.missionSetId}')">상세보기</button>
          </div>
        </td>
      </tr>`;
    }

    // 페이지네이션
    renderPagination(pageInfo) {
        const {number, totalPages, first, last} = pageInfo;

        if (totalPages <= 1) {
            document.getElementById('pagination').innerHTML = '';
            return;
        }

        const startPage = Math.max(0, number - 2);
        const endPage = Math.min(totalPages - 1, number + 2);

        let html = `<button class="page-button" ${first ? 'disabled' : ''} onclick="popupManagement.goToPage(${number})">이전</button>`;

        for (let i = startPage; i <= endPage; i++) {
            const activeClass = i === number ? 'active' : '';
            html += `<button class="page-button ${activeClass}" onclick="popupManagement.goToPage(${i + 1})">${i + 1}</button>`;
        }

        html += `<button class="page-button" ${last ? 'disabled' : ''} onclick="popupManagement.goToPage(${number + 2})">다음</button>`;

        document.getElementById('pagination').innerHTML = html;
    }

    goToPage(page) {
        this.currentPage = page;
        this.loadMissionSets();
    }

    // ===== 필터 =====
    search() {
        const status = document.getElementById('statusFilter')?.value || '';
        const keyword = (document.getElementById('searchKeyword')?.value || '').trim();
        const popupEl = document.getElementById('popupFilter') || document.getElementById('popupIdFilter');
        let popupId = popupEl ? popupEl.value : '';
        // 숫자 형태로 정규화 (서버가 Long 받는 경우 대비)
        if (popupId !== '') popupId = String(Number(popupId));
        const f = {status, keyword};
        if (popupId !== '' && popupId !== 'NaN') f.popupId = popupId;
        this.currentFilters = Object.fromEntries(Object.entries(f).filter(([, v]) => v !== '' && v != null));
        this.currentPage = 1;
        this.loadMissionSets();
    }

    resetFilters() {
        document.getElementById('statusFilter').value = '';
        const popupEl = document.getElementById('popupFilter') || document.getElementById('popupIdFilter');
        if (popupEl) popupEl.value = '';
        document.getElementById('searchKeyword').value = '';
        this.currentFilters = {};
        this.currentPage = 1;
        this.loadMissionSets();
    }

    // ===== 상세 =====
    async viewDetail(setId) {
        try {
            const set = await apiService.getMissionSetDetail(setId);
            this.selectedSetId = set.id || set.missionSetId;

            await this.ensurePopupMap(); // 타이틀 보장
            this.showDetailModal(set);
        } catch (e) {
            console.error(e);
            alert('미션셋 상세 정보를 불러오는데 실패했습니다.');
        }
    }

    showDetailModal(set) {
        const missions = Array.isArray(set.missions) ? set.missions : [];
        const rewards = Array.isArray(set.rewards) ? set.rewards : [];
        const idDisp = (set.id || set.missionSetId || '').toString();
        const popupId = set.popupId;
        const popupTitle = popupId != null ? (this.popupTitleById[popupId] || `#${popupId}`) : '-';

        const modalBody = document.getElementById('modalBody');
        modalBody.innerHTML = `
    <div class="detail-section" id="missionSetInfo">
        <h3>기본 정보</h3>
        <div class="detail-grid" id="missionSetInfoFields">
          <div class="detail-item"><span class="detail-label">미션셋 ID</span><span class="detail-value mono small">${idDisp}</span></div>
          <div class="detail-item"><span class="detail-label">팝업</span><span class="detail-value" title="${popupId ?? ''}">${this.escapeHtml(popupTitle)}</span></div>
          ${set.qrImageUrl ? `
            <div class="detail-item">
              <span class="detail-label">QR 코드</span>
              <span class="detail-value">
                <a href="${set.qrImageUrl}" download="missionset-${idDisp}.png" class="qr-download">QR 다운로드</a>
              </span>
            </div>
          ` : ''}
          <div class="detail-item"><span class="detail-label">필요 완료 수</span><span class="detail-value" id="dispRequiredCount">${set.requiredCount ?? 0}</span></div>
          <div class="detail-item"><span class="detail-label">상태</span><span class="detail-value"><span class="status-badge ${(set.status || '').toLowerCase()}" id="dispStatus">${set.status || '-'}</span></span></div>
          <div class="detail-item"><span class="detail-label">리워드 PIN</span><span class="detail-value" id="dispRewardPin">${set.rewardPin || '-'}</span></div>
          <div class="detail-item"><span class="detail-label">생성일</span><span class="detail-value">${this.formatDate(set.createdAt)}</span></div>
        </div>
        <div style="margin-top:12px">
          <button class="button button-primary" id="editBtn">수정</button>
        </div>
      </div>

    <div class="detail-section">
      <h3>미션 목록</h3>
      <div class="mission-list">
        ${missions.length === 0 ? '<div class="no-data">등록된 미션이 없습니다.</div>' : missions.map(m => `
          <div class="mission-row">
            <div>
                <div><strong>제목: ${this.escapeHtml(m.title || '(제목없음)')}</strong></div>
                ${m.description ? `<div class="small" style="margin-top:4px;">상세설명: ${this.escapeHtml(m.description)}</div>` : ''}
                ${m.answer ? `<div class="small" style="margin-top:4px;">정답: ${this.escapeHtml(m.answer)}</div>` : ''}
            </div>
            <div class="action-buttons">
              <button class="button button-sm button-danger-outline" onclick="missionManagement.deleteMission('${m.id}')">삭제</button>
            </div>
          </div>
        `).join('')}
      </div>
    </div>

      <!-- 리워드 추가 -->
      <div class="detail-section">
        <h3 style="margin:0 0 8px 0;">리워드</h3>
        <div style="display:grid; grid-template-columns: 1fr 160px 120px; gap:8px; align-items:end; max-width:520px; margin-bottom:12px;">
          <div>
            <label class="detail-label" for="rewardNameInput">리워드 이름</label>
            <input id="rewardNameInput" type="text" placeholder="예) 머그컵" />
          </div>
          <div>
            <label class="detail-label" for="rewardTotalInput">총 수량</label>
            <input id="rewardTotalInput" type="number" min="0" step="1" value="0" />
          </div>
          <div>
            <button class="button button-primary" id="addRewardBtn" style="width:100%;">리워드 추가</button>
          </div>
        </div>
        <div id="rewardsArea"></div>
      </div>
    `;
        document.getElementById('detailModal').style.display = 'block';
        document.getElementById('editBtn').addEventListener('click', () => this.enableEditMode(set));
        document.getElementById('addRewardBtn').addEventListener('click', () => this.addReward());

        // 리워드 렌더
        this.renderRewards(rewards);
    }

    // 리워드 추가 (입력창 사용)
    // 리워드 추가 (인풋 사용)
    async addReward() {
        if (!this.selectedSetId) return;

        const nameEl = document.getElementById('rewardNameInput');
        const totalEl = document.getElementById('rewardTotalInput');
        const name = (nameEl?.value || '').trim();
        const total = parseInt(totalEl?.value ?? '0', 10);

        if (!name) {
            alert('리워드 이름을 입력하세요.');
            nameEl?.focus();
            return;
        }
        if (!Number.isInteger(total) || total < 0) {
            alert('총 수량은 0 이상의 정수여야 합니다.');
            totalEl?.focus();
            return;
        }

        await apiService.createReward(this.selectedSetId, {name, total});
        alert('리워드를 추가했습니다.');
        // 입력값 초기화
        if (nameEl) nameEl.value = '';
        if (totalEl) totalEl.value = '0';
        // 목록 갱신
        await this.viewDetail(this.selectedSetId);     }

// 리워드 목록 렌더
    renderRewards(list) {
        const area = document.getElementById('rewardsArea');
        if (!area) return;

        if (!list.length) {
            area.innerHTML = '<div class="no-data">등록된 리워드가 없습니다.</div>';
            return;
        }

        area.innerHTML = `
      <table class="popup-table">
        <thead>
          <tr>
            <th>이름</th>
            <th>총 수량</th>
            <th>발급</th>
            <th>잔여</th>
            <th>액션</th>
          </tr>
        </thead>
        <tbody>
          ${list.map(r => `
            <tr data-id="${r.id}">
              <td><input type="text" id="reward-name-${r.id}" value="${this.escapeHtml(r.name || '')}" /></td>
              <td><input type="number" min="0" step="1" id="reward-total-${r.id}" value="${r.total ?? 0}" /></td>
              <td>${r.issued ?? 0}</td>
              <td>${(r.remaining != null ? r.remaining : Math.max(0, (r.total || 0) - (r.issued || 0)))}</td>
              <td>
                <button class="button button-sm" onclick="missionManagement.saveReward('${r.id}')">수정</button>
                <button class="button button-sm button-danger-outline" onclick="missionManagement.deleteReward('${r.id}')">삭제</button>
              </td>
            </tr>
          `).join('')}
        </tbody>
      </table>
    `;
    }


// 리워드 수정
    async saveReward(rewardId) {
        if (!this.selectedSetId) return;

        const name = (document.getElementById(`reward-name-${rewardId}`)?.value || '').trim();
        const total = parseInt(document.getElementById(`reward-total-${rewardId}`)?.value ?? '0', 10);

        if (!name) {
            alert('이름을 입력하세요.');
            return;
        }
        if (!Number.isInteger(total) || total < 0) {
            alert('총 수량은 0 이상의 정수여야 합니다.');
            return;
        }

        await apiService.updateReward(this.selectedSetId, rewardId, {name, total});
        alert('리워드를 저장했습니다.');
        await this.viewDetail(this.selectedSetId);
    }

// 리워드 삭제
    async deleteReward(rewardId) {
        if (!this.selectedSetId) return;
        if (!confirm('이 리워드를 삭제하시겠습니까?')) return;

        await apiService.deleteReward(this.selectedSetId, rewardId);
        alert('리워드를 삭제했습니다.');
        await this.viewDetail(this.selectedSetId);
    }


    closeDetailModal() {
        document.getElementById('detailModal').style.display = 'none';
        this.selectedSetId = null;
    }

    // ===== 미션셋 등록 =====
    async openCreateSetModal() {
        await this.populatePopupSelect();
        document.getElementById('createSetModal').style.display = 'block';
    }

    closeCreateSetModal() {
        document.getElementById('createSetModal').style.display = 'none';
    }

    async populatePopupSelect() {
        const sel = document.getElementById('createPopupId');
        if (!sel) return;

        sel.disabled = true;
        sel.innerHTML = `<option value="">로딩 중...</option>`;

        // 캐시 있으면 즉시
        if (Object.keys(this.popupTitleById).length > 0) {
            this.fillPopupOptions(sel);
            sel.disabled = false;
            return;
        }

        // 없으면 로드 후 채우기
        try {
            await this.refreshPopupMap();
            this.fillPopupOptions(sel);
        } catch (err) {
            console.error('팝업 옵션 로드 실패:', err);
            sel.innerHTML = `<option value="">로딩 실패</option>`;
        } finally {
            sel.disabled = false;
        }
    }

    fillPopupOptions(selectEl) {
        const ids = Object.keys(this.popupTitleById);
        selectEl.innerHTML =
            `<option value="">팝업을 선택하세요</option>` +
            ids.sort((a, b) => Number(a) - Number(b))
                .map(id => `<option value="${id}">[${id}] ${this.escapeHtml(this.popupTitleById[id])}</option>`)
                .join('');
    }

    // ===== 미션셋 등록 =====
    async createMissionSet() {
        const popupId = Number(document.getElementById('createPopupId').value);
        const requiredCount = Number(document.getElementById('createRequiredCount').value || 0);
        const rewardPin = (document.getElementById('createRewardPin').value || '').trim();
        const status = document.getElementById('createStatus').value || 'ACTIVE';

        if (!popupId) {
            alert('팝업 ID는 필수입니다.');
            return;
        }

        try {
            await apiService.createMissionSet({popupId, requiredCount, status, rewardPin});
            alert('미션셋이 등록되었습니다.');
            this.closeCreateSetModal();
            await this.ensurePopupMap(); // 팝업 캐시 최신화
            this.loadMissionSets();
        } catch (e) {
            console.error(e);
            alert('미션셋 등록에 실패했습니다.');
        }
    }

    async saveMissionSet() {
        if (!this.selectedSetId) return;

        const requiredCount = Number(document.getElementById('editRequiredCount')?.value || 0);
        let status = (document.getElementById('editStatus')?.value || 'ENABLED').toUpperCase();
        // 혹시 예전 값이 들어오면 매핑
        if (status === 'ACTIVE') status = 'ENABLED';
        if (status === 'COMPLETED') status = 'DISABLED';
        const rewardPin = (document.getElementById('editRewardPin')?.value || '').trim();

        try {
            await apiService.updateMissionSet(this.selectedSetId, {requiredCount, status, rewardPin});
            alert('저장했습니다.');
            await this.viewDetail(this.selectedSetId); // 상세 갱신
            this.loadMissionSets();                     // 목록 갱신
        } catch (e) {
            console.error(e);
            alert('저장에 실패했습니다.');
        }
    }

    // ===== 미션 추가/삭제 =====
    openAddMissionModal() {
        if (!this.selectedSetId) return alert('미션셋을 먼저 선택하세요.');
        document.getElementById('missionTitle').value = '';
        document.getElementById('missionDesc').value = '';
        document.getElementById('missionAnswer').value = '';
        document.getElementById('addMissionModal').style.display = 'block';
    }

    closeAddMissionModal() {
        document.getElementById('addMissionModal').style.display = 'none';
    }

    // ===== 미션 추가 =====
    async addMission() {
        if (!this.selectedSetId) return;
        const title = document.getElementById('missionTitle').value.trim();
        const description = document.getElementById('missionDesc').value.trim();
        const answer = document.getElementById('missionAnswer').value.trim();

        if (!title) {
            alert('제목은 필수입니다.');
            return;
        }

        try {
            await apiService.addMission(this.selectedSetId, {title, description, answer});
            alert('미션이 추가되었습니다.');
            this.closeAddMissionModal();
            this.viewDetail(this.selectedSetId); // 상세 재조회
        } catch (e) {
            console.error(e);
            alert('미션 추가에 실패했습니다.');
        }
    }

    async populatePopupFilter() {
        const sel = document.getElementById('popupFilter');
        if (!sel) return;
        sel.disabled = true;
        sel.innerHTML = `<option value="">로딩 중...</option>`;

        try {
            // 캐시가 없다면 맵 갱신
            if (Object.keys(this.popupTitleById).length === 0) {
                await this.refreshPopupMap();
            }
            // 옵션 채우기
            const ids = Object.keys(this.popupTitleById);
            const options =
                `<option value="">전체</option>` +
                ids.sort((a, b) => Number(a) - Number(b))
                    .map(id => `<option value="${id}">[${id}] ${this.escapeHtml(this.popupTitleById[id])}</option>`)
                    .join('');
            sel.innerHTML = options;
        } catch (e) {
            console.error('팝업 필터 옵션 로드 실패:', e);
            sel.innerHTML = `<option value="">로딩 실패</option>`;
        } finally {
            sel.disabled = false;
        }
    }

    async deleteMission(missionId) {
        if (!confirm('이 미션을 삭제하시겠습니까?')) return;
        try {
            await apiService.deleteMission(missionId);
            alert('삭제되었습니다.');
            this.viewDetail(this.selectedSetId); // 상세 다시 불러오기
        } catch (e) {
            console.error(e);
            alert('미션 삭제에 실패했습니다.');
        }
    }

    enableEditMode(set) {
        const fields = document.getElementById('missionSetInfoFields');
        fields.classList.add('edit-mode');
        fields.innerHTML = `
    <div class="detail-item">
      <span class="detail-label">필요 완료 수</span>
      <input type="number" id="editRequiredCount" value="${set.requiredCount ?? 0}" min="0"/>
    </div>
    <div class="detail-item">
      <span class="detail-label">상태</span>
      <select id="editStatus">
        <option value="ENABLED" ${set.status === 'ENABLED' ? 'selected' : ''}>ENABLED</option>
        <option value="DISABLED" ${set.status === 'DISABLED' ? 'selected' : ''}>DISABLED</option>
      </select>
    </div>
    <div class="detail-item">
      <span class="detail-label">리워드 PIN</span>
      <input type="text" id="editRewardPin" value="${set.rewardPin || ''}" maxlength="80"/>
    </div>
  `;

        // 버튼 영역 교체
        const btnArea = document.querySelector('#missionSetInfo > div:last-child');
        btnArea.innerHTML = `
    <button class="button button-primary" onclick="missionManagement.saveMissionSet()">저장</button>
    <button class="button button-secondary" onclick="missionManagement.cancelEdit()">취소</button>
  `;
    }

    cancelEdit() {
        // 다시 상세를 로드해서 원래 표시 모드로 돌려놓기
        this.viewDetail(this.selectedSetId);
    }

    async deleteSet() {
        if (!this.selectedSetId) return;
        if (!confirm('이 미션셋을 삭제하시겠습니까?')) return;
        try {
            await apiService.deleteMissionSet(this.selectedSetId);
            alert('미션셋이 삭제되었습니다.');
            this.closeDetailModal();
            this.loadMissionSets();
        } catch (e) {
            console.error(e);
            alert('미션셋 삭제에 실패했습니다.');
        }
    }

    // ===== 유틸 =====
    formatDate(s) {
        if (!s) return '-';
        const d = new Date(s);
        if (Number.isNaN(d.getTime())) return '-';
        return d.toLocaleDateString('ko-KR', {year: 'numeric', month: '2-digit', day: '2-digit'});
    }

    escapeHtml(s = '') {
        return s.replace(/[&<>"']/g, m => ({'&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'}[m]));
    }
}

// 전역 인스턴스
let missionManagement;
document.addEventListener('DOMContentLoaded', () => {
    missionManagement = new MissionManagement();
});
