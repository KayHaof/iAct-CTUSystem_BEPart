document.addEventListener('DOMContentLoaded', function () {
  function shakeCard() {
    var card = document.querySelector('.card-pf');
    if (!card) return;
    card.classList.add('iact-shake');
    setTimeout(function () {
      card.classList.remove('iact-shake');
    }, 500);
  }

  function focusFirstField(container) {
    if (!container) return;
    var firstInput = container.querySelector('input:not([type="hidden"]):not([disabled]), button:not([disabled])');
    if (firstInput) {
      setTimeout(function () {
        firstInput.focus();
      }, 60);
    }
  }

  document.querySelectorAll('.iact-toggle-password').forEach(function (btn) {
    btn.addEventListener('click', function () {
      var targetId = this.getAttribute('data-target');
      var input = document.getElementById(targetId);
      if (!input) return;

      var isPassword = input.type === 'password';
      input.type = isPassword ? 'text' : 'password';

      var eye = this.querySelector('.iact-eye-icon');
      var eyeOff = this.querySelector('.iact-eye-off-icon');
      if (eye) eye.classList.toggle('hidden', !isPassword);
      if (eyeOff) eyeOff.classList.toggle('hidden', isPassword);
    });
  });

  function calculateStrength(password) {
    if (!password) return 0;
    var score = 0;
    if (password.length >= 8) score++;
    if (/[A-Z]/.test(password)) score++;
    if (/[0-9]/.test(password)) score++;
    if (/[^A-Za-z0-9]/.test(password)) score++;
    return score;
  }

  function updateStrengthUI(inputId, strength) {
    var container = document.querySelector('[data-strength="' + inputId + '"]');
    if (!container) return;

    var bars = container.querySelectorAll('.iact-strength-bar');
    var label = container.querySelector('.iact-strength-label');
    var levels = ['', 'weak', 'fair', 'good', 'strong'];
    var texts = ['', 'Mật khẩu yếu', 'Mật khẩu trung bình', 'Mật khẩu khá', 'Mật khẩu mạnh'];
    var level = levels[strength] || '';

    bars.forEach(function (bar, index) {
      bar.className = 'iact-strength-bar';
      if (index < strength && level) {
        bar.classList.add('active-' + level);
      }
    });

    if (label) {
      if (strength > 0) {
        label.className = 'iact-strength-label visible ' + level;
        label.textContent = texts[strength];
      } else {
        label.className = 'iact-strength-label';
        label.textContent = '';
      }
    }
  }

  document.querySelectorAll('[data-strength]').forEach(function (container) {
    var inputId = container.getAttribute('data-strength');
    var input = document.getElementById(inputId);
    if (!input) return;
    updateStrengthUI(inputId, calculateStrength(input.value));
    input.addEventListener('input', function () {
      updateStrengthUI(inputId, calculateStrength(this.value));
    });
  });

  function setupRequirements(passwordId, confirmId) {
    var passwordInput = document.getElementById(passwordId);
    var confirmInput = confirmId ? document.getElementById(confirmId) : null;
    if (!passwordInput) return;

    var rules = {
      length: /.{8,}/,
      uppercase: /[A-Z]/,
      number: /[0-9]/,
      special: /[^A-Za-z0-9]/
    };

    function updateRequirements() {
      Object.keys(rules).forEach(function (key) {
        var item = document.querySelector('[data-req="' + key + '"]');
        if (!item) return;
        var met = rules[key].test(passwordInput.value);
        item.classList.toggle('met', met);

        var icon = item.querySelector('.iact-pw-req-check');
        if (icon) {
          icon.setAttribute('fill', met ? 'currentColor' : 'none');
          icon.setAttribute('stroke', met ? 'currentColor' : '#94A3B8');
        }
      });

      if (confirmInput) {
        var matchItem = document.querySelector('[data-req="match"]');
        if (matchItem) {
          var isMatch = confirmInput.value.length > 0 && confirmInput.value === passwordInput.value;
          matchItem.classList.toggle('met', isMatch);

          var matchIcon = matchItem.querySelector('.iact-pw-req-check');
          if (matchIcon) {
            matchIcon.setAttribute('fill', isMatch ? 'currentColor' : 'none');
            matchIcon.setAttribute('stroke', isMatch ? 'currentColor' : '#94A3B8');
          }
        }
      }
    }

    passwordInput.addEventListener('input', updateRequirements);
    if (confirmInput) confirmInput.addEventListener('input', updateRequirements);
    updateRequirements();
  }

  setupRequirements('password', 'password-confirm');
  setupRequirements('password-new', 'password-confirm');

  function resolveField(inputEl) {
    if (!inputEl) return null;
    var field = inputEl.closest('.iact-field') || inputEl.parentElement;
    if (field && field.classList.contains('iact-input-wrap')) {
      field = field.parentElement;
    }
    return field;
  }

  function showFieldError(inputEl, message) {
    if (!inputEl) return;
    var field = resolveField(inputEl);
    if (!field) return;

    inputEl.classList.add('has-error');
    field.setAttribute('data-has-error', 'true');

    var errorEl = field.querySelector('.iact-error');
    if (errorEl) {
      errorEl.innerHTML = '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="8" x2="12" y2="12"></line><line x1="12" y1="16" x2="12.01" y2="16"></line></svg> ' + message;
      errorEl.classList.add('visible');
    }
  }

  function clearFieldError(inputEl) {
    if (!inputEl) return;
    var field = resolveField(inputEl);
    inputEl.classList.remove('has-error');
    if (field) {
      field.removeAttribute('data-has-error');
      var errorEl = field.querySelector('.iact-error');
      if (errorEl) {
        errorEl.classList.remove('visible');
        errorEl.textContent = '';
      }
    }
  }

  function validateEmail(value) {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
  }

  function validateRegisterField(input, options) {
    options = options || {};
    var validateRequiredOnBlur = options.validateRequiredOnBlur === true;

    if (!input || input.disabled || input.type === 'hidden' || input.closest('.iact-hidden-submit')) {
      return true;
    }

    var value = input.value.trim();
    var name = input.name;

    if (name === 'lastName' && !value) {
      if (!validateRequiredOnBlur) return true;
      showFieldError(input, 'Vui lòng nhập họ và tên đệm.');
      return false;
    }

    if (name === 'firstName' && !value) {
      if (!validateRequiredOnBlur) return true;
      showFieldError(input, 'Vui lòng nhập tên.');
      return false;
    }

    if (name === 'email') {
      if (!value) {
        if (!validateRequiredOnBlur) return true;
        showFieldError(input, 'Vui lòng nhập email.');
        return false;
      }
      if (!validateEmail(value)) {
        showFieldError(input, 'Email chưa đúng định dạng.');
        return false;
      }
    }

    if (name === 'username') {
      if (!value) {
        if (!validateRequiredOnBlur) return true;
        showFieldError(input, 'Vui lòng nhập tên đăng nhập.');
        return false;
      }
      if (value.length < 3) {
        showFieldError(input, 'Tên đăng nhập cần có ít nhất 3 ký tự.');
        return false;
      }
    }

    if (name === 'password') {
      if (!value) {
        if (!validateRequiredOnBlur) return true;
        showFieldError(input, 'Vui lòng nhập mật khẩu.');
        return false;
      }
      if (value.length < 8) {
        showFieldError(input, 'Mật khẩu cần có ít nhất 8 ký tự.');
        return false;
      }
    }

    if (name === 'password-confirm') {
      if (!value) {
        if (!validateRequiredOnBlur) return true;
        showFieldError(input, 'Vui lòng xác nhận mật khẩu.');
        return false;
      }
      var passwordInput = document.querySelector('input[name="password"], input[name="password-new"]');
      if (passwordInput && passwordInput.value && passwordInput.value !== input.value) {
        showFieldError(input, 'Mật khẩu xác nhận chưa khớp.');
        return false;
      }
    }

    if (name === 'password-new') {
      if (!value) {
        if (!validateRequiredOnBlur) return true;
        showFieldError(input, 'Vui lòng nhập mật khẩu mới.');
        return false;
      }
      if (value.length < 8) {
        showFieldError(input, 'Mật khẩu mới cần có ít nhất 8 ký tự.');
        return false;
      }
    }

    clearFieldError(input);
    return true;
  }

  function bindLiveValidation(form) {
    if (!form) return;
    form.querySelectorAll('input').forEach(function (input) {
      input.addEventListener('input', function () {
        clearFieldError(input);
      });
      input.addEventListener('blur', function () {
        validateRegisterField(input, { validateRequiredOnBlur: false });
      });
    });
  }

  var loginForm = document.getElementById('kc-form-login');
  if (loginForm) {
    bindLiveValidation(loginForm);
    loginForm.addEventListener('submit', function (event) {
      var userInput = loginForm.querySelector('input[name="username"]');
      var passwordInput = loginForm.querySelector('input[name="password"]');
      var valid = true;

      if (!userInput || !userInput.value.trim()) {
        showFieldError(userInput, 'Vui lòng nhập tài khoản hoặc email.');
        valid = false;
      }

      if (!passwordInput || !passwordInput.value.trim()) {
        showFieldError(passwordInput, 'Vui lòng nhập mật khẩu.');
        valid = false;
      }

      if (!valid) {
        event.preventDefault();
        shakeCard();
        return;
      }

      var loginButton = document.getElementById('kc-login');
      if (loginButton) {
        loginButton.classList.add('loading');
        loginButton.disabled = true;
      }
    });
  }

  var registerForm = document.getElementById('kc-register-form');
  if (registerForm) {
    var steps = Array.prototype.slice.call(registerForm.querySelectorAll('.iact-form-step'));
    var indicators = Array.prototype.slice.call(document.querySelectorAll('[data-step-indicator]'));
    var stepFields = {
      1: ['lastName', 'firstName'],
      2: ['email', 'username'],
      3: ['password', 'password-confirm']
    };

    function updateStepper(stepNumber) {
      indicators.forEach(function (item) {
        var itemStep = Number(item.getAttribute('data-step-indicator'));
        item.classList.toggle('is-active', itemStep === stepNumber);
        item.classList.toggle('is-complete', itemStep < stepNumber);
      });
    }

    function goToStep(stepNumber) {
      steps.forEach(function (step) {
        var isActive = Number(step.getAttribute('data-step')) === stepNumber;
        step.classList.toggle('is-active', isActive);
      });
      registerForm.setAttribute('data-current-step', String(stepNumber));
      updateStepper(stepNumber);
      var activeStep = registerForm.querySelector('.iact-form-step.is-active');
      focusFirstField(activeStep);
    }

    function getFieldsForStep(stepNumber) {
      return (stepFields[stepNumber] || []).map(function (fieldName) {
        return registerForm.querySelector('[name="' + fieldName + '"]');
      }).filter(function (field) {
        return !!field;
      });
    }

    function validateStep(stepNumber) {
      var fields = getFieldsForStep(stepNumber);
      var valid = true;
      var firstInvalid = null;

      fields.forEach(function (field) {
        if (!validateRegisterField(field)) {
          valid = false;
          if (!firstInvalid) firstInvalid = field;
        }
      });

      if (!valid) {
        if (firstInvalid) firstInvalid.focus();
        shakeCard();
      }

      return valid;
    }

    function findServerErrorStep() {
      for (var stepNumber = 1; stepNumber <= 3; stepNumber++) {
        var fields = getFieldsForStep(stepNumber);
        var hasError = fields.some(function (field) {
          var container = resolveField(field);
          return container && container.getAttribute('data-has-error') === 'true';
        });
        if (hasError) return stepNumber;
      }
      return 1;
    }

    bindLiveValidation(registerForm);

    registerForm.querySelectorAll('.iact-step-next').forEach(function (button) {
      button.addEventListener('click', function () {
        var nextStep = Number(this.getAttribute('data-next-step'));
        var currentStep = nextStep - 1;
        if (validateStep(currentStep)) {
          goToStep(nextStep);
        }
      });
    });

    registerForm.querySelectorAll('.iact-step-prev').forEach(function (button) {
      button.addEventListener('click', function () {
        var prevStep = Number(this.getAttribute('data-prev-step'));
        goToStep(prevStep);
      });
    });

    registerForm.addEventListener('submit', function (event) {
      var allValid = true;
      var firstInvalidStep = null;

      [1, 2, 3].forEach(function (stepNumber) {
        var stepValid = validateStep(stepNumber);
        if (!stepValid && firstInvalidStep === null) {
          firstInvalidStep = stepNumber;
          allValid = false;
        }
      });

      if (!allValid) {
        event.preventDefault();
        goToStep(firstInvalidStep);
        return;
      }

      var submitButton = registerForm.querySelector('button[type="submit"]');
      if (submitButton) {
        submitButton.classList.add('loading');
        submitButton.disabled = true;
      }
    });

    goToStep(findServerErrorStep());
  }

  var resetForm = document.getElementById('kc-reset-password-form');
  if (resetForm) {
    bindLiveValidation(resetForm);
    resetForm.addEventListener('submit', function (event) {
      var usernameInput = resetForm.querySelector('input[name="username"]');
      if (!usernameInput || !usernameInput.value.trim()) {
        showFieldError(usernameInput, 'Vui lòng nhập tên đăng nhập hoặc email.');
        event.preventDefault();
        shakeCard();
        return;
      }

      var button = resetForm.querySelector('button[type="submit"]');
      if (button) {
        button.classList.add('loading');
        button.disabled = true;
      }
    });
  }

  var updateForm = document.querySelector('form[action]');
  if (updateForm && document.getElementById('password-new')) {
    bindLiveValidation(updateForm);
    updateForm.addEventListener('submit', function (event) {
      var passwordNew = document.getElementById('password-new');
      var passwordConfirm = document.getElementById('password-confirm');
      var valid = true;

      if (!validateRegisterField(passwordNew)) valid = false;
      if (!validateRegisterField(passwordConfirm)) valid = false;

      if (!valid) {
        event.preventDefault();
        shakeCard();
        return;
      }

      var submitButton = updateForm.querySelector('button[type="submit"]');
      if (submitButton) {
        submitButton.classList.add('loading');
        submitButton.disabled = true;
      }
    });
  }

  document.querySelectorAll('.iact-alert').forEach(function (alert) {
    if (!alert.classList.contains('iact-alert-warning') || !document.getElementById('kc-form-login')) {
      setTimeout(function () {
        alert.style.transition = 'opacity 0.4s ease';
        alert.style.opacity = '0';
        setTimeout(function () {
          if (alert.parentNode) alert.parentNode.removeChild(alert);
        }, 400);
      }, 6000);
    }
  });

  var defaultFocus = document.querySelector(
    '#kc-form-login input[name="username"],' +
    '#kc-reset-password-form input[name="username"]'
  );
  if (defaultFocus && !defaultFocus.disabled) {
    setTimeout(function () {
      defaultFocus.focus();
    }, 150);
  }
});
