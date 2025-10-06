document.addEventListener('DOMContentLoaded', async function () {
    await loadComponents();
    initializeLayout();

    const listEl = document.getElementById('reservation-list');
    const emptyEl = document.getElementById('reservation-empty');

    // 상태 바/더보기 버튼 동적 삽입
    const toolbar = document.createElement('div');
    toolbar.className = 'reservation-toolbar';
    toolbar.innerHTML = `
    <div class="status-filter" style="display:flex; gap:8px; flex-wrap:wrap; margin-bottom:12px;">
      <button type="button" class="mypage-btn active" data-status="ALL">전체</button>
      <button type="button" class="mypage-btn" data-status="RESERVED">예약됨</button>
      <button type="button" class="mypage-btn" data-status="VISITED">방문완료</button>
      <button type="button" class="mypage-btn" data-status="CANCELLED">예약취소</button>
    </div>
  `;
    listEl.parentNode.insertBefore(toolbar, listEl);

    const moreWrap = document.createElement('div');
    moreWrap.style = 'display:flex; justify-content:center; margin-top:12px;';
    moreWrap.innerHTML = `<button type="button" id="btnLoadMore" class="mypage-btn">더 보기</button>`;
    listEl.parentNode.insertBefore(moreWrap, moreWrap.nextSibling);
    const btnMore = moreWrap.querySelector('#btnLoadMore');

    // 상태
    let all = [];
    let filtered = [];
    let currentStatus = 'ALL';
    const PAGE_SIZE = 5;
    let offset = 0;

    try {
        const data = await apiService.get('/reservations/my');
        all = Array.isArray(data) ? data : [];

        if (!all.length) {
            emptyEl.style.display = 'block';
            btnMore.style.display = 'none';
            return;
        }

        applyFilter('ALL');
    } catch (err) {
        console.error('예약 목록 불러오기 실패:', err);
        const section = document.querySelector('.content-section');
        if (section) section.innerHTML = `<p style="color:red; text-align:center;">로그인 후 이용 가능합니다.</p>`;
    }

    // 필터 클릭
    toolbar.addEventListener('click', (e) => {
        const btn = e.target.closest('button[data-status]');
        if (!btn) return;
        const next = btn.getAttribute('data-status');
        applyFilter(next);
        Array.from(toolbar.querySelectorAll('button[data-status]')).forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
    });

    // 더 보기
    btnMore.addEventListener('click', () => {
        renderList(false);
    });

    // 이벤트 위임: 상세보기/취소
    listEl.addEventListener('click', async (e) => {
        const goBtn = e.target.closest('.js-go-popup');
        if (goBtn) {
            location.href = `/popup/${goBtn.dataset.popupId}`;
            return;
        }

        const cancelBtn = e.target.closest('.js-cancel');
        if (cancelBtn) {
            const reservationId = Number(cancelBtn.dataset.id);
            const reservation = all.find(r => r.id === reservationId);

            let confirmMessage = '예약을 취소하시겠습니까?';

            // 결제된 예약인 경우 환불 안내 추가
            if (reservation && reservation.paymentAmount && reservation.paymentAmount > 0) {
                confirmMessage = `예약을 취소하시겠습니까?\n\n 결제 금액: ${reservation.paymentAmount.toLocaleString()}원\n🔄 환불 처리: 자동으로 진행됩니다\n⏰ 환불 소요 시간: 1-3영업일`;
            }

            if (!confirm(confirmMessage)) return;

            cancelBtn.disabled = true;
            cancelBtn.textContent = '취소 중...';

            try {
                await apiService.put(`/reservations/${cancelBtn.dataset.id}/cancel`);

                // 메모리 갱신
                updateStatusLocal(reservationId, 'CANCELLED', '예약취소');

                // 현재 필터 기준으로 다시 렌더
                applyFilter(currentStatus);

                // 성공 메시지도 결제 여부에 따라 구분
                if (reservation && reservation.paymentAmount && reservation.paymentAmount > 0) {
                    alert(' 예약이 취소되었습니다.\n 환불 처리가 완료되었습니다.\n\n결제 수단에 따라 환불까지 1-3영업일이 소요될 수 있습니다.');
                } else {
                    alert(' 예약이 취소되었습니다.');
                }

            } catch (e2) {
                console.error('예약 취소 실패:', e2);

                let errorMessage = '취소에 실패했습니다.';

                if (e2?.response?.data?.message) {
                    if (e2.response.data.message.includes('환불')) {
                        errorMessage = '환불 처리 중 오류가 발생했습니다.\n고객센터에 문의해주세요.\n\n오류: ' + e2.response.data.message;
                    } else {
                        errorMessage = e2.response.data.message;
                    }
                }

                alert(errorMessage);
                cancelBtn.disabled = false;
                cancelBtn.textContent = '예약 취소';
            }
        }
    });

    function applyFilter(status) {
        currentStatus = status;
        offset = 0;

        if (status === 'ALL') filtered = all.slice();
        else filtered = all.filter(x => x.status === status);

        listEl.innerHTML = '';
        emptyEl.style.display = filtered.length ? 'none' : 'block';
        renderList(true);
    }

    function renderList(reset) {
        if (reset) {
            listEl.innerHTML = '';
            offset = 0;
        }

        const next = filtered.slice(offset, offset + PAGE_SIZE);
        next.forEach(r => listEl.appendChild(renderReservationCard(r)));
        offset += next.length;

        btnMore.style.display = offset < filtered.length ? 'inline-block' : 'none';
    }

    function updateStatusLocal(id, newStatus, newDesc) {
        // all 배열 갱신
        const idx = all.findIndex(x => x.id === id);
        if (idx >= 0) {
            all[idx] = { ...all[idx], status: newStatus, statusDescription: newDesc || all[idx].statusDescription };
        }
    }
});

/**
 * 카드 렌더링
 */
function renderReservationCard(r) {
    const item = document.createElement('div');
    item.className = 'card';

    const statusBadge = `<span class="status-badge status-${r.status}">${r.statusDescription || r.status}</span>`;

    item.innerHTML = `
    <div class="popup-info">
      <div class="popup-title">${escapeHtml(r.popupTitle || '팝업')}</div>

      <div class="meta-row"><span class="meta-key">장소</span><span class="meta-val">${escapeHtml(r.venueName || '')}</span></div>
      <div class="meta-row"><span class="meta-key">주소</span><span class="meta-val">${escapeHtml(r.venueAddress || '')}</span></div>
      <div class="meta-row"><span class="meta-key">예약일시</span><span class="meta-val">${fmt(r.reservationDate)}</span></div>
      <div class="meta-row"><span class="meta-key">예약상태</span><span class="meta-val">${statusBadge}</span></div>

      <div class="popup-summary">${escapeHtml(r.popupSummary || '')}</div>

      <div class="popup-action" style="display:flex; gap:8px; flex-wrap:wrap;">
        <button type="button" class="mypage-btn js-go-popup" data-popup-id="${r.popupId}">팝업 상세 보기</button>
        ${r.status === 'RESERVED'
        ? `<button type="button" class="mypage-btn js-cancel" data-id="${r.id}">예약 취소</button>`
        : ``}
      </div>
    </div>
  `;
    return item;
}

function fmt(iso) {
    if (!iso) return '-';
    try { return new Date(iso).toLocaleString(); } catch { return iso; }
}

function escapeHtml(str) {
    if (str == null) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}
