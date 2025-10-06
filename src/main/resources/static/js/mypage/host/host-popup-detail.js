const HostPopupDetailPage = {
    popupId: null,

    async init() {
        const pathParts = window.location.pathname.split("/");
        this.popupId = pathParts[pathParts.indexOf("popup") + 1];

        if (!this.popupId) {
            alert("popupId가 없습니다.");
            window.history.back();
            return;
        }

        try {
            const popup = await apiService.get(`/hosts/popups/${this.popupId}`);
            console.log("불러온 팝업 데이터:", popup);
            this.renderPopupDetail(popup);
            this.initVenueModal();
        } catch (err) {
            console.error("팝업 상세 불러오기 실패:", err);
            if (err.message?.includes("401") || err.status === 401) {
                alert("로그인이 필요합니다.");
                window.location.href = "/auth/login";
            } else {
                alert("팝업 정보를 불러올 수 없습니다.");
                window.history.back();
            }
        }
    },

    renderPopupDetail(popup) {
        const elements = {
            'popup-title': popup.title || '-',
            'popup-summary': popup.summary || '-',
            'popup-description': popup.description || '-',
            'popup-schedule': `${popup.startDate || ''} ~ ${popup.endDate || ''}`,
            'popup-venue': popup.venueName || '-',
            'popup-address': popup.venueAddress || '-',
            'popup-status': this.translateStatus(popup.status) || '-'
        };

        Object.entries(elements).forEach(([id, value]) => {
            const element = document.getElementById(id);
            if (element) element.textContent = value;
        });

        this.renderCategory(popup);
        this.renderTags(popup);

        const imageElement = document.getElementById("popup-image");
        if (imageElement) {
            if (popup.mainImageUrl) {
                imageElement.src = popup.mainImageUrl;
            } else if (popup.imageUrl) {
                imageElement.src = popup.imageUrl;
            } else {
                imageElement.style.display = 'block';
                imageElement.style.backgroundColor = '#f5f5f5';
                imageElement.style.border = '2px dashed #ddd';
                imageElement.removeAttribute('src');
            }
            imageElement.onerror = () => {
                imageElement.style.backgroundColor = '#f5f5f5';
                imageElement.style.border = '2px dashed #ddd';
                imageElement.removeAttribute('src');
            };
        }

        const editBtn = document.querySelector('.btn-edit');
        if (editBtn) {
            editBtn.onclick = () => {
                window.location.href = `/mypage/host/popup/${popup.id}/edit`;
            };
        }

        const deleteBtn = document.querySelector('.btn-delete');
        if (deleteBtn) {
            deleteBtn.onclick = async () => {
                if (!confirm("정말 삭제하시겠습니까?")) return;
                try {
                    await apiService.delete(`/hosts/popups/${popup.id}`);
                    alert("팝업이 삭제되었습니다.");
                    window.location.href = "/mypage/host";
                } catch (err) {
                    console.error("삭제 실패:", err);
                    alert("팝업 삭제에 실패했습니다.");
                }
            };
        }
    },

    renderCategory(popup) {
        const categoryElement = document.getElementById('popup-category');
        if (!categoryElement) return;

        const categoryNames = {
            1: "패션",
            2: "반려동물",
            3: "게임",
            4: "캐릭터/IP",
            5: "문화/컨텐츠",
            6: "연예",
            7: "여행/레저/스포츠"
        };

        let categoryText = '-';

        if (popup.category && popup.category.name) {
            categoryText = popup.category.name;
        } else if (popup.categoryId) {
            categoryText = categoryNames[popup.categoryId] || `카테고리 ${popup.categoryId}`;
        }

        categoryElement.textContent = categoryText;
    },

    renderTags(popup) {
        const tagsElement = document.getElementById('popup-tags');
        if (!tagsElement) return;

        const tagNames = {
            1: "할인",
            2: "한정판",
            3: "체험존",
            4: "콜라보",
            5: "신제품"
        };

        let tagsHTML = '';

        if (popup.tags && Array.isArray(popup.tags) && popup.tags.length > 0) {
            tagsHTML = popup.tags.map(tag => {
                const tagName = tag.name || tagNames[tag.id] || `태그${tag.id}`;
                return `<span class="tag-badge">${tagName}</span>`;
            }).join('');
        } else if (popup.tagIds && Array.isArray(popup.tagIds) && popup.tagIds.length > 0) {
            tagsHTML = popup.tagIds.map(tagId => {
                const tagName = tagNames[tagId] || `태그${tagId}`;
                return `<span class="tag-badge">${tagName}</span>`;
            }).join('');
        }

        if (tagsHTML) {
            tagsElement.innerHTML = tagsHTML;
        } else {
            tagsElement.textContent = '-';
        }
    },

    initVenueModal() {
        const modal = document.getElementById('venue-modal');
        const btnRegisterVenue = document.getElementById('btn-register-venue');
        const closeBtn = modal.querySelector('.close');
        const btnCancel = modal.querySelector('.btn-cancel');
        const btnSearchAddress = document.getElementById('btn-search-address');
        const venueForm = document.getElementById('venue-form');

        if (!modal || !btnRegisterVenue) return;

        // 모달 열기
        btnRegisterVenue.onclick = () => {
            modal.style.display = 'block';
        };

        // 모달 닫기
        const closeModal = () => {
            modal.style.display = 'none';
            venueForm.reset();
        };

        closeBtn.onclick = closeModal;
        btnCancel.onclick = closeModal;

        window.onclick = (e) => {
            if (e.target === modal) {
                closeModal();
            }
        };

        // 주소 검색
        btnSearchAddress.onclick = () => {
            this.searchAddress();
        };

        // 폼 제출
        venueForm.onsubmit = async (e) => {
            e.preventDefault();
            await this.submitVenue();
        };
    },

    searchAddress() {
        new daum.Postcode({
            oncomplete: (data) => {
                document.getElementById('venue-road-address').value = data.roadAddress || '';
                document.getElementById('venue-jibun-address').value = data.jibunAddress || '';

                // 좌표 변환
                const geocoder = new kakao.maps.services.Geocoder();
                geocoder.addressSearch(data.roadAddress || data.jibunAddress, (result, status) => {
                    if (status === kakao.maps.services.Status.OK) {
                        document.getElementById('venue-latitude').value = result[0].y;
                        document.getElementById('venue-longitude').value = result[0].x;
                        console.log('좌표 설정 완료:', result[0].y, result[0].x);
                    }
                });
            }
        }).open();
    },

    async submitVenue() {
        const venueData = {
            name: document.getElementById('venue-name').value.trim(),
            roadAddress: document.getElementById('venue-road-address').value.trim(),
            jibunAddress: document.getElementById('venue-jibun-address').value.trim(),
            detailAddress: document.getElementById('venue-detail-address').value.trim(),
            latitude: parseFloat(document.getElementById('venue-latitude').value) || null,
            longitude: parseFloat(document.getElementById('venue-longitude').value) || null,
            parkingAvailable: document.getElementById('venue-parking').checked
        };

        if (!venueData.name || !venueData.roadAddress) {
            alert('장소명과 주소를 입력해 주세요.');
            return;
        }

        try {
            await apiService.post(`/hosts/popups/${this.popupId}/venue`, venueData);
            alert('장소가 등록되었습니다.');
            document.getElementById('venue-modal').style.display = 'none';
            location.reload();
        } catch (err) {
            console.error('장소 등록 실패:', err);
            alert('장소 등록에 실패했습니다.');
        }
    },

    translateStatus(status) {
        switch (status) {
            case 'PLANNED': return '준비 중';
            case 'ONGOING': return '진행 중';
            case 'FINISHED': return '종료됨';
            case 'CANCELLED': return '취소됨';
            case 'PENDING': return '승인 대기';
            case 'ACCEPTED': return '승인됨';
            case 'REJECTED': return '거절됨';
            default: return status || '-';
        }
    }
};

document.addEventListener("DOMContentLoaded", () => {
    HostPopupDetailPage.init();
});

window.HostPopupDetailPage = HostPopupDetailPage;