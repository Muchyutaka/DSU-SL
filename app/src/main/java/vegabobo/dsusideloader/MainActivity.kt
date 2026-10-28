package vegabobo.dsusideloader

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ipc.RootService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuProvider
import vegabobo.dsusideloader.access.AccessController
import vegabobo.dsusideloader.access.AccessGateState
import vegabobo.dsusideloader.model.Session
import vegabobo.dsusideloader.service.PrivilegedProvider
import vegabobo.dsusideloader.service.PrivilegedRootService
import vegabobo.dsusideloader.service.PrivilegedService
import vegabobo.dsusideloader.service.PrivilegedSystemService
import vegabobo.dsusideloader.ui.screen.Destinations
import vegabobo.dsusideloader.ui.screen.Navigation
import vegabobo.dsusideloader.ui.splash.SplashScreen
import vegabobo.dsusideloader.ui.theme.AppearanceViewModel
import vegabobo.dsusideloader.ui.theme.DSUHelperTheme
import vegabobo.dsusideloader.util.OperationMode
import vegabobo.dsusideloader.util.OperationModeUtils
import vegabobo.dsusideloader.ui.util.motionDuration
import vegabobo.dsusideloader.util.collectAsStateWithLifecycle

@AndroidEntryPoint
class MainActivity : ComponentActivity(), Shizuku.OnRequestPermissionResultListener {

    @Inject
    lateinit var session: Session

    @Inject
    lateinit var accessController: AccessController

    private val appearanceViewModel: AppearanceViewModel by viewModels()

    private val tag = this.javaClass.simpleName

    /** Operation mode the privileged services were bound for, null when none. */
    private var appliedMode: OperationMode? = null

    //
    // Shizuku
    //

    val userServiceArgs =
        Shizuku.UserServiceArgs(
            ComponentName(BuildConfig.APPLICATION_ID, PrivilegedService::class.java.name),
        )
            .daemon(false)
            .processNameSuffix("service")
            .debuggable(BuildConfig.DEBUG)
            .version(BuildConfig.VERSION_CODE)

    private val SHIZUKU_REQUEST_CODE = 1000
    private val REQUEST_PERMISSION_RESULT_LISTENER = this::onRequestPermissionResult

    private fun addShizukuListeners() {
        Shizuku.addBinderReceivedListenerSticky(BINDER_RECEIVED_LISTENER)
        Shizuku.addRequestPermissionResultListener(REQUEST_PERMISSION_RESULT_LISTENER)
    }

    private fun removeShizukuListeners() {
        Shizuku.removeRequestPermissionResultListener(REQUEST_PERMISSION_RESULT_LISTENER)
        Shizuku.removeBinderReceivedListener(BINDER_RECEIVED_LISTENER)
    }

    private val BINDER_RECEIVED_LISTENER = Shizuku.OnBinderReceivedListener {
        if (!OperationModeUtils.isShizukuPermissionGranted(this)) {
            askShizukuPermission()
            return@OnBinderReceivedListener
        }
        bindShizuku()
    }

