
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


public interface ListWeightLogsQuery :
    com.google.firebase.dataconnect.generated.GeneratedQuery<
      NutritrackConnectorConnector,
      ListWeightLogsQuery.Data,
      ListWeightLogsQuery.Variables
    >
{
  
    @kotlinx.serialization.Serializable
  public data class Variables(
  
    val userId: String,
  
  ) {
    
    
  }
  

  
    @kotlinx.serialization.Serializable
  public data class Data(
  
    val weightLogs: List<WeightLogsItem>,
  
  ) {
    
      
        @kotlinx.serialization.Serializable
  public data class WeightLogsItem(
  
    val id: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.UUIDSerializer::class) java.util.UUID,
  
    val weightKg: Double,
  
    val timestamp: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.TimestampSerializer::class) com.google.firebase.Timestamp,
  
    val note: String,
  
  ) {
    
    
  }
      
    
    
  }
  

  public companion object {
    public val operationName: String = "ListWeightLogs"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables> =
      kotlinx.serialization.serializer()
  }
}

public fun ListWeightLogsQuery.ref(
  
    userId: String,

  
  
): com.google.firebase.dataconnect.QueryRef<
    ListWeightLogsQuery.Data,
    ListWeightLogsQuery.Variables
  > =
  ref(
    
      ListWeightLogsQuery.Variables(
        userId=userId,
  
      )
    
  )

public suspend fun ListWeightLogsQuery.execute(

  
    
      userId: String,

  

  ): com.google.firebase.dataconnect.QueryResult<
    ListWeightLogsQuery.Data,
    ListWeightLogsQuery.Variables
  > =
  ref(
    
      userId=userId,
  
    
  ).execute()


  public fun ListWeightLogsQuery.flow(
    
      userId: String,

  
    
    ): kotlinx.coroutines.flow.Flow<ListWeightLogsQuery.Data> =
    ref(
        
          userId=userId,
  
        
      ).subscribe()
      .flow
      ._flow_map { querySubscriptionResult -> querySubscriptionResult.result.getOrNull() }
      ._flow_filterNotNull()
      ._flow_map { it.data }

