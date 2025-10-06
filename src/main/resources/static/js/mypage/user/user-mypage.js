document.addEventListener('DOMContentLoaded', async function () {
    await loadComponents();   // header/footer 로드
    initializeLayout();

    loadNotificationSettings();
    setupNotificationSettingEvents();

    try {
        // =============================
        // 사용자 정보 불러오기
        // =============================
        const user = await apiService.getCurrentUser();

        const setText = (id, val) => {
            const el = document.getElementById(id);
            if (el) el.textContent = val ?? '-';
        };
        setText('user-name', user.name);
        setText('user-nickname', user.nickname);
        setText('user-email', user.email);
        setText('user-phone', user.phone);

        // =============================
        // 사용자 정보 수정 버튼 이벤트
        // =============================
        document.querySelectorAll('.edit-btn').forEach((btn, idx) => {
            btn.addEventListener('click', () => {
                let field, label, spanEl;
                switch (idx) {
                    case 0:
                        field = 'name';
                        label = '이름';
                        spanEl = document.getElementById('user-name');
                        break;
                    case 1:
                        field = 'nickname';
                        label = '닉네임';
                        spanEl = document.getElementById('user-nickname');
                        break;
                    case 2:
                        field = 'phone';
                        label = '연락처';
                        spanEl = document.getElementById('user-phone');
                        break;
                }
                if (!spanEl) return;

                const currentValue = spanEl.textContent;
                const input = document.createElement('input');
                input.type = 'text';
                input.value = currentValue;
                input.className = 'inline-edit';

                const saveBtn = document.createElement('button');
                saveBtn.textContent = '저장';
                saveBtn.className = 'save-btn';

                const cancelBtn = document.createElement('button');
                cancelBtn.textContent = '취소';
                cancelBtn.className = 'cancel-btn';

                spanEl.replaceWith(input);
                btn.replaceWith(saveBtn);
                saveBtn.after(cancelBtn);

                cancelBtn.addEventListener('click', () => {
                    input.replaceWith(spanEl);
                    saveBtn.replaceWith(btn);
                    cancelBtn.remove();
                });

                saveBtn.addEventListener('click', async () => {
                    const newValue = input.value.trim();
                    if (!newValue || newValue === currentValue) {
                        cancelBtn.click();
                        return;
                    }

                    try {
                        // 닉네임 중복 확인 추가
                        if (field === 'nickname') {
                            const res = await apiService.checkNicknameDuplicate(newValue);
                            if (res.exists) {
                                alert('이미 사용 중인 닉네임입니다. 다른 닉네임을 입력해주세요.');
                                return;
                            }
                        }

                        const updatedUser = await apiService.put('/users/me', {
                            name: field === 'name' ? newValue : user.name,
                            nickname: field === 'nickname' ? newValue : user.nickname,
                            phone: field === 'phone' ? newValue : user.phone
                        });

                        user.name = updatedUser.name;
                        user.nickname = updatedUser.nickname;
                        user.phone = updatedUser.phone;

                        spanEl.textContent = updatedUser[field] || '-';
                        input.replaceWith(spanEl);
                        saveBtn.replaceWith(btn);
                        cancelBtn.remove();

                        alert(`${label}이(가) 수정되었습니다.`);
                    } catch (err) {
                        console.error(err);
                        alert(`${label} 수정 실패: ${err.message || err}`);
                    }
                });
            });
        });


        // =============================
        // 관심 카테고리 (API 연동)
        // =============================
        const container = document.getElementById('category-container');
        if (container) {
            try {
                // 전체 카테고리 가져오기
                const allCategories = await apiService.get('/categories');
                // 사용자 관심 카테고리 가져오기
                const myCategories = await apiService.get('/categories/me');
                const myIds = new Set(myCategories.map(c => c.id));

                allCategories.forEach(cat => {
                    const btn = document.createElement('button');
                    btn.textContent = cat.name;
                    btn.className = 'category-btn';
                    if (myIds.has(cat.id)) btn.classList.add('active');

                    btn.addEventListener('click', async () => {
                        btn.classList.toggle('active');
                        try {
                            // 서버 업데이트
                            const selected = Array.from(container.querySelectorAll('.category-btn.active'))
                                .map(b => b.textContent);
                            await apiService.put('/categories/me', selected);
                        } catch (err) {
                            console.error(err);
                            alert('카테고리 업데이트 실패');
                        }
                    });

                    container.appendChild(btn);
                });
            } catch (e) {
                console.error('카테고리 로드 실패:', e);
                container.innerHTML = `<p style="color:#777;">카테고리를 불러올 수 없습니다.</p>`;
            }
        }

        // =============================
        // 팝업 제보 모달
        // =============================
        const reportLink = Array.from(document.querySelectorAll('.menu-list li a'))
            .find(a => a.textContent.includes('팝업 제보하기'));

        if (reportLink) {
            reportLink.addEventListener('click', e => {
                e.preventDefault();

                const backdrop = document.createElement('div');
                backdrop.className = 'modal-backdrop';
                backdrop.style.cssText = `
            position:fixed;
            top:0;left:0;right:0;bottom:0;
            background:rgba(0,0,0,0.5);
            display:flex;
            justify-content:center;
            align-items:center;
            z-index:1000;
        `;

                backdrop.innerHTML = `
          <div class="modal-card" style="background:#fff; padding:20px; border-radius:30px; text-align:center; position:relative; width:360px; max-width:90%;">
            <button class="modal-close" id="close-btn" style="position:absolute; top:12px; right:16px; font-size:24px; border:none; background:none; cursor:pointer;">&times;</button>
            <h2 style="margin-bottom:16px; font-size:20px;">팝업 제보하기</h2>
            <form id="popup-report-form" style="display:flex; flex-direction:column; gap:12px; text-align:left;">
              <label>브랜드명<input type="text" name="brandName" class="form-input"></label>
              <label>팝업명*<input type="text" name="popupName" required class="form-input"></label>
              <label>주소*<input type="text" name="address" required class="form-input"></label>
              <label>시작일<input type="date" name="startDate" class="form-input"></label>
              <label>종료일<input type="date" name="endDate" class="form-input"></label>
              <label>추가 정보<textarea name="extraInfo" rows="3" class="form-input"></textarea></label>
              <label>이미지 업로드<input type="file" name="images" accept="image/*" multiple style="width:100%; padding:8px;"></label>
              <button type="submit" class="button button-primary">제출</button>
            </form>
          </div>
        `;

                document.body.appendChild(backdrop);

                const closeBtn = backdrop.querySelector('#close-btn');
                closeBtn.addEventListener('click', () => backdrop.remove());
                backdrop.addEventListener('click', e => {
                    if (e.target === backdrop) backdrop.remove();
                });

                const form = backdrop.querySelector('#popup-report-form');

                form.addEventListener('submit', async e => {
                    e.preventDefault();

                    const raw = Object.fromEntries(new FormData(form).entries());

                    // 유효성 검사
                    if (!raw.popupName?.trim()) {
                        alert("팝업명은 필수 입력 항목입니다.");
                        return;
                    }
                    if (!raw.address?.trim()) {
                        alert("주소는 필수 입력 항목입니다.");
                        return;
                    }

                    if (raw.startDate && raw.endDate) {
                        const start = new Date(raw.startDate);
                        const end = new Date(raw.endDate);
                        if (end < start) {
                            alert("종료일은 시작일 이후여야 합니다.");
                            return;
                        }
                    }

                    const data = {
                        brandName: raw.brandName?.trim() || null,
                        popupName: raw.popupName?.trim(),
                        address: raw.address?.trim(),
                        startDate: raw.startDate || null,
                        endDate: raw.endDate || null,
                        extraInfo: raw.extraInfo?.trim() || null
                    };

                    const formData = new FormData();
                    formData.append("data", new Blob([JSON.stringify(data)], { type: "application/json" }));

                    const files = form.querySelector('input[name="images"]').files;
                    for (const file of files) {
                        formData.append("images", file);
                    }

                    try {
                        await apiService.createPopupReport(formData);
                        alert('팝업 제보가 등록되었습니다.');
                        backdrop.remove();
                    } catch (err) {
                        console.error('팝업 제보 오류:', err);
                        alert('팝업 제보 실패: ' + (err.message || err));
                    }
                });

            });
        }

    } catch (err) {
        console.error(err);
        const mc = document.getElementById('main-content') || document.querySelector('.main-content');
        if (mc) mc.innerHTML = `
            <p style="color:red; text-align:center;">로그인 후 이용 가능합니다.</p>
        `;
    }
});


// 알림 설정 불러오기
async function loadNotificationSettings() {
    try {
        const settings = await apiService.getNotificationSettings();
        // 예: { reservationEnabled: true, systemEnabled: false, eventEnabled: true }

        document.getElementById("reservation-toggle").checked = settings.reservationEnabled;
        document.getElementById("system-toggle").checked = settings.systemEnabled;
        document.getElementById("event-toggle").checked = settings.eventEnabled;
    } catch (err) {
        console.error("알림 설정 불러오기 실패:", err);
    }
}

// 이벤트 등록
function setupNotificationSettingEvents() {
    document.getElementById("reservation-toggle").addEventListener("change", (e) => {
        apiService.updateNotificationSetting("RESERVATION", e.target.checked);
    });
    document.getElementById("system-toggle").addEventListener("change", (e) => {
        apiService.updateNotificationSetting("SYSTEM", e.target.checked);
    });
    document.getElementById("event-toggle").addEventListener("change", (e) => {
        apiService.updateNotificationSetting("EVENT", e.target.checked);
    });
}

document.addEventListener("DOMContentLoaded", () => {
    loadNotificationSettings();
    setupNotificationSettingEvents();
});


