document.addEventListener("DOMContentLoaded", function() {
    document.querySelectorAll('.toggle-password').forEach(function(button) {
        button.addEventListener('click', function() {
            var targetId = this.getAttribute('data-target');
            var input = document.getElementById(targetId);
            var icon = this.querySelector('.eye-icon');

            if (input && input.type === 'password') {
                input.type = 'text';
                input.classList.remove('tracking-widest');
                icon.innerHTML = '<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21" />';
            } else if (input) {
                input.type = 'password';
                input.classList.add('tracking-widest');
                icon.innerHTML = '<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" /><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />';
            }
        });
    });

    function showError(input, message) {
        if (!input) return;
        var formGroup = input.parentElement;
        if (formGroup.classList.contains('relative')) {
            formGroup = formGroup.parentElement; // Nếu là ô pass có con mắt, phải nhảy ra ngoài thêm 1 cấp
        }

        input.classList.remove('border-gray-300', 'focus:ring-blue-500');
        input.classList.add('border-red-500', 'focus:ring-red-500', 'bg-red-50');

        var errorText = formGroup.querySelector('.kc-feedback-text');
        if (!errorText) {
            errorText = document.createElement("span");
            errorText.className = "kc-feedback-text text-red-500 text-sm font-bold mt-2 block animate-pulse";
            formGroup.appendChild(errorText);
        }
        errorText.innerText = message;
        errorText.style.display = 'block';
    }

    function clearError(input) {
        if (!input) return;
        var formGroup = input.parentElement;
        if (formGroup.classList.contains('relative')) {
            formGroup = formGroup.parentElement;
        }

        input.classList.remove('border-red-500', 'focus:ring-red-500', 'bg-red-50');
        input.classList.add('border-gray-300', 'focus:ring-blue-500');

        var errorText = formGroup.querySelector('.kc-feedback-text');
        if (errorText) errorText.remove();
    }

    function attachInputListener(fields) {
        fields.forEach(function(field) {
            var input = document.querySelector('input[name="' + field.name + '"]');
            if (input) {
                input.addEventListener('input', function() { clearError(input); });
            }
        });
    }

    function handleFormSubmit(event, fields) {
        var isValid = true;
        fields.forEach(function(field) {
            var input = document.querySelector('input[name="' + field.name + '"]');
            if (input && !input.value.trim()) {
                showError(input, field.message);
                isValid = false;
            }
        });
        return isValid;
    }

    var registerForm = document.getElementById("kc-register-form");
    if (registerForm) {
        var regFields = [
            { name: "lastName", message: "Vui lòng nhập họ và đệm." },
            { name: "firstName", message: "Vui lòng nhập tên." },
            { name: "email", message: "Vui lòng nhập email hợp lệ." },
            { name: "username", message: "Vui lòng nhập tên đăng nhập." },
            { name: "password", message: "Vui lòng nhập mật khẩu." },
            { name: "password-confirm", message: "Vui lòng xác nhận mật khẩu." }
        ];
        attachInputListener(regFields);

        registerForm.addEventListener("submit", function(e) {
            if (!handleFormSubmit(e, regFields)) e.preventDefault();

            var pass = document.querySelector('input[name="password"]');
            var conf = document.querySelector('input[name="password-confirm"]');
            if (pass && conf && pass.value !== conf.value) {
                showError(conf, "Mật khẩu xác nhận không khớp.");
                e.preventDefault();
            }
        });
    }

    var loginForm = document.getElementById("kc-form-login");
    if (loginForm) {
        var loginFields = [
            { name: "username", message: "Vui lòng nhập tài khoản." },
            { name: "password", message: "Vui lòng nhập mật khẩu." }
        ];
        attachInputListener(loginFields);

        loginForm.addEventListener("submit", function(e) {
            if (!handleFormSubmit(e, loginFields)) e.preventDefault();
        });

        var errorMsg = "";
        var serverAlert = document.querySelector('.bg-red-50.text-red-700');

        if (serverAlert) {
            errorMsg = serverAlert.innerText.trim();
        }

        if (errorMsg) {
            console.log("Server Error Detected:", errorMsg);
            if (errorMsg.includes("Invalid username or password")) {
                var passInput = document.querySelector('input[name="password"]');
                showError(passInput, "Sai tài khoản hoặc mật khẩu.");
                passInput.value = "";
                passInput.focus();
            }
            else if (errorMsg.toLowerCase().includes("account is disabled")) {
                var userInput = document.querySelector('input[name="username"]');
                showError(userInput, "Tài khoản của bạn đã bị vô hiệu hóa.");
            }
            else if (errorMsg.toLowerCase().includes("temporarily disabled")) {
                var userInput = document.querySelector('input[name="username"]');
                showError(userInput, "Tài khoản tạm khóa do đăng nhập sai nhiều lần.");
            }
        }
    }
});