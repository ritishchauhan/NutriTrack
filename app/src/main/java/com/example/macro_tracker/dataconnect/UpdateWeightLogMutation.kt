
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



public interface UpdateWeightLogMutation :
    com.google.firebase.dataconnect.generated.GeneratedMutation<
      NutritrackConnectorConnector,
      UpdateWeightLogMutation.Data,
      UpdateWeightLogMutation.Variables
    >
{
  
    @kotlinx.serialization.Serializable
  public data class Variables(
  
    val id: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.UUIDSerializer::class) java.util.UUID,
  
    val weightKg: Double,
  
    val timestamp: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.TimestampSerializer::class) com.google.firebase.Timestamp,
  
    val note: String,
  
  ) {
    
    
  }
  

  
    @kotlinx.serialization.Serializable
  public data class Data(
  
    val weightLog_update: WeightLogKey?,
  
  ) {
    
    
  }
  

  public companion object {
    public val operationName: String = "UpdateWeightLog"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables> =
      kotlinx.serialization.serializer()
  }
}

public fun UpdateWeightLogMutation.ref(
  
    id: java.util.UUID,weightKg: Double,timestamp: com.google.firebase.Timestamp,note: String,

  
  
): com.google.firebase.dataconnect.MutationRef<
    UpdateWeightLogMutation.Data,
    UpdateWeightLogMutation.Variables
  > =
  ref(
    
      UpdateWeightLogMutation.Variables(
        id=id,weightKg=weightKg,timestamp=timestamp,note=note,
  
      )
    
  )

public suspend fun UpdateWeightLogMutation.execute(

  
    
      id: java.util.UUID,weightKg: Double,timestamp: com.google.firebase.Timestamp,note: String,

  

  ): com.google.firebase.dataconnect.MutationResult<
    UpdateWeightLogMutation.Data,
    UpdateWeightLogMutation.Variables
  > =
  ref(
    
      id=id,weightKg=weightKg,timestamp=timestamp,note=note,
  
    
  ).execute()


