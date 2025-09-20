document.addEventListener('DOMContentLoaded', function () {
    // Hover cho dropdown cấp 1
    document.querySelectorAll('.dropdown').forEach(function (dropdown) {
        dropdown.addEventListener('mouseenter', function () {
            this.querySelector('.dropdown-menu').classList.add('show');
        });
        dropdown.addEventListener('mouseleave', function () {
            this.querySelector('.dropdown-menu').classList.remove('show');
        });
    });

    // Hover cho dropdown-submenu cấp 2 và 3
    document.querySelectorAll('.dropdown-submenu').forEach(function (submenu) {
        submenu.addEventListener('mouseenter', function () {
            this.querySelector('.dropdown-menu').classList.add('show');
        });
        submenu.addEventListener('mouseleave', function () {
            this.querySelector('.dropdown-menu').classList.remove('show');
        });
    });
});