function translateStatus(status) {
    switch (status) {
        case 'PLANNED': return '준비 중';
        case 'ONGOING': return '진행 중';
        case 'ENDED': return '종료됨';
        case 'CANCELLED': return '취소됨';
        case 'PENDING': return '예약 대기 중';
        case 'ACCEPTED': return '승인됨';
        case 'REJECTED': return '거절됨';
        default: return status || '';
    }
}

const HostPage = {
    currentPage: 0,
    totalPages: 0,
    allPopups: [],

    async init() {
        try {
            const [hostInfo, myReservations] = await Promise.all([
                apiService.get('/hosts/me'),
                apiService.get('/space-reservations/my-requests')
            ]);

            this.renderHostInfo(hostInfo);
            this.renderReservations(myReservations);
            this.setupEditButtons(hostInfo);

            // 팝업은 별도로 로드 (첫 페이지)
            await this.loadPopups(0);
            this.setupLoadMoreButton();
        } catch (err) {
            console.error('HostPage init 실패:', err);
            alert('데이터 로딩 중 오류가 발생했습니다.');
        }
    },

    async loadPopups(page = 0) {
        try {
            const response = await apiService.get(`/hosts/popups?page=${page}&size=5`);
            const popups = response.content || response;

            if (page === 0) {
                this.allPopups = popups;
            } else {
                this.allPopups.push(...popups);
            }

            this.currentPage = response.number || page;
            this.totalPages = response.totalPages || 1;

            this.renderPopups(this.allPopups);

            if (page === 0) {
                this.setupLoadMoreButton();
            }

            setTimeout(() => {
                this.updateLoadMoreButton();
            }, 100);

        } catch (err) {
            console.error('팝업 로딩 실패:', err);
        }
    },

    setupLoadMoreButton() {
        const listEl = document.getElementById('my-popup-list');
        const container = listEl.parentNode;

        const existingBtn = document.getElementById('load-more-popups');
        if (existingBtn) {
            existingBtn.remove();
        }

        const loadMoreBtn = document.createElement('button');
        loadMoreBtn.id = 'load-more-popups';
        loadMoreBtn.className = 'load-more-btn';
        loadMoreBtn.textContent = '더보기';
        loadMoreBtn.style.cssText = `
            width: 100%;
            padding: 12px;
            margin-top: 20px;
            background: #f8f9fa;
            border: 1px solid #dee2e6;
            border-radius: 8px;
            cursor: pointer;
            font-size: 14px;
            display: none;
        `;

        loadMoreBtn.addEventListener('click', () => {
            this.loadPopups(this.currentPage + 1);
        });

        loadMoreBtn.addEventListener('mouseenter', () => {
            loadMoreBtn.style.background = '#e9ecef';
        });

        loadMoreBtn.addEventListener('mouseleave', () => {
            loadMoreBtn.style.background = '#f8f9fa';
        });

        container.appendChild(loadMoreBtn);
    },

    updateLoadMoreButton() {
        const loadMoreBtn = document.getElementById('load-more-popups');
        if (loadMoreBtn) {
            if (this.currentPage + 1 < this.totalPages) {
                loadMoreBtn.style.display = 'block';
            } else {
                loadMoreBtn.style.display = 'none';
            }
        }
    },

    renderHostInfo(info) {
        document.getElementById('user-email').textContent = info.email || '-';
        document.getElementById('user-name').textContent = info.name || '-';
        document.getElementById('user-nickname').textContent = info.nickname || '-';
        document.getElementById('user-phone').textContent = info.phone || '-';
        document.getElementById('user-brand').textContent = info.brandName || '-';
    },

    setupEditButtons(hostInfo) {
        const editableFields = [
            { idx: 1, field: 'name', label: '이름', elementId: 'user-name' },
            { idx: 2, field: 'nickname', label: '닉네임', elementId: 'user-nickname' },
            { idx: 3, field: 'phone', label: '연락처', elementId: 'user-phone' }
        ];

        document.querySelectorAll('.edit-btn').forEach((btn, idx) => {
            const fieldConfig = editableFields.find(f => f.idx === idx + 1);
            if (!fieldConfig) return;

            btn.addEventListener('click', () => {
                const spanEl = document.getElementById(fieldConfig.elementId);
                if (!spanEl) return;

                const currentValue = spanEl.textContent;
                const input = document.createElement('input');
                input.type = 'text';
                input.value = currentValue === '-' ? '' : currentValue;
                input.className = 'inline-edit';

                const saveBtn = document.createElement('button');
                saveBtn.textContent = '저장';
                saveBtn.className = 'save-btn';

                const cancelBtn = document.createElement('button');
                cancelBtn.textContent = '취소';
                cancelBtn.className = 'cancel-btn';

                const buttonGroup = document.createElement('div');
                buttonGroup.className = 'button-group';
                buttonGroup.appendChild(saveBtn);
                buttonGroup.appendChild(cancelBtn);

                spanEl.replaceWith(input);
                btn.replaceWith(buttonGroup);

                cancelBtn.addEventListener('click', () => {
                    input.replaceWith(spanEl);
                    buttonGroup.replaceWith(btn);
                });

                saveBtn.addEventListener('click', async () => {
                    const newValue = input.value.trim();
                    if (!newValue || newValue === currentValue) {
                        cancelBtn.click();
                        return;
                    }

                    try {
                        const updatedData = await apiService.put('/users/me', {
                            name: fieldConfig.field === 'name' ? newValue : hostInfo.name,
                            nickname: fieldConfig.field === 'nickname' ? newValue : hostInfo.nickname,
                            phone: fieldConfig.field === 'phone' ? newValue : hostInfo.phone
                        });

                        hostInfo[fieldConfig.field] = updatedData[fieldConfig.field];

                        spanEl.textContent = newValue;
                        input.replaceWith(spanEl);
                        buttonGroup.replaceWith(btn);

                        alert(`${fieldConfig.label}이(가) 수정되었습니다.`);
                    } catch (err) {
                        console.error(err);
                        alert(`${fieldConfig.label} 수정 실패: ${err.message || err}`);
                    }
                });
            });
        });
    },

    renderPopups(popups) {
        const listEl = document.getElementById('my-popup-list');
        listEl.innerHTML = '';

        if (popups && popups.length > 0) {
            popups.forEach(p => {
                const status = p.status || 'PLANNED';
                const card = document.createElement('div');
                card.className = 'popup-card';
                card.innerHTML = `
                    <img src="${p.mainImageUrl || '/img/placeholder.png'}" class="thumb" alt="팝업 이미지">
                    <div class="info">
                        <div class="title">${p.title || '제목 없음'}</div>
                        <div class="meta">
                            <span class="status-badge ${status.toLowerCase()}">${translateStatus(status)}</span>
                        </div>
                    </div>
                    <div class="right-actions">
                        <button class="btn-detail" data-popup-id="${p.id}">상세보기</button>
                        <button class="btn-manage" data-popup-id="${p.id}">예약관리</button>
                        <button class="btn-stats" data-popup-id="${p.id}">통계</button>
                    </div>
                `;
                this.addPopupCardEventListeners(card, p);
                listEl.appendChild(card);
            });
        } else {
            listEl.innerHTML = '<div class="empty">등록한 팝업이 없습니다.</div>';
        }
    },

    addPopupCardEventListeners(card, popup) {
        card.querySelector('.btn-detail').addEventListener('click', () => {
            window.location.href = `/mypage/host/popup/${popup.id}`;
        });
        card.querySelector('.btn-manage').addEventListener('click', () => {
            window.location.href = `/mypage/host/popup/${popup.id}/reservation`;
        });
        card.querySelector('.btn-stats').addEventListener('click', () => {
            if (window.Pages && window.Pages.popupStats) {
                window.Pages.popupStats(popup.id);
            }
        });
    },

    renderReservations(reservations) {
        const listEl = document.getElementById('my-reservation-list');
        listEl.innerHTML = '';

        if (reservations && reservations.length > 0) {
            const activeReservations = reservations.filter(r => r.status !== 'CANCELLED');

            if (activeReservations.length > 0) {
                activeReservations.forEach(r => {
                    const status = r.status || '';
                    const card = document.createElement('div');
                    card.className = 'rent-card';
                    card.innerHTML = `
                        <div class="left">
                            <img src="${r.spaceImageUrl || '/img/placeholder.png'}" class="thumb" alt="공간 이미지" />
                            <div>
                                <div class="address"><strong>${r.spaceTitle || '공간명 없음'}</strong></div>
                                <div class="desc">주소 : ${r.spaceAddress || '주소 정보 없음'}</div>
                                <div class="dates">${r.startDate || ''} ~ ${r.endDate || ''}</div>
                                <span class="status-badge ${status.toLowerCase()}">${translateStatus(status)}</span>
                            </div>
                        </div>
                        <div class="right-actions">
                            <div class="action-left">
                                <button class="btn-detail" data-reservation-id="${r.id}" data-space-id="${r.spaceId}">상세보기</button>
                                <button class="btn-map" data-address="${r.spaceAddress || ''}">지도로 보기</button>
                            </div>
                            <div class="action-right">
                                <button class="btn-cancel" data-reservation-id="${r.id}">예약취소</button>
                                <button class="btn-chat" data-reservation-id="${r.id}"></button>
                            </div>
                        </div>
                    `;
                    this.addReservationCardEventListeners(card, r);
                    listEl.appendChild(card);
                });
            } else {
                listEl.innerHTML = '<div class="empty">진행 중인 예약 내역이 없습니다.</div>';
            }
        } else {
            listEl.innerHTML = '<div class="empty">예약 내역이 없습니다.</div>';
        }
    },

    addReservationCardEventListeners(card, reservation) {
        card.querySelector('.btn-detail').addEventListener('click', () => {
            if (reservation.spaceId) {
                window.location.href = `/space/detail/${reservation.spaceId}`;
            } else {
                const info = `
예약 ID: ${reservation.id}
공간명: ${reservation.spaceTitle || '공간명 없음'}
주소: ${reservation.spaceAddress || '주소 없음'}
예약 기간: ${reservation.startDate || ''} ~ ${reservation.endDate || ''}`
                    + `\n상태: ${translateStatus(reservation.status)}`;
                alert(info);
            }
        });

        card.querySelector('.btn-map').addEventListener('click', () => {
            const address = reservation.spaceAddress;
            if (address && address !== '주소 정보 없음') {
                const searchUrl = `https://map.naver.com/v5/search/${encodeURIComponent(address)}`;
                window.open(searchUrl, '_blank');
            } else {
                alert("주소 정보가 없습니다.");
            }
        });

        card.querySelector('.btn-cancel').addEventListener('click', async () => {
            if (!confirm('정말 예약을 취소하시겠습니까?')) return;
            try {
                await apiService.delete(`/space-reservations/${reservation.id}`);
                alert('예약이 취소되었습니다.');
                card.remove();
            } catch (err) {
                console.error('예약 취소 실패:', err);
                alert('예약 취소에 실패했습니다.');
            }
        });

        const btnChat = card.querySelector('.btn-chat');
        if (btnChat) {
            btnChat.addEventListener('click', () => {
                if (window.Pages && window.Pages.chat) {
                    window.Pages.chat(reservation.id);
                } else {
                    window.location.href = `/chat/${reservation.id}`;
                }
            });
        }
    }
};

document.addEventListener('DOMContentLoaded', () => {
    const btn = document.getElementById('btn-popup-register');
    if (btn) {
        btn.addEventListener('click', () => {
            window.location.href = '/mypage/host/popup/register';
        });
    }
});

window.HostPage = HostPage;