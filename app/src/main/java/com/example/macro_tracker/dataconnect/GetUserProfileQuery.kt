
@file:Suppress(
  "KotlinRedundantDiagnosticSuppress",
  "PropertyName",
  "MayBeConstant",
  "RedundantVisibilityModifier",
  "RedundantCompanionReference",
  "RemoveEmptyClassBody",
  "SpellCheckingInspection",
  "unused",
)

package com.example.macro_tracker.dataconnect


import kotlinx.coroutines.flow.filterNotNull as _flow_filterNotNull
import kotlinx.coroutines.flow.map as _flow_map


public interface GetUserProfileQuery :
    com.google.firebase.dataconnect.generated.GeneratedQuery<
      NutritrackConnectorConnector,
      GetUserProfileQuery.Data,
      GetUserProfileQuery.Variables
    >
{
  
    @kotlinx.serialization.Serializable
  public data class Variables(
  
    val userId: String,
  
  ) {
    
    
  }
  

  
    @kotlinx.serialization.Serializable
  public data class Data(
  
    val userProfile: UserProfile?,
  
  ) {
    
      
        @kotlinx.serialization.Serializable
  public data class UserProfile(
  
    val userId: String,
  
    val name: String?,
  
    val email: String?,
  
    val calorieTarget: Int,
  
    val proteinTarget: Int,
  
    val carbsTarget: Int,
  
    val fatTarget: Int,
  
    val fiberTarget: Int,
  
    val waterTarget: Double,
  
    val waterLogged: Double,
  
    val streakDays: Int,
  
    val weightKg: Double,
  
    val heightCm: Double,
  
    val fitnessGoal: String,
  
    val dietaryPreference: String?,
  
    val activityLevel: String?,
  
    val updatedAt: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.TimestampSerializer::class) com.google.firebase.Timestamp,
  
  ) {
    
    
  }
      
    
    
  }
  

  public companion object {
    public val operationName: String = "GetUserProfile"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables> =
      kotlinx.serialization.serializer()
  }
}

public fun GetUserProfileQuery.ref(
  
    userId: String,

  
  
): com.google.firebase.dataconnect.QueryRef<
    GetUserProfileQuery.Data,
    GetUserProfileQuery.Variables
  > =
  ref(
    
      GetUserProfileQuery.Variables(
        userId=userId,
  
      )
    
  )

public suspend fun GetUserProfileQuery.execute(

  
    
      userId: String,

  

  ): com.google.firebase.dataconnect.QueryResult<
    GetUserProfileQuery.Data,
    GetUserProfileQuery.Variables
  > =
  ref(
    
      userId=userId,
  
    
  ).execute()


  public fun GetUserProfileQuery.flow(
    
      userId: String,

  
    
    ): kotlinx.coroutines.flow.Flow<GetUserProfileQuery.Data> =
    ref(
        
          userId=userId,
  
        
      ).subscribe()
      .flow
      ._flow_map { querySubscriptionResult -> querySubscriptionResult.result.getOrNull() }
      ._flow_filterNotNull()
      ._flow_map { it.data }

