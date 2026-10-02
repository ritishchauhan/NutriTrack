
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

import com.google.firebase.dataconnect.getInstance as _fdcGetInstance
import kotlin.time.Duration.Companion.milliseconds as _milliseconds

public interface NutritrackConnectorConnector : com.google.firebase.dataconnect.generated.GeneratedConnector<NutritrackConnectorConnector> {
  override val dataConnect: com.google.firebase.dataconnect.FirebaseDataConnect

  
    public val createFoodLog: CreateFoodLogMutation
  
    public val createWeightLog: CreateWeightLogMutation
  
    public val deleteFoodLog: DeleteFoodLogMutation
  
    public val deleteWeightLog: DeleteWeightLogMutation
  
    public val getUserProfile: GetUserProfileQuery
  
    public val listActivityMetrics: ListActivityMetricsQuery
  
    public val listFoodLogs: ListFoodLogsQuery
  
    public val listHydrationLogs: ListHydrationLogsQuery
  
    public val listWeightLogs: ListWeightLogsQuery
  
    public val updateWeightLog: UpdateWeightLogMutation
  
    public val upsertUserProfile: UpsertUserProfileMutation
  

  public companion object {
    @Suppress("MemberVisibilityCanBePrivate")
    public val config: com.google.firebase.dataconnect.ConnectorConfig = com.google.firebase.dataconnect.ConnectorConfig(
      connector = "nutritrack-connector",
      location = "asia-southeast1",
      serviceId = "nutritrack-service",
    )

    public fun getInstance(
      dataConnect: com.google.firebase.dataconnect.FirebaseDataConnect
    ):NutritrackConnectorConnector = synchronized(instances) {
      instances.getOrPut(dataConnect) {
        NutritrackConnectorConnectorImpl(dataConnect)
      }
    }

    private val instances = java.util.WeakHashMap<com.google.firebase.dataconnect.FirebaseDataConnect, NutritrackConnectorConnectorImpl>()

    
  }
}

public val NutritrackConnectorConnector.Companion.instance:NutritrackConnectorConnector
  get() = getInstance(com.google.firebase.dataconnect.FirebaseDataConnect._fdcGetInstance(
    config
  ))

public fun NutritrackConnectorConnector.Companion.getInstance(
  settings: com.google.firebase.dataconnect.DataConnectSettings = com.google.firebase.dataconnect.DataConnectSettings()
):NutritrackConnectorConnector =
  getInstance(com.google.firebase.dataconnect.FirebaseDataConnect._fdcGetInstance(config, settings))

public fun NutritrackConnectorConnector.Companion.getInstance(
  app: com.google.firebase.FirebaseApp,
  settings: com.google.firebase.dataconnect.DataConnectSettings = com.google.firebase.dataconnect.DataConnectSettings()
):NutritrackConnectorConnector =
  getInstance(com.google.firebase.dataconnect.FirebaseDataConnect._fdcGetInstance(app, config, settings))

