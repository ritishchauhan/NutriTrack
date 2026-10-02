
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



public interface CreateFoodLogMutation :
    com.google.firebase.dataconnect.generated.GeneratedMutation<
      NutritrackConnectorConnector,
      CreateFoodLogMutation.Data,
      CreateFoodLogMutation.Variables
    >
{
  
    @kotlinx.serialization.Serializable
  public data class Variables(
  
    val userId: String,
  
    val foodName: String,
  
    val calories: Double,
  
    val protein: Double,
  
    val carbs: Double,
  
    val fat: Double,
  
    val fiber: Double,
  
    val portionMultiplier: Double,
  
    val mealType: String,
  
    val barcode: com.google.firebase.dataconnect.OptionalVariable<String?>,
  
    val imageUrl: com.google.firebase.dataconnect.OptionalVariable<String?>,
  
    val timestamp: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.TimestampSerializer::class) com.google.firebase.Timestamp,
  
    val details: String,
  
  ) {
    
    
      
      @kotlin.DslMarker public annotation class BuilderDsl

      
      @BuilderDsl
      public interface Builder {
        public var userId: String
        public var foodName: String
        public var calories: Double
        public var protein: Double
        public var carbs: Double
        public var fat: Double
        public var fiber: Double
        public var portionMultiplier: Double
        public var mealType: String
        public var barcode: String?
        public var imageUrl: String?
        public var timestamp: com.google.firebase.Timestamp
        public var details: String
        
      }

      public companion object {
        
        @Suppress("NAME_SHADOWING")
        public fun build(
          userId: String,foodName: String,calories: Double,protein: Double,carbs: Double,fat: Double,fiber: Double,portionMultiplier: Double,mealType: String,timestamp: com.google.firebase.Timestamp,details: String,
          block_: Builder.() -> Unit
        ): Variables {
          var userId= userId
            var foodName= foodName
            var calories= calories
            var protein= protein
            var carbs= carbs
            var fat= fat
            var fiber= fiber
            var portionMultiplier= portionMultiplier
            var mealType= mealType
            var barcode: com.google.firebase.dataconnect.OptionalVariable<String?> =
                com.google.firebase.dataconnect.OptionalVariable.Undefined
            var imageUrl: com.google.firebase.dataconnect.OptionalVariable<String?> =
                com.google.firebase.dataconnect.OptionalVariable.Undefined
            var timestamp= timestamp
            var details= details
            

          return object : Builder {
            override var userId: String
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { userId = value_ }
              
            override var foodName: String
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { foodName = value_ }
              
            override var calories: Double
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { calories = value_ }
              
            override var protein: Double
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { protein = value_ }
              
            override var carbs: Double
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { carbs = value_ }
              
            override var fat: Double
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { fat = value_ }
              
            override var fiber: Double
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { fiber = value_ }
              
            override var portionMultiplier: Double
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { portionMultiplier = value_ }
              
            override var mealType: String
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { mealType = value_ }
              
            override var barcode: String?
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { barcode = com.google.firebase.dataconnect.OptionalVariable.Value(value_) }
              
            override var imageUrl: String?
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { imageUrl = com.google.firebase.dataconnect.OptionalVariable.Value(value_) }
              
            override var timestamp: com.google.firebase.Timestamp
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { timestamp = value_ }
              
            override var details: String
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { details = value_ }
              
            
          }.apply(block_)
          .let {
            Variables(
              userId=userId,foodName=foodName,calories=calories,protein=protein,carbs=carbs,fat=fat,fiber=fiber,portionMultiplier=portionMultiplier,mealType=mealType,barcode=barcode,imageUrl=imageUrl,timestamp=timestamp,details=details,
            )
          }
        }
      }
    
  }
  

  
    @kotlinx.serialization.Serializable
  public data class Data(
  
    val foodLog_insert: FoodLogKey,
  
  ) {
    
    
  }
  

  public companion object {
    public val operationName: String = "CreateFoodLog"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables> =
      kotlinx.serialization.serializer()
  }
}

public fun CreateFoodLogMutation.ref(
  
    userId: String,foodName: String,calories: Double,protein: Double,carbs: Double,fat: Double,fiber: Double,portionMultiplier: Double,mealType: String,timestamp: com.google.firebase.Timestamp,details: String,

  
    block_: CreateFoodLogMutation.Variables.Builder.() -> Unit = {}
  
): com.google.firebase.dataconnect.MutationRef<
    CreateFoodLogMutation.Data,
    CreateFoodLogMutation.Variables
  > =
  ref(
    
      CreateFoodLogMutation.Variables.build(
        userId=userId,foodName=foodName,calories=calories,protein=protein,carbs=carbs,fat=fat,fiber=fiber,portionMultiplier=portionMultiplier,mealType=mealType,timestamp=timestamp,details=details,
  
    block_
      )
    
  )

public suspend fun CreateFoodLogMutation.execute(

  
    
      userId: String,foodName: String,calories: Double,protein: Double,carbs: Double,fat: Double,fiber: Double,portionMultiplier: Double,mealType: String,timestamp: com.google.firebase.Timestamp,details: String,

  
    block_: CreateFoodLogMutation.Variables.Builder.() -> Unit = {}

  ): com.google.firebase.dataconnect.MutationResult<
    CreateFoodLogMutation.Data,
    CreateFoodLogMutation.Variables
  > =
  ref(
    
      userId=userId,foodName=foodName,calories=calories,protein=protein,carbs=carbs,fat=fat,fiber=fiber,portionMultiplier=portionMultiplier,mealType=mealType,timestamp=timestamp,details=details,
  
    block_
    
  ).execute()


