document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll('.btn-decrease').forEach(btn => {
        btn.addEventListener('click', e => {
            const input = e.target.closest('.input-group').querySelector('input[type="number"]');
            if (input && input.value > 1) input.stepDown();
        });
    });

    document.querySelectorAll('.btn-increase').forEach(btn => {
        btn.addEventListener('click', e => {
            const input = e.target.closest('.input-group').querySelector('input[type="number"]');
            if (input) input.stepUp();
        });
    });
});
