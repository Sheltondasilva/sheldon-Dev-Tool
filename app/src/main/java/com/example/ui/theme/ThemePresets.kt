package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

enum class ThemePreset(
    val title: String,
    val description: String,
    val previewPrimary: Color,
    val previewAccent: Color,
    val previewBg: Color
) {
    CYBERPUNK(
        title = "Cyberpunk Neon",
        description = "High-contrast glowing cyan & neon magenta",
        previewPrimary = Color(0xFF00F0FF),
        previewAccent = Color(0xFFFF007F),
        previewBg = Color(0xFF0D111A)
    ),
    MATRIX(
        title = "Matrix Terminal",
        description = "Classic phosphorescent terminal green",
        previewPrimary = Color(0xFF00FF66),
        previewAccent = Color(0xFF00AA44),
        previewBg = Color(0xFF050E07)
    ),
    DRACULA(
        title = "Dracula Twilight",
        description = "Dark purple, soft pink & vibrant green",
        previewPrimary = Color(0xFFBD93F9),
        previewAccent = Color(0xFFFF79C6),
        previewBg = Color(0xFF1E1F29)
    ),
    MONOKAI(
        title = "Monokai Pro",
        description = "Warm amber yellow, coral & cyan accents",
        previewPrimary = Color(0xFFFFD866),
        previewAccent = Color(0xFFFF6188),
        previewBg = Color(0xFF191A1C)
    ),
    NORD(
        title = "Nord Arctic",
        description = "Cool arctic blue & elegant polar frost",
        previewPrimary = Color(0xFF88C0D0),
        previewAccent = Color(0xFF81A1C1),
        previewBg = Color(0xFF242933)
    ),
    AMOLED(
        title = "AMOLED Pitch Black",
        description = "Pure black background for maximum battery saver",
        previewPrimary = Color(0xFF3B82F6),
        previewAccent = Color(0xFF10B981),
        previewBg = Color(0xFF000000)
    )
}

object DevColorSchemes {

    fun getColorScheme(preset: ThemePreset, isDark: Boolean): ColorScheme {
        return when (preset) {
            ThemePreset.CYBERPUNK -> darkColorScheme(
                primary = Color(0xFF00F0FF),
                onPrimary = Color(0xFF00363A),
                primaryContainer = Color(0xFF004F55),
                onPrimaryContainer = Color(0xFF97F0FF),
                secondary = Color(0xFFFF007F),
                onSecondary = Color(0xFF490022),
                secondaryContainer = Color(0xFF6B0033),
                onSecondaryContainer = Color(0xFFFFD9E2),
                tertiary = Color(0xFFFFE600),
                background = Color(0xFF0B0F19),
                onBackground = Color(0xFFE2E8F0),
                surface = Color(0xFF111827),
                onSurface = Color(0xFFF1F5F9),
                surfaceVariant = Color(0xFF1F2937),
                onSurfaceVariant = Color(0xFF94A3B8),
                outline = Color(0xFF374151)
            )

            ThemePreset.MATRIX -> darkColorScheme(
                primary = Color(0xFF00FF66),
                onPrimary = Color(0xFF003912),
                primaryContainer = Color(0xFF00531D),
                onPrimaryContainer = Color(0xFF6CFF8C),
                secondary = Color(0xFF39D353),
                onSecondary = Color(0xFF003A10),
                secondaryContainer = Color(0xFF00531B),
                onSecondaryContainer = Color(0xFF5AFF7D),
                tertiary = Color(0xFF26A641),
                background = Color(0xFF050E07),
                onBackground = Color(0xFFE2FBE6),
                surface = Color(0xFF0A180E),
                onSurface = Color(0xFFE2FBE6),
                surfaceVariant = Color(0xFF132718),
                onSurfaceVariant = Color(0xFF86A88E),
                outline = Color(0xFF1B3D23)
            )

            ThemePreset.DRACULA -> darkColorScheme(
                primary = Color(0xFFBD93F9),
                onPrimary = Color(0xFF2E1955),
                primaryContainer = Color(0xFF452D75),
                onPrimaryContainer = Color(0xFFEADBFF),
                secondary = Color(0xFFFF79C6),
                onSecondary = Color(0xFF520038),
                secondaryContainer = Color(0xFF751554),
                onSecondaryContainer = Color(0xFFFFD7EE),
                tertiary = Color(0xFF50FA7B),
                background = Color(0xFF1E1F29),
                onBackground = Color(0xFFF8F8F2),
                surface = Color(0xFF282A36),
                onSurface = Color(0xFFF8F8F2),
                surfaceVariant = Color(0xFF383A59),
                onSurfaceVariant = Color(0xFFBDC0CF),
                outline = Color(0xFF44475A)
            )

            ThemePreset.MONOKAI -> darkColorScheme(
                primary = Color(0xFFFFD866),
                onPrimary = Color(0xFF422C00),
                primaryContainer = Color(0xFF5E4100),
                onPrimaryContainer = Color(0xFFFFE196),
                secondary = Color(0xFFFF6188),
                onSecondary = Color(0xFF51001B),
                secondaryContainer = Color(0xFF74122F),
                onSecondaryContainer = Color(0xFFFFD9DF),
                tertiary = Color(0xFF78DCE8),
                background = Color(0xFF191A1C),
                onBackground = Color(0xFFFCFCFA),
                surface = Color(0xFF222426),
                onSurface = Color(0xFFFCFCFA),
                surfaceVariant = Color(0xFF2D3139),
                onSurfaceVariant = Color(0xFFC1C2C5),
                outline = Color(0xFF40444B)
            )

            ThemePreset.NORD -> darkColorScheme(
                primary = Color(0xFF88C0D0),
                onPrimary = Color(0xFF003543),
                primaryContainer = Color(0xFF004D60),
                onPrimaryContainer = Color(0xFFBBE9FF),
                secondary = Color(0xFF81A1C1),
                onSecondary = Color(0xFF003258),
                secondaryContainer = Color(0xFF194977),
                onSecondaryContainer = Color(0xFFD1E4FF),
                tertiary = Color(0xFFB48EAD),
                background = Color(0xFF242933),
                onBackground = Color(0xFFECEFF4),
                surface = Color(0xFF2E3440),
                onSurface = Color(0xFFECEFF4),
                surfaceVariant = Color(0xFF3B4252),
                onSurfaceVariant = Color(0xFFD8DEE9),
                outline = Color(0xFF4C566A)
            )

            ThemePreset.AMOLED -> darkColorScheme(
                primary = Color(0xFF3B82F6),
                onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFF1E3A8A),
                onPrimaryContainer = Color(0xFFDBEAFE),
                secondary = Color(0xFF10B981),
                onSecondary = Color(0xFFFFFFFF),
                secondaryContainer = Color(0xFF064E3B),
                onSecondaryContainer = Color(0xFFD1FAE5),
                tertiary = Color(0xFFF59E0B),
                background = Color(0xFF000000),
                onBackground = Color(0xFFFFFFFF),
                surface = Color(0xFF0A0A0A),
                onSurface = Color(0xFFEDEDED),
                surfaceVariant = Color(0xFF171717),
                onSurfaceVariant = Color(0xFFA3A3A3),
                outline = Color(0xFF262626)
            )
        }
    }
}
