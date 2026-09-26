(() => {
    const toast = document.getElementById('cart-feedback');
    let dismissTimer;

    function showToast(message, isError) {
        if (!toast) return;
        clearTimeout(dismissTimer);
        toast.textContent = message;
        toast.classList.toggle('toast-error', isError);
        toast.setAttribute('role', isError ? 'alert' : 'status');
        toast.hidden = false;
        dismissTimer = setTimeout(() => { toast.hidden = true; }, 3500);
    }

    document.querySelectorAll('form[data-add-to-cart]').forEach((form) => {
        form.addEventListener('submit', async (event) => {
            event.preventDefault();
            const button = form.querySelector('button[type="submit"]');
            button.disabled = true;

            try {
                const response = await fetch(form.dataset.ajaxAction, {
                    method: 'POST',
                    credentials: 'same-origin',
                    headers: {
                        'Accept': 'application/json',
                        'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'
                    },
                    body: new URLSearchParams(new FormData(form))
                });
                if (!response.ok) throw new Error(`Cart request failed: ${response.status}`);

                const result = await response.json();
                document.querySelectorAll('[data-cart-count]').forEach((badge) => {
                    badge.textContent = result.count;
                });
                showToast(toast?.dataset.success || 'Added to cart', false);
            } catch (error) {
                showToast(toast?.dataset.error || 'Could not add this item. Please try again.', true);
            } finally {
                button.disabled = false;
            }
        });
    });

    document.querySelectorAll('form[data-checkout-form]').forEach((form) => {
        const button = form.querySelector('button[type="submit"]');
        const label = button.querySelector('.button-label');
        const spinner = button.querySelector('.button-spinner');
        const idleText = label.textContent;

        function setLoading(loading) {
            form.dataset.submitting = loading ? 'true' : '';
            button.disabled = loading;
            button.setAttribute('aria-busy', loading ? 'true' : 'false');
            spinner.hidden = !loading;
            label.textContent = loading ? (form.dataset.loadingText || idleText) : idleText;
        }

        form.addEventListener('submit', (event) => {
            if (form.dataset.submitting) {
                event.preventDefault(); // already submitting: ignore repeated clicks / Enter presses
                return;
            }
            setLoading(true);
        });

        // Back/forward navigation can restore this page from the bfcache with the button still disabled.
        window.addEventListener('pageshow', (event) => {
            if (event.persisted) setLoading(false);
        });
    });
})();
