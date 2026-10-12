$files = @(
    'C:\Users\hp\IdeaProjects\untitled\src\com\imtidad\view\Login.fxml',
    'C:\Users\hp\IdeaProjects\untitled\src\com\imtidad\view\ProjectListView.fxml',
    'C:\Users\hp\IdeaProjects\untitled\src\com\imtidad\view\AddProjectWizard.fxml',
    'C:\Users\hp\IdeaProjects\untitled\src\com\imtidad\view\ProjectPassport.fxml',
    'C:\Users\hp\IdeaProjects\untitled\src\com\imtidad\view\ReviewQueue.fxml',
    'C:\Users\hp\IdeaProjects\untitled\src\com\imtidad\view\ProjectLineage.fxml',
    'C:\Users\hp\IdeaProjects\untitled\src\com\imtidad\view\AdminDashboard.fxml'
)
$exitCode = 0
foreach ($f in $files) {
    try {
        [xml](Get-Content $f) | Out-Null
        Write-Host "OK: $f"
    } catch {
        Write-Host "FAIL: $f - $($_.Exception.Message)"
        $exitCode = 1
    }
}
exit $exitCode
