
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


public interface ListFoodLogsQuery :
    com.google.firebase.dataconnect.generated.GeneratedQuery<
      NutritrackConnectorConnector,
      ListFoodLogsQuery.Data,
      ListFoodLogsQuery.Variables
    >
{
  
    @kotlinx.serialization.Serializable
  public data class Variables(
  
    val userId: String,
  
  ) {
    
    
  }
  

  
    @kotlinx.serialization.Serializable
  public data class Data(
  
    val foodLogs: List<FoodLogsItem>,
  
  ) {
    
      
        @kotlinx.serialization.Serializable
  public data class FoodLogsItem(
  
    val id: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.UUIDSerializer::class) java.util.UUID,
  
    val foodName: String,
  
    val calories: Double,
  
    val protein: Double,
  
    val carbs: Double,
  
    val fat: Double,
  
    val fiber: Double,
  
    val portionMultiplier: Double,
  
    val mealType: String,
  
    val barcode: String?,
  
    val imageUrl: String?,
  
    val timestamp: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.TimestampSerializer::class) com.google.firebase.Timestamp,
  
    val details: String,
  
  ) {
    
    
  }
      
    
    
  }
  

  public companion object {
    public val operationName: String = "ListFoodLogs"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables> =
      kotlinx.serialization.serializer()
  }
}

public fun ListFoodLogsQuery.ref(
  
    userId: String,

  
  
): com.google.firebase.dataconnect.QueryRef<
    ListFoodLogsQuery.Data,
    ListFoodLogsQuery.Variables
  > =
  ref(
    
      ListFoodLogsQuery.Variables(
        userId=userId,
  
      )
    
  )

public suspend fun ListFoodLogsQuery.execute(

  
    
      userId: String,

  

  ): com.google.firebase.dataconnect.QueryResult<
    ListFoodLogsQuery.Data,
    ListFoodLogsQuery.Variables
  > =
  ref(
    
      userId=userId,
  
    
  ).execute()


  public fun ListFoodLogsQuery.flow(
    
      userId: String,

  
    
    ): kotlinx.coroutines.flow.Flow<ListFoodLogsQuery.Data> =
    ref(
        
          userId=userId,
  
        
      ).subscribe()
      .flow
      ._flow_map { querySubscriptionResult -> querySubscriptionResult.result.getOrNull() }
      ._flow_filterNotNull()
      ._flow_map { it.data }