    private fun askShizukuPermission() {
        if (Shizuku.isPreV11() || Shizuku.getVersion() < 11) {
            requestPermissions(arrayOf(ShizukuProvider.PERMISSION), SHIZUKU_REQUEST_CODE)
        } else {
            Shizuku.requestPermission(SHIZUKU_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
        if (grantResult == PackageManager.PERMISSION_GRANTED && requestCode == SHIZUKU_REQUEST_CODE) {
            bindShizuku()
        }
        Shizuku.removeRequestPermissionResultListener(REQUEST_PERMISSION_RESULT_LISTENER)
    }

    fun bindShizuku() {
        Shizuku.bindUserService(userServiceArgs, PrivilegedProvider.connection)
        // The access method was confirmed by the onboarding screen, do not
        // re-detect it here.
        session.setOperationMode(OperationMode.SHIZUKU)
    }

    //
    // Root
    //

    companion object {
        init {
            // Shell.enableVerboseLogging = BuildConfig.DEBUG
            Shell.setDefaultBuilder(
                Shell.Builder.create()
                    .setFlags(Shell.FLAG_REDIRECT_STDERR)
                    .setTimeout(10),
            )
        }
    }

    //
    // Access method gate
    //

    /**
     * Applies the confirmed access method: sets the operation mode used by the
     * rest of the app and binds the matching privileged service.
     *
     * This replaces the previous silent auto-detection: the user now picks the
     * method in the onboarding screen and it is only applied after a real check.
     */
    private fun applyAccessMode(mode: OperationMode) {
        if (appliedMode == mode) {
            return
        }
        val previousMode = appliedMode
        appliedMode = mode
        session.setOperationMode(mode)
        Log.d(tag, "Applying operation mode: $mode")
        if (previousMode != null) {
            teardownService(previousMode)
        }
        setupService()
    }

    private fun setupService() {
        if (session.isRoot()) {
            val privRootService = Intent(this, PrivilegedRootService::class.java)
            RootService.bind(privRootService, PrivilegedProvider.connection)
            return
        }

        if (session.getOperationMode() == OperationMode.SYSTEM) {
            val service = Intent(this, PrivilegedSystemService::class.java)
            bindService(service, PrivilegedProvider.connection, Context.BIND_AUTO_CREATE)
            return
        }

        if (session.getOperationMode() == OperationMode.SHIZUKU) {
            addShizukuListeners()
        }

        // ADB mode needs no privileged service at all.
    }

    private fun teardownService(mode: OperationMode) {
        try {
            when (mode) {
                OperationMode.ROOT, OperationMode.SYSTEM_AND_ROOT ->
                    RootService.unbind(PrivilegedProvider.connection)

                OperationMode.SYSTEM ->
                    applicationContext.unbindService(PrivilegedProvider.connection)

                OperationMode.SHIZUKU -> {
                    removeShizukuListeners()
                    Shizuku.unbindUserService(userServiceArgs, PrivilegedProvider.connection, true)
                }

                else -> {}
            }
        } catch (e: Exception) {
            // The service may never have been bound (for example when the
            // method was changed before the bind completed).
            Log.w(tag, "Could not unbind $mode service", e)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val appearanceState by appearanceViewModel.uiState.collectAsStateWithLifecycle()
            val gateState by accessController.gateState.collectAsStateWithLifecycle()

            DSUHelperTheme(
                themeMode = appearanceState.themeMode,
                dynamicColor = appearanceState.dynamicColor,
                amoledDark = appearanceState.amoledDark,
            ) {
                // Guarded by rememberSaveable so a configuration change does
                // not replay the launch animation.
                var showSplash by rememberSaveable { mutableStateOf(true) }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                ) {
                    AccessGateContent(gateState)

                    if (showSplash) {
                        SplashScreen(onFinished = { showSplash = false })
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                accessController.gateState.collect { gate ->
                    if (gate is AccessGateState.Confirmed) {
                        applyAccessMode(gate.operationMode)
                    }
                }
            }
        }
    }

    override fun attachBaseContext(newBase: Context?) {
        HiddenApiBypass.addHiddenApiExemptions("")
        super.attachBaseContext(newBase)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isChangingConfigurations) {
            return
        }
        appliedMode?.let { teardownService(it) }
        appliedMode = null
    }
}

/**
 * Switches between the onboarding graph and the main graph.
 *
 * The two graphs are separate [Navigation] instances because the start
 * destination differs, so the swap is animated here: the outgoing graph fades
 * out while sliding left, the incoming one fades in from the right. That is the
 * same pair of animations the onboarding steps use internally.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun AccessGateContent(gateState: AccessGateState) {
    val destination = when (gateState) {
        is AccessGateState.Confirmed -> Destinations.Homepage
        is AccessGateState.NeedsOnboarding -> Destinations.Onboarding
        AccessGateState.Loading -> GATE_LOADING
    }
    val enterMillis = motionDuration(320)
    val exitMillis = motionDuration(280)

    AnimatedContent(
        targetState = destination,
        transitionSpec = {
            (slideInHorizontally(tween(enterMillis)) { it } + fadeIn(tween(enterMillis))) with
                (slideOutHorizontally(tween(exitMillis)) { -it / 4 } + fadeOut(tween(exitMillis)))
        },
    ) { target ->
        if (target == GATE_LOADING) {
            AccessGateLoading()
        } else {
            Navigation(startDestination = target)
        }
    }
}

/** Marker target for the few frames the access gate is still resolving. */
private const val GATE_LOADING = "gate_loading"

/** Shown for the few frames the persisted access method is being validated. */
@Composable
private fun AccessGateLoading() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(36.dp),
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
