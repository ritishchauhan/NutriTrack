
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


public interface ListHydrationLogsQuery :
    com.google.firebase.dataconnect.generated.GeneratedQuery<
      NutritrackConnectorConnector,
      ListHydrationLogsQuery.Data,
      ListHydrationLogsQuery.Variables
    >
{
  
    @kotlinx.serialization.Serializable
  public data class Variables(
  
    val userId: String,
  
  ) {
    
    
  }
  

  
    @kotlinx.serialization.Serializable
  public data class Data(
  
    val hydrationLogs: List<HydrationLogsItem>,
  
  ) {
    
      
        @kotlinx.serialization.Serializable
  public data class HydrationLogsItem(
  
    val id: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.UUIDSerializer::class) java.util.UUID,
  
    val amountLiters: Double,
  
    val timestamp: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.TimestampSerializer::class) com.google.firebase.Timestamp,
  
  ) {
    
    
  }
      
    
    
  }
  

  public companion object {
    public val operationName: String = "ListHydrationLogs"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables> =
      kotlinx.serialization.serializer()
  }
}

public fun ListHydrationLogsQuery.ref(
  
    userId: String,

  
  
): com.google.firebase.dataconnect.QueryRef<
    ListHydrationLogsQuery.Data,
    ListHydrationLogsQuery.Variables
  > =
  ref(
    
      ListHydrationLogsQuery.Variables(
        userId=userId,
  
      )
    
  )

public suspend fun ListHydrationLogsQuery.execute(

  
    
      userId: String,

  

  ): com.google.firebase.dataconnect.QueryResult<
    ListHydrationLogsQuery.Data,
    ListHydrationLogsQuery.Variables
  > =
  ref(
    
      userId=userId,
  
    
  ).execute()


  public fun ListHydrationLogsQuery.flow(
    
      userId: String,

  
    
    ): kotlinx.coroutines.flow.Flow<ListHydrationLogsQuery.Data> =
    ref(
        
          userId=userId,
  
        
      ).subscribe()
      .flow
      ._flow_map { querySubscriptionResult -> querySubscriptionResult.result.getOrNull() }
      ._flow_filterNotNull()
      ._flow_map { it.data }