private class NutritrackConnectorConnectorImpl(
  override val dataConnect: com.google.firebase.dataconnect.FirebaseDataConnect
) : NutritrackConnectorConnector {
  
    override val createFoodLog by lazy(LazyThreadSafetyMode.PUBLICATION) {
      CreateFoodLogMutationImpl(this)
    }
  
    override val createWeightLog by lazy(LazyThreadSafetyMode.PUBLICATION) {
      CreateWeightLogMutationImpl(this)
    }
  
    override val deleteFoodLog by lazy(LazyThreadSafetyMode.PUBLICATION) {
      DeleteFoodLogMutationImpl(this)
    }
  
    override val deleteWeightLog by lazy(LazyThreadSafetyMode.PUBLICATION) {
      DeleteWeightLogMutationImpl(this)
    }
  
    override val getUserProfile by lazy(LazyThreadSafetyMode.PUBLICATION) {
      GetUserProfileQueryImpl(this)
    }
  
    override val listActivityMetrics by lazy(LazyThreadSafetyMode.PUBLICATION) {
      ListActivityMetricsQueryImpl(this)
    }
  
    override val listFoodLogs by lazy(LazyThreadSafetyMode.PUBLICATION) {
      ListFoodLogsQueryImpl(this)
    }
  
    override val listHydrationLogs by lazy(LazyThreadSafetyMode.PUBLICATION) {
      ListHydrationLogsQueryImpl(this)
    }
  
    override val listWeightLogs by lazy(LazyThreadSafetyMode.PUBLICATION) {
      ListWeightLogsQueryImpl(this)
    }
  
    override val updateWeightLog by lazy(LazyThreadSafetyMode.PUBLICATION) {
      UpdateWeightLogMutationImpl(this)
    }
  
    override val upsertUserProfile by lazy(LazyThreadSafetyMode.PUBLICATION) {
      UpsertUserProfileMutationImpl(this)
    }
  

  @com.google.firebase.dataconnect.ExperimentalFirebaseDataConnect
  override fun operations(): List<com.google.firebase.dataconnect.generated.GeneratedOperation<NutritrackConnectorConnector, *, *>> =
    queries() + mutations()

  @com.google.firebase.dataconnect.ExperimentalFirebaseDataConnect
  override fun mutations(): List<com.google.firebase.dataconnect.generated.GeneratedMutation<NutritrackConnectorConnector, *, *>> =
    listOf(
      createFoodLog,
        createWeightLog,
        deleteFoodLog,
        deleteWeightLog,
        updateWeightLog,
        upsertUserProfile,
        
    )

  @com.google.firebase.dataconnect.ExperimentalFirebaseDataConnect
  override fun queries(): List<com.google.firebase.dataconnect.generated.GeneratedQuery<NutritrackConnectorConnector, *, *>> =
    listOf(
      getUserProfile,
        listActivityMetrics,
        listFoodLogs,
        listHydrationLogs,
        listWeightLogs,
        
    )

  @com.google.firebase.dataconnect.ExperimentalFirebaseDataConnect
  override fun copy(dataConnect: com.google.firebase.dataconnect.FirebaseDataConnect) =
    NutritrackConnectorConnectorImpl(dataConnect)

  override fun equals(other: Any?): Boolean =
    other is NutritrackConnectorConnectorImpl &&
    other.dataConnect == dataConnect

  override fun hashCode(): Int =
    java.util.Objects.hash(
      "NutritrackConnectorConnectorImpl",
      dataConnect,
    )

  override fun toString(): String =
    "NutritrackConnectorConnectorImpl(dataConnect=$dataConnect)"
}



private open class NutritrackConnectorConnectorGeneratedQueryImpl<Data, Variables>(
  override val connector: NutritrackConnectorConnector,
  override val operationName: String,
  override val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data>,
  override val variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables>,
) : com.google.firebase.dataconnect.generated.GeneratedQuery<NutritrackConnectorConnector, Data, Variables> {

  @com.google.firebase.dataconnect.ExperimentalFirebaseDataConnect
  override fun copy(
    connector: NutritrackConnectorConnector,
    operationName: String,
    dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data>,
    variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables>,
  ) =
    NutritrackConnectorConnectorGeneratedQueryImpl(
      connector, operationName, dataDeserializer, variablesSerializer
    )

  @com.google.firebase.dataconnect.ExperimentalFirebaseDataConnect
  override fun <NewVariables> withVariablesSerializer(
    variablesSerializer: kotlinx.serialization.SerializationStrategy<NewVariables>
  ) =
    NutritrackConnectorConnectorGeneratedQueryImpl(
      connector, operationName, dataDeserializer, variablesSerializer
    )

  @com.google.firebase.dataconnect.ExperimentalFirebaseDataConnect
  override fun <NewData> withDataDeserializer(
    dataDeserializer: kotlinx.serialization.DeserializationStrategy<NewData>
  ) =
    NutritrackConnectorConnectorGeneratedQueryImpl(
      connector, operationName, dataDeserializer, variablesSerializer
    )

  override fun equals(other: Any?): Boolean =
    other is NutritrackConnectorConnectorGeneratedQueryImpl<*,*> &&
    other.connector == connector &&
    other.operationName == operationName &&
    other.dataDeserializer == dataDeserializer &&
    other.variablesSerializer == variablesSerializer

  override fun hashCode(): Int =
    java.util.Objects.hash(
      "NutritrackConnectorConnectorGeneratedQueryImpl",
      connector, operationName, dataDeserializer, variablesSerializer
    )

  override fun toString(): String =
    "NutritrackConnectorConnectorGeneratedQueryImpl(" +
    "operationName=$operationName, " +
    "dataDeserializer=$dataDeserializer, " +
    "variablesSerializer=$variablesSerializer, " +
    "connector=$connector)"
}

