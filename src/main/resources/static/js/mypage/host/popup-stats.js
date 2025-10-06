document.addEventListener("DOMContentLoaded", async () => {
    const pathParts = window.location.pathname.split("/");
    const popupId = pathParts[4]; // /mypage/host/popup/{id}/stats → index 4가 {id}

    try {
        const data = await apiService.getPopupStats(popupId);

        if (!data || data.length === 0) {
            console.log("데이터가 없습니다.");
            return;
        }

        renderVisitorTrend(data);
        renderHourlyStats(data);
        renderReservationStats(data);
        renderMissionStats(data);
    } catch (err) {
        console.error("통계 데이터 로드 실패:", err);
        alert("통계를 불러오는 중 오류가 발생했습니다.");
    }
});

function renderVisitorTrend(data) {
    const loading = document.getElementById('visitorTrend-loading');
    const canvas = document.getElementById('visitorTrend');
    if (loading) loading.style.display = 'none';
    if (canvas) canvas.style.display = 'block';

    const dailyData = data.reduce((acc, item) => {
        const date = item.date;
        if (!acc[date]) acc[date] = 0;
        acc[date] += item.visitorCount || 0;
        return acc;
    }, {});

    const labels = Object.keys(dailyData).sort();
    const counts = labels.map(date => dailyData[date]);

    new Chart(canvas, {
        type: "line",
        data: {
            labels,
            datasets: [{
                label: "방문자 수",
                data: counts,
                borderColor: "#2563eb",
                backgroundColor: "rgba(37,99,235,0.2)",
                fill: true,
                tension: 0.4
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: false },
                tooltip: {
                    callbacks: {
                        title: (items) => `날짜: ${items[0].label}`,
                        label: (item) => `방문자 수: ${item.formattedValue} 명`
                    }
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    ticks: {
                        stepSize: 1,
                        callback: (value) => Number.isInteger(value) ? value : null
                    }
                }
            }
        }
    });
}

function renderHourlyStats(data) {
    const loading = document.getElementById('hourlyStats-loading');
    const canvas = document.getElementById('hourlyStats');
    if (loading) loading.style.display = 'none';
    if (canvas) canvas.style.display = 'block';

    const hourlyData = data.filter(d => d.hour !== null && d.hour !== undefined);

    if (hourlyData.length === 0) {
        canvas.style.display = 'none';
        const message = document.createElement('div');
        message.style.cssText = 'text-align:center; color:#666; padding:40px; font-size:14px;';
        message.textContent = '시간대별 데이터가 없습니다.';
        canvas.parentNode.appendChild(message);
        return;
    }

    const hourlyGroup = hourlyData.reduce((acc, item) => {
        const hour = item.hour;
        if (!acc[hour]) acc[hour] = 0;
        acc[hour] += item.visitorCount || 0;
        return acc;
    }, {});

    const hours = Object.keys(hourlyGroup).sort((a, b) => parseInt(a) - parseInt(b));
    const labels = hours.map(h => `${h}시`);
    const counts = hours.map(h => hourlyGroup[h]);

    new Chart(canvas, {
        type: "bar",
        data: {
            labels,
            datasets: [{
                label: "시간대별 방문자",
                data: counts,
                backgroundColor: ["#2563eb", "#10b981", "#f59e0b", "#a855f7", "#ef4444", "#06b6d4"],
                borderWidth: 2,
                borderRadius: 8,
                borderSkipped: false
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: false },
                tooltip: {
                    callbacks: {
                        title: (items) => `시간대: ${items[0].label}`,
                        label: (item) => `방문자 수: ${item.formattedValue} 명`
                    }
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    ticks: {
                        stepSize: 1,
                        callback: (value) => Number.isInteger(value) ? value : null
                    },
                    grid: { color: 'rgba(0,0,0,0.05)' }
                },
                x: { grid: { display: false } }
            }
        }
    });
}

function renderReservationStats(data) {
    const loading = document.getElementById('reservationStats-loading');
    const canvas = document.getElementById('reservationStats');
    if (loading) loading.style.display = 'none';
    if (canvas) canvas.style.display = 'block';

    const confirmed = data.reduce((sum, d) => sum + (d.reservationCount || 0), 0);
    const canceled = data.reduce((sum, d) => sum + (d.canceledCount || 0), 0);

    if (confirmed === 0 && canceled === 0) {
        canvas.style.display = 'none';
        const message = document.createElement('div');
        message.style.cssText = 'text-align:center; color:#666; padding:40px; font-size:14px;';
        message.textContent = '예약 데이터가 없습니다.';
        canvas.parentNode.appendChild(message);
        return;
    }

    const chartData = [];
    const chartLabels = [];
    const chartColors = [];

    if (confirmed > 0) {
        chartData.push(confirmed);
        chartLabels.push(`예약 완료`);
        chartColors.push("#3b82f6");
    }

    if (canceled > 0) {
        chartData.push(canceled);
        chartLabels.push(`취소`);
        chartColors.push("#ef4444");
    }

    new Chart(canvas, {
        type: "doughnut",
        data: {
            labels: chartLabels,
            datasets: [{
                data: chartData,
                backgroundColor: chartColors
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: false },
                tooltip: {
                    displayColors: false,
                    callbacks: {
                        title: (items) => '예약 현황',
                        label: (item) => `${chartLabels[item.dataIndex]}: ${item.formattedValue} 건`
                    }
                }
            }
        }
    });
}

function renderMissionStats(data) {
    const loading = document.getElementById('missionStats-loading');
    const canvas = document.getElementById('missionStats');
    if (loading) loading.style.display = 'none';
    if (canvas) canvas.style.display = 'block';

    const missionData = data.reduce((acc, item) => {
        const date = item.date;
        if (!acc[date]) acc[date] = 0;
        acc[date] += item.missionCompletedCount || 0;
        return acc;
    }, {});

    const labels = Object.keys(missionData).sort();
    const counts = labels.map(date => missionData[date]);

    if (labels.length === 0 || counts.every(count => count === 0)) {
        canvas.style.display = 'none';
        const message = document.createElement('div');
        message.style.cssText = 'text-align:center; color:#666; padding:40px; font-size:14px;';
        message.textContent = '미션 데이터가 없습니다.';
        canvas.parentNode.appendChild(message);
        return;
    }

    new Chart(canvas, {
        type: "bar",
        data: {
            labels,
            datasets: [{
                label: "미션 수행자",
                data: counts,
                backgroundColor: "#f59e0b"
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: false },
                tooltip: {
                    callbacks: {
                        title: (items) => `날짜: ${items[0].label}`,
                        label: (item) => `미션 수행자: ${item.formattedValue} 명`
                    }
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    ticks: {
                        stepSize: 1,
                        callback: (value) => Number.isInteger(value) ? value : null
                    }
                }
            }
        }
    });
}
