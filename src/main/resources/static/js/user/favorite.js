document.addEventListener("DOMContentLoaded", function () {
    const favoriteButtons = document.querySelectorAll(".favorite-btn");
    let favorites = JSON.parse(localStorage.getItem("favorites")) || [];

    // --- Giữ nguyên phần xử lý nút favorite ---
    favoriteButtons.forEach(btn => {
        const id = btn.dataset.id;
        const icon = btn.querySelector("i");
        if (favorites.includes(id)) {
            icon.classList.remove("fa-regular");
            icon.classList.add("fa-solid", "text-danger");
        }

        btn.addEventListener("click", () => {
            const icon = btn.querySelector("i");
            const index = favorites.indexOf(id);
            if (index >= 0) {
                favorites.splice(index, 1);
                icon.classList.remove("fa-solid", "text-danger");
                icon.classList.add("fa-regular");
                showToast("Đã xóa khỏi yêu thích 💔");
            } else {
                favorites.push(id);
                icon.classList.remove("fa-regular");
                icon.classList.add("fa-solid", "text-danger");
                showToast("Đã thêm vào yêu thích ❤️");
            }
            localStorage.setItem("favorites", JSON.stringify(favorites));
        });
    });

    // --- 🧠 PHẦN MỚI: Lọc sản phẩm yêu thích ---
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.get("sort") === "favorite") {
        document.querySelectorAll(".product-card").forEach(card => {
            const btn = card.querySelector(".favorite-btn");
            if (btn) {
                const id = btn.dataset.id;
                if (!favorites.includes(id)) {
                    card.parentElement.style.display = "none"; // ẩn col-md-4
                }
            }
        });
    }

    // Toast message
    function showToast(message) {
        let toast = document.createElement("div");
        toast.textContent = message;
        toast.style.position = "fixed";
        toast.style.bottom = "20px";
        toast.style.right = "20px";
        toast.style.background = "#333";
        toast.style.color = "#fff";
        toast.style.padding = "10px 20px";
        toast.style.borderRadius = "20px";
        toast.style.opacity = "0";
        toast.style.transition = "opacity 0.3s";
        document.body.appendChild(toast);
        setTimeout(() => (toast.style.opacity = "1"), 50);
        setTimeout(() => {
            toast.style.opacity = "0";
            setTimeout(() => toast.remove(), 500);
        }, 1500);
    }
});
