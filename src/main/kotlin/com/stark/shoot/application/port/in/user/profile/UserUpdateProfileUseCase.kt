package com.stark.shoot.application.port.`in`.user.profile

import com.stark.shoot.application.port.`in`.user.profile.command.SetBackgroundImageCommand
import com.stark.shoot.application.port.`in`.user.profile.command.SetProfileImageCommand
import com.stark.shoot.application.port.`in`.user.profile.command.UpdateProfileCommand
import com.stark.shoot.domain.user.User

interface UserUpdateProfileUseCase {
    fun updateProfile(command: UpdateProfileCommand): User
    fun setProfileImage(command: SetProfileImageCommand): User
    fun setBackgroundImage(command: SetBackgroundImageCommand): User
}
