package model

enum class Role {
    ROLE_USER,
    ROLE_ADMIN,
    ROLE_MANAGER,
    ;

    companion object {
        fun from(name: String): Role = Role.valueOf(name)
    }
}
