package ee.tallinntastebuds.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import ee.tallinntastebuds.service.LocationProvider

/**
 * Ask for a fix, asking for the permission first if it has not been granted.
 *
 * Returned as one function because every caller wants the same thing and none of
 * them wants to know which half it is doing. A refusal needs no branch of its
 * own: [LocationProvider.request] already fails the same way whether the reader
 * said no or the phone has location switched off, and the reader is told the
 * same thing either way.
 */
@Composable
fun rememberLocationRequest(provider: LocationProvider): () -> Unit {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { provider.request() }

    return remember(provider, launcher) {
        {
            if (provider.hasPermission) provider.request()
            else launcher.launch(LocationProvider.PERMISSIONS)
        }
    }
}
