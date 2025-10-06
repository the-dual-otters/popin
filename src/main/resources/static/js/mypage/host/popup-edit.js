document.addEventListener('DOMContentLoaded', async () => {
    await loadComponents();
    initializeLayout();

    const pathParts = window.location.pathname.split("/");
    const popupId = pathParts[pathParts.indexOf("popup") + 1];

    if (!popupId) {
        alert("잘못된 접근입니다.");
        history.back();
        return;
    }

    const form = document.getElementById('popup-edit-form');
    const selectedTags = new Set();

    // 태그 버튼 토글
    document.querySelectorAll(".tag-btn").forEach(btn => {
        btn.addEventListener("click", () => {
            const tagId = parseInt(btn.dataset.id, 10);
            if (selectedTags.has(tagId)) {
                selectedTags.delete(tagId);
                btn.classList.remove("selected");
            } else {
                selectedTags.add(tagId);
                btn.classList.add("selected");
            }
        });
    });

    setupImagePreview();

    try {
        // 기존 팝업 데이터 불러오기
        const popup = await apiService.get(`/hosts/popups/${popupId}`);

        form.title.value = popup.title || "";
        form.summary.value = popup.summary || "";
        form.description.value = popup.description || "";
        form.startDate.value = popup.startDate || "";
        form.endDate.value = popup.endDate || "";
        form.entryFee.value = popup.entryFee ?? 0;
        form.reservationAvailable.checked = !!popup.reservationAvailable;
        form.reservationLink.value = popup.reservationLink || "";
        form.waitlistAvailable.checked = !!popup.waitlistAvailable;
        form.notice.value = popup.notice || "";
        form.isFeatured.checked = !!popup.isFeatured;

        if (popup.mainImageUrl) {
            const preview = document.getElementById("mainImagePreview");
            const img = document.createElement("img");
            img.src = popup.mainImageUrl;
            img.style.width = '100px';
            img.style.height = '100px';
            img.style.objectFit = 'cover';
            img.style.borderRadius = '8px';
            img.style.border = '1px solid #e5e7eb';
            preview.appendChild(img);
            document.getElementById("mainImageUrl").value = popup.mainImageUrl;
        }

        if (popup.categoryId) {
            const categorySelect = form.categoryId;
            categorySelect.value = popup.categoryId;

            Array.from(categorySelect.options).forEach(option => {
                if (parseInt(option.value) === popup.categoryId) {
                    option.selected = true;
                }
            });
        }

        if (popup.tagIds && Array.isArray(popup.tagIds) && popup.tagIds.length > 0) {
            popup.tagIds.forEach(tagId => {
                selectedTags.add(tagId);
                const btn = document.querySelector(`.tag-btn[data-id='${tagId}']`);
                if (btn) btn.classList.add("selected");
            });
        }

        // 운영시간 초기값 세팅
        if (popup.hours && popup.hours.length > 0) {
            popup.hours.forEach(hour => {
                addHourItem(hour);
            });
        }

    } catch (err) {
        console.error('팝업 상세 불러오기 실패:', err);
        alert('데이터 로딩 실패');
    }

    // 저장 이벤트
    form.addEventListener('submit', async (e) => {
        e.preventDefault();

        try {
            const formData = await collectFormData();
            formData.tagIds = Array.from(selectedTags);

            await apiService.put(`/hosts/popups/${popupId}`, formData);

            alert('팝업이 수정되었습니다.');
            window.location.href = '/mypage/host';
        } catch (err) {
            console.error('팝업 수정 실패:', err);
            alert('수정 실패');
        }
    });

    // 운영시간 추가 버튼 이벤트
    const addHourBtn = document.getElementById("add-hour-btn");
    if (addHourBtn) {
        addHourBtn.addEventListener("click", () => addHourItem());
    }
});

/* ====== 재사용 함수들 ====== */

function addHourItem(hour) {
    const container = document.getElementById("hours-container");
    const item = document.createElement("div");
    item.className = "hour-item";

    // 요일 버튼
    const days = ["월","화","수","목","금","토","일"];
    const dayGroup = document.createElement("div");
    dayGroup.className = "day-buttons";

    days.forEach((day, i) => {
        const btn = document.createElement("button");
        btn.type = "button";
        btn.className = "day-btn";
        btn.textContent = day;
        btn.dataset.value = i;
        if (hour && hour.dayOfWeek === i) btn.classList.add("active");
        btn.addEventListener("click", () => btn.classList.toggle("active"));
        dayGroup.appendChild(btn);
    });

    // 시간 입력
    const timeInputs = document.createElement("div");
    timeInputs.className = "time-inputs";
    timeInputs.innerHTML = `
        <input type="time" class="open-time" required value="${hour?.openTime || ''}">
        <span> ~ </span>
        <input type="time" class="close-time" required value="${hour?.closeTime || ''}">
    `;

    // 삭제 버튼
    const removeBtn = document.createElement("button");
    removeBtn.type = "button";
    removeBtn.textContent = "삭제";
    removeBtn.className = "remove-btn";
    removeBtn.addEventListener("click", () => item.remove());

    // 등록 페이지랑 동일한 순서로 append
    item.appendChild(dayGroup);
    item.appendChild(timeInputs);
    item.appendChild(removeBtn);

    container.appendChild(item);
}