private open class NutritrackConnectorConnectorGeneratedMutationImpl<Data, Variables>(
  override val connector: NutritrackConnectorConnector,
  override val operationName: String,
  override val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data>,
  override val variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables>,
) : com.google.firebase.dataconnect.generated.GeneratedMutation<NutritrackConnectorConnector, Data, Variables> {

  @com.google.firebase.dataconnect.ExperimentalFirebaseDataConnect
  override fun copy(
    connector: NutritrackConnectorConnector,
    operationName: String,
    dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data>,
    variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables>,
  ) =
    NutritrackConnectorConnectorGeneratedMutationImpl(
      connector, operationName, dataDeserializer, variablesSerializer
    )

  @com.google.firebase.dataconnect.ExperimentalFirebaseDataConnect
  override fun <NewVariables> withVariablesSerializer(
    variablesSerializer: kotlinx.serialization.SerializationStrategy<NewVariables>
  ) =
    NutritrackConnectorConnectorGeneratedMutationImpl(
      connector, operationName, dataDeserializer, variablesSerializer
    )

  @com.google.firebase.dataconnect.ExperimentalFirebaseDataConnect
  override fun <NewData> withDataDeserializer(
    dataDeserializer: kotlinx.serialization.DeserializationStrategy<NewData>
  ) =
    NutritrackConnectorConnectorGeneratedMutationImpl(
      connector, operationName, dataDeserializer, variablesSerializer
    )

  override fun equals(other: Any?): Boolean =
    other is NutritrackConnectorConnectorGeneratedMutationImpl<*,*> &&
    other.connector == connector &&
    other.operationName == operationName &&
    other.dataDeserializer == dataDeserializer &&
    other.variablesSerializer == variablesSerializer

  override fun hashCode(): Int =
    java.util.Objects.hash(
      "NutritrackConnectorConnectorGeneratedMutationImpl",
      connector, operationName, dataDeserializer, variablesSerializer
    )

  override fun toString(): String =
    "NutritrackConnectorConnectorGeneratedMutationImpl(" +
    "operationName=$operationName, " +
    "dataDeserializer=$dataDeserializer, " +
    "variablesSerializer=$variablesSerializer, " +
    "connector=$connector)"
}



private class CreateFoodLogMutationImpl(
  connector: NutritrackConnectorConnector
):
  CreateFoodLogMutation,
  NutritrackConnectorConnectorGeneratedMutationImpl<
      CreateFoodLogMutation.Data,
      CreateFoodLogMutation.Variables
  >(
    connector,
    CreateFoodLogMutation.Companion.operationName,
    CreateFoodLogMutation.Companion.dataDeserializer,
    CreateFoodLogMutation.Companion.variablesSerializer,
  )


private class CreateWeightLogMutationImpl(
  connector: NutritrackConnectorConnector
):
  CreateWeightLogMutation,
  NutritrackConnectorConnectorGeneratedMutationImpl<
      CreateWeightLogMutation.Data,
      CreateWeightLogMutation.Variables
  >(
    connector,
    CreateWeightLogMutation.Companion.operationName,
    CreateWeightLogMutation.Companion.dataDeserializer,
    CreateWeightLogMutation.Companion.variablesSerializer,
  )


private class DeleteFoodLogMutationImpl(
  connector: NutritrackConnectorConnector
):
  DeleteFoodLogMutation,
  NutritrackConnectorConnectorGeneratedMutationImpl<
      DeleteFoodLogMutation.Data,
      DeleteFoodLogMutation.Variables
  >(
    connector,
    DeleteFoodLogMutation.Companion.operationName,
    DeleteFoodLogMutation.Companion.dataDeserializer,
    DeleteFoodLogMutation.Companion.variablesSerializer,
  )


