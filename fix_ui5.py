with open("app/src/main/java/com/example/ui/StorageSettingsScreen.kt", "r") as f:
    content = f.read()

# Find SettingsStorageScreen definition
start_idx = content.find('fun SettingsStorageScreen(viewModel: AppViewModel, navController: NavController) {')
if start_idx != -1:
    # Insert snackbarHostState after the first few variables
    scaffold_idx = content.find('    Scaffold(', start_idx)
    if scaffold_idx != -1:
        insert_vars = """    val snackbarHostState = remember { SnackbarHostState() }
"""
        content = content[:scaffold_idx] + insert_vars + content[scaffold_idx:]
        
        # Now update Scaffold
        # Before: Scaffold(\n        topBar = {
        # After: Scaffold(\n        snackbarHost = { SnackbarHost(snackbarHostState) },\n        topBar = {
        scaffold_idx = content.find('    Scaffold(\n        topBar = {', start_idx)
        if scaffold_idx != -1:
            replacement = '    Scaffold(\n        snackbarHost = { SnackbarHost(snackbarHostState) },\n        topBar = {'
            content = content[:scaffold_idx] + replacement + content[scaffold_idx + len('    Scaffold(\n        topBar = {'):]

with open("app/src/main/java/com/example/ui/StorageSettingsScreen.kt", "w") as f:
    f.write(content)
