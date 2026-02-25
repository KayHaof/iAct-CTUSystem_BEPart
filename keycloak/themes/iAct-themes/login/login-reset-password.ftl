<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=true displayMessage=!messagesPerField.existsError('username'); section>

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

            <#-- LOGO -->
            <div class="flex justify-center mb-6">
                <img src="${url.resourcesPath}/img/logo.png" alt="Logo" class="h-20 object-contain">
            </div>

            <#-- TIÊU ĐỀ -->
            <div class="text-center mb-8">
                <h2 class="text-3xl font-extrabold text-blue-600 mb-2 tracking-tight">Quên mật khẩu?</h2>
                <p class="text-gray-500 text-base font-medium">Nhập email hoặc tên đăng nhập của bạn để nhận liên kết khôi phục</p>
            </div>

            <#-- HIỆN LỖI/THÔNG BÁO -->
            <#if message?has_content && (message.type != 'warning' || !isAppInitiatedAction??)>
                <div class="mb-6 p-4 rounded-xl text-sm font-bold flex items-center gap-2
                    <#if message.type = 'success'>bg-green-50 text-green-700
                    <#elseif message.type = 'warning'>bg-yellow-50 text-yellow-700
                    <#elseif message.type = 'error'>bg-red-50 text-red-700
                    <#else>bg-blue-50 text-blue-700</#if>">
                    ${kcSanitize(message.summary)?no_esc}
                </div>
            </#if>

            <#-- FORM SUBMIT -->
            <form id="kc-reset-password-form" class="space-y-6" action="${url.loginAction}" method="post">
                <div>
                    <label for="username" class="block text-sm font-bold text-gray-700 mb-2">
                        <#if !realm.loginWithEmailAllowed>Tên đăng nhập<#elseif !realm.registrationEmailAsUsername>Tên đăng nhập hoặc Email<#else>Email</#if>
                    </label>
                    <input type="text" id="username" name="username" class="w-full px-5 py-4 bg-gray-50 border border-gray-300 rounded-xl focus:ring-2 focus:ring-blue-500 outline-none transition-all font-semibold text-gray-800 text-lg placeholder-gray-400" autofocus placeholder="Nhập tài khoản của bạn..." value="${(auth.attemptedUsername!'')}" />
                </div>

                <div class="pt-2">
                    <button type="submit" class="w-full px-8 py-4 rounded-xl bg-blue-600 text-white hover:bg-blue-700 font-bold shadow-lg hover:shadow-blue-300 transition-all text-lg tracking-wide">
                        Gửi yêu cầu
                    </button>
                </div>

                <#-- QUAY LẠI ĐĂNG NHẬP -->
                <div class="text-center mt-6">
                    <a href="${url.loginUrl}" class="text-base font-bold text-gray-500 hover:text-blue-600 transition-colors underline underline-offset-4">Quay lại trang Đăng nhập</a>
                </div>
            </form>
        </div>

    <#elseif section = "info" >
    </#if>
</@layout.registrationLayout>