private class DeleteWeightLogMutationImpl(
  connector: NutritrackConnectorConnector
):
  DeleteWeightLogMutation,
  NutritrackConnectorConnectorGeneratedMutationImpl<
      DeleteWeightLogMutation.Data,
      DeleteWeightLogMutation.Variables
  >(
    connector,
    DeleteWeightLogMutation.Companion.operationName,
    DeleteWeightLogMutation.Companion.dataDeserializer,
    DeleteWeightLogMutation.Companion.variablesSerializer,
  )


private class GetUserProfileQueryImpl(
  connector: NutritrackConnectorConnector
):
  GetUserProfileQuery,
  NutritrackConnectorConnectorGeneratedQueryImpl<
      GetUserProfileQuery.Data,
      GetUserProfileQuery.Variables
  >(
    connector,
    GetUserProfileQuery.Companion.operationName,
    GetUserProfileQuery.Companion.dataDeserializer,
    GetUserProfileQuery.Companion.variablesSerializer,
  )


private class ListActivityMetricsQueryImpl(
  connector: NutritrackConnectorConnector
):
  ListActivityMetricsQuery,
  NutritrackConnectorConnectorGeneratedQueryImpl<
      ListActivityMetricsQuery.Data,
      ListActivityMetricsQuery.Variables
  >(
    connector,
    ListActivityMetricsQuery.Companion.operationName,
    ListActivityMetricsQuery.Companion.dataDeserializer,
    ListActivityMetricsQuery.Companion.variablesSerializer,
  )


private class ListFoodLogsQueryImpl(
  connector: NutritrackConnectorConnector
):
  ListFoodLogsQuery,
  NutritrackConnectorConnectorGeneratedQueryImpl<
      ListFoodLogsQuery.Data,
      ListFoodLogsQuery.Variables
  >(
    connector,
    ListFoodLogsQuery.Companion.operationName,
    ListFoodLogsQuery.Companion.dataDeserializer,
    ListFoodLogsQuery.Companion.variablesSerializer,
  )


private class ListHydrationLogsQueryImpl(
  connector: NutritrackConnectorConnector
):
  ListHydrationLogsQuery,
  NutritrackConnectorConnectorGeneratedQueryImpl<
      ListHydrationLogsQuery.Data,
      ListHydrationLogsQuery.Variables
  >(
    connector,
    ListHydrationLogsQuery.Companion.operationName,
    ListHydrationLogsQuery.Companion.dataDeserializer,
    ListHydrationLogsQuery.Companion.variablesSerializer,
  )


private class ListWeightLogsQueryImpl(
  connector: NutritrackConnectorConnector
):
  ListWeightLogsQuery,
  NutritrackConnectorConnectorGeneratedQueryImpl<
      ListWeightLogsQuery.Data,
      ListWeightLogsQuery.Variables
  >(
    connector,
    ListWeightLogsQuery.Companion.operationName,
    ListWeightLogsQuery.Companion.dataDeserializer,
    ListWeightLogsQuery.Companion.variablesSerializer,
  )


private class UpdateWeightLogMutationImpl(
  connector: NutritrackConnectorConnector
):
  UpdateWeightLogMutation,
  NutritrackConnectorConnectorGeneratedMutationImpl<
      UpdateWeightLogMutation.Data,
      UpdateWeightLogMutation.Variables
  >(
    connector,
    UpdateWeightLogMutation.Companion.operationName,
    UpdateWeightLogMutation.Companion.dataDeserializer,
    UpdateWeightLogMutation.Companion.variablesSerializer,
  )


private class UpsertUserProfileMutationImpl(
  connector: NutritrackConnectorConnector
):
  UpsertUserProfileMutation,
  NutritrackConnectorConnectorGeneratedMutationImpl<
      UpsertUserProfileMutation.Data,
      UpsertUserProfileMutation.Variables
  >(
    connector,
    UpsertUserProfileMutation.Companion.operationName,
    UpsertUserProfileMutation.Companion.dataDeserializer,
    UpsertUserProfileMutation.Companion.variablesSerializer,
  )


