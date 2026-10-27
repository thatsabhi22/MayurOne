package com.app.pro.mayurone.ui.navigation

sealed class Screen(val route: String) {
    object Estimates : Screen("estimates")
    object EstimateForm : Screen("estimate_form?estimateId={estimateId}") {
        fun createRoute(estimateId: String? = null): String {
            return if (estimateId != null) "estimate_form?estimateId=$estimateId" else "estimate_form"
        }
    }
    object EstimateDetail : Screen("estimate_detail/{estimateId}") {
        fun createRoute(estimateId: String): String = "estimate_detail/$estimateId"
    }
    object Customers : Screen("customers")
    object Settings : Screen("settings")
}
