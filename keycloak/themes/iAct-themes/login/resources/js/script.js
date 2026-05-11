/* ============================================================
   iAct Keycloak Theme - JavaScript
   Handles: toggle password, validation, strength, UX
   ============================================================ */
document.addEventListener('DOMContentLoaded', function () {

  /* -------------------------------------------------------
     1. PASSWORD TOGGLE
     ------------------------------------------------------- */
  document.querySelectorAll('.iact-toggle-password').forEach(function (btn) {
    btn.addEventListener('click', function () {
      var targetId = this.getAttribute('data-target');
      var input = document.getElementById(targetId);
      var icon = this.querySelector('.iact-eye-icon');
      if (!input || !icon) return;

      var isPassword = input.type === 'password';
      input.type = isPassword ? 'text' : 'password';
      icon.setAttribute('fill', isPassword ? 'none' : 'none');
      icon.classList.toggle('hidden', !isPassword);

      var iconAlt = this.querySelector('.iact-eye-off-icon');
      if (iconAlt) iconAlt.classList.toggle('hidden', isPassword);
    });
  });

  /* -------------------------------------------------------
     2. PASSWORD STRENGTH METER
     ------------------------------------------------------- */
  function calculateStrength(pw) {
    if (!pw) return 0;
    var score = 0;
    if (pw.length >= 8) score++;
    if (/[A-Z]/.test(pw)) score++;
    if (/[0-9]/.test(pw)) score++;
    if (/[^A-Za-z0-9]/.test(pw)) score++;
    return score; // 0-4
  }

  function updateStrengthUI(inputId, strength) {
    var container = document.querySelector('[data-strength="' + inputId + '"]');
    if (!container) return;

    var bars = container.querySelectorAll('.iact-strength-bar');
    var label = container.querySelector('.iact-strength-label');
    var levels = ['weak', 'fair', 'good', 'strong'];
    var levelClass = levels[Math.min(strength - 1, 3)] || '';

    bars.forEach(function (bar, i) {
      bar.className = 'iact-strength-bar';
      if (i < strength) {
        bar.classList.add('active-' + levelClass);
      }
    });

    if (label) {
      label.className = 'iact-strength-label visible ' + levelClass;
      var labels = ['', 'Mat khau yeu', 'Mat khau trung binh', 'Mat khau tot', 'Mat khau manh'];
      label.textContent = labels[strength] || '';
    }
  }

  document.querySelectorAll('[data-strength]').forEach(function (container) {
    var inputId = container.getAttribute('data-strength');
    var input = document.getElementById(inputId);
    if (input) {
      input.addEventListener('input', function () {
        updateStrengthUI(inputId, calculateStrength(this.value));
      });
    }
  });

  /* -------------------------------------------------------
     3. PASSWORD REQUIREMENTS CHECKLIST
     ------------------------------------------------------- */
  function setupRequirements(passwordId, confirmId) {
    var password = document.getElementById(passwordId);
    var confirm = confirmId ? document.getElementById(confirmId) : null;
    if (!password) return;

    var checks = {
      length:    /.{8,}/,
      uppercase: /[A-Z]/,
      lowercase: /[a-z]/,
      number:    /[0-9]/,
      special:   /[^A-Za-z0-9]/
    };

    function validate() {
      var pw = password.value;
      Object.keys(checks).forEach(function (key) {
        var el = document.querySelector('[data-req="' + key + '"]');
        if (el) {
          var met = checks[key].test(pw);
          el.classList.toggle('met', met);
          var icon = el.querySelector('.iact-pw-req-check');
          if (icon) {
            icon.setAttribute('fill', met ? 'currentColor' : 'none');
            icon.setAttribute('stroke', met ? 'currentColor' : '#94A3B8');
          }
        }
      });

      if (confirm) {
        var matchEl = document.querySelector('[data-req="match"]');
        if (matchEl) {
          var met = confirm.value.length > 0 && pw === confirm.value;
          matchEl.classList.toggle('met', met);
        }
      }
    }

    password.addEventListener('input', validate);
    if (confirm) confirm.addEventListener('input', validate);
  }

  setupRequirements('password', 'password-confirm');
  setupRequirements('password-new', 'password-confirm');

  /* -------------------------------------------------------
     4. FORM VALIDATION HELPERS
     ------------------------------------------------------- */
  function showFieldError(inputEl, message) {
    if (!inputEl) return;
    var field = inputEl.closest('.iact-field') || inputEl.parentElement;
    if (field.classList.contains('iact-input-wrap')) {
      field = field.parentElement;
    }

    inputEl.classList.add('has-error');

    var errEl = field.querySelector('.iact-error');
    if (errEl) {
      errEl.innerHTML = '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg> ' + message;
      errEl.classList.add('visible');
    }
  }

  function clearFieldError(inputEl) {
    if (!inputEl) return;
    inputEl.classList.remove('has-error');

    var field = inputEl.closest('.iact-field') || inputEl.parentElement;
    if (field.classList.contains('iact-input-wrap')) {
      field = field.parentElement;
    }

    var errEl = field.querySelector('.iact-error');
    if (errEl) errEl.classList.remove('visible');
  }

  function attachLiveValidation(inputName, validatorFn) {
    var input = document.querySelector('input[name="' + inputName + '"]');
    if (input) {
      input.addEventListener('input', function () { clearFieldError(input); });
      input.addEventListener('blur', function () {
        if (this.value && !validatorFn(this.value)) {
          showFieldError(this, 'Gia tri khong hop le.');
        }
      });
    }
  }

  /* -------------------------------------------------------
     5. LOGIN FORM VALIDATION
     ------------------------------------------------------- */
  var loginForm = document.getElementById('kc-form-login');
  if (loginForm) {
    var loginFields = [
      { name: 'username', message: 'Vui long nhap ten dang nhap.' },
      { name: 'password', message: 'Vui long nhap mat khau.' }
    ];

    loginFields.forEach(function (f) {
      var input = document.querySelector('input[name="' + f.name + '"]');
      if (input) {
        input.addEventListener('input', function () { clearFieldError(input); });
      }
    });

    loginForm.addEventListener('submit', function (e) {
      var valid = true;
      loginFields.forEach(function (f) {
        var input = document.querySelector('input[name="' + f.name + '"]');
        if (!input || !input.value.trim()) {
          showFieldError(input, f.message);
          valid = false;
        }
      });

      if (!valid) {
        e.preventDefault();
        var card = document.querySelector('.card-pf');
        if (card) {
          card.classList.add('iact-shake');
          setTimeout(function () { card.classList.remove('iact-shake'); }, 600);
        }
        return;
      }

      var btn = document.getElementById('kc-login');
      if (btn) {
        btn.classList.add('loading');
        btn.disabled = true;
      }
    });
  }

  /* -------------------------------------------------------
     6. REGISTER FORM VALIDATION
     ------------------------------------------------------- */
  var registerForm = document.getElementById('kc-register-form');
  if (registerForm) {
    var regFields = [
      { name: 'lastName',        message: 'Vui long nhap ho va dem.' },
      { name: 'firstName',       message: 'Vui long nhap ten.' },
      { name: 'email',            message: 'Vui long nhap email.',       validate: function (v) { return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v); } },
      { name: 'username',         message: 'Vui long nhap ten dang nhap.', validate: function (v) { return v.length >= 3; } },
      { name: 'password',         message: 'Vui long nhap mat khau.' },
      { name: 'password-confirm', message: 'Vui long xac nhan mat khau.' }
    ];

    regFields.forEach(function (f) {
      var input = document.querySelector('input[name="' + f.name + '"]');
      if (input) {
        input.addEventListener('input', function () { clearFieldError(input); });
        if (f.validate) {
          input.addEventListener('blur', function () {
            if (this.value && !f.validate(this.value)) {
              showFieldError(this, f.message);
            }
          });
        }
      }
    });

    registerForm.addEventListener('submit', function (e) {
      var valid = true;
      regFields.forEach(function (f) {
        var input = document.querySelector('input[name="' + f.name + '"]');
        if (!input || !input.value.trim()) {
          showFieldError(input, f.message);
          valid = false;
        } else if (f.validate && !f.validate(input.value)) {
          showFieldError(input, f.message);
          valid = false;
        }
      });

      var pass = document.querySelector('input[name="password"]');
      var conf = document.querySelector('input[name="password-confirm"]');
      if (pass && conf && pass.value && conf.value && pass.value !== conf.value) {
        showFieldError(conf, 'Mat khau xac nhan khong khop.');
        valid = false;
      }

      if (!valid) {
        e.preventDefault();
        var card = document.querySelector('.card-pf');
        if (card) {
          card.classList.add('iact-shake');
          setTimeout(function () { card.classList.remove('iact-shake'); }, 600);
        }
        return;
      }

      var btn = registerForm.querySelector('button[type="submit"]');
      if (btn) {
        btn.classList.add('loading');
        btn.disabled = true;
      }
    });
  }

  /* -------------------------------------------------------
     7. RESET PASSWORD FORM
     ------------------------------------------------------- */
  var resetForm = document.getElementById('kc-reset-password-form');
  if (resetForm) {
    var usernameInput = document.querySelector('input[name="username"]');
    if (usernameInput) {
      usernameInput.addEventListener('input', function () { clearFieldError(usernameInput); });
    }

    resetForm.addEventListener('submit', function (e) {
      var input = document.querySelector('input[name="username"]');
      if (!input || !input.value.trim()) {
        showFieldError(input, 'Vui long nhap ten dang nhap hoac email.');
        e.preventDefault();
        return;
      }
      var btn = resetForm.querySelector('button[type="submit"]');
      if (btn) {
        btn.classList.add('loading');
        btn.disabled = true;
      }
    });
  }

  /* -------------------------------------------------------
     8. UPDATE PASSWORD FORM
     ------------------------------------------------------- */
  var updateForm = document.querySelector('form[action="${url.loginAction}"]');
  if (updateForm && document.getElementById('password-new')) {
    var pwNew = document.getElementById('password-new');
    var pwConf = document.getElementById('password-confirm');

    if (pwNew) pwNew.addEventListener('input', function () { clearFieldError(pwNew); });
    if (pwConf) pwConf.addEventListener('input', function () { clearFieldError(pwConf); });

    updateForm.addEventListener('submit', function (e) {
      var valid = true;
      if (!pwNew || !pwNew.value.trim()) {
        showFieldError(pwNew, 'Vui long nhap mat khau moi.');
        valid = false;
      }
      if (!pwConf || !pwConf.value.trim()) {
        showFieldError(pwConf, 'Vui long xac nhan mat khau moi.');
        valid = false;
      }
      if (pwNew && pwConf && pwNew.value && pwConf.value && pwNew.value !== pwConf.value) {
        showFieldError(pwConf, 'Mat khau xac nhan khong khop.');
        valid = false;
      }

      if (!valid) {
        e.preventDefault();
        var card = document.querySelector('.card-pf');
        if (card) {
          card.classList.add('iact-shake');
          setTimeout(function () { card.classList.remove('iact-shake'); }, 600);
        }
      } else {
        var btn = updateForm.querySelector('button[type="submit"]');
        if (btn) {
          btn.classList.add('loading');
          btn.disabled = true;
        }
      }
    });
  }

  /* -------------------------------------------------------
     9. AUTO-DISMISS SERVER ALERTS
     ------------------------------------------------------- */
  var alerts = document.querySelectorAll('.iact-alert');
  alerts.forEach(function (alert) {
    if (!alert.classList.contains('iact-alert-warning') || !document.getElementById('kc-form-login')) {
      setTimeout(function () {
        alert.style.transition = 'opacity 0.4s ease';
        alert.style.opacity = '0';
        setTimeout(function () { alert.remove(); }, 400);
      }, 6000);
    }
  });

  /* -------------------------------------------------------
     10. SMOOTH ANCHOR SCROLL
     ------------------------------------------------------- */
  document.querySelectorAll('a[href^="#"]').forEach(function (anchor) {
    anchor.addEventListener('click', function (e) {
      var target = document.querySelector(this.getAttribute('href'));
      if (target) {
        e.preventDefault();
        target.scrollIntoView({ behavior: 'smooth', block: 'start' });
      }
    });
  });

  /* -------------------------------------------------------
     11. AUTO-FOCUS FIRST INPUT
     ------------------------------------------------------- */
  var firstInput = document.querySelector(
    '#kc-form-login input[name="username"],' +
    '#kc-register-form input[name="lastName"],' +
    '#kc-reset-password-form input[name="username"]'
  );
  if (firstInput && !firstInput.disabled) {
    setTimeout(function () { firstInput.focus(); }, 200);
  }

});
