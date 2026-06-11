package paspo.id.ssoprovider.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import paspo.id.ssoprovider.ui.PaspoButtonTheme

@Preview
@Composable
private fun PaspoSignInButtonThemesPreview() {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PaspoButtonTheme.entries.forEach { theme ->
            PaspoSignInButtonContent(onClick = {}, theme = theme)
        }
    }
}

@Preview
@Composable
private fun PaspoSignInButtonCornersPreview() {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PaspoSignInButtonContent(onClick = {})
        PaspoSignInButtonContent(onClick = {}, cornerRadius = 12.dp)
        PaspoSignInButtonContent(onClick = {}, cornerRadius = 0.dp)
    }
}

@Preview
@Composable
private fun PaspoSignInButtonIconOnlyPreview() {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PaspoSignInButtonContent(onClick = {}, iconOnly = true)
        PaspoSignInButtonContent(onClick = {}, theme = PaspoButtonTheme.LIGHT, iconOnly = true)
    }
}

@Preview
@Composable
private fun PaspoSignInButtonDisabledPreview() {
    PaspoSignInButtonContent(onClick = {}, enabled = false, modifier = Modifier.padding(16.dp))
}
