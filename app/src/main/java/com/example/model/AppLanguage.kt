package com.example.model

enum class AppLanguage(val code: String, val displayName: String, val nativeName: String) {
    ENGLISH("en", "English", "English"),
    SPANISH("es", "Spanish", "Español"),
    FRENCH("fr", "French", "Français"),
    GERMAN("de", "German", "Deutsch"),
    JAPANESE("ja", "Japanese", "日本語"),
    PORTUGUESE("pt", "Portuguese", "Português"),
    CHINESE("zh", "Chinese", "中文")
}

data class LocalizedStrings(
    val tabPersonalize: String,
    val tabDeviceTest: String,
    val tabSecurity: String,
    val tabCompiler: String,
    val tabSettings: String,
    val settingsTitle: String,
    val languageSection: String,
    val editorSection: String,
    val securityConfigSection: String,
    val resetSection: String,
    val resetAllConfig: String,
    val resetConfirmTitle: String,
    val resetConfirmMessage: String,
    val fontSizeLabel: String,
    val aboutTitle: String,
    val clearCacheTitle: String,
    val backupTitle: String
)

object LanguageManager {
    fun getStrings(language: AppLanguage): LocalizedStrings {
        return when (language) {
            AppLanguage.SPANISH -> LocalizedStrings(
                tabPersonalize = "Personalizar",
                tabDeviceTest = "Pruebas",
                tabSecurity = "Seguridad",
                tabCompiler = "Compiladores",
                tabSettings = "Ajustes",
                settingsTitle = "Configuración de Dev Tools",
                languageSection = "Idioma de la Aplicación",
                editorSection = "Preferencias del Editor",
                securityConfigSection = "Configuración de Seguridad",
                resetSection = "Restablecer Configuración",
                resetAllConfig = "Restablecer Todos los Ajustes",
                resetConfirmTitle = "¿Restablecer Configuración?",
                resetConfirmMessage = "Esto devolverá el tema, preferencias, editor y base de datos a sus valores iniciales.",
                fontSizeLabel = "Tamaño de Fuente del Editor",
                aboutTitle = "Acerca de Dev Tools",
                clearCacheTitle = "Limpiar Caché Temporal",
                backupTitle = "Exportar Copia de Seguridad"
            )

            AppLanguage.FRENCH -> LocalizedStrings(
                tabPersonalize = "Personnaliser",
                tabDeviceTest = "Tests",
                tabSecurity = "Sécurité",
                tabCompiler = "Compilateurs",
                tabSettings = "Paramètres",
                settingsTitle = "Paramètres de Dev Tools",
                languageSection = "Langue de l'Application",
                editorSection = "Préférences de l'Éditeur",
                securityConfigSection = "Configuration de Sécurité",
                resetSection = "Réinitialiser la Configuration",
                resetAllConfig = "Réinitialiser Tous les Paramètres",
                resetConfirmTitle = "Réinitialiser la Configuration ?",
                resetConfirmMessage = "Cela restaurera le thème, l'éditeur et la base de données aux valeurs par défaut.",
                fontSizeLabel = "Taille de Police de l'Éditeur",
                aboutTitle = "À Propos de Dev Tools",
                clearCacheTitle = "Vider le Cache Temporaire",
                backupTitle = "Sauvegarder les Données"
            )

            AppLanguage.GERMAN -> LocalizedStrings(
                tabPersonalize = "Anpassen",
                tabDeviceTest = "Hardware-Test",
                tabSecurity = "Sicherheit",
                tabCompiler = "Compiler",
                tabSettings = "Einstellungen",
                settingsTitle = "Dev Tools Einstellungen",
                languageSection = "App-Sprache",
                editorSection = "Editor-Einstellungen",
                securityConfigSection = "Sicherheits-Konfiguration",
                resetSection = "Konfiguration Zurücksetzen",
                resetAllConfig = "Alle Einstellungen Zurücksetzen",
                resetConfirmTitle = "Konfiguration Zurücksetzen?",
                resetConfirmMessage = "Dadurch werden Design, Editor-Einstellungen und Datenbank auf Werkseinstellungen zurückgesetzt.",
                fontSizeLabel = "Editor-Schriftgröße",
                aboutTitle = "Über Dev Tools",
                clearCacheTitle = "Temporären Cache Leeren",
                backupTitle = "Konfiguration Exportieren"
            )

            AppLanguage.JAPANESE -> LocalizedStrings(
                tabPersonalize = "カスタム",
                tabDeviceTest = "端末テスト",
                tabSecurity = "セキュリティ",
                tabCompiler = "コンパイラ",
                tabSettings = "設定",
                settingsTitle = "Dev Tools 設定",
                languageSection = "アプリの言語",
                editorSection = "エディタ設定",
                securityConfigSection = "セキュリティ設定",
                resetSection = "設定のリセット",
                resetAllConfig = "すべての設定を初期化",
                resetConfirmTitle = "設定を初期化しますか？",
                resetConfirmMessage = "テーマ、エディタ設定、コードスニペットが初期状態に戻ります。",
                fontSizeLabel = "エディタのフォントサイズ",
                aboutTitle = "Dev Tools について",
                clearCacheTitle = "キャッシュをクリア",
                backupTitle = "バックアップのエクスポート"
            )

            AppLanguage.PORTUGUESE -> LocalizedStrings(
                tabPersonalize = "Personalizar",
                tabDeviceTest = "Testes",
                tabSecurity = "Segurança",
                tabCompiler = "Compiladores",
                tabSettings = "Configurações",
                settingsTitle = "Configurações do Dev Tools",
                languageSection = "Idioma do Aplicativo",
                editorSection = "Preferências do Editor",
                securityConfigSection = "Configuração de Segurança",
                resetSection = "Redefinir Configuração",
                resetAllConfig = "Redefinir Todas as Configurações",
                resetConfirmTitle = "Redefinir Configuração?",
                resetConfirmMessage = "Isso restaurará o tema, o editor e o banco de dados para os valores padrão.",
                fontSizeLabel = "Tamanho da Fonte do Editor",
                aboutTitle = "Sobre o Dev Tools",
                clearCacheTitle = "Limpar Cache Temporário",
                backupTitle = "Exportar Backup"
            )

            AppLanguage.CHINESE -> LocalizedStrings(
                tabPersonalize = "个性化",
                tabDeviceTest = "硬件测试",
                tabSecurity = "安全防护",
                tabCompiler = "编译器",
                tabSettings = "设置",
                settingsTitle = "Dev Tools 设置",
                languageSection = "应用语言",
                editorSection = "代码编辑器偏好",
                securityConfigSection = "安全配置",
                resetSection = "重置应用配置",
                resetAllConfig = "恢复出厂默认设置",
                resetConfirmTitle = "确认重置所有配置？",
                resetConfirmMessage = "此操作将主题、编辑器代码和本地数据库重置为默认初始状态。",
                fontSizeLabel = "代码字体大小",
                aboutTitle = "关于 Dev Tools",
                clearCacheTitle = "清理临时缓存",
                backupTitle = "导出备份"
            )

            else -> LocalizedStrings(
                tabPersonalize = "Personalize",
                tabDeviceTest = "Device Test",
                tabSecurity = "Security",
                tabCompiler = "Compilers",
                tabSettings = "Settings",
                settingsTitle = "Dev Tools Settings",
                languageSection = "App Language",
                editorSection = "Code Editor Preferences",
                securityConfigSection = "Security & Scanner Config",
                resetSection = "Reset Configuration",
                resetAllConfig = "Reset All to Defaults",
                resetConfirmTitle = "Reset Application Configuration?",
                resetConfirmMessage = "This will reset active themes, editor configurations, clear scan history, and restore default snippets.",
                fontSizeLabel = "Editor Font Size",
                aboutTitle = "About Dev Tools",
                clearCacheTitle = "Clear Temporary App Cache",
                backupTitle = "Export Configuration Backup"
            )
        }
    }
}
