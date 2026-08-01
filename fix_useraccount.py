import re

with open("app/src/main/java/com/example/ui/AppViewModel.kt", "r") as f:
    content = f.read()

target = """data class UserAccount(
    @PrimaryKey val id: String,
    val username: String,
    val displayName: String,
    val profilePicUrl: String, // Or gif URL
    val is2FAEnabled: Boolean = false,
    val isActive: Boolean = false,
    val bio: String = "",
    val sessionToken: String? = null,
    val customStatus: String = "",
    val encryptedPasscode: String? = null
)"""

replacement = """data class UserAccount(
    @PrimaryKey val id: String,
    val username: String,
    val displayName: String,
    val profilePicUrl: String, // Or gif URL
    val is2FAEnabled: Boolean = false,
    val isActive: Boolean = false,
    val bio: String = "",
    val sessionToken: String? = null,
    val customStatus: String = "",
    val encryptedPasscode: String? = null,
    val phoneNumber: String = ""
)"""
content = content.replace(target, replacement)

target2 = """                    repository.insertAccount(UserAccount("1", "@neo_hacker", "Neo", "https://i.pravatar.cc/150?img=11", true, true))
                    repository.insertAccount(UserAccount("2", "@synth_wave", "Synth Wave", "https://i.pravatar.cc/150?img=33", false, false))
                    repository.insertAccount(UserAccount("3", "@cyber_punk", "Cyber P.", "https://i.pravatar.cc/150?img=55", false, false))"""

replacement2 = """                    repository.insertAccount(UserAccount("1", "@neo_hacker", "Neo", "https://i.pravatar.cc/150?img=11", true, true, phoneNumber = "+7 (922) 669-26-82"))
                    repository.insertAccount(UserAccount("2", "@synth_wave", "Synth Wave", "https://i.pravatar.cc/150?img=33", false, false, phoneNumber = "+7 (999) 111-22-33"))
                    repository.insertAccount(UserAccount("3", "@cyber_punk", "Cyber P.", "https://i.pravatar.cc/150?img=55", false, false, phoneNumber = "+7 (777) 444-55-66"))"""
content = content.replace(target2, replacement2)

target3 = """    fun createAccount(username: String, displayName: String, bio: String = "", profilePicUrl: String = "", customStatus: String = "") {
        viewModelScope.launch {
            val account = UserAccount(
                id = java.util.UUID.randomUUID().toString(),
                username = username,
                displayName = displayName,
                bio = bio,
                profilePicUrl = profilePicUrl,
                customStatus = customStatus,
                isActive = true
            )"""

replacement3 = """    fun createAccount(phoneNumber: String, username: String, displayName: String, bio: String = "", profilePicUrl: String = "", customStatus: String = "") {
        viewModelScope.launch {
            val account = UserAccount(
                id = java.util.UUID.randomUUID().toString(),
                phoneNumber = phoneNumber,
                username = username,
                displayName = displayName,
                bio = bio,
                profilePicUrl = profilePicUrl,
                customStatus = customStatus,
                isActive = true
            )"""
content = content.replace(target3, replacement3)

with open("app/src/main/java/com/example/ui/AppViewModel.kt", "w") as f:
    f.write(content)
print("Updated AppViewModel.kt")