function setupImagePreview() {
    const mainImageFile = document.getElementById('mainImageFile');
    const mainImagePreview = document.getElementById('mainImagePreview');
    if (mainImageFile) {
        mainImageFile.addEventListener('change', e => handleImagePreview(e.target.files, mainImagePreview, true));
    }

    const extraImageFiles = document.getElementById('extraImageFiles');
    const extraImagePreview = document.getElementById('extraImagePreview');
    if (extraImageFiles) {
        extraImageFiles.addEventListener('change', e => handleImagePreview(e.target.files, extraImagePreview, false));
    }
}

function handleImagePreview(files, container, isSingle = false) {
    if (isSingle) container.innerHTML = '';
    Array.from(files).forEach(file => {
        if (file.type.startsWith('image/')) {
            const reader = new FileReader();
            reader.onload = e => {
                const img = document.createElement('img');
                img.src = e.target.result;
                img.style.width = '100px';
                img.style.height = '100px';
                img.style.objectFit = 'cover';
                img.style.borderRadius = '8px';
                img.style.border = '1px solid #e5e7eb';
                container.appendChild(img);
            };
            reader.readAsDataURL(file);
        }
    });
}

async function uploadImage(file) {
    if (!file) return '';
    const formData = new FormData();
    formData.append('image', file);

    try {
        const response = await fetch('/api/hosts/upload/image', {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${apiService.getStoredToken()}` },
            body: formData
        });
        if (!response.ok) throw new Error('이미지 업로드 실패');
        const result = await response.json();
        return result.imageUrl;
    } catch (error) {
        console.error('이미지 업로드 오류:', error);
        throw error;
    }
}

async function collectFormData() {
    const hours = [];
    document.querySelectorAll(".hour-item").forEach(item => {
        const openTime = item.querySelector(".open-time")?.value;
        const closeTime = item.querySelector(".close-time")?.value;
        if (openTime && closeTime) {
            item.querySelectorAll(".day-btn.active").forEach(btn => {
                hours.push({
                    dayOfWeek: parseInt(btn.dataset.value),
                    openTime,
                    closeTime
                });
            });
        }
    });

    let mainImageUrl = document.getElementById('mainImageUrl')?.value || '';
    const mainImageFile = document.getElementById('mainImageFile')?.files[0];
    if (mainImageFile) {
        try {
            mainImageUrl = await uploadImage(mainImageFile);
        } catch (error) {
            console.error('대표 이미지 업로드 실패:', error);
            alert('대표 이미지 업로드에 실패했습니다.');
        }
    }

    let imageUrls = [];
    const extraImageFiles = document.getElementById('extraImageFiles')?.files;
    if (extraImageFiles && extraImageFiles.length > 0) {
        try {
            for (let i = 0; i < extraImageFiles.length; i++) {
                const file = extraImageFiles[i];
                const uploadedUrl = await uploadImage(file);
                if (uploadedUrl) imageUrls.push(uploadedUrl);
            }
        } catch (error) {
            console.error('추가 이미지 업로드 실패:', error);
            alert('일부 추가 이미지 업로드에 실패했습니다.');
        }
    }

    return {
        title: document.getElementById("title")?.value?.trim() || '',
        summary: document.getElementById("summary")?.value?.trim() || '',
        description: document.getElementById("description")?.value?.trim() || '',
        startDate: document.getElementById("startDate")?.value || '',
        endDate: document.getElementById("endDate")?.value || '',
        entryFee: parseInt(document.getElementById("entryFee")?.value) || 0,
        reservationAvailable: document.getElementById("reservationAvailable")?.checked || false,
        reservationLink: document.getElementById("reservationLink")?.value?.trim() || '',
        waitlistAvailable: document.getElementById("waitlistAvailable")?.checked || false,
        notice: document.getElementById("notice")?.value?.trim() || '',
        mainImageUrl: mainImageUrl,
        isFeatured: document.getElementById("isFeatured")?.checked || false,
        imageUrls: imageUrls,
        hours: hours,
        categoryId: parseInt(document.getElementById("categoryId")?.value) || null
    };
}
