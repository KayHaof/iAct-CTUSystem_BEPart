<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('password','password-confirm'); section>

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

            <h2 class="text-3xl font-extrabold text-center text-blue-600 mb-8 tracking-tight">Đổi Mật Khẩu iAct</h2>

            <form action="${url.loginAction}" method="post" class="space-y-6">

                <#if message?has_content && message.type == 'error'>
                    <div class="bg-red-50 text-red-700 p-4 rounded-xl text-sm font-bold flex items-center gap-2">
                        ${kcSanitize(message.summary)?no_esc}
                    </div>
                </#if>

                <div>
                    <label for="password-new" class="block text-sm font-bold text-gray-700 mb-2">Mật khẩu mới</label>
                    <div class="relative">
                        <input type="password" id="password-new" name="password-new" class="w-full px-5 py-4 pr-12 bg-gray-50 border border-gray-300 rounded-xl focus:ring-2 focus:ring-blue-500 outline-none font-semibold text-gray-800 text-lg tracking-widest placeholder-gray-400 placeholder:tracking-normal" placeholder="Nhập mật khẩu mới..." required autocomplete="new-password"/>
                        <button type="button" class="toggle-password absolute inset-y-0 right-0 px-4 flex items-center text-gray-400 hover:text-blue-600 focus:outline-none" data-target="password-new">
                            <svg class="h-6 w-6 eye-icon" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" /><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" /></svg>
                        </button>
                    </div>
                </div>

                <div>
                    <label for="password-confirm" class="block text-sm font-bold text-gray-700 mb-2">Nhập lại mật khẩu mới</label>
                    <div class="relative">
                        <input type="password" id="password-confirm" name="password-confirm" class="w-full px-5 py-4 pr-12 bg-gray-50 border border-gray-300 rounded-xl focus:ring-2 focus:ring-blue-500 outline-none font-semibold text-gray-800 text-lg tracking-widest placeholder-gray-400 placeholder:tracking-normal" placeholder="Xác nhận lại..." required autocomplete="new-password"/>
                        <button type="button" class="toggle-password absolute inset-y-0 right-0 px-4 flex items-center text-gray-400 hover:text-blue-600 focus:outline-none" data-target="password-confirm">
                            <svg class="h-6 w-6 eye-icon" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" /><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" /></svg>
                        </button>
                    </div>
                </div>

                <div class="pt-2 mt-2">
                    <button type="submit" class="w-full px-8 py-4 rounded-xl bg-blue-600 text-white hover:bg-blue-700 font-bold shadow-lg hover:shadow-blue-300 transition-all text-lg tracking-wide">
                        Cập nhật mật khẩu
                    </button>
                </div>
            </form>
        </div>
    </#if>
</@layout.registrationLayout>