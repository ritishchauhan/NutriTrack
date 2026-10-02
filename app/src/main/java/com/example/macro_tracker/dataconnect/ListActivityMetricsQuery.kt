
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


public interface ListActivityMetricsQuery :
    com.google.firebase.dataconnect.generated.GeneratedQuery<
      NutritrackConnectorConnector,
      ListActivityMetricsQuery.Data,
      ListActivityMetricsQuery.Variables
    >
{
  
    @kotlinx.serialization.Serializable
  public data class Variables(
  
    val userId: String,
  
  ) {
    
    
  }
  

  
    @kotlinx.serialization.Serializable
  public data class Data(
  
    val activityMetrics: List<ActivityMetricsItem>,
  
  ) {
    
      
        @kotlinx.serialization.Serializable
  public data class ActivityMetricsItem(
  
    val id: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.UUIDSerializer::class) java.util.UUID,
  
    val recordType: String,
  
    val value: Double,
  
    val date: com.google.firebase.dataconnect.LocalDate,
  
  ) {
    
    
  }
      
    
    
  }
  

  public companion object {
    public val operationName: String = "ListActivityMetrics"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables> =
      kotlinx.serialization.serializer()
  }
}

public fun ListActivityMetricsQuery.ref(
  
    userId: String,

  
  
): com.google.firebase.dataconnect.QueryRef<
    ListActivityMetricsQuery.Data,
    ListActivityMetricsQuery.Variables
  > =
  ref(
    
      ListActivityMetricsQuery.Variables(
        userId=userId,
  
      )
    
  )

public suspend fun ListActivityMetricsQuery.execute(

  
    
      userId: String,

  

  ): com.google.firebase.dataconnect.QueryResult<
    ListActivityMetricsQuery.Data,
    ListActivityMetricsQuery.Variables
  > =
  ref(
    
      userId=userId,
  
    
  ).execute()


  public fun ListActivityMetricsQuery.flow(
    
      userId: String,

  
    
    ): kotlinx.coroutines.flow.Flow<ListActivityMetricsQuery.Data> =
    ref(
        
          userId=userId,
  
        
      ).subscribe()
      .flow
      ._flow_map { querySubscriptionResult -> querySubscriptionResult.result.getOrNull() }
      ._flow_filterNotNull()
      ._flow_map { it.data }

