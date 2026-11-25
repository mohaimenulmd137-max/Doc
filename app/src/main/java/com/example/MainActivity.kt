package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.model.AspectRatioPreset
import com.example.ui.screens.AutoDocWizardScreen
import com.example.ui.screens.BrandKitScreen
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.ExportScreen
import com.example.ui.screens.ProjectHubScreen
import com.example.ui.theme.DocuCutTheme
import com.example.viewmodel.StudioViewModel

object DocuCutRoutes {
    const val HUB = "hub"
    const val WIZARD = "wizard"
    const val BRAND_KITS = "brand_kits"
    const val EDITOR = "editor/{projectId}"
    const val EXPORT = "export/{projectId}"

    fun editor(projectId: String) = "editor/$projectId"
    fun export(projectId: String) = "export/$projectId"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DocuCutTheme {
                val navController = rememberNavController()
                val studioViewModel: StudioViewModel = viewModel()
                val projects by studioViewModel.projects.collectAsStateWithLifecycle()
                val brandKits by studioViewModel.brandKits.collectAsStateWithLifecycle()

                NavHost(
                    navController = navController,
                    startDestination = DocuCutRoutes.HUB
                ) {
                    composable(DocuCutRoutes.HUB) {
                        ProjectHubScreen(
                            projects = projects,
                            onNewProject = { theme ->
                                studioViewModel.createEmptyProject(
                                    title = "New Documentary",
                                    theme = theme,
                                    aspectRatio = AspectRatioPreset.RATIO_16_9
                                ) { id ->
                                    navController.navigate(DocuCutRoutes.editor(id))
                                }
                            },
                            onOpenAutoDocWizard = { navController.navigate(DocuCutRoutes.WIZARD) },
                            onOpenBrandKits = { navController.navigate(DocuCutRoutes.BRAND_KITS) },
                            onLoadSampleCase = {
                                studioViewModel.loadSampleCaseProject { id ->
                                    navController.navigate(DocuCutRoutes.editor(id))
                                }
                            },
                            onOpenProject = { id ->
                                navController.navigate(DocuCutRoutes.editor(id))
                            },
                            onDeleteProject = { id -> studioViewModel.deleteProject(id) }
                        )
                    }

                    composable(DocuCutRoutes.WIZARD) {
                        AutoDocWizardScreen(
                            onBack = { navController.popBackStack() },
                            onAssemble = { title, client, theme, ratio, visuals, voices, bgmId, kb, duck, fit ->
                                studioViewModel.createFromAutoDocWizard(
                                    title = title,
                                    clientName = client,
                                    theme = theme,
                                    aspectRatio = ratio,
                                    visualUris = visuals,
                                    voiceUris = voices,
                                    bgmPresetId = bgmId,
                                    autoKenBurns = kb,
                                    autoDucking = duck,
                                    autoFitMode = fit
                                ) { id ->
                                    navController.navigate(DocuCutRoutes.editor(id)) {
                                        popUpTo(DocuCutRoutes.HUB)
                                    }
                                }
                            }
                        )
                    }

                    composable(DocuCutRoutes.BRAND_KITS) {
                        BrandKitScreen(
                            brandKits = brandKits,
                            onBack = { navController.popBackStack() },
                            onSaveKit = { studioViewModel.saveBrandKit(it) },
                            onDeleteKit = { studioViewModel.deleteBrandKit(it) }
                        )
                    }

                    composable(
                        route = DocuCutRoutes.EDITOR,
                        arguments = listOf(navArgument("projectId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable
                        EditorScreen(
                            projectId = projectId,
                            viewModel = studioViewModel,
                            onBack = { navController.popBackStack() },
                            onOpenExport = { id -> navController.navigate(DocuCutRoutes.export(id)) }
                        )
                    }

                    composable(
                        route = DocuCutRoutes.EXPORT,
                        arguments = listOf(navArgument("projectId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable
                        ExportScreen(
                            projectId = projectId,
                            viewModel = studioViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
