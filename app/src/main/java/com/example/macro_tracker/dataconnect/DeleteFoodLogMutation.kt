
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



public interface DeleteFoodLogMutation :
    com.google.firebase.dataconnect.generated.GeneratedMutation<
      NutritrackConnectorConnector,
      DeleteFoodLogMutation.Data,
      DeleteFoodLogMutation.Variables
    >
{
  
    @kotlinx.serialization.Serializable
  public data class Variables(
  
    val id: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.UUIDSerializer::class) java.util.UUID,
  
  ) {
    
    
  }
  

  
    @kotlinx.serialization.Serializable
  public data class Data(
  
    val foodLog_delete: FoodLogKey?,
  
  ) {
    
    
  }
  

  public companion object {
    public val operationName: String = "DeleteFoodLog"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables> =
      kotlinx.serialization.serializer()
  }
}

public fun DeleteFoodLogMutation.ref(
  
    id: java.util.UUID,

  
  
): com.google.firebase.dataconnect.MutationRef<
    DeleteFoodLogMutation.Data,
    DeleteFoodLogMutation.Variables
  > =
  ref(
    
      DeleteFoodLogMutation.Variables(
        id=id,
  
      )
    
  )

public suspend fun DeleteFoodLogMutation.execute(

  
    
      id: java.util.UUID,

  

  ): com.google.firebase.dataconnect.MutationResult<
    DeleteFoodLogMutation.Data,
    DeleteFoodLogMutation.Variables
  > =
  ref(
    
      id=id,
  
    
  ).execute()


