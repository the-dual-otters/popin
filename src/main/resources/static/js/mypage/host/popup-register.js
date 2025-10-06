document.addEventListener('DOMContentLoaded', function() {
    const addHourBtn = document.getElementById("add-hour-btn");
    if (addHourBtn) {
        addHourBtn.addEventListener("click", addHourItem);
    }

    const form = document.getElementById("popup-register-form");
    if (form) {
        form.addEventListener("submit", handleFormSubmit);
    }

    setupImagePreview();
    setupTagSelection();
});

function setupTagSelection() {
    const selectedTags = new Set();
    const tagButtons = document.querySelectorAll(".tag-btn");

    tagButtons.forEach(btn => {
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

    window.getSelectedTags = () => Array.from(selectedTags);
}

function addHourItem() {
    const container = document.getElementById("hours-container");
    const item = document.createElement("div");
    item.className = "hour-item";

    const days = ["월","화","수","목","금","토","일"];
    const dayButtons = days.map((day, i) => {
        const btn = document.createElement("button");
        btn.type = "button";
        btn.className = "day-btn";
        btn.textContent = day;
        btn.dataset.value = i;
        btn.addEventListener("click", () => btn.classList.toggle("active"));
        return btn;
    });

    const dayGroup = document.createElement("div");
    dayGroup.className = "day-buttons";
    dayButtons.forEach(b => dayGroup.appendChild(b));

    const timeInputs = document.createElement("div");
    timeInputs.className = "time-inputs";
    timeInputs.innerHTML = `
        <input type="time" class="open-time" required>
        <span> ~ </span>
        <input type="time" class="close-time" required>
    `;

    const removeBtn = document.createElement("button");
    removeBtn.type = "button";
    removeBtn.textContent = "삭제";
    removeBtn.className = "remove-btn";
    removeBtn.addEventListener("click", () => item.remove());

    item.append(dayGroup, timeInputs, removeBtn);
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

async function handleFormSubmit(e) {
    e.preventDefault();
    try {
        if (!validateForm()) return;

        const submitBtn = document.querySelector('.btn-primary');
        const originalText = submitBtn.textContent;
        submitBtn.textContent = '등록 중...';
        submitBtn.disabled = true;

        const formData = await collectFormData();
        console.log('전송할 데이터:', formData);

        const response = await apiService.createPopup(formData);

        alert("팝업이 성공적으로 등록되었습니다!");

        if (response && response.id) {
            window.location.href = `/mypage/host`;
        } else {
            window.location.href = "/mypage/host";
        }
    } catch (err) {
        console.error("팝업 등록 실패:", err);
        let errorMessage = "팝업 등록에 실패했습니다.";
        if (err.message) {
            if (err.message.includes('401')) errorMessage = "로그인이 필요합니다.";
            else if (err.message.includes('422') || err.message.includes('400')) errorMessage = "입력 정보를 확인해주세요.";
        }
        alert(errorMessage);
    } finally {
        const submitBtn = document.querySelector('.btn-primary');
        submitBtn.textContent = '등록하기';
        submitBtn.disabled = false;
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

    let mainImageUrl = '';
    const mainImageFile = document.getElementById('mainImageFile')?.files[0];
    if (mainImageFile) {
        try {
            mainImageUrl = await uploadImage(mainImageFile);
            console.log('대표 이미지 업로드 성공:', mainImageUrl);
        } catch (error) {
            console.error('대표 이미지 업로드 실패:', error);
            alert('대표 이미지 업로드에 실패했습니다. 계속 진행하시겠습니까?');
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
            console.log('추가 이미지 업로드 성공:', imageUrls);
        } catch (error) {
            console.error('추가 이미지 업로드 실패:', error);
            alert('일부 추가 이미지 업로드에 실패했습니다.');
        }
    }

    const formData = {
        venueId: null,
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
        isFeatured: false,
        imageUrls: imageUrls,
        hours: hours,
        categoryId: parseInt(document.getElementById("categoryId")?.value) || null,
        tagIds: window.getSelectedTags ? window.getSelectedTags() : []
    };

    return formData;
}

function validateForm() {
    const title = document.getElementById("title")?.value?.trim();
    if (!title) {
        alert("제목을 입력해주세요.");
        document.getElementById("title")?.focus();
        return false;
    }

    const startDate = document.getElementById("startDate")?.value;
    if (!startDate) {
        alert("시작일을 선택해주세요.");
        document.getElementById("startDate")?.focus();
        return false;
    }

    const endDate = document.getElementById("endDate")?.value;
    if (!endDate) {
        alert("종료일을 선택해주세요.");
        document.getElementById("endDate")?.focus();
        return false;
    }

    if (startDate && endDate && new Date(endDate) < new Date(startDate)) {
        alert("종료일은 시작일 이후여야 합니다.");
        document.getElementById("endDate")?.focus();
        return false;
    }

    const reservationAvailable = document.getElementById("reservationAvailable")?.checked;
    const reservationLink = document.getElementById("reservationLink")?.value?.trim();
    if (reservationAvailable && !reservationLink) {
        alert("예약이 가능한 경우 예약 링크를 입력해주세요.");
        document.getElementById("reservationLink")?.focus();
        return false;
    }

    return true;
}

function goBack() {
    if (confirm("작성 중인 내용이 사라집니다. 정말 취소하시겠습니까?")) {
        window.history.back();
    }
}
