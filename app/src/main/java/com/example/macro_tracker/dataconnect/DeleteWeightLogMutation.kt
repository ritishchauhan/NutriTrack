
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



public interface DeleteWeightLogMutation :
    com.google.firebase.dataconnect.generated.GeneratedMutation<
      NutritrackConnectorConnector,
      DeleteWeightLogMutation.Data,
      DeleteWeightLogMutation.Variables
    >
{
  
    @kotlinx.serialization.Serializable
  public data class Variables(
  
    val id: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.UUIDSerializer::class) java.util.UUID,
  
  ) {
    
    
  }
  

  
    @kotlinx.serialization.Serializable
  public data class Data(
  
    val weightLog_delete: WeightLogKey?,
  
  ) {
    
    
  }
  

  public companion object {
    public val operationName: String = "DeleteWeightLog"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables> =
      kotlinx.serialization.serializer()
  }
}

public fun DeleteWeightLogMutation.ref(
  
    id: java.util.UUID,

  
  
): com.google.firebase.dataconnect.MutationRef<
    DeleteWeightLogMutation.Data,
    DeleteWeightLogMutation.Variables
  > =
  ref(
    
      DeleteWeightLogMutation.Variables(
        id=id,
  
      )
    
  )

public suspend fun DeleteWeightLogMutation.execute(

  
    
      id: java.util.UUID,

  

  ): com.google.firebase.dataconnect.MutationResult<
    DeleteWeightLogMutation.Data,
    DeleteWeightLogMutation.Variables
  > =
  ref(
    
      id=id,
  
    
  ).execute()


