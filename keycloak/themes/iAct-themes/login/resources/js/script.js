document.addEventListener("DOMContentLoaded", function() {
    // --- 1. HÀM HIỂN THỊ LỖI CHUNG ---
    function showError(input, message) {
        if (!input) return;
        var formGroup = input.closest('.form-group') || input.closest('.pf-c-form-control-group') || input.parentElement;

        input.setAttribute("aria-invalid", "true"); // Tô đỏ viền

        var errorText = formGroup.querySelector('.kc-feedback-text');
        if (!errorText) {
            errorText = document.createElement("span");
            errorText.className = "kc-feedback-text";
            formGroup.appendChild(errorText);
        }
        errorText.innerText = message;
        errorText.style.display = 'block';
    }

    function clearError(input) {
        if (!input) return;
        var formGroup = input.closest('.form-group') || input.closest('.pf-c-form-control-group') || input.parentElement;
        input.removeAttribute("aria-invalid");
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
            { name: "username", message: "Please specify username." },
            { name: "email", message: "Please specify email." },
            { name: "password", message: "Please specify password." },
            { name: "password-confirm", message: "Please specify confirm password." },
            { name: "firstName", message: "Please specify first name." },
            { name: "lastName", message: "Please specify last name." }
        ];
        attachInputListener(regFields);
        registerForm.addEventListener("submit", function(e) {
            if (!handleFormSubmit(e, regFields)) e.preventDefault();

            var pass = document.querySelector('input[name="password"]');
            var conf = document.querySelector('input[name="password-confirm"]');
            if (pass && conf && pass.value !== conf.value) {
                showError(conf, "Password confirmation does not match.");
                e.preventDefault();
            }
        });
    }

    var loginForm = document.getElementById("kc-form-login");
    if (loginForm) {
        var loginFields = [
            { name: "username", message: "Please specify username or email." },
            { name: "password", message: "Please specify password." }
        ];
        attachInputListener(loginFields);

        loginForm.addEventListener("submit", function(e) {
            if (!handleFormSubmit(e, loginFields)) e.preventDefault();
        });

        var errorMsg = "";
        var serverAlert = document.querySelector('.alert-error, .pf-c-alert.pf-m-danger');
        var inputError = document.getElementById('input-error');

        if (serverAlert) {
            errorMsg = serverAlert.innerText.trim();
        } else if (inputError) {
            errorMsg = inputError.innerText.trim();
        }

        if (errorMsg) {
            console.log("Server Error Detected:", errorMsg);
            if (errorMsg.includes("Invalid username or password")) {
                var passInput = document.querySelector('input[name="password"]');
                showError(passInput, "Invalid username or password.");
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
            else {
                var userInput = document.querySelector('input[name="username"]');
                showError(userInput, errorMsg);
            }
        }
    }
});