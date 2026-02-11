package com.anonforge.domain.usecase

import com.anonforge.domain.model.DomainIdentity
import com.anonforge.domain.repository.IdentityRepository
import kotlinx.datetime.Clock
import javax.inject.Inject

class SaveIdentityUseCase @Inject constructor(
    private val repository: IdentityRepository
) {
    suspend operator fun invoke(identity: DomainIdentity): Result<Unit> {
        return try {
            identity.expiresAt?.let { expiresAt ->
                require(expiresAt > Clock.System.now()) {
                    "Expiry must be in the future"
                }
            }
            
            repository.insertIdentity(identity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
