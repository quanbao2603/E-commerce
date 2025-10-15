// chat.js

async function startChat(btn) {
    const vendorId = btn.dataset.vendorId;
    if (!vendorId) return;

    // Kiểm tra xem user đã login chưa (client-side)
    const currentUserId = document.getElementById('currentUserId')?.value;
    if (!currentUserId || currentUserId.trim() === "") {
        // Chưa login → chuyển hướng sang login page
        window.location.href = `/auth/login`;
        return;
    }

    // Lấy CSRF token từ meta
    const csrfToken  = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

    try {
        const res = await fetch(`/api/chat/start?vendorId=${vendorId}`, {
            method: 'POST',
            credentials: 'include', // gửi cookie session
            headers: {
                'Content-Type': 'application/json',
                ...(csrfToken && csrfHeader ? { [csrfHeader]: csrfToken } : {})
            }
        });

        if (res.status === 403) {
            // 403 Forbidden → coi như chưa login, redirect
            window.location.href = `/auth/login`;
            return;
        }

        if (!res.ok) {
            const err = await res.json().catch(() => ({}));
            alert(err.error || 'Cannot start chat');
            return;
        }

        const data = await res.json();
        const chatId = data.chatId;
        if (!chatId) return;

        // Lưu chatId tạm thời
        sessionStorage.setItem('currentChatId', chatId);

        // Chuyển sang trang chat
        window.location.href = `/user/chat?chatId=${chatId}`;

    } catch (error) {
        console.error(error);
        alert('Error starting chat');
    }
}

// Thêm event listener cho các button chat
document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('button[data-vendor-id]').forEach(btn => {
        btn.addEventListener('click', () => startChat(btn));
    });
});
