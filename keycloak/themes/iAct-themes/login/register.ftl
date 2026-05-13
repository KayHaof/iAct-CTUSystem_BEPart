<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('firstName','lastName','email','username','password','password-confirm'); section>

    <#if section = "header">
    <#elseif section = "form">

        <div class="iact-logo-area">
            <img src="${url.resourcesPath}/img/logo.png" alt="iAct Logo" class="iact-logo">
            <span class="iact-brand-name">iAct</span>
            <span class="iact-brand-sub">Can Tho University</span>
        </div>

        <div class="iact-page-header iact-page-header-compact">
            <h1 class="iact-page-title">Tạo tài khoản mới</h1>
            <p class="iact-page-desc">Hoàn tất 3 bước ngắn để bắt đầu tham gia hoạt động cùng iAct.</p>
        </div>

        <#if message?has_content && (message.type != 'warning' || !isAppInitiatedAction??)>
            <div class="iact-alert iact-alert-<#if message.type = 'success'>success<#elseif message.type = 'warning'>warning<#elseif message.type = 'error'>error<#else>info</#if> iact-mb-6">
                <span class="iact-alert-icon">
                    <#if message.type = 'success'>
                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
                    <#elseif message.type = 'warning'>
                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
                    <#elseif message.type = 'error'>
                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
                    <#else>
                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>
                    </#if>
                </span>
                <span>${kcSanitize(message.summary)?no_esc}</span>
            </div>
        </#if>

        <div class="iact-stepper" aria-label="Tiến trình đăng ký">
            <div class="iact-stepper-item is-active" data-step-indicator="1">
                <span class="iact-stepper-dot">1</span>
                <div class="iact-stepper-copy">
                    <strong>Họ tên</strong>
                    <span>Thông tin cá nhân</span>
                </div>
            </div>
            <div class="iact-stepper-line" aria-hidden="true"></div>
            <div class="iact-stepper-item" data-step-indicator="2">
                <span class="iact-stepper-dot">2</span>
                <div class="iact-stepper-copy">
                    <strong>Tài khoản</strong>
                    <span>Email và đăng nhập</span>
                </div>
            </div>
            <div class="iact-stepper-line" aria-hidden="true"></div>
            <div class="iact-stepper-item" data-step-indicator="3">
                <span class="iact-stepper-dot">3</span>
                <div class="iact-stepper-copy">
                    <strong>Bảo mật</strong>
                    <span>Mật khẩu</span>
                </div>
            </div>
        </div>

        <form id="kc-register-form" action="${url.registrationAction}" method="post" novalidate data-current-step="1">

            <section class="iact-form-step is-active" data-step="1">
                <div class="iact-step-header">
                    <span class="iact-step-badge">Bước 1</span>
                    <h2 class="iact-step-title">Thông tin cá nhân</h2>
                    <p class="iact-step-desc">Nhập họ tên để hệ thống hiển thị đúng trên hồ sơ và các hoạt động.</p>
                </div>

                <div class="iact-form-grid-2">
                    <div class="iact-field"<#if messagesPerField.existsError('lastName')> data-has-error="true"</#if>>
                        <label for="lastName" class="iact-label">
                            Họ và tên đệm <span class="iact-label-required">*</span>
                        </label>
                        <div class="iact-input-wrap">
                            <span class="iact-input-icon">
                                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                    <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                                    <circle cx="12" cy="7" r="4"/>
                                </svg>
                            </span>
                            <input type="text" id="lastName" name="lastName" value="${(register.formData.lastName!'')}"
                                class="iact-input has-icon<#if messagesPerField.existsError('lastName')> has-error</#if>" placeholder="Ví dụ: Nguyễn Văn"
                                autocomplete="family-name">
                        </div>
                        <span class="iact-error<#if messagesPerField.existsError('lastName')> visible</#if>"><#if messagesPerField.existsError('lastName')>${kcSanitize(messagesPerField.get('lastName'))?no_esc}</#if></span>
                    </div>

                    <div class="iact-field"<#if messagesPerField.existsError('firstName')> data-has-error="true"</#if>>
                        <label for="firstName" class="iact-label">
                            Tên <span class="iact-label-required">*</span>
                        </label>
                        <div class="iact-input-wrap">
                            <input type="text" id="firstName" name="firstName" value="${(register.formData.firstName!'')}"
                                class="iact-input<#if messagesPerField.existsError('firstName')> has-error</#if>" placeholder="Ví dụ: An"
                                autocomplete="given-name">
                        </div>
                        <span class="iact-error<#if messagesPerField.existsError('firstName')> visible</#if>"><#if messagesPerField.existsError('firstName')>${kcSanitize(messagesPerField.get('firstName'))?no_esc}</#if></span>
                    </div>
                </div>

                <div class="iact-step-actions">
                    <button type="button" class="iact-btn iact-btn-primary iact-step-next" data-next-step="2">
                        Tiếp tục
                    </button>
                </div>
            </section>

            <section class="iact-form-step" data-step="2">
                <div class="iact-step-header">
                    <span class="iact-step-badge">Bước 2</span>
                    <h2 class="iact-step-title">Thông tin tài khoản</h2>
                    <p class="iact-step-desc">Điền email và thông tin đăng nhập để nhận thông báo và truy cập hệ thống.</p>
                </div>

                <div class="iact-field"<#if messagesPerField.existsError('email')> data-has-error="true"</#if>>
                    <label for="email" class="iact-label">
                        Email <span class="iact-label-required">*</span>
                    </label>
                    <div class="iact-input-wrap">
                        <span class="iact-input-icon">
                            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
                                <polyline points="22,6 12,13 2,6"/>
                            </svg>
                        </span>
                        <input type="email" id="email" name="email" value="${(register.formData.email!'')}"
                            class="iact-input has-icon<#if messagesPerField.existsError('email')> has-error</#if>" placeholder="email@student.ctu.edu.vn"
                            autocomplete="email">
                    </div>
                    <span class="iact-helper-text">Nên dùng email đang hoạt động để nhận xác thực và khôi phục mật khẩu.</span>
                    <span class="iact-error<#if messagesPerField.existsError('email')> visible</#if>"><#if messagesPerField.existsError('email')>${kcSanitize(messagesPerField.get('email'))?no_esc}</#if></span>
                </div>

                <#if !realm.registrationEmailAsUsername>
                    <div class="iact-field"<#if messagesPerField.existsError('username')> data-has-error="true"</#if>>
                        <label for="username" class="iact-label">
                            Tên đăng nhập <span class="iact-label-required">*</span>
                        </label>
                        <div class="iact-input-wrap">
                            <span class="iact-input-icon">
                                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                    <circle cx="12" cy="12" r="10"/>
                                    <path d="M12 8v4l3 3"/>
                                </svg>
                            </span>
                            <input type="text" id="username" name="username" value="${(register.formData.username!'')}"
                                class="iact-input has-icon<#if messagesPerField.existsError('username')> has-error</#if>" placeholder="Ví dụ: an.nguyen"
                                autocomplete="username">
                        </div>
                        <span class="iact-helper-text">Tên đăng nhập nên ngắn gọn, dễ nhớ và không chứa khoảng trắng.</span>
                        <span class="iact-error<#if messagesPerField.existsError('username')> visible</#if>"><#if messagesPerField.existsError('username')>${kcSanitize(messagesPerField.get('username'))?no_esc}</#if></span>
                    </div>
                <#else>
                    <div class="iact-inline-note">
                        Email của bạn sẽ được dùng làm tên đăng nhập.
                    </div>
                </#if>

                <div class="iact-step-actions iact-step-actions-between">
                    <button type="button" class="iact-btn iact-btn-ghost iact-step-prev" data-prev-step="1">
                        Quay lại
                    </button>
                    <button type="button" class="iact-btn iact-btn-primary iact-step-next" data-next-step="3">
                        Tiếp tục
                    </button>
                </div>
            </section>

            <section class="iact-form-step" data-step="3">
                <div class="iact-step-header">
                    <span class="iact-step-badge">Bước 3</span>
                    <h2 class="iact-step-title">Thiết lập mật khẩu</h2>
                    <p class="iact-step-desc">Chọn mật khẩu đủ mạnh để bảo vệ tài khoản của bạn tốt hơn.</p>
                </div>

                <#if passwordRequired??>
                    <div class="iact-field"<#if messagesPerField.existsError('password')> data-has-error="true"</#if>>
                        <label for="password" class="iact-label">
                            Mật khẩu <span class="iact-label-required">*</span>
                        </label>
                        <div class="iact-input-wrap">
                            <input type="password" id="password" name="password" class="iact-input<#if messagesPerField.existsError('password')> has-error</#if>"
                                placeholder="Tạo mật khẩu mới"
                                autocomplete="new-password">
                            <button type="button" class="iact-toggle-password" data-target="password" aria-label="Hiển thị mật khẩu">
                                <svg class="iact-eye-icon" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                    <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                                    <circle cx="12" cy="12" r="3"/>
                                </svg>
                                <svg class="iact-eye-off-icon hidden" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                    <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/>
                                    <line x1="1" y1="1" x2="23" y2="23"/>
                                </svg>
                            </button>
                        </div>
                        <div class="iact-password-strength" data-strength="password">
                            <div class="iact-strength-bars">
                                <span class="iact-strength-bar"></span>
                                <span class="iact-strength-bar"></span>
                                <span class="iact-strength-bar"></span>
                                <span class="iact-strength-bar"></span>
                            </div>
                            <span class="iact-strength-label"></span>
                        </div>
                        <div class="iact-pw-requirements">
                            <span class="iact-pw-req" data-req="length">
                                <svg class="iact-pw-req-check" viewBox="0 0 24 24" fill="none" stroke="#94A3B8" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
                                Ít nhất 8 ký tự
                            </span>
                            <span class="iact-pw-req" data-req="uppercase">
                                <svg class="iact-pw-req-check" viewBox="0 0 24 24" fill="none" stroke="#94A3B8" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
                                Có chữ in hoa
                            </span>
                            <span class="iact-pw-req" data-req="number">
                                <svg class="iact-pw-req-check" viewBox="0 0 24 24" fill="none" stroke="#94A3B8" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
                                Có chữ số
                            </span>
                            <span class="iact-pw-req" data-req="special">
                                <svg class="iact-pw-req-check" viewBox="0 0 24 24" fill="none" stroke="#94A3B8" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
                                Có ký tự đặc biệt
                            </span>
                        </div>
                        <span class="iact-error<#if messagesPerField.existsError('password')> visible</#if>"><#if messagesPerField.existsError('password')>${kcSanitize(messagesPerField.get('password'))?no_esc}</#if></span>
                    </div>

                    <div class="iact-field"<#if messagesPerField.existsError('password-confirm')> data-has-error="true"</#if>>
                        <label for="password-confirm" class="iact-label">
                            Xác nhận mật khẩu <span class="iact-label-required">*</span>
                        </label>
                        <div class="iact-input-wrap">
                            <input type="password" id="password-confirm" name="password-confirm" class="iact-input<#if messagesPerField.existsError('password-confirm')> has-error</#if>"
                                placeholder="Nhập lại mật khẩu"
                                autocomplete="new-password">
                            <button type="button" class="iact-toggle-password" data-target="password-confirm" aria-label="Hiển thị mật khẩu">
                                <svg class="iact-eye-icon" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                    <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                                    <circle cx="12" cy="12" r="3"/>
                                </svg>
                                <svg class="iact-eye-off-icon hidden" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                    <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/>
                                    <line x1="1" y1="1" x2="23" y2="23"/>
                                </svg>
                            </button>
                        </div>
                        <div class="iact-pw-requirements">
                            <span class="iact-pw-req" data-req="match">
                                <svg class="iact-pw-req-check" viewBox="0 0 24 24" fill="none" stroke="#94A3B8" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
                                Mật khẩu khớp nhau
                            </span>
                        </div>
                        <span class="iact-error<#if messagesPerField.existsError('password-confirm')> visible</#if>"><#if messagesPerField.existsError('password-confirm')>${kcSanitize(messagesPerField.get('password-confirm'))?no_esc}</#if></span>
                    </div>
                </#if>

                <div class="iact-step-actions iact-step-actions-between">
                    <button type="button" class="iact-btn iact-btn-ghost iact-step-prev" data-prev-step="2">
                        Quay lại
                    </button>
                    <button type="submit" class="iact-btn iact-btn-primary">
                        Tạo tài khoản
                    </button>
                </div>
            </section>

        </form>

        <div class="iact-footer">
            <span class="iact-footer-text">Đã có tài khoản?</span>
            <a href="${url.loginUrl}" class="iact-footer-link">Đăng nhập ngay</a>
        </div>

    </#if>
</@layout.registrationLayout>
