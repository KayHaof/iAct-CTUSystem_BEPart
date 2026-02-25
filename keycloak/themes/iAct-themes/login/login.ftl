<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('username','password') displayInfo=false; section>

    <#if section = "header">
    <#elseif section = "form">
        <script src="https://cdn.tailwindcss.com"></script>

        <style>
            body, .login-pf-page {
                background-image: url('${url.resourcesPath}/img/bg.jpg') !important;
                background-size: cover !important; background-position: center !important;
                background-attachment: fixed !important; background-color: #f3f4f6 !important;
                min-height: 100vh !important; width: 100vw !important;
                display: flex !important; align-items: center !important; justify-content: center !important;
                margin: 0 !important; padding: 2rem 1rem !important; box-sizing: border-box !important;
            }
            .card-pf {
                background: transparent !important; border: none !important; box-shadow: none !important;
                padding: 0 !important; margin: 0 !important; width: 100% !important; max-width: 100% !important;
            }
            #kc-header, #kc-page-title { display: none !important; }
        </style>

        <div class="w-full max-w-lg mx-auto p-8 sm:p-10 bg-white rounded-3xl shadow-2xl">

            <div class="flex justify-center mb-6">
                <img src="${url.resourcesPath}/img/logo.png" alt="Logo" class="h-20 object-contain">
            </div>

            <div class="text-center mb-8">
                <h2 class="text-3xl font-extrabold text-blue-600 mb-2 tracking-tight">
                    <#if usernameEditDisabled??>Xác thực danh tính<#else>Đăng nhập iAct</#if>
                </h2>
                <p class="text-gray-500 text-base font-medium">
                    <#if usernameEditDisabled??>Vui lòng nhập mật khẩu hiện tại để tiếp tục<#else>Chào mừng bạn quay lại hệ thống</#if>
                </p>
            </div>

            <#if message?has_content && (message.type != 'warning' || !isAppInitiatedAction??)>
                <div class="mb-6 p-4 rounded-xl text-sm font-bold flex items-center gap-2
                    <#if message.type = 'success'>bg-green-50 text-green-700
                    <#elseif message.type = 'warning'>bg-yellow-50 text-yellow-700
                    <#elseif message.type = 'error'>bg-red-50 text-red-700
                    <#else>bg-blue-50 text-blue-700</#if>">
                    ${kcSanitize(message.summary)?no_esc}
                </div>
            </#if>

            <form id="kc-form-login" onsubmit="login.disabled = true; return true;" action="${url.loginAction}" method="post" class="space-y-6">
                <div>
                    <label for="username" class="block text-sm font-bold text-gray-700 mb-2">
                        <#if !realm.loginWithEmailAllowed>Tên đăng nhập<#elseif !realm.registrationEmailAsUsername>Tên đăng nhập hoặc Email<#else>Email</#if>
                    </label>
                    <#if usernameEditDisabled??>
                        <div class="relative">
                            <input tabindex="1" id="username" class="w-full px-5 py-4 bg-gray-100 border border-gray-200 rounded-xl text-gray-500 cursor-not-allowed outline-none font-semibold text-lg" name="username" value="${(login.username!'')}" type="text" disabled />
                            <svg class="absolute right-5 top-4.5 h-6 w-6 text-gray-400" fill="currentColor" viewBox="0 0 16 16"><path d="M8 1a2 2 0 0 1 2 2v4H6V3a2 2 0 0 1 2-2zm3 6V3a3 3 0 0 0-6 0v4a2 2 0 0 0-2 2v5a2 2 0 0 0 2 2h6a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2z"/></svg>
                        </div>
                        <input type="hidden" name="username" value="${(login.username!'')}"/>
                    <#else>
                        <input tabindex="1" id="username" class="w-full px-5 py-4 bg-gray-50 border border-gray-300 rounded-xl focus:ring-2 focus:ring-blue-500 outline-none transition-all font-semibold text-gray-800 text-lg placeholder-gray-400" name="username" value="${(login.username!'')}" type="text" autofocus autocomplete="off" placeholder="Nhập tài khoản..."/>
                    </#if>
                </div>

                <div>
                    <div class="flex justify-between items-center mb-2">
                        <label for="password" class="block text-sm font-bold text-gray-700">Mật khẩu</label>
                        <#if realm.resetPasswordAllowed>
                            <a tabindex="5" href="${url.loginResetCredentialsUrl}" class="text-sm font-bold text-blue-600 hover:text-blue-800 transition-colors">Quên mật khẩu?</a>
                        </#if>
                    </div>
                    <div class="relative">
                        <input tabindex="2" id="password" class="w-full px-5 py-4 pr-12 bg-gray-50 border border-gray-300 rounded-xl focus:ring-2 focus:ring-blue-500 outline-none transition-all font-semibold text-gray-800 text-lg tracking-widest placeholder-gray-400 placeholder:tracking-normal" name="password" type="password" autocomplete="off" placeholder="Nhập mật khẩu..."/>
                        <button type="button" class="toggle-password absolute inset-y-0 right-0 px-4 flex items-center text-gray-400 hover:text-blue-600 focus:outline-none" data-target="password">
                            <svg class="h-6 w-6 eye-icon" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" /><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" /></svg>
                        </button>
                    </div>
                </div>

                <div class="pt-2">
                    <input type="hidden" id="id-hidden-input" name="credentialId" <#if auth.selectedCredential?has_content>value="${auth.selectedCredential}"</#if>/>
                    <button tabindex="4" name="login" id="kc-login" type="submit" class="w-full px-8 py-4 rounded-xl bg-blue-600 text-white hover:bg-blue-700 font-bold shadow-lg hover:shadow-blue-300 transition-all text-lg tracking-wide">
                        <#if usernameEditDisabled??>Xác thực để đổi mật khẩu<#else>Đăng nhập</#if>
                    </button>
                </div>

                <#if usernameEditDisabled??>
                    <div class="text-center mt-4">
                        <a href="${url.loginAction?replace('authenticate', 'cancel')}" class="text-sm font-semibold text-gray-500 hover:text-gray-700">Hủy bỏ, quay lại trang trước</a>
                    </div>
                </#if>
            </form>

            <#if realm.password && realm.registrationAllowed && !registrationDisabled?? && !usernameEditDisabled??>
                <div class="text-center mt-8 pt-6 border-t border-gray-100">
                    <span class="text-gray-500 font-medium text-base">Bạn chưa có tài khoản? </span>
                    <a tabindex="6" href="${url.registrationUrl}" class="font-bold text-blue-600 hover:text-blue-800 transition-colors underline underline-offset-4 text-base">Đăng ký ngay</a>
                </div>
            </#if>
        </div>
    </#if>
</@layout.registrationLayout>