<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=true; section>
    <#if section = "header">
    <#-- Để trống -->
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

        <div class="w-full max-w-lg mx-auto p-8 sm:p-10 bg-white rounded-3xl shadow-2xl animate-fade-in">

            <#-- LOGO -->
            <div class="flex justify-center mb-6">
                <img src="${url.resourcesPath}/img/logo.png" alt="Logo" class="h-20 object-contain">
            </div>

            <#-- TIÊU ĐỀ -->
            <div class="text-center mb-8">
                <h2 class="text-3xl font-extrabold text-blue-600 mb-2 tracking-tight">Xác thực Email</h2>
                <div class="h-1 w-20 bg-blue-600 mx-auto rounded-full"></div>
            </div>

            <#-- THÔNG BÁO -->
            <div class="bg-yellow-50 border-l-4 border-yellow-400 p-6 rounded-xl mb-8">
                <div class="flex items-start">
                    <div class="flex-shrink-0">
                        <svg class="h-6 w-6 text-yellow-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                        </svg>
                    </div>
                    <div class="ml-3">
                        <p class="text-sm font-bold text-yellow-700 leading-relaxed">
                            ${msg("emailVerifyInstruction")}
                        </p>
                    </div>
                </div>
            </div>

            <#-- NÚT GỬI LẠI EMAIL (ĐÃ SỬA HOVER) -->
            <div class="space-y-4">
                <p class="text-center text-gray-500 font-medium">
                    Bạn chưa nhận được email?
                </p>
                <a href="${url.loginAction}" class="block w-full text-center px-8 py-4 rounded-xl bg-blue-600 text-white hover:bg-blue-700 hover:text-white font-bold shadow-lg hover:shadow-blue-200 transition-all text-lg tracking-wide">
                    Gửi lại mã xác nhận
                </a>
            </div>

            <#-- QUAY LẠI ĐĂNG NHẬP -->
            <div class="text-center mt-8 pt-6 border-t border-gray-100">
                <a href="${url.loginUrl}" class="text-base font-bold text-gray-400 hover:text-blue-600 transition-colors flex items-center justify-center gap-2">
                    <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 19l-7-7m0 0l7-7m-7 7h18" />
                    </svg>
                    Quay lại Đăng nhập
                </a>
            </div>
        </div>

    <#elseif section = "info" >
    <#-- Để trống -->
    </#if>
</@layout.registrationLayout>