
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



public interface CreateWeightLogMutation :
    com.google.firebase.dataconnect.generated.GeneratedMutation<
      NutritrackConnectorConnector,
      CreateWeightLogMutation.Data,
      CreateWeightLogMutation.Variables
    >
{
  
    @kotlinx.serialization.Serializable
  public data class Variables(
  
    val userId: String,
  
    val weightKg: Double,
  
    val timestamp: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.TimestampSerializer::class) com.google.firebase.Timestamp,
  
    val note: String,
  
  ) {
    
    
  }
  

  
    @kotlinx.serialization.Serializable
  public data class Data(
  
    val weightLog_insert: WeightLogKey,
  
  ) {
    
    
  }
  

  public companion object {
    public val operationName: String = "CreateWeightLog"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables> =
      kotlinx.serialization.serializer()
  }
}

public fun CreateWeightLogMutation.ref(
  
    userId: String,weightKg: Double,timestamp: com.google.firebase.Timestamp,note: String,

  
  
): com.google.firebase.dataconnect.MutationRef<
    CreateWeightLogMutation.Data,
    CreateWeightLogMutation.Variables
  > =
  ref(
    
      CreateWeightLogMutation.Variables(
        userId=userId,weightKg=weightKg,timestamp=timestamp,note=note,
  
      )
    
  )

public suspend fun CreateWeightLogMutation.execute(

  
    
      userId: String,weightKg: Double,timestamp: com.google.firebase.Timestamp,note: String,

  

  ): com.google.firebase.dataconnect.MutationResult<
    CreateWeightLogMutation.Data,
    CreateWeightLogMutation.Variables
  > =
  ref(
    
      userId=userId,weightKg=weightKg,timestamp=timestamp,note=note,
  
    
  ).execute()


